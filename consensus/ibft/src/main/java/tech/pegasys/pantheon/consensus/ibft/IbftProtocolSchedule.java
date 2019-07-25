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
package tech.pegasys.pantheon.consensus.ibft;

import static tech.pegasys.pantheon.consensus.ibft.IbftBlockHeaderValidationRulesetFactory.ibftBlockHeaderValidator;

import tech.pegasys.pantheon.config.GenesisConfigOptions;
import tech.pegasys.pantheon.config.IbftConfigOptions;
import tech.pegasys.pantheon.ethereum.MainnetBlockValidator;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockBodyValidator;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockImporter;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolScheduleBuilder;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpecBuilder;

import java.math.BigInteger;
import java.time.Clock;

/** Defines the protocol behaviours for a blockchain using IBFT. */
public class IbftProtocolSchedule {

  private static final BigInteger DEFAULT_CHAIN_ID = BigInteger.ONE;

  public static ProtocolSchedule<IbftContext> create(
      final GenesisConfigOptions config,
      final PrivacyParameters privacyParameters,
      final boolean isRevertReasonEnabled,
      final Clock clock) {
    final IbftConfigOptions ibftConfig = config.getIbftLegacyConfigOptions();
    final long blockPeriod = ibftConfig.getBlockPeriodSeconds();

    return new ProtocolScheduleBuilder<>(
            config,
            DEFAULT_CHAIN_ID,
            builder -> applyIbftChanges(blockPeriod, builder, clock),
            privacyParameters,
            isRevertReasonEnabled,
            clock)
        .createProtocolSchedule();
  }

  public static ProtocolSchedule<IbftContext> create(
      final GenesisConfigOptions config, final boolean isRevertReasonEnabled, final Clock clock) {
    return create(config, PrivacyParameters.DEFAULT, isRevertReasonEnabled, clock);
  }

  public static ProtocolSchedule<IbftContext> create(
      final GenesisConfigOptions config, final Clock clock) {
    return create(config, PrivacyParameters.DEFAULT, false, clock);
  }

  private static ProtocolSpecBuilder<IbftContext> applyIbftChanges(
      final long secondsBetweenBlocks, final ProtocolSpecBuilder<Void> builder, final Clock clock) {
    return builder
        .<IbftContext>changeConsensusContextType(
            difficultyCalculator -> ibftBlockHeaderValidator(secondsBetweenBlocks, clock),
            difficultyCalculator -> ibftBlockHeaderValidator(secondsBetweenBlocks, clock),
            MainnetBlockBodyValidator::new,
            MainnetBlockValidator::new,
            MainnetBlockImporter::new,
            (time, parent, protocolContext) -> BigInteger.ONE)
        .blockReward(Wei.ZERO)
        .blockHeaderFunctions(IbftBlockHeaderFunctions.forOnChainBlock());
  }
}
