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
package tech.pegasys.pantheon.metrics.prometheus;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.function.Supplier;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

class CurrentValueCollector extends Collector {

  private final String metricName;
  private final String help;
  private final Supplier<Double> valueSupplier;

  CurrentValueCollector(
      final String metricName, final String help, final Supplier<Double> valueSupplier) {
    this.metricName = metricName;
    this.help = help;
    this.valueSupplier = valueSupplier;
  }

  @Override
  public List<MetricFamilySamples> collect() {
    final Sample sample = new Sample(metricName, emptyList(), emptyList(), valueSupplier.get());
    return singletonList(
        new MetricFamilySamples(metricName, Type.GAUGE, help, singletonList(sample)));
  }
}
