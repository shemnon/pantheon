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

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.BlockParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;
import tech.pegasys.pantheon.ethereum.retesteth.results.DebugAccountRangeAtResult;
import tech.pegasys.pantheon.util.bytes.Bytes32;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class DebugAccountRangeAt extends AbstractBlockParameterMethod {

  private final JsonRpcParameter parameters = new JsonRpcParameter();

  public DebugAccountRangeAt(final RetestethContext context) {
    super(context);
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequest request) {
    return parameters.required(request.getParams(), 0, BlockParameter.class);
  }

  @Override
  public String getName() {
    return "debug_accountRangeAt";
  }

  @Override
  protected Object resultByBlockNumber(final JsonRpcRequest request, final long blockNumber) {
    final Object[] params = request.getParams();
    final int txIndex = parameters.required(params, 1, Integer.TYPE);
    final String addressHash = parameters.required(params, 2, String.class);
    final int maxResults = parameters.required(params, 3, Integer.TYPE);

    final Optional<BlockWithMetadata<Hash, Hash>> block =
        getBlockchainQueries().blockByNumberWithTxHashes(blockNumber);
    final Hash stateRoot;
    if (block.isEmpty()) {
      return new JsonRpcSuccessResponse(request.getId(), Map.of());
    }
    if (txIndex >= block.get().getTransactions().size()) {
      stateRoot = block.get().getHeader().getStateRoot();
    } else {
      stateRoot = null;
      // uggh, we have to replay :(
      // FIXME
    }
    final Optional<MutableWorldState> state =
        getProtocolContext().getWorldStateArchive().getMutable(stateRoot);

    // We need to get all the hashed addresses, then sort them to figure out where to start.
    final TreeMap<String, String> sortedAnswers =
        new TreeMap<>(
            state
                .get()
                .streamAccounts()
                .collect(
                    Collectors.toMap(
                        account -> account.getAddressHash().toUnprefixedString(),
                        account -> account.getAddress().toUnprefixedString())));

    int remaining = maxResults;
    final Map<String, String> addressMap = new TreeMap<>();
    final Iterator<Map.Entry<String, String>> addressIter = sortedAnswers.entrySet().iterator();
    while (addressIter.hasNext() && remaining > 0) {
      final Map.Entry<String, String> entry = addressIter.next();
      if (entry.getKey().compareTo(addressHash) >= 0) {
        addressMap.put(entry.getKey(), entry.getValue());
        remaining--;
      }
    }
    final String nextKey =
        addressIter.hasNext() ? addressIter.next().getKey() : Bytes32.ZERO.toUnprefixedString();

    return new DebugAccountRangeAtResult(addressMap, nextKey);
  }
}
