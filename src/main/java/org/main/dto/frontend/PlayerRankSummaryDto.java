package org.main.dto.frontend;

import java.time.OffsetDateTime;

public record PlayerRankSummaryDto(
        String queueType,
        String tier,
        String rank,
        Integer leaguePoints,
        Integer wins,
        Integer losses,
        double winRate,
        OffsetDateTime lastSyncedAt
) {
}
