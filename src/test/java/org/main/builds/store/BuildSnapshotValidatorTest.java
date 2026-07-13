package org.main.builds.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BaselineKey;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;

class BuildSnapshotValidatorTest {

    private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final PatchWindow WINDOW = new PatchWindow("16.13", "16.12");

    private static final OffsetDateTime WATERMARK = OffsetDateTime.parse("2026-07-01T12:00:00Z");

    private static final BaselineKey BASELINE = new BaselineKey(22, BuildRole.MIDDLE);

    private final BuildSnapshotValidator validator = new BuildSnapshotValidator(10);

    @Test
    void acceptsACompleteBaselineAndEmptySituationalItems() {
        AggregationResult result = result(List.of(baseline(payload())), Set.of(BASELINE), 12);

        assertThat(validator.validate(run(), RUN_ID, result, 1, WINDOW,
                BuildQueue.SOLO_DUO, WATERMARK)).isEqualTo(1);
    }

    @Test
    void rejectsEmptyResultsAndMissingExpectedBaselines() {
        assertInvalid(result(List.of(), Set.of(), 0), "empty");
        assertInvalid(result(List.of(exact(12, payload())), Set.of(BASELINE), 12), "baseline");
    }

    @Test
    void rejectsDuplicateCohortKeysAndExactCohortsBelowTheThreshold() {
        AggregatedCohort baseline = baseline(payload());
        assertInvalid(result(List.of(baseline, baseline), Set.of(BASELINE), 24), "duplicate");
        assertInvalid(result(List.of(baseline, exact(9, payload())), Set.of(BASELINE), 21),
                "threshold");
    }

    @Test
    void rejectsRunVersionWindowQueueAndWatermarkMismatches() {
        AggregationResult result = validResult();
        assertThatThrownBy(() -> validator.validate(run(), UUID.randomUUID(), result, 1,
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK)).hasMessageContaining("run");
        assertThatThrownBy(() -> validator.validate(run(), RUN_ID, result, 2,
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK)).hasMessageContaining("version");
        assertThatThrownBy(() -> validator.validate(run(), RUN_ID, result, 1,
                new PatchWindow("16.14", "16.13"), BuildQueue.SOLO_DUO, WATERMARK)).
                hasMessageContaining("window");
        assertThatThrownBy(() -> validator.validate(run(), RUN_ID, result, 1,
                WINDOW, BuildQueue.FLEX, WATERMARK)).hasMessageContaining("queue");
        assertThatThrownBy(() -> validator.validate(run(), RUN_ID, result, 1,
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusSeconds(1))).
                hasMessageContaining("watermark");
    }

    @Test
    void rejectsNegativeCountsWinsAboveGamesAndBrokenPatchTotals() {
        assertInvalid(cohort(-1, 0, -1, 0, BuildScope.CHAMPION_ROLE, null, payload()), "negative");
        assertInvalid(cohort(12, 13, 7, 5, BuildScope.CHAMPION_ROLE, null, payload()), "wins");
        assertInvalid(cohort(12, 6, 7, 4, BuildScope.CHAMPION_ROLE, null, payload()), "patch");
        assertInvalid(new AggregationResult(List.of(baseline(payload())), Set.of(BASELINE), -1),
                "negative");
    }

    @Test
    void rejectsMissingPrimaryPayloadComponentsAndUnusableEvidence() {
        BuildSnapshotPayload complete = payload();
        for (int missing = 0; missing < 6; missing++) {
            List<List<BuildChoice>> components = new java.util.ArrayList<>(List.of(
                    complete.startingItems(), complete.boots(), complete.coreItems(),
                    complete.runePages(), complete.spellPairs(), complete.skillOrders()));
            components.set(missing, List.of());
            BuildSnapshotPayload incomplete = new BuildSnapshotPayload(
                    components.get(0), components.get(1), components.get(2), List.of(),
                    components.get(3), components.get(4), components.get(5),
                    complete.skillMaxPriority());
            assertInvalid(baseline(incomplete), "payload");
        }
        BuildSnapshotPayload noPriority = new BuildSnapshotPayload(
                complete.startingItems(), complete.boots(), complete.coreItems(), List.of(),
                complete.runePages(), complete.spellPairs(), complete.skillOrders(), List.of());
        assertInvalid(baseline(noPriority), "payload");

        BuildChoice noEvidence = new BuildChoice(List.of(1055), 0, 0, 0, 0, 0);
        BuildSnapshotPayload unusable = new BuildSnapshotPayload(
                List.of(noEvidence), complete.boots(), complete.coreItems(), List.of(),
                complete.runePages(), complete.spellPairs(), complete.skillOrders(),
                complete.skillMaxPriority());
        assertInvalid(baseline(unusable), "evidence");
    }

    @Test
    void rejectsUnsupportedChampionOpponentPayloadAndSkillIds() {
        assertInvalid(cohort(12, 6, 7, 5, BuildScope.CHAMPION_ROLE, null, payload(), 0), "champion");
        assertInvalid(cohort(12, 6, 7, 5, BuildScope.EXACT_MATCHUP, 0, payload()), "opponent");
        assertInvalid(baseline(payloadWithStartingIds(List.of(0))), "id");
        BuildSnapshotPayload complete = payload();
        BuildSnapshotPayload badSkills = new BuildSnapshotPayload(
                complete.startingItems(), complete.boots(), complete.coreItems(), List.of(),
                complete.runePages(), complete.spellPairs(),
                List.of(choice(List.of(5))), complete.skillMaxPriority());
        assertInvalid(baseline(badSkills), "skill");
    }

    private void assertInvalid(AggregationResult result, String message) {
        assertThatThrownBy(() -> validator.validate(run(), RUN_ID, result, 1, WINDOW,
                BuildQueue.SOLO_DUO, WATERMARK)).isInstanceOf(IllegalArgumentException.class).
                hasMessageMatching("(?i).*" + message + ".*");
    }

    private void assertInvalid(AggregatedCohort cohort, String message) {
        assertInvalid(result(List.of(cohort), Set.of(BASELINE), Math.max(0, cohort.games())), message);
    }

    private AggregationResult validResult() {
        return result(List.of(baseline(payload())), Set.of(BASELINE), 12);
    }

    private AggregationResult result(List<AggregatedCohort> cohorts,
                                     Set<BaselineKey> expected, int sourceCount) {
        return new AggregationResult(cohorts, expected, sourceCount);
    }

    private AggregationRun run() {
        return new AggregationRun(RUN_ID, 1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK,
                "RUNNING", 0, 0, 0, null,
                OffsetDateTime.parse("2026-07-01T12:01:00Z"), null);
    }

    private AggregatedCohort baseline(BuildSnapshotPayload payload) {
        return cohort(12, 6, 7, 5, BuildScope.CHAMPION_ROLE, null, payload);
    }

    private AggregatedCohort exact(int games, BuildSnapshotPayload payload) {
        return cohort(games, games / 2, games, 0, BuildScope.EXACT_MATCHUP, 55, payload);
    }

    private AggregatedCohort cohort(int games, int wins, int anchorGames,
                                    int comparisonGames, BuildScope scope,
                                    Integer opponentId, BuildSnapshotPayload payload) {
        return cohort(games, wins, anchorGames, comparisonGames, scope, opponentId, payload, 22);
    }

    private AggregatedCohort cohort(int games, int wins, int anchorGames,
                                    int comparisonGames, BuildScope scope,
                                    Integer opponentId, BuildSnapshotPayload payload,
                                    int championId) {
        return new AggregatedCohort(championId, BuildRole.MIDDLE, opponentId, scope,
                games, wins, anchorGames, comparisonGames, BuildConfidence.LOW, payload);
    }

    private BuildSnapshotPayload payloadWithStartingIds(List<Integer> ids) {
        BuildSnapshotPayload complete = payload();
        return new BuildSnapshotPayload(List.of(choice(ids)), complete.boots(),
                complete.coreItems(), List.of(), complete.runePages(), complete.spellPairs(),
                complete.skillOrders(), complete.skillMaxPriority());
    }

    private BuildSnapshotPayload payload() {
        return new BuildSnapshotPayload(
                List.of(choice(List.of(1055))),
                List.of(choice(List.of(3006))),
                List.of(choice(List.of(6672, 3031))),
                List.of(),
                List.of(choice(List.of(8000, 8005, 9111, 9104, 8014,
                        8300, 8304, 8347, 5005, 5008, 5002))),
                List.of(choice(List.of(4, 14))),
                List.of(choice(List.of(1, 2, 3, 1, 1, 4))),
                List.of(1, 2, 3));
    }

    private BuildChoice choice(List<Integer> ids) {
        return new BuildChoice(ids, 12, 6, 1.0, 0.5, 8.4);
    }
}
