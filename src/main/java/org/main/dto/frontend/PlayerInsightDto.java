package org.main.dto.frontend;

import java.time.OffsetDateTime;

public record PlayerInsightDto(
        Long id,
        String puuid,
        String insightType,
        String title,
        String description,
        Double score,
        OffsetDateTime createdAt
) {
}