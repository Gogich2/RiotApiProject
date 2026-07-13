package org.main.builds.api;

import org.main.builds.model.BuildRole;

public record RequestedFilters(
        int queueId, String patch, BuildRole role, Integer opponentId) {
}
