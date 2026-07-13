package org.main.builds.extract;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.main.builds.model.RunePage;

public final class RunePageExtractor {

    public RunePage extract(JsonNode perks) {
        JsonNode primary = findStyle(perks, "primaryStyle");
        JsonNode secondary = findStyle(perks, "subStyle");
        List<Integer> shards = List.of(
                positive(perks.path("statPerks").path("offense")),
                positive(perks.path("statPerks").path("flex")),
                positive(perks.path("statPerks").path("defense"))
        );
        List<Integer> primarySelections = selections(primary);
        List<Integer> secondarySelections = selections(secondary);
        if (primarySelections.size() != 4 || secondarySelections.size() != 2 || shards.size() != 3) {
            throw new IllegalArgumentException("Rune selections are incomplete");
        }
        return new RunePage(positive(primary.path("style")), primarySelections,
                positive(secondary.path("style")), secondarySelections, shards);
    }

    private JsonNode findStyle(JsonNode perks, String description) {
        for (JsonNode style : perks.path("styles")) {
            if (description.equalsIgnoreCase(style.path("description").asText())) {
                return style;
            }
        }
        throw new IllegalArgumentException("Missing rune style: " + description);
    }

    private List<Integer> selections(JsonNode style) {
        List<Integer> selections = new ArrayList<>();
        for (JsonNode selection : style.path("selections")) {
            selections.add(positive(selection.path("perk")));
        }
        return List.copyOf(selections);
    }

    private int positive(JsonNode node) {
        int value = node.asInt();
        if (value <= 0) {
            throw new IllegalArgumentException("Expected a positive rune id");
        }
        return value;
    }
}
