package com.hospital.allocator.service;

import com.hospital.allocator.algo.BloomFilter;
import com.hospital.allocator.model.Models.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Duplicate-patient screening. The Bloom filter is only a fast pre-check: a hit means
 * "possibly present" and is confirmed against the authoritative exact set before it is
 * reported as a duplicate.
 */
@Service
public class AdmissionService {

    public AdmissionResult check(AdmissionRequest req) {
        if (req == null || req.incomingIds() == null || req.incomingIds().isEmpty()) {
            throw new IllegalArgumentException("Incoming admission list is empty");
        }
        List<String> existing = req.existingIds() == null ? List.of() : req.existingIds();
        double fpr = req.falsePositiveRate() == null ? 0.01 : req.falsePositiveRate();
        if (!(fpr > 0 && fpr <= 0.5)) {
            throw new IllegalArgumentException("falsePositiveRate must be in (0, 0.5]");
        }
        for (String id : existing) {
            requireId(id);
        }
        for (String id : req.incomingIds()) {
            requireId(id);
        }
        BloomFilter filter = new BloomFilter(Math.max(1, existing.size()), fpr);
        Set<String> exact = new HashSet<>();
        for (String id : existing) {
            filter.add(id);
            exact.add(id);
        }
        List<AdmissionRow> rows = new ArrayList<>();
        int hits = 0;
        int confirmed = 0;
        for (String id : req.incomingIds()) {
            boolean possible = filter.mightContain(id);
            boolean duplicate = possible && exact.contains(id); // authoritative confirmation
            if (possible) {
                hits++;
            }
            if (duplicate) {
                confirmed++;
            }
            rows.add(new AdmissionRow(id, possible, duplicate));
        }
        return new AdmissionResult(existing.size(), req.incomingIds().size(), hits, confirmed,
                hits - confirmed, filter.bitCount(), filter.hashCount(), fpr, rows);
    }

    private static void requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Blank patient id in request");
        }
    }
}
