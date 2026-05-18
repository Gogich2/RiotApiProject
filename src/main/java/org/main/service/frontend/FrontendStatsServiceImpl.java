package org.main.service.frontend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.main.dto.frontend.ChampionAbilityDto;
import org.main.dto.frontend.ChampionDetailsDto;
import org.main.dto.frontend.ChampionItemStatsDto;
import org.main.dto.frontend.ChampionSearchResultDto;
import org.main.dto.frontend.ChampionStatDto;
import org.main.dto.frontend.ChampionSummaryDto;
import org.main.dto.frontend.MatchDetailsDto;
import org.main.dto.frontend.MatchMetricsDto;
import org.main.dto.frontend.MatchParticipantDto;
import org.main.dto.frontend.MatchParticipantItemEventDto;
import org.main.dto.frontend.MatchParticipantRuneDto;
import org.main.dto.frontend.MatchParticipantSkillOrderDto;
import org.main.dto.frontend.MatchSummaryDto;
import org.main.dto.frontend.MatchTeamDto;
import org.main.dto.frontend.MatchTimelineEventDto;
import org.main.dto.frontend.MatchTimelinePositionDto;
import org.main.dto.frontend.OverviewStatsDto;
import org.main.dto.frontend.PlayerChampionStatsDto;
import org.main.dto.frontend.PlayerInsightDto;
import org.main.dto.frontend.PlayerLeaderboardDto;
import org.main.dto.frontend.PlayerLeaderboardResponseDto;
import org.main.dto.frontend.PlayerMatchItemDto;
import org.main.dto.frontend.PlayerRecentMatchDto;
import org.main.dto.frontend.PlayerSearchResultDto;
import org.main.dto.frontend.PlayerSummaryDto;
import org.main.dto.frontend.SearchResultDto;
import org.main.exception.NotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
                        getDouble(rs, "winrate"),
                        null
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
                        getDouble(rs, "winrate"),
                        null
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
    public List<ChampionStatDto> getChampions() {
        return jdbcTemplate.query("""
                        WITH role_counts AS (
                            SELECT
                                p.champion_id,
                                CASE
                                    WHEN COALESCE(NULLIF(p.team_position, ''), NULLIF(p.individual_position, ''))
                                            IN ('TOP', 'JUNGLE', 'MIDDLE', 'BOTTOM', 'UTILITY') THEN
                                        COALESCE(NULLIF(p.team_position, ''), NULLIF(p.individual_position, ''))
                                    ELSE NULL
                                END AS primary_role,
                                COUNT(*) AS role_games
                            FROM core.participants p
                            WHERE p.champion_id IS NOT NULL
                            GROUP BY p.champion_id, primary_role
                        ),
                        champion_roles AS (
                            SELECT DISTINCT ON (rc.champion_id)
                                rc.champion_id,
                                rc.primary_role
                            FROM role_counts rc
                            WHERE rc.primary_role IS NOT NULL
                            ORDER BY rc.champion_id, rc.role_games DESC, rc.primary_role ASC
                        )
                        SELECT
                            c.champion_id,
                            COALESCE(c.name, 'Unknown') AS champion_name,
                            CASE
                                WHEN c.image_full IS NULL THEN NULL
                                ELSE CONCAT(?, '/', c.version, '/img/champion/', c.image_full)
                            END AS image_url,
                            COUNT(p.match_id) AS games,
                            COALESCE(SUM(CASE WHEN p.win THEN 1 ELSE 0 END), 0) AS wins,
                            COALESCE(
                                ROUND((SUM(CASE WHEN p.win THEN 1 ELSE 0 END) * 100.0
                                           / NULLIF(COUNT(p.match_id), 0))::numeric, 2)::double precision,
                                0
                            ) AS winrate,
                            cr.primary_role
                        FROM static.champions c
                        LEFT JOIN core.participants p
                            ON p.champion_id = c.champion_id
                        LEFT JOIN champion_roles cr
                            ON cr.champion_id = c.champion_id
                        GROUP BY c.champion_id, c.name, c.version, c.image_full, cr.primary_role
                        ORDER BY games DESC, champion_name ASC
                        """,
                (rs, rowNum) -> new ChampionStatDto(
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("image_url"),
                        getLong(rs, "games"),
                        getLong(rs, "wins"),
                        getDouble(rs, "winrate"),
                        rs.getString("primary_role")
                ),
                DATA_DRAGON_BASE_URL
        );
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

        List<PlayerRecentMatchRow> rows = jdbcTemplate.query("""
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
                            p.participant_id,
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
                (rs, rowNum) -> new PlayerRecentMatchRow(
                        rs.getString("match_id"),
                        getInteger(rs, "participant_id"),
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

        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, List<PlayerMatchItemDto>> itemsByMatchParticipant = loadRecentMatchItems(rows);
        List<PlayerRecentMatchDto> matches = new ArrayList<>();

        for (PlayerRecentMatchRow row : rows) {
            matches.add(new PlayerRecentMatchDto(
                    row.matchId(),
                    row.championId(),
                    row.championName(),
                    row.championImageUrl(),
                    row.win(),
                    row.kills(),
                    row.deaths(),
                    row.assists(),
                    row.queueId(),
                    row.gameVersion(),
                    row.gameCreationMs(),
                    row.gameDurationMs(),
                    itemsByMatchParticipant.getOrDefault(row.key(), List.of())
            ));
        }

        return matches;
    }

    @Override
    public MatchDetailsDto getMatchDetails(String matchId, String puuid) {
        MatchSummaryDto match = getMatchSummary(matchId);
        List<MatchParticipantRow> participantRows = getMatchParticipantRows(matchId);

        if (participantRows.isEmpty()) {
            throw new NotFoundException("Match not found: " + matchId);
        }

        Map<Integer, List<PlayerMatchItemDto>> finalItems = loadMatchFinalItems(matchId);
        Map<Integer, List<MatchParticipantRuneDto>> runes = loadMatchRunes(matchId);
        Map<Integer, List<MatchParticipantSkillOrderDto>> skillOrder = loadMatchSkillOrder(matchId);
        Map<Integer, List<MatchParticipantItemEventDto>> itemEvents = loadMatchItemEvents(matchId);

        List<MatchParticipantDto> participants = new ArrayList<>();

        for (MatchParticipantRow row : participantRows) {
            participants.add(new MatchParticipantDto(
                    row.matchId(),
                    row.participantId(),
                    row.puuid(),
                    row.gameName(),
                    row.tagLine(),
                    row.championId(),
                    row.championName(),
                    row.championImageUrl(),
                    row.teamId(),
                    row.win(),
                    row.kills(),
                    row.deaths(),
                    row.assists(),
                    calculateKda(row.kills(), row.deaths(), row.assists()),
                    row.champLevel(),
                    row.goldEarned(),
                    row.totalDamageToChampions(),
                    row.totalDamageTaken(),
                    row.visionScore(),
                    row.wardsPlaced(),
                    row.wardsKilled(),
                    row.totalMinionsKilled(),
                    row.neutralMinionsKilled(),
                    row.summoner1Id(),
                    row.summoner2Id(),
                    finalItems.getOrDefault(row.participantId(), List.of()),
                    runes.getOrDefault(row.participantId(), List.of()),
                    skillOrder.getOrDefault(row.participantId(), List.of()),
                    itemEvents.getOrDefault(row.participantId(), List.of())
            ));
        }

        MatchParticipantDto selectedParticipant = selectParticipant(participants, puuid);
        List<MatchTeamDto> teams = buildMatchTeams(participants);
        List<MatchTimelineEventDto> timelineEvents = loadMatchTimelineEvents(matchId);
        MatchMetricsDto metrics = new MatchMetricsDto("Metrics charts will be added later.");

        return new MatchDetailsDto(
                match,
                selectedParticipant,
                participants,
                teams,
                timelineEvents,
                metrics
        );
    }

    private Map<String, List<PlayerMatchItemDto>> loadRecentMatchItems(List<PlayerRecentMatchRow> matches) {
        List<String> matchIds = new ArrayList<>();

        for (PlayerRecentMatchRow match : matches) {
            matchIds.add(match.matchId());
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(matchIds.size(), "?"));
        Object[] params = new Object[matchIds.size() + 1];
        params[0] = DATA_DRAGON_BASE_URL;

        for (int i = 0; i < matchIds.size(); i++) {
            params[i + 1] = matchIds.get(i);
        }

        Map<String, List<PlayerMatchItemDto>> itemsByMatchParticipant = new LinkedHashMap<>();

        try {
            jdbcTemplate.query("""
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
                            )
                            SELECT
                                i.match_id,
                                i.participant_id,
                                i.item_id,
                                i.item_slot,
                                COALESCE(li.name, CONCAT('Item ', i.item_id)) AS item_name,
                                CASE
                                    WHEN COALESCE(li.version, liv.version) IS NULL THEN NULL
                                    ELSE CONCAT(?, '/', COALESCE(li.version, liv.version), '/img/item/', i.item_id,
                                        '.png')
                                END AS image_url
                            FROM core.participant_final_items i
                            LEFT JOIN latest_items li
                                ON li.item_id = i.item_id
                               AND li.rn = 1
                            CROSS JOIN latest_item_version liv
                            WHERE i.match_id IN (""" + placeholders + ") " + """
                              AND i.item_id IS NOT NULL
                              AND i.item_id > 0
                            ORDER BY i.match_id, i.participant_id, i.item_slot ASC
                            """,
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                        String key = buildMatchParticipantKey(
                                rs.getString("match_id"),
                                getInteger(rs, "participant_id")
                        );
                        PlayerMatchItemDto item = new PlayerMatchItemDto(
                                getInteger(rs, "item_id"),
                                rs.getString("item_name"),
                                rs.getString("image_url"),
                                getInteger(rs, "item_slot")
                        );

                        itemsByMatchParticipant.computeIfAbsent(key, unused -> new ArrayList<>()).add(item);
                    },
                    params
            );
        } catch (DataAccessException ex) {
            return Map.of();
        }

        return itemsByMatchParticipant;
    }

    private MatchSummaryDto getMatchSummary(String matchId) {
        List<MatchSummaryDto> matches = jdbcTemplate.query("""
                        SELECT
                            match_id,
                            queue_id,
                            game_version,
                            game_creation_ms,
                            game_duration_ms
                        FROM core.matches
                        WHERE match_id = ?
                        """,
                (rs, rowNum) -> {
                    String gameVersion = rs.getString("game_version");
                    Long durationMs = getLong(rs, "game_duration_ms");
                    Double durationMinutes = durationMs == null ? null
                            : Math.round((durationMs / 60000.0) * 10.0) / 10.0;

                    return new MatchSummaryDto(
                            rs.getString("match_id"),
                            getInteger(rs, "queue_id"),
                            formatQueueName(getInteger(rs, "queue_id")),
                            gameVersion,
                            formatPatchVersion(gameVersion),
                            getLong(rs, "game_creation_ms"),
                            durationMs,
                            durationMinutes
                    );
                },
                matchId
        );

        if (matches.isEmpty()) {
            throw new NotFoundException("Match not found: " + matchId);
        }

        return matches.get(0);
    }

    private List<MatchParticipantRow> getMatchParticipantRows(String matchId) {
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
                            p.participant_id,
                            p.puuid,
                            COALESCE(pl.game_name, 'Unknown') AS game_name,
                            COALESCE(pl.tag_line, '') AS tag_line,
                            p.champion_id,
                            COALESCE(p.champion_name, 'Unknown') AS champion_name,
                            CASE
                                WHEN lc.image_full IS NULL THEN NULL
                                ELSE CONCAT(?, '/', lc.version, '/img/champion/', lc.image_full)
                            END AS champion_image_url,
                            p.team_id,
                            p.win,
                            p.kills,
                            p.deaths,
                            p.assists,
                            p.champ_level,
                            p.gold_earned,
                            p.total_damage_to_champions,
                            p.total_damage_taken,
                            p.vision_score,
                            p.wards_placed,
                            p.wards_killed,
                            p.total_minions_killed,
                            p.neutral_minions_killed,
                            p.summoner1_id,
                            p.summoner2_id
                        FROM core.participants p
                        LEFT JOIN raw.players pl
                            ON pl.puuid = p.puuid
                        LEFT JOIN latest_champions lc
                            ON lc.champion_id = p.champion_id
                           AND lc.rn = 1
                        WHERE p.match_id = ?
                        ORDER BY p.team_id ASC, p.win DESC, p.kills DESC, p.assists DESC, p.participant_id ASC
                        """,
                (rs, rowNum) -> new MatchParticipantRow(
                        rs.getString("match_id"),
                        getInteger(rs, "participant_id"),
                        rs.getString("puuid"),
                        rs.getString("game_name"),
                        rs.getString("tag_line"),
                        getInteger(rs, "champion_id"),
                        rs.getString("champion_name"),
                        rs.getString("champion_image_url"),
                        getInteger(rs, "team_id"),
                        getBoolean(rs, "win"),
                        getInteger(rs, "kills"),
                        getInteger(rs, "deaths"),
                        getInteger(rs, "assists"),
                        getInteger(rs, "champ_level"),
                        getInteger(rs, "gold_earned"),
                        getInteger(rs, "total_damage_to_champions"),
                        getInteger(rs, "total_damage_taken"),
                        getInteger(rs, "vision_score"),
                        getInteger(rs, "wards_placed"),
                        getInteger(rs, "wards_killed"),
                        getInteger(rs, "total_minions_killed"),
                        getInteger(rs, "neutral_minions_killed"),
                        getInteger(rs, "summoner1_id"),
                        getInteger(rs, "summoner2_id")
                ),
                DATA_DRAGON_BASE_URL,
                matchId
        );
    }

    private Map<Integer, List<PlayerMatchItemDto>> loadMatchFinalItems(String matchId) {
        Map<Integer, List<PlayerMatchItemDto>> itemsByParticipant = new LinkedHashMap<>();

        try {
            jdbcTemplate.query("""
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
                            )
                            SELECT
                                i.participant_id,
                                i.item_id,
                                i.item_slot,
                                COALESCE(li.name, CONCAT('Item ', i.item_id)) AS item_name,
                                CASE
                                    WHEN COALESCE(li.version, liv.version) IS NULL THEN NULL
                                    ELSE CONCAT(?, '/', COALESCE(li.version, liv.version), '/img/item/', i.item_id,
                                        '.png')
                                END AS image_url
                            FROM core.participant_final_items i
                            LEFT JOIN latest_items li
                                ON li.item_id = i.item_id
                               AND li.rn = 1
                            CROSS JOIN latest_item_version liv
                            WHERE i.match_id = ?
                              AND i.item_id IS NOT NULL
                              AND i.item_id > 0
                            ORDER BY i.participant_id ASC, i.item_slot ASC
                            """,
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> itemsByParticipant.computeIfAbsent(
                            getInteger(rs, "participant_id"),
                            unused -> new ArrayList<>()
                    ).add(new PlayerMatchItemDto(
                            getInteger(rs, "item_id"),
                            rs.getString("item_name"),
                            rs.getString("image_url"),
                            getInteger(rs, "item_slot")
                    )),
                    DATA_DRAGON_BASE_URL,
                    matchId
            );
        } catch (DataAccessException ex) {
            return Map.of();
        }

        return itemsByParticipant;
    }

    private Map<Integer, List<MatchParticipantRuneDto>> loadMatchRunes(String matchId) {
        Map<Integer, List<MatchParticipantRuneDto>> runesByParticipant = new LinkedHashMap<>();

        try {
            jdbcTemplate.query("""
                            WITH latest_runes AS (
                                SELECT
                                    rune_id,
                                    version,
                                    name,
                                    icon,
                                    ROW_NUMBER() OVER (
                                        PARTITION BY rune_id
                                        ORDER BY version DESC
                                    ) AS rn
                                FROM static.runes
                            ),
                            latest_styles AS (
                                SELECT
                                    style_id,
                                    version,
                                    name,
                                    icon,
                                    ROW_NUMBER() OVER (
                                        PARTITION BY style_id
                                        ORDER BY version DESC
                                    ) AS rn
                                FROM static.rune_styles
                            )
                            SELECT
                                r.participant_id,
                                r.style_id,
                                r.style_type,
                                ls.name AS style_name,
                                CASE
                                    WHEN ls.icon IS NULL THEN NULL
                                    ELSE CONCAT(?, '/', ls.version, '/img/', ls.icon)
                                END AS style_icon_url,
                                r.rune_id,
                                lr.name AS rune_name,
                                CASE
                                    WHEN lr.icon IS NULL THEN NULL
                                    ELSE CONCAT(?, '/', lr.version, '/img/', lr.icon)
                                END AS rune_icon_url,
                                r.rune_slot,
                                r.selection_order,
                                r.is_keystone
                            FROM core.participant_rune_selections r
                            LEFT JOIN latest_runes lr
                                ON lr.rune_id = r.rune_id
                               AND lr.rn = 1
                            LEFT JOIN latest_styles ls
                                ON ls.style_id = r.style_id
                               AND ls.rn = 1
                            WHERE r.match_id = ?
                            ORDER BY r.participant_id ASC, r.style_type ASC, r.rune_slot ASC, r.selection_order ASC
                            """,
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> runesByParticipant.computeIfAbsent(
                            getInteger(rs, "participant_id"),
                            unused -> new ArrayList<>()
                    ).add(new MatchParticipantRuneDto(
                            getInteger(rs, "style_id"),
                            rs.getString("style_type"),
                            rs.getString("style_name"),
                            rs.getString("style_icon_url"),
                            getInteger(rs, "rune_id"),
                            rs.getString("rune_name"),
                            rs.getString("rune_icon_url"),
                            getInteger(rs, "rune_slot"),
                            getInteger(rs, "selection_order"),
                            getBoolean(rs, "is_keystone")
                    )),
                    DATA_DRAGON_BASE_URL,
                    DATA_DRAGON_BASE_URL,
                    matchId
            );
        } catch (DataAccessException ex) {
            return Map.of();
        }

        return runesByParticipant;
    }

    private Map<Integer, List<MatchParticipantSkillOrderDto>> loadMatchSkillOrder(String matchId) {
        Map<Integer, List<MatchParticipantSkillOrderDto>> skillOrderByParticipant = new LinkedHashMap<>();

        try {
            jdbcTemplate.query("""
                            SELECT
                                participant_id,
                                skill_order,
                                skill_slot,
                                level_up_type,
                                timestamp_ms,
                                minute
                            FROM core.participant_skill_order
                            WHERE match_id = ?
                            ORDER BY participant_id ASC, skill_order ASC
                            """,
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> skillOrderByParticipant.computeIfAbsent(
                            getInteger(rs, "participant_id"),
                            unused -> new ArrayList<>()
                    ).add(new MatchParticipantSkillOrderDto(
                            getInteger(rs, "skill_order"),
                            getInteger(rs, "skill_slot"),
                            rs.getString("level_up_type"),
                            getLong(rs, "timestamp_ms"),
                            getInteger(rs, "minute")
                    )),
                    matchId
            );
        } catch (DataAccessException ex) {
            return Map.of();
        }

        return skillOrderByParticipant;
    }

    private Map<Integer, List<MatchParticipantItemEventDto>> loadMatchItemEvents(String matchId) {
        Map<Integer, List<MatchParticipantItemEventDto>> eventsByParticipant = new LinkedHashMap<>();

        try {
            jdbcTemplate.query("""
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
                            )
                            SELECT
                                e.participant_id,
                                e.event_type,
                                e.item_id,
                                COALESCE(li.name, CONCAT('Item ', e.item_id)) AS item_name,
                                CASE
                                    WHEN COALESCE(li.version, liv.version) IS NULL THEN NULL
                                    ELSE CONCAT(?, '/', COALESCE(li.version, liv.version), '/img/item/', e.item_id,
                                        '.png')
                                END AS image_url,
                                e.timestamp_ms,
                                e.minute
                            FROM core.participant_item_events e
                            LEFT JOIN latest_items li
                                ON li.item_id = e.item_id
                               AND li.rn = 1
                            CROSS JOIN latest_item_version liv
                            WHERE e.match_id = ?
                              AND e.event_type = 'ITEM_PURCHASED'
                            ORDER BY e.participant_id ASC, e.timestamp_ms ASC
                            """,
                    (org.springframework.jdbc.core.RowCallbackHandler) rs -> eventsByParticipant.computeIfAbsent(
                            getInteger(rs, "participant_id"),
                            unused -> new ArrayList<>()
                    ).add(new MatchParticipantItemEventDto(
                            rs.getString("event_type"),
                            getInteger(rs, "item_id"),
                            rs.getString("item_name"),
                            rs.getString("image_url"),
                            getLong(rs, "timestamp_ms"),
                            getInteger(rs, "minute")
                    )),
                    DATA_DRAGON_BASE_URL,
                    matchId
            );
        } catch (DataAccessException ex) {
            return Map.of();
        }

        return eventsByParticipant;
    }

    private List<MatchTimelineEventDto> loadMatchTimelineEvents(String matchId) {
        try {
            return jdbcTemplate.query("""
                            WITH latest_items AS (
                                SELECT
                                    item_id,
                                    name,
                                    ROW_NUMBER() OVER (
                                        PARTITION BY item_id
                                        ORDER BY version DESC
                                    ) AS rn
                                FROM static.items
                            )
                            SELECT
                                ts_ms,
                                CAST(ts_ms / 60000 AS integer) AS minute,
                                type,
                                participant_id,
                                killer_id,
                                victim_id,
                                e.item_id,
                                li.name AS item_name,
                                CAST(e.position ->> 'x' AS integer) AS pos_x,
                                CAST(e.position ->> 'y' AS integer) AS pos_y
                            FROM core.timeline_events e
                            LEFT JOIN latest_items li
                                ON li.item_id = e.item_id
                               AND li.rn = 1
                            WHERE match_id = ?
                              AND type IN (
                                  'CHAMPION_KILL',
                                  'ELITE_MONSTER_KILL',
                                  'BUILDING_KILL',
                                  'ITEM_PURCHASED',
                                  'WARD_PLACED',
                                  'WARD_KILL'
                              )
                            ORDER BY ts_ms ASC, event_id ASC
                            """,
                    (rs, rowNum) -> new MatchTimelineEventDto(
                            getLong(rs, "ts_ms"),
                            getInteger(rs, "minute"),
                            rs.getString("type"),
                            getInteger(rs, "participant_id"),
                            getInteger(rs, "killer_id"),
                            getInteger(rs, "victim_id"),
                            getInteger(rs, "item_id"),
                            rs.getString("item_name"),
                            buildMatchTimelinePosition(
                                    getInteger(rs, "pos_x"),
                                    getInteger(rs, "pos_y")
                            )
                    ),
                    matchId
            );
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private MatchTimelinePositionDto buildMatchTimelinePosition(Integer x, Integer y) {
        if (x == null || y == null) {
            return null;
        }

        return new MatchTimelinePositionDto(x, y);
    }

    private MatchParticipantDto selectParticipant(List<MatchParticipantDto> participants, String puuid) {
        if (puuid != null && !puuid.isBlank()) {
            for (MatchParticipantDto participant : participants) {
                if (puuid.equals(participant.puuid())) {
                    return participant;
                }
            }
        }

        return participants.get(0);
    }

    private List<MatchTeamDto> buildMatchTeams(List<MatchParticipantDto> participants) {
        Map<Integer, List<MatchParticipantDto>> grouped = new LinkedHashMap<>();

        for (MatchParticipantDto participant : participants) {
            grouped.computeIfAbsent(participant.teamId(), unused -> new ArrayList<>()).add(participant);
        }

        List<MatchTeamDto> teams = new ArrayList<>();

        for (Map.Entry<Integer, List<MatchParticipantDto>> entry : grouped.entrySet()) {
            List<MatchParticipantDto> teamParticipants = new ArrayList<>(entry.getValue());
            teamParticipants.sort(Comparator.comparingInt(
                    participant -> participant.participantId() == null ? Integer.MAX_VALUE : participant.participantId()
            ));
            Boolean win = teamParticipants.isEmpty() ? null : teamParticipants.get(0).win();
            teams.add(new MatchTeamDto(
                    entry.getKey(),
                    formatTeamName(entry.getKey()),
                    win,
                    teamParticipants
            ));
        }

        teams.sort(Comparator.comparingInt(team -> team.teamId() == null ? Integer.MAX_VALUE : team.teamId()));
        return teams;
    }

    private Double calculateKda(Integer kills, Integer deaths, Integer assists) {
        double numerator = (kills == null ? 0 : kills) + (assists == null ? 0 : assists);

        if (deaths == null || deaths == 0) {
            return numerator;
        }

        return Math.round((numerator / deaths) * 100.0) / 100.0;
    }

    private String formatQueueName(Integer queueId) {
        if (queueId == null) {
            return "Unknown queue";
        }

        return switch (queueId) {
            case 400 -> "Normal Draft";
            case 420 -> "Ranked Solo/Duo";
            case 430 -> "Normal Blind";
            case 440 -> "Ranked Flex";
            case 450 -> "ARAM";
            case 700 -> "Clash";
            case 720 -> "ARAM Clash";
            case 830 -> "Co-op Intro";
            case 840 -> "Co-op Beginner";
            case 850 -> "Co-op Intermediate";
            case 900 -> "URF";
            case 1020 -> "One for All";
            case 1300 -> "Nexus Blitz";
            case 1400 -> "Ultimate Spellbook";
            case 1700, 1710 -> "Arena";
            default -> "Queue " + queueId;
        };
    }

    private String formatPatchVersion(String gameVersion) {
        if (gameVersion == null || gameVersion.isBlank()) {
            return "-";
        }

        String[] parts = gameVersion.split("\\.");

        if (parts.length < 2) {
            return gameVersion;
        }

        return parts[0] + "." + parts[1];
    }

    private String formatTeamName(Integer teamId) {
        if (teamId != null && teamId == 100) {
            return "Blue Team";
        }

        if (teamId != null && teamId == 200) {
            return "Red Team";
        }

        return "Team " + teamId;
    }

    private record MatchParticipantRow(
            String matchId,
            Integer participantId,
            String puuid,
            String gameName,
            String tagLine,
            Integer championId,
            String championName,
            String championImageUrl,
            Integer teamId,
            Boolean win,
            Integer kills,
            Integer deaths,
            Integer assists,
            Integer champLevel,
            Integer goldEarned,
            Integer totalDamageToChampions,
            Integer totalDamageTaken,
            Integer visionScore,
            Integer wardsPlaced,
            Integer wardsKilled,
            Integer totalMinionsKilled,
            Integer neutralMinionsKilled,
            Integer summoner1Id,
            Integer summoner2Id
    ) {
    }

    private List<ChampionAbilityDto> getChampionAbilities(Integer championId) {
        List<ChampionAbilityDto> abilities = getChampionAbilitiesFromRawJson(championId);

        if (!abilities.isEmpty()) {
            return abilities;
        }

        return getChampionAbilitiesFromStaticTable(championId);
    }

    private List<ChampionAbilityDto> getChampionAbilitiesFromStaticTable(Integer championId) {
        try {
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
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<ChampionAbilityDto> getChampionAbilitiesFromRawJson(Integer championId) {
        return jdbcTemplate.query("""
                        WITH latest_champion AS (
                            SELECT
                                champion_id,
                                version,
                                raw_json
                            FROM static.champions
                            WHERE champion_id = ?
                            ORDER BY version DESC
                            LIMIT 1
                        ),
                        passive_ability AS (
                            SELECT
                                'PASSIVE' AS ability_key,
                                lc.raw_json -> 'passive' ->> 'name' AS ability_name,
                                COALESCE(
                                    lc.raw_json -> 'passive' ->> 'description',
                                    lc.raw_json -> 'passive' ->> 'sanitizedDescription'
                                ) AS ability_description,
                                CASE
                                    WHEN lc.raw_json -> 'passive' -> 'image' ->> 'full' IS NULL THEN NULL
                                    ELSE CONCAT(
                                        ?,
                                        '/',
                                        lc.version,
                                        '/img/passive/',
                                        lc.raw_json -> 'passive' -> 'image' ->> 'full'
                                    )
                                END AS image_url,
                                0 AS sort_order
                            FROM latest_champion lc
                        ),
                        spell_abilities AS (
                            SELECT
                                CASE spell.ordinality
                                    WHEN 1 THEN 'Q'
                                    WHEN 2 THEN 'W'
                                    WHEN 3 THEN 'E'
                                    WHEN 4 THEN 'R'
                                    ELSE COALESCE(
                                        NULLIF(spell.spell_json ->> 'id', ''),
                                        NULLIF(spell.spell_json ->> 'key', ''),
                                        CONCAT('SPELL_', spell.ordinality)
                                    )
                                END AS ability_key,
                                spell.spell_json ->> 'name' AS ability_name,
                                COALESCE(
                                    spell.spell_json ->> 'description',
                                    spell.spell_json ->> 'tooltip'
                                ) AS ability_description,
                                CASE
                                    WHEN spell.spell_json -> 'image' ->> 'full' IS NULL THEN NULL
                                    ELSE CONCAT(
                                        ?,
                                        '/',
                                        lc.version,
                                        '/img/spell/',
                                        spell.spell_json -> 'image' ->> 'full'
                                    )
                                END AS image_url,
                                spell.ordinality AS sort_order
                            FROM latest_champion lc
                            CROSS JOIN LATERAL jsonb_array_elements(
                                COALESCE(lc.raw_json -> 'spells', '[]'::jsonb)
                            ) WITH ORDINALITY AS spell(spell_json, ordinality)
                        )
                        SELECT
                            ability_key,
                            ability_name,
                            ability_description,
                            image_url
                        FROM (
                            SELECT *
                            FROM passive_ability
                            UNION ALL
                            SELECT *
                            FROM spell_abilities
                        ) abilities
                        WHERE ability_name IS NOT NULL
                        ORDER BY sort_order
                        """,
                (rs, rowNum) -> new ChampionAbilityDto(
                        rs.getString("ability_key"),
                        rs.getString("ability_name"),
                        rs.getString("ability_description"),
                        rs.getString("image_url")
                ),
                championId,
                DATA_DRAGON_BASE_URL,
                DATA_DRAGON_BASE_URL
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

    @Override
    public PlayerLeaderboardResponseDto getPlayerLeaderboards() {
        String query = """
                SELECT
                    puuid,
                    game_name,
                    tag_line,
                    profile_icon_id,
                    profile_icon_url,
                    matches,
                    wins,
                    winrate,
                    average_kills,
                    average_deaths,
                    average_assists
                FROM analyzed.player_leaderboard_stats
                %s
                ORDER BY %s
                LIMIT 20
                """;
        try {
            List<PlayerLeaderboardDto> bestPlayers = jdbcTemplate.query(
                    query.formatted("WHERE matches >= 10", "winrate DESC, matches DESC, game_name ASC"),
                    (rs, rowNum) -> mapPlayerLeaderboardRow(rs)
            );

            List<PlayerLeaderboardDto> mostActivePlayers = jdbcTemplate.query(
                    query.formatted("", "matches DESC, winrate DESC, game_name ASC"),
                    (rs, rowNum) -> mapPlayerLeaderboardRow(rs)
            );

            return new PlayerLeaderboardResponseDto(bestPlayers, mostActivePlayers);
        } catch (DataAccessException ex) {
            throw new IllegalStateException(
                    "Leaderboard view analyzed.player_leaderboard_stats is unavailable. "
                            + "Refresh or create the materialized view before calling /api/players/leaderboard.",
                    ex
            );
        }
    }

    private PlayerLeaderboardDto mapPlayerLeaderboardRow(java.sql.ResultSet rs)
            throws java.sql.SQLException {
        return new PlayerLeaderboardDto(
                rs.getString("puuid"),
                rs.getString("game_name"),
                rs.getString("tag_line"),
                getInteger(rs, "profile_icon_id"),
                rs.getString("profile_icon_url"),
                getLong(rs, "matches"),
                getLong(rs, "wins"),
                getDouble(rs, "winrate"),
                getDouble(rs, "average_kills"),
                getDouble(rs, "average_deaths"),
                getDouble(rs, "average_assists")
        );
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String buildMatchParticipantKey(String matchId, Integer participantId) {
        return matchId + ":" + participantId;
    }

    private record PlayerRecentMatchRow(
            String matchId,
            Integer participantId,
            Integer championId,
            String championName,
            String championImageUrl,
            Boolean win,
            Integer kills,
            Integer deaths,
            Integer assists,
            Integer queueId,
            String gameVersion,
            Long gameCreationMs,
            Long gameDurationMs
    ) {
        private String key() {
            return matchId + ":" + participantId;
        }
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
