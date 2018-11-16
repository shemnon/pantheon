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
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.File;
import java.util.List;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
  description = "This command evaluates EVM transactions.",
  abbreviateSynopsis = true,
  name = "evm",
  sortOptions = false,
  // TODO: pull out of main   versionProvider = VersionProvider.class,
  header = "Usage:",
  synopsisHeading = "%n",
  descriptionHeading = "%nDescription:%n%n",
  optionListHeading = "%nOptions:%n",
  footerHeading = "%n",
  footer = "Pantheon is licensed under the Apache License 2.0"
)
public class EVMToolCommand implements Runnable {

  @Option(
    names = {"--code"},
    paramLabel = "<code>",
    description = "code to be executed"
  )
  String codeHexString = "";

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
  int gas = 0;

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
  BytesValue callData = BytesValue.of();

  @Option(
    names = {"--value"},
    paramLabel = "<int>"
  )
  int ethValue = 0;

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
    names = {"--prestate"},
    description = "a chain specification, the same one that the client normally would use"
  )
  File prestate;

  public void parse(
      final CommandLine.AbstractParseResultHandler<List<Object>> resultHandler,
      final CommandLine.DefaultExceptionHandler<List<Object>> exceptionHandler,
      final String[] args) {

    final CommandLine commandLine = new CommandLine(this);

    // add sub commands here

    commandLine.registerConverter(Address.class, Address::fromHexString);
    commandLine.registerConverter(BytesValue.class, BytesValue::fromHexString);
    commandLine.registerConverter(Wei.class, (arg) -> Wei.of(Long.parseUnsignedLong(arg)));

    commandLine.parseWithHandlers(resultHandler, exceptionHandler, args);
  }

  @Override
  public void run() {
    // TODO: something useful
  }
}
