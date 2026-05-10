package org.main.dto.frontend;

public record ChampionSummaryDto(
        Integer championId,
        String championName,
        Long games,
        Long wins,
        Double winrate,
        Double averageKills,
        Double averageDeaths,
        Double averageAssists,
        Double averageGold,
        Double averageDamageToChampions,
        Double averageVisionScore
) {
}