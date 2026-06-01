package org.main.service.frontend;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.main.dto.frontend.PlayerMatchItemDto;
import org.main.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchDetailsServiceImpl implements MatchDetailsService {

    private static final Logger log = LoggerFactory.getLogger(MatchDetailsServiceImpl.class);

    private static final String DATA_DRAGON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn";

    private final JdbcTemplate jdbcTemplate;

    public MatchDetailsServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                                    ELSE CONCAT(?, '/img/', ls.icon)
                                END AS style_icon_url,
                                r.rune_id,
                                lr.name AS rune_name,
                                CASE
                                    WHEN lr.icon IS NULL THEN NULL
                                    ELSE CONCAT(?, '/img/', lr.icon)
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
                                e.ward_type,
                                e.building_type,
                                e.lane_type,
                                CAST(e.position ->> 'x' AS integer) AS pos_x,
                                CAST(e.position ->> 'y' AS integer) AS pos_y
                            FROM core.timeline_events e
                            LEFT JOIN latest_items li
                                ON li.item_id = e.item_id
                               AND li.rn = 1
                            WHERE match_id = ?
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
                            rs.getString("ward_type"),
                            rs.getString("building_type"),
                            rs.getString("lane_type"),
                            buildMatchTimelinePosition(
                                    getInteger(rs, "pos_x"),
                                    getInteger(rs, "pos_y")
                            )
                    ),
                    matchId
            );
        } catch (DataAccessException ex) {
            log.warn("Could not load timeline events for match {}", matchId, ex);
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

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        boolean value = rs.getBoolean(columnName);
        return rs.wasNull() ? null : value;
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
}
