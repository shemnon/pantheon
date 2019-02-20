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
package tech.pegasys.pantheon.cli;

import static com.google.common.base.Preconditions.checkNotNull;
import static tech.pegasys.pantheon.cli.DefaultCommandValues.MANDATORY_FILE_FORMAT_HELP;
import static tech.pegasys.pantheon.cli.PasswordSubCommand.COMMAND_NAME;

import io.vertx.core.Vertx;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCrypt;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import tech.pegasys.pantheon.cli.PasswordSubCommand.HashSubCommand;
import tech.pegasys.pantheon.cli.RLPSubCommand.EncodeSubCommand;
import tech.pegasys.pantheon.consensus.common.ConsensusHelpers;
import tech.pegasys.pantheon.consensus.ibft.IbftExtraData;
import tech.pegasys.pantheon.metrics.prometheus.MetricsConfiguration;
import tech.pegasys.pantheon.metrics.prometheus.MetricsService;

@Command(
    name = COMMAND_NAME,
    description = "This command provides password related actions.",
    mixinStandardHelpOptions = true,
    subcommands = {EncodeSubCommand.class})
class RLPSubCommand implements Runnable {

  static final String COMMAND_NAME = "rlp";

  @SuppressWarnings("unused")
  @ParentCommand
  private PantheonCommand parentCommand;

  @SuppressWarnings("unused")
  @Spec
  private CommandSpec spec;

  final PrintStream out;

  RLPSubCommand(final PrintStream out) {
    this.out = out;
  }

  @Override
  public void run() {
    spec.commandLine().usage(out);
  }

  /**
   * RLP encode sub-command
   *
   * <p>Encode a JSON data into an RLP hex string.
   */
  @Command(
      name = "encode",
      description = "This command encodes a JSON typed data from a file into an RLP hex string.",
      mixinStandardHelpOptions = true)
  static class EncodeSubCommand implements Runnable {
    @SuppressWarnings("unused")
    @ParentCommand
    private BlocksSubCommand parentCommand; // Picocli injects reference to parent command

    @Option(
        names = "--type",
        required = true,
        description = "Type of the data, possible values are ${COMPLETION-CANDIDATES}. "
            + "(default: ${DEFAULT-VALUE})" )
    private final RLPType type = RLPType.IBFT_EXTRA_DATA;

    @Option(
        names = "--from",
        required = true,
        paramLabel = MANDATORY_FILE_FORMAT_HELP,
        description = "File containing blocks to import",
        arity = "1..1")
    private final File jsonSourceFile = null;

    @Override
    public void run() {

      checkNotNull(parentCommand);

      try {
        // As jsonSourceFile even if initialized as null is injected by PicoCLI and param is
        // mandatory
        // So we are sure it's always not null, we can remove the warning
        //noinspection ConstantConditions
        final Path path = jsonSourceFile.toPath();

        parentCommand.blockImporter.importBlockchain(
            path, parentCommand.parentCommand.buildController());


        final IbftExtraData extraData =
            new IbftExtraData(
                ConsensusHelpers.zeroLeftPad(vanityData, IbftExtraData.EXTRA_VANITY_LENGTH),
                Collections.emptyList(),
                toVote(proposal),
                round,
                validators);

        return extraData.encode();
      } catch (final FileNotFoundException e) {
        throw new ExecutionException(
            new CommandLine(this), "Could not find file to import: " + blocksImportFile);
      } catch (final IOException e) {
        throw new ExecutionException(
            new CommandLine(this), "Unable to import blocks from " + blocksImportFile, e);
    }
  }
}
