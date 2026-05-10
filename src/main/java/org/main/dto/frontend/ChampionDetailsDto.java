package org.main.dto.frontend;

import java.util.List;

public record ChampionDetailsDto(
        Integer championId,
        String championName,
        String title,
        String imageUrl,
        String splashUrl,
        String lore,
        ChampionSummaryDto summary,
        List<ChampionAbilityDto> abilities
) {
}