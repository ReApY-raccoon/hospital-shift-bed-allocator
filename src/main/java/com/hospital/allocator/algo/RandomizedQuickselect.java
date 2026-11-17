package com.hospital.allocator.algo;

import java.util.Random;

/**
 * Randomized Quickselect with 3-way partitioning (handles many duplicates).
 * Self-implemented: no Arrays.sort / Collections.sort is used.
 *
 * Expected time O(n), worst case O(n^2) (probability vanishes quickly because the
 * pivot is chosen uniformly at random). Space O(1) extra (in place).
 */
public final class RandomizedQuickselect {

    private RandomizedQuickselect() {
    }

    /**
     * Returns the k-th smallest element (0-based) of a. The array is PARTIALLY REORDERED in place;
     * use {@link #selectCopy} to keep the input intact.
     */
    public static double select(double[] a, int k, Random rnd) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("Cannot select from an empty array");
        }
        if (k < 0 || k >= a.length) {
            throw new IllegalArgumentException("k out of range: " + k + " for n=" + a.length);
        }
        for (double v : a) {
            if (Double.isNaN(v)) {
                throw new IllegalArgumentException("NaN is not allowed");
            }
        }
        int lo = 0;
        int hi = a.length - 1;
        while (true) {
            if (lo == hi) {
                return a[lo];
            }
            double pivot = a[lo + rnd.nextInt(hi - lo + 1)];
            int lt = lo;
            int gt = hi;
            int i = lo;
            while (i <= gt) {
                if (a[i] < pivot) {
                    swap(a, lt++, i++);
                } else if (a[i] > pivot) {
                    swap(a, i, gt--);
                } else {
                    i++;
                }
            }
            // a[lo..lt-1] < pivot, a[lt..gt] == pivot, a[gt+1..hi] > pivot
            if (k < lt) {
                hi = lt - 1;
            } else if (k > gt) {
                lo = gt + 1;
            } else {
                return pivot;
            }
        }
    }

    public static double selectCopy(double[] a, int k, Random rnd) {
        if (a == null) {
            throw new IllegalArgumentException("Array is null");
        }
        return select(a.clone(), k, rnd);
    }

    /** Nearest-rank percentile (p in (0,100]) of the values, input not modified. */
    public static double percentile(double[] a, double p, Random rnd) {
        if (a == null || a.length == 0) {
            throw new IllegalArgumentException("Cannot compute a percentile of an empty array");
        }
        if (!(p > 0 && p <= 100)) {
            throw new IllegalArgumentException("Percentile must be in (0, 100], got " + p);
        }
        int rank = (int) Math.ceil(p / 100.0 * a.length);
        return selectCopy(a, Math.max(rank, 1) - 1, rnd);
    }

    private static void swap(double[] a, int i, int j) {
        double t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
