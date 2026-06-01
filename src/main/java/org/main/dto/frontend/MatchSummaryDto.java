package org.main.dto.frontend;

public record MatchSummaryDto(
        String matchId,
        Integer queueId,
        String queueName,
        String gameVersion,
        String patch,
        Long gameCreationMs,
        Long gameDurationMs,
        Double gameDurationMinutes
) {
}
