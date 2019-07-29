/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.retesteth;

import tech.pegasys.pantheon.config.JsonGenesisConfigOptions;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.blockcreation.EthHashBlockCreator;
import tech.pegasys.pantheon.ethereum.blockcreation.RandomNonceGenerator;
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthMessages;
import tech.pegasys.pantheon.ethereum.eth.manager.EthPeers;
import tech.pegasys.pantheon.ethereum.eth.manager.EthScheduler;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncState;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPool;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPoolConfiguration;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPoolFactory;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.BlockReplay;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.mainnet.EthHashSolver;
import tech.pegasys.pantheon.ethereum.mainnet.EthHasher;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.worldstate.DebuggableWorldStateArchive;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.base.Functions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RetestethContext {

  private static final Logger LOG = LogManager.getLogger();

  private final ReentrantLock contextLock = new ReentrantLock();
  private String genesisConfig;
  private Address coinbase;
  private MutableBlockchain blockchain;
  private ProtocolContext<Void> protocolContext;
  private BlockchainQueries blockchainQueries;
  private ProtocolSchedule<Void> protocolSchedule;
  private HeaderValidationMode headerValidationMode;
  private BlockReplay blockReplay;
  private RetestethClock retesethClock;

  private TransactionPool transactionPool;
  private EthScheduler ethScheduler;
  private String sealEngine;

  public boolean resetContext(
      final String genesisConfigString, final String sealEngine, final Optional<Long> clockTime) {
    contextLock.lock();
    try {
      tearDownContext();
      return buildContext(genesisConfigString, sealEngine, clockTime);
    } catch (final Exception e) {
      LOG.error("Error shutting down existing runner", e);
      return false;
    } finally {
      contextLock.unlock();
    }
  }

  private void tearDownContext() {
    try {
      if (ethScheduler != null) {
        ethScheduler.stop();
        ethScheduler.awaitStop();
      }
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean buildContext(
      final String genesisConfigString, final String sealEngine, final Optional<Long> clockTime) {
    genesisConfig = genesisConfigString;
    this.sealEngine = sealEngine;

    final JsonObject genesisConfig = normalizeKeys(new JsonObject(genesisConfigString));

    retesethClock = new RetestethClock();
    clockTime.ifPresent(retesethClock::resetTime);
    final MetricsSystem metricsSystem = new NoOpMetricsSystem();

    protocolSchedule =
        MainnetProtocolSchedule.fromConfig(
            JsonGenesisConfigOptions.fromJsonObject(genesisConfig.getJsonObject("config")));
    if ("NoReward".equalsIgnoreCase(sealEngine)) {
      protocolSchedule = new NoRewardProtocolScheduleWrapper<>(protocolSchedule);
    }

    final GenesisState genesisState = GenesisState.fromJson(genesisConfigString, protocolSchedule);
    coinbase = genesisState.getBlock().getHeader().getCoinbase();

    final DebuggableWorldStateArchive worldStateArchive =
        new DebuggableWorldStateArchive(
            new WorldStateKeyValueStorage(new InMemoryKeyValueStorage()));
    final MutableWorldState worldState = worldStateArchive.getMutable();
    genesisState.writeStateTo(worldState);

    blockchain = createInMemoryBlockchain(genesisState.getBlock());
    protocolContext = new ProtocolContext<>(blockchain, worldStateArchive, null);

    blockchainQueries = new BlockchainQueries(blockchain, worldStateArchive);

    headerValidationMode =
        genesisConfig.getString("sealengine", "").equals("NoProof")
            ? HeaderValidationMode.LIGHT
            : HeaderValidationMode.FULL;

    blockReplay =
        new BlockReplay(
            protocolSchedule,
            blockchainQueries.getBlockchain(),
            blockchainQueries.getWorldStateArchive());

    // mining support

    final EthPeers ethPeers = new EthPeers("reteseth", retesethClock, metricsSystem);
    final SyncState syncState = new SyncState(blockchain, ethPeers);

    ethScheduler = new EthScheduler(1, 1, 1, metricsSystem);
    final EthContext ethContext = new EthContext(ethPeers, new EthMessages(), ethScheduler);

    final TransactionPoolConfiguration transactionPoolConfiguration =
        TransactionPoolConfiguration.builder().build();

    transactionPool =
        TransactionPoolFactory.createTransactionPool(
            protocolSchedule,
            protocolContext,
            ethContext,
            retesethClock,
            metricsSystem,
            syncState,
            Wei.ZERO,
            transactionPoolConfiguration);

    LOG.trace("Genesis Block {} ", genesisState::getBlock);

    return true;
  }

  public boolean mineNewBlock() {
    retesethClock.tickSeconds(15);

    final EthHashSolver solver =
        new EthHashSolver(new RandomNonceGenerator(), new EthHasher.Light());
    final EthHashBlockCreator blockCreator =
        new EthHashBlockCreator(
            coinbase,
            header -> BytesValue.of(),
            transactionPool.getPendingTransactions(),
            protocolContext,
            protocolSchedule,
            Functions.identity(),
            solver,
            Wei.ZERO,
            blockchain.getChainHeadHeader());
    final Block block = blockCreator.createBlock(retesethClock.instant().getEpochSecond());

    final BlockImporter<Void> blockImporter =
        protocolSchedule.getByBlockNumber(blockchain.getChainHeadBlockNumber()).getBlockImporter();
    return blockImporter.importBlock(
        protocolContext, block, getHeaderValidationMode(), getHeaderValidationMode());
  }

  private static MutableBlockchain createInMemoryBlockchain(final Block genesisBlock) {
    return createInMemoryBlockchain(genesisBlock, new MainnetBlockHeaderFunctions());
  }

  private static MutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock, final BlockHeaderFunctions blockHeaderFunctions) {
    final InMemoryKeyValueStorage keyValueStorage = new InMemoryKeyValueStorage();
    return new DefaultMutableBlockchain(
        genesisBlock,
        new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, blockHeaderFunctions),
        new NoOpMetricsSystem());
  }

  @SuppressWarnings("unchecked")
  private static JsonObject normalizeKeys(final JsonObject genesis) {
    final Map<String, Object> normalized = new HashMap<>();
    genesis
        .getMap()
        .forEach(
            (key, value) -> {
              final String normalizedKey = key.toLowerCase(Locale.US);
              if (value instanceof JsonObject) {
                normalized.put(normalizedKey, normalizeKeys((JsonObject) value));
              } else if (value instanceof Map) {
                normalized.put(
                    normalizedKey, normalizeKeys(new JsonObject((Map<String, Object>) value)));
              } else {
                normalized.put(normalizedKey, value);
              }
            });
    return new JsonObject(normalized);
  }

  public ProtocolSchedule<Void> getProtocolSchedule() {
    return protocolSchedule;
  }

  public ProtocolContext<Void> getProtocolContext() {
    return protocolContext;
  }

  public long getBlockHeight() {
    return blockchain.getChainHeadBlockNumber();
  }

  public ProtocolSpec<Void> getProtocolSpec(final long blockNumber) {
    return getProtocolSchedule().getByBlockNumber(blockNumber);
  }

  public BlockHeader getBlockHeader(final long blockNumber) {
    return blockchain.getBlockHeader(blockNumber).get();
  }

  public BlockchainQueries getBlockchainQueries() {
    return blockchainQueries;
  }

  public HeaderValidationMode getHeaderValidationMode() {
    return headerValidationMode;
  }

  public void setTimestamp(final long epochSeconds) {
    // this impacts the genesis block, so rebuild
    resetContext(genesisConfig, sealEngine, Optional.of(epochSeconds));
  }

  public BlockReplay getBlockReplay() {
    return blockReplay;
  }

  public TransactionPool getTransactionPool() {
    return transactionPool;
  }

  public String getSealEngine() {
    return sealEngine;
  }
}
