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
package tech.pegasys.pantheon.ethereum.eth.sync;

import static java.lang.Math.toIntExact;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;

import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;

public class CheckpointRange {
  private final BlockHeader start;
  private final Optional<BlockHeader> end;

  public CheckpointRange(final BlockHeader start) {
    this.start = start;
    this.end = Optional.empty();
  }

  public CheckpointRange(final BlockHeader start, final BlockHeader end) {
    this.start = start;
    this.end = Optional.of(end);
  }

  public BlockHeader getStart() {
    return start;
  }

  public boolean hasEnd() {
    return end.isPresent();
  }

  public BlockHeader getEnd() {
    return end.get();
  }

  public int getSegmentLengthExclusive() {
    return toIntExact(end.get().getNumber() - start.getNumber() - 1);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final CheckpointRange that = (CheckpointRange) o;
    return Objects.equals(start, that.start) && Objects.equals(end, that.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("start", start).add("end", end).toString();
  }
}
