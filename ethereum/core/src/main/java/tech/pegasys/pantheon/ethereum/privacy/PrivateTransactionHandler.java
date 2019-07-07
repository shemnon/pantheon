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
package tech.pegasys.pantheon.ethereum.privacy;

import static tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.INCORRECT_PRIVATE_NONCE;
import static tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason.PRIVATE_NONCE_TOO_LOW;

import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.enclave.Enclave;
import tech.pegasys.pantheon.enclave.types.ReceiveRequest;
import tech.pegasys.pantheon.enclave.types.ReceiveResponse;
import tech.pegasys.pantheon.enclave.types.SendRequest;
import tech.pegasys.pantheon.enclave.types.SendResponse;
import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.PrivacyParameters;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.Util;
import tech.pegasys.pantheon.ethereum.mainnet.TransactionValidator.TransactionInvalidReason;
import tech.pegasys.pantheon.ethereum.mainnet.ValidationResult;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateArchive;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.bytes.BytesValues;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Charsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PrivateTransactionHandler {

  private static final Logger LOG = LogManager.getLogger();

  private final Enclave enclave;
  private final Address privacyPrecompileAddress;
  private final SECP256K1.KeyPair nodeKeyPair;
  private final Address signerAddress;
  private final PrivateStateStorage privateStateStorage;
  private final WorldStateArchive privateWorldStateArchive;

  public PrivateTransactionHandler(final PrivacyParameters privacyParameters) {
    this(
        new Enclave(privacyParameters.getEnclaveUri()),
        Address.privacyPrecompiled(privacyParameters.getPrivacyAddress()),
        privacyParameters.getSigningKeyPair(),
        privacyParameters.getPrivateStateStorage(),
        privacyParameters.getPrivateWorldStateArchive());
  }

  public PrivateTransactionHandler(
      final Enclave enclave,
      final Address privacyPrecompileAddress,
      final SECP256K1.KeyPair nodeKeyPair,
      final PrivateStateStorage privateStateStorage,
      final WorldStateArchive privateWorldStateArchive) {
    this.enclave = enclave;
    this.privacyPrecompileAddress = privacyPrecompileAddress;
    this.nodeKeyPair = nodeKeyPair;
    this.signerAddress = Util.publicKeyToAddress(nodeKeyPair.getPublicKey());
    this.privateStateStorage = privateStateStorage;
    this.privateWorldStateArchive = privateWorldStateArchive;
  }

  public String sendToOrion(final PrivateTransaction privateTransaction) throws Exception {
    final SendRequest sendRequest = createSendRequest(privateTransaction);
    final SendResponse sendResponse;

    try {
      LOG.trace("Storing private transaction in enclave");
      sendResponse = enclave.send(sendRequest);
      return sendResponse.getKey();
    } catch (Exception e) {
      LOG.error("Failed to store private transaction in enclave", e);
      throw e;
    }
  }

  public String getPrivacyGroup(final String key, final BytesValue from) throws Exception {
    final ReceiveRequest receiveRequest = new ReceiveRequest(key, BytesValues.asString(from));
    LOG.debug("Getting privacy group for {}", BytesValues.asString(from));
    final ReceiveResponse receiveResponse;
    try {
      receiveResponse = enclave.receive(receiveRequest);
      return BytesValue.wrap(receiveResponse.getPrivacyGroupId().getBytes(Charsets.UTF_8))
          .toString();
    } catch (Exception e) {
      LOG.error("Failed to retrieve private transaction in enclave", e);
      throw e;
    }
  }

  public Transaction createPrivacyMarkerTransaction(
      final String transactionEnclaveKey,
      final PrivateTransaction privateTransaction,
      final Long nonce) {

    return Transaction.builder()
        .nonce(nonce)
        .gasPrice(privateTransaction.getGasPrice())
        .gasLimit(privateTransaction.getGasLimit())
        .to(privacyPrecompileAddress)
        .value(privateTransaction.getValue())
        .payload(BytesValue.wrap(transactionEnclaveKey.getBytes(Charsets.UTF_8)))
        .sender(signerAddress)
        .signAndBuild(nodeKeyPair);
  }

  public ValidationResult<TransactionInvalidReason> validatePrivateTransaction(
      final PrivateTransaction privateTransaction, final String privacyGroupId) {
    final long actualNonce = privateTransaction.getNonce();
    final long expectedNonce = getSenderNonce(privateTransaction.getSender(), privacyGroupId);
    LOG.debug("Validating actual nonce {} with expected nonce {}", actualNonce, expectedNonce);
    if (expectedNonce > actualNonce) {
      return ValidationResult.invalid(
          PRIVATE_NONCE_TOO_LOW,
          String.format(
              "private transaction nonce %s does not match sender account nonce %s.",
              actualNonce, expectedNonce));
    }

    if (expectedNonce != actualNonce) {
      return ValidationResult.invalid(
          INCORRECT_PRIVATE_NONCE,
          String.format(
              "private transaction nonce %s does not match sender account nonce %s.",
              actualNonce, expectedNonce));
    }

    return ValidationResult.valid();
  }

  private SendRequest createSendRequest(final PrivateTransaction privateTransaction) {
    final List<String> privateFor =
        privateTransaction.getPrivateFor().stream()
            .map(BytesValues::asString)
            .collect(Collectors.toList());

    // FIXME: Orion should concatenate to and from - not it pantheon
    if (privateFor.isEmpty()) {
      privateFor.add(BytesValues.asString(privateTransaction.getPrivateFrom()));
    }

    final BytesValueRLPOutput bvrlp = new BytesValueRLPOutput();
    privateTransaction.writeTo(bvrlp);

    return new SendRequest(
        Base64.getEncoder().encodeToString(bvrlp.encoded().extractArray()),
        BytesValues.asString(privateTransaction.getPrivateFrom()),
        privateFor);
  }

  public long getSenderNonce(final Address sender, final String privacyGroupId) {
    return privateStateStorage
        .getPrivateAccountState(BytesValue.fromHexString(privacyGroupId))
        .map(
            lastRootHash ->
                privateWorldStateArchive
                    .getMutable(lastRootHash)
                    .map(
                        worldState -> {
                          final Account maybePrivateSender = worldState.get(sender);

                          if (maybePrivateSender != null) {
                            return maybePrivateSender.getNonce();
                          }
                          // account has not interacted in this private state
                          return Account.DEFAULT_NONCE;
                        })
                    // private state does not exist
                    .orElse(Account.DEFAULT_NONCE))
        .orElse(
            // private state does not exist
            Account.DEFAULT_NONCE);
  }

  public Address getSignerAddress() {
    return signerAddress;
  }
}
