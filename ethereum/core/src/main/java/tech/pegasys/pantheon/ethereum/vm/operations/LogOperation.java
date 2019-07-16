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
import tech.pegasys.pantheon.ethereum.core.Log;
import tech.pegasys.pantheon.ethereum.core.LogTopic;
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.EVM;
import tech.pegasys.pantheon.ethereum.vm.ExceptionalHaltReason;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.EnumSet;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

public class LogOperation extends AbstractOperation {

  private final int numTopics;

  private static final Gas LOG_OPERATION_BASE_GAS_COST = Gas.of(375L);
  private static final Gas LOG_OPERATION_DATA_BYTE_GAS_COST = Gas.of(8L);
  private static final Gas LOG_OPERATION_TOPIC_GAS_COST = Gas.of(375L);

  public LogOperation(final int numTopics, final GasCalculator gasCalculator) {
    super(0xA0 + numTopics, "LOG" + numTopics, numTopics + 2, 0, false, 1, gasCalculator);
    this.numTopics = numTopics;
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    final UInt256 dataOffset = frame.getStackItem(0).asUInt256();
    final UInt256 dataLength = frame.getStackItem(1).asUInt256();

    return Gas.ZERO
        .plus(LOG_OPERATION_BASE_GAS_COST)
        .plus(LOG_OPERATION_DATA_BYTE_GAS_COST.times(Gas.of(dataLength)))
        .plus(LOG_OPERATION_TOPIC_GAS_COST.times(numTopics))
        .plus(getGasCalculator().memoryExpansionGasCost(frame, dataOffset, dataLength));
  }

  @Override
  public void execute(final MessageFrame frame) {
    final Address address = frame.getRecipientAddress();

    final UInt256 dataLocation = frame.popStackItem().asUInt256();
    final UInt256 numBytes = frame.popStackItem().asUInt256();
    final BytesValue data = frame.readMemory(dataLocation, numBytes);

    final ImmutableList.Builder<LogTopic> builder =
        ImmutableList.builderWithExpectedSize(numTopics);
    for (int i = 0; i < numTopics; i++) {
      builder.add(LogTopic.of(frame.popStackItem()));
    }

    frame.addLog(new Log(address, data, builder.build()));
  }

  @Override
  public Optional<ExceptionalHaltReason> exceptionalHaltCondition(
      final MessageFrame frame,
      final EnumSet<ExceptionalHaltReason> previousReasons,
      final EVM evm) {
    return frame.isStatic()
        ? Optional.of(ExceptionalHaltReason.ILLEGAL_STATE_CHANGE)
        : Optional.empty();
  }
}
