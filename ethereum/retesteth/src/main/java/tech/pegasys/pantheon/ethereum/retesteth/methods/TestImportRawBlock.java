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

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.ethereum.rlp.RLPException;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestImportRawBlock implements JsonRpcMethod {
  private static final Logger LOG = LogManager.getLogger();

  private static final String METHOD_NAME = "test_importRawBlock";

  private final RetestethContext runner;
  JsonRpcParameter parameter = new JsonRpcParameter();

  public TestImportRawBlock(final RetestethContext runner) {
    this.runner = runner;
  }

  @Override
  public String getName() {
    return METHOD_NAME;
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final String input = parameter.required(request.getParams(), 0, String.class);
    final ProtocolSpec<Void> protocolSpec = runner.getProtocolSpec(runner.getBlockHeight());
    final ProtocolContext<Void> context = runner.getProtocolContext();

    final Block block;
    try {
      block =
          Block.readFrom(
              RLP.input(BytesValue.fromHexString(input)), protocolSpec.getBlockHeaderFunctions());
    } catch (final RLPException e) {
      LOG.debug("Failed to parse block RLP", e);
      return new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    }

    final BlockImporter<Void> blockImporter = protocolSpec.getBlockImporter();
    final boolean imported =
        blockImporter.importBlock(
            context, block, HeaderValidationMode.NONE, HeaderValidationMode.NONE);

    return new JsonRpcSuccessResponse(request.getId(), imported);
  }
}
