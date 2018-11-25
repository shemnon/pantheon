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

import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.vm.EVM;

import java.util.function.Function;
import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(
  modules = {
    ProtocolModule.class,
    GenesisFileModule.class,
    InMemoryDataStoreModule.class,
    BlockchainModule.class
  }
)
public interface EvmToolComponent {

  Function<Integer, EVM> getEvmAtBlock();

  WorldUpdater getWorldUpdater();

  Blockchain getBlockchain();
}
