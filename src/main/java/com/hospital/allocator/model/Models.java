package com.hospital.allocator.model;

import java.util.List;

/** Request / response records shared by the services and the REST layer. */
public final class Models {

    private Models() {
    }

    // ---- Roster ----
    public record Nurse(String id, String skill, int maxShifts, List<String> availableShifts) {
    }

    public record Shift(String id, String requiredSkill, int headcount) {
    }

    public record RosterRequest(List<Nurse> nurses, List<Shift> shifts) {
    }

    public record Assignment(String nurseId, String shiftId) {
    }

    public record ShiftStatus(String shiftId, int required, int assigned, int shortfall) {
    }

    public record NurseLoad(String nurseId, int assigned, int maxShifts, double utilization) {
    }

    public record RosterResult(
            String algorithm,
            boolean feasible,
            int totalRequired,
            int totalAssigned,
            List<Assignment> assignments,
            List<ShiftStatus> shifts,
            List<NurseLoad> nurses,
            List<String> bottleneckShifts,
            List<String> minCutEdges,
            int augmentations,
            double timeMs) {
    }

    // ---- Patient statistics ----
    public record Patient(String id, double waitMinutes, double riskScore) {
    }

    public record StatsRequest(List<Patient> patients, Double percentile, Integer topK, Long seed) {
    }

    public record StatsResult(
            int patientCount,
            double meanWaitMinutes,
            double medianWaitMinutes,
            double percentile,
            double percentileWaitMinutes,
            int topK,
            List<Patient> topRisk,
            String method) {
    }

    // ---- Admissions (duplicate screening) ----
    public record AdmissionRequest(List<String> existingIds, List<String> incomingIds, Double falsePositiveRate) {
    }

    public record AdmissionRow(String id, boolean bloomPossiblyPresent, boolean confirmedDuplicate) {
    }

    public record AdmissionResult(
            int existingCount,
            int incomingCount,
            int bloomHits,
            int confirmedDuplicates,
            int bloomFalsePositives,
            int filterBits,
            int hashFunctions,
            double configuredFalsePositiveRate,
            List<AdmissionRow> rows) {
    }

    // ---- Sample data & benchmarks ----
    public record SampleData(RosterRequest roster, List<Patient> patients, List<String> existingIds, List<String> incomingIds) {
    }

    public record BenchRow(
            String experiment,
            int n,
            double algorithmMs,
            double baselineMs,
            String baselineName,
            boolean outputsMatch,
            String note) {
    }
}
