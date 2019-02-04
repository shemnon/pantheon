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
package tech.pegasys.pantheon.ethereum.eth.sync.fastsync;

import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockBody;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockParts;
import tech.pegasys.pantheon.ethereum.core.TransactionReceipt;

import java.util.List;

class BlockWithReceipts implements BlockParts {
  private final Block block;
  private final List<TransactionReceipt> receipts;

  BlockWithReceipts(final Block block, final List<TransactionReceipt> receipts) {
    this.block = block;
    this.receipts = receipts;
  }

  @Override
  public BlockHeader getHeader() {
    return block.getHeader();
  }

  @Override
  public BlockBody getBody() {
    return block.getBody();
  }

  public Block getBlock() {
    return block;
  }

  public List<TransactionReceipt> getReceipts() {
    return receipts;
  }

}
