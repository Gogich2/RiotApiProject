package org.main.builds.model;

import java.util.List;

public record BuildChoice(
        List<Integer> ids,
        int games,
        int wins,
        double pickRate,
        double winRate,
        double weightedScore
) {

    public BuildChoice {
        ids = List.copyOf(ids);
    }
}
