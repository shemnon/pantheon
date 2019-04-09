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
package tech.pegasys.pantheon.ethereum.blockcreation;

import tech.pegasys.pantheon.config.GenesisConfigFile;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.ExecutionContextTestFixture;
import tech.pegasys.pantheon.ethereum.core.PendingTransactions;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.EthHashSolver;
import tech.pegasys.pantheon.ethereum.mainnet.EthHasher.Light;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolScheduleBuilder;
import tech.pegasys.pantheon.ethereum.mainnet.ValidationTestUtils;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.testutil.TestClock;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.IOException;
import java.util.function.Function;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class EthHashBlockCreatorTest {

  private final Address BLOCK_1_COINBASE =
      Address.fromHexString("0x05a56e2d52c817161883f50c441c3228cfe54d9f");

  private static final long BLOCK_1_TIMESTAMP = Long.parseUnsignedLong("55ba4224", 16);

  private static final long BLOCK_1_NONCE = Long.parseLong("539bd4979fef1ec4", 16);

  private static final BytesValue BLOCK_1_EXTRA_DATA =
      BytesValue.fromHexString("0x476574682f76312e302e302f6c696e75782f676f312e342e32");
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  private final ExecutionContextTestFixture executionContextTestFixture =
      ExecutionContextTestFixture.builder()
          .protocolSchedule(
              new ProtocolScheduleBuilder<>(
                      GenesisConfigFile.DEFAULT.getConfigOptions(),
                      42,
                      Function.identity(),
                      PrivacyParameters.DEFAULT)
                  .createProtocolSchedule())
          .build();

  @Test
  public void createMainnetBlock1() throws IOException {
    final EthHashSolver solver = new EthHashSolver(Lists.newArrayList(BLOCK_1_NONCE), new Light());
    final EthHashBlockCreator blockCreator =
        new EthHashBlockCreator(
            BLOCK_1_COINBASE,
            parent -> BLOCK_1_EXTRA_DATA,
            new PendingTransactions(1, TestClock.fixed(), metricsSystem),
            executionContextTestFixture.getProtocolContext(),
            executionContextTestFixture.getProtocolSchedule(),
            gasLimit -> gasLimit,
            solver,
            Wei.ZERO,
            executionContextTestFixture.getBlockchain().getChainHeadHeader());

    // A Hashrate should not exist in the block creator prior to creating a block
    Assertions.assertThat(blockCreator.getHashesPerSecond().isPresent()).isFalse();

    final Block actualBlock = blockCreator.createBlock(BLOCK_1_TIMESTAMP);
    final Block expectedBlock = ValidationTestUtils.readBlock(1);

    Assertions.assertThat(actualBlock).isEqualTo(expectedBlock);
    Assertions.assertThat(blockCreator.getHashesPerSecond().isPresent()).isTrue();
  }
}
