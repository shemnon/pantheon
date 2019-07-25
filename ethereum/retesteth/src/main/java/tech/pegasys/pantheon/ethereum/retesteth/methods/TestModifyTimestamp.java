package tech.pegasys.pantheon.ethereum.retesteth.methods;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;

public class TestModifyTimestamp implements JsonRpcMethod {

  private final RetestethContext context;
  private final JsonRpcParameter parameter = new JsonRpcParameter();

  public TestModifyTimestamp(final RetestethContext context) {
    this.context = context;
  }

  @Override
  public String getName() {
    return "test_modifyTimestamp";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final long epochSeconds = parameter.required(request.getParams(), 0, Long.class);
    context.setTimestamp(epochSeconds);
    return new JsonRpcSuccessResponse(request.getId(), true);
  }
}
