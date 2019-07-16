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

import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.uint.UInt256;

public class ExpOperation extends AbstractOperation {

  private static final Gas EXP_OPERATION_BASE_GAS_COST = Gas.of(10);
  private static final Gas FRONTIER_EXP_OPERATION_BYTE_GAS_COST = Gas.of(10);
  private static final Gas SPURIOUS_DRAGON_EXP_OPERATION_BYTE_GAS_COST = Gas.of(50L);

  private final Gas baseCost;
  private final Gas byteCost;

  private ExpOperation(final GasCalculator gasCalculator, final Gas baseCost, final Gas byteCost) {
    super(0x0A, "EXP", 2, 1, false, 1, gasCalculator);
    this.baseCost = baseCost;
    this.byteCost = byteCost;
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    final UInt256 power = frame.getStackItem(1).asUInt256();
    final int numBytes = (power.bitLength() + 7) / 8;
    return byteCost.times(Gas.of(numBytes)).plus(baseCost);
  }

  @Override
  public void execute(final MessageFrame frame) {
    final UInt256 value0 = frame.popStackItem().asUInt256();
    final UInt256 value1 = frame.popStackItem().asUInt256();

    final UInt256 result = value0.pow(value1);

    frame.pushStackItem(result.getBytes());
  }

  public static ExpOperation frontier(final GasCalculator gasCalculator) {
    return new ExpOperation(
        gasCalculator, EXP_OPERATION_BASE_GAS_COST, FRONTIER_EXP_OPERATION_BYTE_GAS_COST);
  }

  public static ExpOperation spuriousDragon(final GasCalculator gasCalculator) {
    return new ExpOperation(
        gasCalculator, EXP_OPERATION_BASE_GAS_COST, SPURIOUS_DRAGON_EXP_OPERATION_BYTE_GAS_COST);
  }
}
