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
package tech.pegasys.pantheon.ethereum.retesteth.methods;

import static org.junit.Assert.assertEquals;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import org.junit.Test;

public class EthGetTransactionCountTest {

  private final EthGetTransactionCount ethGetTransactionCount =
      new EthGetTransactionCount(RetestethFixtures.context);
  private static final String ACCOUNT_ADDRESS = "0xa94f5374fce5edbc8e2a8697c15331677e6ebf0b";

  @Test
  public void countsLatest() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "1", "eth_getTransactionCount", new Object[] {ACCOUNT_ADDRESS, "latest"});
    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) ethGetTransactionCount.response(request);
    assertEquals("0x20", response.getResult());
  }

  @Test
  public void countsNumber() {
    final JsonRpcRequest request =
        new JsonRpcRequest("2", "eth_getTransactionCount", new Object[] {ACCOUNT_ADDRESS, "28"});
    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) ethGetTransactionCount.response(request);
    assertEquals("0x1c", response.getResult());
  }
}
