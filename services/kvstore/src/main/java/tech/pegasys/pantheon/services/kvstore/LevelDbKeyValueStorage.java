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
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBException;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.impl.Iq80DBFactory;
import org.iq80.leveldb.table.BloomFilterPolicy;

public class LevelDbKeyValueStorage implements KeyValueStorage, Closeable {

  private static final Logger LOG = LogManager.getLogger();

  private static final long CACHESIZE = 16 * (2 << 20);
  private static final int MAX_OPEN_FILES = 1023;

  private final Options options;
  private final DB db;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  private final OperationTimer readLatency;
  private final OperationTimer removeLatency;
  private final OperationTimer writeLatency;
  private final OperationTimer commitLatency;
  private final Counter rollbackCount;

  public static KeyValueStorage create(
      final Path storageDirectory, final MetricsSystem metricsSystem) throws StorageException {
    return new LevelDbKeyValueStorage(storageDirectory, metricsSystem);
  }

  private LevelDbKeyValueStorage(final Path storageDirectory, final MetricsSystem metricsSystem) {
    try {
      options =
          new Options()
              .createIfMissing(true)
              .cacheSize(CACHESIZE)
              .filterPolicy(new BloomFilterPolicy(10))
              .maxOpenFiles(MAX_OPEN_FILES);
      db = new Iq80DBFactory().open(storageDirectory.toFile(), options);

      readLatency =
          metricsSystem.createTimer(
              MetricCategory.ROCKSDB, "read_latency_seconds", "Latency for read from RocksDB.");
      removeLatency =
          metricsSystem.createTimer(
              MetricCategory.ROCKSDB,
              "remove_latency_seconds",
              "Latency of remove requests from RocksDB.");
      writeLatency =
          metricsSystem.createTimer(
              MetricCategory.ROCKSDB, "write_latency_seconds", "Latency for write to RocksDB.");
      commitLatency =
          metricsSystem.createTimer(
              MetricCategory.ROCKSDB, "commit_latency_seconds", "Latency for commits to RocksDB.");

      rollbackCount =
          metricsSystem.createCounter(
              MetricCategory.ROCKSDB,
              "rollback_count",
              "Number of RocksDB transactions rolled back.");
    } catch (final IOException ioe) {
      throw new StorageException(ioe);
    }
  }

  @Override
  public Optional<BytesValue> get(final BytesValue key) throws StorageException {
    throwIfClosed();

    try (final OperationTimer.TimingContext ignored = readLatency.startTimer()) {
      return Optional.ofNullable(db.get(key.getArrayUnsafe())).map(BytesValue::wrap);
    } catch (final DBException e) {
      throw new StorageException(e);
    }
  }

  @Override
  public Transaction startTransaction() throws StorageException {
    throwIfClosed();
    return new LevelDbTransaction(db);
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      //      txOptions.close();
      //      options.close();
      try {
        db.close();
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void throwIfClosed() {
    if (closed.get()) {
      LOG.error("Attempting to use a closed RocksDbKeyValueStorage");
      throw new IllegalStateException("Storage has been closed");
    }
  }

  //  /**
  //   * Iterates over rocksDB key-value entries. Reads from a db snapshot implicitly taken when the
  //   * RocksIterator passed to the constructor was created.
  //   *
  //   * <p>Implements {@link AutoCloseable} and can be used with try-with-resources construct. When
  //   * transformed to a stream (see {@link #toStream}), iterator is automatically closed when the
  //   * stream is closed.
  //   */
  //  private static class RocksDbEntryIterator implements Iterator<Entry>, AutoCloseable {
  //    private final RocksIterator rocksIt;
  //    private volatile boolean closed = false;
  //
  //    RocksDbEntryIterator(final RocksIterator rocksIt) {
  //      this.rocksIt = rocksIt;
  //    }
  //
  //    @Override
  //    public boolean hasNext() {
  //      return rocksIt.isValid();
  //    }
  //
  //    @Override
  //    public Entry next() {
  //      if (closed) {
  //        throw new IllegalStateException("Attempt to read from a closed RocksDbEntryIterator.");
  //      }
  //      try {
  //        rocksIt.status();
  //      } catch (final DBException e) {
  //        LOG.error("RocksDbEntryIterator encountered a problem while iterating.", e);
  //      }
  //      if (!hasNext()) {
  //        throw new NoSuchElementException();
  //      }
  //      final Entry entry =
  //          Entry.create(BytesValue.wrap(rocksIt.key()), BytesValue.wrap(rocksIt.value()));
  //      rocksIt.next();
  //      return entry;
  //    }
  //
  //    Stream<Entry> toStream() {
  //      final Spliterator<Entry> split =
  //          Spliterators.spliteratorUnknownSize(
  //              this, Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.NONNULL);
  //
  //      return StreamSupport.stream(split, false).onClose(this::close);
  //    }
  //
  //    @Override
  //    public void close() {
  //      rocksIt.close();
  //      closed = true;
  //    }
  //  }

  private class LevelDbTransaction extends AbstractTransaction {
    private volatile boolean closed;
    private final WriteBatch writeBatch;
    private final DB db;

    LevelDbTransaction(final DB db) {
      this.writeBatch = db.createWriteBatch();
      this.db = db;
      this.closed = false;
    }

    @Override
    protected void doPut(final BytesValue key, final BytesValue value) {
      try (final OperationTimer.TimingContext ignored = writeLatency.startTimer()) {
        writeBatch.put(key.getArrayUnsafe(), value.getArrayUnsafe());
      } catch (final DBException e) {
        throw new StorageException(e);
      }
    }

    @Override
    protected void doRemove(final BytesValue key) {
      try (final OperationTimer.TimingContext ignored = removeLatency.startTimer()) {
        writeBatch.delete(key.getArrayUnsafe());
      } catch (final DBException e) {
        throw new StorageException(e);
      }
    }

    @Override
    protected void doCommit() throws StorageException {
      if (closed) {
        throw new StorageException(new IllegalStateException("Cannot commit closed transaction"));
      }
      try (final OperationTimer.TimingContext ignored = commitLatency.startTimer()) {
        db.write(writeBatch);
      } catch (final DBException e) {
        throw new StorageException(e);
      } finally {
        close();
      }
    }

    @Override
    protected void doRollback() {
      rollbackCount.inc();
      close();
    }

    private void close() {
      closed = true;
    }
  }
}
