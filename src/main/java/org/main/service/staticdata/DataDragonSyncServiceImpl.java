package org.main.service.staticdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DataDragonSyncServiceImpl implements DataDragonSyncService {

    private static final String DATA_DRAGON_BASE_URL = "https://ddragon.leagueoflegends.com";

    private static final String LANGUAGE = "en_US";

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    public DataDragonSyncServiceImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String syncLatestVersion() {
        String version = fetchLatestVersion();

        syncChampions(version);
        syncItems(version);
        syncSummonerSpells(version);
        syncRunes(version);

        return version;
    }

    private String fetchLatestVersion() {
        String url = DATA_DRAGON_BASE_URL + "/api/versions.json";
        JsonNode versions = fetchJson(url);

        if (!versions.isArray() || versions.isEmpty()) {
            throw new IllegalStateException("Data Dragon versions list is empty");
        }

        return versions.get(0).asText();
    }

    private void syncChampions(String version) {
        String url = DATA_DRAGON_BASE_URL + "/cdn/" + version + "/data/" + LANGUAGE + "/champion.json";
        JsonNode root = fetchJson(url);
        JsonNode data = root.path("data");

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

        while (fields.hasNext()) {
            JsonNode champion = fields.next().getValue();

            Integer championId = parseInteger(champion.path("key").asText(null));

            if (championId == null) {
                continue;
            }

            jdbcTemplate.update("""
                    INSERT INTO static.champions
                    (
                        champion_id,
                        version,
                        champion_key,
                        name,
                        title,
                        image_full,
                        image_sprite,
                        tags,
                        raw_json,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                    ON CONFLICT (champion_id, version) DO UPDATE
                    SET champion_key = EXCLUDED.champion_key,
                        name = EXCLUDED.name,
                        title = EXCLUDED.title,
                        image_full = EXCLUDED.image_full,
                        image_sprite = EXCLUDED.image_sprite,
                        tags = EXCLUDED.tags,
                        raw_json = EXCLUDED.raw_json,
                        updated_at = EXCLUDED.updated_at
                    """,
                    championId,
                    version,
                    champion.path("id").asText(null),
                    champion.path("name").asText(null),
                    champion.path("title").asText(null),
                    champion.path("image").path("full").asText(null),
                    champion.path("image").path("sprite").asText(null),
                    toJson(champion.path("tags")),
                    toJson(champion),
                    OffsetDateTime.now()
            );
        }
    }

    private void syncItems(String version) {
        String url = DATA_DRAGON_BASE_URL + "/cdn/" + version + "/data/" + LANGUAGE + "/item.json";
        JsonNode root = fetchJson(url);
        JsonNode data = root.path("data");

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();

            Integer itemId = parseInteger(entry.getKey());
            JsonNode item = entry.getValue();

            if (itemId == null) {
                continue;
            }

            jdbcTemplate.update("""
                    INSERT INTO static.items
                    (
                        item_id,
                        version,
                        name,
                        description,
                        plaintext,
                        image_full,
                        image_sprite,
                        gold_base,
                        gold_total,
                        gold_sell,
                        purchasable,
                        tags,
                        maps,
                        raw_json,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?)
                    ON CONFLICT (item_id, version) DO UPDATE
                    SET name = EXCLUDED.name,
                        description = EXCLUDED.description,
                        plaintext = EXCLUDED.plaintext,
                        image_full = EXCLUDED.image_full,
                        image_sprite = EXCLUDED.image_sprite,
                        gold_base = EXCLUDED.gold_base,
                        gold_total = EXCLUDED.gold_total,
                        gold_sell = EXCLUDED.gold_sell,
                        purchasable = EXCLUDED.purchasable,
                        tags = EXCLUDED.tags,
                        maps = EXCLUDED.maps,
                        raw_json = EXCLUDED.raw_json,
                        updated_at = EXCLUDED.updated_at
                    """,
                    itemId,
                    version,
                    item.path("name").asText(null),
                    item.path("description").asText(null),
                    item.path("plaintext").asText(null),
                    item.path("image").path("full").asText(null),
                    item.path("image").path("sprite").asText(null),
                    nullableInt(item.path("gold").path("base")),
                    nullableInt(item.path("gold").path("total")),
                    nullableInt(item.path("gold").path("sell")),
                    nullableBoolean(item.path("gold").path("purchasable")),
                    toJson(item.path("tags")),
                    toJson(item.path("maps")),
                    toJson(item),
                    OffsetDateTime.now()
            );
        }
    }

    private void syncSummonerSpells(String version) {
        String url = DATA_DRAGON_BASE_URL + "/cdn/" + version + "/data/" + LANGUAGE + "/summoner.json";
        JsonNode root = fetchJson(url);
        JsonNode data = root.path("data");

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

        while (fields.hasNext()) {
            JsonNode spell = fields.next().getValue();

            Integer spellId = parseInteger(spell.path("key").asText(null));

            if (spellId == null) {
                continue;
            }

            jdbcTemplate.update("""
                    INSERT INTO static.summoner_spells
                    (
                        spell_id,
                        version,
                        spell_key,
                        name,
                        description,
                        tooltip,
                        image_full,
                        image_sprite,
                        modes,
                        raw_json,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?)
                    ON CONFLICT (spell_id, version) DO UPDATE
                    SET spell_key = EXCLUDED.spell_key,
                        name = EXCLUDED.name,
                        description = EXCLUDED.description,
                        tooltip = EXCLUDED.tooltip,
                        image_full = EXCLUDED.image_full,
                        image_sprite = EXCLUDED.image_sprite,
                        modes = EXCLUDED.modes,
                        raw_json = EXCLUDED.raw_json,
                        updated_at = EXCLUDED.updated_at
                    """,
                    spellId,
                    version,
                    spell.path("id").asText(null),
                    spell.path("name").asText(null),
                    spell.path("description").asText(null),
                    spell.path("tooltip").asText(null),
                    spell.path("image").path("full").asText(null),
                    spell.path("image").path("sprite").asText(null),
                    toJson(spell.path("modes")),
                    toJson(spell),
                    OffsetDateTime.now()
            );
        }
    }

    private void syncRunes(String version) {
        String url = DATA_DRAGON_BASE_URL + "/cdn/" + version + "/data/" + LANGUAGE + "/runesReforged.json";
        JsonNode styles = fetchJson(url);

        if (!styles.isArray()) {
            throw new IllegalStateException("Invalid runesReforged response");
        }

        for (JsonNode style : styles) {
            Integer styleId = nullableInt(style.path("id"));

            if (styleId == null) {
                continue;
            }

            jdbcTemplate.update("""
                    INSERT INTO static.rune_styles
                    (
                        style_id,
                        version,
                        name,
                        icon,
                        raw_json,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?::jsonb, ?)
                    ON CONFLICT (style_id, version) DO UPDATE
                    SET name = EXCLUDED.name,
                        icon = EXCLUDED.icon,
                        raw_json = EXCLUDED.raw_json,
                        updated_at = EXCLUDED.updated_at
                    """,
                    styleId,
                    version,
                    style.path("name").asText(null),
                    style.path("icon").asText(null),
                    toJson(style),
                    OffsetDateTime.now()
            );

            JsonNode slots = style.path("slots");

            if (!slots.isArray()) {
                continue;
            }

            for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                JsonNode slot = slots.get(slotIndex);
                JsonNode runes = slot.path("runes");

                if (!runes.isArray()) {
                    continue;
                }

                for (JsonNode rune : runes) {
                    Integer runeId = nullableInt(rune.path("id"));

                    if (runeId == null || runeId == 0) {
                        continue;
                    }

                    jdbcTemplate.update("""
                            INSERT INTO static.runes
                            (
                                rune_id,
                                version,
                                style_id,
                                name,
                                short_desc,
                                long_desc,
                                icon,
                                slot_index,
                                raw_json,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                            ON CONFLICT (rune_id, version) DO UPDATE
                            SET style_id = EXCLUDED.style_id,
                                name = EXCLUDED.name,
                                short_desc = EXCLUDED.short_desc,
                                long_desc = EXCLUDED.long_desc,
                                icon = EXCLUDED.icon,
                                slot_index = EXCLUDED.slot_index,
                                raw_json = EXCLUDED.raw_json,
                                updated_at = EXCLUDED.updated_at
                            """,
                            runeId,
                            version,
                            styleId,
                            rune.path("name").asText(null),
                            rune.path("shortDesc").asText(null),
                            rune.path("longDesc").asText(null),
                            rune.path("icon").asText(null),
                            slotIndex,
                            toJson(rune),
                            OffsetDateTime.now()
                    );
                }
            }
        }
    }

    private JsonNode fetchJson(String url) {
        String body = restTemplate.getForObject(url, String.class);

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Empty response from Data Dragon: " + url);
        }

        try {
            return objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Data Dragon JSON from " + url, ex);
        }
    }

    private String toJson(JsonNode node) {
        try {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }

            return objectMapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize JSON node", ex);
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer nullableInt(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asInt();
    }

    private Boolean nullableBoolean(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asBoolean();
    }
}