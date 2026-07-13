package org.main.builds.store;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;
import org.springframework.transaction.annotation.Transactional;

public class BuildPublisher {

    private final BuildSnapshotRepository repository;

    private final BuildSnapshotValidator validator;

    public BuildPublisher(BuildSnapshotRepository repository, BuildSnapshotValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Transactional
    public void publish(
            UUID runId,
            AggregationResult result,
            int aggregationVersion,
            int payloadSchemaVersion,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark
    ) {
        AggregationRun run = repository.findRun(runId).
                orElseThrow(() -> new IllegalArgumentException("Aggregation run was not found"));
        int validationCount = validator.validate(
                run, runId, result, aggregationVersion, window, queue, watermark);
        repository.insertSnapshots(runId, result.cohorts(), aggregationVersion,
                payloadSchemaVersion, window, queue, watermark, validationCount);
        repository.publishRun(runId);
    }
}
