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
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.ethereum.vm.Words;
import tech.pegasys.pantheon.util.bytes.Bytes32;

public class BalanceOperation extends AbstractOperation {

  public static final Gas FRONTIER_BALANCE_OPERATION_GAS_COST = Gas.of(20L);

  private static final Gas TANGERINE_WHISTLE_BALANCE_OPERATION_GAS_COST = Gas.of(400L);

  private final Gas cost;

  private BalanceOperation(final GasCalculator gasCalculator, final Gas cost) {
    super(0x31, "BALANCE", 1, 1, false, 1, gasCalculator);
    this.cost = cost;
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    return cost;
  }

  @Override
  public void execute(final MessageFrame frame) {
    final Address accountAddress = Words.toAddress(frame.popStackItem());
    final Account account = frame.getWorldState().get(accountAddress);
    frame.pushStackItem(account == null ? Bytes32.ZERO : account.getBalance().getBytes());
  }

  public static BalanceOperation frontier(final GasCalculator gasCalculator) {
    return new BalanceOperation(gasCalculator, FRONTIER_BALANCE_OPERATION_GAS_COST);
  }

  public static BalanceOperation tangerineWhistle(final GasCalculator gasCalculator) {
    return new BalanceOperation(gasCalculator, TANGERINE_WHISTLE_BALANCE_OPERATION_GAS_COST);
  }
}
