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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.privacy;

import static org.apache.logging.log4j.LogManager.getLogger;

import tech.pegasys.pantheon.enclave.Enclave;
import tech.pegasys.pantheon.enclave.types.CreatePrivacyGroupRequest;
import tech.pegasys.pantheon.enclave.types.PrivacyGroup;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import org.apache.logging.log4j.Logger;

public class EeaCreatePrivacyGroup implements JsonRpcMethod {

  private static final Logger LOG = getLogger();
  private final Enclave enclave;
  private final JsonRpcParameter parameters;

  public EeaCreatePrivacyGroup(final Enclave enclave, final JsonRpcParameter parameters) {
    this.enclave = enclave;
    this.parameters = parameters;
  }

  @Override
  public String getName() {
    return RpcMethod.EEA_CREATE_PRIVACY_GROUP.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    LOG.trace("Executing {}", RpcMethod.EEA_CREATE_PRIVACY_GROUP.getMethodName());

    final String from = parameters.required(request.getParams(), 0, String.class);
    final String name = parameters.required(request.getParams(), 1, String.class);
    final String description = parameters.required(request.getParams(), 2, String.class);
    final String[] addresses = parameters.required(request.getParams(), 3, String[].class);

    LOG.trace("Creating a privacy group with name {} and description {}", name, description);

    CreatePrivacyGroupRequest createPrivacyGroupRequest =
        new CreatePrivacyGroupRequest(addresses, from, name, description);
    PrivacyGroup response;
    try {
      response = enclave.createPrivacyGroup(createPrivacyGroupRequest);
    } catch (Exception e) {
      LOG.error("Failed to fetch transaction from Enclave with error " + e.getMessage());
      LOG.error(e);
      return new JsonRpcSuccessResponse(request.getId(), JsonRpcError.CREATE_PRIVACY_GROUP_ERROR);
    }
    return new JsonRpcSuccessResponse(request.getId(), response.getPrivacyGroupId());
  }
}
