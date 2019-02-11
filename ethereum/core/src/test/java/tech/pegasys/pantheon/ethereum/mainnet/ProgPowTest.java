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

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("RedundantArrayCreation")
public class ProgPowTest {

  private static final EthHashCacheFactory cacheFactory = new EthHashCacheFactory();

  private ProgPow progPow;

  @Before
  public void setUp() {
    progPow = new ProgPow();
  }

  /**
   * Test vectors from
   * https://github.com/XKCP/XKCP/blob/master/tests/TestVectors/KeccakF-800-IntermediateValues.txt.
   */
  // Either redundant array creation or super long autoformatted methods.
  @SuppressWarnings("RedundantArrayCreation")
  @Test
  public void XkcpTestVectorsForKeccakF800() {
    final int[] state = new int[25];

    ProgPow.keccakf800(state);
    assertThat(state)
        .containsExactly(
            new int[] {
              0xE531D45D, 0xF404C6FB, 0x23A0BF99, 0xF1F8452F, 0x51FFD042,
              0xE539F578, 0xF00B80A7, 0xAF973664, 0xBF5AF34C, 0x227A2424,
              0x88172715, 0x9F685884, 0xB15CD054, 0x1BF4FC0E, 0x6166FA91,
              0x1A9E599A, 0xA3970A1F, 0xAB659687, 0xAFAB8D68, 0xE74B1015,
              0x34001A98, 0x4119EFF3, 0x930A0E76, 0x87B28070, 0x11EFE996,
            });

    ProgPow.keccakf800(state);
    assertThat(state)
        .containsExactly(
            new int[] {
              0x75BF2D0D, 0x9B610E89, 0xC826AF40, 0x64CD84AB, 0xF905BDD6,
              0xBC832835, 0x5F8001B9, 0x15662CCE, 0x8E38C95E, 0x701FE543,
              0x1B544380, 0x89ACDEFF, 0x51EDB5DE, 0x0E9702D9, 0x6C19AA16,
              0xA2913EEE, 0x60754E9A, 0x9819063C, 0xF4709254, 0xD09F9084,
              0x772DA259, 0x1DB35DF7, 0x5AA60162, 0x358825D5, 0xB3783BAB,
            });
  }

  @Test
  public void kiss99TestVector() {
    final ProgPow.Kiss99 kiss99 = new ProgPow.Kiss99(362436069, 521288629, 123456789, 380116160);

    assertThat(kiss99.next()).isEqualTo(769445856);
    assertThat(kiss99.next()).isEqualTo(742012328);
    assertThat(kiss99.next()).isEqualTo(2121196314);
    assertThat(kiss99.next()).isEqualTo(-1489346354);

    for (int i = 0; i < 100000 - 5; ++i) {
      kiss99.next();
    }

    // The 100000th number.
    assertThat(kiss99.next()).isEqualTo(941074834);
  }

  @Test
  public void fillMix3000() {
    final int[] result = new int[32];
    progPow.fill_mix(0xEE304846DDD0A47BL, 0, result);

    assertThat(result)
        .containsExactly(
            new int[] {
              0x10C02F0D, 0x99891C9E, 0xC59649A0, 0x43F0394D,
              0x24D2BAE4, 0xC4E89D4C, 0x398AD25C, 0xF5C0E467,
              0x7A3302D6, 0xE6245C6C, 0x760726D3, 0x1F322EE7,
              0x85405811, 0xC2F1E765, 0xA0EB7045, 0xDA39E821,
              0x79FC6A48, 0x089E401F, 0x8488779F, 0xD79E414F,
              0x041A826B, 0x313C0D79, 0x10125A3C, 0x3F4BDFAC,
              0xA7352F36, 0x7E70CB54, 0x3B0BB37D, 0x74A3E24A,
              0xCC37236A, 0xA442B311, 0x955AB27A, 0x6D175B7E
            });

    progPow.fill_mix(0xEE304846DDD0A47BL, 13, result);
    assertThat(result)
        .containsExactly(
            new int[] {
              0x4E46D05D, 0x2E77E734, 0x2C479399, 0x70712177,
              0xA75D7FF5, 0xBEF18D17, 0x8D42252E, 0x35B4FA0E,
              0x462C850A, 0x2DD2B5D5, 0x5F32B5EC, 0xED5D9EED,
              0xF9E2685E, 0x1F29DC8E, 0xA78F098B, 0x86A8687B,
              0xEA7A10E7, 0xBE732B9D, 0x4EEBCB60, 0x94DD7D97,
              0x39A425E9, 0xC0E782BF, 0xBA7B870F, 0x4823FF60,
              0xF97A5A1C, 0xB00BCAF4, 0x02D0F8C4, 0x28399214,
              0xB4CCB32D, 0x83A09132, 0x27EA8279, 0x3837DDA3
            });
  }

  @Test
  public void keccakF800Progpow3000() {
    int[] result =
        ProgPow.keccakF800Progpow(
            new int[] {
              0xCCDDEEFF, 0x8899AABB, 0x44556677, 0x00112233,
              0x33221100, 0x77665544, 0xBBAA9988, 0xFFEEDDCC
            },
            0x123456789ABCDEF0L,
            new int[] {
              0x00000000, 0x00000000, 0x00000000, 0x00000000,
              0x00000000, 0x00000000, 0x00000000, 0x00000000
            });
    assertThat(result)
        .containsExactly(
            new int[] {
              0x464830EE, 0x7BA4D0DD, 0x969E1798, 0xCEC50EB6,
              0x7872E2EA, 0x597E3634, 0xE380E73D, 0x2F89D1E6
            });
    result =
        ProgPow.keccakF800Progpow(
            new int[] {
              0xCCDDEEFF, 0x8899AABB, 0x44556677, 0x00112233,
              0x33221100, 0x77665544, 0xBBAA9988, 0xFFEEDDCC
            },
            0xEE304846DDD0A47BL,
            new int[] {
              0x0598F111, 0x66B48AC5, 0x719CFF10, 0x5F0ACF9D,
              0x162FFA18, 0xEF8E7905, 0x21470C77, 0x7D767492
            });
    assertThat(result)
        .containsExactly(
            new int[] {
              0x47CD7C5B, 0xD9FDBE2D, 0xAC5C895B, 0xFF67CE8E,
              0x6B5AEB0D, 0xE1C6ECD2, 0x003D3862, 0xCE8E72C3
            });
  }

  @Test
  public void math_chfastHash30000() {
    assertThat(ProgPow.math(0x8626BB1F, 0xBBDFBC4E, 0x883E5B49)).isEqualTo(0x4206776D); // add
    assertThat(ProgPow.math(0x3F4BDFAC, 0xD79E414F, 0x36B71236)).isEqualTo(0x4C5CB214); // mul
    assertThat(ProgPow.math(0x6D175B7E, 0xC4E89D4C, 0x944ECABB)).isEqualTo(0x53E9023F); // mul_hi32
    assertThat(ProgPow.math(0x2EDDD94C, 0x7E70CB54, 0x3F472A85)).isEqualTo(0x2EDDD94C); // min
    assertThat(ProgPow.math(0x8A81E396, 0x3F4BDFAC, 0xCEC46E67)).isEqualTo(0x1E3968A8); // rotl32
    assertThat(ProgPow.math(0x8A81E396, 0x7E70CB54, 0xDBE71FF7)).isEqualTo(0x1E3968A8); // rotr32
    assertThat(ProgPow.math(0xA7352F36, 0xA0EB7045, 0x59E7B9D8))
        .isEqualTo(0xA0212004); // bitwise and
    assertThat(ProgPow.math(0xC89805AF, 0x64291E2F, 0x1BDC84A9))
        .isEqualTo(0xECB91FAF); // bitwise or
    assertThat(ProgPow.math(0x760726D3, 0x79FC6A48, 0xC675CAC5))
        .isEqualTo(0x0FFB4C9B); // bitwise xor
    assertThat(ProgPow.math(0x75551D43, 0x3383BA34, 0x2863AD31))
        .isEqualTo(0x00000003); // clz, leading zeros
    assertThat(ProgPow.math(0xEA260841, 0xE92C44B7, 0xF83FFE7D))
        .isEqualTo(0x0000001B); // popcount, number of 1s
  }

  @Test
  public void merge_chfastHash30000() {
    assertThat(ProgPow.merge(0x3B0BB37D, 0xA0212004, 0x9BD26AB0)).isEqualTo(0x3CA34321); // mul/add
    assertThat(ProgPow.merge(0x10C02F0D, 0x870FA227, 0xD4F45515)).isEqualTo(0x91C1326A); // xor/mul
    assertThat(ProgPow.merge(0x24D2BAE4, 0x0FFB4C9B, 0x7FDBC2F2)).isEqualTo(0x2EDDD94C); // rotl/xor
    assertThat(ProgPow.merge(0xDA39E821, 0x089C4008, 0x8B6CD8C3)).isEqualTo(0x8A81E396); // rotr/xor
  }

  @Test
  public void chfastHash30000() {
    final long blockNumber = 30000;
    final EthHashCacheFactory.EthHashDescriptor cache = cacheFactory.ethHashCacheFor(blockNumber);

    final int[] result =
        progPow.progPowHash(
            blockNumber,
            0x123456789abcdef0L,
            ProgPowHashTest.toIntArray(
                BytesValue.fromHexString(
                    "ffeeddccbbaa9988776655443322110000112233445566778899aabbccddeeff")),
            (target, ind) -> EthHash.calcDatasetItem(target, cache.getCache(), ind));

    ProgPowHashTest.toIntArray(
        BytesValue.fromHexString(
            "11f19805c58ab46610ff9c719dcf0a5f18fa2f1605798eef770c47219274767d"));
    assertThat(result)
        .containsExactly(
            ProgPowHashTest.toIntArray(
                BytesValue.fromHexString(
                    "5b7ccd472dbefdd95b895cac8ece67ff0deb5a6bd2ecc6e162383d00c3728ece")));
  }
}
