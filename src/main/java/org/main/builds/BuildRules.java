package org.main.builds;

import org.main.builds.model.BuildCandidate;
import org.main.builds.model.BuildConfidence;

import java.util.Comparator;
import java.util.List;

public final class BuildRules {

    private final BuildProperties properties;

    BuildRules(BuildProperties properties) {
        this.properties = properties;
    }

    public BuildConfidence confidence(int games) {
        if (games < properties.matchupMinGames()) {
            return BuildConfidence.INSUFFICIENT;
        }
        if (games < properties.mediumConfidenceGames()) {
            return BuildConfidence.LOW;
        }
        if (games < properties.highConfidenceGames()) {
            return BuildConfidence.MEDIUM;
        }
        return BuildConfidence.HIGH;
    }

    public double weightedPickScore(BuildCandidate<?> candidate) {
        return candidate.anchorPicks() * properties.anchorPatchWeight()
                + candidate.comparisonPicks() * properties.comparisonPatchWeight();
    }

    public double weightedWinRate(BuildCandidate<?> candidate) {
        double weightedPicks = weightedPickScore(candidate);
        if (weightedPicks == 0) {
            return 0;
        }
        return (candidate.anchorWins() * properties.anchorPatchWeight()
                + candidate.comparisonWins() * properties.comparisonPatchWeight()) / weightedPicks;
    }

    public boolean exactMatchupEligible(int games) {
        return games >= properties.matchupMinGames();
    }

    public <T extends Comparable<? super T>> List<BuildCandidate<T>> rank(
            List<BuildCandidate<T>> candidates
    ) {
        Comparator<BuildCandidate<T>> ranking = Comparator.
                <BuildCandidate<T>>comparingDouble(this::weightedPickScore).reversed().
                thenComparing(Comparator.comparingDouble(this::weightedWinRate).reversed()).
                thenComparing(BuildCandidate::value);
        return candidates.stream().sorted(ranking).toList();
    }
}
