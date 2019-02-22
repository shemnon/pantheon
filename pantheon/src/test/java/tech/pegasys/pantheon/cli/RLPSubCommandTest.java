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
package tech.pegasys.pantheon.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.junit.After;
import org.junit.Test;
import picocli.CommandLine.Model.CommandSpec;

public class RLPSubCommandTest extends CommandTestAbstract {
  //  [ "0000000000000000000000000000000000000000000000000000000000000000",
  //      [ "be068f726a13c8d46c44be6ce9d275600e1735a4",
  //      "5ff6f4b66a46a2b2310a6f3a93aaddc0d9a1c193" ],
  //      "",
  //      "00000000",
  //      []
  //      ]

  private static final String EXPECTED_RLP_USAGE =
      "Usage: pantheon rlp [-hV] [COMMAND]"
          + System.lineSeparator()
          + "This command provides RLP data related actions."
          + System.lineSeparator()
          + "  -h, --help      Show this help message and exit."
          + System.lineSeparator()
          + "  -V, --version   Print version information and exit."
          + System.lineSeparator()
          + "Commands:"
          + System.lineSeparator()
          + "  encode  This command encodes a JSON typed data into an RLP hex string.";

  private static final String RLP_SUBCOMMAND_NAME = "rlp";
  private static final String RLP_ENCODE_SUBCOMMAND_NAME = "encode";

  // RLP sub-command
  @Test
  public void rlpSubCommandExistAnbHaveSubCommands() {
    CommandSpec spec = parseCommand();
    assertThat(spec.subcommands()).containsKeys(RLP_SUBCOMMAND_NAME);
    assertThat(spec.subcommands().get(RLP_SUBCOMMAND_NAME).getSubcommands())
        .containsKeys(RLP_ENCODE_SUBCOMMAND_NAME);
    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void callingRLPSubCommandWithoutSubSubcommandMustDisplayUsage() {
    parseCommand(RLP_SUBCOMMAND_NAME);
    assertThat(commandOutput.toString()).startsWith(EXPECTED_RLP_USAGE);
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void callingRPLSubCommandHelpMustDisplayUsage() {
    parseCommand(RLP_SUBCOMMAND_NAME, "--help");
    assertThat(commandOutput.toString()).startsWith(EXPECTED_RLP_USAGE);
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  // Encode RLP sub-command
  @Test
  public void callingRLPEncodeSubCommandWithoutPathMustWriteToStandardOutput() {

    String jsonInput =
        "{\"validators\":[ \"be068f726a13c8d46c44be6ce9d275600e1735a4\",\n"
            + "\"5ff6f4b66a46a2b2310a6f3a93aaddc0d9a1c193\" ]}";

    // set stdin
    ByteArrayInputStream stdIn = new ByteArrayInputStream(jsonInput.getBytes(UTF_8));

    parseCommand(stdIn, RLP_SUBCOMMAND_NAME, RLP_ENCODE_SUBCOMMAND_NAME);

    String expectedRlpString =
        "0xf853a00000000000000000000000000000000000000000000000000000000000000000ea94be068f726a13c8d"
            + "46c44be6ce9d275600e1735a4945ff6f4b66a46a2b2310a6f3a93aaddc0d9a1c193808400000000c0";
    assertThat(commandOutput.toString()).contains(expectedRlpString);
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @Test
  public void callingRLPEncodeSubCommandWithFilePathMustWriteInThisFile() throws Exception {

    final File file = File.createTempFile("ibftExtraData", "rlp");

    String jsonInput =
        "{\"validators\":[ \"be068f726a13c8d46c44be6ce9d275600e1735a4\",\n"
            + "\"5ff6f4b66a46a2b2310a6f3a93aaddc0d9a1c193\" ]}";

    // set stdin
    ByteArrayInputStream stdIn = new ByteArrayInputStream(jsonInput.getBytes(UTF_8));

    parseCommand(stdIn, RLP_SUBCOMMAND_NAME, RLP_ENCODE_SUBCOMMAND_NAME, "--to", file.getPath());

    String expectedRlpString =
        "0xf853a00000000000000000000000000000000000000000000000000000000000000000ea94be068f726a13c8d"
            + "46c44be6ce9d275600e1735a4945ff6f4b66a46a2b2310a6f3a93aaddc0d9a1c193808400000000c0";

    assertThat(contentOf(file)).contains(expectedRlpString);

    assertThat(commandOutput.toString()).isEmpty();
    assertThat(commandErrorOutput.toString()).isEmpty();
  }

  @After
  public void restoreStdin() {
    System.setIn(System.in);
  }
}
