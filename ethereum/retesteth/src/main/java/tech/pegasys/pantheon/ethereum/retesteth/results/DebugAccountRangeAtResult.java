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
package tech.pegasys.pantheon.ethereum.retesteth.results;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.JsonRpcResult;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonGetter;

public class DebugAccountRangeAtResult implements JsonRpcResult {

  private final Map<String, String> addressMap;
  private final String nextKey;

  public DebugAccountRangeAtResult(final Map<String, String> addressMap, final String nextKey) {
    this.addressMap = addressMap;
    this.nextKey = nextKey;
  }

  @JsonGetter(value = "addressMap")
  public Map<String, String> getAddressMap() {
    return addressMap;
  }

  @JsonGetter(value = "nextKey")
  public String getNextKey() {
    return nextKey;
  }
}
