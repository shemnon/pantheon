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

import tech.pegasys.pantheon.ethereum.chain.BlockAddedEvent;
import tech.pegasys.pantheon.ethereum.chain.BlockchainStorage;
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockBody;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.metrics.MetricsSystem;

import java.util.Optional;

public class RetestethMutableBlockchain extends DefaultMutableBlockchain {
  public RetestethMutableBlockchain(
      final Block genesisBlock,
      final BlockchainStorage blockchainStorage,
      final MetricsSystem metricsSystem) {
    super(genesisBlock, blockchainStorage, metricsSystem);
  }

  public boolean rewindToBlock(final long oldBlockNumber) {
    final Optional<Hash> oldBlockHash = blockchainStorage.getBlockHash(oldBlockNumber);
    if (oldBlockHash.isEmpty()) {
      return false;
    }

    final Optional<BlockHeader> oldBlockHeader =
        blockchainStorage.getBlockHeader(oldBlockHash.get());
    final Optional<BlockBody> oldBlockBody = blockchainStorage.getBlockBody(oldBlockHash.get());
    if (oldBlockHeader.isEmpty() || oldBlockBody.isEmpty()) {
      return false;
    }
    final Block oldBlock = new Block(oldBlockHeader.get(), oldBlockBody.get());

    final BlockchainStorage.Updater updater = blockchainStorage.updater();
    final BlockAddedEvent result = this.handleChainReorg(updater, oldBlock);
    updater.commit();

    return result.isNewCanonicalHead();
  }
}
