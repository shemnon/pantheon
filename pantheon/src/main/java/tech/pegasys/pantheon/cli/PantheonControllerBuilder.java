/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.cli;

import static tech.pegasys.pantheon.controller.KeyPairUtil.loadKeyPair;

import tech.pegasys.pantheon.config.GenesisConfigFile;
import tech.pegasys.pantheon.controller.PantheonController;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.ethereum.core.MiningParameters;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.eth.EthereumWireProtocolConfiguration;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.eth.transactions.PendingTransactions;
import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.RocksDbStorageProvider;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.services.kvstore.RocksDbConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

public class PantheonControllerBuilder {

  private SynchronizerConfiguration synchronizerConfiguration;
  private EthereumWireProtocolConfiguration ethereumWireProtocolConfiguration;
  private RocksDbConfiguration rocksDbConfiguration;
  private Path homePath;
  private EthNetworkConfig ethNetworkConfig;
  private MiningParameters miningParameters;
  private boolean devMode;
  private File nodePrivateKeyFile;
  private MetricsSystem metricsSystem;
  private PrivacyParameters privacyParameters;
  private Integer maxPendingTransactions = PendingTransactions.MAX_PENDING_TRANSACTIONS;

  public PantheonControllerBuilder synchronizerConfiguration(
      final SynchronizerConfiguration synchronizerConfiguration) {
    this.synchronizerConfiguration = synchronizerConfiguration;
    return this;
  }

  public PantheonControllerBuilder ethereumWireProtocolConfiguration(
      final EthereumWireProtocolConfiguration ethereumWireProtocolConfiguration) {
    this.ethereumWireProtocolConfiguration = ethereumWireProtocolConfiguration;
    return this;
  }

  public PantheonControllerBuilder rocksDbConfiguration(
      final RocksDbConfiguration rocksDbConfiguration) {
    this.rocksDbConfiguration = rocksDbConfiguration;
    return this;
  }

  public PantheonControllerBuilder homePath(final Path homePath) {
    this.homePath = homePath;
    return this;
  }

  public PantheonControllerBuilder ethNetworkConfig(final EthNetworkConfig ethNetworkConfig) {
    this.ethNetworkConfig = ethNetworkConfig;
    return this;
  }

  public PantheonControllerBuilder miningParameters(final MiningParameters miningParameters) {
    this.miningParameters = miningParameters;
    return this;
  }

  public PantheonControllerBuilder devMode(final boolean devMode) {
    this.devMode = devMode;
    return this;
  }

  public PantheonControllerBuilder nodePrivateKeyFile(final File nodePrivateKeyFile) {
    this.nodePrivateKeyFile = nodePrivateKeyFile;
    return this;
  }

  public PantheonControllerBuilder metricsSystem(final MetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
    return this;
  }

  public PantheonControllerBuilder maxPendingTransactions(final Integer maxPendingTransactions) {
    this.maxPendingTransactions = maxPendingTransactions;
    return this;
  }

  public PantheonControllerBuilder privacyParameters(final PrivacyParameters privacyParameters) {
    this.privacyParameters = privacyParameters;
    return this;
  }

  public PantheonController<?> build() throws IOException {
    // instantiate a controller with mainnet config if no genesis file is defined
    // otherwise use the indicated genesis file
    final KeyPair nodeKeys = loadKeyPair(nodePrivateKeyFile);
    privacyParameters.setSigningKeyPair(nodeKeys);

    final StorageProvider storageProvider =
        RocksDbStorageProvider.create(rocksDbConfiguration, metricsSystem);

    final GenesisConfigFile genesisConfigFile;
    if (devMode) {
      genesisConfigFile = GenesisConfigFile.development();
    } else {
      final String genesisConfig = ethNetworkConfig.getGenesisConfig();
      genesisConfigFile = GenesisConfigFile.fromConfig(genesisConfig);
    }
    Clock clock = Clock.systemUTC();
    return PantheonController.fromConfig(
        genesisConfigFile,
        synchronizerConfiguration,
        ethereumWireProtocolConfiguration,
        storageProvider,
        ethNetworkConfig.getNetworkId(),
        miningParameters,
        nodeKeys,
        metricsSystem,
        privacyParameters,
        homePath,
        clock,
        maxPendingTransactions);
  }
}
