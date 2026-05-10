package org.main.controller.frontend;

import java.util.List;
import org.main.dto.frontend.PlayerInsightDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.PlayerSummaryDto;
import org.main.service.frontend.FrontendStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final FrontendStatsService frontendStatsService;

    public PlayerController(FrontendStatsService frontendStatsService) {
        this.frontendStatsService = frontendStatsService;
    }

    @GetMapping("/{puuid}/summary")
    public PlayerSummaryDto summary(@PathVariable String puuid) {
        return frontendStatsService.getPlayerSummary(puuid);
    }

    @GetMapping("/{puuid}/matches")
    public List<PlayerRecentMatchDto> matches(@PathVariable String puuid,
                                              @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return frontendStatsService.getPlayerRecentMatches(puuid, limit);
    }

    @GetMapping("/{puuid}/insights")
    public List<PlayerInsightDto> insights(@PathVariable String puuid) {
        return frontendStatsService.getPlayerInsights(puuid);
    }
}