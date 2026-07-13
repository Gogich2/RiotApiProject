package org.main.builds.model;

import java.util.List;

public record BuildSnapshotPayload(
        List<BuildChoice> startingItems,
        List<BuildChoice> boots,
        List<BuildChoice> coreItems,
        List<BuildChoice> situationalItems,
        List<BuildChoice> runePages,
        List<BuildChoice> spellPairs,
        List<BuildChoice> skillOrders,
        List<Integer> skillMaxPriority
) {

    public BuildSnapshotPayload {
        startingItems = List.copyOf(startingItems);
        boots = List.copyOf(boots);
        coreItems = List.copyOf(coreItems);
        situationalItems = List.copyOf(situationalItems);
        runePages = List.copyOf(runePages);
        spellPairs = List.copyOf(spellPairs);
        skillOrders = List.copyOf(skillOrders);
        skillMaxPriority = List.copyOf(skillMaxPriority);
    }
}
