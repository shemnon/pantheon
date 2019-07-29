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
package tech.pegasys.pantheon.ethereum.retesteth;

import tech.pegasys.pantheon.ethereum.jsonrpc.JsonRpcConfiguration;
import tech.pegasys.pantheon.ethereum.jsonrpc.JsonRpcHttpService;
import tech.pegasys.pantheon.ethereum.jsonrpc.JsonRpcMethodsFactory;
import tech.pegasys.pantheon.ethereum.jsonrpc.health.HealthService;
import tech.pegasys.pantheon.ethereum.jsonrpc.health.LivenessCheck;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.Web3ClientVersion;
import tech.pegasys.pantheon.ethereum.retesteth.methods.DebugAccountRangeAt;
import tech.pegasys.pantheon.ethereum.retesteth.methods.DebugStorageRangeAt;
import tech.pegasys.pantheon.ethereum.retesteth.methods.EthBlockNumber;
import tech.pegasys.pantheon.ethereum.retesteth.methods.EthGetBalance;
import tech.pegasys.pantheon.ethereum.retesteth.methods.EthGetBlockByNumber;
import tech.pegasys.pantheon.ethereum.retesteth.methods.EthGetCode;
import tech.pegasys.pantheon.ethereum.retesteth.methods.EthGetTransactionCount;
import tech.pegasys.pantheon.ethereum.retesteth.methods.EthSendRawTransaction;
import tech.pegasys.pantheon.ethereum.retesteth.methods.TestGetLogHash;
import tech.pegasys.pantheon.ethereum.retesteth.methods.TestImportRawBlock;
import tech.pegasys.pantheon.ethereum.retesteth.methods.TestMineBlocks;
import tech.pegasys.pantheon.ethereum.retesteth.methods.TestModifyTimestamp;
import tech.pegasys.pantheon.ethereum.retesteth.methods.TestRewindToBlock;
import tech.pegasys.pantheon.ethereum.retesteth.methods.TestSetChainParams;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.vertx.core.Vertx;

public class RetestethService {

  private final JsonRpcHttpService jsonRpcHttpService;
  private final Vertx vertx;

  private final RetestethContext retestethContext;

  public RetestethService(
      final String clientVersion,
      final RetestethConfiguration retestethConfiguration,
      final JsonRpcConfiguration jsonRpcConfiguration) {
    vertx = Vertx.vertx();
    retestethContext = new RetestethContext();

    final Map<String, JsonRpcMethod> jsonRpcMethods = new HashMap<>();
    JsonRpcMethodsFactory.addMethods(
        jsonRpcMethods,
        new Web3ClientVersion(clientVersion),
        new TestSetChainParams(retestethContext),
        new TestImportRawBlock(retestethContext),
        new EthBlockNumber(retestethContext),
        new EthGetBlockByNumber(retestethContext),
        new DebugAccountRangeAt(retestethContext),
        new EthGetBalance(retestethContext),
        new EthGetCode(retestethContext),
        new EthGetTransactionCount(retestethContext),
        new DebugStorageRangeAt(retestethContext),
        new TestModifyTimestamp(retestethContext),
        new EthSendRawTransaction(retestethContext),
        new TestMineBlocks(retestethContext),
        new TestGetLogHash(retestethContext),
        new TestRewindToBlock(retestethContext));

    jsonRpcHttpService =
        new JsonRpcHttpService(
            vertx,
            retestethConfiguration.getDataPath(),
            jsonRpcConfiguration,
            new NoOpMetricsSystem(),
            Optional.empty(),
            jsonRpcMethods,
            new HealthService(new LivenessCheck()),
            HealthService.ALWAYS_HEALTHY);
  }

  public void start() {
    jsonRpcHttpService.start();
  }

  public void close() {
    stop();
  }

  public void stop() {
    jsonRpcHttpService.stop();
  }
}
