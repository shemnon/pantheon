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
package tech.pegasys.pantheon.ethereum.storage.keyvalue;

import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.services.kvstore.HaloDbConfiguration;
import tech.pegasys.pantheon.services.kvstore.HaloDbKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage;

import java.io.IOException;
import java.nio.file.Files;

public class HaloDbStorageProvider {

  public static StorageProvider create(
      final HaloDbConfiguration haloDbConfiguration, final MetricsSystem metricsSystem)
      throws IOException {
    Files.createDirectories(haloDbConfiguration.getDatabaseDir());
    final KeyValueStorage kv = HaloDbKeyValueStorage.create(haloDbConfiguration, metricsSystem);
    return new KeyValueStorageProvider(kv);
  }
}
