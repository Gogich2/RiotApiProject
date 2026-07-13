package org.main.builds.api;

import java.time.OffsetDateTime;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildScope;

public record ChampionBuildResponse(
        boolean available,
        RequestedFilters requested,
        ResolvedFilters resolved,
        BuildScope resultScope,
        BuildConfidence confidence,
        int games,
        int wins,
        double winRate,
        boolean stale,
        boolean historical,
        BuildFallbackReason fallbackReason,
        String evidenceLabel,
        String explanation,
        OffsetDateTime calculatedAt,
        OffsetDateTime publishedAt,
        DisplayBuildPayload build
) {
}
