package org.main.controller;

import org.main.dto.DataIntegrityReportDto;
import org.main.dto.PlayerProfileRepairResultDto;
import org.main.dto.RankRepairResultDto;
import org.main.service.DataIntegrityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataIntegrityController {

    private final DataIntegrityService dataIntegrityService;

    public DataIntegrityController(DataIntegrityService dataIntegrityService) {
        this.dataIntegrityService = dataIntegrityService;
    }

    @GetMapping("/api/integrity/check")
    public DataIntegrityReportDto check() {
        return dataIntegrityService.check();
    }

    @PostMapping("/api/integrity/repair")
    public DataIntegrityReportDto repair(@RequestParam(defaultValue = "20") int limit) {
        return dataIntegrityService.repairMissingTimelines(limit);
    }

    @PostMapping("/api/integrity/repair-ranks")
    public RankRepairResultDto repairRanks(@RequestParam(defaultValue = "20") int limit) {
        return dataIntegrityService.repairMissingRanks(limit);
    }

    @PostMapping("/api/integrity/repair-player-profiles")
    public PlayerProfileRepairResultDto repairPlayerProfiles(@RequestParam(defaultValue = "20") int limit) {
        return dataIntegrityService.repairMissingPlayerProfiles(limit);
    }
}