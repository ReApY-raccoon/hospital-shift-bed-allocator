package com.hospital.allocator.service;

import com.hospital.allocator.algo.BloomFilter;
import com.hospital.allocator.algo.RandomizedQuickselect;
import com.hospital.allocator.model.Models.*;
import com.hospital.allocator.util.DataGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Reproducible (fixed-seed) benchmarks of each core algorithm against a baseline at 3 growing sizes. */
@Service
public class BenchmarkService {

    private final RosterService rosterService;

    public BenchmarkService(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    public List<BenchRow> runAll() {
        return runAll(new int[] {10_000, 100_000, 1_000_000}, new int[] {10_000, 100_000, 500_000},
                new int[] {100, 400, 1600});
    }

    public List<BenchRow> runAll(int[] selectSizes, int[] bloomSizes, int[] rosterSizes) {
        List<BenchRow> rows = new ArrayList<>();
        for (int n : selectSizes) {
            rows.add(quickselect(n));
        }
        for (int n : bloomSizes) {
            rows.add(bloom(n));
        }
        for (int n : rosterSizes) {
            rows.add(roster(n));
        }
        return rows;
    }

    BenchRow quickselect(int n) {
        Random gen = new Random(7);
        double[] data = new double[n];
        for (int i = 0; i < n; i++) {
            data[i] = gen.nextDouble() * 1000;
        }
        RandomizedQuickselect.selectCopy(data, n / 2, new Random(1)); // warm-up
        long t0 = System.nanoTime();
        double viaSelect = RandomizedQuickselect.selectCopy(data, (n - 1) / 2, new Random(1));
        double selectMs = (System.nanoTime() - t0) / 1e6;
        t0 = System.nanoTime();
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        double viaSort = sorted[(n - 1) / 2];
        double sortMs = (System.nanoTime() - t0) / 1e6;
        return new BenchRow("Median: Quickselect vs full sort", n, selectMs, sortMs, "Arrays.sort",
                viaSelect == viaSort, "expected O(n) vs O(n log n); identical median");
    }

    BenchRow bloom(int n) {
        List<String> members = new ArrayList<>(n);
        List<String> strangers = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            members.add("P" + i);
            strangers.add("X" + i);
        }
        long t0 = System.nanoTime();
        BloomFilter bf = new BloomFilter(n, 0.01);
        for (String m : members) {
            bf.add(m);
        }
        boolean noFalseNegatives = true;
        for (String m : members) {
            noFalseNegatives &= bf.mightContain(m);
        }
        int falsePositives = 0;
        for (String x : strangers) {
            if (bf.mightContain(x)) {
                falsePositives++;
            }
        }
        double bloomMs = (System.nanoTime() - t0) / 1e6;

        t0 = System.nanoTime();
        Set<String> set = new HashSet<>(members);
        boolean all = true;
        for (String m : members) {
            all &= set.contains(m);
        }
        int hashHits = 0;
        for (String x : strangers) {
            if (set.contains(x)) {
                hashHits++;
            }
        }
        double setMs = (System.nanoTime() - t0) / 1e6;
        String note = String.format("measured FPR %.4f (target 0.01); filter %d KB (%.1f bits/item), "
                        + "no false negatives; wins on memory, not raw speed",
                (double) falsePositives / n, bf.bitCount() / 8 / 1024, (double) bf.bitCount() / n);
        return new BenchRow("Membership: Bloom filter vs HashSet", n, bloomMs, setMs, "HashSet",
                noFalseNegatives && all && hashHits == 0, note);
    }

    BenchRow roster(int nurses) {
        RosterRequest req = DataGenerator.tightRoster(nurses, 42);
        rosterService.solveMaxFlow(DataGenerator.tightRoster(20, 1)); // warm-up
        RosterResult flow = rosterService.solveMaxFlow(req);
        RosterResult greedy = rosterService.solveGreedy(req);
        String note = String.format("filled %d (max-flow) vs %d (greedy) of %d required slots",
                flow.totalAssigned(), greedy.totalAssigned(), flow.totalRequired());
        return new BenchRow("Roster: Edmonds-Karp vs greedy", nurses, flow.timeMs(), greedy.timeMs(),
                "Greedy", flow.totalAssigned() >= greedy.totalAssigned(), note);
    }
}
