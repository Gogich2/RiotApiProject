package org.main.builds.model;

public record BuildCandidate<T extends Comparable<? super T>>(
        T value,
        int anchorPicks,
        int comparisonPicks,
        int anchorWins,
        int comparisonWins
) {
}
