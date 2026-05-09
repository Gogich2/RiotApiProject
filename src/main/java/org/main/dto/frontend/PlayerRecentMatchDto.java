package org.main.dto.frontend;

public record PlayerRecentMatchDto(
        String matchId,
        Integer championId,
        String championName,
        Boolean win,
        Integer kills,
        Integer deaths,
        Integer assists,
        Integer queueId,
        String gameVersion,
        Long gameCreationMs,
        Long gameDurationMs
) {
}