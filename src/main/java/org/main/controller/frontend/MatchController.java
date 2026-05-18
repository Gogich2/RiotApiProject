package org.main.controller.frontend;

import org.main.dto.frontend.MatchDetailsDto;
import org.main.service.frontend.FrontendStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final FrontendStatsService frontendStatsService;

    public MatchController(FrontendStatsService frontendStatsService) {
        this.frontendStatsService = frontendStatsService;
    }

    @GetMapping("/{matchId}/details")
    public MatchDetailsDto details(@PathVariable String matchId,
                                   @RequestParam(value = "puuid", required = false) String puuid) {
        return frontendStatsService.getMatchDetails(matchId, puuid);
    }
}
