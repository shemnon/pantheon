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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.BlockParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.DebugAccountRangeAtResult;
import tech.pegasys.pantheon.util.bytes.Bytes32;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;

public class DebugAccountRangeAt extends AbstractBlockParameterMethod {

  final JsonRpcParameter parameters;

  public DebugAccountRangeAt(
      final JsonRpcParameter parameters, final BlockchainQueries blockchainQueries) {
    this(parameters, Suppliers.ofInstance(blockchainQueries));
  }

  public DebugAccountRangeAt(
      final JsonRpcParameter parameters, final Supplier<BlockchainQueries> blockchainQueries) {
    super(blockchainQueries, parameters);
    this.parameters = parameters;
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
    final String addressHash = parameters.required(params, 2, String.class);
    final int maxResults = parameters.required(params, 3, Integer.TYPE);

    final Optional<BlockWithMetadata<Hash, Hash>> block =
        getBlockchainQueries().blockByNumberWithTxHashes(blockNumber);
    if (block.isEmpty()) {
      return new DebugAccountRangeAtResult(Map.of(), Bytes32.ZERO.toUnprefixedString());
    }

    // TODO deal with mid-block locations

    final Optional<MutableWorldState> state = getBlockchainQueries().getWorldState(blockNumber);

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
