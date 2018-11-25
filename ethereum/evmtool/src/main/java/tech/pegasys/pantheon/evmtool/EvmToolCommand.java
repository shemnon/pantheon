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
package tech.pegasys.pantheon.evmtool;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderBuilder;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.LogsBloomFilter;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHashFunction;
import tech.pegasys.pantheon.ethereum.vm.BlockHashLookup;
import tech.pegasys.pantheon.ethereum.vm.Code;
import tech.pegasys.pantheon.ethereum.vm.EVM;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.ethereum.vm.ehalt.ExceptionalHaltException;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.bytes.BytesValues;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;

import com.google.common.base.Stopwatch;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  description = "This command evaluates EVM transactions.",
  abbreviateSynopsis = true,
  name = "evm",
  mixinStandardHelpOptions = true,
  sortOptions = false,
  // TODO: pull out of main   versionProvider = VersionProvider.class,
  header = "Usage:",
  synopsisHeading = "%n",
  descriptionHeading = "%nDescription:%n%n",
  optionListHeading = "%nOptions:%n",
  footerHeading = "%n",
  footer = "Pantheon is licensed under the Apache License 2.0"
)
public class EvmToolCommand implements Runnable {

  private static final Logger LOG = LogManager.getLogger();

  @Option(
    names = {"--code"},
    paramLabel = "<code>",
    description = "code to be executed"
  )
  BytesValue codeHexString = BytesValue.EMPTY;

  @Option(
    names = {"--codefile"},
    paramLabel = "<file>",
    description = "file containing code to be executed"
  )
  File codeFile;

  @Option(
    names = {"--gas"},
    paramLabel = "<int>"
  )
  Gas gas = Gas.of(10_000_000_000L);

  @Option(
    names = {"--price"},
    paramLabel = "<int>"
  )
  Wei gasPriceGWei = Wei.ZERO;

  @Option(
    names = {"--sender"},
    paramLabel = "<address>",
    description = "address of ORIGIN"
  )
  Address sender = Address.fromHexString("0x00");

  @Option(
    names = {"--receiver"},
    paramLabel = "<address>",
    description = "address of ADDRESS"
  )
  Address receiver = Address.fromHexString("0x00");

  @Option(
    names = {"--input"},
    paramLabel = "<code>",
    description = "CALLDATA"
  )
  BytesValue callData = BytesValue.EMPTY;

  @Option(
    names = {"--value"},
    paramLabel = "<int>"
  )
  Wei ethValue = Wei.ZERO;

  @Option(
    names = {"--json"},
    description = "output json output for each opcode"
  )
  boolean showJsonResults = false;

  @Option(
    names = {"--nomemory"},
    description = "disable showing the full memory output for each op"
  )
  boolean showMemory = true;

  @Option(
    names = {"--create"},
    description = "initcode to execute"
  )
  BytesValue createCode = BytesValue.of();

  @Option(
    names = {"--prestate", "--genesis"},
    description = "a chain specification, the same one that the client normally would use"
  )
  File genesisFile;

  public void parse(
      final CommandLine.AbstractParseResultHandler<List<Object>> resultHandler,
      final CommandLine.DefaultExceptionHandler<List<Object>> exceptionHandler,
      final String[] args) {

    final CommandLine commandLine = new CommandLine(this);

    // add sub commands here

    commandLine.registerConverter(Address.class, Address::fromHexString);
    commandLine.registerConverter(BytesValue.class, BytesValue::fromHexString);
    commandLine.registerConverter(Gas.class, (arg) -> Gas.of(Long.parseUnsignedLong(arg)));
    commandLine.registerConverter(Wei.class, (arg) -> Wei.of(Long.parseUnsignedLong(arg)));

    commandLine.parseWithHandlers(resultHandler, exceptionHandler, args);
  }

  @Override
  public void run() {
    try {
      final EvmToolComponent component =
          DaggerEvmToolComponent.builder()
              .protocolModule(new ProtocolModule())
              .genesisFileModule(new GenesisFileModule(genesisFile))
              .build();

      final Address zeroAddress = Address.fromHexString(String.format("%020x", 0));

      final BlockHeader blockHeader =
          BlockHeaderBuilder.create()
              .parentHash(Hash.EMPTY)
              .coinbase(zeroAddress)
              .difficulty(UInt256.ONE)
              .number(1)
              .gasLimit(5000)
              .timestamp(Instant.now().toEpochMilli())
              .ommersHash(Hash.EMPTY_LIST_HASH)
              .stateRoot(Hash.EMPTY_TRIE_HASH)
              .transactionsRoot(Hash.EMPTY)
              .receiptsRoot(Hash.EMPTY)
              .logsBloom(LogsBloomFilter.empty())
              .gasUsed(0)
              .extraData(BytesValue.EMPTY)
              .mixHash(Hash.EMPTY)
              .nonce(0)
              .blockHashFunction(MainnetBlockHashFunction::createHash)
              .buildBlockHeader();

      final MessageFrame messageFrame =
          MessageFrame.builder()
              .type(MessageFrame.Type.MESSAGE_CALL)
              .messageFrameStack(new ArrayDeque<>())
              .blockchain(component.getBlockchain())
              .worldState(component.getWorldUpdater())
              .initialGas(gas)
              .contract(zeroAddress)
              .address(receiver)
              .originator(sender)
              .gasPrice(gasPriceGWei)
              .inputData(callData)
              .sender(zeroAddress)
              .value(ethValue)
              .apparentValue(ethValue)
              .code(new Code(codeHexString))
              .blockHeader(blockHeader)
              .depth(0)
              .completer(c -> {})
              .miningBeneficiary(blockHeader.getCoinbase())
              .blockHashLookup(new BlockHashLookup(blockHeader, component.getBlockchain()))
              .build();

      messageFrame.setState(MessageFrame.State.CODE_EXECUTING);
      final EVM evm = component.getEvmAtBlock().apply(0);

      final Stopwatch stopwatch = Stopwatch.createStarted();
      evm.runToHalt(
          messageFrame,
          (frame, currentGasCost, executeOperation) -> {
            if (showJsonResults) {
              System.out.println(createEvmTraceOperation(messageFrame));
            }
            executeOperation.execute();
          });
      stopwatch.stop();

      System.out.println(
          new JsonObject()
              .put(
                  "gasUser",
                  gas.minus(messageFrame.getRemainingGas()).asUInt256().toShortHexString())
              .put("timens", stopwatch.elapsed().toNanos())
              .put("time", stopwatch.elapsed().toNanos() / 1000));

    } catch (final IOException | ExceptionalHaltException e) {
      LOG.fatal(e);
    }
  }

  JsonObject createEvmTraceOperation(final MessageFrame messageFrame) {
    final JsonArray stack = new JsonArray();
    for (int i = 0; i < messageFrame.stackSize(); i++) {
      stack.add(messageFrame.getStackItem(i).asUInt256().toShortHexString());
    }

    return new JsonObject()
        .put("pc", messageFrame.getPC())
        .put("op", messageFrame.getCurrentOperation().getOpcode())
        .put("gas", messageFrame.getRemainingGas().asUInt256().toShortHexString())
        .put(
            "gasCost",
            messageFrame.getCurrentOperation().cost(messageFrame).asUInt256().toShortHexString())
        .put(
            "memory",
            "0x"
                + BytesValues.asUnsignedBigInteger(
                        messageFrame.readMemory(UInt256.ZERO, messageFrame.memoryWordSize()))
                    .toString(16))
        .put("memSize", messageFrame.memoryByteSize())
        .put("depth", messageFrame.getMessageStackDepth() + 1)
        .put("stack", stack)
        .put("error", (JsonObject) null) // FIXME
        .put("opName", messageFrame.getCurrentOperation().getName());
  }
}
