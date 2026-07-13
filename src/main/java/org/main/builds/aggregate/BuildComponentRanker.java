package org.main.builds.aggregate;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.main.builds.BuildRules;
import org.main.builds.model.BuildCandidate;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildObservation;
import org.main.builds.model.PatchWindow;

final class BuildComponentRanker {

    private final BuildRules rules;

    private final PatchWindow window;

    BuildComponentRanker(BuildRules rules, PatchWindow window) {
        this.rules = rules;
        this.window = window;
    }

    List<BuildChoice> rank(
            List<BuildObservation> observations,
            Function<BuildObservation, List<List<Integer>>> extractor,
            int minimumGames
    ) {
        Map<Ids, Counts> counts = new HashMap<>();
        for (BuildObservation observation : observations) {
            for (List<Integer> ids : new LinkedHashSet<>(extractor.apply(observation))) {
                if (!ids.isEmpty()) {
                    counts.computeIfAbsent(new Ids(ids), Counts::new).add(
                            observation.patch().equals(window.anchorPatch()), observation.win());
                }
            }
        }

        double denominator = rules.weightedPickScore(observationCounts(observations).candidate());
        List<BuildCandidate<Ids>> candidates = counts.values().stream().
                filter(candidate -> candidate.games() >= minimumGames).
                map(Counts::candidate).
                toList();
        return rules.rank(candidates).stream().
                map(candidate -> new BuildChoice(candidate.value().values(),
                        candidate.anchorPicks() + candidate.comparisonPicks(),
                        candidate.anchorWins() + candidate.comparisonWins(),
                        denominator == 0 ? 0
                                : rules.weightedPickScore(candidate) / denominator,
                        rules.weightedWinRate(candidate),
                        rules.weightedPickScore(candidate))).
                toList();
    }

    private Counts observationCounts(List<BuildObservation> observations) {
        Counts counts = new Counts(new Ids(List.of()));
        for (BuildObservation observation : observations) {
            counts.add(observation.patch().equals(window.anchorPatch()), observation.win());
        }
        return counts;
    }

    private record Ids(List<Integer> values) implements Comparable<Ids> {

        private Ids {
            values = List.copyOf(values);
        }

        @Override
        public int compareTo(Ids other) {
            for (int index = 0; index < Math.min(values.size(), other.values.size()); index++) {
                int comparison = Integer.compare(values.get(index), other.values.get(index));
                if (comparison != 0) {
                    return comparison;
                }
            }
            return Integer.compare(values.size(), other.values.size());
        }
    }

    private static final class Counts {

        private final Ids ids;

        private int anchorPicks;

        private int comparisonPicks;

        private int anchorWins;

        private int comparisonWins;

        private Counts(Ids ids) {
            this.ids = ids;
        }

        private void add(boolean anchor, boolean win) {
            if (anchor) {
                anchorPicks++;
                anchorWins += win ? 1 : 0;
            } else {
                comparisonPicks++;
                comparisonWins += win ? 1 : 0;
            }
        }

        private int games() {
            return anchorPicks + comparisonPicks;
        }

        private int wins() {
            return anchorWins + comparisonWins;
        }

        private BuildCandidate<Ids> candidate() {
            return new BuildCandidate<>(ids, anchorPicks, comparisonPicks,
                    anchorWins, comparisonWins);
        }
    }
}
