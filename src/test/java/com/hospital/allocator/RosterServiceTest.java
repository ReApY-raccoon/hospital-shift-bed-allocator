package com.hospital.allocator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hospital.allocator.model.Models.*;
import com.hospital.allocator.service.RosterService;
import com.hospital.allocator.util.DataGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RosterServiceTest {

    private final RosterService service = new RosterService();

    private static Nurse nurse(String id, String skill, int max, String... shifts) {
        return new Nurse(id, skill, max, List.of(shifts));
    }

    private static Shift shift(String id, String skill, int head) {
        return new Shift(id, skill, head);
    }

    @Test
    void feasibleRosterFillsEverySlot() {
        RosterRequest req = new RosterRequest(
                List.of(nurse("A", "GENERAL", 2, "S1", "S2"), nurse("B", "GENERAL", 2, "S1", "S2")),
                List.of(shift("S1", "ANY", 1), shift("S2", "ANY", 1)));
        RosterResult r = service.solveMaxFlow(req);
        assertTrue(r.feasible());
        assertEquals(2, r.totalAssigned());
        assertTrue(r.bottleneckShifts().isEmpty());
    }

    @Test
    void infeasibleRosterReportsShortfallAndBottleneck() {
        RosterRequest req = new RosterRequest(
                List.of(nurse("A", "GENERAL", 1, "S1")),
                List.of(shift("S1", "ANY", 3)));
        RosterResult r = service.solveMaxFlow(req);
        assertFalse(r.feasible());
        assertEquals(1, r.totalAssigned());
        assertEquals(List.of("S1"), r.bottleneckShifts());
        assertEquals(2, r.shifts().get(0).shortfall());
        assertFalse(r.minCutEdges().isEmpty());
    }

    @Test
    void maxFlowBeatsGreedyOnAdversarialCase() {
        // Greedy gives S1 to A (first available) and strands S2; max-flow gives S1->B, S2->A.
        RosterRequest req = new RosterRequest(
                List.of(nurse("A", "GENERAL", 1, "S1", "S2"), nurse("B", "GENERAL", 1, "S1")),
                List.of(shift("S1", "ANY", 1), shift("S2", "ANY", 1)));
        assertEquals(2, service.solveMaxFlow(req).totalAssigned());
        assertEquals(1, service.solveGreedy(req).totalAssigned());
    }

    @Test
    void skillRequirementIsRespected() {
        RosterRequest req = new RosterRequest(
                List.of(nurse("A", "GENERAL", 1, "S1"), nurse("B", "ICU", 1, "S1")),
                List.of(shift("S1", "ICU", 1)));
        RosterResult r = service.solveMaxFlow(req);
        assertEquals(1, r.assignments().size());
        assertEquals("B", r.assignments().get(0).nurseId());
    }

    @Test
    void neverExceedsNurseCapacityOrShiftHeadcount() {
        RosterRequest req = DataGenerator.roster(120, 5);
        RosterResult r = service.solveMaxFlow(req);
        Map<String, Integer> perNurse = new HashMap<>();
        Map<String, Integer> perShift = new HashMap<>();
        for (Assignment a : r.assignments()) {
            perNurse.merge(a.nurseId(), 1, Integer::sum);
            perShift.merge(a.shiftId(), 1, Integer::sum);
        }
        for (Nurse n : req.nurses()) {
            assertTrue(perNurse.getOrDefault(n.id(), 0) <= n.maxShifts());
        }
        for (Shift s : req.shifts()) {
            assertTrue(perShift.getOrDefault(s.id(), 0) <= s.headcount());
        }
    }

    @Test
    void minCutCapacityEqualsAssignedSlots() {
        RosterResult r = service.solveMaxFlow(DataGenerator.roster(60, 9));
        assertEquals(r.totalAssigned(), r.assignments().size());
        assertTrue(r.totalAssigned() <= r.totalRequired());
    }

    @Test
    void matchesBruteForceOptimumOnSmallRandomInstances() {
        Random rnd = new Random(2024);
        for (int trial = 0; trial < 200; trial++) {
            int nn = 1 + rnd.nextInt(4);
            int ss = 1 + rnd.nextInt(3);
            List<Shift> shifts = new ArrayList<>();
            for (int j = 0; j < ss; j++) {
                shifts.add(shift("S" + j, "ANY", 1 + rnd.nextInt(2)));
            }
            List<Nurse> nurses = new ArrayList<>();
            for (int i = 0; i < nn; i++) {
                List<String> av = new ArrayList<>();
                for (int j = 0; j < ss; j++) {
                    if (rnd.nextBoolean()) {
                        av.add("S" + j);
                    }
                }
                nurses.add(new Nurse("N" + i, "GENERAL", rnd.nextInt(3), av));
            }
            RosterRequest req = new RosterRequest(nurses, shifts);
            assertEquals(bruteForce(req), service.solveMaxFlow(req).totalAssigned(), "trial " + trial);
        }
    }

    @Test
    void emptyDatasetsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.solveMaxFlow(new RosterRequest(List.of(), List.of(shift("S1", "ANY", 1)))));
        assertThrows(IllegalArgumentException.class,
                () -> service.solveMaxFlow(new RosterRequest(List.of(nurse("A", "G", 1)), List.of())));
    }

    @Test
    void duplicateIdsAndBadValuesAreRejected() {
        List<Shift> s = List.of(shift("S1", "ANY", 1));
        assertThrows(IllegalArgumentException.class, () -> service.solveMaxFlow(new RosterRequest(
                List.of(nurse("A", "G", 1, "S1"), nurse("A", "G", 1, "S1")), s)));
        assertThrows(IllegalArgumentException.class, () -> service.solveMaxFlow(new RosterRequest(
                List.of(nurse("A", "G", -1, "S1")), s)));
        assertThrows(IllegalArgumentException.class, () -> service.solveMaxFlow(new RosterRequest(
                List.of(nurse("A", "G", 1, "NOPE")), s)));
        assertThrows(IllegalArgumentException.class, () -> service.solveMaxFlow(new RosterRequest(
                List.of(nurse("A", "G", 1, "S1")), List.of(shift("S1", "ANY", 0)))));
    }

    @Test
    void duplicateAvailabilityEntriesDoNotDoubleBook() {
        RosterRequest req = new RosterRequest(
                List.of(nurse("A", "GENERAL", 5, "S1", "S1", "S1")),
                List.of(shift("S1", "ANY", 3)));
        assertEquals(1, service.solveMaxFlow(req).totalAssigned());
    }

    /** Tries every subset of nurse-shift pairs and keeps the largest one that respects all capacities. */
    private static int bruteForce(RosterRequest req) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < req.nurses().size(); i++) {
            for (String sid : req.nurses().get(i).availableShifts()) {
                for (int j = 0; j < req.shifts().size(); j++) {
                    if (req.shifts().get(j).id().equals(sid)) {
                        pairs.add(new int[] {i, j});
                    }
                }
            }
        }
        int best = 0;
        for (int mask = 0; mask < (1 << pairs.size()); mask++) {
            int[] nurseLoad = new int[req.nurses().size()];
            int[] shiftLoad = new int[req.shifts().size()];
            boolean ok = true;
            int count = 0;
            for (int p = 0; p < pairs.size() && ok; p++) {
                if ((mask & (1 << p)) != 0) {
                    int[] ij = pairs.get(p);
                    nurseLoad[ij[0]]++;
                    shiftLoad[ij[1]]++;
                    count++;
                    ok = nurseLoad[ij[0]] <= req.nurses().get(ij[0]).maxShifts()
                            && shiftLoad[ij[1]] <= req.shifts().get(ij[1]).headcount();
                }
            }
            if (ok) {
                best = Math.max(best, count);
            }
        }
        return best;
    }
}
