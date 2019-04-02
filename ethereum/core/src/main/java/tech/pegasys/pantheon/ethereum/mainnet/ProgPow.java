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

import static com.google.common.base.Preconditions.checkArgument;

import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.Objects;
import java.util.function.BiConsumer;

class ProgPow {

  /** Number of blocks before changing the random program. */
  private final int progPowPeriod;

  /** The number of parallel lanes that coordinate to calculate a single hash instance. */
  final int progPowLanes;

  /** The register file usage size */
  final int progPowRegs;

  /** Number of uint32 loads from the DAG per lane. */
  private final int progPowDagLoads;

  /** The size of the cache. */
  private final int progPowCacheBytes;

  /** The number of DAG accesses, defined as the outer loop of the algorithm. */
  private final int progPowCntDag;

  /** The number of cache accesses per loop. */
  private final int progPowCntCache;

  /** The number of math operations per loop. */
  private final int progPowCntMath;

  /** FNV 32-bit prime. */
  private static final int FNV_PRIME = 0x01000193;

  /** FNV 32-bit offset basis. */
  private static final int FNV_OFFSET_BASIS = 0x811c9dc5;

  private ProgPow(
      final int progPowPeriod,
      final int progPowLanes,
      final int progPowRegs,
      final int progPowDagLoads,
      final int progPowCacheBytes,
      final int progPowCntDag,
      final int progPowCntCache,
      final int progPowCntMath) {
    this.progPowPeriod = progPowPeriod;
    this.progPowLanes = progPowLanes;
    this.progPowRegs = progPowRegs;
    this.progPowDagLoads = progPowDagLoads;
    this.progPowCacheBytes = progPowCacheBytes;
    this.progPowCntDag = progPowCntDag;
    this.progPowCntCache = progPowCntCache;
    this.progPowCntMath = progPowCntMath;
  }

  public static ProgPow progPow_0_9_2() {
    return new ProgPow(
        /* PROG_POW_PERIOD      */ 50,
        /* PROG_POW_LANES       */ 16,
        /* PROG_POW_REGS        */ 32,
        /* PROG_POW_PERIOD      */ 4,
        /* PROG_POW_CACHE_BYTES */ 16 * 1024,
        /* PROG_POW_CNT_DAG     */ 64,
        /* PROG_POW_CNT_CACHE   */ 12,
        /* PROG_POW_CNT_MATH    */ 20);
  }

  public static ProgPow progPow_0_9_3() {
    return new ProgPow(
        /* PROG_POW_PERIOD      */ 10,
        /* PROG_POW_LANES       */ 16,
        /* PROG_POW_REGS        */ 32,
        /* PROG_POW_PERIOD      */ 4,
        /* PROG_POW_CACHE_BYTES */ 16 * 1024,
        /* PROG_POW_CNT_DAG     */ 64,
        /* PROG_POW_CNT_CACHE   */ 11,
        /* PROG_POW_CNT_MATH    */ 18);
  }

  static int fnv1a(final int h, final int d) {
    return (h ^ d) * FNV_PRIME;
  }

  static class Kiss99 {
    int z, w, jsr, jcong;

    Kiss99(final int z, final int w, final int jsr, final int jcong) {
      this.z = z;
      this.w = w;
      this.jsr = jsr;
      this.jcong = jcong;
    }

    // KISS99 is simple, fast, and passes the TestU01 suite
    // https://en.wikipedia.org/wiki/KISS_(algorithm)
    // http://www.cse.yorku.ca/~oz/marsaglia-rng.html
    int next() {
      z = 36969 * (z & 65535) + (z >>> 16);
      w = 18000 * (w & 65535) + (w >>> 16);
      final int MWC = ((z << 16) + w);
      jsr ^= (jsr << 17);
      jsr ^= (jsr >>> 13);
      jsr ^= (jsr << 5);
      jcong = 69069 * jcong + 1234567;
      return ((MWC ^ jcong) + jsr);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Kiss99 kiss99 = (Kiss99) o;
      return z == kiss99.z && w == kiss99.w && jsr == kiss99.jsr && jcong == kiss99.jcong;
    }

    @Override
    public int hashCode() {
      return Objects.hash(z, w, jsr, jcong);
    }
  }

  void fill_mix(final long seed, final int lane_id, final int[] mix) {
    checkArgument(mix.length == progPowRegs);
    // Use FNV to expand the per-warp seed to per-lane
    // Use KISS to expand the per-lane seed to fill mix
    final int z = fnv1a(FNV_OFFSET_BASIS, (int) seed);
    final int w = fnv1a(z, (int) (seed >> 32));
    final int jsr = fnv1a(w, lane_id);
    final int jcong = fnv1a(jsr, lane_id);
    final Kiss99 kiss99 = new Kiss99(z, w, jsr, jcong);
    for (int i = 0; i < progPowRegs; i++) {
      mix[i] = kiss99.next();
    }
  }

  private static final int[] keccakRoundConstants = {
    0x00000001, 0x00008082, 0x0000808A, 0x80008000,
    0x0000808B, 0x80000001, 0x80008081, 0x00008009,
    0x0000008A, 0x00000088, 0x80008009, 0x8000000A,
    0x8000808B, 0x0000008B, 0x00008089, 0x00008003,
    0x00008002, 0x00000080, 0x0000800A, 0x8000000A,
    0x80008081, 0x00008080,
  };

  /**
   * Derived from {#link org.bouncycastle.crypto.digests.KeccakDigest#KeccakPermutation()}.
   * Copyright (c) 2000-2017 The Legion Of The Bouncy Castle Inc. (http://www.bouncycastle.org) The
   * original source is licensed under a MIT license.
   */
  static void keccakf800(final int[] state) {
    int a00 = state[0], a01 = state[1], a02 = state[2], a03 = state[3], a04 = state[4];
    int a05 = state[5], a06 = state[6], a07 = state[7], a08 = state[8], a09 = state[9];
    int a10 = state[10], a11 = state[11], a12 = state[12], a13 = state[13], a14 = state[14];
    int a15 = state[15], a16 = state[16], a17 = state[17], a18 = state[18], a19 = state[19];
    int a20 = state[20], a21 = state[21], a22 = state[22], a23 = state[23], a24 = state[24];

    for (int i = 0; i < 22; i++) {
      // theta
      int c0 = a00 ^ a05 ^ a10 ^ a15 ^ a20;
      int c1 = a01 ^ a06 ^ a11 ^ a16 ^ a21;
      final int c2 = a02 ^ a07 ^ a12 ^ a17 ^ a22;
      final int c3 = a03 ^ a08 ^ a13 ^ a18 ^ a23;
      final int c4 = a04 ^ a09 ^ a14 ^ a19 ^ a24;

      final int d1 = (c1 << 1 | c1 >>> 31) ^ c4;
      final int d2 = (c2 << 1 | c2 >>> 31) ^ c0;
      final int d3 = (c3 << 1 | c3 >>> 31) ^ c1;
      final int d4 = (c4 << 1 | c4 >>> 31) ^ c2;
      final int d0 = (c0 << 1 | c0 >>> 31) ^ c3;

      a00 ^= d1;
      a05 ^= d1;
      a10 ^= d1;
      a15 ^= d1;
      a20 ^= d1;
      a01 ^= d2;
      a06 ^= d2;
      a11 ^= d2;
      a16 ^= d2;
      a21 ^= d2;
      a02 ^= d3;
      a07 ^= d3;
      a12 ^= d3;
      a17 ^= d3;
      a22 ^= d3;
      a03 ^= d4;
      a08 ^= d4;
      a13 ^= d4;
      a18 ^= d4;
      a23 ^= d4;
      a04 ^= d0;
      a09 ^= d0;
      a14 ^= d0;
      a19 ^= d0;
      a24 ^= d0;

      // rho/pi
      c1 = a01 << 1 | a01 >>> 31;
      a01 = a06 << 12 | a06 >>> 20;
      a06 = a09 << 20 | a09 >>> 12;
      a09 = a22 << 29 | a22 >>> 3;
      a22 = a14 << 7 | a14 >>> 25;
      a14 = a20 << 18 | a20 >>> 14;
      a20 = a02 << 30 | a02 >>> 2;
      a02 = a12 << 11 | a12 >>> 21;
      a12 = a13 << 25 | a13 >>> 7;
      a13 = a19 << 8 | a19 >>> 24;
      a19 = a23 << 24 | a23 >>> 8;
      a23 = a15 << 9 | a15 >>> 23;
      a15 = a04 << 27 | a04 >>> 5;
      a04 = a24 << 14 | a24 >>> 18;
      a24 = a21 << 2 | a21 >>> 30;
      a21 = a08 << 23 | a08 >>> 9;
      a08 = a16 << 13 | a16 >>> 19;
      a16 = a05 << 4 | a05 >>> 28;
      a05 = a03 << 28 | a03 >>> 4;
      a03 = a18 << 21 | a18 >>> 11;
      a18 = a17 << 15 | a17 >>> 17;
      a17 = a11 << 10 | a11 >>> 22;
      a11 = a07 << 6 | a07 >>> 26;
      a07 = a10 << 3 | a10 >>> 29;
      a10 = c1;

      // chi
      c0 = a00 ^ (~a01 & a02);
      c1 = a01 ^ (~a02 & a03);
      a02 ^= ~a03 & a04;
      a03 ^= ~a04 & a00;
      a04 ^= ~a00 & a01;
      a00 = c0;
      a01 = c1;

      c0 = a05 ^ (~a06 & a07);
      c1 = a06 ^ (~a07 & a08);
      a07 ^= ~a08 & a09;
      a08 ^= ~a09 & a05;
      a09 ^= ~a05 & a06;
      a05 = c0;
      a06 = c1;

      c0 = a10 ^ (~a11 & a12);
      c1 = a11 ^ (~a12 & a13);
      a12 ^= ~a13 & a14;
      a13 ^= ~a14 & a10;
      a14 ^= ~a10 & a11;
      a10 = c0;
      a11 = c1;

      c0 = a15 ^ (~a16 & a17);
      c1 = a16 ^ (~a17 & a18);
      a17 ^= ~a18 & a19;
      a18 ^= ~a19 & a15;
      a19 ^= ~a15 & a16;
      a15 = c0;
      a16 = c1;

      c0 = a20 ^ (~a21 & a22);
      c1 = a21 ^ (~a22 & a23);
      a22 ^= ~a23 & a24;
      a23 ^= ~a24 & a20;
      a24 ^= ~a20 & a21;
      a20 = c0;
      a21 = c1;

      // iota
      a00 ^= keccakRoundConstants[i];
    }

    state[0] = a00;
    state[1] = a01;
    state[2] = a02;
    state[3] = a03;
    state[4] = a04;
    state[5] = a05;
    state[6] = a06;
    state[7] = a07;
    state[8] = a08;
    state[9] = a09;
    state[10] = a10;
    state[11] = a11;
    state[12] = a12;
    state[13] = a13;
    state[14] = a14;
    state[15] = a15;
    state[16] = a16;
    state[17] = a17;
    state[18] = a18;
    state[19] = a19;
    state[20] = a20;
    state[21] = a21;
    state[22] = a22;
    state[23] = a23;
    state[24] = a24;
  }

  static int[] keccakF800Progpow(final int[] header, final long seed, final int[] digest) {
    final int[] state = new int[25];

    System.arraycopy(header, 0, state, 0, 8);
    state[8] = (int) seed;
    state[9] = (int) (seed >> 32);
    System.arraycopy(digest, 0, state, 10, 8);

    keccakf800(state);

    final int[] ret = new int[8];
    System.arraycopy(state, 0, ret, 0, 8);
    return ret;
  }

  public int[] progPowHash(
      final long blockNumber,
      //      final long prog_seed, // value is (block_number/progPowPeriod)
      final long nonce,
      final int[] header,
      final int[] cachedDag,
      final BiConsumer<byte[], Integer> datasetLookup) {
    final int[][] mix = new int[progPowLanes][progPowRegs];
    final int[] digest = new int[8];

    // keccak(header..nonce)
    final int[] seed_256 = keccakF800Progpow(header, nonce, digest);
    // endian swap so byte 0 of the hash is the MSB of the value
    final long seed =
        (Integer.toUnsignedLong(Integer.reverseBytes(seed_256[0])) << 32)
            | Integer.toUnsignedLong(Integer.reverseBytes(seed_256[1]));

    // initialize mix for all lanes
    for (int l = 0; l < progPowLanes; l++) {
      fill_mix(seed, l, mix[l]);
    }

    // execute the randomly generated inner loop
    for (int i = 0; i < progPowCntDag; i++) {
      progPowLoop(blockNumber, i, mix, cachedDag, datasetLookup);
    }

    // Reduce mix data to a per-lane 32-bit digest
    final int[] digest_lane = new int[progPowLanes];
    for (int l = 0; l < progPowLanes; l++) {
      digest_lane[l] = FNV_OFFSET_BASIS;
      for (int i = 0; i < progPowRegs; i++) {
        digest_lane[l] = fnv1a(digest_lane[l], mix[l][i]);
      }
    }
    // Reduce all lanes to a single 256-bit digest
    for (int i = 0; i < 8; i++) {
      digest[i] = FNV_OFFSET_BASIS;
    }
    for (int l = 0; l < progPowLanes; l++) {
      digest[l % 8] = fnv1a(digest[l % 8], digest_lane[l]);
    }
    return keccakF800Progpow(header, seed, digest);
  }

  Kiss99 progPowInit(final long prog_seed, final int[] mixSeqDst, final int[] mixSeqSrc) {
    checkArgument(mixSeqDst.length == progPowRegs);
    checkArgument(mixSeqSrc.length == progPowRegs);
    final int z = fnv1a(FNV_OFFSET_BASIS, (int) prog_seed);
    final int w = fnv1a(z, (int) (prog_seed >> 32));
    final int jsr = fnv1a(w, (int) prog_seed);
    final int jcong = fnv1a(jsr, (int) (prog_seed >> 32));
    final Kiss99 progRnd = new Kiss99(z, w, jsr, jcong);
    // Create a random sequence of mix destinations for merge() and mix sources for cache reads
    // guarantees every destination merged once
    // guarantees no duplicate cache reads, which could be optimized away
    // Uses Fisher-Yates shuffle
    for (int i = 0; i < progPowRegs; i++) {
      mixSeqDst[i] = i;
      mixSeqSrc[i] = i;
    }
    for (int i = progPowRegs - 1; i > 0; i--) {
      int j;
      j = Integer.remainderUnsigned(progRnd.next(), (i + 1));
      int tmp = mixSeqDst[i];
      mixSeqDst[i] = mixSeqDst[j];
      mixSeqDst[j] = tmp;
      j = Integer.remainderUnsigned(progRnd.next(), (i + 1));
      tmp = mixSeqSrc[i];
      mixSeqSrc[i] = mixSeqSrc[j];
      mixSeqSrc[j] = tmp;
    }
    return progRnd;
  }

  // Merge new data from b into the value in a
  // Assuming A has high entropy only do ops that retain entropy
  // even if B is low entropy
  // (IE don't do A&B)
  static int merge(final int a, final int b, final int r) {
    switch (Integer.remainderUnsigned(r, 4)) {
      case 0:
        return (a * 33) + b;
      case 1:
        return (a ^ b) * 33;
        // prevent rotate by 0 which is a NOP
      case 2:
        return Integer.rotateLeft(a, ((r >>> 16) % 31) + 1) ^ b;
      case 3:
        return Integer.rotateRight(a, ((r >>> 16) % 31) + 1) ^ b;
    }
    throw new RuntimeException("This should be impossible.");
  }

  // Random math between two input values
  static int math(final int a, final int b, final int r) {
    switch (Integer.remainderUnsigned(r, 11)) {
      case 0:
        return a + b;
      case 1:
        return a * b;
      case 2:
        return (int) ((Integer.toUnsignedLong(a) * Integer.toUnsignedLong(b)) >> 32);
      case 3:
        return Integer.compareUnsigned(a, b) > 0 ? b : a;
      case 4:
        return Integer.rotateLeft(a, b);
      case 5:
        return Integer.rotateRight(a, b);
      case 6:
        return a & b;
      case 7:
        return a | b;
      case 8:
        return a ^ b;
      case 9:
        return Integer.numberOfLeadingZeros(a) + Integer.numberOfLeadingZeros(b);
      case 10:
        return Integer.bitCount(a) + Integer.bitCount(b);
    }
    throw new RuntimeException("This should be impossible.");
  }

  public int[] createDagCache(
      final long blockNumber, final BiConsumer<byte[], Integer> datasetLookup) {
    // cache the first 16KB of the dag
    final int[] cdag = new int[4096];
    final byte[] lookupHolder = new byte[64];
    for (int i = 0; i < 4096; i++) {
      // this could be sped up 16x
      datasetLookup.accept(lookupHolder, i >> 4);
      final int dagValue =
          Integer.reverseBytes(BytesValue.wrap(lookupHolder).getInt((i & 0xf) << 2));
      cdag[i] = dagValue;
    }
    return cdag;
  }

  void progPowLoop(
      final long blockNum,
      final int loop,
      final int[][] mix,
      final int[] cDag,
      final BiConsumer<byte[], Integer> datasetLookup) {
    final long datasetSize = EthHash.datasetSize(blockNum / EthHash.EPOCH_LENGTH);
    final int[][] dagEntry = new int[progPowLanes][progPowDagLoads];
    final int dagAddrBase =
        Integer.remainderUnsigned(
            mix[loop % progPowLanes][0],
            Math.toIntExact(datasetSize / (progPowLanes * progPowDagLoads * Integer.BYTES)));

    final byte[] lookupHolder = new byte[64];
    for (int l = 0; l < progPowLanes; l++) {
      final int dagAddrLane =
          dagAddrBase * progPowLanes + Integer.remainderUnsigned(l ^ loop, progPowLanes);
      final int offset = Integer.remainderUnsigned(l ^ loop, progPowLanes);
      for (int i = 0; i < progPowDagLoads; i++) {
        final int lookup = dagAddrLane / 4 + (offset >> 4);
        datasetLookup.accept(lookupHolder, lookup);
        final int lookupOffset = (i * 4 + ((offset & 0xf) << 4)) % 64;
        dagEntry[l][i] = Integer.reverseBytes(BytesValue.wrap(lookupHolder).getInt(lookupOffset));
      }
    }

    // Initialize the program seed and sequences
    // When mining these are evaluated on the CPU and compiled away
    final int[] mix_seq_dst = new int[progPowRegs];
    final int[] mix_seq_src = new int[progPowRegs];
    int mix_seq_dst_cnt = 0;
    int mix_seq_src_cnt = 0;
    final Kiss99 prog_rnd = progPowInit(blockNum / progPowPeriod, mix_seq_dst, mix_seq_src);

    final int max_i = Integer.max(progPowCntCache, progPowCntMath);
    for (int i = 0; i < max_i; i++) {
      if (i < progPowCntCache) {
        // Cached memory access
        // lanes access random 32-bit locations within the first portion of the DAG
        final int src = mix_seq_src[(mix_seq_src_cnt++) % progPowRegs];
        final int dst = mix_seq_dst[(mix_seq_dst_cnt++) % progPowRegs];
        final int sel = prog_rnd.next();

        for (int l = 0; l < progPowLanes; l++) {
          final int offset =
              Integer.remainderUnsigned(mix[l][src], (progPowCacheBytes / Integer.BYTES));
          final int cdagValue = cDag[offset];
          mix[l][dst] = merge(mix[l][dst], cdagValue, sel);
        }
      }
      if (i < progPowCntMath) {
        // Random Math
        // Generate 2 unique sources
        final int src_rnd =
            Integer.remainderUnsigned(prog_rnd.next(), (progPowRegs * (progPowRegs - 1)));
        final int src1 = Integer.remainderUnsigned(src_rnd, progPowRegs); // 0 <= src1 < progPowRegs
        int src2 = src_rnd / progPowRegs; // 0 <= src2 < progPowRegs - 1
        if (src2 >= src1) ++src2; // src2 is now any reg other than src1
        final int sel1 = prog_rnd.next();
        final int dst = mix_seq_dst[(mix_seq_dst_cnt++) % progPowRegs];
        final int sel2 = prog_rnd.next();
        for (int l = 0; l < progPowLanes; l++) {
          final int data = math(mix[l][src1], mix[l][src2], sel1);
          mix[l][dst] = merge(mix[l][dst], data, sel2);
        }
      }
    }

    // Consume the global load data at the very end of the loop to allow full latency hiding
    // Always merge into mix[0] to feed the offset calculation
    for (int i = 0; i < progPowDagLoads; i++) {
      final int dst = (i == 0) ? 0 : mix_seq_dst[(mix_seq_dst_cnt++) % progPowRegs];
      final int sel = prog_rnd.next();
      for (int l = 0; l < progPowLanes; l++) {
        mix[l][dst] = merge(mix[l][dst], dagEntry[l][i], sel);
      }
    }
  }
}
