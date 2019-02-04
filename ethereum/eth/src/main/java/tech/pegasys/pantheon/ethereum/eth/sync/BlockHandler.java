package tech.pegasys.pantheon.ethereum.eth.sync;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BlockHandler<B> {
  CompletableFuture<List<B>> downloadBlocks(final List<BlockHeader> headers);

  CompletableFuture<List<B>> validateAndImportBlocks(final List<B> blocks);
}
