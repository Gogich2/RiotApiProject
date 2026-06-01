package org.main.dto.frontend;

import java.util.List;

public record PlayerLeaderboardResponseDto(
        List<PlayerLeaderboardDto> bestPlayers,
        List<PlayerLeaderboardDto> mostActivePlayers
) {
}
