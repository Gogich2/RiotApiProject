package org.main.dto.frontend;

public record ChampionStatDto(
        Integer championId,
        String championName,
        Long games,
        Long wins,
        Double winrate
) {
}