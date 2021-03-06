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
package tech.pegasys.pantheon.ethereum.api.jsonrpc.internal.methods;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;

import org.junit.Test;

public class Web3Sha3Test {

  private final Web3Sha3 method = new Web3Sha3();

  @Test
  public void shouldReturnCorrectMethodName() {
    assertThat(method.getName()).isEqualTo("web3_sha3");
  }

  @Test
  public void shouldReturnCorrectResult() {
    final JsonRpcRequest request =
        new JsonRpcRequest("2", "web3_sha3", new Object[] {"0x68656c6c6f20776f726c64"});

    final JsonRpcResponse expected =
        new JsonRpcSuccessResponse(
            request.getId(), "0x47173285a8d7341e5e972fc677286384f802f8ef42a5ec5f03bbfa254cb01fad");
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnEmptyStringResult() {
    final JsonRpcRequest request = new JsonRpcRequest("2", "web3_sha3", new Object[] {""});

    final JsonRpcResponse expected =
        new JsonRpcSuccessResponse(
            request.getId(), "0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470");
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnErrorOnOddLengthParam() {
    final JsonRpcRequest request =
        new JsonRpcRequest("2", "web3_sha3", new Object[] {"0x68656c6c6f20776f726c6"});

    final JsonRpcResponse expected =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnErrorOnNonHexParam() {
    final JsonRpcRequest request =
        new JsonRpcRequest("2", "web3_sha3", new Object[] {"0x68656c6c6fThisIsNotHex"});

    final JsonRpcResponse expected =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnErrorOnNoPrefixParam() {
    final JsonRpcRequest request =
        new JsonRpcRequest("2", "web3_sha3", new Object[] {"68656c6c6f20776f726c64"});

    final JsonRpcResponse expected =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnErrorOnNoPrefixNonHexParam() {
    final JsonRpcRequest request =
        new JsonRpcRequest("2", "web3_sha3", new Object[] {"68656c6c6fThisIsNotHex"});

    final JsonRpcResponse expected =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnErrorOnExtraParam() {
    final JsonRpcRequest request =
        new JsonRpcRequest(
            "2", "web3_sha3", new Object[] {"0x68656c6c6f20776f726c64", "{encode:'hex'}"});

    final JsonRpcResponse expected =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void shouldReturnErrorOnNoParam() {
    final JsonRpcRequest request = new JsonRpcRequest("2", "web3_sha3", new Object[] {});

    final JsonRpcResponse expected =
        new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    final JsonRpcResponse actual = method.response(request);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }
}
