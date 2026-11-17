package com.hospital.allocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hospital.allocator.algo.RandomizedQuickselect;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RandomizedQuickselectTest {

    @Test
    void matchesSortedOrderOnRandomArrays() {
        Random gen = new Random(11);
        for (int trial = 0; trial < 300; trial++) {
            int n = 1 + gen.nextInt(60);
            double[] a = new double[n];
            for (int i = 0; i < n; i++) {
                a[i] = gen.nextInt(20); // many duplicates on purpose
            }
            double[] sorted = a.clone();
            Arrays.sort(sorted);
            int k = gen.nextInt(n);
            assertEquals(sorted[k], RandomizedQuickselect.selectCopy(a, k, new Random(trial)), 0.0);
        }
    }

    @Test
    void singleElement() { // boundary
        assertEquals(42.0, RandomizedQuickselect.selectCopy(new double[] {42}, 0, new Random(1)), 0.0);
    }

    @Test
    void allEqualElements() {
        double[] a = new double[1000];
        Arrays.fill(a, 5.5);
        assertEquals(5.5, RandomizedQuickselect.selectCopy(a, 500, new Random(2)), 0.0);
    }

    @Test
    void sortedAndReverseSortedInputs() {
        double[] up = new double[500];
        double[] down = new double[500];
        for (int i = 0; i < 500; i++) {
            up[i] = i;
            down[i] = 499 - i;
        }
        assertEquals(123.0, RandomizedQuickselect.selectCopy(up, 123, new Random(3)), 0.0);
        assertEquals(123.0, RandomizedQuickselect.selectCopy(down, 123, new Random(3)), 0.0);
    }

    @Test
    void minAndMaxAtBoundaries() {
        double[] a = {9, -3, 7, 100, 0.5};
        assertEquals(-3.0, RandomizedQuickselect.selectCopy(a, 0, new Random(4)), 0.0);
        assertEquals(100.0, RandomizedQuickselect.selectCopy(a, 4, new Random(4)), 0.0);
    }

    @Test
    void selectCopyDoesNotModifyInput() {
        double[] a = {5, 1, 4, 2, 3};
        double[] before = a.clone();
        RandomizedQuickselect.selectCopy(a, 2, new Random(5));
        assertEquals(Arrays.toString(before), Arrays.toString(a));
    }

    @Test
    void percentileUsesNearestRank() {
        double[] a = {15, 20, 35, 40, 50}; // classic nearest-rank example
        assertEquals(20.0, RandomizedQuickselect.percentile(a, 30, new Random(6)), 0.0);
        assertEquals(35.0, RandomizedQuickselect.percentile(a, 50, new Random(6)), 0.0);
        assertEquals(50.0, RandomizedQuickselect.percentile(a, 100, new Random(6)), 0.0);
    }

    @Test
    void sameSeedGivesSameResult() {
        double[] a = new double[200];
        Random gen = new Random(8);
        for (int i = 0; i < a.length; i++) {
            a[i] = gen.nextDouble();
        }
        assertEquals(RandomizedQuickselect.selectCopy(a, 77, new Random(99)),
                RandomizedQuickselect.selectCopy(a, 77, new Random(99)), 0.0);
    }

    @Test
    void invalidInputsAreRejected() {
        Random r = new Random(1);
        assertThrows(IllegalArgumentException.class, () -> RandomizedQuickselect.select(new double[0], 0, r));
        assertThrows(IllegalArgumentException.class, () -> RandomizedQuickselect.select(new double[] {1, 2}, 2, r));
        assertThrows(IllegalArgumentException.class, () -> RandomizedQuickselect.select(new double[] {1, 2}, -1, r));
        assertThrows(IllegalArgumentException.class,
                () -> RandomizedQuickselect.select(new double[] {1, Double.NaN}, 0, r));
        assertThrows(IllegalArgumentException.class,
                () -> RandomizedQuickselect.percentile(new double[] {1, 2}, 0, r));
    }
}
