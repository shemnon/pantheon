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
package tech.pegasys.pantheon.cli.rlp;

import tech.pegasys.pantheon.consensus.ibft.IbftExtraData;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IbftExtraDataCLIAdapter implements JSONtoRLP {

  private final Collection<String> validators;

  @JsonCreator
  IbftExtraDataCLIAdapter(@JsonProperty("validators") final Collection<String> validators) {
    this.validators = validators;
  }

  @Override
  public BytesValue encode() {
    List<Address> validators =
        this.validators.stream().map(Address::fromHexString).collect(Collectors.toList());
    return new IbftExtraData(
            BytesValue.wrap(new byte[32]), Collections.emptyList(), Optional.empty(), 0, validators)
        .encode();
  }

  @Override
  public Class<? extends JSONtoRLP> getType() {
    return this.getClass();
  }
}
