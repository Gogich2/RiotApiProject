package org.main.refresh.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;

public record PlayerRefreshStatusDto(
        UUID id,
        String puuid,
        RefreshSource source,
        RefreshState state,
        OffsetDateTime requestedAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime retryAfter,
        String message
) {

    public static PlayerRefreshStatusDto from(PlayerRefreshJobEntity job) {
        return new PlayerRefreshStatusDto(
                job.getId(),
                job.getPuuid(),
                job.getSource(),
                job.getState(),
                job.getRequestedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getRetryAfter(),
                job.getUserMessage()
        );
    }
}
