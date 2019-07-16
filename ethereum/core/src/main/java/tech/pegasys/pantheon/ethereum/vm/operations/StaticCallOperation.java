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

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.vm.AbstractCallOperation;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.ethereum.vm.Words;
import tech.pegasys.pantheon.util.uint.UInt256;

public class StaticCallOperation extends AbstractCallOperation {

  public StaticCallOperation(final GasCalculator gasCalculator) {
    super(0xFA, "STATICCALL", 6, 1, false, 1, gasCalculator);
  }

  @Override
  protected Gas gas(final MessageFrame frame) {
    return Gas.of(frame.getStackItem(0));
  }

  @Override
  protected Address to(final MessageFrame frame) {
    return Words.toAddress(frame.getStackItem(1));
  }

  @Override
  protected Wei value(final MessageFrame frame) {
    return Wei.ZERO;
  }

  @Override
  protected Wei apparentValue(final MessageFrame frame) {
    return value(frame);
  }

  @Override
  protected UInt256 inputDataOffset(final MessageFrame frame) {
    return frame.getStackItem(2).asUInt256();
  }

  @Override
  protected UInt256 inputDataLength(final MessageFrame frame) {
    return frame.getStackItem(3).asUInt256();
  }

  @Override
  protected UInt256 outputDataOffset(final MessageFrame frame) {
    return frame.getStackItem(4).asUInt256();
  }

  @Override
  protected UInt256 outputDataLength(final MessageFrame frame) {
    return frame.getStackItem(5).asUInt256();
  }

  @Override
  protected Address address(final MessageFrame frame) {
    return to(frame);
  }

  @Override
  protected Address sender(final MessageFrame frame) {
    return frame.getRecipientAddress();
  }

  @Override
  public Gas gasAvailableForChildCall(final MessageFrame frame) {
    return getGasCalculator().gasAvailableForChildCall(frame, gas(frame), !value(frame).isZero());
  }

  @Override
  protected boolean isStatic(final MessageFrame frame) {
    return true;
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    final Gas stipend = gas(frame);
    final UInt256 inputDataOffset = inputDataOffset(frame).asUInt256();
    final UInt256 inputDataLength = inputDataLength(frame).asUInt256();
    final UInt256 outputDataOffset = outputDataOffset(frame).asUInt256();
    final UInt256 outputDataLength = outputDataLength(frame).asUInt256();
    final Account recipient = frame.getWorldState().get(address(frame));

    return getGasCalculator()
        .callOperationGasCost(
            frame,
            stipend,
            inputDataOffset,
            inputDataLength,
            outputDataOffset,
            outputDataLength,
            value(frame),
            recipient);
  }
}
