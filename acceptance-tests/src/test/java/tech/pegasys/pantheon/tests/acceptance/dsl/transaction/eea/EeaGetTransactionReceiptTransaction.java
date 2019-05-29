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
package tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.NodeRequests;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.Transaction;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea.EeaRequestFactory.PrivateTransactionReceipt;
import tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea.EeaRequestFactory.PrivateTransactionReceiptResponse;

import java.io.IOException;

public class EeaGetTransactionReceiptTransaction implements Transaction<PrivateTransactionReceipt> {

  private final String txHash;

  public EeaGetTransactionReceiptTransaction(final String txHash) {
    this.txHash = txHash;
  }

  @Override
  public PrivateTransactionReceipt execute(final NodeRequests node) {
    try {
      final PrivateTransactionReceiptResponse result =
          node.eea().eeaGetTransactionReceipt(txHash).send();
      assertThat(result).isNotNull();
      assertThat(result.hasError()).isFalse();
      return result.getResult();
    } catch (IOException e) {
      return null;
    }
  }
}
