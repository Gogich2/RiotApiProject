package org.main.dto.frontend;

public record MatchParticipantSkillOrderDto(
        Integer skillOrder,
        Integer skillSlot,
        String levelUpType,
        Long timestampMs,
        Integer minute
) {
}
