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
public class ProgPow_0_9_3_HashTest {

  @Parameters
  public static Object[][] getParameters() {
    return new Object[][] {
      {
        30000,
        "ffeeddccbbaa9988776655443322110000112233445566778899aabbccddeeff",
        "123456789abcdef0",
        "34d8436444aa5c61761ce0bcce0f11401df2eace77f5c14ba7039b86b5800c08",
      },
      {
        0,
        "0000000000000000000000000000000000000000000000000000000000000000",
        "0000000000000000",
        "b3bad9ca6f7c566cf0377d1f8cce29d6516a96562c122d924626281ec948ef02"
      },
      {
        49,
        "63155f732f2bf556967f906155b510c917e48e99685ead76ea83f4eca03ab12b",
        "0000000006ff2c47",
        "ca0c365f1290ede4ee0d19cab08cd827030425ae8aba96b5248faafe732f1f80"
      },
      {
        50,
        "9e7248f20914913a73d80a70174c331b1d34f260535ac3631d770e656b5dd922",
        "00000000076e482e",
        "75439f6c6e153d3c798309f01ba37e7a284d172f50841c7b523e81c1b8247083"
      },
      {
        99,
        "de37e1824c86d35d154cf65a88de6d9286aec4f7f10c3fc9f0fa1bcc2687188d",
        "000000003917afab",
        "2618185c024ad29fd75bc350da388cc0d47cdebbd6798400f17692a7ccf3314c"
      },
      {
        29950,
        "ac7b55e801511b77e11d52e9599206101550144525b5679f2dab19386f23dcce",
        "005d409dbc23a62a",
        "8ec8a0486e759c59c6f7ba586450dc2a5c8c3586b52345efb9b604fa82f40f65"
      },
      {
        29999,
        "e43d7e0bdc8a4a3f6e291a5ed790b9fa1a0948a2b9e33c844888690847de19f5",
        "005db5fa4c2a3d03",
        "de03c1354159e07cf804ecc9a53f82b0187dd4a24837d20e56cae28b65c35eb0"
      },
      {
        30000,
        "d34519f72c97cae8892c277776259db3320820cb5279a299d0ef1e155e5c6454",
        "005db8607994ff30",
        "a717a28081999625860cbb09262dbbcc6090427411a5a3c60fb86a0ded8d369e"
      },
      {
        30049,
        "8b6ce5da0b06d18db7bd8492d9e5717f8b53e7e098d9fef7886d58a6e913ef64",
        "005e2e215a8ca2e7",
        "dd85d293db9b1063c6428ac9ca74e8d5d4d9fee49e0123bafb914fa787f58e89"
      },
      {
        30050,
        "c2c46173481b9ced61123d2e293b42ede5a1b323210eb2a684df0874ffe09047",
        "005e30899481055e",
        "4e83a686a5390b8105a261c4c1480b23a17938d4d029d1239042be7515e980fa"
      },
      {
        30099,
        "ea42197eb2ba79c63cb5e655b8b1f612c5f08aae1a49ff236795a3516d87bc71",
        "005ea6aef136f88b",
        "72a6b01403faf90b2e74cb28920e953016d2c04f3e22d64aa4712ed00b5b6681"
      },
      {
        59950,
        "49e15ba4bf501ce8fe8876101c808e24c69a859be15de554bf85dbc095491bd6",
        "02ebe0503bd7b1da",
        "56c9fefbfe93eac6de18b1bd4e42d6bf784f9dc5a112955d2ffa6d5fb3cc0657"
      },
      {
        59999,
        "f5c50ba5c0d6210ddb16250ec3efda178de857b2b1703d8d5403bd0f848e19cf",
        "02edb6275bd221e3",
        "116053ccb7866e23df4263a359794fa84afceb0d11d97cb9389ffa763b7be43a"
      },
      {
        10_000_000,
        "efda178de857b2b1703d8d5403bd0f848e19cff5c50ba5c0d6210ddb16250ec3",
        "005e30899481055e",
        "5dca7eab5997b489420b5d05d56394b8be83824bcb5916b84d8b39d54186a6d6"
      },
      {
        100_000_000,
        "49e15ba4bf501ce8fe88765403bd0f848e19cff5c50ba5c0d6210ddb16250ec3",
        "02abe0589481055e",
        "eba819f45d27b39cc0a8deb68b6dde03c37a9790634eeb6a1d0edb40ed26ee1d"
      },
    };
  }

  private final long blockNumber;
  private final String headerHex;
  private final String nonceHex;
  private final String resultHex;

  public ProgPow_0_9_3_HashTest(
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
    progPow = ProgPow.progPow_0_9_3();
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
