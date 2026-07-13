package org.main.builds.api;

import org.main.builds.model.BuildRole;

public record ResolvedFilters(
        int queueId,
        String anchorPatch,
        String comparisonPatch,
        BuildRole role,
        Integer opponentId
) {
}
