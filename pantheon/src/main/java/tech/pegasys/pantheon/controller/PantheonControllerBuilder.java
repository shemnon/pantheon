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
package tech.pegasys.pantheon.controller;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static tech.pegasys.pantheon.controller.KeyPairUtil.loadKeyPair;

import tech.pegasys.pantheon.config.GenesisConfigFile;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.blockcreation.MiningCoordinator;
import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.MiningParameters;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Synchronizer;
import tech.pegasys.pantheon.ethereum.eth.EthProtocol;
import tech.pegasys.pantheon.ethereum.eth.EthereumWireProtocolConfiguration;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManager;
import tech.pegasys.pantheon.ethereum.eth.peervalidation.DaoForkPeerValidator;
import tech.pegasys.pantheon.ethereum.eth.peervalidation.PeerValidatorRunner;
import tech.pegasys.pantheon.ethereum.eth.sync.DefaultSynchronizer;
import tech.pegasys.pantheon.ethereum.eth.sync.SyncMode;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncState;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPool;
import tech.pegasys.pantheon.ethereum.eth.transactions.TransactionPoolFactory;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethodFactory;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.p2p.config.SubProtocolConfiguration;
import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.RocksDbStorageProvider;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.services.kvstore.RocksDbConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PantheonControllerBuilder<C> {
  private static final Logger LOG = LogManager.getLogger();

  protected GenesisConfigFile genesisConfig;
  protected SynchronizerConfiguration syncConfig;
  protected EthereumWireProtocolConfiguration ethereumWireProtocolConfiguration;
  protected Integer networkId;
  protected MiningParameters miningParameters;
  protected MetricsSystem metricsSystem;
  protected PrivacyParameters privacyParameters;
  protected Path dataDirectory;
  protected Clock clock;
  protected Integer maxPendingTransactions;
  protected Integer pendingTransactionRetentionPeriod;
  protected KeyPair nodeKeys;
  private StorageProvider storageProvider;
  private final List<Runnable> shutdownActions = new ArrayList<>();
  private RocksDbConfiguration rocksDbConfiguration;

  public PantheonControllerBuilder<C> rocksDbConfiguration(
      final RocksDbConfiguration rocksDbConfiguration) {
    this.rocksDbConfiguration = rocksDbConfiguration;
    return this;
  }

  public PantheonControllerBuilder<C> storageProvider(final StorageProvider storageProvider) {
    this.storageProvider = storageProvider;
    return this;
  }

  public PantheonControllerBuilder<C> genesisConfigFile(final GenesisConfigFile genesisConfig) {
    this.genesisConfig = genesisConfig;
    return this;
  }

  public PantheonControllerBuilder<C> synchronizerConfiguration(
      final SynchronizerConfiguration synchronizerConfig) {
    this.syncConfig = synchronizerConfig;
    return this;
  }

  public PantheonControllerBuilder<C> ethereumWireProtocolConfiguration(
      final EthereumWireProtocolConfiguration ethereumWireProtocolConfiguration) {
    this.ethereumWireProtocolConfiguration = ethereumWireProtocolConfiguration;
    return this;
  }

  public PantheonControllerBuilder<C> networkId(final int networkId) {
    this.networkId = networkId;
    return this;
  }

  public PantheonControllerBuilder<C> miningParameters(final MiningParameters miningParameters) {
    this.miningParameters = miningParameters;
    return this;
  }

  public PantheonControllerBuilder<C> nodePrivateKeyFile(final File nodePrivateKeyFile)
      throws IOException {
    this.nodeKeys = loadKeyPair(nodePrivateKeyFile);
    return this;
  }

  public PantheonControllerBuilder<C> nodeKeys(final KeyPair nodeKeys) {
    this.nodeKeys = nodeKeys;
    return this;
  }

  public PantheonControllerBuilder<C> metricsSystem(final MetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
    return this;
  }

  public PantheonControllerBuilder<C> privacyParameters(final PrivacyParameters privacyParameters) {
    this.privacyParameters = privacyParameters;
    return this;
  }

  public PantheonControllerBuilder<C> dataDirectory(final Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    return this;
  }

  public PantheonControllerBuilder<C> clock(final Clock clock) {
    this.clock = clock;
    return this;
  }

  public PantheonControllerBuilder<C> maxPendingTransactions(final int maxPendingTransactions) {
    this.maxPendingTransactions = maxPendingTransactions;
    return this;
  }

  public PantheonControllerBuilder<C> pendingTransactionRetentionPeriod(
      final int pendingTransactionRetentionPeriod) {
    this.pendingTransactionRetentionPeriod = pendingTransactionRetentionPeriod;
    return this;
  }

  public PantheonController<C> build() throws IOException {
    checkNotNull(genesisConfig, "Missing genesis config");
    checkNotNull(syncConfig, "Missing sync config");
    checkNotNull(ethereumWireProtocolConfiguration, "Missing ethereum protocol configuration");
    checkNotNull(networkId, "Missing network ID");
    checkNotNull(miningParameters, "Missing mining parameters");
    checkNotNull(metricsSystem, "Missing metrics system");
    checkNotNull(privacyParameters, "Missing privacy parameters");
    checkNotNull(dataDirectory, "Missing data directory"); // Why do we need this?
    checkNotNull(clock, "Mising clock");
    checkNotNull(maxPendingTransactions, "Missing max pending transactions");
    checkNotNull(nodeKeys, "Missing node keys");
    checkArgument(
        storageProvider != null || rocksDbConfiguration != null,
        "Must supply either a storage provider or RocksDB configuration");
    checkArgument(
        storageProvider == null || rocksDbConfiguration == null,
        "Must supply either storage provider or RocksDB confguration, but not both");
    privacyParameters.setSigningKeyPair(nodeKeys);

    if (storageProvider == null && rocksDbConfiguration != null) {
      storageProvider = RocksDbStorageProvider.create(rocksDbConfiguration, metricsSystem);
    }

    prepForBuild();

    final ProtocolSchedule<C> protocolSchedule = createProtocolSchedule();
    final GenesisState genesisState = GenesisState.fromConfig(genesisConfig, protocolSchedule);
    final ProtocolContext<C> protocolContext =
        ProtocolContext.init(
            storageProvider,
            genesisState,
            protocolSchedule,
            metricsSystem,
            this::createConsensusContext);
    final MutableBlockchain blockchain = protocolContext.getBlockchain();

    final boolean fastSyncEnabled = syncConfig.syncMode().equals(SyncMode.FAST);
    final EthProtocolManager ethProtocolManager =
        createEthProtocolManager(protocolContext, fastSyncEnabled);
    final SyncState syncState =
        new SyncState(blockchain, ethProtocolManager.ethContext().getEthPeers());
    final Synchronizer synchronizer =
        new DefaultSynchronizer<>(
            syncConfig,
            protocolSchedule,
            protocolContext,
            protocolContext.getWorldStateArchive().getStorage(),
            ethProtocolManager.ethContext(),
            syncState,
            dataDirectory,
            clock,
            metricsSystem);

    final OptionalLong daoBlock = genesisConfig.getConfigOptions().getDaoForkBlock();
    if (daoBlock.isPresent()) {
      // Setup dao validator
      final EthContext ethContext = ethProtocolManager.ethContext();
      final DaoForkPeerValidator daoForkPeerValidator =
          new DaoForkPeerValidator(
              ethContext, protocolSchedule, metricsSystem, daoBlock.getAsLong());
      PeerValidatorRunner.runValidator(ethContext, daoForkPeerValidator);
    }

    final TransactionPool transactionPool =
        TransactionPoolFactory.createTransactionPool(
            protocolSchedule,
            protocolContext,
            ethProtocolManager.ethContext(),
            clock,
            maxPendingTransactions,
            metricsSystem,
            syncState,
            pendingTransactionRetentionPeriod);

    final MiningCoordinator miningCoordinator =
        createMiningCoordinator(
            protocolSchedule,
            protocolContext,
            transactionPool,
            miningParameters,
            syncState,
            ethProtocolManager);

    final SubProtocolConfiguration subProtocolConfiguration =
        createSubProtocolConfiguration(ethProtocolManager);

    final JsonRpcMethodFactory additionalJsonRpcMethodFactory =
        createAdditionalJsonRpcMethodFactory(protocolContext);
    return new PantheonController<>(
        protocolSchedule,
        protocolContext,
        genesisConfig.getConfigOptions(),
        subProtocolConfiguration,
        synchronizer,
        additionalJsonRpcMethodFactory,
        nodeKeys,
        transactionPool,
        miningCoordinator,
        privacyParameters,
        () -> {
          shutdownActions.forEach(Runnable::run);
          try {
            storageProvider.close();
            if (privacyParameters.getPrivateStorageProvider() != null) {
              privacyParameters.getPrivateStorageProvider().close();
            }
          } catch (final IOException e) {
            LOG.error("Failed to close storage provider", e);
          }
        });
  }

  protected void prepForBuild() {}

  protected JsonRpcMethodFactory createAdditionalJsonRpcMethodFactory(
      final ProtocolContext<C> protocolContext) {
    return apis -> Collections.emptyMap();
  }

  protected SubProtocolConfiguration createSubProtocolConfiguration(
      final EthProtocolManager ethProtocolManager) {
    return new SubProtocolConfiguration().withSubProtocol(EthProtocol.get(), ethProtocolManager);
  }

  protected final void addShutdownAction(final Runnable action) {
    shutdownActions.add(action);
  }

  protected abstract MiningCoordinator createMiningCoordinator(
      ProtocolSchedule<C> protocolSchedule,
      ProtocolContext<C> protocolContext,
      TransactionPool transactionPool,
      MiningParameters miningParameters,
      SyncState syncState,
      EthProtocolManager ethProtocolManager);

  protected abstract ProtocolSchedule<C> createProtocolSchedule();

  protected abstract C createConsensusContext(
      Blockchain blockchain, WorldStateArchive worldStateArchive);

  protected EthProtocolManager createEthProtocolManager(
      final ProtocolContext<C> protocolContext, final boolean fastSyncEnabled) {
    return new EthProtocolManager(
        protocolContext.getBlockchain(),
        protocolContext.getWorldStateArchive(),
        networkId,
        fastSyncEnabled,
        syncConfig.downloaderParallelism(),
        syncConfig.transactionsParallelism(),
        syncConfig.computationParallelism(),
        clock,
        metricsSystem,
        ethereumWireProtocolConfiguration);
  }
}
