package org.main.dto.frontend;

import java.util.List;
import org.main.refresh.dto.PlayerRefreshStatusDto;

public record PlayerDashboardDto(
        PlayerSummaryDto player,
        String analysisQueue,
        int analysisQueueId,
        List<RecentFormDto> recentForm,
        List<PlayerRankSummaryDto> ranks,
        List<PlayerChampionStatsDto> championPool,
        ChampionPoolHealthDto championPoolHealth,
        List<PlayerInsightDto> priorities,
        PlayerFreshnessDto freshness,
        PlayerRefreshStatusDto refresh
) {
}
