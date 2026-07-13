package org.main.builds.extract;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.main.builds.model.ItemPath;
import org.main.builds.source.ItemCatalog;

public final class ItemSequenceExtractor {

    private final long cutoffMillis;

    public ItemSequenceExtractor(Duration startingItemsCutoff) {
        cutoffMillis = startingItemsCutoff.toMillis();
    }

    public ItemPath extract(JsonNode timeline, int participantId,
                            Set<Integer> finalItemIds, ItemCatalog catalog) {
        List<Acquisition> inventory = new ArrayList<>();
        List<Acquisition> earlyInventory = new ArrayList<>();
        int ordinal = 0;

        for (JsonNode frame : timeline.path("info").path("frames")) {
            for (JsonNode event : frame.path("events")) {
                if (event.path("participantId").asInt(-1) != participantId) {
                    continue;
                }
                long timestamp = event.path("timestamp").asLong(Long.MAX_VALUE);
                ordinal = apply(event, inventory, ordinal);
                if (timestamp <= cutoffMillis) {
                    apply(event, earlyInventory, ordinal - 1);
                }
            }
        }

        List<Integer> starting = earlyInventory.stream().
                sorted(Comparator.comparingInt(Acquisition::ordinal)).
                map(Acquisition::itemId).
                filter(catalog::isStartingItem).
                toList();
        Integer boots = inventory.stream().
                sorted(Comparator.comparingInt(Acquisition::ordinal)).
                map(Acquisition::itemId).
                filter(finalItemIds::contains).
                filter(catalog::isCompletedBoot).
                findFirst().
                orElse(null);
        List<Integer> core = inventory.stream().
                sorted(Comparator.comparingInt(Acquisition::ordinal)).
                map(Acquisition::itemId).
                filter(finalItemIds::contains).
                filter(catalog::isCompletedCoreItem).
                distinct().
                limit(3).
                toList();
        return new ItemPath(starting, boots, core);
    }

    private int apply(JsonNode event, List<Acquisition> inventory, int ordinal) {
        return switch (event.path("type").asText()) {
            case "ITEM_PURCHASED" -> {
                add(inventory, event.path("itemId").asInt(), ordinal);
                yield ordinal + 1;
            }
            case "ITEM_SOLD", "ITEM_DESTROYED" -> {
                removeLast(inventory, event.path("itemId").asInt());
                yield ordinal;
            }
            case "ITEM_UNDO" -> {
                removeLast(inventory, event.path("beforeId").asInt());
                int restored = event.path("afterId").asInt();
                if (restored > 0) {
                    add(inventory, restored, ordinal);
                    yield ordinal + 1;
                }
                yield ordinal;
            }
            default -> ordinal;
        };
    }

    private void add(List<Acquisition> inventory, int itemId, int ordinal) {
        if (itemId > 0) {
            inventory.add(new Acquisition(itemId, ordinal));
        }
    }

    private void removeLast(List<Acquisition> inventory, int itemId) {
        for (int index = inventory.size() - 1; index >= 0; index--) {
            if (inventory.get(index).itemId() == itemId) {
                inventory.remove(index);
                return;
            }
        }
    }

    private record Acquisition(int itemId, int ordinal) {
    }
}
