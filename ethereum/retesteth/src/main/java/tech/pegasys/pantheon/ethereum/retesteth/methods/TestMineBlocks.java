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
package tech.pegasys.pantheon.ethereum.retesteth.methods;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;

public class TestMineBlocks implements JsonRpcMethod {
  private final RetestethContext context;
  private final JsonRpcParameter parameters;

  public TestMineBlocks(final RetestethContext context, final JsonRpcParameter parameters) {
    this.context = context;
    this.parameters = parameters;
  }

  @Override
  public String getName() {
    return "test_mineBlocks";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    long blocksToMine = parameters.required(request.getParams(), 0, Long.class);
    while (blocksToMine-- > 0) {
      if (!context.mineNewBlock()) {
        return new JsonRpcSuccessResponse(request.getId(), false);
      }
    }

    return new JsonRpcSuccessResponse(request.getId(), true);
  }
}
