package org.main.builds.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcBuildAssetRepository implements BuildAssetRepository {

    private final JdbcTemplate jdbc;

    public JdbcBuildAssetRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<DisplayAsset> findChampion(int championId) {
        List<DisplayAsset> rows = jdbc.query("""
                select champion_id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/', version,
                              '/img/champion/', image_full) image_url
                from static.champions where champion_id = ?
                """, (row, index) -> new DisplayAsset(
                row.getInt("champion_id"), row.getString("name"),
                row.getString("image_url")), championId);
        return rows.stream().findFirst();
    }

    @Override
    public Map<Integer, DisplayAsset> findChampions(List<Integer> championIds) {
        return findEach(championIds, id -> findChampion(id).orElse(null));
    }

    @Override
    public Map<Integer, DisplayAsset> findItems(List<Integer> itemIds) {
        return findEach(itemIds, id -> one("""
                select item_id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/', version,
                              '/img/item/', image_full) image_url
                from static.items where item_id = ?
                """, "item_id", id));
    }

    @Override
    public Map<Integer, DisplayAsset> findRunes(List<Integer> runeIds) {
        return findEach(runeIds, id -> one("""
                select id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/img/', icon) image_url
                from (
                    select rune_id id, name, icon from static.runes where rune_id = ?
                    union all
                    select style_id id, name, icon from static.rune_styles where style_id = ?
                ) asset limit 1
                """, "id", id, id));
    }

    @Override
    public Map<Integer, DisplayAsset> findSpells(List<Integer> spellIds) {
        return findEach(spellIds, id -> one("""
                select spell_id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/', version,
                              '/img/spell/', image_full) image_url
                from static.summoner_spells where spell_id = ?
                """, "spell_id", id));
    }

    private DisplayAsset one(String sql, String idColumn, Object... arguments) {
        List<DisplayAsset> rows = jdbc.query(sql, (row, index) -> new DisplayAsset(
                row.getInt(idColumn), row.getString("name"), row.getString("image_url")),
                arguments);
        return rows.stream().findFirst().orElse(null);
    }

    private Map<Integer, DisplayAsset> findEach(
            List<Integer> ids, IntFunction<DisplayAsset> finder) {
        Map<Integer, DisplayAsset> result = new LinkedHashMap<>();
        ids.stream().distinct().forEach(id -> {
            DisplayAsset asset = finder.apply(id);
            if (asset != null) {
                result.put(id, asset);
            }
        });
        return Map.copyOf(result);
    }
}
