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
package tech.pegasys.pantheon.ethereum.eth.sync.fullsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThatObject;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static tech.pegasys.pantheon.ethereum.core.InMemoryStorageProvider.createInMemoryBlockchain;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockBody;
import tech.pegasys.pantheon.ethereum.core.BlockDataGenerator;
import tech.pegasys.pantheon.ethereum.core.BlockDataGenerator.BlockOptions;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.TransactionReceipt;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManager;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManagerTestUtil;
import tech.pegasys.pantheon.ethereum.eth.manager.EthScheduler;
import tech.pegasys.pantheon.ethereum.eth.manager.RespondingEthPeer;
import tech.pegasys.pantheon.ethereum.eth.manager.RespondingEthPeer.Responder;
import tech.pegasys.pantheon.ethereum.eth.manager.ethtaskutils.BlockchainSetupUtil;
import tech.pegasys.pantheon.ethereum.eth.messages.EthPV62;
import tech.pegasys.pantheon.ethereum.eth.messages.GetBlockHeadersMessage;
import tech.pegasys.pantheon.ethereum.eth.sync.ChainDownloader;
import tech.pegasys.pantheon.ethereum.eth.sync.EthTaskChainDownloader;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncState;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.p2p.api.MessageData;
import tech.pegasys.pantheon.ethereum.p2p.wire.messages.DisconnectMessage.DisconnectReason;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FullSyncChainDownloaderTest {

  @Parameter public boolean usePipelineDownloader;

  @Parameter(1)
  public String name;

  @Parameters(name = "{1}")
  public static Object[][] params() {
    return new Object[][] {{false, "EthTask"}, {true, "Pipeline"}};
  }

  protected ProtocolSchedule<Void> protocolSchedule;
  protected EthProtocolManager ethProtocolManager;
  protected EthContext ethContext;
  protected ProtocolContext<Void> protocolContext;
  private SyncState syncState;

  private BlockDataGenerator gen;
  private BlockchainSetupUtil<Void> localBlockchainSetup;
  protected MutableBlockchain localBlockchain;
  private BlockchainSetupUtil<Void> otherBlockchainSetup;
  protected Blockchain otherBlockchain;
  private final MetricsSystem metricsSystem = new NoOpMetricsSystem();

  @Before
  public void setupTest() {
    gen = new BlockDataGenerator();
    localBlockchainSetup = BlockchainSetupUtil.forTesting();
    localBlockchain = spy(localBlockchainSetup.getBlockchain());
    otherBlockchainSetup = BlockchainSetupUtil.forTesting();
    otherBlockchain = otherBlockchainSetup.getBlockchain();

    protocolSchedule = localBlockchainSetup.getProtocolSchedule();
    protocolContext = localBlockchainSetup.getProtocolContext();
    ethProtocolManager =
        EthProtocolManagerTestUtil.create(
            localBlockchain,
            localBlockchainSetup.getWorldArchive(),
            new EthScheduler(1, 1, 1, new NoOpMetricsSystem()));
    ethContext = ethProtocolManager.ethContext();
    syncState = new SyncState(protocolContext.getBlockchain(), ethContext.getEthPeers());
  }

  private ChainDownloader downloader(final SynchronizerConfiguration syncConfig) {
    return FullSyncChainDownloader.create(
        syncConfig, protocolSchedule, protocolContext, ethContext, syncState, metricsSystem);
  }

  private ChainDownloader downloader() {
    final SynchronizerConfiguration syncConfig = syncConfigBuilder().build();
    return downloader(syncConfig);
  }

  private SynchronizerConfiguration.Builder syncConfigBuilder() {
    return SynchronizerConfiguration.builder()
        .piplineDownloaderForFullSyncEnabled(usePipelineDownloader);
  }

  @Test
  public void syncsToBetterChain_multipleSegments() {
    otherBlockchainSetup.importFirstBlocks(15);
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(10).build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    peer.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peer.getEthPeer());

    peer.respondWhileOtherThreadsWork(
        responder, () -> localBlockchain.getChainHeadBlockNumber() < targetBlock);

    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(targetBlock);
  }

  @Test
  public void syncsToBetterChain_singleSegment() {
    otherBlockchainSetup.importFirstBlocks(5);
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(10).build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    peer.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peer.getEthPeer());

    peer.respondWhileOtherThreadsWork(
        responder, () -> localBlockchain.getChainHeadBlockNumber() < targetBlock);

    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(targetBlock);
  }

  @Test
  public void syncsToBetterChain_singleSegmentOnBoundary() {
    otherBlockchainSetup.importFirstBlocks(5);
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(4).build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    peer.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peer.getEthPeer());

    peer.respondWhileOtherThreadsWork(
        responder, () -> localBlockchain.getChainHeadBlockNumber() < targetBlock);

    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(targetBlock);
  }

  @Test
  public void doesNotSyncToWorseChain() {
    localBlockchainSetup.importFirstBlocks(15);
    // Sanity check
    assertThat(localBlockchain.getChainHeadBlockNumber())
        .isGreaterThan(BlockHeader.GENESIS_BLOCK_NUMBER);

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final ChainDownloader downloader = downloader();
    downloader.start();

    peer.respond(responder);
    assertThat(syncState.syncTarget()).isNotPresent();

    peer.respondWhileOtherThreadsWork(responder, peer::hasOutstandingRequests);

    assertThat(syncState.syncTarget()).isNotPresent();
    verify(localBlockchain, times(0)).appendBlock(any(), any());
  }

  @Test
  public void syncsToBetterChain_fromFork() {
    otherBlockchainSetup.importFirstBlocks(15);
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();

    // Add divergent blocks to local chain
    localBlockchainSetup.importFirstBlocks(3);
    gen = new BlockDataGenerator();
    final Block chainHead = localBlockchain.getChainHeadBlock();
    final Block forkBlock =
        gen.block(gen.nextBlockOptions(chainHead).setDifficulty(UInt256.of(0L)));
    localBlockchain.appendBlock(forkBlock, gen.receipts(forkBlock));

    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());
    assertThat(otherBlockchain.contains(localBlockchain.getChainHead().getHash())).isFalse();

    final RespondingEthPeer peer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(10).build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    peer.respondWhileOtherThreadsWork(
        responder,
        () ->
            localBlockchain.getChainHeadBlockNumber() < targetBlock
                || syncState.syncTarget().isPresent());

    // Synctarget should not exist as chain has fully downloaded.
    assertThat(syncState.syncTarget().isPresent()).isFalse();
    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(targetBlock);
  }

  @Test
  public void choosesBestPeerAsSyncTarget_byTd() {
    final UInt256 localTd = localBlockchain.getChainHead().getTotalDifficulty();

    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);
    final RespondingEthPeer peerA =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100));
    final RespondingEthPeer peerB =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(200));

    final ChainDownloader downloader = downloader();
    downloader.start();

    // Process until the sync target is selected
    while (!syncState.syncTarget().isPresent()) {
      RespondingEthPeer.respondOnce(responder, peerA, peerB);
    }
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peerB.getEthPeer());
  }

  @Test
  public void choosesBestPeerAsSyncTarget_byTdAndHeight() {
    final UInt256 localTd = localBlockchain.getChainHead().getTotalDifficulty();

    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);
    final RespondingEthPeer peerA =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100), 100);
    final RespondingEthPeer peerB =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(200), 50);

    final ChainDownloader downloader = downloader();
    downloader.start();

    // Process until the sync target is selected
    while (!syncState.syncTarget().isPresent()) {
      RespondingEthPeer.respondOnce(responder, peerA, peerB);
    }
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peerB.getEthPeer());
  }

  @Test
  public void switchesSyncTarget_betterHeight() {
    final UInt256 localTd = localBlockchain.getChainHead().getTotalDifficulty();
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    // Peer A is initially better
    final RespondingEthPeer peerA =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100), 60);
    final RespondingEthPeer peerB =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100), 50);

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder()
            .downloaderChainSegmentSize(5)
            .downloaderChangeTargetThresholdByHeight(10)
            .build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    // Process until the sync target is selected
    peerA.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peerA.getEthPeer());

    // Update Peer B so that its a better target and send some responses to push logic forward
    peerB.getEthPeer().chainState().update(gen.hash(), 100);

    processUntilSyncTargetChecked(responder, downloader, peerA, peerB);

    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peerB.getEthPeer());
  }

  @Test
  public void doesNotSwitchSyncTarget_betterHeightUnderThreshold() {
    otherBlockchainSetup.importFirstBlocks(8);
    final UInt256 localTd = localBlockchain.getChainHead().getTotalDifficulty();
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(200));
    final RespondingEthPeer otherPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100));

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder()
            .downloaderChainSegmentSize(5)
            .downloaderChangeTargetThresholdByHeight(1000)
            .build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    // Process until the sync target is selected
    bestPeer.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(bestPeer.getEthPeer());

    // Update otherPeer so that its a better target, but under the threshold to switch
    otherPeer.getEthPeer().chainState().update(gen.hash(), 100);

    processUntilSyncTargetChecked(responder, downloader, bestPeer, otherPeer);

    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(bestPeer.getEthPeer());
  }

  @Test
  public void switchesSyncTarget_betterTd() {
    final UInt256 localTd = localBlockchain.getChainHead().getTotalDifficulty();
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    // Peer A is initially better
    final RespondingEthPeer peerA =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(200));
    final RespondingEthPeer peerB =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100));

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder()
            .downloaderChainSegmentSize(5)
            .downloaderChangeTargetThresholdByTd(UInt256.of(10))
            .build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    // Process until the sync target is selected
    peerA.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peerA.getEthPeer());

    // Update Peer B so that its a better target and send some responses to push logic forward
    peerB
        .getEthPeer()
        .chainState()
        .updateForAnnouncedBlock(
            gen.header(), localBlockchain.getChainHead().getTotalDifficulty().plus(300));

    processUntilSyncTargetChecked(responder, downloader, peerA, peerB);

    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(peerB.getEthPeer());
  }

  @Test
  public void doesNotSwitchSyncTarget_betterTdUnderThreshold() {
    final long localChainHeadAtStart = localBlockchain.getChainHeadBlockNumber();
    final UInt256 localTd = localBlockchain.getChainHead().getTotalDifficulty();
    otherBlockchainSetup.importFirstBlocks(8);
    final Responder responder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    // Sanity check
    assertThat(localChainHeadAtStart).isLessThan(otherBlockchain.getChainHeadBlockNumber());

    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(200));
    final RespondingEthPeer otherPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, localTd.plus(100));

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder()
            .downloaderChainSegmentSize(5)
            .downloaderChangeTargetThresholdByTd(UInt256.of(100_000_000L))
            .build();
    final ChainDownloader downloader = downloader(syncConfig);
    downloader.start();

    // Process until the sync target is selected
    bestPeer.respondWhileOtherThreadsWork(responder, () -> !syncState.syncTarget().isPresent());
    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(bestPeer.getEthPeer());

    // Update otherPeer so that its a better target and send some responses to push logic forward
    final BlockHeader newBestBlock =
        gen.header(1000, new BlockOptions().setDifficulty(UInt256.ZERO));
    bestPeer.getEthPeer().chainState().updateForAnnouncedBlock(newBestBlock, localTd.plus(201));
    otherPeer.getEthPeer().chainState().updateForAnnouncedBlock(newBestBlock, localTd.plus(300));

    processUntilSyncTargetChecked(responder, downloader, bestPeer, otherPeer);

    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(bestPeer.getEthPeer());
  }

  @Test
  public void recoversFromSyncTargetDisconnect() {
    localBlockchainSetup.importFirstBlocks(2);
    final long localChainHeadAtStart = localBlockchain.getChainHeadBlockNumber();
    otherBlockchainSetup.importAllBlocks();
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(5).downloaderHeadersRequestSize(3).build();
    final ChainDownloader downloader = downloader(syncConfig);

    final long bestPeerChainHead = otherBlockchain.getChainHeadBlockNumber();
    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final long secondBestPeerChainHead = bestPeerChainHead - 3;
    final Blockchain shorterChain = createShortChain(otherBlockchain, secondBestPeerChainHead);
    final RespondingEthPeer secondBestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, shorterChain);
    final Responder bestResponder = RespondingEthPeer.blockchainResponder(otherBlockchain);
    final Responder secondBestResponder = RespondingEthPeer.blockchainResponder(shorterChain);
    downloader.start();

    // Process through sync target selection
    bestPeer.respondWhileOtherThreadsWork(bestResponder, () -> !syncState.syncTarget().isPresent());

    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(bestPeer.getEthPeer());

    // The next message should be for checkpoint headers from the sync target
    final Optional<MessageData> maybeNextMessage = bestPeer.peekNextOutgoingRequest();
    assertThat(maybeNextMessage).isPresent();
    final MessageData nextMessage = maybeNextMessage.get();
    assertThat(nextMessage.getCode()).isEqualTo(EthPV62.GET_BLOCK_HEADERS);
    final GetBlockHeadersMessage headersMessage = GetBlockHeadersMessage.readFrom(nextMessage);
    assertThat(headersMessage.skip()).isGreaterThan(0);

    // Process through the first import
    await()
        .atMost(10, TimeUnit.SECONDS)
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .untilAsserted(
            () -> {
              if (!bestPeer.respond(bestResponder)) {
                secondBestPeer.respond(secondBestResponder);
              }
              assertThat(localBlockchain.getChainHeadBlockNumber())
                  .isNotEqualTo(localChainHeadAtStart);
            });

    // Sanity check that we haven't already passed the second best peer
    assertThat(localBlockchain.getChainHeadBlockNumber()).isLessThan(secondBestPeerChainHead);

    // Disconnect peer
    ethProtocolManager.handleDisconnect(
        bestPeer.getPeerConnection(), DisconnectReason.TOO_MANY_PEERS, true);

    // Downloader should recover and sync to next best peer, but it may stall
    // for 10 seconds first (by design).
    secondBestPeer.respondWhileOtherThreadsWork(
        secondBestResponder,
        () -> localBlockchain.getChainHeadBlockNumber() != secondBestPeerChainHead);

    assertThat(localBlockchain.getChainHeadBlockNumber()).isEqualTo(secondBestPeerChainHead);
  }

  @Test
  public void requestsCheckpointsFromSyncTarget() {
    localBlockchainSetup.importFirstBlocks(2);
    otherBlockchainSetup.importAllBlocks();
    final long targetBlock = otherBlockchain.getChainHeadBlockNumber();
    // Sanity check
    assertThat(targetBlock).isGreaterThan(localBlockchain.getChainHeadBlockNumber());

    final SynchronizerConfiguration syncConfig =
        syncConfigBuilder().downloaderChainSegmentSize(5).downloaderHeadersRequestSize(3).build();
    final ChainDownloader downloader = downloader(syncConfig);

    // Setup the best peer we should use as our sync target
    final long bestPeerChainHead = otherBlockchain.getChainHeadBlockNumber();
    final RespondingEthPeer bestPeer =
        EthProtocolManagerTestUtil.createPeer(ethProtocolManager, otherBlockchain);
    final Responder bestResponder = RespondingEthPeer.blockchainResponder(otherBlockchain);

    // Create some other peers that are available to sync from
    final int otherPeersCount = 5;
    final List<RespondingEthPeer> otherPeers = new ArrayList<>(otherPeersCount);
    final long otherChainhead = bestPeerChainHead - 3;
    final Blockchain shorterChain = createShortChain(otherBlockchain, otherChainhead);
    final Responder otherResponder = RespondingEthPeer.blockchainResponder(shorterChain);
    for (int i = 0; i < otherPeersCount; i++) {
      final RespondingEthPeer otherPeer =
          EthProtocolManagerTestUtil.createPeer(ethProtocolManager, shorterChain);
      otherPeers.add(otherPeer);
    }

    downloader.start();

    // Process through sync target selection
    await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              bestPeer.respond(bestResponder);
              assertThat(syncState.syncTarget()).isNotEmpty();
            });

    assertThat(syncState.syncTarget()).isPresent();
    assertThat(syncState.syncTarget().get().peer()).isEqualTo(bestPeer.getEthPeer());

    int count = 0;
    while (localBlockchain.getChainHeadBlockNumber() < bestPeerChainHead) {
      if (count > 10_000) {
        fail("Did not reach chain head soon enough");
      }
      count++;
      // Check that any requests for checkpoint headers are only sent to the best peer
      final long checkpointRequestsToOtherPeers =
          otherPeers.stream()
              .map(RespondingEthPeer::pendingOutgoingRequests)
              .flatMap(Function.identity())
              .filter(m -> m.getCode() == EthPV62.GET_BLOCK_HEADERS)
              .map(GetBlockHeadersMessage::readFrom)
              .filter(m -> m.skip() > 0)
              .count();
      assertThat(checkpointRequestsToOtherPeers).isEqualTo(0L);

      bestPeer.respond(bestResponder);
      for (final RespondingEthPeer otherPeer : otherPeers) {
        otherPeer.respond(otherResponder);
      }
    }
  }

  private MutableBlockchain createShortChain(
      final Blockchain blockchain, final long truncateAtBlockNumber) {
    final BlockHeader genesisHeader =
        blockchain.getBlockHeader(BlockHeader.GENESIS_BLOCK_NUMBER).get();
    final BlockBody genesisBody = blockchain.getBlockBody(genesisHeader.getHash()).get();
    final Block genesisBlock = new Block(genesisHeader, genesisBody);
    final MutableBlockchain shortChain = createInMemoryBlockchain(genesisBlock);
    long nextBlock = genesisHeader.getNumber() + 1;
    while (nextBlock <= truncateAtBlockNumber) {
      final BlockHeader header = blockchain.getBlockHeader(nextBlock).get();
      final BlockBody body = blockchain.getBlockBody(header.getHash()).get();
      final List<TransactionReceipt> receipts = blockchain.getTxReceipts(header.getHash()).get();
      final Block block = new Block(header, body);
      shortChain.appendBlock(block, receipts);
      nextBlock++;
    }
    return shortChain;
  }

  @SuppressWarnings("unchecked")
  private void processUntilSyncTargetChecked(
      final Responder responder,
      final ChainDownloader rawDownloader,
      final RespondingEthPeer... peers) {
    // This breaks encapsulation and depends on the particular semantics of EthTaskChainDownloader
    // These cases are now handled by unit tests in BetterSyncTargetEvaluatorTest
    assumeThatObject(rawDownloader).isInstanceOf(EthTaskChainDownloader.class);
    final EthTaskChainDownloader<Void> downloader = (EthTaskChainDownloader<Void>) rawDownloader;
    // Process through first task cycle
    final CompletableFuture<?> firstTask = downloader.getCurrentTask();
    while (downloader.getCurrentTask() == firstTask) {
      RespondingEthPeer.respondOnce(responder, peers);
    }
  }
}
