package org.main.builds.source;

import java.time.OffsetDateTime;
import java.util.List;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;

public record BuildSourceSelection(
        PatchWindow window,
        BuildQueue queue,
        OffsetDateTime inputWatermark,
        List<String> matchIds
) {

    public BuildSourceSelection {
        matchIds = List.copyOf(matchIds);
    }
}
