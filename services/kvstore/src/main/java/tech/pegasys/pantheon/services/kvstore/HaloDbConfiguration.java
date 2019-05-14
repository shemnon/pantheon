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

public class HaloDbConfiguration {

  private final boolean enable;
  private final Path databaseDir;
  private final String label;
  private final int maxFileSize;
  private final long flushDataSizeBytes;
  private final double compationThresholdPerFile;
  private final int compactionJobRate;
  private final int numberOfRecords;
  private final boolean cleanUpTombstonesDuringOpen;
  private final boolean cleanUpInMemoryIndexOnClose;
  private final boolean useMemoryPool;
  private final int memoryPoolChunkSize;
  private final int fixedKeySize;

  public HaloDbConfiguration(
      final boolean enable,
      final Path databaseDir,
      final String label,
      final int maxFileSize,
      final long flushDataSizeBytes,
      final double compationThresholdPerFile,
      final int compactionJobRate,
      final int numberOfRecords,
      final boolean cleanUpTombstonesDuringOpen,
      final boolean cleanUpInMemoryIndexOnClose,
      final boolean useMemoryPool,
      final int memoryPoolChunkSize,
      final int fixedKeySize) {
    this.enable = enable;
    this.databaseDir = databaseDir;
    this.label = label;
    this.maxFileSize = maxFileSize;
    this.flushDataSizeBytes = flushDataSizeBytes;
    this.compationThresholdPerFile = compationThresholdPerFile;
    this.compactionJobRate = compactionJobRate;
    this.numberOfRecords = numberOfRecords;
    this.cleanUpTombstonesDuringOpen = cleanUpTombstonesDuringOpen;
    this.cleanUpInMemoryIndexOnClose = cleanUpInMemoryIndexOnClose;
    this.useMemoryPool = useMemoryPool;
    this.memoryPoolChunkSize = memoryPoolChunkSize;
    this.fixedKeySize = fixedKeySize;
  }

  public boolean isEnabled() {
    return enable;
  }

  public Path getDatabaseDir() {
    return databaseDir;
  }

  public String getLabel() {
    return label;
  }

  public int getMaxFileSize() {
    return maxFileSize;
  }

  public long getFlushDataSizeBytes() {
    return flushDataSizeBytes;
  }

  public double getCompationThresholdPerFile() {
    return compationThresholdPerFile;
  }

  public int getCompactionJobRate() {
    return compactionJobRate;
  }

  public int getNumberOfRecords() {
    return numberOfRecords;
  }

  public boolean isCleanUpTombstonesDuringOpen() {
    return cleanUpTombstonesDuringOpen;
  }

  public boolean isCleanUpInMemoryIndexOnClose() {
    return cleanUpInMemoryIndexOnClose;
  }

  public boolean isUseMemoryPool() {
    return useMemoryPool;
  }

  public int getMemoryPoolChunkSize() {
    return memoryPoolChunkSize;
  }

  public int getFixedKeySize() {
    return fixedKeySize;
  }

  public static class Builder {

    Path databaseDir;
    String label = "blockchain";

    @CommandLine.Option(
        names = {"--Xhalodb-enable"},
        hidden = true,
        defaultValue = "false", // 1 GiB
        paramLabel = "<BOOLEAN>",
        description = "Use HaloDB instead of RocksDB(default: ${DEFAULT-VALUE})")
    boolean enable = false;

    @CommandLine.Option(
        names = {"--Xhalodb-max-file-size"},
        hidden = true,
        defaultValue = "1073741824", // 1 GiB
        paramLabel = "<INTEGER>",
        description = "The size of each data file (default: ${DEFAULT-VALUE})")
    int maxFileSize = 1073741824;

    @CommandLine.Option(
        names = {"--Xhalodb-flush-data-size-bytes"},
        hidden = true,
        defaultValue = "10485760", // 10 MiB
        paramLabel = "<LONG>",
        description =
            "the threshold at which page cache is synced to disk (default: ${DEFAULT-VALUE})")
    long flushDataSizeBytes = 10485760;

    @CommandLine.Option(
        names = {"--Xhalodb-compaction-threshold-per-file"},
        hidden = true,
        defaultValue = "0.7",
        paramLabel = "<DOUBLE>",
        description =
            "The percentage of stale data in a data file at which the file will be compacted (default: ${DEFAULT-VALUE})")
    double compationThresholdPerFile = 0.7D;

    @CommandLine.Option(
        names = {"--Xhalodb-compaction-job-rate"},
        hidden = true,
        defaultValue = "52428800", // 50 MiB
        paramLabel = "<DOUBLE>",
        description =
            "Maximum amount of data in bytes which will be copied by the compaction thread per second (default: ${DEFAULT-VALUE})")
    int compactionJobRate = 52428800;

    @CommandLine.Option(
        names = {"--Xhalodb-number-of-records"},
        hidden = true,
        defaultValue = "500000000", // 500 million
        paramLabel = "<INTEGER>",
        description = "Preallocated number of records (default: ${DEFAULT-VALUE})")
    int numberOfRecords = 500_000_000;

    @CommandLine.Option(
        names = {"--Xhalodb-clean-up-tombstones-during-open"},
        hidden = true,
        defaultValue = "true",
        paramLabel = "<BOOLEAN>",
        description =
            "During startup delete outdated tombstone records (default: ${DEFAULT-VALUE})")
    boolean cleanUpTombstonesDuringOpen = true;

    @CommandLine.Option(
        names = {"--Xhalodb-clean-up-in-memory-index-on-close"},
        hidden = true,
        defaultValue = "false",
        paramLabel = "<BOOLEAN>",
        description = "Release native memory when database is closed (default: ${DEFAULT-VALUE})")
    boolean cleanUpInMemoryIndexOnClose = false;

    @CommandLine.Option(
        names = {"--Xhalodb-use-memory-pool"},
        hidden = true,
        defaultValue = "true",
        paramLabel = "<BOOLEAN>",
        description = "Enable memory pool (default: ${DEFAULT-VALUE})")
    boolean useMemoryPool = true;

    @CommandLine.Option(
        names = {"--Xhalodb-memory-pool-chunk-size"},
        hidden = true,
        defaultValue = "2097152", // 2 MiB
        paramLabel = "<INTEGER>",
        description = "Size of memory pool chunks (default: ${DEFAULT-VALUE})")
    int memoryPoolChunkSize = 2097152;

    @CommandLine.Option(
        names = {"--Xhalodb-fixed-key-size"},
        hidden = true,
        defaultValue = "35",
        paramLabel = "<INTEGER>",
        description = "Size of keys for memory pool (default: ${DEFAULT-VALUE})")
    int fixedKeySize = 35;

    public Builder databaseDir(final Path databaseDir) {
      this.databaseDir = databaseDir;
      return this;
    }

    public Builder label(final String label) {
      this.label = label;
      return this;
    }

    public Builder MaxFileSize(final int maxFileSize) {
      this.maxFileSize = maxFileSize;
      return this;
    }

    public Builder FlushDataSizeBytes(final long flushDataSizeBytes) {
      this.flushDataSizeBytes = flushDataSizeBytes;
      return this;
    }

    public Builder CompationThresholdPerFile(final double compationThresholdPerFile) {
      this.compationThresholdPerFile = compationThresholdPerFile;
      return this;
    }

    public Builder CompactionJobRate(final int compactionJobRate) {
      this.compactionJobRate = compactionJobRate;
      return this;
    }

    public Builder NumberOfRecords(final int numberOfRecords) {
      this.numberOfRecords = numberOfRecords;
      return this;
    }

    public Builder CleanUpTombstonesDuringOpen(final boolean cleanUpTombstonesDuringOpen) {
      this.cleanUpTombstonesDuringOpen = cleanUpTombstonesDuringOpen;
      return this;
    }

    public Builder CleanUpInMemoryIndexOnClose(final boolean cleanUpInMemoryIndexOnClose) {
      this.cleanUpInMemoryIndexOnClose = cleanUpInMemoryIndexOnClose;
      return this;
    }

    public Builder UseMemoryPool(final boolean useMemoryPool) {
      this.useMemoryPool = useMemoryPool;
      return this;
    }

    public Builder MemoryPoolChunkSize(final int memoryPoolChunkSize) {
      this.memoryPoolChunkSize = memoryPoolChunkSize;
      return this;
    }

    public Builder FixedKeySize(final int fixedKeySize) {
      this.fixedKeySize = fixedKeySize;
      return this;
    }

    public HaloDbConfiguration build() {
      return new HaloDbConfiguration(
          enable,
          databaseDir,
          label,
          maxFileSize,
          flushDataSizeBytes,
          compationThresholdPerFile,
          compactionJobRate,
          numberOfRecords,
          cleanUpTombstonesDuringOpen,
          cleanUpInMemoryIndexOnClose,
          useMemoryPool,
          memoryPoolChunkSize,
          fixedKeySize);
    }
  }
}
