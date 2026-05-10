package org.main.controller.frontend;

import org.main.dto.frontend.OverviewStatsDto;
import org.main.service.frontend.FrontendStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final FrontendStatsService frontendStatsService;

    public StatsController(FrontendStatsService frontendStatsService) {
        this.frontendStatsService = frontendStatsService;
    }

    @GetMapping("/overview")
    public OverviewStatsDto overview() {
        return frontendStatsService.getOverview();
    }
}