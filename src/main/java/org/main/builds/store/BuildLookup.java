package org.main.builds.store;

import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.PatchWindow;

public record BuildLookup(
        int aggregationVersion,
        PatchWindow window,
        BuildQueue queue,
        int championId,
        BuildRole role,
        Integer opponentChampionId
) {
}
