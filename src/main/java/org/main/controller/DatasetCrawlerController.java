package org.main.controller;

import java.util.List;
import org.main.dto.BalancedDatasetResultDto;
import org.main.service.BalancedDatasetCrawlerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for automatic balanced dataset collection.
 */
@RestController
public class DatasetCrawlerController {

    private final BalancedDatasetCrawlerService balancedDatasetCrawlerService;

    public DatasetCrawlerController(BalancedDatasetCrawlerService balancedDatasetCrawlerService) {
        this.balancedDatasetCrawlerService = balancedDatasetCrawlerService;
    }

    @PostMapping("/api/dataset/euw/balanced")
    public BalancedDatasetResultDto collectBalancedDataset(
            @RequestParam List<String> seedPuuids,
            @RequestParam(defaultValue = "2") int targetPerBucket,
            @RequestParam(defaultValue = "5") int matchesPerPlayer,
            @RequestParam(defaultValue = "5") int maxPlayersToVisit
    ) {
        return balancedDatasetCrawlerService.collectBalancedDatasetEUW(
                seedPuuids,
                targetPerBucket,
                matchesPerPlayer,
                maxPlayersToVisit
        );
    }

    @GetMapping("/api/dataset/euw/balanced/help")
    public String help() {
        return """
                POST /api/dataset/euw/balanced

                Required:
                seedPuuids - one or more starting PUUID values

                Optional:
                targetPerBucket - target amount for each role/result bucket
                matchesPerPlayer - how many matches to scan per player
                maxPlayersToVisit - safety limit for visited players

                Example:
                curl -X POST "http://localhost:8080/api/dataset/euw/balanced?seedPuuids=PUUID_HERE&targetPerBucket=2&matchesPerPlayer=5&maxPlayersToVisit=5"
                """;
    }
}