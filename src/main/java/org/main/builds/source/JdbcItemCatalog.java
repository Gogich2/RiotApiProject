package org.main.builds.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.main.builds.model.PatchVersion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcItemCatalog implements ItemCatalog {

    private final Map<Integer, ItemDefinition> items;

    public JdbcItemCatalog(JdbcTemplate jdbcTemplate) {
        ObjectMapper mapper = new ObjectMapper();
        String latestVersion = jdbcTemplate.queryForList(
                        "SELECT DISTINCT version FROM static.items", String.class).stream().
                max(JdbcItemCatalog::compareVersions).
                orElse(null);
        Map<Integer, ItemDefinition> loaded = new HashMap<>();
        if (latestVersion != null) {
            jdbcTemplate.query("""
                    SELECT item_id, tags::text, maps::text, raw_json::text
                    FROM static.items
                    WHERE version = ?
                    """, resultSet -> {
                        int itemId = resultSet.getInt("item_id");
                        JsonNode tags = read(mapper, resultSet.getString("tags"));
                        JsonNode maps = read(mapper, resultSet.getString("maps"));
                        JsonNode raw = read(mapper, resultSet.getString("raw_json"));
                        loaded.put(itemId, new ItemDefinition(tags, maps, raw));
                    }, latestVersion);
        }
        items = Map.copyOf(loaded);
    }

    @Override
    public boolean isStartingItem(int itemId) {
        ItemDefinition item = items.get(itemId);
        return item != null && item.allowed();
    }

    @Override
    public boolean isCompletedBoot(int itemId) {
        ItemDefinition item = items.get(itemId);
        return item != null && item.allowed() && item.hasTag("Boots") && item.isCompleted();
    }

    @Override
    public boolean isCompletedCoreItem(int itemId) {
        ItemDefinition item = items.get(itemId);
        return item != null && item.allowed() && !item.hasTag("Boots") && item.isCompleted();
    }

    private static int compareVersions(String left, String right) {
        int comparison = PatchVersion.parse(left).compareTo(PatchVersion.parse(right));
        return comparison != 0 ? comparison : left.compareTo(right);
    }

    private static JsonNode read(ObjectMapper mapper, String json) {
        try {
            return json == null ? mapper.nullNode() : mapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid static item JSON", exception);
        }
    }

    private record ItemDefinition(JsonNode tags, JsonNode maps, JsonNode raw) {

        private static final Set<String> EXCLUDED_TAGS = Set.of("Consumable", "Trinket");

        boolean allowed() {
            boolean onSummonersRift = maps.path("11").asBoolean(false);
            boolean purchasable = raw.path("gold").path("purchasable").asBoolean(true);
            return onSummonersRift && purchasable
                    && EXCLUDED_TAGS.stream().noneMatch(this::hasTag);
        }

        boolean hasTag(String expected) {
            for (JsonNode tag : tags) {
                if (expected.equalsIgnoreCase(tag.asText())) {
                    return true;
                }
            }
            return false;
        }

        boolean isCompleted() {
            return !raw.path("into").isArray() || raw.path("into").isEmpty();
        }
    }
}
