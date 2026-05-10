package org.main.dto.frontend;

import java.util.List;

public record SearchResultDto(
        List<ChampionSearchResultDto> champions,
        List<PlayerSearchResultDto> players
) {
}