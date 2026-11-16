package com.hospital.allocator.service;

import com.hospital.allocator.algo.EdmondsKarp;
import com.hospital.allocator.model.Models.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Builds the roster as a capacitated flow network and solves it with Edmonds-Karp.
 *
 * Network: SOURCE -> nurse (cap = maxShifts) -> shift (cap 1, only if the nurse is available and
 * the skill matches) -> SINK (cap = headcount). Max flow = most nurse-shift slots that can be
 * filled; every flow unit on a nurse->shift edge is one assignment. The min cut explains any
 * shortfall.
 */
@Service
public class RosterService {

    public RosterResult solveMaxFlow(RosterRequest req) {
        validate(req);
        long start = System.nanoTime();
        List<Nurse> nurses = req.nurses();
        List<Shift> shifts = req.shifts();
        int n = nurses.size();
        int s = shifts.size();
        int source = 0;
        int sink = n + s + 1;
        EdmondsKarp net = new EdmondsKarp(n + s + 2);
        Map<String, Integer> shiftIdx = indexShifts(shifts);

        for (int i = 0; i < n; i++) {
            net.addEdge(source, 1 + i, nurses.get(i).maxShifts());
        }
        for (int j = 0; j < s; j++) {
            net.addEdge(1 + n + j, sink, shifts.get(j).headcount());
        }
        List<EdmondsKarp.Edge> pairEdges = new ArrayList<>();
        List<int[]> pairIdx = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Nurse nurse = nurses.get(i);
            for (String shiftId : new LinkedHashSet<>(nurse.availableShifts())) {
                int j = shiftIdx.get(shiftId);
                if (skillMatches(nurse, shifts.get(j))) {
                    pairEdges.add(net.addEdge(1 + i, 1 + n + j, 1));
                    pairIdx.add(new int[] {i, j});
                }
            }
        }
        net.maxFlow(source, sink);

        List<Assignment> assignments = new ArrayList<>();
        for (int e = 0; e < pairEdges.size(); e++) {
            if (pairEdges.get(e).flow() == 1) {
                int[] ij = pairIdx.get(e);
                assignments.add(new Assignment(nurses.get(ij[0]).id(), shifts.get(ij[1]).id()));
            }
        }
        List<String> cut = new ArrayList<>();
        for (EdmondsKarp.Edge e : net.minCutEdges(source)) {
            cut.add(nodeName(e.from, n, s, nurses, shifts) + " -> " + nodeName(e.to, n, s, nurses, shifts)
                    + " (capacity " + e.capacity + ")");
        }
        double ms = (System.nanoTime() - start) / 1e6;
        return buildResult("Edmonds-Karp max-flow", req, assignments, cut, net.augmentations(), ms);
    }

    /** Baseline: shift-by-shift greedy, first available nurse with capacity left. Not optimal. */
    public RosterResult solveGreedy(RosterRequest req) {
        validate(req);
        long start = System.nanoTime();
        int[] remaining = new int[req.nurses().size()];
        for (int i = 0; i < remaining.length; i++) {
            remaining[i] = req.nurses().get(i).maxShifts();
        }
        List<Set<String>> avail = new ArrayList<>();
        for (Nurse nurse : req.nurses()) {
            avail.add(new HashSet<>(nurse.availableShifts()));
        }
        List<Assignment> assignments = new ArrayList<>();
        for (Shift shift : req.shifts()) {
            int need = shift.headcount();
            for (int i = 0; i < remaining.length && need > 0; i++) {
                Nurse nurse = req.nurses().get(i);
                if (remaining[i] > 0 && avail.get(i).contains(shift.id()) && skillMatches(nurse, shift)) {
                    assignments.add(new Assignment(nurse.id(), shift.id()));
                    remaining[i]--;
                    need--;
                }
            }
        }
        double ms = (System.nanoTime() - start) / 1e6;
        return buildResult("Greedy baseline", req, assignments, List.of(), 0, ms);
    }

    // ---- helpers ----

    private RosterResult buildResult(String algo, RosterRequest req, List<Assignment> assignments,
            List<String> cut, int augmentations, double ms) {
        Map<String, Integer> perShift = new HashMap<>();
        Map<String, Integer> perNurse = new HashMap<>();
        for (Assignment a : assignments) {
            perShift.merge(a.shiftId(), 1, Integer::sum);
            perNurse.merge(a.nurseId(), 1, Integer::sum);
        }
        int required = 0;
        List<ShiftStatus> shiftStatuses = new ArrayList<>();
        List<String> bottleneck = new ArrayList<>();
        for (Shift sh : req.shifts()) {
            int got = perShift.getOrDefault(sh.id(), 0);
            int shortfall = sh.headcount() - got;
            required += sh.headcount();
            shiftStatuses.add(new ShiftStatus(sh.id(), sh.headcount(), got, shortfall));
            if (shortfall > 0) {
                bottleneck.add(sh.id());
            }
        }
        List<NurseLoad> loads = new ArrayList<>();
        for (Nurse nu : req.nurses()) {
            int got = perNurse.getOrDefault(nu.id(), 0);
            double util = nu.maxShifts() == 0 ? 0 : Math.round(1000.0 * got / nu.maxShifts()) / 1000.0;
            loads.add(new NurseLoad(nu.id(), got, nu.maxShifts(), util));
        }
        return new RosterResult(algo, assignments.size() == required, required, assignments.size(),
                assignments, shiftStatuses, loads, bottleneck, cut, augmentations, ms);
    }

    private void validate(RosterRequest req) {
        if (req == null || req.nurses() == null || req.nurses().isEmpty()) {
            throw new IllegalArgumentException("Nurse list is empty");
        }
        if (req.shifts() == null || req.shifts().isEmpty()) {
            throw new IllegalArgumentException("Shift list is empty");
        }
        Set<String> nurseIds = new HashSet<>();
        for (Nurse n : req.nurses()) {
            if (n == null || n.id() == null || n.id().isBlank()) {
                throw new IllegalArgumentException("Nurse with missing id");
            }
            if (!nurseIds.add(n.id())) {
                throw new IllegalArgumentException("Duplicate nurse id: " + n.id());
            }
            if (n.maxShifts() < 0) {
                throw new IllegalArgumentException("Negative maxShifts for nurse " + n.id());
            }
            if (n.availableShifts() == null) {
                throw new IllegalArgumentException("Missing availability for nurse " + n.id());
            }
        }
        Set<String> shiftIds = new HashSet<>();
        for (Shift sh : req.shifts()) {
            if (sh == null || sh.id() == null || sh.id().isBlank()) {
                throw new IllegalArgumentException("Shift with missing id");
            }
            if (!shiftIds.add(sh.id())) {
                throw new IllegalArgumentException("Duplicate shift id: " + sh.id());
            }
            if (sh.headcount() <= 0) {
                throw new IllegalArgumentException("Headcount must be positive for shift " + sh.id());
            }
        }
        for (Nurse n : req.nurses()) {
            for (String sid : n.availableShifts()) {
                if (!shiftIds.contains(sid)) {
                    throw new IllegalArgumentException("Nurse " + n.id() + " references unknown shift " + sid);
                }
            }
        }
    }

    private static Map<String, Integer> indexShifts(List<Shift> shifts) {
        Map<String, Integer> idx = new HashMap<>();
        for (int j = 0; j < shifts.size(); j++) {
            idx.put(shifts.get(j).id(), j);
        }
        return idx;
    }

    static boolean skillMatches(Nurse nurse, Shift shift) {
        String req = shift.requiredSkill();
        if (req == null || req.isBlank() || req.equalsIgnoreCase("ANY")) {
            return true;
        }
        return req.equalsIgnoreCase(nurse.skill());
    }

    private static String nodeName(int node, int n, int s, List<Nurse> nurses, List<Shift> shifts) {
        if (node == 0) {
            return "SOURCE";
        }
        if (node == n + s + 1) {
            return "SINK";
        }
        if (node <= n) {
            return "Nurse " + nurses.get(node - 1).id();
        }
        return "Shift " + shifts.get(node - 1 - n).id();
    }
}
