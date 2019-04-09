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
package tech.pegasys.pantheon.consensus.ibft.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.pegasys.pantheon.consensus.ibft.IbftContextBuilder.setupContextWithValidators;
import static tech.pegasys.pantheon.ethereum.core.InMemoryStorageProvider.createInMemoryWorldStateArchive;

import tech.pegasys.pantheon.config.GenesisConfigFile;
import tech.pegasys.pantheon.consensus.ibft.IbftBlockHashing;
import tech.pegasys.pantheon.consensus.ibft.IbftBlockHeaderValidationRulesetFactory;
import tech.pegasys.pantheon.consensus.ibft.IbftContext;
import tech.pegasys.pantheon.consensus.ibft.IbftExtraData;
import tech.pegasys.pantheon.consensus.ibft.IbftProtocolSchedule;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.AddressHelpers;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderTestFixture;
import tech.pegasys.pantheon.ethereum.core.PendingTransactions;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.BlockHeaderValidator;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.testutil.TestClock;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.Test;

public class IbftBlockCreatorTest {
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Test
  public void createdBlockPassesValidationRulesAndHasAppropriateHashAndMixHash() {
    // Construct a parent block.
    final BlockHeaderTestFixture blockHeaderBuilder = new BlockHeaderTestFixture();
    blockHeaderBuilder.gasLimit(5000); // required to pass validation rule checks.
    final BlockHeader parentHeader = blockHeaderBuilder.buildHeader();
    final Optional<BlockHeader> optionalHeader = Optional.of(parentHeader);

    // Construct a block chain and world state
    final MutableBlockchain blockchain = mock(MutableBlockchain.class);
    when(blockchain.getChainHeadHash()).thenReturn(parentHeader.getHash());
    when(blockchain.getBlockHeader(any())).thenReturn(optionalHeader);

    final List<Address> initialValidatorList = Lists.newArrayList();
    for (int i = 0; i < 4; i++) {
      initialValidatorList.add(AddressHelpers.ofValue(i));
    }

    final ProtocolSchedule<IbftContext> protocolSchedule =
        IbftProtocolSchedule.create(
            GenesisConfigFile.fromConfig("{\"config\": {\"spuriousDragonBlock\":0}}")
                .getConfigOptions());
    final ProtocolContext<IbftContext> protContext =
        new ProtocolContext<>(
            blockchain,
            createInMemoryWorldStateArchive(),
            setupContextWithValidators(initialValidatorList));

    final IbftBlockCreator blockCreator =
        new IbftBlockCreator(
            initialValidatorList.get(0),
            parent ->
                new IbftExtraData(
                        BytesValue.wrap(new byte[32]),
                        Collections.emptyList(),
                        Optional.empty(),
                        0,
                        initialValidatorList)
                    .encode(),
            new PendingTransactions(1, TestClock.fixed(), metricsSystem),
            protContext,
            protocolSchedule,
            parentGasLimit -> parentGasLimit,
            Wei.ZERO,
            parentHeader);

    final Block block = blockCreator.createBlock(Instant.now().getEpochSecond());

    final BlockHeaderValidator<IbftContext> rules =
        IbftBlockHeaderValidationRulesetFactory.ibftBlockHeaderValidator(0);

    // NOTE: The header will not contain commit seals, so can only do light validation on header.
    final boolean validationResult =
        rules.validateHeader(
            block.getHeader(), parentHeader, protContext, HeaderValidationMode.LIGHT);

    assertThat(validationResult).isTrue();

    final BlockHeader header = block.getHeader();
    final IbftExtraData extraData = IbftExtraData.decode(header.getExtraData());
    assertThat(block.getHash())
        .isEqualTo(IbftBlockHashing.calculateDataHashForCommittedSeal(header, extraData));
  }
}
