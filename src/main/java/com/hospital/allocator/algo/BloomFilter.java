package com.hospital.allocator.algo;

import java.nio.charset.StandardCharsets;

/**
 * Bloom filter for string keys. Self-implemented bit array and hash functions.
 *
 * A "true" from {@link #mightContain} means POSSIBLY PRESENT (false positives happen).
 * A "false" is always correct (no false negatives). Always confirm positives with an
 * authoritative exact check when exactness matters.
 *
 * Sizing: m = ceil(-n ln p / (ln 2)^2) bits, k = round(m / n * ln 2) hash functions.
 * Time: O(k) per add / query. Space: O(m) bits.
 */
public final class BloomFilter {

    private final long[] words;
    private final int bitCount;
    private final int hashCount;
    private int inserted;

    public BloomFilter(long expectedItems, double falsePositiveRate) {
        if (expectedItems <= 0) {
            throw new IllegalArgumentException("expectedItems must be positive");
        }
        if (!(falsePositiveRate > 0 && falsePositiveRate < 1)) {
            throw new IllegalArgumentException("falsePositiveRate must be in (0, 1)");
        }
        double ln2 = Math.log(2);
        double m = Math.ceil(-expectedItems * Math.log(falsePositiveRate) / (ln2 * ln2));
        if (m > Integer.MAX_VALUE - 64) {
            throw new IllegalArgumentException("Filter would be too large; relax the false-positive rate");
        }
        this.bitCount = (int) Math.max(64, m);
        this.hashCount = (int) Math.max(1, Math.round((double) bitCount / expectedItems * ln2));
        this.words = new long[(bitCount + 63) >>> 6];
    }

    public void add(String key) {
        long h1 = hash(key, 0xcbf29ce484222325L);
        long h2 = hash(key, 0x9e3779b97f4a7c15L) | 1L;
        for (int i = 0; i < hashCount; i++) {
            int idx = index(h1, h2, i);
            words[idx >>> 6] |= 1L << (idx & 63);
        }
        inserted++;
    }

    public boolean mightContain(String key) {
        long h1 = hash(key, 0xcbf29ce484222325L);
        long h2 = hash(key, 0x9e3779b97f4a7c15L) | 1L;
        for (int i = 0; i < hashCount; i++) {
            int idx = index(h1, h2, i);
            if ((words[idx >>> 6] & (1L << (idx & 63))) == 0) {
                return false;
            }
        }
        return true;
    }

    /** Theoretical false-positive rate for the current fill: (1 - e^(-k n / m))^k. */
    public double estimatedFalsePositiveRate() {
        return Math.pow(1 - Math.exp(-(double) hashCount * inserted / bitCount), hashCount);
    }

    public int bitCount() {
        return bitCount;
    }

    public int hashCount() {
        return hashCount;
    }

    public int insertedCount() {
        return inserted;
    }

    private int index(long h1, long h2, int i) {
        long combined = h1 + (long) i * h2; // double hashing: g_i = h1 + i*h2
        return (int) Long.remainderUnsigned(combined, bitCount);
    }

    /** FNV-1a over UTF-8 bytes with a seed, followed by a murmur-style finalizer for avalanche. */
    private static long hash(String key, long seed) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        long h = seed;
        for (byte b : key.getBytes(StandardCharsets.UTF_8)) {
            h ^= (b & 0xffL);
            h *= 0x100000001b3L;
        }
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
