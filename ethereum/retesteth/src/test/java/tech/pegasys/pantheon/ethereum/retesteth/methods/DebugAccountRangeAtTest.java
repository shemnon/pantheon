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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertEquals;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.retesteth.results.DebugAccountRangeAtResult;
import tech.pegasys.pantheon.util.bytes.Bytes32;

import org.junit.Test;

public class DebugAccountRangeAtTest {

  // json rpc tests block #16
  private static final String BLOCK_HASH =
      "0x1878c6f27178250f3d55186a2887b076936599f307d96dabcf331b2ff0a38f0c";
  private static final int TRANSACTION_INDEX = 1;

  private static final Address ACCOUNT_1 =
      Address.fromHexString("a94f5374fce5edbc8e2a8697c15331677e6ebf0b");
  private static final Address ACCOUNT_2 =
      Address.fromHexString("bcde5374fce5edbc8e2a8697c15331677e6ebf0b");
  private static final Address ACCOUNT_3 =
      Address.fromHexString("8888f1f195afa192cfee860698584c030f4c9db1");
  private static final Address ACCOUNT_4 =
      Address.fromHexString("6295ee1b4f6dd65047762f924ecd367c17eabf8f");

  private final DebugAccountRangeAt debugAccountRangeAt =
      new DebugAccountRangeAt(RetestethFixtures.context);

  @Test
  public void nameShouldBeDebugStorageRangeAt() {
    assertEquals("debug_accountRangeAt", debugAccountRangeAt.getName());
  }

  @Test
  public void shouldRetrieveStorageRange_partialByBlockHash() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_accountRangeAt",
            new Object[] {
              BLOCK_HASH, TRANSACTION_INDEX, Hash.hash(ACCOUNT_2).toUnprefixedString(), 2
            });

    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugAccountRangeAt.response(request);
    final DebugAccountRangeAtResult result = (DebugAccountRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getAddressMap())
        .containsOnly(
            entry(Hash.hash(ACCOUNT_2).toUnprefixedString(), ACCOUNT_2.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_3).toUnprefixedString(), ACCOUNT_3.toUnprefixedString()));
    assertThat(result.getNextKey()).isEqualTo(Hash.hash(ACCOUNT_4).toUnprefixedString());
  }

  @Test
  public void shouldRetrieveStorageRange_completeByBlockHash() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_storageRangeAt",
            new Object[] {
              BLOCK_HASH, TRANSACTION_INDEX, Bytes32.fromHexString("0x00").toUnprefixedString(), 100
            });

    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugAccountRangeAt.response(request);
    final DebugAccountRangeAtResult result = (DebugAccountRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getAddressMap())
        .containsOnly(
            entry(Hash.hash(ACCOUNT_1).toUnprefixedString(), ACCOUNT_1.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_2).toUnprefixedString(), ACCOUNT_2.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_3).toUnprefixedString(), ACCOUNT_3.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_4).toUnprefixedString(), ACCOUNT_4.toUnprefixedString()));
    assertThat(result.getNextKey()).isEqualTo(Bytes32.ZERO.toUnprefixedString());
  }

  @Test
  public void shouldRetrieveStorageRange_partialByBlockNumber() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_accountRangeAt",
            new Object[] {"0x10", TRANSACTION_INDEX, Hash.hash(ACCOUNT_2).toUnprefixedString(), 2});

    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugAccountRangeAt.response(request);
    final DebugAccountRangeAtResult result = (DebugAccountRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getAddressMap())
        .containsOnly(
            entry(Hash.hash(ACCOUNT_2).toUnprefixedString(), ACCOUNT_2.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_3).toUnprefixedString(), ACCOUNT_3.toUnprefixedString()));
    assertThat(result.getNextKey()).isEqualTo(Hash.hash(ACCOUNT_4).toUnprefixedString());
  }

  @Test
  public void shouldRetrieveStorageRange_completeByBlockNumber() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_storageRangeAt",
            new Object[] {
              "0x10", TRANSACTION_INDEX, Bytes32.fromHexString("0x00").toUnprefixedString(), 100
            });

    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugAccountRangeAt.response(request);
    final DebugAccountRangeAtResult result = (DebugAccountRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getAddressMap())
        .containsOnly(
            entry(Hash.hash(ACCOUNT_1).toUnprefixedString(), ACCOUNT_1.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_2).toUnprefixedString(), ACCOUNT_2.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_3).toUnprefixedString(), ACCOUNT_3.toUnprefixedString()),
            entry(Hash.hash(ACCOUNT_4).toUnprefixedString(), ACCOUNT_4.toUnprefixedString()));
    assertThat(result.getNextKey()).isEqualTo(Bytes32.ZERO.toUnprefixedString());
  }
}
