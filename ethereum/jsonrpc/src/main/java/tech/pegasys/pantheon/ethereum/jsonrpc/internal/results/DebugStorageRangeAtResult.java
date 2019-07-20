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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results;

import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.MoreObjects;

public class DebugStorageRangeAtResult implements JsonRpcResult {

  private final Map<String, StorageEntry> storage = new HashMap<>();
  private final String nextKey;

  public DebugStorageRangeAtResult(
      final Map<Bytes32, UInt256> entries,
      final Bytes32 nextKey,
      final Map<BytesValue, BytesValue> preimages) {
    entries.forEach(
        (keyHash, value) ->
            storage.put(keyHash.toString(), new StorageEntry(preimages.get(keyHash), value)));
    this.nextKey = nextKey != null ? nextKey.toString() : null;
  }

  @JsonGetter(value = "storage")
  public Map<String, StorageEntry> getStorage() {
    return storage;
  }

  @JsonGetter(value = "nextKey")
  public String getNextKey() {
    return nextKey;
  }

  @JsonGetter(value = "complete")
  public boolean getComplete() {
    return nextKey == null;
  }

  @JsonPropertyOrder(value = {"key", "value"})
  public static class StorageEntry {
    private final String key;
    private final String value;

    public StorageEntry(final BytesValue key, final UInt256 value) {
      this.key = key == null ? null : strictShortHex(key.toString());
      this.value = strictShortHex(value.toHexString());
    }

    String strictShortHex(final String hexString) {
      String workString = hexString;
      if (workString.startsWith(("0x"))) {
        workString = workString.substring(2);
      }
      while (workString.startsWith("00")) {
        workString = workString.substring(2);
      }
      if (workString.length() == 0) {
        workString = "00";
      }
      return "0x" + workString;
    }

    @JsonGetter(value = "key")
    public String getKey() {
      return key;
    }

    @JsonGetter(value = "value")
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("key", key).add("value", value).toString();
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final StorageEntry that = (StorageEntry) o;
      return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }
  }
}
