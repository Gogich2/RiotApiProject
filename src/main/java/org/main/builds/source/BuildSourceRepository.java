package org.main.builds.source;

import java.util.List;
import java.util.Optional;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;

public interface BuildSourceRepository {

    Optional<String> findLatestPatch(BuildQueue queue);

    Optional<String> findPreviousMajorLastPatch(BuildQueue queue, int previousMajor);

    BuildSourceSelection selectSource(PatchWindow window, BuildQueue queue);

    List<BuildSourceMatch> loadBatch(List<String> selectedMatchIds);
}
