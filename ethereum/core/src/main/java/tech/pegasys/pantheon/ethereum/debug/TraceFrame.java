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
package tech.pegasys.pantheon.ethereum.debug;

import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.vm.ExceptionalHaltReason;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

public class TraceFrame {

  private final int pc;
  private final String opcode;
  private final Gas gasRemaining;
  private final Optional<Gas> gasCost;
  private final int depth;
  private final EnumSet<ExceptionalHaltReason> exceptionalHaltReasons;
  private final Optional<Bytes32[]> stack;
  private final Optional<Bytes32[]> memory;
  private final Optional<Map<UInt256, UInt256>> storage;
  private final String revertReason;

  public TraceFrame(
      final int pc,
      final String opcode,
      final Gas gasRemaining,
      final Optional<Gas> gasCost,
      final int depth,
      final EnumSet<ExceptionalHaltReason> exceptionalHaltReasons,
      final Optional<Bytes32[]> stack,
      final Optional<Bytes32[]> memory,
      final Optional<Map<UInt256, UInt256>> storage,
      final String revertReason) {
    this.pc = pc;
    this.opcode = opcode;
    this.gasRemaining = gasRemaining;
    this.gasCost = gasCost;
    this.depth = depth;
    this.exceptionalHaltReasons = exceptionalHaltReasons;
    this.stack = stack;
    this.memory = memory;
    this.storage = storage;
    this.revertReason = revertReason;
  }

  public TraceFrame(
      final int pc,
      final String opcode,
      final Gas gasRemaining,
      final Optional<Gas> gasCost,
      final int depth,
      final EnumSet<ExceptionalHaltReason> exceptionalHaltReasons,
      final Optional<Bytes32[]> stack,
      final Optional<Bytes32[]> memory,
      final Optional<Map<UInt256, UInt256>> storage) {
    this(
        pc,
        opcode,
        gasRemaining,
        gasCost,
        depth,
        exceptionalHaltReasons,
        stack,
        memory,
        storage,
        null);
  }

  public int getPc() {
    return pc;
  }

  public String getOpcode() {
    return opcode;
  }

  public Gas getGasRemaining() {
    return gasRemaining;
  }

  public Optional<Gas> getGasCost() {
    return gasCost;
  }

  public int getDepth() {
    return depth;
  }

  public EnumSet<ExceptionalHaltReason> getExceptionalHaltReasons() {
    return exceptionalHaltReasons;
  }

  public Optional<Bytes32[]> getStack() {
    return stack;
  }

  public Optional<Bytes32[]> getMemory() {
    return memory;
  }

  public Optional<Map<UInt256, UInt256>> getStorage() {
    return storage;
  }

  public String getRevertReason() {
    return revertReason;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("pc", pc)
        .add("opcode", opcode)
        .add("getGasRemaining", gasRemaining)
        .add("gasCost", gasCost)
        .add("depth", depth)
        .add("exceptionalHaltReasons", exceptionalHaltReasons)
        .add("stack", stack)
        .add("memory", memory)
        .add("storage", storage)
        .toString();
  }
}
