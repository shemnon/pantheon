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
package tech.pegasys.pantheon.ethereum.mainnet.precompiles;

import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.mainnet.AbstractPrecompiledContract;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public class IDPrecompiledContract extends AbstractPrecompiledContract {

  public IDPrecompiledContract(final GasCalculator gasCalculator) {
    super("ID", gasCalculator);
  }

  @Override
  public Gas gasRequirement(final BytesValue input, final MessageFrame messageFrame) {
    return getGasCalculator().idPrecompiledContractGasCost(input);
  }

  @Override
  public BytesValue compute(final BytesValue input, final MessageFrame messageFrame) {
    return input;
  }
}
