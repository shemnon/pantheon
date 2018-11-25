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
package tech.pegasys.pantheon.evmtool;

import tech.pegasys.pantheon.config.GenesisConfigFile;
import tech.pegasys.pantheon.config.GenesisConfigOptions;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHashFunction;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.json.JsonObject;

@Module
public class GenesisFileModule {

  final String genesisConfig;

  protected GenesisFileModule(final File genesisFile) throws IOException {
    this.genesisConfig =
        new String(Files.readAllBytes(genesisFile.toPath()), Charset.defaultCharset());
  }

  protected GenesisFileModule(final String genesisConfig) {
    this.genesisConfig = genesisConfig;
  }

  @Singleton
  @Provides
  GenesisConfigFile providesGenesisConfigFile() {
    return GenesisConfigFile.fromConfig(genesisConfig);
  }

  @Singleton
  @Provides
  GenesisConfigOptions provideGenesisConfigOptions(final GenesisConfigFile genesisConfigFile) {
    return genesisConfigFile.getConfigOptions();
  }

  @Singleton
  @Provides
  ProtocolSchedule<?> provideProtocolSchedule(final GenesisConfigOptions configOptions) {
    throw new RuntimeException("Abstract");
  }

  @Singleton
  @Provides
  GenesisState provideGenesisState(
      final GenesisConfigFile genesisConfigFile, final ProtocolSchedule<?> protocolSchedule) {
    return GenesisState.fromConfig(genesisConfigFile, protocolSchedule);
  }

  @Singleton
  @Provides
  BlockHashFunction blockHashFunction() {
    throw new RuntimeException("Abstract");
  }

  @Singleton
  @Provides
  @Named("GenesisBlock")
  Block provideGenesisBlock(final GenesisState genesisState) {
    return genesisState.getBlock();
  }

  static GenesisFileModule creteGenesisModule(final File genesisFile) throws IOException {
    return createGenesisModule(
        new String(Files.readAllBytes(genesisFile.toPath()), Charset.defaultCharset()));
  }

  private static GenesisFileModule createGenesisModule(final String genesisConfig) {
    // duplicating work from JsonGenesisConfigOptions, but in a refactoring this goes away.
    final JsonObject genesis = new JsonObject(genesisConfig);
    final JsonObject config = genesis.getJsonObject("config");
    if (config.containsKey("ethash")) {
      return new MainnetGenesisFileModule(genesisConfig);
    } else if (config.containsKey("ibft")) {
      return new IBFTGenesisFileModule(genesisConfig);
    } else if (config.containsKey("clique")) {
      return new CliqueGenesisFileModule(genesisConfig);
    } else {
      // default is mainnet
      return new MainnetGenesisFileModule(genesisConfig);
    }
  }
}
