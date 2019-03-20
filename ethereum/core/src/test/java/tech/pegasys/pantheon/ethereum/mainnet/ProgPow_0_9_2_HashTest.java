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
package tech.pegasys.pantheon.ethereum.mainnet;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.util.bytes.BytesValue;

import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProgPow_0_9_2_HashTest {

  @Parameters
  public static Object[][] getParameters() {
    return new Object[][] {
      {
        30000,
        "ffeeddccbbaa9988776655443322110000112233445566778899aabbccddeeff",
        "123456789abcdef0",
        "5b7ccd472dbefdd95b895cac8ece67ff0deb5a6bd2ecc6e162383d00c3728ece",
      },
      {
        0,
        "0000000000000000000000000000000000000000000000000000000000000000",
        "0000000000000000",
        "63155f732f2bf556967f906155b510c917e48e99685ead76ea83f4eca03ab12b"
      },
      {
        49,
        "63155f732f2bf556967f906155b510c917e48e99685ead76ea83f4eca03ab12b",
        "0000000006ff2c47",
        "9e7248f20914913a73d80a70174c331b1d34f260535ac3631d770e656b5dd922"
      },
      {
        50,
        "9e7248f20914913a73d80a70174c331b1d34f260535ac3631d770e656b5dd922",
        "00000000076e482e",
        "de37e1824c86d35d154cf65a88de6d9286aec4f7f10c3fc9f0fa1bcc2687188d"
      },
      {
        99,
        "de37e1824c86d35d154cf65a88de6d9286aec4f7f10c3fc9f0fa1bcc2687188d",
        "000000003917afab",
        "ac7b55e801511b77e11d52e9599206101550144525b5679f2dab19386f23dcce"
      },
      {
        29950,
        "ac7b55e801511b77e11d52e9599206101550144525b5679f2dab19386f23dcce",
        "005d409dbc23a62a",
        "e43d7e0bdc8a4a3f6e291a5ed790b9fa1a0948a2b9e33c844888690847de19f5"
      },
      {
        29999,
        "e43d7e0bdc8a4a3f6e291a5ed790b9fa1a0948a2b9e33c844888690847de19f5",
        "005db5fa4c2a3d03",
        "d34519f72c97cae8892c277776259db3320820cb5279a299d0ef1e155e5c6454"
      },
      {
        30000,
        "d34519f72c97cae8892c277776259db3320820cb5279a299d0ef1e155e5c6454",
        "005db8607994ff30",
        "8b6ce5da0b06d18db7bd8492d9e5717f8b53e7e098d9fef7886d58a6e913ef64"
      },
      {
        30049,
        "8b6ce5da0b06d18db7bd8492d9e5717f8b53e7e098d9fef7886d58a6e913ef64",
        "005e2e215a8ca2e7",
        "c2c46173481b9ced61123d2e293b42ede5a1b323210eb2a684df0874ffe09047"
      },
      {
        30050,
        "c2c46173481b9ced61123d2e293b42ede5a1b323210eb2a684df0874ffe09047",
        "005e30899481055e",
        "ea42197eb2ba79c63cb5e655b8b1f612c5f08aae1a49ff236795a3516d87bc71"
      },
      {
        30099,
        "ea42197eb2ba79c63cb5e655b8b1f612c5f08aae1a49ff236795a3516d87bc71",
        "005ea6aef136f88b",
        "49e15ba4bf501ce8fe8876101c808e24c69a859be15de554bf85dbc095491bd6"
      },
      {
        59950,
        "49e15ba4bf501ce8fe8876101c808e24c69a859be15de554bf85dbc095491bd6",
        "02ebe0503bd7b1da",
        "f5c50ba5c0d6210ddb16250ec3efda178de857b2b1703d8d5403bd0f848e19cf"
      },
      {
        59999,
        "f5c50ba5c0d6210ddb16250ec3efda178de857b2b1703d8d5403bd0f848e19cf",
        "02edb6275bd221e3",
        "341562d10d4afb706ec2c8d5537cb0c810de02b4ebb0a0eea5ae335af6fb2e88"
      },
      {
        10_000_000,
        "efda178de857b2b1703d8d5403bd0f848e19cff5c50ba5c0d6210ddb16250ec3",
        "005e30899481055e",
        "206aee640c0fd21473d5cc3654d63c80442d9e2dfa676d2801d3ec1fbab38a6d"
      },
      {
        100_000_000,
        "49e15ba4bf501ce8fe88765403bd0f848e19cff5c50ba5c0d6210ddb16250ec3",
        "02abe0589481055e",
        "b879f84923e71b812ef5a42ece0b5b9366c31cab218f40afe65f8a2cae448a6f"
      },
    };
  }

  private final long blockNumber;
  private final String headerHex;
  private final String nonceHex;
  private final String resultHex;

  public ProgPow_0_9_2_HashTest(
      final long blockNumber,
      final String headerHex,
      final String nonceHex,
      final String resultHex) {
    this.blockNumber = blockNumber;
    this.headerHex = headerHex;
    this.nonceHex = nonceHex;
    this.resultHex = resultHex;
  }

  private static final EthHashCacheFactory cacheFactory = new EthHashCacheFactory();

  private ProgPow progPow;

  @Before
  public void setUp() {
    progPow = ProgPow.progPow_0_9_2();
  }

  @Test
  public void validateProPowHash() {
    final EthHashCacheFactory.EthHashDescriptor cache = cacheFactory.ethHashCacheFor(blockNumber);

    final int[] result =
        progPow.progPowHash(
            blockNumber,
            BytesValue.fromHexString(nonceHex).getLong(0),
            toIntArray(BytesValue.fromHexString(headerHex)),
            (target, ind) -> EthHash.calcDatasetItem(target, cache.getCache(), ind));

    assertThat(fromIntArray(result)).isEqualTo(BytesValue.fromHexString(resultHex));
  }

  static int[] toIntArray(final BytesValue bytesValue) {
    Preconditions.checkArgument(
        bytesValue.size() % 4 == 0, "BytesValue length must be divisible by 4.");

    final int[] result = new int[bytesValue.size() / 4];
    for (int i = 0; i < result.length; i++) {
      result[i] = Integer.reverseBytes(bytesValue.getInt(i * 4));
    }
    return result;
  }

  private static BytesValue fromIntArray(final int[] array) {
    final byte[] resultArray = new byte[array.length * 4];
    for (int i = 0; i < array.length; i++) {
      final int val = array[i];
      int bIndex = i * 4;
      resultArray[bIndex++] = (byte) (val);
      resultArray[bIndex++] = (byte) (val >> 8);
      resultArray[bIndex++] = (byte) (val >> 16);
      resultArray[bIndex] = (byte) (val >> 24);
    }
    return BytesValue.wrap(resultArray);
  }
}
