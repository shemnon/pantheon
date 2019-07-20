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
package tech.pegasys.pantheon.ethereum.retesteth.methods;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.retesteth.RetestethContext;
import tech.pegasys.pantheon.ethereum.util.RawBlockIterator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

class RetestethFixtures {

  static RetestethContext context;

  static {
    try {
      final String genesisJson =
          Resources.toString(
              RetestethFixtures.class.getResource("jsonRpcTestGenesis.json"), Charsets.UTF_8);
      context = new RetestethContext();
      context.resetContext(genesisJson);

      final URL blocksUrl = RetestethFixtures.class.getResource("jsonRpcTestBlockchain.blocks");
      final BlockImporter<Void> blockImporter = context.getProtocolSpec(0).getBlockImporter();
      try (final RawBlockIterator iterator =
          new RawBlockIterator(
              Paths.get(blocksUrl.toURI()),
              rlp ->
                  BlockHeader.readFrom(
                      rlp,
                      ScheduleBasedBlockHeaderFunctions.create(context.getProtocolSchedule())))) {
        while (iterator.hasNext()) {
          blockImporter.importBlock(
              context.getProtocolContext(),
              iterator.next(),
              HeaderValidationMode.NONE,
              HeaderValidationMode.NONE);
        }
      }
    } catch (final URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
