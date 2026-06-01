package org.main.dto.frontend;

public record MatchParticipantItemEventDto(
        String eventType,
        Integer itemId,
        String itemName,
        String imageUrl,
        Long timestampMs,
        Integer minute
) {
}
