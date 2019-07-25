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
package tech.pegasys.pantheon.ethereum.jsonrpc;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsonRpcEnclaveErrorConverter {

  public static JsonRpcError convertEnclaveInvalidReason(final String reason) {

    List<JsonRpcError> err =
        Arrays.stream(JsonRpcError.values())
            .filter(e -> e.getMessage().contains(reason))
            .collect(Collectors.toList());

    if (err.size() == 1) {
      return err.get(0);
    } else {
      return JsonRpcError.ENCLAVE_ERROR;
    }
  }
}
