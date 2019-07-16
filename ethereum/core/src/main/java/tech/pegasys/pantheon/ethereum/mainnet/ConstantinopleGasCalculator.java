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
import tech.pegasys.pantheon.util.uint.UInt256;

public class ConstantinopleGasCalculator extends SpuriousDragonGasCalculator {

  private static final Gas SSTORE_NO_OP_COST = Gas.of(200);
  private static final Gas SSTORE_ADDITIONAL_WRITE_COST = Gas.of(200);
  private static final Gas SSTORE_FIRST_DIRTY_NEW_STORAGE_COST = Gas.of(20_000);
  private static final Gas SSTORE_FIRST_DIRTY_EXISTING_STORAGE_COST = Gas.of(5_000);
  private static final Gas STORAGE_RESET_REFUND_AMOUNT = Gas.of(15_000);
  private static final Gas NEGATIVE_STORAGE_RESET_REFUND_AMOUNT = Gas.of(-15_000);
  private static final Gas SSTORE_DIRTY_RETURN_TO_UNUSED_REFUND_AMOUNT = Gas.of(19800);
  private static final Gas SSTORE_DIRTY_RETURN_TO_ORIGINAL_VALUE_REFUND_AMOUNT = Gas.of(4800);

  @Override
  // As per https://eips.ethereum.org/EIPS/eip-1283
  public Gas calculateStorageCost(
      final Account account, final UInt256 key, final UInt256 newValue) {

    final UInt256 currentValue = account.getStorageValue(key);
    if (currentValue.equals(newValue)) {
      return SSTORE_NO_OP_COST;
    } else {
      final UInt256 originalValue = account.getOriginalStorageValue(key);
      if (originalValue.equals(currentValue)) {
        return originalValue.isZero()
            ? SSTORE_FIRST_DIRTY_NEW_STORAGE_COST
            : SSTORE_FIRST_DIRTY_EXISTING_STORAGE_COST;
      } else {
        return SSTORE_ADDITIONAL_WRITE_COST;
      }
    }
  }

  @Override
  // As per https://eips.ethereum.org/EIPS/eip-1283
  public Gas calculateStorageRefundAmount(
      final Account account, final UInt256 key, final UInt256 newValue) {

    final UInt256 currentValue = account.getStorageValue(key);
    if (currentValue.equals(newValue)) {
      return Gas.ZERO;
    } else {
      final UInt256 originalValue = account.getOriginalStorageValue(key);
      if (originalValue.equals(currentValue)) {
        if (originalValue.isZero()) {
          return Gas.ZERO;
        } else if (newValue.isZero()) {
          return STORAGE_RESET_REFUND_AMOUNT;
        } else {
          return Gas.ZERO;
        }
      } else {
        Gas refund = Gas.ZERO;
        if (!originalValue.isZero()) {
          if (currentValue.isZero()) {
            refund = NEGATIVE_STORAGE_RESET_REFUND_AMOUNT;
          } else if (newValue.isZero()) {
            refund = STORAGE_RESET_REFUND_AMOUNT;
          }
        }

        if (originalValue.equals(newValue)) {
          refund =
              refund.plus(
                  originalValue.isZero()
                      ? SSTORE_DIRTY_RETURN_TO_UNUSED_REFUND_AMOUNT
                      : SSTORE_DIRTY_RETURN_TO_ORIGINAL_VALUE_REFUND_AMOUNT);
        }
        return refund;
      }
    }
  }
}
