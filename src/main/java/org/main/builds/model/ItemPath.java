package org.main.builds.model;

import java.util.List;

public record ItemPath(List<Integer> startingItems, Integer boots, List<Integer> coreItems) {

    public ItemPath {
        startingItems = List.copyOf(startingItems);
        coreItems = List.copyOf(coreItems);
    }
}
