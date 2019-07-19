package tech.pegasys.pantheon.ethereum.retesteth.methods;

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockWithMetadata;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugAccountRangeAt implements JsonRpcMethod {

  private static final Logger LOG = LogManager.getLogger();

  private final RetestethContext context;
  private final JsonRpcParameter parameters = new JsonRpcParameter();

  public DebugAccountRangeAt(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "debug_accountRangeAt";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Object[] params = request.getParams();
    final String blockHashOrNumber = parameters.required(params, 0, String.class);
    final int txIndex = parameters.required(params, 1, Integer.TYPE);
    final String addressHash = parameters.required(params, 2, String.class);
    final int maxResults = parameters.required(params, 3, Integer.TYPE);

    final UInt256 blockId = UInt256.fromHexString(blockHashOrNumber);
    final Optional<BlockWithMetadata<Hash, Hash>> block;
    if (blockId.fitsLong()) {
      block = context.getBlockchainQueries().blockByNumberWithTxHashes(blockId.toLong());
    } else {
      block =
          context
              .getBlockchainQueries()
              .blockByHashWithTxHashes(Hash.fromHexString(blockHashOrNumber));
    }
    final Hash stateRoot;
    if (txIndex >= block.get().getTransactions().size()) {
      stateRoot = block.get().getHeader().getStateRoot();
    } else {
      stateRoot = null;
      // uggh, we have to replay :(
      // FIXME
    }
    final Optional<MutableWorldState> state =
        context.getProtocolContext().getWorldStateArchive().getMutable(stateRoot);

    // We need to get all the hashed addresses, then sort them to figure out where to start.
    final TreeMap<String, String> sortedAnswers =
        new TreeMap<>(
            state
                .get()
                .streamAccounts()
                .collect(
                    Collectors.toMap(
                        account -> account.getAddressHash().toUnprefixedString(),
                        account1 -> account1.getAddress().toUnprefixedString())));

    final Map<String, String> addressMap = new TreeMap<>();
    int remaining = maxResults;
    String lastKey = addressHash;
    for (final Map.Entry<String, String> entry : sortedAnswers.entrySet()) {
      final String hash = entry.getKey();
      if (remaining < 0) {
        lastKey = hash;
        break;
      }
      if (lastKey.compareTo(hash) <= 0) {
        lastKey = hash;
        addressMap.put(entry.getKey(), entry.getValue());
        remaining--;
      }
    }
    if (remaining > 0) {
      lastKey = Bytes32.ZERO.toUnprefixedString();
    }

    return new JsonRpcSuccessResponse(
        request.getId(), Map.of("addressMap", addressMap, "nextKey", lastKey));
  }
}
