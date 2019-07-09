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
package tech.pegasys.pantheon.ethereum.vm;

import static org.junit.Assume.assumeTrue;
import static tech.pegasys.pantheon.ethereum.vm.BlockchainReferenceTestTools.executeTest;
import static tech.pegasys.pantheon.ethereum.vm.BlockchainReferenceTestTools.generateTestParametersForConfig;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Select reference tests to "smoke out" general test paths in the main EVM. */
@RunWith(Parameterized.class)
public class BlockchainReferenceTest_smoke {

  private static final String[] TEST_CONFIG_FILE_DIR_PATH =
      new String[] {
        "BlockchainTests/GeneralStateTests/stTimeConsuming/sstore_combinations_initial0_d1061g0v0.json",
        "BlockchainTests/GeneralStateTests/stTransactionTest/UserTransactionZeroCost_d0g0v0.json",
        "BlockchainTests/GeneralStateTests/stExtCodeHash/extCodeHashSelfInInit_d0g0v0.json",
      };

  @Parameters(name = "Name: {0}")
  public static Collection<Object[]> getTestParametersForConfig() {
    return generateTestParametersForConfig(TEST_CONFIG_FILE_DIR_PATH);
  }

  private final String name;
  private final BlockchainReferenceTestCaseSpec spec;

  public BlockchainReferenceTest_smoke(
      final String name, final BlockchainReferenceTestCaseSpec spec, final boolean runTest) {
    this.name = name;
    this.spec = spec;
    assumeTrue("Test was blacklisted", runTest);
  }

  @Test
  public void execution() {
    executeTest(spec);
  }
}
