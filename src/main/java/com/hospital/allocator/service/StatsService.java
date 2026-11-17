package com.hospital.allocator.service;

import com.hospital.allocator.algo.RandomizedQuickselect;
import com.hospital.allocator.model.Models.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/** Median / percentile wait times and top-k risk patients via randomized Quickselect (no full sort). */
@Service
public class StatsService {

    public StatsResult compute(StatsRequest req) {
        if (req == null || req.patients() == null || req.patients().isEmpty()) {
            throw new IllegalArgumentException("Patient list is empty");
        }
        List<Patient> ps = req.patients();
        int n = ps.size();
        double[] waits = new double[n];
        double[] risks = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            Patient p = ps.get(i);
            if (p == null || p.id() == null || p.id().isBlank()) {
                throw new IllegalArgumentException("Patient with missing id at index " + i);
            }
            if (!Double.isFinite(p.waitMinutes()) || p.waitMinutes() < 0) {
                throw new IllegalArgumentException("Invalid wait time for patient " + p.id());
            }
            if (!Double.isFinite(p.riskScore()) || p.riskScore() < 0) {
                throw new IllegalArgumentException("Invalid risk score for patient " + p.id());
            }
            waits[i] = p.waitMinutes();
            risks[i] = p.riskScore();
            sum += waits[i];
        }
        double pct = req.percentile() == null ? 90.0 : req.percentile();
        if (!(pct > 0 && pct <= 100)) {
            throw new IllegalArgumentException("Percentile must be in (0, 100]");
        }
        int k = req.topK() == null ? 5 : req.topK();
        if (k < 1) {
            throw new IllegalArgumentException("topK must be at least 1");
        }
        k = Math.min(k, n);
        Random rnd = req.seed() == null ? new Random() : new Random(req.seed());

        double median = RandomizedQuickselect.percentile(waits, 50, rnd);
        double pctValue = RandomizedQuickselect.percentile(waits, pct, rnd);

        // k-th largest risk = (n-k)-th smallest; everything above it plus enough ties form the top-k.
        double threshold = RandomizedQuickselect.selectCopy(risks, n - k, rnd);
        List<Patient> top = new ArrayList<>();
        for (Patient p : ps) {
            if (p.riskScore() > threshold) {
                top.add(p);
            }
        }
        for (Patient p : ps) {
            if (top.size() >= k) {
                break;
            }
            if (p.riskScore() == threshold) {
                top.add(p);
            }
        }
        insertionSortByRiskDesc(top); // only k elements
        return new StatsResult(n, sum / n, median, pct, pctValue, k, top,
                "Randomized Quickselect, expected O(n); nearest-rank percentile");
    }

    private static void insertionSortByRiskDesc(List<Patient> list) {
        for (int i = 1; i < list.size(); i++) {
            Patient key = list.get(i);
            int j = i - 1;
            while (j >= 0 && list.get(j).riskScore() < key.riskScore()) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, key);
        }
    }
}
