package org.main.builds.api;

import java.util.List;

public record DisplayBuildChoice(
        List<DisplayAsset> assets,
        int games,
        int wins,
        double pickRate,
        double winRate
) {
}
