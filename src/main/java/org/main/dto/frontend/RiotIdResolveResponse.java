package org.main.dto.frontend;

import org.main.refresh.dto.PlayerRefreshStatusDto;

public record RiotIdResolveResponse(
        String puuid,
        String gameName,
        String tagLine,
        PlayerRefreshStatusDto refresh
) {
}
