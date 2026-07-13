package org.main.builds.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.main.builds.BuildProperties;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;
import org.main.builds.store.AggregationRun;
import org.main.builds.store.BuildSnapshot;
import org.main.builds.store.BuildSnapshotRepository;
import org.main.exception.NotFoundException;

class ChampionBuildServiceTest {

    private static final PatchWindow CURRENT = new PatchWindow("16.13", "16.12");

    private static final OffsetDateTime PUBLISHED =
            OffsetDateTime.parse("2026-07-13T10:00:00Z");

    private final BuildSnapshotRepository snapshots = mock(BuildSnapshotRepository.class);

    private final BuildAssetRepository assets = mock(BuildAssetRepository.class);

    private ChampionBuildService service;

    @BeforeEach
    void setUp() {
        service = new ChampionBuildService(snapshots, assets, properties());
        when(assets.findChampion(22)).thenReturn(Optional.of(
                new DisplayAsset(22, "Ashe", "/champion/Ashe.png")));
        when(assets.findChampion(55)).thenReturn(Optional.of(
                new DisplayAsset(55, "Katarina", "/champion/Katarina.png")));
        when(assets.findChampions(List.of(55))).thenReturn(Map.of(
                55, new DisplayAsset(55, "Katarina", "/champion/Katarina.png")));
        when(assets.findItems(List.of(1055))).thenReturn(Map.of(
                1055, new DisplayAsset(1055, "Doran's Blade", "/item/1055.png")));
    }

    @Test
    void returnsEligibleExactMatchupWithoutFallback() {
        BuildSnapshot exact = snapshot(CURRENT, BuildQueue.SOLO_DUO, BuildRole.BOTTOM,
                55, BuildScope.EXACT_MATCHUP, 12, PUBLISHED);
        when(snapshots.findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 55)).thenReturn(Optional.of(exact));

        ChampionBuildResponse response = service.builds(
                22, 420, "16.13", BuildRole.BOTTOM, 55);

        assertThat(response.available()).isTrue();
        assertThat(response.resultScope()).isEqualTo(BuildScope.EXACT_MATCHUP);
        assertThat(response.fallbackReason()).isEqualTo(BuildFallbackReason.NONE);
        assertThat(response.games()).isEqualTo(12);
        assertThat(response.wins()).isEqualTo(6);
        assertThat(response.build().startingItems().getFirst().assets().getFirst().label()).
                isEqualTo("Doran's Blade");
    }

    @Test
    void calculatesFilteredOptionsAndKeepsFlexUnavailableWithoutFlexSnapshots() {
        BuildSnapshot baseline = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, null, BuildScope.CHAMPION_ROLE, 80, PUBLISHED);
        BuildSnapshot exact = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, 55, BuildScope.EXACT_MATCHUP, 12, PUBLISHED);
        when(snapshots.findPublishedForChampion(
                1, BuildQueue.SOLO_DUO, 22)).thenReturn(List.of(baseline, exact));
        when(snapshots.findPublishedForChampion(
                1, BuildQueue.FLEX, 22)).thenReturn(List.of());

        ChampionBuildOptionsResponse response = service.options(
                22, 420, "16.13", BuildRole.BOTTOM);

        assertThat(response.defaults()).isEqualTo(
                new RequestedFilters(420, "16.13", BuildRole.BOTTOM, null));
        assertThat(response.queues()).extracting(QueueOption::available).
                containsExactly(true, false);
        assertThat(response.patches()).extracting(PatchOption::patch).
                containsExactly("16.13");
        assertThat(response.roles()).filteredOn(RoleOption::available).
                containsExactly(new RoleOption(BuildRole.BOTTOM, 80, true));
        assertThat(response.opponents()).containsExactly(
                new OpponentOption(55, "Katarina", "/champion/Katarina.png", 12));
        verify(assets).findChampions(List.of(55));
    }

    @Test
    void usesLatestKnownPatchAsAnchorForExplicitUnavailableFlex() {
        BuildSnapshot baseline = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, null, BuildScope.CHAMPION_ROLE, 80, PUBLISHED);
        when(snapshots.findPublishedForChampion(
                1, BuildQueue.SOLO_DUO, 22)).thenReturn(List.of(baseline));
        when(snapshots.findPublishedForChampion(
                1, BuildQueue.FLEX, 22)).thenReturn(List.of());

        ChampionBuildOptionsResponse response = service.options(
                22, 440, null, BuildRole.BOTTOM);

        assertThat(response.defaults()).isEqualTo(
                new RequestedFilters(440, "16.13", BuildRole.BOTTOM, null));
        assertThat(response.patches()).containsExactly(new PatchOption("16.13"));
        assertThat(response.roles()).noneMatch(RoleOption::available);
        assertThat(response.opponents()).isEmpty();
    }

    @Test
    void queueAvailabilityAndDefaultUseTheRequestedPatchAndRole() {
        BuildSnapshot soloBottom = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, null, BuildScope.CHAMPION_ROLE, 80, PUBLISHED);
        BuildSnapshot flexOldBottom = snapshot(new PatchWindow("16.12", "16.11"),
                BuildQueue.FLEX, BuildRole.BOTTOM, null,
                BuildScope.CHAMPION_ROLE, 60, PUBLISHED.minusDays(7));
        BuildSnapshot flexTop = snapshot(CURRENT, BuildQueue.FLEX,
                BuildRole.TOP, null, BuildScope.CHAMPION_ROLE, 40, PUBLISHED);
        when(snapshots.findPublishedForChampion(
                1, BuildQueue.SOLO_DUO, 22)).thenReturn(List.of(soloBottom));
        when(snapshots.findPublishedForChampion(
                1, BuildQueue.FLEX, 22)).thenReturn(List.of(flexOldBottom, flexTop));

        ChampionBuildOptionsResponse bottom = service.options(
                22, null, "16.13", BuildRole.BOTTOM);
        ChampionBuildOptionsResponse top = service.options(
                22, null, "16.13", BuildRole.TOP);

        assertThat(bottom.queues()).extracting(QueueOption::available).
                containsExactly(true, false);
        assertThat(bottom.defaults().queueId()).isEqualTo(420);
        assertThat(top.queues()).extracting(QueueOption::available).
                containsExactly(false, true);
        assertThat(top.defaults().queueId()).isEqualTo(440);
    }

    @Test
    void keepsRequestedOpponentWhenFallingBackToCurrentRoleBaseline() {
        BuildSnapshot belowThreshold = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, 55, BuildScope.EXACT_MATCHUP, 9, PUBLISHED);
        BuildSnapshot baseline = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, null, BuildScope.CHAMPION_ROLE, 80, PUBLISHED);
        when(snapshots.findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 55)).thenReturn(Optional.of(belowThreshold));
        when(snapshots.findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, null)).thenReturn(Optional.of(baseline));

        ChampionBuildResponse response = service.builds(
                22, 420, "16.13", BuildRole.BOTTOM, 55);

        assertThat(response.requested().opponentId()).isEqualTo(55);
        assertThat(response.resolved().opponentId()).isNull();
        assertThat(response.resultScope()).isEqualTo(BuildScope.CHAMPION_ROLE);
        assertThat(response.fallbackReason()).
                isEqualTo(BuildFallbackReason.MATCHUP_SAMPLE_TOO_SMALL);
    }

    @Test
    void returnsOnlyOneOfTheTwoHistoricalBaselinesRequestedFromTheRepository() {
        BuildSnapshot historical = snapshot(new PatchWindow("16.12", "16.11"),
                BuildQueue.SOLO_DUO, BuildRole.BOTTOM, null,
                BuildScope.CHAMPION_ROLE, 40, PUBLISHED.minusDays(14));
        when(snapshots.findHistoricalBaselines(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 2)).thenReturn(List.of(historical));

        ChampionBuildResponse response = service.builds(
                22, 420, "16.13", BuildRole.BOTTOM, 55);

        assertThat(response.historical()).isTrue();
        assertThat(response.resolved().anchorPatch()).isEqualTo("16.12");
        assertThat(response.fallbackReason()).
                isEqualTo(BuildFallbackReason.REQUESTED_PATCH_UNAVAILABLE);
        verify(snapshots).findHistoricalBaselines(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 2);
    }

    @Test
    void rejectsHistoricalBaselineOutsideAdjacentPatchLookback() {
        BuildSnapshot tooOld = snapshot(new PatchWindow("16.2", "16.1"),
                BuildQueue.SOLO_DUO, BuildRole.BOTTOM, null,
                BuildScope.CHAMPION_ROLE, 40, PUBLISHED.minusDays(90));
        when(snapshots.findHistoricalBaselines(1, "16.9", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 2)).thenReturn(List.of(tooOld));

        ChampionBuildResponse response = service.builds(
                22, 420, "16.9", BuildRole.BOTTOM, null);

        assertThat(response.available()).isFalse();
        assertThat(response.fallbackReason()).isEqualTo(BuildFallbackReason.DATA_UNAVAILABLE);
    }

    @Test
    void marksPublishedSnapshotStaleOnlyForANewerFailedExactRun() {
        BuildSnapshot baseline = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, null, BuildScope.CHAMPION_ROLE, 80, PUBLISHED);
        AggregationRun failed = run("FAILED", CURRENT, BuildQueue.SOLO_DUO,
                PUBLISHED.minusMinutes(10), PUBLISHED.plusHours(1));
        when(snapshots.findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, null)).thenReturn(Optional.of(baseline));
        when(snapshots.findLatestRun(1, CURRENT, BuildQueue.SOLO_DUO)).
                thenReturn(Optional.of(failed));

        ChampionBuildResponse response = service.builds(
                22, 420, "16.13", BuildRole.BOTTOM, null);

        assertThat(response.stale()).isTrue();
        assertThat(response.resultScope()).isEqualTo(BuildScope.CHAMPION_ROLE);
        assertThat(response.fallbackReason()).
                isEqualTo(BuildFallbackReason.AGGREGATION_FAILED_USING_LAST_PUBLISHED);
    }

    @Test
    void returnsStructuredUnavailableWithoutCrossingQueueOrRole() {
        ChampionBuildResponse response = service.builds(
                22, 440, "16.13", BuildRole.TOP, null);

        assertThat(response.available()).isFalse();
        verify(snapshots).findPublished(1, "16.13", BuildQueue.FLEX,
                22, BuildRole.TOP, null);
        verify(snapshots).findHistoricalBaselines(1, "16.13", BuildQueue.FLEX,
                22, BuildRole.TOP, 2);
        verify(snapshots, never()).findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.TOP, null);
    }

    @Test
    void rejectsInvalidBuildQueueBeforeAnyRepositoryAccessEvenForUnknownChampion() {
        assertThatThrownBy(() -> service.builds(
                999, 999, "16.13", BuildRole.TOP, null)).
                isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(assets, snapshots);
    }

    @Test
    void rejectsMalformedBuildPatchBeforeAnyRepositoryAccessEvenForUnknownChampion() {
        assertThatThrownBy(() -> service.builds(
                999, 420, "live", BuildRole.TOP, null)).
                isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(assets, snapshots);
    }

    @Test
    void rejectsMissingBuildRoleBeforeAnyRepositoryAccessEvenForUnknownChampion() {
        assertThatThrownBy(() -> service.builds(
                999, 420, "16.13", null, null)).
                isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(assets, snapshots);
    }

    @Test
    void rejectsInvalidOptionFiltersBeforeAnyRepositoryAccess() {
        assertThatThrownBy(() -> service.options(999, 999, null, null)).
                isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(assets, snapshots);
    }

    @Test
    void rejectsMalformedOptionPatchBeforeAnyRepositoryAccess() {
        assertThatThrownBy(() -> service.options(999, 420, "live", null)).
                isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(assets, snapshots);
    }

    @Test
    void unknownChampionIsNotFoundAfterValidFilterValidation() {
        assertThatThrownBy(() -> service.builds(
                999, 420, "16.13", BuildRole.TOP, null)).
                isInstanceOf(NotFoundException.class);

        verify(assets).findChampion(999);
        verifyNoInteractions(snapshots);
    }

    @Test
    void enrichesAllBuildGroupsWithOneBatchCallPerAssetType() {
        BuildSnapshotPayload payload = new BuildSnapshotPayload(
                List.of(choice(List.of(1055))),
                List.of(choice(List.of(3006))),
                List.of(choice(List.of(6672, 3031))),
                List.of(choice(List.of(3089))),
                List.of(choice(List.of(8000, 8005))),
                List.of(choice(List.of(4, 14))),
                List.of(choice(List.of(1, 2))),
                List.of(1, 2, 3));
        BuildSnapshot exact = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, 55, BuildScope.EXACT_MATCHUP,
                12, 7, PUBLISHED, payload);
        when(snapshots.findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 55)).thenReturn(Optional.of(exact));

        service.builds(22, 420, "16.13", BuildRole.BOTTOM, 55);

        verify(assets, times(1)).findItems(List.of(1055, 3006, 6672, 3031, 3089));
        verify(assets, times(1)).findRunes(List.of(8000, 8005));
        verify(assets, times(1)).findSpells(List.of(4, 14));
    }

    @Test
    void returnsBackendRoundedPercentageWinRate() {
        BuildSnapshot exact = snapshot(CURRENT, BuildQueue.SOLO_DUO,
                BuildRole.BOTTOM, 55, BuildScope.EXACT_MATCHUP,
                12, 7, PUBLISHED, payload(12, 7));
        when(snapshots.findPublished(1, "16.13", BuildQueue.SOLO_DUO,
                22, BuildRole.BOTTOM, 55)).thenReturn(Optional.of(exact));

        ChampionBuildResponse response = service.builds(
                22, 420, "16.13", BuildRole.BOTTOM, 55);

        assertThat(response.winRate()).isEqualTo(58.33);
        assertThat(response.build().startingItems().getFirst().pickRate()).isEqualTo(100.0);
        assertThat(response.build().startingItems().getFirst().winRate()).isEqualTo(58.33);
    }

    private BuildSnapshot snapshot(PatchWindow window, BuildQueue queue, BuildRole role,
                                   Integer opponent, BuildScope scope, int games,
                                   OffsetDateTime publishedAt) {
        return snapshot(window, queue, role, opponent, scope, games, games / 2,
                publishedAt, payload(games, games / 2));
    }

    private BuildSnapshot snapshot(
            PatchWindow window,
            BuildQueue queue,
            BuildRole role,
            Integer opponent,
            BuildScope scope,
            int games,
            int wins,
            OffsetDateTime publishedAt,
            BuildSnapshotPayload payload
    ) {
        return new BuildSnapshot(UUID.randomUUID(), UUID.randomUUID(), 1, 1,
                window, queue, 22, role, opponent, scope, games, wins,
                games, 0, BuildConfidence.LOW, publishedAt.minusHours(1), games,
                publishedAt.minusMinutes(5), publishedAt, "PUBLISHED", payload);
    }

    private BuildSnapshotPayload payload(int games, int wins) {
        return new BuildSnapshotPayload(
                List.of(new BuildChoice(List.of(1055), games, wins,
                        1.0, games == 0 ? 0.0 : (double) wins / games, games)),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(1, 2, 3));
    }

    private BuildChoice choice(List<Integer> ids) {
        return new BuildChoice(ids, 12, 7, 1.0, 0.58, 12);
    }

    private AggregationRun run(
            String state,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {
        return new AggregationRun(UUID.randomUUID(), 1, window, queue, startedAt,
                state, 0, 0, 0, "PUBLISH", startedAt, completedAt);
    }

    private BuildProperties properties() {
        return new BuildProperties(1, 1, 10, 25, 50, 0.7, 0.3, 2,
                Duration.ofMinutes(2), 2, Duration.ofHours(1), false);
    }
}
