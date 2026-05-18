package org.main.dto.frontend;

public record ChampionStatDto(
        Integer championId,
        String championName,
        String imageUrl,
        Long games,
        Long wins,
        Double winrate,
        String primaryRole
) {
}
