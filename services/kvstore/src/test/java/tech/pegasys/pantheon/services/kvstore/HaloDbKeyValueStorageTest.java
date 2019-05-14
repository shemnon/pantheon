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

import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HaloDbKeyValueStorageTest extends AbstractKeyValueStorageTest {

  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  @Override
  protected KeyValueStorage createStore() throws Exception {
    return HaloDbKeyValueStorage.create(
        new HaloDbConfiguration.Builder().databaseDir(folder.newFolder().toPath()).build(),
        new NoOpMetricsSystem());
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionCommit() throws Exception {
    super.transactionCommit();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionRollback() throws Exception {
    super.transactionRollback();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionRollbackEmpty() throws Exception {
    super.transactionRollbackEmpty();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionPutAfterCommit() throws Exception {
    super.transactionPutAfterCommit();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionRemoveAfterCommit() throws Exception {
    super.transactionRemoveAfterCommit();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionPutAfterRollback() throws Exception {
    super.transactionPutAfterRollback();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionRemoveAfterRollback() throws Exception {
    super.transactionRemoveAfterRollback();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionCommitAfterRollback() throws Exception {
    super.transactionCommitAfterRollback();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionCommitTwice() throws Exception {
    super.transactionCommitTwice();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionRollbackAfterCommit() throws Exception {
    super.transactionRollbackAfterCommit();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionRollbackTwice() throws Exception {
    super.transactionRollbackTwice();
  }

  @Override
  @Test
  @Ignore("HaloDB has no transaction support")
  public void transactionIsolation() throws Exception {
    super.transactionIsolation();
  }
}
