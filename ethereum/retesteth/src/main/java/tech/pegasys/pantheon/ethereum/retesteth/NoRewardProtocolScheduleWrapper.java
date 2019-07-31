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

import tech.pegasys.pantheon.ethereum.BlockValidator;
import tech.pegasys.pantheon.ethereum.MainnetBlockValidator;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.core.TransactionFilter;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.BlockProcessor;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockImporter;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockProcessor;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;

public class NoRewardProtocolScheduleWrapper<C> implements ProtocolSchedule<C> {

  private final Set<String> TOUCH_ACCOUNT_SCHEDULES =
      Set.of("Frontier", "Homestead", "TangerineWhistle");

  private final ProtocolSchedule<C> delegate;

  NoRewardProtocolScheduleWrapper(final ProtocolSchedule<C> delegate) {
    this.delegate = delegate;
  }

  @Override
  public ProtocolSpec<C> getByBlockNumber(final long number) {
    final ProtocolSpec<C> original = delegate.getByBlockNumber(number);
    // Pre Spurious Dragon we need to "touch" the accounts to create a zero-balance version.  We use
    // the maximum possible reward as a sentinel value.
    final Wei blockReward =
        TOUCH_ACCOUNT_SCHEDULES.contains(original.getName()) ? Wei.MAX_WEI : Wei.ZERO;
    final BlockProcessor noRewardBlockProcessor =
        new MainnetBlockProcessor(
            original.getTransactionProcessor(),
            original.getTransactionReceiptFactory(),
            blockReward,
            original.getMiningBeneficiaryCalculator());
    final BlockValidator<C> noRewardBlockValidator =
        new MainnetBlockValidator<>(
            original.getBlockHeaderValidator(),
            original.getBlockBodyValidator(),
            noRewardBlockProcessor);
    final BlockImporter<C> noRewardBlockImporter =
        new MainnetBlockImporter<>(noRewardBlockValidator);
    return new ProtocolSpec<>(
        original.getName(),
        original.getEvm(),
        original.getTransactionValidator(),
        original.getTransactionProcessor(),
        original.getBlockHeaderValidator(),
        original.getOmmerHeaderValidator(),
        original.getBlockBodyValidator(),
        noRewardBlockProcessor,
        noRewardBlockImporter,
        noRewardBlockValidator,
        original.getBlockHeaderFunctions(),
        original.getTransactionReceiptFactory(),
        original.getDifficultyCalculator(),
        blockReward, // block reward
        null, // transaction receipt type; unused
        original.getMiningBeneficiaryCalculator(),
        original.getPrecompileContractRegistry());
  }

  @Override
  public Optional<BigInteger> getChainId() {
    return delegate.getChainId();
  }

  @Override
  public void setTransactionFilter(final TransactionFilter transactionFilter) {
    delegate.setTransactionFilter(transactionFilter);
  }
}
