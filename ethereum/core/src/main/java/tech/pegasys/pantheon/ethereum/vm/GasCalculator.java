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
package tech.pegasys.pantheon.ethereum.vm;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.AbstractMessageProcessor;
import tech.pegasys.pantheon.ethereum.vm.operations.ExtCodeCopyOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ExtCodeSizeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SelfDestructOperation;
import tech.pegasys.pantheon.util.uint.UInt256;

/**
 * Provides various gas cost lookups and calculations used during block processing.
 *
 * <p>The {@code GasCalculator} is meant to encapsulate all {@link Gas}-related calculations except
 * for the following "safe" operations:
 *
 * <ul>
 *   <li><b>Operation Gas Deductions:</b> Deducting the operation's gas cost from the VM's current
 *       message frame because the
 * </ul>
 */
public interface GasCalculator {

  // Transaction Gas Calculations

  /**
   * Returns a {@link Transaction}s intrinisic gas cost
   *
   * @param transaction The transaction
   * @return the transaction's intrinsic gas cost
   */
  Gas transactionIntrinsicGasCost(Transaction transaction);

  // Contract Creation Gas Calculations

  /**
   * Returns the cost for a {@link AbstractMessageProcessor} to deposit the code in storage
   *
   * @param codeSize The size of the code in bytes
   * @return the code deposit cost
   */
  Gas codeDepositGasCost(int codeSize);

  // Gas Tier Lookups

  /**
   * Returns the gas cost for the zero gas tier.
   *
   * @return the gas cost for the zero gas tier
   */
  Gas getZeroTierGasCost();

  /**
   * Returns the gas cost for the very low gas tier.
   *
   * @return the gas cost for the very low gas tier
   */
  Gas getVeryLowTierGasCost();

  /**
   * Returns the gas cost for the low gas tier.
   *
   * @return the gas cost for the low gas tier
   */
  Gas getLowTierGasCost();

  /**
   * Returns the gas cost for the base gas tier.
   *
   * @return the gas cost for the base gas tier
   */
  Gas getBaseTierGasCost();

  /**
   * Returns the gas cost for the mid gas tier.
   *
   * @return the gas cost for the mid gas tier
   */
  Gas getMidTierGasCost();

  /**
   * Returns the gas cost for the high gas tier.
   *
   * @return the gas cost for the high gas tier
   */
  Gas getHighTierGasCost();

  /** The gas cost of SHA3 per byte calculations */
  Gas getSha3WordGasCost();

  /** The gas cost of SHA3 base cost */
  Gas getSha3BaseGasCost();

  // Call/Create Operation Calculations

  /**
   * Returns the gas cost for one of the various CALL operations.
   *
   * @param frame The current frame
   * @param stipend The gas stipend being provided by the CALL caller
   * @param inputDataOffset The offset in memory to retrieve the CALL input data
   * @param inputDataLength The CALL input data length
   * @param outputDataOffset The offset in memory to place the CALL output data
   * @param outputDataLength The CALL output data length
   * @param transferValue The wei being transferred
   * @param recipient The CALL recipient
   * @return The gas cost for the CALL operation
   */
  Gas callOperationGasCost(
      MessageFrame frame,
      Gas stipend,
      UInt256 inputDataOffset,
      UInt256 inputDataLength,
      UInt256 outputDataOffset,
      UInt256 outputDataLength,
      Wei transferValue,
      Account recipient);

  /**
   * Returns the amount of gas parent will provide its child CALL.
   *
   * @param frame The current frame
   * @param stipend The gas stipend being provided by the CALL caller
   * @param transfersValue Whether or not the call transfers any wei
   * @return the amount of gas parent will provide its child CALL
   */
  Gas gasAvailableForChildCall(MessageFrame frame, Gas stipend, boolean transfersValue);

  /**
   * Returns the amount of gas the CREATE operation will consume.
   *
   * @param frame The current frame
   * @return the amount of gas the CREATE operation will consume
   */
  Gas createOperationGasCost(MessageFrame frame);

  /**
   * Returns the amount of gas parent will provide its child CREATE.
   *
   * @param stipend The gas stipend being provided by the CREATE caller
   * @return the amount of gas parent will provide its child CREATE
   */
  Gas gasAvailableForChildCreate(Gas stipend);

  // Re-used Operation Calculations

  /**
   * Returns the amount of gas consumed by the data copy operation.
   *
   * @param frame The current frame
   * @param offset The offset in memory to copy the data to
   * @param length The length of the data being copied into memory
   * @return the amount of gas consumed by the data copy operation
   */
  Gas dataCopyOperationGasCost(MessageFrame frame, UInt256 offset, UInt256 length);

  /**
   * A general function to calculate memory metered copying costs
   *
   * @param frame The current frame
   * @param baseGasCost the base cost added to all calculations
   * @param wordGasCost the cost added per word
   * @param offset the index to start copying
   * @param length the total lengh to copy
   * @return the amount of gas consumed by the data copy operation
   */
  Gas copyWordsToMemoryGasCost(
      final MessageFrame frame,
      final Gas baseGasCost,
      final Gas wordGasCost,
      final UInt256 offset,
      final UInt256 length);

  /**
   * Returns the cost of expanding memory for the specified access.
   *
   * @param frame The current frame
   * @param offset The offset in memory where the access occurs
   * @param length the length of the memory access
   * @return The gas required to expand memory for the specified access
   */
  Gas memoryExpansionGasCost(MessageFrame frame, UInt256 offset, UInt256 length);

  // Specific Non-call Operation Calculations

  /**
   * Returns the cost for executing a {@link ExtCodeCopyOperation}.
   *
   * @param frame The current frame
   * @param offset The offset in memory to external code copy the data to
   * @param length The length of the code being copied into memory
   * @return the cost for executing the external code size operation
   */
  Gas extCodeCopyOperationGasCost(MessageFrame frame, UInt256 offset, UInt256 length);

  /**
   * Returns the cost for executing a {@link ExtCodeSizeOperation}.
   *
   * @return the cost for executing the external code size operation
   */
  Gas getExtCodeSizeOperationGasCost();

  /**
   * Returns the cost for executing a {@link SelfDestructOperation}.
   *
   * @param recipient The recipient of the self destructed inheritance (may be null)
   * @param inheritance The amount the recipient will receive
   * @return the cost for executing the self destruct operation
   */
  Gas selfDestructOperationGasCost(Account recipient, Wei inheritance);

  /**
   * Returns the cost for an SSTORE operation.
   *
   * @param account the account that storage will be changed in
   * @param key the key the new value is to be stored under
   * @param newValue the new value to be stored
   * @return the gas cost for the SSTORE operation
   */
  Gas calculateStorageCost(Account account, UInt256 key, UInt256 newValue);

  /**
   * Returns the refund amount for an SSTORE operation.
   *
   * @param account the account that storage will be changed in
   * @param key the key the new value is to be stored under
   * @param newValue the new value to be stored
   * @return the gas refund for the SSTORE operation
   */
  Gas calculateStorageRefundAmount(Account account, UInt256 key, UInt256 newValue);

  /**
   * Returns the refund amount for deleting an account in a {@link SelfDestructOperation}.
   *
   * @return the refund amount for deleting an account in a self destruct operation
   */
  Gas getSelfDestructRefundAmount();
}
