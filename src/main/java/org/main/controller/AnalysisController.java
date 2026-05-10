package org.main.controller;

import java.util.Map;
import org.main.service.analysis.MatchAnalysisService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisController {

    private final MatchAnalysisService matchAnalysisService;

    public AnalysisController(MatchAnalysisService matchAnalysisService) {
        this.matchAnalysisService = matchAnalysisService;
    }

    @PostMapping("/api/analysis/process-new")
    public Map<String, Object> processNewMatches(@RequestParam(defaultValue = "50") int limit) {
        int processed = matchAnalysisService.processNewMatches(limit);

        return Map.of(
                "processed", processed,
                "limit", limit
        );
    }

    @PostMapping("/api/analysis/matches/{matchId}")
    public Map<String, Object> processMatch(@PathVariable String matchId) {
        matchAnalysisService.processMatch(matchId);

        return Map.of(
                "matchId", matchId,
                "status", "processed"
        );
    }
}