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

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

public class Create2Operation extends AbstractCreateOperation {

  private static final BytesValue PREFIX = BytesValue.fromHexString("0xFF");

  public Create2Operation(final GasCalculator gasCalculator) {
    super(0xF5, "CREATE2", 4, 1, false, 1, gasCalculator);
  }

  @Override
  protected Address targetContractAddress(final MessageFrame frame) {
    final Address sender = frame.getRecipientAddress();
    final UInt256 offset = frame.getStackItem(1).asUInt256();
    final UInt256 length = frame.getStackItem(2).asUInt256();
    final Bytes32 salt = frame.getStackItem(3);
    final BytesValue initCode = frame.readMemory(offset, length);
    final Hash hash = Hash.hash(PREFIX.concat(sender).concat(salt).concat(Hash.hash(initCode)));
    return Address.extract(hash);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    final UInt256 initCodeLength = frame.getStackItem(2).asUInt256();
    final UInt256 numWords = initCodeLength.dividedCeilBy(Bytes32.SIZE);
    final Gas initCodeHashCost = getGasCalculator().getSha3WordGasCost().times(Gas.of(numWords));
    return getGasCalculator().createOperationGasCost(frame).plus(initCodeHashCost);
  }
}
