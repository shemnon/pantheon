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

import tech.pegasys.pantheon.ethereum.core.Account;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    if (block.isEmpty()) {
      return new JsonRpcSuccessResponse(request.getId(), Map.of());
    }

    // TODO deal with mid-block locations

    final Hash stateRoot = block.get().getHeader().getStateRoot();
    final Optional<MutableWorldState> state =
        getProtocolContext().getWorldStateArchive().getMutable(stateRoot);

    if (state.isEmpty()) {
      return new DebugAccountRangeAtResult(Map.of(), Bytes32.ZERO.toUnprefixedString());
    } else {
      final List<Account> accounts =
          state
              .get()
              .streamAccounts(Bytes32.fromHexStringLenient(addressHash), maxResults + 1)
              .collect(Collectors.toList());
      Bytes32 nextKey = Bytes32.ZERO;
      if (accounts.size() == maxResults + 1) {
        nextKey = accounts.get(maxResults).getAddressHash();
        accounts.remove(maxResults);
      }

      return new DebugAccountRangeAtResult(
          accounts.stream()
              .collect(
                  Collectors.toMap(
                      account -> account.getAddressHash().toUnprefixedString(),
                      account -> account.getAddress().toUnprefixedString())),
          nextKey.toUnprefixedString());
    }
  }
}
