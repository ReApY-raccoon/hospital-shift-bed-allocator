package com.hospital.allocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hospital.allocator.algo.BloomFilter;
import org.junit.jupiter.api.Test;

class BloomFilterTest {

    @Test
    void neverProducesFalseNegatives() {
        BloomFilter bf = new BloomFilter(10_000, 0.01);
        for (int i = 0; i < 10_000; i++) {
            bf.add("P" + i);
        }
        for (int i = 0; i < 10_000; i++) {
            assertTrue(bf.mightContain("P" + i), "false negative for P" + i);
        }
    }

    @Test
    void falsePositiveRateStaysNearTarget() {
        BloomFilter bf = new BloomFilter(10_000, 0.01);
        for (int i = 0; i < 10_000; i++) {
            bf.add("P" + i);
        }
        int fp = 0;
        int trials = 100_000;
        for (int i = 0; i < trials; i++) {
            if (bf.mightContain("X" + i)) {
                fp++;
            }
        }
        double rate = (double) fp / trials;
        assertTrue(rate < 0.02, "measured FPR too high: " + rate);
    }

    @Test
    void emptyFilterContainsNothing() { // boundary
        BloomFilter bf = new BloomFilter(100, 0.01);
        assertFalse(bf.mightContain("anything"));
        assertEquals(0, bf.insertedCount());
    }

    @Test
    void sizingFollowsTheFormula() {
        BloomFilter bf = new BloomFilter(1000, 0.01);
        // m = -n ln p / (ln2)^2 ~ 9586 bits, k ~ 7
        assertTrue(bf.bitCount() >= 9500 && bf.bitCount() <= 9700, "bits=" + bf.bitCount());
        assertEquals(7, bf.hashCount());
    }

    @Test
    void addingTheSameKeyTwiceStillFindsIt() {
        BloomFilter bf = new BloomFilter(10, 0.05);
        bf.add("P1");
        bf.add("P1");
        assertTrue(bf.mightContain("P1"));
    }

    @Test
    void estimatedRateGrowsWithFill() {
        BloomFilter bf = new BloomFilter(1000, 0.01);
        double empty = bf.estimatedFalsePositiveRate();
        for (int i = 0; i < 1000; i++) {
            bf.add("K" + i);
        }
        assertTrue(bf.estimatedFalsePositiveRate() > empty);
        assertTrue(bf.estimatedFalsePositiveRate() < 0.02);
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(0, 0.01));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(10, 1));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(10, 0.01).add(null));
    }
}
