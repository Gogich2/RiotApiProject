package org.main.builds.store;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;

public record BuildSnapshot(
        UUID id,
        UUID runId,
        int aggregationVersion,
        int payloadSchemaVersion,
        PatchWindow window,
        BuildQueue queue,
        int championId,
        BuildRole role,
        Integer opponentChampionId,
        BuildScope scope,
        int games,
        int wins,
        int anchorGames,
        int comparisonGames,
        BuildConfidence confidence,
        OffsetDateTime inputWatermark,
        int sourceMatchCount,
        OffsetDateTime calculatedAt,
        OffsetDateTime publishedAt,
        String publicationState,
        BuildSnapshotPayload payload
) {
}
