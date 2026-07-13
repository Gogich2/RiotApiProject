package org.main.builds.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
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
        return Optional.ofNullable(findChampions(List.of(championId)).get(championId));
    }

    @Override
    public Map<Integer, DisplayAsset> findChampions(List<Integer> championIds) {
        return find("""
                select champion_id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/', version,
                              '/img/champion/', image_full) image_url
                from static.champions where champion_id in (%s)
                """, "champion_id", championIds);
    }

    @Override
    public Map<Integer, DisplayAsset> findItems(List<Integer> itemIds) {
        return find("""
                select item_id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/', version,
                              '/img/item/', image_full) image_url
                from static.items where item_id in (%s)
                """, "item_id", itemIds);
    }

    @Override
    public Map<Integer, DisplayAsset> findRunes(List<Integer> runeIds) {
        List<Integer> ids = unique(runeIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        String placeholders = placeholders(ids.size());
        Object[] arguments = Stream.concat(ids.stream(), ids.stream()).toArray();
        return findRows("""
                select id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/img/', icon) image_url
                from (
                    select rune_id id, name, icon from static.runes where rune_id in (%s)
                    union all
                    select style_id id, name, icon
                    from static.rune_styles where style_id in (%s)
                ) asset
                """.formatted(placeholders, placeholders), "id", arguments);
    }

    @Override
    public Map<Integer, DisplayAsset> findSpells(List<Integer> spellIds) {
        return find("""
                select spell_id, name,
                       concat('https://ddragon.leagueoflegends.com/cdn/', version,
                              '/img/spell/', image_full) image_url
                from static.summoner_spells where spell_id in (%s)
                """, "spell_id", spellIds);
    }

    private Map<Integer, DisplayAsset> find(
            String sql, String idColumn, List<Integer> requestedIds) {
        List<Integer> ids = unique(requestedIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        return findRows(sql.formatted(placeholders(ids.size())),
                idColumn, ids.toArray());
    }

    private Map<Integer, DisplayAsset> findRows(
            String sql, String idColumn, Object... arguments) {
        List<DisplayAsset> rows = jdbc.query(sql, (row, index) -> new DisplayAsset(
                row.getInt(idColumn), row.getString("name"), row.getString("image_url")),
                arguments);
        Map<Integer, DisplayAsset> result = new LinkedHashMap<>();
        rows.forEach(asset -> result.put(asset.id(), asset));
        return Map.copyOf(result);
    }

    private List<Integer> unique(List<Integer> ids) {
        return ids.stream().distinct().toList();
    }

    private String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
    }
}
