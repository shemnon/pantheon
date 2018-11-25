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

import tech.pegasys.pantheon.config.GenesisConfigOptions;
import tech.pegasys.pantheon.consensus.ibft.IbftBlockHashing;
import tech.pegasys.pantheon.consensus.ibft.IbftProtocolSchedule;
import tech.pegasys.pantheon.ethereum.core.BlockHashFunction;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;

import java.io.File;
import java.io.IOException;

public class IBFTGenesisFileModule extends GenesisFileModule {

  public IBFTGenesisFileModule(final File genesisFile) throws IOException {
    super(genesisFile);
  }

  public IBFTGenesisFileModule(final String genesisConfig) {
    super(genesisConfig);
  }

  @Override
  ProtocolSchedule<?> provideProtocolSchedule(final GenesisConfigOptions configOptions) {
    return IbftProtocolSchedule.create(configOptions);
  }

  @Override
  BlockHashFunction blockHashFunction() {
    return IbftBlockHashing::calculateHashOfIbftBlockOnChain;
  }
}
