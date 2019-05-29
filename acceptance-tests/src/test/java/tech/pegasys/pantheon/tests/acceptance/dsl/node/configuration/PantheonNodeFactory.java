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
package tech.pegasys.pantheon.tests.acceptance.dsl.node.configuration;

import static java.util.Arrays.asList;

import tech.pegasys.pantheon.ethereum.jsonrpc.JsonRpcConfiguration;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcApi;
import tech.pegasys.pantheon.ethereum.jsonrpc.websocket.WebSocketConfiguration;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.Node;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.PantheonNode;
import tech.pegasys.pantheon.tests.acceptance.dsl.node.configuration.genesis.GenesisConfigurationFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

public class PantheonNodeFactory {

  private final GenesisConfigurationFactory genesis = new GenesisConfigurationFactory();
  private final NodeConfigurationFactory node = new NodeConfigurationFactory();

  public PantheonNode create(final PantheonFactoryConfiguration config) throws IOException {
    return new PantheonNode(
        config.getName(),
        config.getMiningParameters(),
        config.getPrivacyParameters(),
        config.getJsonRpcConfiguration(),
        config.getWebSocketConfiguration(),
        config.getMetricsConfiguration(),
        config.getPermissioningConfiguration(),
        config.getKeyFilePath(),
        config.isDevMode(),
        config.getGenesisConfigProvider(),
        config.isP2pEnabled(),
        config.isDiscoveryEnabled(),
        config.isBootnodeEligible(),
        config.getPlugins(),
        config.getExtraCLIOptions());
  }

  public PantheonNode createMinerNode(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .miningEnabled()
            .jsonRpcEnabled()
            .webSocketEnabled()
            .build());
  }

  public PantheonNode createArchiveNode(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcEnabled()
            .webSocketEnabled()
            .build());
  }

  public Node createArchiveNodeThatMustNotBeTheBootnode(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcEnabled()
            .webSocketEnabled()
            .bootnodeEligible(false)
            .build());
  }

  public PantheonNode createArchiveNodeWithDiscoveryDisabledAndAdmin(final String name)
      throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcConfiguration(node.jsonRpcConfigWithAdmin())
            .webSocketEnabled()
            .discoveryEnabled(false)
            .build());
  }

  public PantheonNode createArchiveNodeNetServicesEnabled(final String name) throws IOException {
    // TODO: Enable metrics coverage in the acceptance tests. See PIE-1606
    // final MetricsConfiguration metricsConfiguration = MetricsConfiguration.createDefault();
    // metricsConfiguration.setEnabled(true);
    // metricsConfiguration.setPort(0);
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            // .setMetricsConfiguration(metricsConfiguration)
            .jsonRpcConfiguration(node.jsonRpcConfigWithAdmin())
            .webSocketEnabled()
            .p2pEnabled(true)
            .build());
  }

  public PantheonNode createArchiveNodeNetServicesDisabled(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcConfiguration(node.jsonRpcConfigWithAdmin())
            .p2pEnabled(false)
            .build());
  }

  public PantheonNode createArchiveNodeWithAuthentication(final String name)
      throws IOException, URISyntaxException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcEnabled()
            .jsonRpcAuthenticationEnabled()
            .webSocketEnabled()
            .build());
  }

  public PantheonNode createArchiveNodeWithAuthenticationOverWebSocket(final String name)
      throws IOException, URISyntaxException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .webSocketEnabled()
            .webSocketAuthenticationEnabled()
            .build());
  }

  public PantheonNode createNodeWithP2pDisabled(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .p2pEnabled(false)
            .jsonRpcConfiguration(node.createJsonRpcEnabledConfig())
            .build());
  }

  public PantheonNode createArchiveNodeWithRpcDisabled(final String name) throws IOException {
    return create(new PantheonFactoryConfigurationBuilder().name(name).build());
  }

  public PantheonNode createPluginsNode(
      final String name, final List<String> plugins, final List<String> extraCLIOptions)
      throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .plugins(plugins)
            .extraCLIOptions(extraCLIOptions)
            .build());
  }

  public PantheonNode createArchiveNodeWithRpcApis(
      final String name, final RpcApi... enabledRpcApis) throws IOException {
    final JsonRpcConfiguration jsonRpcConfig = node.createJsonRpcEnabledConfig();
    jsonRpcConfig.setRpcApis(asList(enabledRpcApis));
    final WebSocketConfiguration webSocketConfig = node.createWebSocketEnabledConfig();
    webSocketConfig.setRpcApis(asList(enabledRpcApis));

    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcConfiguration(jsonRpcConfig)
            .webSocketConfiguration(webSocketConfig)
            .build());
  }

  public PantheonNode createNodeWithNoDiscovery(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder().name(name).discoveryEnabled(false).build());
  }

  public PantheonNode createCliqueNode(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .miningEnabled()
            .jsonRpcConfiguration(node.createJsonRpcWithCliqueEnabledConfig())
            .webSocketConfiguration(node.createWebSocketEnabledConfig())
            .devMode(false)
            .genesisConfigProvider(genesis::createCliqueGenesisConfig)
            .build());
  }

  public PantheonNode createIbft2Node(final String name) throws IOException {
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .miningEnabled()
            .jsonRpcConfiguration(node.createJsonRpcWithIbft2EnabledConfig())
            .webSocketConfiguration(node.createWebSocketEnabledConfig())
            .devMode(false)
            .genesisConfigProvider(genesis::createIbft2GenesisConfig)
            .build());
  }

  public PantheonNode createCustomGenesisNode(
      final String name, final String genesisPath, final boolean canBeBootnode) throws IOException {
    final String genesisFile = genesis.readGenesisFile(genesisPath);
    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .jsonRpcEnabled()
            .webSocketEnabled()
            .genesisConfigProvider((a) -> Optional.of(genesisFile))
            .devMode(false)
            .bootnodeEligible(canBeBootnode)
            .build());
  }

  public PantheonNode createCliqueNodeWithValidators(final String name, final String... validators)
      throws IOException {

    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .miningEnabled()
            .jsonRpcConfiguration(node.createJsonRpcWithCliqueEnabledConfig())
            .webSocketConfiguration(node.createWebSocketEnabledConfig())
            .devMode(false)
            .genesisConfigProvider(
                nodes ->
                    node.createGenesisConfigForValidators(
                        asList(validators), nodes, genesis::createCliqueGenesisConfig))
            .build());
  }

  public PantheonNode createIbft2NodeWithValidators(final String name, final String... validators)
      throws IOException {

    return create(
        new PantheonFactoryConfigurationBuilder()
            .name(name)
            .miningEnabled()
            .jsonRpcConfiguration(node.createJsonRpcWithIbft2EnabledConfig())
            .webSocketConfiguration(node.createWebSocketEnabledConfig())
            .devMode(false)
            .genesisConfigProvider(
                nodes ->
                    node.createGenesisConfigForValidators(
                        asList(validators), nodes, genesis::createIbft2GenesisConfig))
            .build());
  }
}
