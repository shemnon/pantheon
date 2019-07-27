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
package tech.pegasys.pantheon.ethereum.mainnet;

import tech.pegasys.pantheon.ethereum.vm.EVM;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.OperationRegistry;
import tech.pegasys.pantheon.ethereum.vm.operations.AddModOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.AddOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.AddressOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.AndOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.BalanceOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.BlockHashOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ByteOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallCodeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallDataCopyOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallDataLoadOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallDataSizeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallValueOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CallerOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ChainIdOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CodeCopyOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CodeSizeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.CoinbaseOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.Create2Operation;
import tech.pegasys.pantheon.ethereum.vm.operations.CreateOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.DelegateCallOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.DifficultyOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.DivOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.DupOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.EqOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ExpOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ExtCodeCopyOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ExtCodeHashOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ExtCodeSizeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.GasLimitOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.GasOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.GasPriceOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.GtOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.InvalidOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.IsZeroOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.JumpDestOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.JumpOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.JumpiOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.LogOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.LtOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.MLoadOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.MSizeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.MStore8Operation;
import tech.pegasys.pantheon.ethereum.vm.operations.MStoreOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ModOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.MulModOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.MulOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.NotOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.NumberOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.OrOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.OriginOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.PCOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.PopOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.PushOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ReturnDataCopyOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ReturnDataSizeOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ReturnOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.RevertOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SDivOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SGtOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SLoadOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SLtOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SModOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SStoreOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SarOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SelfDestructOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.Sha3Operation;
import tech.pegasys.pantheon.ethereum.vm.operations.ShlOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.ShrOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SignExtendOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.StaticCallOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.StopOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SubOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.SwapOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.TimestampOperation;
import tech.pegasys.pantheon.ethereum.vm.operations.XorOperation;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;

/** Provides EVMs supporting the appropriate operations for mainnet hard forks. */
abstract class MainnetEvmRegistries {

  static EVM frontier(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerFrontierOpcodes(registry, gasCalculator);

    return new EVM(registry, new InvalidOperation(gasCalculator));
  }

  static EVM homestead(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerHomesteadOpcodes(registry, gasCalculator);

    return new EVM(registry, new InvalidOperation(gasCalculator));
  }

  static EVM byzantium(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerByzantiumOpcodes(registry, gasCalculator);

    return new EVM(registry, new InvalidOperation(gasCalculator));
  }

  static EVM constantinople(final GasCalculator gasCalculator) {
    final OperationRegistry registry = new OperationRegistry();

    registerConstantinopleOpcodes(registry, gasCalculator);

    return new EVM(registry, new InvalidOperation(gasCalculator));
  }

  static EVM istanbul(final GasCalculator gasCalculator, final BigInteger chainId) {
    final OperationRegistry registry = new OperationRegistry();

    registerIstanbulOpcodes(registry, gasCalculator, chainId);

    return new EVM(registry, new InvalidOperation(gasCalculator));
  }

  private static void registerFrontierOpcodes(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registry.put(new AddOperation(gasCalculator));
    registry.put(new AddOperation(gasCalculator));
    registry.put(new MulOperation(gasCalculator));
    registry.put(new SubOperation(gasCalculator));
    registry.put(new DivOperation(gasCalculator));
    registry.put(new SDivOperation(gasCalculator));
    registry.put(new ModOperation(gasCalculator));
    registry.put(new SModOperation(gasCalculator));
    registry.put(new ExpOperation(gasCalculator));
    registry.put(new AddModOperation(gasCalculator));
    registry.put(new MulModOperation(gasCalculator));
    registry.put(new SignExtendOperation(gasCalculator));
    registry.put(new LtOperation(gasCalculator));
    registry.put(new GtOperation(gasCalculator));
    registry.put(new SLtOperation(gasCalculator));
    registry.put(new SGtOperation(gasCalculator));
    registry.put(new EqOperation(gasCalculator));
    registry.put(new IsZeroOperation(gasCalculator));
    registry.put(new AndOperation(gasCalculator));
    registry.put(new OrOperation(gasCalculator));
    registry.put(new XorOperation(gasCalculator));
    registry.put(new NotOperation(gasCalculator));
    registry.put(new ByteOperation(gasCalculator));
    registry.put(new Sha3Operation(gasCalculator));
    registry.put(new AddressOperation(gasCalculator));
    registry.put(new BalanceOperation(gasCalculator));
    registry.put(new OriginOperation(gasCalculator));
    registry.put(new CallerOperation(gasCalculator));
    registry.put(new CallValueOperation(gasCalculator));
    registry.put(new CallDataLoadOperation(gasCalculator));
    registry.put(new CallDataSizeOperation(gasCalculator));
    registry.put(new CallDataCopyOperation(gasCalculator));
    registry.put(new CodeSizeOperation(gasCalculator));
    registry.put(new CodeCopyOperation(gasCalculator));
    registry.put(new GasPriceOperation(gasCalculator));
    registry.put(new ExtCodeCopyOperation(gasCalculator));
    registry.put(new ExtCodeSizeOperation(gasCalculator));
    registry.put(new BlockHashOperation(gasCalculator));
    registry.put(new CoinbaseOperation(gasCalculator));
    registry.put(new TimestampOperation(gasCalculator));
    registry.put(new NumberOperation(gasCalculator));
    registry.put(new DifficultyOperation(gasCalculator));
    registry.put(new GasLimitOperation(gasCalculator));
    registry.put(new PopOperation(gasCalculator));
    registry.put(new MLoadOperation(gasCalculator));
    registry.put(new MStoreOperation(gasCalculator));
    registry.put(new MStore8Operation(gasCalculator));
    registry.put(new SLoadOperation(gasCalculator));
    registry.put(new SStoreOperation(gasCalculator, SStoreOperation.FRONTIER_MINIMUM));
    registry.put(new JumpOperation(gasCalculator));
    registry.put(new JumpiOperation(gasCalculator));
    registry.put(new PCOperation(gasCalculator));
    registry.put(new MSizeOperation(gasCalculator));
    registry.put(new GasOperation(gasCalculator));
    registry.put(new JumpDestOperation(gasCalculator));
    registry.put(new ReturnOperation(gasCalculator));
    registry.put(new InvalidOperation(gasCalculator));
    registry.put(new StopOperation(gasCalculator));
    registry.put(new SelfDestructOperation(gasCalculator));
    registry.put(new CreateOperation(gasCalculator));
    registry.put(new CallOperation(gasCalculator));
    registry.put(new CallCodeOperation(gasCalculator));

    // Register the PUSH1, PUSH2, ..., PUSH32 operations.
    for (int i = 1; i <= 32; ++i) {
      registry.put(new PushOperation(i, gasCalculator));
    }

    // Register the DUP1, DUP2, ..., DUP16 operations.
    for (int i = 1; i <= 16; ++i) {
      registry.put(new DupOperation(i, gasCalculator));
    }

    // Register the SWAP1, SWAP2, ..., SWAP16 operations.
    for (int i = 1; i <= 16; ++i) {
      registry.put(new SwapOperation(i, gasCalculator));
    }

    // Register the LOG0, LOG1, ..., LOG4 operations.
    for (int i = 0; i < 5; ++i) {
      registry.put(new LogOperation(i, gasCalculator));
    }
  }

  private static void registerHomesteadOpcodes(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registerFrontierOpcodes(registry, gasCalculator);
    registry.put(new DelegateCallOperation(gasCalculator));
  }

  private static void registerByzantiumOpcodes(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registerHomesteadOpcodes(registry, gasCalculator);
    registry.put(new ReturnDataCopyOperation(gasCalculator));
    registry.put(new ReturnDataSizeOperation(gasCalculator));
    registry.put(new RevertOperation(gasCalculator));
    registry.put(new StaticCallOperation(gasCalculator));
  }

  private static void registerConstantinopleOpcodes(
      final OperationRegistry registry, final GasCalculator gasCalculator) {
    registerByzantiumOpcodes(registry, gasCalculator);
    registry.put(new Create2Operation(gasCalculator));
    registry.put(new SarOperation(gasCalculator));
    registry.put(new ShlOperation(gasCalculator));
    registry.put(new ShrOperation(gasCalculator));
    registry.put(new ExtCodeHashOperation(gasCalculator));
  }

  private static void registerIstanbulOpcodes(
      final OperationRegistry registry,
      final GasCalculator gasCalculator,
      final BigInteger chainId) {
    registerConstantinopleOpcodes(registry, gasCalculator);
    registry.put(new SStoreOperation(gasCalculator, SStoreOperation.EIP_1706_MINIMUM));

    registry.put(
        new ChainIdOperation(gasCalculator, Bytes32.leftPad(BytesValue.of(chainId.toByteArray()))));
    registry.put(new SStoreOperation(gasCalculator, SStoreOperation.EIP_1706_MINIMUM));
  }
}
