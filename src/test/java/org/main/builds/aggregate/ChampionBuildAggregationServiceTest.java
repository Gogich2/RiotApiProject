package org.main.builds.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.main.builds.BuildProperties;
import org.main.builds.extract.BuildObservationFactory;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;
import org.main.builds.source.BuildSourceMatch;
import org.main.builds.source.BuildSourceRepository;
import org.main.builds.source.BuildSourceSelection;
import org.main.builds.source.ItemCatalog;
import org.main.builds.store.AggregationRun;
import org.main.builds.store.BuildPublisher;
import org.main.builds.store.BuildSnapshotRepository;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ExtendWith(MockitoExtension.class)
class ChampionBuildAggregationServiceTest {

    private static final PatchWindow WINDOW = new PatchWindow("16.13", "16.12");

    private static final OffsetDateTime WATERMARK =
            OffsetDateTime.parse("2026-07-13T10:15:30Z");

    private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock
    private BuildSourceRepository sourceRepository;

    @Mock
    private ItemCatalog itemCatalog;

    @Mock
    private BuildObservationFactory observationFactory;

    @Mock
    private BuildAggregator aggregator;

    @Mock
    private BuildSnapshotRepository snapshotRepository;

    @Mock
    private BuildPublisher publisher;

    private ChampionBuildAggregationService service;

    @BeforeEach
    void setUp() {
        service = new DefaultChampionBuildAggregationService(sourceRepository, itemCatalog,
                observationFactory, aggregator, snapshotRepository, publisher, properties());
    }

    @Test
    void publishesAnAnchorOnlyAggregationWithoutSkippingTheComparisonPatch() {
        BuildSourceMatch firstMatch = mock(BuildSourceMatch.class);
        BuildSourceMatch secondMatch = mock(BuildSourceMatch.class);
        BuildObservation firstObservation = mock(BuildObservation.class);
        BuildObservation secondObservation = mock(BuildObservation.class);
        AggregationResult result = result(2);
        prepareSelection(List.of("EUW1_1", "EUW1_2"));
        when(sourceRepository.loadBatch(List.of("EUW1_1", "EUW1_2"))).
                thenReturn(List.of(firstMatch, secondMatch));
        when(observationFactory.from(firstMatch)).thenReturn(List.of(firstObservation));
        when(observationFactory.from(secondMatch)).thenReturn(List.of(secondObservation));
        when(aggregator.aggregate(WINDOW, BuildQueue.SOLO_DUO,
                List.of(firstObservation, secondObservation))).thenReturn(result);

        AggregationOutcome outcome = service.refresh(BuildQueue.SOLO_DUO);

        assertThat(outcome).isEqualTo(new AggregationOutcome(
                AggregationOutcome.Status.PUBLISHED, "16.13", 2, 2, RUN_ID));
        verify(sourceRepository, never()).findPreviousMajorLastPatch(any(), any(Integer.class));
        verify(itemCatalog).refresh();
        verify(publisher).publish(RUN_ID, result, 1, 1, WINDOW,
                BuildQueue.SOLO_DUO, WATERMARK);
    }

    @Test
    void returnsNoChangeOnlyForTheMatchingCompletedRun() {
        when(sourceRepository.findLatestPatch(BuildQueue.SOLO_DUO)).thenReturn(Optional.of("16.13"));
        when(sourceRepository.selectSource(WINDOW, BuildQueue.SOLO_DUO)).
                thenReturn(new BuildSourceSelection(WINDOW, BuildQueue.SOLO_DUO,
                        WATERMARK, List.of("EUW1_1")));
        when(snapshotRepository.findLatestRun("16.13", BuildQueue.SOLO_DUO)).
                thenReturn(Optional.of(run("COMPLETED", 1, WINDOW, WATERMARK, 7)));

        AggregationOutcome outcome = service.refresh(BuildQueue.SOLO_DUO);

        assertThat(outcome).isEqualTo(new AggregationOutcome(
                AggregationOutcome.Status.NO_CHANGE, "16.13", 1, 7, RUN_ID));
        verify(snapshotRepository, never()).startRun(any(Integer.class), any(), any(), any());
        verify(itemCatalog, never()).refresh();
        verify(sourceRepository, never()).loadBatch(any());
    }

    @Test
    void aCompletedRunWithDifferentIdentityDoesNotSuppressAggregation() {
        BuildSourceMatch match = mock(BuildSourceMatch.class);
        BuildObservation observation = mock(BuildObservation.class);
        AggregationResult result = result(1);
        prepareSelection(List.of("EUW1_1"));
        when(snapshotRepository.findLatestRun("16.13", BuildQueue.SOLO_DUO)).
                thenReturn(Optional.of(run("COMPLETED", 2, WINDOW, WATERMARK, 1)));
        when(sourceRepository.loadBatch(List.of("EUW1_1"))).thenReturn(List.of(match));
        when(observationFactory.from(match)).thenReturn(List.of(observation));
        when(aggregator.aggregate(WINDOW, BuildQueue.SOLO_DUO, List.of(observation))).
                thenReturn(result);

        assertThat(service.refresh(BuildQueue.SOLO_DUO).status()).
                isEqualTo(AggregationOutcome.Status.PUBLISHED);

        verify(snapshotRepository).startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);
    }

    @Test
    void returnsInsufficientWithoutARunAtAnUnresolvedAnnualBoundary() {
        when(sourceRepository.findLatestPatch(BuildQueue.SOLO_DUO)).thenReturn(Optional.of("16.1"));
        when(sourceRepository.findPreviousMajorLastPatch(BuildQueue.SOLO_DUO, 15)).
                thenReturn(Optional.empty());

        AggregationOutcome outcome = service.refresh(BuildQueue.SOLO_DUO);

        assertThat(outcome).isEqualTo(new AggregationOutcome(
                AggregationOutcome.Status.INSUFFICIENT_SOURCE_DATA, "16.1", 0, 0, null));
        verify(sourceRepository, never()).selectSource(any(), any());
        verify(snapshotRepository, never()).startRun(any(Integer.class), any(), any(), any());
    }

    @Test
    void emptyFrozenSourceOrWatermarkReturnsInsufficientWithoutARun() {
        when(sourceRepository.findLatestPatch(BuildQueue.SOLO_DUO)).thenReturn(Optional.of("16.13"));
        when(sourceRepository.selectSource(WINDOW, BuildQueue.SOLO_DUO)).
                thenReturn(new BuildSourceSelection(WINDOW, BuildQueue.SOLO_DUO,
                        WATERMARK, List.of()));

        assertThat(service.refresh(BuildQueue.SOLO_DUO).status()).
                isEqualTo(AggregationOutcome.Status.INSUFFICIENT_SOURCE_DATA);

        when(sourceRepository.selectSource(WINDOW, BuildQueue.SOLO_DUO)).
                thenReturn(new BuildSourceSelection(WINDOW, BuildQueue.SOLO_DUO,
                        null, List.of("EUW1_1")));

        assertThat(service.refresh(BuildQueue.SOLO_DUO).status()).
                isEqualTo(AggregationOutcome.Status.INSUFFICIENT_SOURCE_DATA);
        verify(snapshotRepository, never()).startRun(any(Integer.class), any(), any(), any());
    }

    @Test
    void emptyObservationsFailTheExactRunWithoutPublishing() {
        BuildSourceMatch match = mock(BuildSourceMatch.class);
        prepareSelection(List.of("EUW1_1"));
        when(sourceRepository.loadBatch(List.of("EUW1_1"))).thenReturn(List.of(match));
        when(observationFactory.from(match)).thenReturn(List.of());

        AggregationOutcome outcome = service.refresh(BuildQueue.SOLO_DUO);

        assertThat(outcome).isEqualTo(new AggregationOutcome(
                AggregationOutcome.Status.FAILED, "16.13", 1, 0, RUN_ID));
        verify(snapshotRepository).failRun(RUN_ID, "EMPTY_OBSERVATIONS");
        verify(aggregator, never()).aggregate(any(), any(), any());
        verify(publisher, never()).publish(any(), any(), any(Integer.class),
                any(Integer.class), any(), any(), any());
        verify(snapshotRepository, never()).publishRun(any());
    }

    @Test
    void publisherFailureFailsTheExactRunWithABoundedSafeCategory() {
        BuildSourceMatch match = mock(BuildSourceMatch.class);
        BuildObservation observation = mock(BuildObservation.class);
        AggregationResult result = result(1);
        prepareSelection(List.of("EUW1_1"));
        when(sourceRepository.loadBatch(List.of("EUW1_1"))).thenReturn(List.of(match));
        when(observationFactory.from(match)).thenReturn(List.of(observation));
        when(aggregator.aggregate(WINDOW, BuildQueue.SOLO_DUO, List.of(observation))).
                thenReturn(result);
        doThrow(new IllegalStateException("database-secret-should-not-leak")).when(publisher).
                publish(RUN_ID, result, 1, 1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);

        AggregationOutcome outcome = service.refresh(BuildQueue.SOLO_DUO);

        assertThat(outcome.status()).isEqualTo(AggregationOutcome.Status.FAILED);
        verify(snapshotRepository).failRun(RUN_ID, "PUBLISH");
        verify(snapshotRepository, never()).publishRun(any());
        verify(snapshotRepository, never()).insertSnapshots(any(), any(),
                any(Integer.class), any(Integer.class), any(), any(), any(), any(Integer.class));
    }

    @Test
    void refreshFailureAfterRunCreationFailsThatRun() {
        prepareSelection(List.of("EUW1_1"));
        doThrow(new IllegalStateException("refresh failed")).when(itemCatalog).refresh();

        assertThat(service.refresh(BuildQueue.SOLO_DUO).status()).
                isEqualTo(AggregationOutcome.Status.FAILED);

        verify(snapshotRepository).failRun(RUN_ID, "ITEM_CATALOG_REFRESH");
        verify(sourceRepository, never()).loadBatch(any());
    }

    @Test
    void slicesTheFrozenIdsWithoutReselectingAndRefreshesCatalogOnce() {
        List<String> mutableIds = new ArrayList<>(
                List.of("EUW1_1", "EUW1_2", "EUW1_3", "EUW1_4", "EUW1_5"));
        BuildSourceSelection frozen = new BuildSourceSelection(
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK, mutableIds);
        when(sourceRepository.findLatestPatch(BuildQueue.SOLO_DUO)).thenReturn(Optional.of("16.13"));
        when(sourceRepository.selectSource(WINDOW, BuildQueue.SOLO_DUO)).thenReturn(frozen);
        when(snapshotRepository.findLatestRun("16.13", BuildQueue.SOLO_DUO)).
                thenReturn(Optional.empty());
        when(snapshotRepository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK)).
                thenReturn(RUN_ID);
        when(sourceRepository.loadBatch(any())).thenReturn(List.of());
        mutableIds.clear();

        service.refresh(BuildQueue.SOLO_DUO);

        verify(sourceRepository).loadBatch(List.of("EUW1_1", "EUW1_2"));
        verify(sourceRepository).loadBatch(List.of("EUW1_3", "EUW1_4"));
        verify(sourceRepository).loadBatch(List.of("EUW1_5"));
        verify(sourceRepository).selectSource(WINDOW, BuildQueue.SOLO_DUO);
        verify(itemCatalog, times(1)).refresh();
        InOrder order = inOrder(itemCatalog, sourceRepository);
        order.verify(itemCatalog).refresh();
        order.verify(sourceRepository).loadBatch(List.of("EUW1_1", "EUW1_2"));
    }

    @Test
    void guardedSchedulerInvokesOnlySoloDuo() {
        List<BuildQueue> queues = new ArrayList<>();
        ChampionBuildScheduler scheduler = new ChampionBuildScheduler(queue -> {
            queues.add(queue);
            return new AggregationOutcome(
                    AggregationOutcome.Status.NO_CHANGE, "16.13", 0, 0, null);
        });

        scheduler.refreshSoloDuo();

        assertThat(queues).containsExactly(BuildQueue.SOLO_DUO);
        ConditionalOnProperty guard = ChampionBuildScheduler.class.
                getAnnotation(ConditionalOnProperty.class);
        assertThat(guard.name()).containsExactly("app.builds.scheduler-enabled");
        assertThat(guard.havingValue()).isEqualTo("true");
        assertThat(guard.matchIfMissing()).isFalse();
    }

    private void prepareSelection(List<String> matchIds) {
        when(sourceRepository.findLatestPatch(BuildQueue.SOLO_DUO)).thenReturn(Optional.of("16.13"));
        when(sourceRepository.selectSource(WINDOW, BuildQueue.SOLO_DUO)).
                thenReturn(new BuildSourceSelection(WINDOW, BuildQueue.SOLO_DUO,
                        WATERMARK, matchIds));
        when(snapshotRepository.findLatestRun("16.13", BuildQueue.SOLO_DUO)).
                thenReturn(Optional.empty());
        when(snapshotRepository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK)).
                thenReturn(RUN_ID);
    }

    private AggregationRun run(String state, int version, PatchWindow window,
                               OffsetDateTime watermark, int snapshots) {
        return new AggregationRun(RUN_ID, version, window, BuildQueue.SOLO_DUO,
                watermark, state, 1, snapshots, snapshots, null, WATERMARK, WATERMARK);
    }

    private AggregationResult result(int snapshots) {
        List<AggregatedCohort> cohorts = new ArrayList<>();
        for (int index = 0; index < snapshots; index++) {
            cohorts.add(mock(AggregatedCohort.class));
        }
        return new AggregationResult(cohorts, java.util.Set.of(), snapshots);
    }

    private BuildProperties properties() {
        return new BuildProperties(1, 1, 10, 25, 50, 0.7, 0.3, 2,
                Duration.ofMinutes(2), 2, Duration.ofHours(1), false);
    }
}
