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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.crypto.Hash;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public class Web3Sha3 implements JsonRpcMethod {

  public Web3Sha3() {}

  @Override
  public String getName() {
    return RpcMethod.WEB3_SHA3.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest req) {
    if (req.getParamLength() != 1) {
      // Do we want custom messages for each different type of invalid params?
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.INVALID_PARAMS);
    }

    final String data = req.getParams()[0].toString();

    if (!data.isEmpty() && !data.startsWith("0x")) {
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.INVALID_PARAMS);
    }

    try {
      final BytesValue byteData = BytesValue.fromHexString(data);
      return new JsonRpcSuccessResponse(req.getId(), Hash.keccak256(byteData).toString());
    } catch (final IllegalArgumentException err) {
      return new JsonRpcErrorResponse(req.getId(), JsonRpcError.INVALID_PARAMS);
    }
  }
}
