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
package tech.pegasys.pantheon.tests.acceptance.dsl.condition.eea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import tech.pegasys.pantheon.tests.acceptance.dsl.jsonrpc.Eea;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea.EeaRequestFactory.PrivateTransactionReceipt;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea.EeaTransactions;

public class ExpectValidPrivateContractDeployedReceipt extends GetValidPrivateTransactionReceipt {

  private final String contractAddress;

  public ExpectValidPrivateContractDeployedReceipt(
      final String contractAddress, final Eea eea, final EeaTransactions transactions) {
    super(eea, transactions);
    this.contractAddress = contractAddress;
  }

  @Override
  public void verify(final PantheonNode node, final String transactionHash) {
    final PrivateTransactionReceipt privateTxReceipt =
        getPrivateTransactionReceipt(node, transactionHash);

    assertEquals(contractAddress, privateTxReceipt.getContractAddress());
    assertNotEquals("0x", privateTxReceipt.getOutput());
  }
}
