package org.main.dto.frontend;

public record ChampionAbilityDto(
        String abilityKey,
        String abilityName,
        String abilityDescription,
        String imageUrl
) {
}