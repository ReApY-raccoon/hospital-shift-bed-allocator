package com.hospital.allocator.util;

import com.hospital.allocator.model.Models.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Seeded synthetic data (nurses, shifts, patients) so every run and benchmark is reproducible. */
public final class DataGenerator {

    private static final String[] SKILLS = {"GENERAL", "ICU", "ER"};

    private DataGenerator() {
    }

    public static RosterRequest roster(int nurseCount, long seed) {
        if (nurseCount < 1) {
            throw new IllegalArgumentException("nurseCount must be >= 1");
        }
        Random rnd = new Random(seed);
        int shiftCount = Math.max(2, nurseCount / 4);
        List<Shift> shifts = new ArrayList<>();
        for (int j = 0; j < shiftCount; j++) {
            double r = rnd.nextDouble();
            String skill = r < 0.8 ? "ANY" : (r < 0.9 ? "ICU" : "ER");
            shifts.add(new Shift("S" + (j + 1), skill, 2 + rnd.nextInt(3)));
        }
        List<Integer> order = new ArrayList<>();
        for (int j = 0; j < shiftCount; j++) {
            order.add(j);
        }
        List<Nurse> nurses = new ArrayList<>();
        for (int i = 0; i < nurseCount; i++) {
            Collections.shuffle(order, rnd);
            int take = Math.min(shiftCount, 6 + rnd.nextInt(7));
            List<String> avail = new ArrayList<>();
            for (int t = 0; t < take; t++) {
                avail.add(shifts.get(order.get(t)).id());
            }
            nurses.add(new Nurse("N" + (i + 1), SKILLS[rnd.nextInt(SKILLS.length)], 2 + rnd.nextInt(4), avail));
        }
        return new RosterRequest(nurses, shifts);
    }

    /**
     * Supply roughly equals demand with sparse availability, so a greedy pass strands slots
     * that max-flow can still fill. Used for the roster benchmark.
     */
    public static RosterRequest tightRoster(int nurseCount, long seed) {
        if (nurseCount < 1) {
            throw new IllegalArgumentException("nurseCount must be >= 1");
        }
        Random rnd = new Random(seed);
        int shiftCount = Math.max(2, nurseCount / 2);
        List<Shift> shifts = new ArrayList<>();
        for (int j = 0; j < shiftCount; j++) {
            shifts.add(new Shift("S" + (j + 1), "ANY", 2 + rnd.nextInt(3)));
        }
        List<Nurse> nurses = new ArrayList<>();
        for (int i = 0; i < nurseCount; i++) {
            int take = Math.min(shiftCount, 2 + rnd.nextInt(3));
            List<String> avail = new ArrayList<>();
            while (avail.size() < take) {
                String id = shifts.get(rnd.nextInt(shiftCount)).id();
                if (!avail.contains(id)) {
                    avail.add(id);
                }
            }
            nurses.add(new Nurse("N" + (i + 1), "GENERAL", 1 + rnd.nextInt(2), avail));
        }
        return new RosterRequest(nurses, shifts);
    }

    public static List<Patient> patients(int count, long seed) {
        Random rnd = new Random(seed);
        List<Patient> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double wait = Math.round(-Math.log(1 - rnd.nextDouble()) * 45 * 10) / 10.0;
            double risk = Math.round(rnd.nextDouble() * 1000) / 10.0;
            out.add(new Patient(String.format("P%05d", i + 1), wait, risk));
        }
        return out;
    }

    public static SampleData sample(int nurseCount, long seed) {
        List<String> existing = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            existing.add(String.format("P%05d", i));
        }
        List<String> incoming = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            incoming.add(String.format("P%05d", i * 7)); // 20 known duplicates
        }
        for (int i = 1; i <= 30; i++) {
            incoming.add(String.format("P%05d", 5000 + i)); // 30 new admissions
        }
        return new SampleData(roster(nurseCount, seed), patients(200, seed), existing, incoming);
    }
}
