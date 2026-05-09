package org.main.dto.frontend;

public record PlayerSummaryDto(
        String puuid,
        String gameName,
        String tagLine,
        Long matches,
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