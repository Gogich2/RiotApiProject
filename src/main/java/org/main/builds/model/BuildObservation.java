package org.main.builds.model;

import java.util.List;

public record BuildObservation(
        String matchId,
        String patch,
        BuildQueue queue,
        int championId,
        BuildRole role,
        Integer opponentChampionId,
        boolean win,
        ItemPath items,
        RunePage runes,
        List<Integer> spells,
        SkillPath skills
) {

    public BuildObservation {
        spells = List.copyOf(spells);
    }
}
