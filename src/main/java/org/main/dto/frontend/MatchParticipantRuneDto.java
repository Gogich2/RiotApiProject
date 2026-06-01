package org.main.dto.frontend;

public record MatchParticipantRuneDto(
        Integer styleId,
        String styleType,
        String styleName,
        String styleIconUrl,
        Integer runeId,
        String runeName,
        String runeIconUrl,
        Integer runeSlot,
        Integer selectionOrder,
        Boolean isKeystone
) {
}
