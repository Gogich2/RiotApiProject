package org.main.dto.frontend;

public record PlayerChampionStatsDto(
        Integer championId,
        String championName,
        String imageUrl,
        Long games,
        Long wins,
        Double winrate,
        Double averageKills,
        Double averageDeaths,
        Double averageAssists
) {
}