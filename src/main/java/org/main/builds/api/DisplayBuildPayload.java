package org.main.builds.api;

import java.util.List;

public record DisplayBuildPayload(
        List<DisplayBuildChoice> startingItems,
        List<DisplayBuildChoice> boots,
        List<DisplayBuildChoice> coreItems,
        List<DisplayBuildChoice> situationalItems,
        List<DisplayBuildChoice> runePages,
        List<DisplayBuildChoice> spellPairs,
        List<DisplayBuildChoice> skillOrders,
        List<Integer> skillMaxPriority
) {

    static DisplayBuildPayload empty() {
        return new DisplayBuildPayload(List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of());
    }
}
