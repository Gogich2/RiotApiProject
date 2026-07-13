package org.main.builds.store;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BaselineKey;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;

public final class BuildSnapshotValidator {

    private final int matchupMinGames;

    public BuildSnapshotValidator(int matchupMinGames) {
        if (matchupMinGames < 0) {
            throw new IllegalArgumentException("Matchup threshold cannot be negative");
        }
        this.matchupMinGames = matchupMinGames;
    }

    public int validate(
            AggregationRun run,
            UUID runId,
            AggregationResult result,
            int aggregationVersion,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark
    ) {
        validateRun(run, runId, aggregationVersion, window, queue, watermark);
        if (result.sourceObservationCount() < 0) {
            throw new IllegalArgumentException("Negative source count");
        }
        if (result.cohorts().isEmpty()) {
            throw new IllegalArgumentException("Aggregation result is empty");
        }

        Set<CohortKey> keys = new HashSet<>();
        Set<BaselineKey> baselines = new HashSet<>();
        for (AggregatedCohort cohort : result.cohorts()) {
            validateCohort(cohort);
            if (!keys.add(new CohortKey(cohort.championId(), cohort.role(),
                    cohort.opponentChampionId()))) {
                throw new IllegalArgumentException("Duplicate cohort key");
            }
            if (cohort.scope() == BuildScope.CHAMPION_ROLE) {
                baselines.add(new BaselineKey(cohort.championId(), cohort.role()));
            }
        }
        if (!baselines.containsAll(result.expectedBaselines())) {
            throw new IllegalArgumentException("Missing expected baseline");
        }
        return result.cohorts().size();
    }

    private void validateRun(
            AggregationRun run,
            UUID runId,
            int aggregationVersion,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark
    ) {
        if (!run.id().equals(runId) || !"RUNNING".equals(run.state())) {
            throw new IllegalArgumentException("Mismatched or non-running run");
        }
        if (run.aggregationVersion() != aggregationVersion) {
            throw new IllegalArgumentException("Mismatched aggregation version");
        }
        if (!run.window().equals(window)) {
            throw new IllegalArgumentException("Mismatched patch window");
        }
        if (run.queue() != queue) {
            throw new IllegalArgumentException("Mismatched queue");
        }
        if (!run.inputWatermark().isEqual(watermark)) {
            throw new IllegalArgumentException("Mismatched watermark");
        }
    }

    private void validateCohort(AggregatedCohort cohort) {
        if (cohort.championId() <= 0) {
            throw new IllegalArgumentException("Unsupported champion id");
        }
        if (cohort.scope() == BuildScope.CHAMPION_ROLE
                && cohort.opponentChampionId() != null) {
            throw new IllegalArgumentException("Baseline cannot have an opponent id");
        }
        if (cohort.scope() == BuildScope.EXACT_MATCHUP
                && (cohort.opponentChampionId() == null || cohort.opponentChampionId() <= 0)) {
            throw new IllegalArgumentException("Unsupported opponent id");
        }
        if (cohort.games() < 0 || cohort.wins() < 0 || cohort.anchorGames() < 0
                || cohort.comparisonGames() < 0) {
            throw new IllegalArgumentException("Negative cohort count");
        }
        if (cohort.wins() > cohort.games()) {
            throw new IllegalArgumentException("Cohort wins exceed games");
        }
        if (cohort.anchorGames() + cohort.comparisonGames() != cohort.games()) {
            throw new IllegalArgumentException("Patch game counts do not equal games");
        }
        if (cohort.scope() == BuildScope.EXACT_MATCHUP
                && cohort.games() < matchupMinGames) {
            throw new IllegalArgumentException("Exact cohort is below matchup threshold");
        }
        validatePayload(cohort.payload());
    }

    private void validatePayload(BuildSnapshotPayload payload) {
        if (payload == null
                || payload.startingItems().isEmpty()
                || payload.boots().isEmpty()
                || payload.coreItems().isEmpty()
                || payload.runePages().isEmpty()
                || payload.spellPairs().isEmpty()
                || payload.skillOrders().isEmpty()
                || payload.skillMaxPriority().isEmpty()) {
            throw new IllegalArgumentException("Missing required payload component");
        }
        validateChoices(payload.startingItems(), false);
        validateChoices(payload.boots(), false);
        validateChoices(payload.coreItems(), false);
        validateChoices(payload.situationalItems(), false);
        validateChoices(payload.runePages(), false);
        validateChoices(payload.spellPairs(), false);
        validateChoices(payload.skillOrders(), true);
        if (payload.skillMaxPriority().stream().anyMatch(id -> id < 1 || id > 4)
                || new HashSet<>(payload.skillMaxPriority()).size()
                != payload.skillMaxPriority().size()) {
            throw new IllegalArgumentException("Unsupported skill priority id");
        }
    }

    private void validateChoices(List<BuildChoice> choices, boolean skillIds) {
        for (BuildChoice choice : choices) {
            if (choice.ids().isEmpty()
                    || choice.ids().stream().anyMatch(id -> id == null || id <= 0)) {
                throw new IllegalArgumentException("Unsupported payload id");
            }
            if (skillIds && choice.ids().stream().anyMatch(id -> id > 4)) {
                throw new IllegalArgumentException("Unsupported skill id");
            }
            if (choice.games() <= 0 || choice.wins() < 0 || choice.wins() > choice.games()
                    || !rate(choice.pickRate()) || !rate(choice.winRate())
                    || !Double.isFinite(choice.weightedScore()) || choice.weightedScore() <= 0) {
                throw new IllegalArgumentException("Unusable payload evidence");
            }
        }
    }

    private boolean rate(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 1;
    }

    private record CohortKey(int championId, BuildRole role, Integer opponentChampionId) {
    }
}
