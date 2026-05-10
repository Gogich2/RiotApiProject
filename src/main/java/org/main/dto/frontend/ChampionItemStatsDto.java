package org.main.dto.frontend;

public record ChampionItemStatsDto(
        Integer itemId,
        Long games,
        Long wins,
        Double winrate,
        Double pickrate
) {
}