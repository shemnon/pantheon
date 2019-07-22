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

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.BlockParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.TransactionWithMetadata;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.TransactionCompleteResult;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.TransactionHashResult;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.TransactionResult;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;
import tech.pegasys.pantheon.ethereum.retesteth.results.BlockResult;

import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public abstract class AbstractBlockParameterMethod implements JsonRpcMethod {

  private final RetestethContext context;

  AbstractBlockParameterMethod(final RetestethContext context) {
    this.context = context;
  }

  protected abstract BlockParameter blockParameter(JsonRpcRequest request);

  protected abstract Object resultByBlockNumber(JsonRpcRequest request, long blockNumber);

  BlockchainQueries getBlockchainQueries() {
    return context.getBlockchainQueries();
  }

  ProtocolContext<Void> getProtocolContext() {
    return context.getProtocolContext();
  }

  private Object pendingResult() {
    throw new UnsupportedOperationException("Retesteth mode does not support mining");
  }

  private Object latestResult(final JsonRpcRequest request) {
    return resultByBlockNumber(request, getBlockchainQueries().headBlockNumber());
  }

  private Object findResultByParamType(final JsonRpcRequest request) {
    final BlockParameter blockParam = blockParameter(request);

    final Object result;
    final OptionalLong blockNumber = blockParam.getNumber();
    if (blockNumber.isPresent()) {
      result = resultByBlockNumber(request, blockNumber.getAsLong());
    } else if (blockParam.isLatest()) {
      result = latestResult(request);
    } else {
      // If block parameter is not numeric or latest, it is pending.
      result = pendingResult();
    }

    return result;
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    return new JsonRpcSuccessResponse(request.getId(), findResultByParamType(request));
  }

  static BlockResult transactionComplete(
      final BlockWithMetadata<TransactionWithMetadata, Hash> blockWithMetadata) {
    final List<TransactionResult> txs =
        blockWithMetadata.getTransactions().stream()
            .map(TransactionCompleteResult::new)
            .collect(Collectors.toList());
    final List<JsonNode> ommers =
        blockWithMetadata.getOmmers().stream()
            .map(Hash::toString)
            .map(TextNode::new)
            .collect(Collectors.toList());
    return new BlockResult(
        blockWithMetadata.getHeader(),
        txs,
        ommers,
        blockWithMetadata.getTotalDifficulty(),
        blockWithMetadata.getSize());
  }

  public static BlockResult transactionHash(final BlockWithMetadata<Hash, Hash> blockWithMetadata) {
    final List<TransactionResult> txs =
        blockWithMetadata.getTransactions().stream()
            .map(Hash::toString)
            .map(TransactionHashResult::new)
            .collect(Collectors.toList());
    final List<JsonNode> ommers =
        blockWithMetadata.getOmmers().stream()
            .map(Hash::toString)
            .map(TextNode::new)
            .collect(Collectors.toList());
    return new BlockResult(
        blockWithMetadata.getHeader(),
        txs,
        ommers,
        blockWithMetadata.getTotalDifficulty(),
        blockWithMetadata.getSize());
  }
}
