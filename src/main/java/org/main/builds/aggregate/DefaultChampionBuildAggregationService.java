package org.main.builds.aggregate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.main.builds.BuildProperties;
import org.main.builds.extract.BuildObservationFactory;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchVersion;
import org.main.builds.model.PatchWindow;
import org.main.builds.source.BuildSourceMatch;
import org.main.builds.source.BuildSourceRepository;
import org.main.builds.source.BuildSourceSelection;
import org.main.builds.source.ItemCatalog;
import org.main.builds.store.AggregationRun;
import org.main.builds.store.BuildPublisher;
import org.main.builds.store.BuildSnapshotRepository;

public final class DefaultChampionBuildAggregationService
        implements ChampionBuildAggregationService {

    private final BuildSourceRepository sourceRepository;

    private final ItemCatalog itemCatalog;

    private final BuildObservationFactory observationFactory;

    private final BuildAggregator aggregator;

    private final BuildSnapshotRepository snapshotRepository;

    private final BuildPublisher publisher;

    private final BuildProperties properties;

    public DefaultChampionBuildAggregationService(
            BuildSourceRepository sourceRepository,
            ItemCatalog itemCatalog,
            BuildObservationFactory observationFactory,
            BuildAggregator aggregator,
            BuildSnapshotRepository snapshotRepository,
            BuildPublisher publisher,
            BuildProperties properties
    ) {
        this.sourceRepository = sourceRepository;
        this.itemCatalog = itemCatalog;
        this.observationFactory = observationFactory;
        this.aggregator = aggregator;
        this.snapshotRepository = snapshotRepository;
        this.publisher = publisher;
        this.properties = properties;
    }

    @Override
    public AggregationOutcome refresh(BuildQueue queue) {
        String patch = null;
        try {
            Optional<String> latestPatch = sourceRepository.findLatestPatch(queue);
            if (latestPatch.isEmpty()) {
                return insufficient(null);
            }
            PatchVersion anchor = PatchVersion.parse(latestPatch.get());
            patch = anchor.displayName();
            Optional<PatchVersion> previous = previousPatch(queue, anchor);
            if (anchor.minor() <= 1 && previous.isEmpty()) {
                return insufficient(patch);
            }
            PatchWindow window = PatchWindow.forAnchor(anchor, previous.stream().toList());
            BuildSourceSelection selection = sourceRepository.selectSource(window, queue);
            List<String> matchIds = selection.matchIds();
            OffsetDateTime watermark = selection.inputWatermark();
            if (matchIds.isEmpty() || watermark == null) {
                return insufficient(patch);
            }
            Optional<AggregationRun> latestRun =
                    snapshotRepository.findLatestRun(
                            properties.aggregationVersion(), window, queue);
            if (latestRun.filter(run -> unchanged(run, window, queue, watermark)).isPresent()) {
                AggregationRun run = latestRun.get();
                return new AggregationOutcome(AggregationOutcome.Status.NO_CHANGE,
                        patch, matchIds.size(), run.snapshotCount(), run.id());
            }
            UUID runId = snapshotRepository.startRun(
                    properties.aggregationVersion(), window, queue, watermark);
            return aggregate(runId, patch, window, queue, watermark, matchIds);
        } catch (RuntimeException exception) {
            return new AggregationOutcome(
                    AggregationOutcome.Status.FAILED, patch, 0, 0, null);
        }
    }

    private Optional<PatchVersion> previousPatch(BuildQueue queue, PatchVersion anchor) {
        if (anchor.minor() > 1) {
            return Optional.empty();
        }
        return sourceRepository.findPreviousMajorLastPatch(queue, anchor.major() - 1).
                map(PatchVersion::parse);
    }

    private boolean unchanged(
            AggregationRun run,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark
    ) {
        return "COMPLETED".equals(run.state())
                && run.aggregationVersion() == properties.aggregationVersion()
                && run.window().equals(window)
                && run.queue() == queue
                && run.inputWatermark().isEqual(watermark);
    }

    private AggregationOutcome aggregate(
            UUID runId,
            String patch,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark,
            List<String> matchIds
    ) {
        String failureCategory = "ITEM_CATALOG_REFRESH";
        try {
            itemCatalog.refresh();
            List<BuildObservation> observations = new ArrayList<>();
            for (int start = 0; start < matchIds.size(); start += properties.batchSize()) {
                int end = Math.min(start + properties.batchSize(), matchIds.size());
                failureCategory = "LOAD_BATCH";
                List<BuildSourceMatch> matches = sourceRepository.loadBatch(
                        matchIds.subList(start, end));
                failureCategory = "EXTRACT_OBSERVATIONS";
                for (BuildSourceMatch match : matches) {
                    observations.addAll(observationFactory.from(match));
                }
            }
            if (observations.isEmpty()) {
                failRun(runId, "EMPTY_OBSERVATIONS");
                return failed(patch, matchIds.size(), runId);
            }
            failureCategory = "AGGREGATE";
            AggregationResult result = aggregator.aggregate(window, queue, observations);
            failureCategory = "PUBLISH";
            publisher.publish(runId, result, properties.aggregationVersion(),
                    properties.payloadSchemaVersion(), window, queue, watermark);
            return new AggregationOutcome(AggregationOutcome.Status.PUBLISHED,
                    patch, matchIds.size(), result.cohorts().size(), runId);
        } catch (RuntimeException exception) {
            failRun(runId, failureCategory);
            return failed(patch, matchIds.size(), runId);
        }
    }

    private void failRun(UUID runId, String category) {
        try {
            snapshotRepository.failRun(runId, category);
        } catch (RuntimeException ignored) {
            // The orchestration outcome remains FAILED if failure recording also fails.
        }
    }

    private AggregationOutcome insufficient(String patch) {
        return new AggregationOutcome(
                AggregationOutcome.Status.INSUFFICIENT_SOURCE_DATA, patch, 0, 0, null);
    }

    private AggregationOutcome failed(String patch, int sourceMatches, UUID runId) {
        return new AggregationOutcome(
                AggregationOutcome.Status.FAILED, patch, sourceMatches, 0, runId);
    }
}
