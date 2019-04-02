package tech.pegasys.pantheon.ethereum.mainnet;

import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@SuppressWarnings("WeakerAccess")
@State(Scope.Thread)
public class ProgPoWHashBenchmark {

  private final Map<String, ProgPow> hashers =
      ImmutableMap.of("0.9.2", ProgPow.progPow_0_9_2(), "0.9.3", ProgPow.progPow_0_9_3());

  @Param({"0.9.2", "0.9.3"})
  public String version;

  @Param("10000000")
  public long blockNumber;

  @Param("efda178de857b2b1703d8d5403bd0f848e19cff5c50ba5c0d6210ddb16250ec3")
  public String headerString;

  @Param("005e30899481055e")
  public String nonceString;

  private ProgPow hasher;
  private long nonce;
  private int[] header;
  private EthHashCacheFactory.EthHashDescriptor cache;
  int[] cdag;

  @Setup
  public void setUp() {

    hasher = hashers.get(version);
    nonce = BytesValue.fromHexString(nonceString).getLong(0);
    header = toIntArray(BytesValue.fromHexString(headerString));

    cache = new EthHashCacheFactory().ethHashCacheFor(blockNumber);
    cdag = hasher.createDagCache(blockNumber, (target, ind) -> EthHash.calcDatasetItem(target, cache.getCache(), ind));
  }

  private static int[] toIntArray(final BytesValue bytesValue) {
    Preconditions.checkArgument(
        bytesValue.size() % 4 == 0, "BytesValue length must be divisible by 4.");

    final int[] result = new int[bytesValue.size() / 4];
    for (int i = 0; i < result.length; i++) {
      result[i] = Integer.reverseBytes(bytesValue.getInt(i * 4));
    }
    return result;
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public int[] hashBlock() {
    return hasher.progPowHash(
        blockNumber,
        nonce,
        header,
        cdag,
        (target, ind) -> EthHash.calcDatasetItem(target, cache.getCache(), ind));
  }
}
