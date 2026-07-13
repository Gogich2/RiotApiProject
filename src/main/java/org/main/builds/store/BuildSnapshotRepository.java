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
                         OffsetDateTime watermark, int validationCount);

    void publishRun(UUID runId);

    void failRun(UUID runId, String failureCategory);

    Optional<BuildSnapshot> findPublished(BuildLookup lookup);

    Optional<BuildSnapshot> findPublished(
            int aggregationVersion,
            String anchorPatch,
            BuildQueue queue,
            int championId,
            org.main.builds.model.BuildRole role,
            Integer opponentChampionId
    );

    List<BuildSnapshot> findHistoricalBaselines(BuildLookup lookup, int limit);

    List<BuildSnapshot> findHistoricalBaselines(
            int aggregationVersion,
            String anchorPatch,
            BuildQueue queue,
            int championId,
            org.main.builds.model.BuildRole role,
            int limit
    );

    List<BuildSnapshot> findPublishedForChampion(
            int aggregationVersion, BuildQueue queue, int championId);

    Optional<AggregationRun> findRun(UUID runId);

    Optional<AggregationRun> findLatestRun(String patch, BuildQueue queue);

    Optional<AggregationRun> findLatestRun(
            int aggregationVersion, PatchWindow window, BuildQueue queue);
}
