package com.hospital.allocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hospital.allocator.model.Models.*;
import com.hospital.allocator.service.AdmissionService;
import com.hospital.allocator.service.StatsService;
import com.hospital.allocator.util.DataGenerator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StatsAndAdmissionServiceTest {

    private final StatsService stats = new StatsService();
    private final AdmissionService admissions = new AdmissionService();

    private static List<Patient> waits(double... w) {
        List<Patient> ps = new ArrayList<>();
        for (int i = 0; i < w.length; i++) {
            ps.add(new Patient("P" + i, w[i], i));
        }
        return ps;
    }

    @Test
    void medianAndPercentileUseNearestRank() {
        StatsResult r = stats.compute(new StatsRequest(waits(15, 20, 35, 40, 50), 90.0, 2, 1L));
        assertEquals(35.0, r.medianWaitMinutes(), 0.0);
        assertEquals(50.0, r.percentileWaitMinutes(), 0.0);
        assertEquals(32.0, r.meanWaitMinutes(), 1e-9);
    }

    @Test
    void topRiskReturnsHighestRiskPatientsInOrder() {
        List<Patient> ps = new ArrayList<>();
        double[] risk = {10, 90, 50, 70, 30};
        for (int i = 0; i < risk.length; i++) {
            ps.add(new Patient("P" + i, 5, risk[i]));
        }
        StatsResult r = stats.compute(new StatsRequest(ps, 50.0, 3, 1L));
        assertEquals(List.of("P1", "P3", "P2"), r.topRisk().stream().map(Patient::id).toList());
    }

    @Test
    void topRiskWithTiesStillReturnsExactlyK() {
        List<Patient> ps = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ps.add(new Patient("P" + i, 1, 50)); // every risk score tied
        }
        assertEquals(4, stats.compute(new StatsRequest(ps, 50.0, 4, 1L)).topRisk().size());
    }

    @Test
    void topKLargerThanNIsClamped() {
        assertEquals(3, stats.compute(new StatsRequest(waits(1, 2, 3), 50.0, 99, 1L)).topRisk().size());
    }

    @Test
    void singlePatientBoundary() {
        StatsResult r = stats.compute(new StatsRequest(waits(12), 90.0, 1, 1L));
        assertEquals(12.0, r.medianWaitMinutes(), 0.0);
    }

    @Test
    void invalidStatsInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> stats.compute(new StatsRequest(List.of(), 50.0, 1, 1L)));
        assertThrows(IllegalArgumentException.class, () -> stats.compute(new StatsRequest(waits(1, 2), 150.0, 1, 1L)));
        assertThrows(IllegalArgumentException.class, () -> stats.compute(new StatsRequest(waits(1, -2), 50.0, 1, 1L)));
        assertThrows(IllegalArgumentException.class, () -> stats.compute(new StatsRequest(waits(1, 2), 50.0, 0, 1L)));
    }

    @Test
    void realDuplicatesAreAlwaysConfirmed() {
        SampleData d = DataGenerator.sample(10, 1);
        AdmissionResult r = admissions.check(new AdmissionRequest(d.existingIds(), d.incomingIds(), 0.01));
        assertEquals(20, r.confirmedDuplicates()); // 20 known duplicates, zero false negatives
        assertEquals(50, r.incomingCount());
    }

    @Test
    void bloomHitsThatAreNotRealAreNotReportedAsDuplicates() {
        // A deliberately weak filter produces false positives; none may be reported as confirmed duplicates.
        List<String> existing = new ArrayList<>();
        List<String> incoming = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            existing.add("E" + i);
            incoming.add("N" + i);
        }
        AdmissionResult r = admissions.check(new AdmissionRequest(existing, incoming, 0.5));
        assertEquals(0, r.confirmedDuplicates());
        assertTrue(r.bloomFalsePositives() > 0);
        assertEquals(r.bloomHits(), r.bloomFalsePositives());
    }

    @Test
    void emptyExistingRegistryMeansNoDuplicates() { // boundary
        AdmissionResult r = admissions.check(new AdmissionRequest(List.of(), List.of("A", "B"), 0.01));
        assertEquals(0, r.confirmedDuplicates());
        assertFalse(r.rows().get(0).bloomPossiblyPresent());
    }

    @Test
    void invalidAdmissionInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> admissions.check(new AdmissionRequest(List.of("A"), List.of(), 0.01)));
        assertThrows(IllegalArgumentException.class,
                () -> admissions.check(new AdmissionRequest(List.of("A"), List.of("B"), 0.0)));
        assertThrows(IllegalArgumentException.class,
                () -> admissions.check(new AdmissionRequest(List.of("A"), List.of(" "), 0.01)));
    }
}
