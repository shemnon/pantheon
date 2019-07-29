package tech.pegasys.pantheon.ethereum.retesteth;

import tech.pegasys.pantheon.ethereum.BlockValidator;
import tech.pegasys.pantheon.ethereum.MainnetBlockValidator;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.core.TransactionFilter;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.BlockProcessor;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockImporter;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockProcessor;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;

import java.math.BigInteger;
import java.util.Optional;

public class NoRewardProtocolScheduleWrapper<C> implements ProtocolSchedule<C> {

  private final ProtocolSchedule<C> delegate;

  public NoRewardProtocolScheduleWrapper(final ProtocolSchedule<C> delegate) {
    this.delegate = delegate;
  }

  @Override
  public ProtocolSpec<C> getByBlockNumber(final long number) {
    final ProtocolSpec<C> original = delegate.getByBlockNumber(number);
    BlockProcessor noRewardBlockProcessor =
        new MainnetBlockProcessor(
            original.getTransactionProcessor(),
            original.getTransactionReceiptFactory(),
            Wei.ZERO,
            original.getMiningBeneficiaryCalculator());
    BlockValidator<C> noRewardBlockValidator =
        new MainnetBlockValidator<>(
            original.getBlockHeaderValidator(),
            original.getBlockBodyValidator(),
            noRewardBlockProcessor);
    BlockImporter noRewardBlockImporter = new MainnetBlockImporter(noRewardBlockValidator);
    return new ProtocolSpec<C>(
        original.getName(),
        original.getEvm(),
        original.getTransactionValidator(),
        original.getTransactionProcessor(),
        original.getBlockHeaderValidator(),
        original.getOmmerHeaderValidator(),
        original.getBlockBodyValidator(),
        noRewardBlockProcessor,
        noRewardBlockImporter,
        noRewardBlockValidator,
        original.getBlockHeaderFunctions(),
        original.getTransactionReceiptFactory(),
        original.getDifficultyCalculator(),
        Wei.ZERO, // block reward
        null, // transaction receipt type; unused
        original.getMiningBeneficiaryCalculator(),
        original.getPrecompileContractRegistry());
  }

  @Override
  public Optional<BigInteger> getChainId() {
    return delegate.getChainId();
  }

  @Override
  public void setTransactionFilter(final TransactionFilter transactionFilter) {
    delegate.setTransactionFilter(transactionFilter);
  }
}
