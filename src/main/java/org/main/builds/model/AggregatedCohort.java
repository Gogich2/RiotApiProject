package org.main.builds.model;

public record AggregatedCohort(
        int championId,
        BuildRole role,
        Integer opponentChampionId,
        BuildScope scope,
        int games,
        int wins,
        int anchorGames,
        int comparisonGames,
        BuildConfidence confidence,
        BuildSnapshotPayload payload
) {
}
