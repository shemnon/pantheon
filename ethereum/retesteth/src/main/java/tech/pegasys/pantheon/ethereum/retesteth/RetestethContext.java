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
import tech.pegasys.pantheon.config.JsonUtil;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.blockcreation.EthHashBlockCreator;
import tech.pegasys.pantheon.ethereum.blockcreation.IncrementingNonceGenerator;
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
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
import tech.pegasys.pantheon.ethereum.eth.transactions.PendingTransactions;
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
import tech.pegasys.pantheon.ethereum.storage.keyvalue.WorldStatePreimageKeyValueStorage;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Functions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RetestethContext {

  private static final Logger LOG = LogManager.getLogger();
  private static final EthHasher NO_WORK_HASHER =
      (final byte[] buffer, final long nonce, final long number, final byte[] headerHash) -> {};

  private final ReentrantLock contextLock = new ReentrantLock();
  private Address coinbase;
  private DefaultMutableBlockchain blockchain;
  private ProtocolContext<Void> protocolContext;
  private BlockchainQueries blockchainQueries;
  private ProtocolSchedule<Void> protocolSchedule;
  private HeaderValidationMode headerValidationMode;
  private BlockReplay blockReplay;
  private RetestethClock retesethClock;

  private TransactionPool transactionPool;
  private EthScheduler ethScheduler;
  private EthHashSolver ethHashSolver;

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
    final ObjectNode genesisConfig =
        normalizeKeys(JsonUtil.objectNodeFromString(genesisConfigString));

    retesethClock = new RetestethClock();
    clockTime.ifPresent(retesethClock::resetTime);
    final MetricsSystem metricsSystem = new NoOpMetricsSystem();

    protocolSchedule =
        MainnetProtocolSchedule.fromConfig(
            JsonGenesisConfigOptions.fromJsonObject((ObjectNode) genesisConfig.get("config")));
    if ("NoReward".equalsIgnoreCase(sealEngine)) {
      protocolSchedule = new NoRewardProtocolScheduleWrapper<>(protocolSchedule);
    }

    final GenesisState genesisState = GenesisState.fromJson(genesisConfigString, protocolSchedule);
    coinbase = genesisState.getBlock().getHeader().getCoinbase();

    final WorldStateArchive worldStateArchive =
        new WorldStateArchive(
            new WorldStateKeyValueStorage(new InMemoryKeyValueStorage()),
            new WorldStatePreimageKeyValueStorage(new InMemoryKeyValueStorage()));
    final MutableWorldState worldState = worldStateArchive.getMutable();
    genesisState.writeStateTo(worldState);

    blockchain = createInMemoryBlockchain(genesisState.getBlock());
    protocolContext = new ProtocolContext<>(blockchain, worldStateArchive, null);

    blockchainQueries = new BlockchainQueries(blockchain, worldStateArchive);

    final String sealengine = JsonUtil.getString(genesisConfig, "sealengine", "");
    headerValidationMode =
        "NoProof".equals(sealengine) || "NoReward".equals(sealEngine)
            ? HeaderValidationMode.LIGHT
            : HeaderValidationMode.FULL;

    final Iterable<Long> nonceGenerator = new IncrementingNonceGenerator(0);
    ethHashSolver =
        ("NoProof".equals(sealengine) || "NoReward".equals(sealEngine))
            ? new EthHashSolver(nonceGenerator, NO_WORK_HASHER)
            : new EthHashSolver(nonceGenerator, new EthHasher.Light());

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
    final EthHashBlockCreator blockCreator =
        new EthHashBlockCreator(
            coinbase,
            header -> BytesValue.of(),
            transactionPool.getPendingTransactions(),
            protocolContext,
            protocolSchedule,
            Functions.identity(),
            ethHashSolver,
            Wei.ZERO,
            blockchain.getChainHeadHeader());
    final Block block = blockCreator.createBlock(retesethClock.instant().getEpochSecond());

    // advance clock so next mine won't hit the same timestamp
    retesethClock.advanceSeconds(1);

    final BlockImporter<Void> blockImporter =
        protocolSchedule.getByBlockNumber(blockchain.getChainHeadBlockNumber()).getBlockImporter();
    return blockImporter.importBlock(
        protocolContext, block, getHeaderValidationMode(), getHeaderValidationMode());
  }

  public boolean rewindToBlock(final long blockNumber) {
    return blockchain.rewindToBlock(blockNumber);
  }

  private static DefaultMutableBlockchain createInMemoryBlockchain(final Block genesisBlock) {
    return createInMemoryBlockchain(genesisBlock, new MainnetBlockHeaderFunctions());
  }

  private static DefaultMutableBlockchain createInMemoryBlockchain(
      final Block genesisBlock, final BlockHeaderFunctions blockHeaderFunctions) {
    final InMemoryKeyValueStorage keyValueStorage = new InMemoryKeyValueStorage();
    return new DefaultMutableBlockchain(
        genesisBlock,
        new KeyValueStoragePrefixedKeyBlockchainStorage(keyValueStorage, blockHeaderFunctions),
        new NoOpMetricsSystem());
  }

  /* Converts all to lowercase for easier lookup since the keys in a 'genesis.json' file are assumed
   * case insensitive.
   */
  private static ObjectNode normalizeKeys(final ObjectNode genesis) {
    final ObjectNode normalized = JsonUtil.createEmptyObjectNode();
    genesis
        .fields()
        .forEachRemaining(
            entry -> {
              final String key = entry.getKey();
              final JsonNode value = entry.getValue();
              final String normalizedKey = key.toLowerCase(Locale.US);
              if (value instanceof ObjectNode) {
                normalized.set(normalizedKey, normalizeKeys((ObjectNode) value));
              } else {
                normalized.set(normalizedKey, value);
              }
            });
    return normalized;
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
    retesethClock.resetTime(epochSeconds);
  }

  BlockReplay getBlockReplay() {
    return blockReplay;
  }

  TransactionPool getTransactionPool() {
    return transactionPool;
  }

  PendingTransactions getPendingTransactions() {
    return transactionPool.getPendingTransactions();
  }
}
