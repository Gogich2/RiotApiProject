package org.main.dto.frontend;

public record MatchTimelineEventDto(
        Long timestampMs,
        Integer minute,
        String type,
        Integer participantId,
        Integer killerId,
        Integer victimId,
        Integer itemId,
        String itemName,
        String wardType,
        String buildingType,
        String laneType,
        MatchTimelinePositionDto position
) {
}
