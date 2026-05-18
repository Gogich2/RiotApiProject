package org.main.dto.frontend;

public record ChampionItemStatsDto(
        Integer itemId,
        String itemName,
        String imageUrl,
        Long games,
        Long wins,
        Double winrate,
        Double pickrate
) {
}
