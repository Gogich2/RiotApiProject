package org.main.builds.api;

import java.util.List;

public record ChampionBuildOptionsResponse(
        int championId,
        List<QueueOption> queues,
        List<PatchOption> patches,
        List<RoleOption> roles,
        List<OpponentOption> opponents,
        RequestedFilters defaults
) {
}
