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

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.DebugStorageRangeAtResult;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.DebugStorageRangeAtResult.StorageEntry;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.UInt256;

import org.junit.Test;

public class DebugStorageRangeAtTest {

  // json rpc tests block #16
  private static final String BLOCK_HASH =
      "0x1878c6f27178250f3d55186a2887b076936599f307d96dabcf331b2ff0a38f0c";
  private static final int TRANSACTION_INDEX = 1;
  private static final String ACCOUNT_ADDRESS = "0x6295ee1b4f6dd65047762f924ecd367c17eabf8f";
  private static final String START_KEY_HASH =
      "0x405787fa12a823e0f2b7631cc41b3ba8828b3321ca811111fa75cd3aa3bb5ace"; // SHA3(0x02 @32 bytes)
  private final DebugStorageRangeAt debugStorageRangeAt =
      new DebugStorageRangeAt(RetestethFixtures.context);

  @Test
  public void nameShouldBeDebugStorageRangeAt() {
    assertEquals("debug_storageRangeAt", debugStorageRangeAt.getName());
  }

  @Test
  public void shouldRetrieveStorageRange_partial() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_storageRangeAt",
            new Object[] {BLOCK_HASH, TRANSACTION_INDEX, ACCOUNT_ADDRESS, START_KEY_HASH, 2});

    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugStorageRangeAt.response(request);
    final DebugStorageRangeAtResult result = (DebugStorageRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getStorage())
        .containsOnly(
            entry(
                Hash.hash(Bytes32.fromHexString("0x02")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x02"),
                    UInt256.fromHexString(
                        "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffee"))),
            entry(
                Hash.hash(Bytes32.fromHexString("0x04")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x04"),
                    UInt256.fromHexString(
                        "0xaabbccffffffffffffffffffffffffffffffffffffffffffffffffffffffffee"))));
    assertThat(result.getNextKey()).isEqualTo(Hash.hash(Bytes32.fromHexString("0x01")).toString());
    assertThat(result.getComplete()).isFalse();
  }

  @Test
  public void shouldRetrieveStorageRange_complete() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2.0",
            "debug_storageRangeAt",
            new Object[] {BLOCK_HASH, TRANSACTION_INDEX, ACCOUNT_ADDRESS, "0x00", 100});

    final JsonRpcSuccessResponse response =
        (JsonRpcSuccessResponse) debugStorageRangeAt.response(request);
    final DebugStorageRangeAtResult result = (DebugStorageRangeAtResult) response.getResult();

    assertThat(result).isNotNull();
    assertThat(result.getStorage())
        .containsOnly(
            entry(
                Hash.hash(Bytes32.fromHexString("0x00")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x00"),
                    UInt256.fromHexString(
                        "0x000000000000000000000000000000000000000000000000000000000008fa01"))),
            entry(
                Hash.hash(Bytes32.fromHexString("0x01")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x01"),
                    UInt256.fromHexString(
                        "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffee"))),
            entry(
                Hash.hash(Bytes32.fromHexString("0x02")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x02"),
                    UInt256.fromHexString(
                        "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffee"))),
            entry(
                Hash.hash(Bytes32.fromHexString("0x03")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x03"),
                    UInt256.fromHexString(
                        "0xaabbccffffffffffffffffffffffffffffffffffffffffffffffffffffffffee"))),
            entry(
                Hash.hash(Bytes32.fromHexString("0x04")).toString(),
                new StorageEntry(
                    Bytes32.fromHexString("0x04"),
                    UInt256.fromHexString(
                        "0xaabbccffffffffffffffffffffffffffffffffffffffffffffffffffffffffee"))));
    assertThat(result.getNextKey()).isNull();
    assertThat(result.getComplete()).isTrue();
  }
}
