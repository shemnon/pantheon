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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;

class LabelledCurrentValueCollector extends Collector {

  private final String metricName;
  private final String help;
  private final List<String> labelNames;
  private final Map<List<String>, Supplier<Double>> labeledGauges;

  LabelledCurrentValueCollector(
      final String metricName, final String help, final List<String> labelNames) {
    this.metricName = metricName;
    this.help = help;
    this.labelNames = labelNames;
    labeledGauges = new HashMap<>();
  }

  void addGauge(final List<String> labels, final Supplier<Double> gauge) {
    checkArgument(
        labels.size() == labelNames.size(), "Count of Label names and provided labels must match");
    labeledGauges.put(labels, gauge);
  }

  @Override
  public List<MetricFamilySamples> collect() {
    return singletonList(
        new MetricFamilySamples(
            metricName,
            Type.GAUGE,
            help,
            labeledGauges.entrySet().stream()
                .map(
                    entry ->
                        new Sample(metricName, labelNames, entry.getKey(), entry.getValue().get()))
                .collect(Collectors.toList())));
  }
}
