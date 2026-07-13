package org.main.controller.frontend;

import java.util.List;
import org.main.dto.frontend.PlayerInsightDto;
import org.main.dto.frontend.PlayerLeaderboardResponseDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.PlayerSummaryDto;
import org.main.persistence.repository.LeagueEntryRepository;
import org.main.persistence.repository.LeagueEntrySnapshotRepository;
import org.main.service.RankEnrichmentService;
import org.main.service.frontend.FrontendStatsService;
import org.main.dto.frontend.PlayerChampionStatsDto;
import org.main.dto.frontend.PlayerDashboardDto;
import org.main.service.frontend.PlayerDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final FrontendStatsService frontendStatsService;

    private final RankEnrichmentService rankEnrichmentService;

    private final LeagueEntryRepository leagueEntryRepository;

    private final LeagueEntrySnapshotRepository leagueEntrySnapshotRepository;

    private final PlayerDashboardService playerDashboardService;

    public PlayerController(FrontendStatsService frontendStatsService,
                            RankEnrichmentService rankEnrichmentService,
                            LeagueEntryRepository leagueEntryRepository,
                            LeagueEntrySnapshotRepository leagueEntrySnapshotRepository,
                            PlayerDashboardService playerDashboardService) {
        this.frontendStatsService = frontendStatsService;
        this.rankEnrichmentService = rankEnrichmentService;
        this.leagueEntryRepository = leagueEntryRepository;
        this.leagueEntrySnapshotRepository = leagueEntrySnapshotRepository;
        this.playerDashboardService = playerDashboardService;

    }

    @GetMapping("/leaderboard")
    public PlayerLeaderboardResponseDto leaderboard() {
        return frontendStatsService.getPlayerLeaderboards();
    }

    @GetMapping("/{puuid}/summary")
    public PlayerSummaryDto summary(@PathVariable String puuid) {
        return frontendStatsService.getPlayerSummary(puuid);
    }

    @GetMapping("/{puuid}/champions")
    public List<PlayerChampionStatsDto> champions(@PathVariable String puuid) {
        return frontendStatsService.getPlayerChampions(puuid);
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

    @GetMapping("/{puuid}/dashboard")
    public PlayerDashboardDto dashboard(@PathVariable String puuid) {
        return playerDashboardService.getDashboard(puuid);
    }
}
