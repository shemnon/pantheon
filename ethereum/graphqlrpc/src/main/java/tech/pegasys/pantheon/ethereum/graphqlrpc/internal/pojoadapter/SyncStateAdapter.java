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
package tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter;

import tech.pegasys.pantheon.ethereum.core.SyncStatus;

import java.util.Optional;

import com.google.common.primitives.UnsignedLong;

public class SyncStateAdapter {
  private SyncStatus syncStatus;

  public SyncStateAdapter(final SyncStatus syncStatus) {
    this.syncStatus = syncStatus;
  }

  public Optional<UnsignedLong> getStartingBlock() {
    return Optional.of(UnsignedLong.valueOf(syncStatus.getStartingBlock()));
  }

  public Optional<UnsignedLong> getCurrentBlock() {
    return Optional.of(UnsignedLong.valueOf(syncStatus.getCurrentBlock()));
  }

  public Optional<UnsignedLong> getHighestBlock() {
    return Optional.of(UnsignedLong.valueOf(syncStatus.getHighestBlock()));
  }

  /*
   * # PulledStates is the number of state entries fetched so far, or null # if
   * this is not known or not relevant. pulledStates: Long # KnownStates is the
   * number of states the node knows of so far, or null # if this is not known or
   * not relevant. knownStates: Long
   */
}