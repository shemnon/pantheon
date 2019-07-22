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
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.BlockReplay;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.worldstate.DebuggableWorldStateArchive;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RetestethContext {

  private static final Logger LOG = LogManager.getLogger();

  private final ReentrantLock contextLock = new ReentrantLock();
  private MutableBlockchain blockchain;
  private ProtocolContext<Void> protocolContext;
  private BlockchainQueries blockchainQueries;
  private ProtocolSchedule<Void> protocolSchedule;
  private HeaderValidationMode headerValidationMode;
  private BlockReplay blockReplay;

  public boolean resetContext(final String genesisConfigString) {
    final JsonObject genesisConfig = normalizeKeys(new JsonObject(genesisConfigString));

    contextLock.lock();
    try {
      protocolSchedule =
          MainnetProtocolSchedule.fromConfig(
              JsonGenesisConfigOptions.fromJsonObject(genesisConfig.getJsonObject("config")));

      final GenesisState genesisState =
          GenesisState.fromJson(genesisConfigString, protocolSchedule);

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

      return true;
    } catch (final Exception e) {
      LOG.error("Error shutting down existing runner", e);
      return false;
    } finally {
      contextLock.unlock();
    }
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

  public BlockReplay getBlockReplay() {
    return blockReplay;
  }
}
