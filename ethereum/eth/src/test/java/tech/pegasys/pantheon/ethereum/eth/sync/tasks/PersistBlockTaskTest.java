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
package tech.pegasys.pantheon.ethereum.eth.sync.tasks;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockDataGenerator;
import tech.pegasys.pantheon.ethereum.eth.manager.ethtaskutils.BlockchainSetupUtil;
import tech.pegasys.pantheon.ethereum.eth.sync.tasks.exceptions.InvalidBlockException;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PersistBlockTaskTest {

  private BlockchainSetupUtil<Void> blockchainUtil;
  private ProtocolSchedule<Void> protocolSchedule;
  private ProtocolContext<Void> protocolContext;
  private MutableBlockchain blockchain;
  private final LabelledMetric<OperationTimer> ethTasksTimer =
      NoOpMetricsSystem.NO_OP_LABELLED_TIMER;

  @Before
  public void setup() {
    blockchainUtil = BlockchainSetupUtil.forTesting();
    protocolSchedule = blockchainUtil.getProtocolSchedule();
    protocolContext = blockchainUtil.getProtocolContext();
    blockchain = blockchainUtil.getBlockchain();
  }

  @Test
  public void importsValidBlock() throws Exception {
    blockchainUtil.importFirstBlocks(3);
    final Block nextBlock = blockchainUtil.getBlock(3);

    // Sanity check
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();

    // Create task
    final PersistBlockTask<Void> task =
        PersistBlockTask.create(
            protocolSchedule, protocolContext, nextBlock, HeaderValidationMode.FULL, ethTasksTimer);
    final CompletableFuture<BlockWithReceipts> result = task.run();

    Awaitility.await().atMost(30, SECONDS).until(result::isDone);

    assertThat(result.isCompletedExceptionally()).isFalse();
    assertThat(result.get().getBlock()).isEqualTo(nextBlock);
    assertThat(blockchain.contains(nextBlock.getHash())).isTrue();
  }

  @Test
  public void failsToImportInvalidBlock() {
    final BlockDataGenerator gen = new BlockDataGenerator();
    blockchainUtil.importFirstBlocks(3);
    final Block nextBlock = gen.block();

    // Sanity check
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();

    // Create task
    final PersistBlockTask<Void> task =
        PersistBlockTask.create(
            protocolSchedule, protocolContext, nextBlock, HeaderValidationMode.FULL, ethTasksTimer);
    final CompletableFuture<BlockWithReceipts> result = task.run();

    Awaitility.await().atMost(30, SECONDS).until(result::isDone);

    assertThat(result.isCompletedExceptionally()).isTrue();
    assertThatThrownBy(result::get).hasCauseInstanceOf(InvalidBlockException.class);
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
  }

  @Test
  public void importsValidBlockSequence() throws Exception {
    blockchainUtil.importFirstBlocks(3);
    final List<BlockWithReceipts> nextBlocks =
        Arrays.asList(
            new BlockWithReceipts(blockchainUtil.getBlock(3), null),
            new BlockWithReceipts(blockchainUtil.getBlock(4), null));

    // Sanity check
    for (final BlockWithReceipts nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getBlock().getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forSequentialBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isFalse();
    assertThat(task.get()).isEqualTo(nextBlocks);
    for (final BlockWithReceipts nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getBlock().getHash())).isTrue();
    }
  }

  @Test
  public void failsToImportInvalidBlockSequenceWhereSecondBlockFails() throws Exception {
    final BlockDataGenerator gen = new BlockDataGenerator();
    blockchainUtil.importFirstBlocks(3);
    final List<BlockWithReceipts> nextBlocks =
        Arrays.asList(
            new BlockWithReceipts(blockchainUtil.getBlock(3), null),
            new BlockWithReceipts(gen.block(), null));

    // Sanity check
    for (final BlockWithReceipts nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getBlock().getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forSequentialBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isTrue();
    assertThatThrownBy(task::get).hasCauseInstanceOf(InvalidBlockException.class);
    assertThat(blockchain.contains(nextBlocks.get(0).getBlock().getHash())).isTrue();
    assertThat(blockchain.contains(nextBlocks.get(1).getBlock().getHash())).isFalse();
  }

  @Test
  public void failsToImportInvalidBlockSequenceWhereFirstBlockFails() throws Exception {
    final BlockDataGenerator gen = new BlockDataGenerator();
    blockchainUtil.importFirstBlocks(3);
    final List<BlockWithReceipts> nextBlocks =
        Arrays.asList(
            new BlockWithReceipts(gen.block(), null),
            new BlockWithReceipts(blockchainUtil.getBlock(3), null));

    // Sanity check
    for (final BlockWithReceipts nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getBlock().getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forSequentialBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isTrue();
    assertThatThrownBy(task::get).hasCauseInstanceOf(InvalidBlockException.class);
    assertThat(blockchain.contains(nextBlocks.get(0).getBlock().getHash())).isFalse();
    assertThat(blockchain.contains(nextBlocks.get(1).getBlock().getHash())).isFalse();
  }

  @Test
  public void importsValidUnorderedBlocks() throws Exception {
    blockchainUtil.importFirstBlocks(3);
    final Block valid = blockchainUtil.getBlock(3);
    final BlockWithReceipts validWithReceipts = new BlockWithReceipts(valid, null);
    final List<Block> nextBlocks = Collections.singletonList(valid);

    // Sanity check
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forUnorderedBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isFalse();
    assertThat(task.get().size()).isEqualTo(1);
    assertThat(task.get().contains(validWithReceipts)).isTrue();
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isTrue();
    }
  }

  @Test
  public void importsInvalidUnorderedBlock() throws Exception {
    final BlockDataGenerator gen = new BlockDataGenerator();
    blockchainUtil.importFirstBlocks(3);
    final Block invalid = gen.block();
    final List<Block> nextBlocks = Collections.singletonList(invalid);

    // Sanity check
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forUnorderedBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isTrue();
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
    }
  }

  @Test
  public void importsInvalidUnorderedBlocks() throws Exception {
    final BlockDataGenerator gen = new BlockDataGenerator();
    blockchainUtil.importFirstBlocks(3);
    final List<Block> nextBlocks = Arrays.asList(gen.block(), gen.block());

    // Sanity check
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forUnorderedBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isTrue();
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
    }
  }

  @Test
  public void importsUnorderedBlocksWithMixOfValidAndInvalidBlocks() throws Exception {
    final BlockDataGenerator gen = new BlockDataGenerator();
    blockchainUtil.importFirstBlocks(3);
    final Block valid = blockchainUtil.getBlock(3);
    final BlockWithReceipts validWithReceipt = new BlockWithReceipts(valid, null);
    final Block invalid = gen.block();
    final List<Block> nextBlocks = Arrays.asList(invalid, valid);

    // Sanity check
    for (final Block nextBlock : nextBlocks) {
      assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
    }

    // Create task
    final CompletableFuture<List<BlockWithReceipts>> task =
        PersistBlockTask.forUnorderedBlocks(
                protocolSchedule,
                protocolContext,
                nextBlocks,
                HeaderValidationMode.FULL,
                ethTasksTimer)
            .get();

    Awaitility.await().atMost(30, SECONDS).until(task::isDone);

    assertThat(task.isCompletedExceptionally()).isFalse();
    assertThat(task.get().size()).isEqualTo(1);
    assertThat(task.get().contains(validWithReceipt)).isTrue();
    assertThat(blockchain.contains(valid.getHash())).isTrue();
    assertThat(blockchain.contains(invalid.getHash())).isFalse();
  }

  @Test
  public void cancelBeforeRunning() throws Exception {
    blockchainUtil.importFirstBlocks(3);
    final Block nextBlock = blockchainUtil.getBlock(3);

    // Sanity check
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();

    // Create task
    final PersistBlockTask<Void> task =
        PersistBlockTask.create(
            protocolSchedule, protocolContext, nextBlock, HeaderValidationMode.FULL, ethTasksTimer);

    task.cancel();
    final CompletableFuture<BlockWithReceipts> result = task.run();

    assertThat(result.isCancelled()).isTrue();
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
  }

  @Test
  public void cancelAfterRunning() throws Exception {
    blockchainUtil.importFirstBlocks(3);
    final Block nextBlock = blockchainUtil.getBlock(3);

    // Sanity check
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();

    // Create task
    final PersistBlockTask<Void> task =
        PersistBlockTask.create(
            protocolSchedule, protocolContext, nextBlock, HeaderValidationMode.FULL, ethTasksTimer);
    final PersistBlockTask<Void> taskSpy = Mockito.spy(task);
    Mockito.doNothing().when(taskSpy).executeTaskTimed();

    final CompletableFuture<BlockWithReceipts> result = taskSpy.run();
    taskSpy.cancel();

    assertThat(result.isCancelled()).isTrue();
    assertThat(blockchain.contains(nextBlock.getHash())).isFalse();
  }
}
