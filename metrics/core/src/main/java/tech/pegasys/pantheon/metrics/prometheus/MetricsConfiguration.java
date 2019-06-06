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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

@AutoValue
public abstract class MetricsConfiguration {
  private static final String DEFAULT_METRICS_HOST = "127.0.0.1";
  public static final int DEFAULT_METRICS_PORT = 9545;

  private static final String DEFAULT_METRICS_PUSH_HOST = "127.0.0.1";
  public static final int DEFAULT_METRICS_PUSH_PORT = 9001;

  public abstract boolean isEnabled();

  public abstract int getPort();

  public abstract String getHost();

  public abstract ImmutableSet<MetricCategory> getMetricCategories();

  public abstract int getPushPort();

  public abstract String getPushHost();

  public abstract boolean isPushEnabled();

  public abstract int getPushInterval();

  public abstract String getPrometheusJob();

  public abstract ImmutableSet<String> getHostsWhitelist();

  public static Builder builder() {
    return new AutoValue_MetricsConfiguration.Builder()
        .enabled(false)
        .port(DEFAULT_METRICS_PORT)
        .host(DEFAULT_METRICS_HOST)
        .hostsWhitelist(ImmutableSet.of("localhost", "127.0.0.1"))
        .metricCategories(MetricCategory.DEFAULT_METRIC_CATEGORIES)
        .pushEnabled(false)
        .pushPort(DEFAULT_METRICS_PUSH_PORT)
        .pushHost(DEFAULT_METRICS_PUSH_HOST)
        .pushInterval(15)
        .prometheusJob("pantheon-client");
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder enabled(boolean enabled);

    public abstract Builder port(int port);

    public abstract Builder host(String host);

    public abstract Builder metricCategories(ImmutableSet<MetricCategory> metricCategories);

    public abstract Builder metricCategories(MetricCategory... metricCategories);

    public abstract Builder pushPort(int pushPort);

    public abstract Builder pushHost(String pushHost);

    public abstract Builder pushEnabled(boolean pushEnabled);

    public abstract Builder pushInterval(int pushInterval);

    public abstract Builder prometheusJob(String prometheusJob);

    public abstract Builder hostsWhitelist(ImmutableSet<String> hostsWhitelist);

    public abstract Builder hostsWhitelist(String... hostsWhitelist);

    public abstract MetricsConfiguration build();
  }
}
