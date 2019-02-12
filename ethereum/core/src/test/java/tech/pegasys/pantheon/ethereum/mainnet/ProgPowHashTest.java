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
public class ProgPowHashTest {

  @Parameters
  public static Object[][] getParameters() {
    return new Object[][] {
      {
        "chfast hash_3000",
        30000,
        "ffeeddccbbaa9988776655443322110000112233445566778899aabbccddeeff",
        "123456789abcdef0",
        "11f19805c58ab46610ff9c719dcf0a5f18fa2f1605798eef770c47219274767d",
        "5b7ccd472dbefdd95b895cac8ece67ff0deb5a6bd2ecc6e162383d00c3728ece",
      },
      {
        "chfast block 0",
        0,
        "0000000000000000000000000000000000000000000000000000000000000000",
        "0000000000000000",
        "faeb1be51075b03a4ff44b335067951ead07a3b078539ace76fd56fc410557a3",
        "63155f732f2bf556967f906155b510c917e48e99685ead76ea83f4eca03ab12b"
      },
      {
        "chfast block 49",
        49,
        "63155f732f2bf556967f906155b510c917e48e99685ead76ea83f4eca03ab12b",
        "0000000006ff2c47",
        "c789c1180f890ec555ff42042913465481e8e6bc512cb981e1c1108dc3f2227d",
        "9e7248f20914913a73d80a70174c331b1d34f260535ac3631d770e656b5dd922"
      },
      {
        "chfast block 50",
        50,
        "9e7248f20914913a73d80a70174c331b1d34f260535ac3631d770e656b5dd922",
        "00000000076e482e",
        "c7340542c2a06b3a7dc7222635f7cd402abf8b528ae971ddac6bbe2b0c7cb518",
        "de37e1824c86d35d154cf65a88de6d9286aec4f7f10c3fc9f0fa1bcc2687188d"
      },
      {
        "chfast block 99",
        99,
        "de37e1824c86d35d154cf65a88de6d9286aec4f7f10c3fc9f0fa1bcc2687188d",
        "000000003917afab",
        "f5e60b2c5bfddd136167a30cbc3c8dbdbd15a512257dee7964e0bc6daa9f8ba7",
        "ac7b55e801511b77e11d52e9599206101550144525b5679f2dab19386f23dcce"
      },
      {
        "chfast block 29950",
        29950,
        "ac7b55e801511b77e11d52e9599206101550144525b5679f2dab19386f23dcce",
        "005d409dbc23a62a",
        "07393d15805eb08ee6fc6cb3ad4ad1010533bd0ff92d6006850246829f18fd6e",
        "e43d7e0bdc8a4a3f6e291a5ed790b9fa1a0948a2b9e33c844888690847de19f5"
      },
      {
        "chfast block 29999",
        29999,
        "e43d7e0bdc8a4a3f6e291a5ed790b9fa1a0948a2b9e33c844888690847de19f5",
        "005db5fa4c2a3d03",
        "7551bddf977491da2f6cfc1679299544b23483e8f8ee0931c4c16a796558a0b8",
        "d34519f72c97cae8892c277776259db3320820cb5279a299d0ef1e155e5c6454"
      },
      {
        "chfast block 300000",
        30000,
        "d34519f72c97cae8892c277776259db3320820cb5279a299d0ef1e155e5c6454",
        "005db8607994ff30",
        "f1c2c7c32266af9635462e6ce1c98ebe4e7e3ecab7a38aaabfbf2e731e0fbff4",
        "8b6ce5da0b06d18db7bd8492d9e5717f8b53e7e098d9fef7886d58a6e913ef64"
      },
      {
        "chfast block 30049",
        30049,
        "8b6ce5da0b06d18db7bd8492d9e5717f8b53e7e098d9fef7886d58a6e913ef64",
        "005e2e215a8ca2e7",
        "57fe6a9fbf920b4e91deeb66cb0efa971e08229d1a160330e08da54af0689add",
        "c2c46173481b9ced61123d2e293b42ede5a1b323210eb2a684df0874ffe09047"
      },
      {
        "chfast block 30050",
        30050,
        "c2c46173481b9ced61123d2e293b42ede5a1b323210eb2a684df0874ffe09047",
        "005e30899481055e",
        "ba30c61cc5a2c74a5ecaf505965140a08f24a296d687e78720f0b48baf712f2d",
        "ea42197eb2ba79c63cb5e655b8b1f612c5f08aae1a49ff236795a3516d87bc71"
      },
      {
        "chfast block 30099",
        30099,
        "ea42197eb2ba79c63cb5e655b8b1f612c5f08aae1a49ff236795a3516d87bc71",
        "005ea6aef136f88b",
        "cfd5e46048cd133d40f261fe8704e51d3f497fc14203ac6a9ef6a0841780b1cd",
        "49e15ba4bf501ce8fe8876101c808e24c69a859be15de554bf85dbc095491bd6"
      },
      {
        "chfast block 59950",
        59950,
        "49e15ba4bf501ce8fe8876101c808e24c69a859be15de554bf85dbc095491bd6",
        "02ebe0503bd7b1da",
        "21511fbaa31fb9f5fc4998a754e97b3083a866f4de86fa7500a633346f56d773",
        "f5c50ba5c0d6210ddb16250ec3efda178de857b2b1703d8d5403bd0f848e19cf"
      },
      {
        "chfast block 59999",
        59999,
        "f5c50ba5c0d6210ddb16250ec3efda178de857b2b1703d8d5403bd0f848e19cf",
        "02edb6275bd221e3",
        "653eda37d337e39d311d22be9bbd3458d3abee4e643bee4a7280a6d08106ef98",
        "341562d10d4afb706ec2c8d5537cb0c810de02b4ebb0a0eea5ae335af6fb2e88"
      },
    };
  }

  private String testName;
  private long blockNumber;
  private String headerHex;
  private String nonceHex;
  private String mixHashHex;
  private String resultHex;

  public ProgPowHashTest(
      final String testName,
      final long blockNumber,
      final String headerHex,
      final String nonceHex,
      final String mixHashHex,
      final String resultHex) {
    this.testName = testName;
    this.blockNumber = blockNumber;
    this.headerHex = headerHex;
    this.nonceHex = nonceHex;
    this.mixHashHex = mixHashHex;
    this.resultHex = resultHex;
  }

  private static final EthHashCacheFactory cacheFactory = new EthHashCacheFactory();

  private ProgPow progPow;

  @Before
  public void setUp() {
    progPow = new ProgPow();
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
