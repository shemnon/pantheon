package tech.pegasys.pantheon.ethereum.mainnet;

import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.concurrent.TimeUnit;

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
public class EthashHashBenchmark {

  @Param("10000000")
  public long blockNumber;

  @Param("efda178de857b2b1703d8d5403bd0f848e19cff5c50ba5c0d6210ddb16250ec3")
  public String headerString;

  @Param("005e30899481055e")
  public String nonceString;

  private long nonce;
  private byte[] headerBytes;
  private EthHashCacheFactory.EthHashDescriptor cache;

  @Setup
  public void setUp() {

    nonce = BytesValue.fromHexString(nonceString).getLong(0);
    headerBytes = BytesValue.fromHexString(headerString).extractArray();

    cache = new EthHashCacheFactory().ethHashCacheFor(blockNumber);
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.NANOSECONDS)
  public byte[] hashBlock() {
    return EthHash.hashimotoLight(cache.getDatasetSize(), cache.getCache(), headerBytes, nonce);
  }
}
