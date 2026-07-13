package org.main.account.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedProfileDto(
        UUID id,
        String puuid,
        String gameName,
        String tagLine,
        Integer profileIconId,
        String personalLabel,
        boolean isDefault,
        OffsetDateTime savedAt,
        OffsetDateTime lastViewedAt
) {
}
