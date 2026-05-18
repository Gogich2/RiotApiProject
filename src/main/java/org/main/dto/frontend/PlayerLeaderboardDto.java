package org.main.dto.frontend;

public record PlayerLeaderboardDto(
        String puuid,
        String gameName,
        String tagLine,
        Integer profileIconId,
        String profileIconUrl,
        Long matches,
        Long wins,
        Double winrate,
        Double averageKills,
        Double averageDeaths,
        Double averageAssists
) {
}
