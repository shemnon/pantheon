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
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.MutableWorldView;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.db.BlockchainStorage;
import tech.pegasys.pantheon.ethereum.db.DefaultMutableBlockchain;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStorageWorldStateStorage;
import tech.pegasys.pantheon.ethereum.worldstate.DefaultMutableWorldState;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateStorage;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = {GenesisFileModule.class, DataStoreModule.class})
public class BlockchainModule {

  @Singleton
  @Provides
  Blockchain provideBlockchain(
      @Named("GenesisBlock") final Block genesisBlock, final BlockchainStorage blockchainStorage) {
    return new DefaultMutableBlockchain(genesisBlock, blockchainStorage);
  }

  @Provides
  MutableWorldView getMutableWorldView(final WorldStateStorage worldStateStorage) {
    return new DefaultMutableWorldState(worldStateStorage);
  }

  @Provides
  WorldStateStorage provideWorldStateStorage(final KeyValueStorage keyValueStorage) {
    return new KeyValueStorageWorldStateStorage(keyValueStorage);
  }

  @Provides
  WorldUpdater provideWorldUpdater(final MutableWorldView mutableWorldView) {
    return mutableWorldView.updater();
  }
}
