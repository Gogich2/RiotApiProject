package org.main.builds.store;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;

public record AggregationRun(
        UUID id,
        int aggregationVersion,
        PatchWindow window,
        BuildQueue queue,
        OffsetDateTime inputWatermark,
        String state,
        int sourceMatchCount,
        int validationCount,
        int snapshotCount,
        String failureCategory,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
}
