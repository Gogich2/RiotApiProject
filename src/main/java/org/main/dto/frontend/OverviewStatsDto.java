package org.main.dto.frontend;

import java.util.List;

public record OverviewStatsDto(
        Long totalMatches,
        Long totalPlayers,
        Long totalParticipants,
        Double averageMatchDurationMinutes,
        List<ChampionStatDto> mostPopularChampions,
        List<ChampionStatDto> bestWinrateChampions
) {
}