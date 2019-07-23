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
package tech.pegasys.pantheon.ethereum.worldstate;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.DefaultMutableBlockchain;
import tech.pegasys.pantheon.ethereum.chain.GenesisState;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStoragePrefixedKeyBlockchainStorage;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.WorldStateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.util.RawBlockIterator;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DebuggableMutableWorldStateTest {

  @ClassRule public static final TemporaryFolder folder = new TemporaryFolder();

  private static ProtocolSchedule<Void> PROTOCOL_SCHEDULE;
  private static Block GENESIS_BLOCK;
  private static List<Block> BLOCKS;
  private static String GENESIS_JSON;

  @BeforeClass
  public static void setupBlockchain() throws Exception {
    PROTOCOL_SCHEDULE = MainnetProtocolSchedule.create();
    final URL blocksUrl =
        ensureFileUrl(
            DebuggableMutableWorldStateTest.class
                .getClassLoader()
                .getResource("testBlockchain.blocks"));

    final URL genesisJsonUrl =
        ensureFileUrl(
            DebuggableMutableWorldStateTest.class.getClassLoader().getResource("testGenesis.json"));

    BLOCKS = new ArrayList<>();
    try (final RawBlockIterator iterator =
        new RawBlockIterator(
            Paths.get(blocksUrl.toURI()),
            rlp -> BlockHeader.readFrom(rlp, new MainnetBlockHeaderFunctions()))) {
      while (iterator.hasNext()) {
        BLOCKS.add(iterator.next());
      }
    }

    GENESIS_JSON = Resources.toString(genesisJsonUrl, Charsets.UTF_8);

    GENESIS_BLOCK = BLOCKS.get(0);
  }

  /** Take a resource URL and if needed copy it to a temp file and return that URL. */
  private static URL ensureFileUrl(final URL resource) throws Exception {
    assertThat(resource).isNotNull();
    try {
      Paths.get(resource.toURI());
    } catch (final FileSystemNotFoundException e) {
      final File target = folder.newFile();
      Files.copy(resource.openStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return target.toURI().toURL();
    }
    return resource;
  }

  @Test
  public void verifyAccountHashes() {
    final InMemoryKeyValueStorage keyValueStorage = new InMemoryKeyValueStorage();
    final MutableBlockchain blockchain =
        new DefaultMutableBlockchain(
            GENESIS_BLOCK,
            new KeyValueStoragePrefixedKeyBlockchainStorage(
                keyValueStorage, new MainnetBlockHeaderFunctions()),
            new NoOpMetricsSystem());

    final GenesisState genesisState = GenesisState.fromJson(GENESIS_JSON, PROTOCOL_SCHEDULE);

    final DebuggableWorldStateArchive worldStateArchive =
        new DebuggableWorldStateArchive(
            new WorldStateKeyValueStorage(new InMemoryKeyValueStorage()));
    final MutableWorldState worldState = worldStateArchive.getMutable();
    assertThat(worldState).isInstanceOf(DebuggableMutableWorldState.class);

    genesisState.writeStateTo(worldState);

    final ProtocolContext<Void> protocolContext =
        new ProtocolContext<Void>(blockchain, worldStateArchive, null);

    final ProtocolSpec<Void> protocolSpec = PROTOCOL_SCHEDULE.getByBlockNumber(1);
    final BlockImporter<Void> blockImporter = protocolSpec.getBlockImporter();
    for (final Block block : BLOCKS) {
      assertThat(blockImporter.importBlock(protocolContext, block, HeaderValidationMode.NONE))
          .isTrue();
    }

    assertThat(worldState.streamAccounts().map(Account::getAddress).map(Address::toString))
        .containsOnly("0xa94f5374fce5edbc8e2a8697c15331677e6ebf0b");

    assertThat(
            ((DebuggableMutableWorldState) worldState)
                .getPreimages().entrySet().stream()
                    .map(
                        entry ->
                            entry.getKey().toUnprefixedString()
                                + "/"
                                + entry.getValue().toUnprefixedString()))
        .containsOnly(
            "c2575a0e9e593c00f959f8c92f12db2869c3395a3b0502d05e2516446f71f85b/0000000000000000000000000000000000000000000000000000000000000003",
            "15b040a190663dd210787de31621bae9fb17d6b59d1cd1d319c0ed452d7a0f15/bcde5374fce5edbc8e2a8697c15331677e6ebf0b",
            "8a35acfbc15ff81a39ae7d344fd709f28e8600b4aa8c65c6b64bfe7fe36bd19b/0000000000000000000000000000000000000000000000000000000000000004",
            "eb8ec137a2f5a74ec3a73144b552caad890b18b5f725872fa212fff6d4d565ba/6295ee1b4f6dd65047762f924ecd367c17eabf8f",
            "405787fa12a823e0f2b7631cc41b3ba8828b3321ca811111fa75cd3aa3bb5ace/0000000000000000000000000000000000000000000000000000000000000002",
            "03601462093b5945d1676df093446790fd31b20e7b12a2e8e5e09d068109616b/a94f5374fce5edbc8e2a8697c15331677e6ebf0b",
            "a32fbb2bcac38ed596cbb34ae265df4d60b832ce8077a3abc6f57b4611005cfd/8888f1f195afa192cfee860698584c030f4c9db1",
            "290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e563/0000000000000000000000000000000000000000000000000000000000000000",
            "b10e2d527612073b26eecdfd717e6a320cf44b4afac2b0732d9fcbe2b7fa0cf6/0000000000000000000000000000000000000000000000000000000000000001");
  }
}
