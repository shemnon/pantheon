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
package tech.pegasys.pantheon.ethereum.graphqlrpc;

import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.BlockchainQuery;
import tech.pegasys.pantheon.ethereum.graphqlrpc.internal.pojoadapter.NormalBlockAdapter;
import tech.pegasys.pantheon.ethereum.p2p.wire.Capability;

import java.util.Optional;
import java.util.Set;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

public abstract class AbstractDataFetcherTest {

  DataFetcher<Optional<NormalBlockAdapter>> fetcher;
  private GraphQLDataFetchers fetchers;

  @Mock protected Set<Capability> supportedCapabilities;

  @Mock protected DataFetchingEnvironment environment;

  @Mock protected GraphQLDataFetcherContext context;

  @Mock protected BlockchainQuery query;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void before() {
    fetchers = new GraphQLDataFetchers(supportedCapabilities);
    fetcher = fetchers.getBlockDataFetcher();
    when(environment.getContext()).thenReturn(context);
    when(context.getBlockchainQuery()).thenReturn(query);
  }
}
