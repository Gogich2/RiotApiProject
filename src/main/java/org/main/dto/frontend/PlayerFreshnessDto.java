package org.main.dto.frontend;

import java.time.OffsetDateTime;

public record PlayerFreshnessDto(
        OffsetDateTime lastUpdatedAt,
        boolean stale,
        int sampleSize
) {
}
