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

import tech.pegasys.pantheon.metrics.MetricCategory;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.OperationTimer;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oath.halodb.HaloDB;
import com.oath.halodb.HaloDBException;
import com.oath.halodb.HaloDBOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HaloDbKeyValueStorage implements KeyValueStorage, Closeable {

  private static final Logger LOG = LogManager.getLogger();

  private final AtomicBoolean closed = new AtomicBoolean(false);

  private final OperationTimer readLatency;
  private final OperationTimer removeLatency;
  private final OperationTimer writeLatency;
  private final HaloDB db;

  public static KeyValueStorage create(
      final HaloDbConfiguration haloDBConfiguration, final MetricsSystem metricsSystem)
      throws StorageException {
    return new HaloDbKeyValueStorage(haloDBConfiguration, metricsSystem);
  }

  private HaloDbKeyValueStorage(
      final HaloDbConfiguration haloDbConfiguration, final MetricsSystem metricsSystem) {
    try {
      // Open a db with default options.
      HaloDBOptions options = new HaloDBOptions();

      // size of each data file will be 1GB.
      options.setMaxFileSize(1024 * 1024 * 1024);

      // the threshold at which page cache is synced to disk.
      // data will be durable only if it is flushed to disk, therefore
      // more data will be lost if this value is set too high. Setting
      // this value too low might interfere with read and write performance.
      options.setFlushDataSizeBytes(10 * 1024 * 1024);

      // The percentage of stale data in a data file at which the file will be compacted.
      // This value helps control write and space amplification. Increasing this value will
      // reduce write amplification but will increase space amplification.
      // This along with the compactionJobRate below is the most important setting
      // for tuning HaloDB performance. If this is set to x then write amplification
      // will be approximately 1/x.
      options.setCompactionThresholdPerFile(0.7);

      // Controls how fast the compaction job should run.
      // This is the amount of data which will be copied by the compaction thread per second.
      // Optimal value depends on the compactionThresholdPerFile option.
      options.setCompactionJobRate(50 * 1024 * 1024);

      // Setting this value is important as it helps to preallocate enough
      // memory for the off-heap cache. If the value is too low the db might
      // need to rehash the cache. For a db of size n set this value to 2*n.
      options.setNumberOfRecords(100_000_000);

      // Delete operation for a key will write a tombstone record to a tombstone file.
      // the tombstone record can be removed only when all previous version of that key
      // has been deleted by the compaction job.
      // enabling this option will delete during startup all tombstone records whose previous
      // versions were removed from the data file.
      options.setCleanUpTombstonesDuringOpen(true);

      // HaloDB does native memory allocation for the in-memory index.
      // Enabling this option will release all allocated memory back to the kernel when the db is
      // closed.
      // This option is not necessary if the JVM is shutdown when the db is closed, as in that case
      // allocated memory is released automatically by the kernel.
      // If using in-memory index without memory pool this option,
      // depending on the number of records in the database,
      // could be a slow as we need to call _free_ for each record.
      options.setCleanUpInMemoryIndexOnClose(false);

      // ** settings for memory pool **
      options.setUseMemoryPool(true);

      // Hash table implementation in HaloDB is similar to that of ConcurrentHashMap in Java 7.
      // Hash table is divided into segments and each segment manages its own native memory.
      // The number of segments is twice the number of cores in the machine.
      // A segment's memory is further divided into chunks whose size can be configured here.
      options.setMemoryPoolChunkSize(2 * 1024 * 1024);

      // using a memory pool requires us to declare the size of keys in advance.
      // Any write request with key length greater than the declared value will fail, but it
      // is still possible to store keys smaller than this declared size.
      options.setFixedKeySize(35);

      // The directory will be created if it doesn't exist and all database files will be stored in
      // this directory
      String directory = "directory";

      // Open the database. Directory will be created if it doesn't exist.
      // If we are opening an existing database HaloDB needs to scan all the
      // index files to create the in-memory index, which, depending on the db size, might take a
      // few minutes.
      db = HaloDB.open(directory, options);

      readLatency =
          metricsSystem
              .createLabelledTimer(
                  MetricCategory.KVSTORE_ROCKSDB,
                  "read_latency_seconds",
                  "Latency for read from RocksDB.",
                  "database")
              .labels(haloDbConfiguration.getLabel());
      removeLatency =
          metricsSystem
              .createLabelledTimer(
                  MetricCategory.KVSTORE_ROCKSDB,
                  "remove_latency_seconds",
                  "Latency of remove requests from RocksDB.",
                  "database")
              .labels(haloDbConfiguration.getLabel());
      writeLatency =
          metricsSystem
              .createLabelledTimer(
                  MetricCategory.KVSTORE_ROCKSDB,
                  "write_latency_seconds",
                  "Latency for write to RocksDB.",
                  "database")
              .labels(haloDbConfiguration.getLabel());
      //      commitLatency =
      //          metricsSystem
      //              .createLabelledTimer(
      //                  MetricCategory.KVSTORE_ROCKSDB,
      //                  "commit_latency_seconds",
      //                  "Latency for commits to RocksDB.",
      //                  "database")
      //              .labels(haloDbConfiguration.getLabel());

      //      if (metricsSystem instanceof PrometheusMetricsSystem) {
      //        RocksDBStats.registerRocksDBMetrics(stats, (PrometheusMetricsSystem) metricsSystem);
      //      }

      //      metricsSystem.createLongGauge(
      //          MetricCategory.KVSTORE_ROCKSDB,
      //          "rocks_db_table_readers_memory_bytes",
      //          "Estimated memory used for RocksDB index and filter blocks in bytes",
      //          () -> {
      //            try {
      //              return db.getLongProperty("rocksdb.estimate-table-readers-mem");
      //            } catch (final HaloDBException e) {
      //              LOG.debug("Failed to get RocksDB metric", e);
      //              return 0L;
      //            }
      //          });
      //
      //      rollbackCount =
      //          metricsSystem
      //              .createLabelledCounter(
      //                  MetricCategory.KVSTORE_ROCKSDB,
      //                  "rollback_count",
      //                  "Number of RocksDB transactions rolled back.",
      //                  "database")
      //              .labels(haloDbConfiguration.getLabel());
    } catch (final HaloDBException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public Optional<BytesValue> get(final BytesValue key) throws StorageException {
    throwIfClosed();

    try (final OperationTimer.TimingContext ignored = readLatency.startTimer()) {
      return Optional.ofNullable(db.get(key.getArrayUnsafe())).map(BytesValue::wrap);
    } catch (final HaloDBException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public Transaction startTransaction() throws StorageException {
    throwIfClosed();
    return new HaloDBTransaction();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        db.close();
      } catch (final HaloDBException e) {
        throw new StorageException(e);
      }
    }
  }

  private void throwIfClosed() {
    if (closed.get()) {
      LOG.error("Attempting to use a closed RocksDbKeyValueStorage");
      throw new IllegalStateException("Storage has been closed");
    }
  }

  private class HaloDBTransaction extends AbstractTransaction {

    HaloDBTransaction() {}

    @Override
    protected void doPut(final BytesValue key, final BytesValue value) {
      try (final OperationTimer.TimingContext ignored = writeLatency.startTimer()) {
        db.put(key.getArrayUnsafe(), value.getArrayUnsafe());
      } catch (final HaloDBException e) {
        throw new StorageException(e);
      }
    }

    @Override
    protected void doRemove(final BytesValue key) {
      try (final OperationTimer.TimingContext ignored = removeLatency.startTimer()) {
        db.delete(key.getArrayUnsafe());
      } catch (final HaloDBException e) {
        throw new StorageException(e);
      }
    }

    @Override
    protected void doCommit() throws StorageException {
      close();
    }

    @Override
    protected void doRollback() {
      throw new UnsupportedOperationException("Rollbacks not supported");
    }

    private void close() {}
  }
}
