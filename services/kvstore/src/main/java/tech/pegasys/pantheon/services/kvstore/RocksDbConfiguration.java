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
package tech.pegasys.pantheon.services.kvstore;

import java.nio.file.Path;

import picocli.CommandLine;

public class RocksDbConfiguration {

  private final Path databaseDir;
  private final int maxOpenFiles;
  private final long cacheCapacity;
  private final String label;
  private final int maxBackgroundCompactions;
  private final int backgroundThreadCount;
  private final long writeBufferSize;
  private final int writeBuffersMax;
  private final int witeBuffersToMergeMin;

  public RocksDbConfiguration(
      final Path databaseDir,
      final int maxOpenFiles,
      final int maxBackgroundCompactions,
      final int backgroundThreadCount,
      final long cacheCapacity,
      final String label,
      final long writeBufferSize,
      final int writeBuffersMax,
      final int witeBuffersToMergeMin) {
    this.maxBackgroundCompactions = maxBackgroundCompactions;
    this.backgroundThreadCount = backgroundThreadCount;
    this.databaseDir = databaseDir;
    this.maxOpenFiles = maxOpenFiles;
    this.cacheCapacity = cacheCapacity;
    this.label = label;
    this.writeBufferSize = writeBufferSize;
    this.writeBuffersMax = writeBuffersMax;
    this.witeBuffersToMergeMin = witeBuffersToMergeMin;
  }

  public Path getDatabaseDir() {
    return databaseDir;
  }

  public int getMaxOpenFiles() {
    return maxOpenFiles;
  }

  public int getMaxBackgroundCompactions() {
    return maxBackgroundCompactions;
  }

  public int getBackgroundThreadCount() {
    return backgroundThreadCount;
  }

  public long getCacheCapacity() {
    return cacheCapacity;
  }

  public String getLabel() {
    return label;
  }

  public long getWriteBufferSize() {
    return writeBufferSize;
  }

  public int getWriteBuffersMax() {
    return writeBuffersMax;
  }

  public int getWiteBuffersToMergeMin() {
    return witeBuffersToMergeMin;
  }

  public static class Builder {

    Path databaseDir;
    String label = "blockchain";

    @CommandLine.Option(
        names = {"--Xrocksdb-max-open-files"},
        hidden = true,
        defaultValue = "1024",
        paramLabel = "<INTEGER>",
        description = "Max number of files RocksDB will open (default: ${DEFAULT-VALUE})")
    int maxOpenFiles;

    @CommandLine.Option(
        names = {"--Xrocksdb-cache-capacity"},
        hidden = true,
        defaultValue = "3221225472",
        paramLabel = "<LONG>",
        description = "Cache capacity of RocksDB (default: ${DEFAULT-VALUE})")
    long cacheCapacity;

    @CommandLine.Option(
        names = {"--Xrocksdb-write-buffer-size"},
        hidden = true,
        defaultValue = "67108864",
        paramLabel = "<LONG>",
        description = "Size of the WriteBuffer in bytes for RocksDB (default: ${DEFAULT-VALUE})")
    long writeBufferSize;

    @CommandLine.Option(
        names = {"--Xrocksdb-write-buffers-max"},
        hidden = true,
        defaultValue = "6",
        paramLabel = "<INTEGER>",
        description = "Maximum number of write buffers for RocksDB (default: ${DEFAULT-VALUE})")
    int writeBuffersMax;

    @CommandLine.Option(
        names = {"--Xrocksdb-write-buffers-to-merge-min"},
        hidden = true,
        defaultValue = "2",
        paramLabel = "<INTEGER>",
        description =
            "Minumum number of write buffers to merge RocksDB (default: ${DEFAULT-VALUE})")
    int writeBuffersToMergeMin;

    @CommandLine.Option(
        names = {"--Xrocksdb-max-background-compactions"},
        hidden = true,
        defaultValue = "4",
        paramLabel = "<INTEGER>",
        description =
            "Maximum number of RocksDB background compactions (default: ${DEFAULT-VALUE})")
    int maxBackgroundCompactions;

    @CommandLine.Option(
        names = {"--Xrocksdb-background-thread-count"},
        hidden = true,
        defaultValue = "4",
        paramLabel = "<INTEGER>",
        description = "Number of RocksDB background threads (default: ${DEFAULT-VALUE})")
    int backgroundThreadCount;

    public Builder databaseDir(final Path databaseDir) {
      this.databaseDir = databaseDir;
      return this;
    }

    public Builder maxOpenFiles(final int maxOpenFiles) {
      this.maxOpenFiles = maxOpenFiles;
      return this;
    }

    public Builder label(final String label) {
      this.label = label;
      return this;
    }

    public Builder cacheCapacity(final long cacheCapacity) {
      this.cacheCapacity = cacheCapacity;
      return this;
    }

    public Builder writeBufferSize(final long writeBufferSize) {
      this.writeBufferSize = writeBufferSize;
      return this;
    }

    public Builder writeBuffersMax(final int writeBuffersMax) {
      this.writeBuffersMax = writeBuffersMax;
      return this;
    }

    public Builder writeBuffersToMergeMin(final int writeBuffersToMergeMin) {
      this.writeBuffersToMergeMin = writeBuffersToMergeMin;
      return this;
    }

    public Builder maxBackgroundCompactions(final int maxBackgroundCompactions) {
      this.maxBackgroundCompactions = maxBackgroundCompactions;
      return this;
    }

    public Builder backgroundThreadCount(final int backgroundThreadCount) {
      this.backgroundThreadCount = backgroundThreadCount;
      return this;
    }

    public RocksDbConfiguration build() {
      return new RocksDbConfiguration(
          databaseDir,
          maxOpenFiles,
          maxBackgroundCompactions,
          backgroundThreadCount,
          cacheCapacity,
          label,
          writeBufferSize,
          writeBuffersMax,
          writeBuffersToMergeMin);
    }
  }
}
