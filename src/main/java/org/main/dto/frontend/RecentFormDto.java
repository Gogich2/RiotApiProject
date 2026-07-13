package org.main.dto.frontend;

public record RecentFormDto(
        int window,
        int games,
        int wins,
        int losses,
        double winRate,
        double averageKda
) {
}
