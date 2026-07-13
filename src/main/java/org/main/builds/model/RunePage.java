package org.main.builds.model;

import java.util.List;

public record RunePage(
        int primaryStyleId,
        List<Integer> primarySelections,
        int secondaryStyleId,
        List<Integer> secondarySelections,
        List<Integer> statShards
) {

    public RunePage {
        primarySelections = List.copyOf(primarySelections);
        secondarySelections = List.copyOf(secondarySelections);
        statShards = List.copyOf(statShards);
    }
}
