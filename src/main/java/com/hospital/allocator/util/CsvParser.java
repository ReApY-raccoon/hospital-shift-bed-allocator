package com.hospital.allocator.util;

import com.hospital.allocator.model.Models.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal CSV readers (no quoted fields).
 * nurses.csv: id,skill,maxShifts,availableShifts   (availableShifts separated by ';')
 * shifts.csv: id,requiredSkill,headcount
 */
public final class CsvParser {

    private CsvParser() {
    }

    public static List<Nurse> parseNurses(String csv) {
        List<String[]> rows = rows(csv, "nurses.csv", "id,skill,maxshifts,availableshifts");
        List<Nurse> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] c = rows.get(i);
            int line = i + 2;
            if (c.length < 4) {
                throw new IllegalArgumentException("nurses.csv line " + line + ": expected 4 columns");
            }
            List<String> avail = new ArrayList<>();
            for (String s : c[3].split(";")) {
                if (!s.isBlank()) {
                    avail.add(s.trim());
                }
            }
            out.add(new Nurse(c[0], c[1], parseInt(c[2], "nurses.csv", line, "maxShifts"), avail));
        }
        return out;
    }

    public static List<Shift> parseShifts(String csv) {
        List<String[]> rows = rows(csv, "shifts.csv", "id,requiredskill,headcount");
        List<Shift> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            String[] c = rows.get(i);
            int line = i + 2;
            if (c.length < 3) {
                throw new IllegalArgumentException("shifts.csv line " + line + ": expected 3 columns");
            }
            out.add(new Shift(c[0], c[1], parseInt(c[2], "shifts.csv", line, "headcount")));
        }
        return out;
    }

    private static List<String[]> rows(String csv, String name, String expectedHeader) {
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException(name + " is empty");
        }
        String text = csv.startsWith("﻿") ? csv.substring(1) : csv;
        String[] lines = text.split("\\r?\\n");
        String header = lines[0].replace(" ", "").toLowerCase();
        if (!header.equals(expectedHeader)) {
            throw new IllegalArgumentException(name + ": expected header '" + expectedHeader + "'");
        }
        List<String[]> out = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] cells = lines[i].split(",", -1);
            out.add(Arrays.stream(cells).map(String::trim).toArray(String[]::new));
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException(name + " has a header but no data rows");
        }
        return out;
    }

    private static int parseInt(String s, String file, int line, String field) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(file + " line " + line + ": " + field + " is not an integer: '" + s + "'");
        }
    }
}
