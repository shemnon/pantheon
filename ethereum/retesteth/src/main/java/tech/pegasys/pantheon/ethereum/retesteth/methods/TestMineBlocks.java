package tech.pegasys.pantheon.ethereum.retesteth.methods;

import tech.pegasys.pantheon.ethereum.blockcreation.EthHashBlockCreator;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;

public class TestMineBlocks implements JsonRpcMethod {
  private final RetestethContext context;
  private final JsonRpcParameter parameter = new JsonRpcParameter();

  public TestMineBlocks(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "test_mineBlocks";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    long blocksToMine = parameter.required(request.getParams(), 0, Long.class);
    while (blocksToMine-- > 0) {
      if (!context.mineNewBlock()) {
        return new JsonRpcSuccessResponse(request.getId(), false);
      }
    }

    return new JsonRpcSuccessResponse(request.getId(), true);
  }
}
