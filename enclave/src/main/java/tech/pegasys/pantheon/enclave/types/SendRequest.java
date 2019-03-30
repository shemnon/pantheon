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
package tech.pegasys.pantheon.enclave.types;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"payload", "from", "to"})
public class SendRequest {
  private byte[] payload;
  private String from;
  private List<String> to;

  public SendRequest(
      @JsonProperty(value = "payload") final String payload,
      @JsonProperty(value = "from") final String from,
      @JsonProperty(value = "to") final List<String> to) {
    this.payload = payload.getBytes(UTF_8);
    this.from = from;
    this.to = to;
  }

  public byte[] getPayload() {
    return payload;
  }

  public String getFrom() {
    return from;
  }

  public List<String> getTo() {
    return to;
  }
}
