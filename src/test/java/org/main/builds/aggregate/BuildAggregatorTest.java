package org.main.builds.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.main.builds.BuildConfiguration;
import org.main.builds.BuildProperties;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BaselineKey;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.ItemPath;
import org.main.builds.model.PatchWindow;
import org.main.builds.model.RunePage;
import org.main.builds.model.SkillPath;

class BuildAggregatorTest {

    private static final PatchWindow WINDOW = new PatchWindow("16.2", "16.1");

    private static final RunePage RUNES = new RunePage(
            8000, List.of(8005, 9111, 9104, 8014),
            8300, List.of(8304, 8347), List.of(5005, 5008, 5002));

    private static final List<Integer> COMPLETE_SKILLS = List.of(
            1, 2, 3, 1, 1, 4, 1, 2, 1, 2, 4, 2, 2, 3, 3, 4, 3, 3);

    private static final List<Integer> INCOMPLETE_SKILLS = List.of(2, 1, 3, 2, 4, 2);

    private final BuildAggregator aggregator = new BuildAggregator(
            new BuildConfiguration().buildRules(properties()));

    @Test
    void patchWeightingChangesTheWinnerAndRatesUseWeightedCohortDenominators() {
        List<BuildObservation> observations = new ArrayList<>();
        observations.addAll(observations(6, 5, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 2, new ItemPath(List.of(1055), 3006, List.of(100))));
        observations.addAll(observations(10, 4, "16.1", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 2, new ItemPath(List.of(2003), 3009, List.of(200))));

        AggregatedCohort baseline = baseline(aggregator.aggregate(
                WINDOW, BuildQueue.SOLO_DUO, observations), 1, BuildRole.TOP);

        assertThat(baseline.games()).isEqualTo(16);
        assertThat(baseline.wins()).isEqualTo(9);
        assertThat(baseline.anchorGames()).isEqualTo(6);
        assertThat(baseline.comparisonGames()).isEqualTo(10);
        assertThat(baseline.payload().coreItems()).extracting(BuildChoice::ids).
                containsExactly(List.of(100), List.of(200));
        BuildChoice weightedWinner = baseline.payload().coreItems().getFirst();
        assertThat(weightedWinner.games()).isEqualTo(6);
        assertThat(weightedWinner.wins()).isEqualTo(5);
        assertThat(weightedWinner.weightedScore()).isCloseTo(4.2, within(0.000000001));
        assertThat(weightedWinner.pickRate()).isCloseTo(7.0 / 12.0, within(0.000000001));
        assertThat(weightedWinner.winRate()).isCloseTo(5.0 / 6.0, within(0.000000001));
    }

    @Test
    void exactCohortsStartAtTenRawGamesWithoutCrossingQueueOrRole() {
        List<BuildObservation> observations = new ArrayList<>();
        observations.addAll(observations(9, 5, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 2, items(100)));
        observations.addAll(observations(10, 5, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 3, items(100)));
        observations.addAll(observations(7, 4, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.JUNGLE, 3, items(200)));
        observations.addAll(observations(20, 10, "16.2", BuildQueue.FLEX,
                1, BuildRole.TOP, 3, items(300)));

        AggregationResult result = aggregator.aggregate(WINDOW, BuildQueue.SOLO_DUO, observations);

        assertThat(result.sourceObservationCount()).isEqualTo(26);
        assertThat(result.expectedBaselines()).containsExactlyInAnyOrder(
                new BaselineKey(1, BuildRole.TOP), new BaselineKey(1, BuildRole.JUNGLE));
        assertThat(baseline(result, 1, BuildRole.TOP).games()).isEqualTo(19);
        assertThat(baseline(result, 1, BuildRole.JUNGLE).games()).isEqualTo(7);
        assertThat(result.cohorts().stream().
                filter(cohort -> cohort.scope() == BuildScope.EXACT_MATCHUP)).
                singleElement().
                satisfies(cohort -> {
                    assertThat(cohort.role()).isEqualTo(BuildRole.TOP);
                    assertThat(cohort.opponentChampionId()).isEqualTo(3);
                    assertThat(cohort.games()).isEqualTo(10);
                });
    }

    @Test
    void situationalItemsNeedTenCandidateGamesInsideTheLargerCohort() {
        List<BuildObservation> observations = new ArrayList<>();
        observations.addAll(observations(15, 8, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 2, items(100, 200)));
        observations.addAll(observations(10, 5, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 2, items(100, 300)));
        observations.addAll(observations(9, 5, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, 2, items(100, 400)));

        AggregatedCohort baseline = baseline(aggregator.aggregate(
                WINDOW, BuildQueue.SOLO_DUO, observations), 1, BuildRole.TOP);

        assertThat(baseline.games()).isEqualTo(34);
        assertThat(baseline.payload().coreItems().getFirst().ids()).containsExactly(100, 200);
        assertThat(baseline.payload().situationalItems()).
                extracting(BuildChoice::ids).
                containsExactly(List.of(300));
        assertThat(baseline.payload().situationalItems().getFirst().games()).isEqualTo(10);
    }

    @Test
    void exactTiesUseStableLexicographicIdsAcrossRepeatedInputOrders() {
        List<BuildObservation> firstOrder = new ArrayList<>();
        firstOrder.addAll(observations(5, 3, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, null, items(2, 1)));
        firstOrder.addAll(observations(5, 3, "16.2", BuildQueue.SOLO_DUO,
                1, BuildRole.TOP, null, items(1, 2)));
        List<BuildObservation> reversed = firstOrder.reversed();

        for (List<BuildObservation> input : List.of(firstOrder, reversed, firstOrder, reversed)) {
            assertThat(baseline(aggregator.aggregate(
                    WINDOW, BuildQueue.SOLO_DUO, input), 1, BuildRole.TOP).
                    payload().coreItems()).extracting(BuildChoice::ids).
                    containsExactly(List.of(1, 2), List.of(2, 1));
        }
    }

    @Test
    void emitsEveryCompleteComponentAndDerivesCompleteOnlySkillMaxPriority() {
        ItemPath primary = new ItemPath(List.of(1055, 2003), 3006, List.of(6672, 3031));
        ItemPath alternative = new ItemPath(List.of(1055, 2003), 3006, List.of(6672, 3094));
        List<BuildObservation> observations = new ArrayList<>();
        observations.addAll(observations(10, 6, "16.2", BuildQueue.SOLO_DUO,
                22, BuildRole.MIDDLE, 55, primary, COMPLETE_SKILLS, List.of(14, 4)));
        observations.addAll(observations(10, 4, "16.2", BuildQueue.SOLO_DUO,
                22, BuildRole.MIDDLE, 55, alternative, COMPLETE_SKILLS, List.of(14, 4)));
        observations.add(observation("16.2", BuildQueue.SOLO_DUO, 22, BuildRole.MIDDLE,
                55, false, new ItemPath(List.of(), null, List.of()),
                INCOMPLETE_SKILLS, List.of(14, 4), "empty"));

        AggregatedCohort baseline = baseline(aggregator.aggregate(
                WINDOW, BuildQueue.SOLO_DUO, observations), 22, BuildRole.MIDDLE);

        assertThat(baseline.games()).isEqualTo(21);
        assertThat(baseline.payload().startingItems()).extracting(BuildChoice::ids).
                containsExactly(List.of(1055, 2003));
        assertThat(baseline.payload().startingItems().getFirst().games()).isEqualTo(20);
        assertThat(baseline.payload().boots()).extracting(BuildChoice::ids).
                containsExactly(List.of(3006));
        assertThat(baseline.payload().coreItems()).extracting(BuildChoice::ids).
                containsExactly(List.of(6672, 3031), List.of(6672, 3094));
        assertThat(baseline.payload().situationalItems()).extracting(BuildChoice::ids).
                containsExactly(List.of(3094));
        assertThat(baseline.payload().runePages()).extracting(BuildChoice::ids).
                containsExactly(List.of(8000, 8005, 9111, 9104, 8014,
                        8300, 8304, 8347, 5005, 5008, 5002));
        assertThat(baseline.payload().spellPairs()).extracting(BuildChoice::ids).
                containsExactly(List.of(4, 14));
        assertThat(baseline.payload().skillOrders()).extracting(BuildChoice::ids).
                containsExactly(COMPLETE_SKILLS, INCOMPLETE_SKILLS);
        assertThat(baseline.payload().skillMaxPriority()).containsExactly(1, 2, 3);
    }

    private AggregatedCohort baseline(AggregationResult result, int championId, BuildRole role) {
        return result.cohorts().stream().
                filter(cohort -> cohort.scope() == BuildScope.CHAMPION_ROLE).
                filter(cohort -> cohort.championId() == championId && cohort.role() == role).
                findFirst().
                orElseThrow();
    }

    private List<BuildObservation> observations(
            int games, int wins, String patch, BuildQueue queue, int championId,
            BuildRole role, Integer opponentId, ItemPath items
    ) {
        return observations(games, wins, patch, queue, championId, role,
                opponentId, items, COMPLETE_SKILLS, List.of(4, 14));
    }

    private List<BuildObservation> observations(
            int games, int wins, String patch, BuildQueue queue, int championId,
            BuildRole role, Integer opponentId, ItemPath items,
            List<Integer> skills, List<Integer> spells
    ) {
        return IntStream.range(0, games).
                mapToObj(index -> observation(patch, queue, championId, role, opponentId,
                        index < wins, items, skills, spells, patch + "-" + championId + "-"
                                + role + "-" + opponentId + "-" + index + "-" + games)).
                toList();
    }

    private BuildObservation observation(
            String patch, BuildQueue queue, int championId, BuildRole role,
            Integer opponentId, boolean win, ItemPath items, List<Integer> skills,
            List<Integer> spells, String matchId
    ) {
        return new BuildObservation(matchId, patch, queue, championId, role,
                opponentId, win, items, RUNES, spells, new SkillPath(skills));
    }

    private ItemPath items(Integer... ids) {
        return new ItemPath(List.of(1055), 3006, List.of(ids));
    }

    private static BuildProperties properties() {
        return new BuildProperties(1, 1, 10, 25, 50, 0.7, 0.3, 2,
                Duration.ofMinutes(2), 250, Duration.ofHours(1), false);
    }
}
