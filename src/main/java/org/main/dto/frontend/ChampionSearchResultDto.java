package org.main.dto.frontend;

public record ChampionSearchResultDto(
        Integer championId,
        String championName,
        Long games
) {
}