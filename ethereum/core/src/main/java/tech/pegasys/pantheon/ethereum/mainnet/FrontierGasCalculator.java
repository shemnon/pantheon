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
package tech.pegasys.pantheon.ethereum.mainnet;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

public class FrontierGasCalculator implements GasCalculator {

  private static final Gas TX_DATA_ZERO_COST = Gas.of(4L);

  private static final Gas TX_DATA_NON_ZERO_COST = Gas.of(68L);

  private static final Gas TX_BASE_COST = Gas.of(21_000L);

  private static final Gas TX_CREATE_EXTRA_COST = Gas.of(0L);

  private static final Gas CODE_DEPOSIT_BYTE_COST = Gas.of(200L);

  private static final Gas VERY_LOW_TIER_GAS_COST = Gas.of(3L);

  private static final Gas LOW_TIER_GAS_COST = Gas.of(5L);

  private static final Gas BASE_TIER_GAS_COST = Gas.of(2L);

  private static final Gas MID_TIER_GAS_COST = Gas.of(8L);

  private static final Gas HIGH_TIER_GAS_COST = Gas.of(10L);

  private static final Gas CALL_OPERATION_BASE_GAS_COST = Gas.of(40L);

  private static final Gas CALL_VALUE_TRANSFER_GAS_COST = Gas.of(9_000L);

  private static final Gas ADDITIONAL_CALL_STIPEND = Gas.of(2_300L);

  private static final Gas NEW_ACCOUNT_GAS_COST = Gas.of(25_000L);

  private static final Gas CREATE_OPERATION_GAS_COST = Gas.of(32_000L);

  private static final Gas COPY_WORD_GAS_COST = Gas.of(3L);

  private static final Gas MEMORY_WORD_GAS_COST = Gas.of(3L);

  private static final Gas EXT_CODE_BASE_GAS_COST = Gas.of(20L);

  private static final Gas SELFDESTRUCT_OPERATION_GAS_COST = Gas.of(0);

  private static final Gas SHA3_OPERATION_BASE_GAS_COST = Gas.of(30L);

  private static final Gas SHA3_OPERATION_WORD_GAS_COST = Gas.of(6L);

  private static final Gas STORAGE_SET_GAS_COST = Gas.of(20_000L);

  private static final Gas STORAGE_RESET_GAS_COST = Gas.of(5_000L);

  private static final Gas STORAGE_RESET_REFUND_AMOUNT = Gas.of(15_000L);

  private static final Gas SELF_DESTRUCT_REFUND_AMOUNT = Gas.of(24_000L);

  @Override
  public Gas transactionIntrinsicGasCost(final Transaction transaction) {
    final BytesValue payload = transaction.getPayload();
    int zeros = 0;
    for (int i = 0; i < payload.size(); i++) {
      if (payload.get(i) == 0) {
        ++zeros;
      }
    }
    final int nonZeros = payload.size() - zeros;

    Gas cost =
        Gas.ZERO
            .plus(TX_BASE_COST)
            .plus(TX_DATA_ZERO_COST.times(zeros))
            .plus(TX_DATA_NON_ZERO_COST.times(nonZeros));

    if (transaction.isContractCreation()) {
      cost = cost.plus(txCreateExtraGasCost());
    }

    return cost;
  }

  /**
   * Returns the additional gas cost for contract creation transactions
   *
   * @return the additional gas cost for contract creation transactions
   */
  protected Gas txCreateExtraGasCost() {
    return TX_CREATE_EXTRA_COST;
  }

  @Override
  public Gas codeDepositGasCost(final int codeSize) {
    return CODE_DEPOSIT_BYTE_COST.times(codeSize);
  }

  @Override
  public Gas getZeroTierGasCost() {
    return Gas.ZERO;
  }

  @Override
  public Gas getVeryLowTierGasCost() {
    return VERY_LOW_TIER_GAS_COST;
  }

  @Override
  public Gas getLowTierGasCost() {
    return LOW_TIER_GAS_COST;
  }

  @Override
  public Gas getBaseTierGasCost() {
    return BASE_TIER_GAS_COST;
  }

  @Override
  public Gas getMidTierGasCost() {
    return MID_TIER_GAS_COST;
  }

  @Override
  public Gas getHighTierGasCost() {
    return HIGH_TIER_GAS_COST;
  }

  @Override
  public Gas getSha3BaseGasCost() {
    return SHA3_OPERATION_BASE_GAS_COST;
  }

  @Override
  public Gas getSha3WordGasCost() {
    return SHA3_OPERATION_WORD_GAS_COST;
  }

  /**
   * Returns the base gas cost to execute a call operation.
   *
   * @return the base gas cost to execute a call operation
   */
  protected Gas callOperationBaseGasCost() {
    return CALL_OPERATION_BASE_GAS_COST;
  }

  /**
   * Returns the gas cost to transfer funds in a call operation.
   *
   * @return the gas cost to transfer funds in a call operation
   */
  protected Gas callValueTransferGasCost() {
    return CALL_VALUE_TRANSFER_GAS_COST;
  }

  /**
   * Returns the gas cost to create a new account.
   *
   * @return the gas cost to create a new account
   */
  protected Gas newAccountGasCost() {
    return NEW_ACCOUNT_GAS_COST;
  }

  @Override
  public Gas callOperationGasCost(
      final MessageFrame frame,
      final Gas stipend,
      final UInt256 inputDataOffset,
      final UInt256 inputDataLength,
      final UInt256 outputDataOffset,
      final UInt256 outputDataLength,
      final Wei transferValue,
      final Account recipient) {
    final Gas inputDataMemoryExpansionCost =
        memoryExpansionGasCost(frame, inputDataOffset, inputDataLength);
    final Gas outputDataMemoryExpansionCost =
        memoryExpansionGasCost(frame, outputDataOffset, outputDataLength);
    final Gas memoryExpansionCost = inputDataMemoryExpansionCost.max(outputDataMemoryExpansionCost);

    Gas cost = callOperationBaseGasCost().plus(stipend).plus(memoryExpansionCost);

    if (!transferValue.isZero()) {
      cost = cost.plus(callValueTransferGasCost());
    }

    if (recipient == null) {
      cost = cost.plus(newAccountGasCost());
    }

    return cost;
  }

  /**
   * Returns the additional call stipend for calls with value transfers.
   *
   * @return the additional call stipend for calls with value transfers
   */
  protected Gas additionalCallStipend() {
    return ADDITIONAL_CALL_STIPEND;
  }

  @Override
  public Gas gasAvailableForChildCall(
      final MessageFrame frame, final Gas stipend, final boolean transfersValue) {
    if (transfersValue) {
      return stipend.plus(additionalCallStipend());
    } else {
      return stipend;
    }
  }

  @Override
  public Gas createOperationGasCost(final MessageFrame frame) {
    final UInt256 initCodeOffset = frame.getStackItem(1).asUInt256();
    final UInt256 initCodeLength = frame.getStackItem(2).asUInt256();

    final Gas memoryGasCost = memoryExpansionGasCost(frame, initCodeOffset, initCodeLength);
    return CREATE_OPERATION_GAS_COST.plus(memoryGasCost);
  }

  @Override
  public Gas gasAvailableForChildCreate(final Gas stipend) {
    return stipend;
  }

  @Override
  public Gas dataCopyOperationGasCost(
      final MessageFrame frame, final UInt256 offset, final UInt256 length) {
    return copyWordsToMemoryGasCost(
        frame, VERY_LOW_TIER_GAS_COST, COPY_WORD_GAS_COST, offset, length);
  }

  @Override
  public Gas memoryExpansionGasCost(
      final MessageFrame frame, final UInt256 offset, final UInt256 length) {

    final Gas pre = memoryCost(frame.memoryWordSize());
    final Gas post = memoryCost(frame.calculateMemoryExpansion(offset, length));

    return post.minus(pre);
  }

  /**
   * Returns the base gas cost for external code accesses.
   *
   * @return the base gas cost for external code accesses
   */
  protected Gas extCodeBaseGasCost() {
    return EXT_CODE_BASE_GAS_COST;
  }

  @Override
  public Gas extCodeCopyOperationGasCost(
      final MessageFrame frame, final UInt256 offset, final UInt256 length) {
    return copyWordsToMemoryGasCost(
        frame, extCodeBaseGasCost(), COPY_WORD_GAS_COST, offset, length);
  }

  @Override
  public Gas getExtCodeSizeOperationGasCost() {
    return extCodeBaseGasCost();
  }

  @Override
  public Gas selfDestructOperationGasCost(final Account recipient, final Wei inheritance) {
    return SELFDESTRUCT_OPERATION_GAS_COST;
  }

  @Override
  public Gas calculateStorageCost(
      final Account account, final UInt256 key, final UInt256 newValue) {
    return !newValue.isZero() && account.getStorageValue(key).isZero()
        ? STORAGE_SET_GAS_COST
        : STORAGE_RESET_GAS_COST;
  }

  @Override
  public Gas calculateStorageRefundAmount(
      final Account account, final UInt256 key, final UInt256 newValue) {
    return newValue.isZero() && !account.getStorageValue(key).isZero()
        ? STORAGE_RESET_REFUND_AMOUNT
        : Gas.ZERO;
  }

  @Override
  public Gas getSelfDestructRefundAmount() {
    return SELF_DESTRUCT_REFUND_AMOUNT;
  }

  @Override
  public Gas copyWordsToMemoryGasCost(
      final MessageFrame frame,
      final Gas baseGasCost,
      final Gas wordGasCost,
      final UInt256 offset,
      final UInt256 length) {
    final UInt256 numWords = length.dividedCeilBy(Bytes32.SIZE);

    final Gas copyCost = wordGasCost.times(Gas.of(numWords)).plus(baseGasCost);
    final Gas memoryCost = memoryExpansionGasCost(frame, offset, length);

    return copyCost.plus(memoryCost);
  }

  private static Gas memoryCost(final UInt256 length) {
    if (!length.fitsInt()) {
      return Gas.MAX_VALUE;
    }
    final Gas len = Gas.of(length);
    final Gas base = len.times(len).dividedBy(512);

    return MEMORY_WORD_GAS_COST.times(len).plus(base);
  }
}
