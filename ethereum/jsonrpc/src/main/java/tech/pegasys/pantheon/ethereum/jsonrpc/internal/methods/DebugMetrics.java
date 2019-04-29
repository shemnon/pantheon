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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.Observation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugMetrics implements JsonRpcMethod {

  private final MetricsSystem metricsSystem;

  public DebugMetrics(final MetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
  }

  @Override
  public String getName() {
    return "debug_metrics";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Map<String, Object> observations = new HashMap<>();
    metricsSystem.observations().forEach(observation -> addObservation(observations, observation));
    return new JsonRpcSuccessResponse(request.getId(), observations);
  }

  private void addObservation(
      final Map<String, Object> observations, final Observation observation) {
    final Map<String, Object> categoryObservations =
        getNextMapLevel(observations, observation.getCategory().getName());
    if (observation.getLabels().isEmpty()) {
      categoryObservations.put(observation.getMetricName(), observation.getValue());
    } else {
      addLabelledObservation(categoryObservations, observation);
    }
  }

  private void addLabelledObservation(
      final Map<String, Object> categoryObservations, final Observation observation) {
    final List<String> labels = observation.getLabels();
    Map<String, Object> values = getNextMapLevel(categoryObservations, observation.getMetricName());
    for (int i = 0; i < labels.size() - 1; i++) {
      values = getNextMapLevel(values, labels.get(i));
    }
    values.put(labels.get(labels.size() - 1), observation.getValue());
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getNextMapLevel(
      final Map<String, Object> current, final String name) {
    return (Map<String, Object>)
        current.computeIfAbsent(name, key -> new HashMap<String, Object>());
  }
}
