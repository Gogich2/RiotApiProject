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
        String url = DATA_DRAGON_BASE_URL + "/cdn/" + version + "/data/" + LANGUAGE + "/championFull.json";
        JsonNode root = fetchJson(url);
        JsonNode data = root.path("data");

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();

        while (fields.hasNext()) {
            JsonNode champion = fields.next().getValue();

            Integer championId = parseInteger(champion.path("key").asText(null));

            if (championId == null) {
                continue;
            }

            OffsetDateTime now = OffsetDateTime.now();
            String championKey = champion.path("id").asText(null);
            String name = champion.path("name").asText(null);
            String title = champion.path("title").asText(null);
            String imageFull = champion.path("image").path("full").asText(null);
            String imageSprite = champion.path("image").path("sprite").asText(null);
            String tagsJson = toJson(champion.path("tags"));
            String rawJson = toJson(champion);

            int updated = jdbcTemplate.update("""
                    UPDATE static.champions
                    SET version = ?,
                        champion_key = ?,
                        name = ?,
                        title = ?,
                        image_full = ?,
                        image_sprite = ?,
                        tags = ?::jsonb,
                        raw_json = ?::jsonb,
                        updated_at = ?
                    WHERE champion_id = ?
                    """,
                    version,
                    championKey,
                    name,
                    title,
                    imageFull,
                    imageSprite,
                    tagsJson,
                    rawJson,
                    now,
                    championId
            );

            if (updated == 0) {
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
                        """,
                        championId,
                        version,
                        championKey,
                        name,
                        title,
                        imageFull,
                        imageSprite,
                        tagsJson,
                        rawJson,
                        now
                );
            }
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

            OffsetDateTime now = OffsetDateTime.now();
            String name = item.path("name").asText(null);
            String description = item.path("description").asText(null);
            String plaintext = item.path("plaintext").asText(null);
            String imageFull = item.path("image").path("full").asText(null);
            String imageSprite = item.path("image").path("sprite").asText(null);
            Integer goldBase = nullableInt(item.path("gold").path("base"));
            Integer goldTotal = nullableInt(item.path("gold").path("total"));
            Integer goldSell = nullableInt(item.path("gold").path("sell"));
            Boolean purchasable = nullableBoolean(item.path("gold").path("purchasable"));
            String tagsJson = toJson(item.path("tags"));
            String mapsJson = toJson(item.path("maps"));
            String rawJson = toJson(item);

            int updated = jdbcTemplate.update("""
                    UPDATE static.items
                    SET version = ?,
                        name = ?,
                        description = ?,
                        plaintext = ?,
                        image_full = ?,
                        image_sprite = ?,
                        gold_base = ?,
                        gold_total = ?,
                        gold_sell = ?,
                        purchasable = ?,
                        tags = ?::jsonb,
                        maps = ?::jsonb,
                        raw_json = ?::jsonb,
                        updated_at = ?
                    WHERE item_id = ?
                    """,
                    version,
                    name,
                    description,
                    plaintext,
                    imageFull,
                    imageSprite,
                    goldBase,
                    goldTotal,
                    goldSell,
                    purchasable,
                    tagsJson,
                    mapsJson,
                    rawJson,
                    now,
                    itemId
            );

            if (updated == 0) {
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
                        """,
                        itemId,
                        version,
                        name,
                        description,
                        plaintext,
                        imageFull,
                        imageSprite,
                        goldBase,
                        goldTotal,
                        goldSell,
                        purchasable,
                        tagsJson,
                        mapsJson,
                        rawJson,
                        now
                );
            }
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

            OffsetDateTime now = OffsetDateTime.now();
            String spellKey = spell.path("id").asText(null);
            String name = spell.path("name").asText(null);
            String description = spell.path("description").asText(null);
            String tooltip = spell.path("tooltip").asText(null);
            String imageFull = spell.path("image").path("full").asText(null);
            String imageSprite = spell.path("image").path("sprite").asText(null);
            String modesJson = toJson(spell.path("modes"));
            String rawJson = toJson(spell);

            int updated = jdbcTemplate.update("""
                    UPDATE static.summoner_spells
                    SET version = ?,
                        spell_key = ?,
                        name = ?,
                        description = ?,
                        tooltip = ?,
                        image_full = ?,
                        image_sprite = ?,
                        modes = ?::jsonb,
                        raw_json = ?::jsonb,
                        updated_at = ?
                    WHERE spell_id = ?
                    """,
                    version,
                    spellKey,
                    name,
                    description,
                    tooltip,
                    imageFull,
                    imageSprite,
                    modesJson,
                    rawJson,
                    now,
                    spellId
            );

            if (updated == 0) {
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
                        """,
                        spellId,
                        version,
                        spellKey,
                        name,
                        description,
                        tooltip,
                        imageFull,
                        imageSprite,
                        modesJson,
                        rawJson,
                        now
                );
            }
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

            OffsetDateTime styleUpdatedAt = OffsetDateTime.now();
            String styleName = style.path("name").asText(null);
            String styleIcon = style.path("icon").asText(null);
            String styleRawJson = toJson(style);

            int updatedStyle = jdbcTemplate.update("""
                    UPDATE static.rune_styles
                    SET version = ?,
                        name = ?,
                        icon = ?,
                        raw_json = ?::jsonb,
                        updated_at = ?
                    WHERE style_id = ?
                    """,
                    version,
                    styleName,
                    styleIcon,
                    styleRawJson,
                    styleUpdatedAt,
                    styleId
            );

            if (updatedStyle == 0) {
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
                        """,
                        styleId,
                        version,
                        styleName,
                        styleIcon,
                        styleRawJson,
                        styleUpdatedAt
                );
            }

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

                    OffsetDateTime runeUpdatedAt = OffsetDateTime.now();
                    String runeName = rune.path("name").asText(null);
                    String shortDesc = rune.path("shortDesc").asText(null);
                    String longDesc = rune.path("longDesc").asText(null);
                    String runeIcon = rune.path("icon").asText(null);
                    String runeRawJson = toJson(rune);

                    int updatedRune = jdbcTemplate.update("""
                            UPDATE static.runes
                            SET version = ?,
                                style_id = ?,
                                name = ?,
                                short_desc = ?,
                                long_desc = ?,
                                icon = ?,
                                slot_index = ?,
                                raw_json = ?::jsonb,
                                updated_at = ?
                            WHERE rune_id = ?
                            """,
                            version,
                            styleId,
                            runeName,
                            shortDesc,
                            longDesc,
                            runeIcon,
                            slotIndex,
                            runeRawJson,
                            runeUpdatedAt,
                            runeId
                    );

                    if (updatedRune == 0) {
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
                                """,
                                runeId,
                                version,
                                styleId,
                                runeName,
                                shortDesc,
                                longDesc,
                                runeIcon,
                                slotIndex,
                                runeRawJson,
                                runeUpdatedAt
                        );
                    }
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
