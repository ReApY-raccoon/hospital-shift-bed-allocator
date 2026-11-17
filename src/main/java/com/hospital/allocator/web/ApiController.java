package com.hospital.allocator.web;

import com.hospital.allocator.model.Models.*;
import com.hospital.allocator.service.AdmissionService;
import com.hospital.allocator.service.BenchmarkService;
import com.hospital.allocator.service.RosterService;
import com.hospital.allocator.service.StatsService;
import com.hospital.allocator.util.CsvParser;
import com.hospital.allocator.util.DataGenerator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RosterService rosterService;
    private final StatsService statsService;
    private final AdmissionService admissionService;
    private final BenchmarkService benchmarkService;

    public ApiController(RosterService rosterService, StatsService statsService,
            AdmissionService admissionService, BenchmarkService benchmarkService) {
        this.rosterService = rosterService;
        this.statsService = statsService;
        this.admissionService = admissionService;
        this.benchmarkService = benchmarkService;
    }

    @PostMapping("/roster")
    public RosterResult roster(@RequestBody RosterRequest req,
            @RequestParam(defaultValue = "maxflow") String algorithm) {
        return run(req, algorithm);
    }

    @PostMapping("/roster/compare")
    public Map<String, RosterResult> compare(@RequestBody RosterRequest req) {
        Map<String, RosterResult> out = new LinkedHashMap<>();
        out.put("maxflow", rosterService.solveMaxFlow(req));
        out.put("greedy", rosterService.solveGreedy(req));
        return out;
    }

    @PostMapping("/roster/upload")
    public Map<String, RosterResult> upload(@RequestParam("nurses") MultipartFile nurses,
            @RequestParam("shifts") MultipartFile shifts) throws IOException {
        List<Nurse> n = CsvParser.parseNurses(new String(nurses.getBytes(), StandardCharsets.UTF_8));
        List<Shift> s = CsvParser.parseShifts(new String(shifts.getBytes(), StandardCharsets.UTF_8));
        return compare(new RosterRequest(n, s));
    }

    @PostMapping("/stats")
    public StatsResult stats(@RequestBody StatsRequest req) {
        return statsService.compute(req);
    }

    @PostMapping("/admissions/check")
    public AdmissionResult admissions(@RequestBody AdmissionRequest req) {
        return admissionService.check(req);
    }

    @GetMapping("/benchmark")
    public List<BenchRow> benchmark() {
        return benchmarkService.runAll();
    }

    @GetMapping("/sample")
    public SampleData sample(@RequestParam(defaultValue = "30") int nurses,
            @RequestParam(defaultValue = "42") long seed) {
        if (nurses < 1 || nurses > 5000) {
            throw new IllegalArgumentException("nurses must be between 1 and 5000");
        }
        return DataGenerator.sample(nurses, seed);
    }

    private RosterResult run(RosterRequest req, String algorithm) {
        return switch (algorithm.toLowerCase()) {
            case "maxflow" -> rosterService.solveMaxFlow(req);
            case "greedy" -> rosterService.solveGreedy(req);
            default -> throw new IllegalArgumentException("algorithm must be 'maxflow' or 'greedy'");
        };
    }
}
