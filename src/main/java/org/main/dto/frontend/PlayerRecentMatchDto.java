package org.main.dto.frontend;

import java.util.List;

public record PlayerRecentMatchDto(
        String matchId,
        Integer championId,
        String championName,
        String championImageUrl,
        Boolean win,
        Integer kills,
        Integer deaths,
        Integer assists,
        Integer queueId,
        String gameVersion,
        Long gameCreationMs,
        Long gameDurationMs,
        List<PlayerMatchItemDto> finalItems
) {
}
