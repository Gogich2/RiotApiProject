package org.main.service.frontend;

import java.util.List;
import org.main.dto.frontend.ChampionAbilityDto;
import org.main.dto.frontend.ChampionDetailsDto;
import org.main.dto.frontend.ChampionItemStatsDto;
import org.main.dto.frontend.ChampionSearchResultDto;
import org.main.dto.frontend.ChampionStatDto;
import org.main.dto.frontend.ChampionSummaryDto;
import org.main.dto.frontend.OverviewStatsDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.PlayerSearchResultDto;
import org.main.dto.frontend.PlayerSummaryDto;
import org.main.dto.frontend.SearchResultDto;
import org.main.exception.NotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.main.dto.frontend.PlayerInsightDto;
import org.main.dto.frontend.PlayerChampionStatsDto;
import org.springframework.stereotype.Service;

@Service
public class FrontendStatsServiceImpl implements FrontendStatsService {

    private static final int SEARCH_LIMIT = 8;

    private static final String DATA_DRAGON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn";

    private final JdbcTemplate jdbcTemplate;

    public FrontendStatsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PlayerInsightDto> getPlayerInsights(String puuid) {
        return jdbcTemplate.query("""
                    SELECT
                        id,
                        puuid,
                        insight_type,
                        title,
                        description,
                        metric_value AS score,
                        created_at
                    FROM analyzed.player_insights
                    WHERE puuid = ?
                    ORDER BY created_at DESC
                    LIMIT 20
                    """,
                (rs, rowNum) -> new PlayerInsightDto(
                        getLong(rs, "id"),
                        rs.getString("puuid"),
                        rs.getString("insight_type"),
                        rs.getString("title"),
                        rs.getString("description"),
                        getDouble(rs, "score"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)
                ),
                puuid
        );
    }

    @Override
    public SearchResultDto search(String query) {
        String safeQuery = query == null ? "" : query.trim();

        if (safeQuery.length() < 2) {
            return new SearchResultDto(List.of(), List.of());
        }

        String pattern = "%" + safeQuery.toLowerCase() + "%";

        List<ChampionSearchResultDto> champions = jdbcTemplate.query("""
                        SELECT
                            p.champion_id,
                            COALESCE(MAX(p.champion_name), 'Unknown') AS champion_name,
                            COUNT(*) AS games
                        FROM core.participants p
                        WHERE p.champion_id IS NOT NULL
                          AND LOWER(COALESCE(p.champion_name, '')) LIKE ?
                        GROUP BY p.champion_id
                        ORDER BY games DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new ChampionSearchResultDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        getLong(rs, "games")
                ),
                pattern,
                SEARCH_LIMIT
        );

        List<PlayerSearchResultDto> players = jdbcTemplate.query("""
                        SELECT
                            pl.puuid,
                            pl.game_name,
                            pl.tag_line,
                            COUNT(p.match_id) AS matches
                        FROM raw.players pl
                        LEFT JOIN core.participants p
                            ON p.puuid = pl.puuid
                        WHERE LOWER(COALESCE(pl.game_name, '')) LIKE ?
                           OR LOWER(COALESCE(pl.tag_line, '')) LIKE ?
                        GROUP BY pl.puuid, pl.game_name, pl.tag_line
                        ORDER BY matches DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new PlayerSearchResultDto(
                        rs.getString("puuid"),
                        rs.getString("game_name"),
                        rs.getString("tag_line"),
                        getLong(rs, "matches")
                ),
                pattern,
                pattern,
                SEARCH_LIMIT
        );

        return new SearchResultDto(champions, players);
    }

    @Override
    public OverviewStatsDto getOverview() {
        Long totalMatches = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM core.matches
            """, Long.class);

        Long totalPlayers = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM raw.players
            WHERE puuid IS NOT NULL
              AND puuid <> ''
              AND puuid <> 'BOT'
            """, Long.class);

        Long totalParticipants = jdbcTemplate.queryForObject("""
            SELECT COUNT(DISTINCT champion_id)
            FROM core.participants
            WHERE champion_id IS NOT NULL
            """, Long.class);

        Double averageMatchDurationMinutes = jdbcTemplate.queryForObject("""
            SELECT ROUND((AVG(game_duration_ms) / 60000.0)::numeric, 2)::double precision
            FROM core.match_details_view
            WHERE game_duration_ms IS NOT NULL
            """, Double.class);

        List<ChampionStatDto> mostPopularChampions = jdbcTemplate.query("""
                    SELECT
                        p.champion_id,
                        COALESCE(MAX(p.champion_name), MAX(c.name), 'Unknown') AS champion_name,
                        CASE
                            WHEN MAX(c.image_full) IS NULL THEN NULL
                            ELSE CONCAT(?, '/', MAX(c.version), '/img/champion/', MAX(c.image_full))
                        END AS image_url,
                        COUNT(*) AS games,
                        SUM(CASE WHEN p.win THEN 1 ELSE 0 END) AS wins,
                        ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0
                                   / COUNT(*))::numeric, 2)::double precision AS winrate
                    FROM core.participants p
                    LEFT JOIN static.champions c
                        ON c.champion_id = p.champion_id
                    WHERE p.champion_id IS NOT NULL
                    GROUP BY p.champion_id
                    ORDER BY games DESC
                    LIMIT 10
                    """,
                (rs, rowNum) -> new ChampionStatDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("image_url"),
                        getLong(rs, "games"),
                        getLong(rs, "wins"),
                        getDouble(rs, "winrate")
                ),
                DATA_DRAGON_BASE_URL
        );

        List<ChampionStatDto> bestWinrateChampions = jdbcTemplate.query("""
                    SELECT
                        p.champion_id,
                        COALESCE(MAX(p.champion_name), MAX(c.name), 'Unknown') AS champion_name,
                        CASE
                            WHEN MAX(c.image_full) IS NULL THEN NULL
                            ELSE CONCAT(?, '/', MAX(c.version), '/img/champion/', MAX(c.image_full))
                        END AS image_url,
                        COUNT(*) AS games,
                        SUM(CASE WHEN p.win THEN 1 ELSE 0 END) AS wins,
                        ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0
                                   / COUNT(*))::numeric, 2)::double precision AS winrate
                    FROM core.participants p
                    LEFT JOIN static.champions c
                        ON c.champion_id = p.champion_id
                    WHERE p.champion_id IS NOT NULL
                    GROUP BY p.champion_id
                    HAVING COUNT(*) >= 10
                    ORDER BY winrate DESC, games DESC
                    LIMIT 10
                    """,
                (rs, rowNum) -> new ChampionStatDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("image_url"),
                        getLong(rs, "games"),
                        getLong(rs, "wins"),
                        getDouble(rs, "winrate")
                ),
                DATA_DRAGON_BASE_URL
        );

        return new OverviewStatsDto(
                nullToZero(totalMatches),
                nullToZero(totalPlayers),
                nullToZero(totalParticipants),
                averageMatchDurationMinutes == null ? 0.0 : averageMatchDurationMinutes,
                mostPopularChampions,
                bestWinrateChampions
        );
    }

    @Override
    public ChampionDetailsDto getChampionDetails(Integer championId) {
        List<ChampionDetailsDto> championDetails = jdbcTemplate.query("""
                SELECT
                    c.champion_id,
                    c.name AS champion_name,
                    COALESCE(c.title, c.raw_json ->> 'title') AS title,
                    CONCAT(
                        'https://ddragon.leagueoflegends.com/cdn/',
                        c.version,
                        '/img/champion/',
                        c.raw_json -> 'image' ->> 'full'
                    ) AS image_url,
                    CONCAT(
                        'https://ddragon.leagueoflegends.com/cdn/img/champion/splash/',
                        c.raw_json ->> 'id',
                        '_0.jpg'
                    ) AS splash_url,
                    c.raw_json ->> 'blurb' AS lore
                FROM static.champions c
                WHERE c.champion_id = ?
                """,
                (rs, rowNum) -> new ChampionDetailsDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("title"),
                        rs.getString("image_url"),
                        rs.getString("splash_url"),
                        rs.getString("lore"),
                        null,
                        List.of()
                ),
                championId
        );

        if (championDetails.isEmpty()) {
            throw new NotFoundException("Champion not found: " + championId);
        }

        ChampionDetailsDto base = championDetails.get(0);
        ChampionSummaryDto summary = getChampionSummary(championId);
        List<ChampionAbilityDto> abilities = getChampionAbilities(championId);

        return new ChampionDetailsDto(
                base.championId(),
                base.championName(),
                base.title(),
                base.imageUrl(),
                base.splashUrl(),
                base.lore(),
                summary,
                abilities
        );
    }

    @Override
    public ChampionSummaryDto getChampionSummary(Integer championId) {
        List<ChampionSummaryDto> summaries = jdbcTemplate.query("""
                        SELECT
                            c.champion_id,
                            COALESCE(MAX(p.champion_name), MAX(c.name), 'Unknown') AS champion_name,
                            COUNT(p.match_id) AS games,
                            COALESCE(SUM(CASE WHEN p.win THEN 1 ELSE 0 END), 0) AS wins,
                            COALESCE(
                                ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0 
                                           / NULLIF(COUNT(p.match_id), 0))::numeric, 2)::double precision,
                                0
                            ) AS winrate,
                            COALESCE(ROUND(AVG(p.kills)::numeric, 2)::double precision, 0) AS avg_kills,
                            COALESCE(ROUND(AVG(p.deaths)::numeric, 2)::double precision, 0) AS avg_deaths,
                            COALESCE(ROUND(AVG(p.assists)::numeric, 2)::double precision, 0) AS avg_assists,
                            COALESCE(ROUND(AVG(p.gold_earned)::numeric, 2)::double precision, 0) AS avg_gold,
                            COALESCE(ROUND(AVG(p.total_damage_to_champions)::numeric, 2)::double precision, 0) 
                                AS avg_damage,
                            COALESCE(ROUND(AVG(p.vision_score)::numeric, 2)::double precision, 0) AS avg_vision
                        FROM static.champions c
                        LEFT JOIN core.participants p
                            ON p.champion_id = c.champion_id
                        WHERE c.champion_id = ?
                        GROUP BY c.champion_id
                        """,
                (rs, rowNum) -> new ChampionSummaryDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        getLong(rs, "games"),
                        getLong(rs, "wins"),
                        getDouble(rs, "winrate"),
                        getDouble(rs, "avg_kills"),
                        getDouble(rs, "avg_deaths"),
                        getDouble(rs, "avg_assists"),
                        getDouble(rs, "avg_gold"),
                        getDouble(rs, "avg_damage"),
                        getDouble(rs, "avg_vision")
                ),
                championId
        );

        if (summaries.isEmpty()) {
            throw new NotFoundException("Champion summary not found: " + championId);
        }

        return summaries.get(0);
    }

    @Override
    public List<ChampionItemStatsDto> getChampionItems(Integer championId) {
        try {
            return jdbcTemplate.query("""
                            WITH latest_items AS (
                                SELECT
                                    item_id,
                                    version,
                                    name,
                                    ROW_NUMBER() OVER (
                                        PARTITION BY item_id
                                        ORDER BY version DESC
                                    ) AS rn
                                FROM static.items
                            ),
                            latest_item_version AS (
                                SELECT MAX(version) AS version
                                FROM static.items
                            ),
                            champion_games AS (
                                SELECT COUNT(*) AS total_games
                                FROM core.participants
                                WHERE champion_id = ?
                            )
                            SELECT
                                i.item_id,
                                COALESCE(li.name, CONCAT('Item ', i.item_id)) AS item_name,
                                CASE
                                    WHEN COALESCE(li.version, liv.version) IS NULL THEN NULL
                                    ELSE CONCAT(
                                        ?,
                                        '/',
                                        COALESCE(li.version, liv.version),
                                        '/img/item/',
                                        i.item_id,
                                        '.png'
                                    )
                                END AS image_url,
                                COUNT(*) AS games,
                                SUM(CASE WHEN p.win THEN 1 ELSE 0 END) AS wins,
                                ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0
                                           / COUNT(*))::numeric, 2)::double precision AS winrate,
                                ROUND((COUNT(*) * 100.0 / NULLIF((SELECT total_games
                                                                  FROM champion_games), 0))::numeric, 2)
                                    ::double precision AS pickrate
                            FROM core.participant_final_items i
                            JOIN core.participants p
                                ON p.match_id = i.match_id
                               AND p.participant_id = i.participant_id
                            LEFT JOIN latest_items li
                                ON li.item_id = i.item_id
                               AND li.rn = 1
                            CROSS JOIN latest_item_version liv
                            WHERE p.champion_id = ?
                            GROUP BY i.item_id, li.name, li.version, liv.version
                            ORDER BY games DESC
                            LIMIT 30
                            """,
                    (rs, rowNum) -> new ChampionItemStatsDto(
                            getInteger(rs, "item_id"),
                            rs.getString("item_name"),
                            rs.getString("image_url"),
                            getLong(rs, "games"),
                            getLong(rs, "wins"),
                            getDouble(rs, "winrate"),
                            getDouble(rs, "pickrate")
                    ),
                    championId,
                    DATA_DRAGON_BASE_URL,
                    championId
            );
        } catch (DataAccessException ex) {
            return jdbcTemplate.query("""
                            WITH champion_games AS (
                                SELECT COUNT(*) AS total_games
                                FROM core.participants
                                WHERE champion_id = ?
                            )
                            SELECT
                                i.item_id,
                                CONCAT('Item ', i.item_id) AS item_name,
                                CONCAT(?, '/15.10.1/img/item/', i.item_id, '.png') AS image_url,
                                COUNT(*) AS games,
                                SUM(CASE WHEN p.win THEN 1 ELSE 0 END) AS wins,
                                ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0
                                           / COUNT(*))::numeric, 2)::double precision AS winrate,
                                ROUND((COUNT(*) * 100.0 / NULLIF((SELECT total_games
                                                                  FROM champion_games), 0))::numeric, 2)
                                    ::double precision AS pickrate
                            FROM core.participant_final_items i
                            JOIN core.participants p
                                ON p.match_id = i.match_id
                               AND p.participant_id = i.participant_id
                            WHERE p.champion_id = ?
                            GROUP BY i.item_id
                            ORDER BY games DESC
                            LIMIT 30
                            """,
                    (rs, rowNum) -> new ChampionItemStatsDto(
                            getInteger(rs, "item_id"),
                            rs.getString("item_name"),
                            rs.getString("image_url"),
                            getLong(rs, "games"),
                            getLong(rs, "wins"),
                            getDouble(rs, "winrate"),
                            getDouble(rs, "pickrate")
                    ),
                    championId,
                    DATA_DRAGON_BASE_URL,
                    championId
            );
        }
    }

    @Override
    public PlayerSummaryDto getPlayerSummary(String puuid) {
        List<PlayerSummaryDto> summaries = jdbcTemplate.query("""
                    SELECT
                        p.puuid,
                        COALESCE(MAX(pl.game_name), 'Unknown') AS game_name,
                        COALESCE(MAX(pl.tag_line), '') AS tag_line,
                        MAX(pl.profile_icon_id) AS profile_icon_id,
                        COUNT(*) AS matches,
                        SUM(CASE WHEN p.win THEN 1 ELSE 0 END) AS wins,
                        ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0
                                   / COUNT(*))::numeric, 2)::double precision AS winrate,
                        ROUND(AVG(p.kills)::numeric, 2)::double precision AS avg_kills,
                        ROUND(AVG(p.deaths)::numeric, 2)::double precision AS avg_deaths,
                        ROUND(AVG(p.assists)::numeric, 2)::double precision AS avg_assists,
                        ROUND(AVG(p.gold_earned)::numeric, 2)::double precision AS avg_gold,
                        ROUND(AVG(p.total_damage_to_champions)::numeric, 2)::double precision AS avg_damage,
                        ROUND(AVG(p.vision_score)::numeric, 2)::double precision AS avg_vision
                    FROM core.participants p
                    LEFT JOIN raw.players pl
                        ON pl.puuid = p.puuid
                    WHERE p.puuid = ?
                    GROUP BY p.puuid
                    """,
                (rs, rowNum) -> new PlayerSummaryDto(
                        rs.getString("puuid"),
                        rs.getString("game_name"),
                        rs.getString("tag_line"),
                        getInteger(rs, "profile_icon_id"),
                        getLong(rs, "matches"),
                        getLong(rs, "wins"),
                        getDouble(rs, "winrate"),
                        getDouble(rs, "avg_kills"),
                        getDouble(rs, "avg_deaths"),
                        getDouble(rs, "avg_assists"),
                        getDouble(rs, "avg_gold"),
                        getDouble(rs, "avg_damage"),
                        getDouble(rs, "avg_vision")
                ),
                puuid
        );

        if (summaries.isEmpty()) {
            throw new NotFoundException("Player not found: " + puuid);
        }

        return summaries.get(0);
    }


    @Override
    public List<PlayerRecentMatchDto> getPlayerRecentMatches(String puuid, int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 50);

        return jdbcTemplate.query("""
                    WITH latest_champions AS (
                        SELECT
                            champion_id,
                            version,
                            image_full,
                            ROW_NUMBER() OVER (
                                PARTITION BY champion_id
                                ORDER BY version DESC
                            ) AS rn
                        FROM static.champions
                    )
                    SELECT
                        p.match_id,
                        p.champion_id,
                        COALESCE(p.champion_name, 'Unknown') AS champion_name,
                        CASE
                            WHEN lc.image_full IS NULL THEN NULL
                            ELSE CONCAT(?, '/', lc.version, '/img/champion/', lc.image_full)
                        END AS champion_image_url,
                        p.win,
                        p.kills,
                        p.deaths,
                        p.assists,
                        m.queue_id,
                        m.game_version,
                        m.game_creation_ms,
                        m.game_duration_ms
                    FROM core.participants p
                    JOIN core.match_details_view m
                        ON m.match_id = p.match_id
                    LEFT JOIN latest_champions lc
                        ON lc.champion_id = p.champion_id
                       AND lc.rn = 1
                    WHERE p.puuid = ?
                    ORDER BY m.game_creation_ms DESC NULLS LAST
                    LIMIT ?
                    """,
                (rs, rowNum) -> new PlayerRecentMatchDto(
                        rs.getString("match_id"),
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("champion_image_url"),
                        getBoolean(rs, "win"),
                        getInteger(rs, "kills"),
                        getInteger(rs, "deaths"),
                        getInteger(rs, "assists"),
                        getInteger(rs, "queue_id"),
                        rs.getString("game_version"),
                        getLong(rs, "game_creation_ms"),
                        getLong(rs, "game_duration_ms")
                ),
                DATA_DRAGON_BASE_URL,
                puuid,
                safeLimit
        );
    }

    private List<ChampionAbilityDto> getChampionAbilities(Integer championId) {
        return jdbcTemplate.query("""
                        SELECT
                            ability_key,
                            ability_name,
                            ability_description,
                            image_url
                        FROM static.champion_abilities
                        WHERE champion_id = ?
                        ORDER BY
                            CASE ability_key
                                WHEN 'PASSIVE' THEN 0
                                WHEN 'Q' THEN 1
                                WHEN 'W' THEN 2
                                WHEN 'E' THEN 3
                                WHEN 'R' THEN 4
                                ELSE 5
                            END
                        """,
                (rs, rowNum) -> new ChampionAbilityDto(
                        rs.getString("ability_key"),
                        rs.getString("ability_name"),
                        rs.getString("ability_description"),
                        rs.getString("image_url")
                ),
                championId
        );
    }

    @Override
    public List<PlayerChampionStatsDto> getPlayerChampions(String puuid) {
        return jdbcTemplate.query("""
                    SELECT
                        p.champion_id,
                        COALESCE(MAX(p.champion_name), MAX(c.name), 'Unknown') AS champion_name,
                        CASE
                            WHEN MAX(c.image_full) IS NULL THEN NULL
                            ELSE CONCAT(?, '/', MAX(c.version), '/img/champion/', MAX(c.image_full))
                        END AS image_url,
                        COUNT(*) AS games,
                        SUM(CASE WHEN p.win THEN 1 ELSE 0 END) AS wins,
                        ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0 / 
                               COUNT(*))::numeric, 2)::double precision AS winrate,
                        ROUND(AVG(p.kills)::numeric, 2)::double precision AS avg_kills,
                        ROUND(AVG(p.deaths)::numeric, 2)::double precision AS avg_deaths,
                        ROUND(AVG(p.assists)::numeric, 2)::double precision AS avg_assists
                    FROM core.participants p
                    LEFT JOIN static.champions c
                        ON c.champion_id = p.champion_id
                    WHERE p.puuid = ?
                      AND p.champion_id IS NOT NULL
                    GROUP BY p.champion_id
                    ORDER BY games DESC, winrate DESC, avg_kills DESC
                    LIMIT 8
                    """,
                (rs, rowNum) -> new PlayerChampionStatsDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("image_url"),
                        getLong(rs, "games"),
                        getLong(rs, "wins"),
                        getDouble(rs, "winrate"),
                        getDouble(rs, "avg_kills"),
                        getDouble(rs, "avg_deaths"),
                        getDouble(rs, "avg_assists")
                ),
                DATA_DRAGON_BASE_URL,
                puuid
        );
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private Integer getInteger(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Long getLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Double getDouble(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBoolean(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        boolean value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
    }

}
