package org.main.builds.extract;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        Map<Integer, List<Integer>> acquisitionHistory = new HashMap<>();
        Map<Integer, List<Integer>> earlyHistory = new HashMap<>();
        int ordinal = 0;

        for (JsonNode frame : timeline.path("info").path("frames")) {
            for (JsonNode event : frame.path("events")) {
                if (event.path("participantId").asInt(-1) != participantId) {
                    continue;
                }
                long timestamp = event.path("timestamp").asLong(Long.MAX_VALUE);
                ordinal = apply(event, inventory, acquisitionHistory, ordinal);
                if (timestamp <= cutoffMillis) {
                    apply(event, earlyInventory, earlyHistory, ordinal - 1);
                }
            }
        }

        List<Integer> starting = earlyInventory.stream().
                sorted(Comparator.comparingInt(Acquisition::ordinal)).
                map(Acquisition::itemId).
                filter(catalog::isStartingItem).
                toList();
        Integer boots = acquisitionHistory.entrySet().stream().
                sorted(Comparator.comparingInt(entry -> entry.getValue().getFirst())).
                map(Map.Entry::getKey).
                filter(finalItemIds::contains).
                filter(catalog::isCompletedBoot).
                findFirst().
                orElse(null);
        List<Integer> core = acquisitionHistory.entrySet().stream().
                sorted(Comparator.comparingInt(entry -> entry.getValue().getFirst())).
                map(Map.Entry::getKey).
                filter(finalItemIds::contains).
                filter(catalog::isCompletedCoreItem).
                limit(3).
                toList();
        return new ItemPath(starting, boots, core);
    }

    private int apply(JsonNode event, List<Acquisition> inventory,
                      Map<Integer, List<Integer>> history, int ordinal) {
        return switch (event.path("type").asText()) {
            case "ITEM_PURCHASED" -> {
                add(inventory, history, event.path("itemId").asInt(), ordinal);
                yield ordinal + 1;
            }
            case "ITEM_SOLD", "ITEM_DESTROYED" -> {
                removeLast(inventory, event.path("itemId").asInt());
                yield ordinal;
            }
            case "ITEM_UNDO" -> {
                int undone = event.path("beforeId").asInt();
                removeLast(inventory, undone);
                removeLastHistory(history, undone);
                int restored = event.path("afterId").asInt();
                if (restored > 0) {
                    add(inventory, history, restored, ordinal);
                    yield ordinal + 1;
                }
                yield ordinal;
            }
            default -> ordinal;
        };
    }

    private void add(List<Acquisition> inventory, Map<Integer, List<Integer>> history,
                     int itemId, int ordinal) {
        if (itemId > 0) {
            inventory.add(new Acquisition(itemId, ordinal));
            history.computeIfAbsent(itemId, unused -> new ArrayList<>()).add(ordinal);
        }
    }

    private void removeLastHistory(Map<Integer, List<Integer>> history, int itemId) {
        List<Integer> acquisitions = history.get(itemId);
        if (acquisitions != null && !acquisitions.isEmpty()) {
            acquisitions.removeLast();
            if (acquisitions.isEmpty()) {
                history.remove(itemId);
            }
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
