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

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.DebugStorageRangeAtResult;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;
import tech.pegasys.pantheon.ethereum.worldstate.DebuggableMutableWorldState;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.NavigableMap;
import java.util.Optional;

import io.vertx.core.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugStorageRangeAt implements JsonRpcMethod {

  private static final Logger LOG = LogManager.getLogger();

  private final JsonRpcParameter parameters = new JsonRpcParameter();
  RetestethContext context;

  public DebugStorageRangeAt(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return RpcMethod.DEBUG_STORAGE_RANGE_AT.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final String blockHashOrNumber = parameters.required(request.getParams(), 0, String.class);
    final int transactionIndex = parameters.required(request.getParams(), 1, Integer.class);
    final Address accountAddress = parameters.required(request.getParams(), 2, Address.class);
    final Hash startKey =
        Hash.fromHexStringLenient(parameters.required(request.getParams(), 3, String.class));
    final int limit = parameters.required(request.getParams(), 4, Integer.class);

    final UInt256 blockId = UInt256.fromHexString(blockHashOrNumber);
    //    final Optional<TransactionWithMetadata> optional;
    final Optional<Hash> blockHash;
    if (blockId.fitsLong()) {
      blockHash = context.getBlockchainQueries().getBlockHashByNumber(blockId.toLong());
    } else {
      blockHash = Optional.of(Hash.fromHexStringLenient(blockHashOrNumber));
    }

    if (blockHash.isPresent()) {
      if (context.getBlockchainQueries().getTransactionCount(blockHash.get()) >= transactionIndex) {
        // post block state
      } else {
        // sub-block state
      }
    }

    return extractStorageAt(
        request,
        accountAddress,
        startKey,
        limit,
        context
            .getProtocolContext()
            .getWorldStateArchive()
            .getMutable(
                context
                    .getBlockchainQueries()
                    .blockByHash(blockHash.get())
                    .get()
                    .getHeader()
                    .getStateRoot())
            .get());
    //    blockHash.map(blockHash -> context.getBlockReplay());
    //      Optional<BlockWithMetadata<Hash, Hash>> block =
    // context.getBlockchainQueries().blockByNumberWithTxHashes(blockId.toLong());
    //      if (block.isPresent()) {
    //        blockHash = block.get().getHeader().getHash();
    //        optional =
    //            context
    //                .getBlockchainQueries()
    //                .transactionByBlockNumberAndIndex(blockId.toLong(), transactionIndex);
    //      } else {
    //        optional = Optional.empty();
    //      }
    //    } else {
    //      optional =
    //          context
    //              .getBlockchainQueries()
    //              .transactionByBlockHashAndIndex(
    //                  Hash.fromHexString(blockHashOrNumber), transactionIndex);
    //      blockHash = optional.map(tx -> tx.getBlockHash()).orElse(null);
    //    }
    //    return optional
    //        .map(
    //            transactionWithMetadata ->
    //                (context
    //                    .getBlockReplay()
    //                    .afterTransactionInBlock(
    //                        blockHash,
    //                        transactionWithMetadata.getTransaction().hash(),
    //                        (transaction, blockHeader, blockchain, worldState,
    // transactionProcessor) ->
    //                            extractStorageAt(request, accountAddress, startKey, limit,
    // worldState))
    //                    .orElseGet(() -> new JsonRpcSuccessResponse(request.getId(), null))))
    //        .orElseGet(() -> new JsonRpcSuccessResponse(request.getId(), null));
  }

  private JsonRpcSuccessResponse extractStorageAt(
      final JsonRpcRequest request,
      final Address accountAddress,
      final Hash startKey,
      final int limit,
      final MutableWorldState worldState) {
    final Account account = worldState.get(accountAddress);
    final NavigableMap<Bytes32, UInt256> entries = account.storageEntriesFrom(startKey, limit + 1);

    Bytes32 nextKey = null;
    if (entries.size() == limit + 1) {
      nextKey = entries.lastKey();
      entries.remove(nextKey);
    }
    JsonRpcSuccessResponse result = new JsonRpcSuccessResponse(
        request.getId(),
        new DebugStorageRangeAtResult(
            entries, nextKey, ((DebuggableMutableWorldState) worldState).getPreimages()));
    LOG.info(Json.encodePrettily(result));
    return result;
  }
}
