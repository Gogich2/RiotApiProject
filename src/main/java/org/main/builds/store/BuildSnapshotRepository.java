package org.main.builds.store;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;

public interface BuildSnapshotRepository {

    UUID startRun(int version, PatchWindow window, BuildQueue queue,
                  OffsetDateTime watermark);

    void insertSnapshots(UUID runId, List<AggregatedCohort> cohorts,
                         int aggregationVersion, int payloadSchemaVersion,
                         PatchWindow window, BuildQueue queue,
                         OffsetDateTime watermark);

    void publishRun(UUID runId);

    void failRun(UUID runId, String failureCategory);

    Optional<BuildSnapshot> findPublished(BuildLookup lookup);

    List<BuildSnapshot> findHistoricalBaselines(BuildLookup lookup, int limit);

    Optional<AggregationRun> findLatestRun(String patch, BuildQueue queue);
}
