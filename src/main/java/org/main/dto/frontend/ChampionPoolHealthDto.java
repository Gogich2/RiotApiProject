package org.main.dto.frontend;

public record ChampionPoolHealthDto(
        String status,
        int uniqueChampions,
        int gamesAnalyzed,
        String message
) {
}
