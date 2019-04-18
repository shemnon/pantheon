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

import tech.pegasys.pantheon.metrics.Counter;
import tech.pegasys.pantheon.metrics.MetricCategory;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.OperationTimer;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.protonail.bolt.jna.Bolt;
import com.protonail.bolt.jna.BoltBucket;
import com.protonail.bolt.jna.BoltFileMode;
import com.protonail.bolt.jna.BoltOptions;
import com.protonail.bolt.jna.BoltTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BoltDBKeyValueStorage implements KeyValueStorage, Closeable {

  private static final Logger LOG = LogManager.getLogger();

  private final BoltOptions options;
  private final Bolt db;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private final OperationTimer readLatency;
  private final OperationTimer removeLatency;
  private final OperationTimer writeLatency;
  private final OperationTimer commitLatency;
  private final Counter rollbackCount;

  public static KeyValueStorage create(
      final RocksDbConfiguration rocksDbConfiguration, final MetricsSystem metricsSystem)
      throws StorageException {
    return new BoltDBKeyValueStorage(rocksDbConfiguration, metricsSystem);
  }

  private BoltDBKeyValueStorage(
      final RocksDbConfiguration rocksDbConfiguration, final MetricsSystem metricsSystem) {
    options = new BoltOptions(5000 /* timeout in ms */);
    db =
        new Bolt(
            rocksDbConfiguration.getDatabaseDir().toFile().getAbsolutePath() + "/bolt",
            BoltFileMode.DEFAULT,
            options);

    readLatency =
        metricsSystem
            .createLabelledTimer(
                MetricCategory.KVSTORE_ROCKSDB,
                "read_latency_seconds",
                "Latency for read from RocksDB.",
                "database")
            .labels(rocksDbConfiguration.getLabel());
    removeLatency =
        metricsSystem
            .createLabelledTimer(
                MetricCategory.KVSTORE_ROCKSDB,
                "remove_latency_seconds",
                "Latency of remove requests from RocksDB.",
                "database")
            .labels(rocksDbConfiguration.getLabel());
    writeLatency =
        metricsSystem
            .createLabelledTimer(
                MetricCategory.KVSTORE_ROCKSDB,
                "write_latency_seconds",
                "Latency for write to RocksDB.",
                "database")
            .labels(rocksDbConfiguration.getLabel());
    commitLatency =
        metricsSystem
            .createLabelledTimer(
                MetricCategory.KVSTORE_ROCKSDB,
                "commit_latency_seconds",
                "Latency for commits to RocksDB.",
                "database")
            .labels(rocksDbConfiguration.getLabel());

    rollbackCount =
        metricsSystem
            .createLabelledCounter(
                MetricCategory.KVSTORE_ROCKSDB,
                "rollback_count",
                "Number of RocksDB transactions rolled back.",
                "database")
            .labels(rocksDbConfiguration.getLabel());
  }

  @Override
  public Optional<BytesValue> get(final BytesValue key) throws StorageException {
    throwIfClosed();
    try (final OperationTimer.TimingContext ignored = readLatency.startTimer()) {
      final BoltTransaction boltTransaction = db.begin(false);
      try (final BoltBucket bucket =
          boltTransaction.getBucket(
              "my-bucket".getBytes(StandardCharsets.UTF_8))) { // 'my-bucket' must be exists
        return Optional.ofNullable(bucket.get(key.getArrayUnsafe())).map(BytesValue::wrap);
      } finally {
        boltTransaction.rollback();
      }
    }
  }

  @Override
  public Transaction startTransaction() throws StorageException {
    throwIfClosed();
    return new BoltDBTransaction(db.begin(true), "my-bucket");
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      options.close();
      db.close();
    }
  }

  private void throwIfClosed() {
    if (closed.get()) {
      LOG.error("Attempting to use a closed RocksDbKeyValueStorage");
      throw new IllegalStateException("Storage has been closed");
    }
  }

  private class BoltDBTransaction extends AbstractTransaction {
    private final BoltTransaction innerTx;
    BoltBucket bucket;

    BoltDBTransaction(final BoltTransaction innerTx, final String bucketName) {
      this.innerTx = innerTx;
      bucket = innerTx.getBucket(bucketName.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doPut(final BytesValue key, final BytesValue value) {
      try (final OperationTimer.TimingContext ignored = writeLatency.startTimer()) {
        bucket.put(key.getArrayUnsafe(), value.getArrayUnsafe());
      }
    }

    @Override
    protected void doRemove(final BytesValue key) {
      try (final OperationTimer.TimingContext ignored = removeLatency.startTimer()) {
        bucket.delete(key.getArrayUnsafe());
      }
    }

    @Override
    protected void doCommit() throws StorageException {
      try (final OperationTimer.TimingContext ignored = commitLatency.startTimer()) {
        bucket.close();
        innerTx.commit();
      }
    }

    @Override
    protected void doRollback() {
      bucket.close();
      innerTx.rollback();
      rollbackCount.inc();
    }
  }
}
