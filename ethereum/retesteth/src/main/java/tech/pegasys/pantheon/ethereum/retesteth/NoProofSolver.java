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
package tech.pegasys.pantheon.ethereum.retesteth;

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.mainnet.EthHashSolution;
import tech.pegasys.pantheon.ethereum.mainnet.EthHashSolver;
import tech.pegasys.pantheon.ethereum.mainnet.EthHashSolverInputs;
import tech.pegasys.pantheon.util.bytes.Bytes32;

import java.util.Optional;

public class NoProofSolver extends EthHashSolver {

  NoProofSolver(final Iterable<Long> nonceGenerator) {
    super(nonceGenerator, null);
  }

  @Override
  public EthHashSolution solveFor(final EthHashSolverJob job) {
    return new EthHashSolution(
        getNonceGenerator().iterator().next(), Hash.EMPTY, new byte[Bytes32.SIZE]);
  }

  @Override
  public boolean submitSolution(final EthHashSolution solution) {
    return true;
  }

  @Override
  public Optional<EthHashSolverInputs> getWorkDefinition() {
    return Optional.empty();
  }

  @Override
  public Optional<Long> hashesPerSecond() {
    return Optional.of(1L);
  }
}
