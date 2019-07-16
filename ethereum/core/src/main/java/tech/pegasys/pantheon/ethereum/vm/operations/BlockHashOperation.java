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
package tech.pegasys.pantheon.ethereum.vm.operations;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.ProcessableBlockHeader;
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.BlockHashLookup;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.UInt256;

public class BlockHashOperation extends AbstractOperation {

  private static final int MAX_RELATIVE_BLOCK = 255;

  private static final Gas BLOCKHASH_OPERATION_GAS_COST = Gas.of(20L);

  public BlockHashOperation(final GasCalculator gasCalculator) {
    super(0x40, "BLOCKHASH", 1, 1, false, 1, gasCalculator);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    return BLOCKHASH_OPERATION_GAS_COST;
  }

  @Override
  public void execute(final MessageFrame frame) {
    final UInt256 blockArg = frame.popStackItem().asUInt256();

    // Short-circuit if value is unreasonably large
    if (!blockArg.fitsLong()) {
      frame.pushStackItem(Bytes32.ZERO);
      return;
    }

    final long soughtBlock = blockArg.toLong();
    final ProcessableBlockHeader blockHeader = frame.getBlockHeader();
    final long currentBlockNumber = blockHeader.getNumber();
    final long mostRecentBlockNumber = currentBlockNumber - 1;

    // If the current block is the genesis block or the sought block is
    // not within the last 256 completed blocks, zero is returned.
    if (currentBlockNumber == BlockHeader.GENESIS_BLOCK_NUMBER
        || soughtBlock < (mostRecentBlockNumber - MAX_RELATIVE_BLOCK)
        || soughtBlock > mostRecentBlockNumber) {
      frame.pushStackItem(Bytes32.ZERO);
    } else {
      final BlockHashLookup blockHashLookup = frame.getBlockHashLookup();
      final Hash blockHash = blockHashLookup.getBlockHash(soughtBlock);
      frame.pushStackItem(blockHash);
    }
  }
}
