/*
 * Copyright 2019 ConsenSys AG.
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

import tech.pegasys.pantheon.metrics.MetricCategory;

import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

@Builder
@SuppressWarnings("FallThrough")
@EqualsAndHashCode
@ToString
public class MetricsConfiguration {
  private static final String DEFAULT_METRICS_HOST = "127.0.0.1";
  public static final int DEFAULT_METRICS_PORT = 9545;

  private static final String DEFAULT_METRICS_PUSH_HOST = "127.0.0.1";
  public static final int DEFAULT_METRICS_PUSH_PORT = 9001;

  @Getter private boolean enabled;
  @Getter @Builder.Default private int port = DEFAULT_METRICS_PORT;
  @Getter @Builder.Default private String host = DEFAULT_METRICS_HOST;
  @Getter @Singular private Set<MetricCategory> metricCategories;
  @Getter private boolean pushEnabled;
  @Getter @Builder.Default private int pushPort = DEFAULT_METRICS_PUSH_PORT;
  @Getter @Builder.Default private String pushHost = DEFAULT_METRICS_PUSH_HOST;
  @Getter @Builder.Default private int pushInterval = 15;
  @Getter @Builder.Default private String prometheusJob = "pantheon-client";

  @Getter
  @Singular("hostWhitelist")
  private List<String> hostsWhitelist;

  public static MetricsConfigurationBuilder myBuilder() {
    return builder()
        .metricCategories(MetricCategory.DEFAULT_METRIC_CATEGORIES)
        .hostWhitelist("localhost")
        .hostWhitelist("127.0.0.1");
  }
}
