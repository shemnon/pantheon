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
package tech.pegasys.pantheon.ethereum.retesteth.results;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.TransactionResult;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

@JsonPropertyOrder({
  "number",
  "hash",
  "parentHash",
  "nonce",
  "sha3Uncles",
  "logsBloom",
  "transactionsRoot",
  "stateRoot",
  "receiptsRoot",
  "author",
  "miner",
  "difficulty",
  "totalDifficulty",
  "extraData",
  "size",
  "gasLimit",
  "gasUsed",
  "timestamp",
  "uncles",
  "transactions"
})
public class BlockResult
    extends tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.BlockResult {
  public <T extends TransactionResult> BlockResult(
      final BlockHeader header,
      final List<TransactionResult> transactions,
      final List<JsonNode> ommers,
      final UInt256 totalDifficulty,
      final int size) {
    super(header, transactions, ommers, totalDifficulty, size);
  }

  @JsonGetter(value = "author")
  public String getAuthor() {
    return getMiner();
  }
}
