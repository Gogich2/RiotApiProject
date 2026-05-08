package org.main.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MatchAnalysisServiceImpl implements MatchAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(MatchAnalysisServiceImpl.class);

    private record TimelineSkillEvent(
            String matchId,
            short participantId,
            short skillSlot,
            String levelUpType,
            Long timestampMs
    ) {
    }

    private static final List<String> ITEM_EVENT_TYPES = List.of(
            "ITEM_PURCHASED",
            "ITEM_SOLD",
            "ITEM_DESTROYED",
            "ITEM_UNDO"
    );

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    private final TransactionTemplate transactionTemplate;

    public MatchAnalysisServiceImpl(JdbcTemplate jdbcTemplate,
                                    ObjectMapper objectMapper,
                                    TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public int processNewMatches(int limit) {
        int safeLimit = limit <= 0 ? 50 : Math.min(limit, 200);

        List<String> matchIds = jdbcTemplate.queryForList("""
                SELECT m.match_id
                FROM raw.matches m
                WHERE COALESCE(m.analysis_status, 'NEW') IN ('NEW', 'WAITING_FOR_TIMELINE')
                  AND m.raw_match_json IS NOT NULL
                ORDER BY m.fetched_at ASC
                LIMIT ?
                """, String.class, safeLimit);

        int processed = 0;

        for (String matchId : matchIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> processMatchInsideTransaction(matchId));
                processed++;
            } catch (Exception ex) {
                markFailed(matchId, ex);
                log.error("Match analysis failed: matchId='{}'", matchId, ex);
            }
        }

        return processed;
    }

    @Override
    public void processMatch(String matchId) {
        transactionTemplate.executeWithoutResult(status -> processMatchInsideTransaction(matchId));
    }

    private TimelineSkillEvent mapTimelineSkillEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TimelineSkillEvent(
                rs.getString("match_id"),
                rs.getShort("participant_id"),
                rs.getShort("skill_slot"),
                rs.getString("level_up_type"),
                getLong(rs, "ts_ms")
        );
    }

    private void processMatchInsideTransaction(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId is required");
        }

        if (!hasTimelineEvents(matchId)) {
            markWaitingForTimeline(matchId);
            log.info("Match waits for timeline before analysis: matchId='{}'", matchId);
            return;
        }

        markProcessing(matchId);

        MatchSource source = loadMatchSource(matchId);
        JsonNode matchJson = readJson(source.rawMatchJson());
        JsonNode info = matchJson.path("info");
        JsonNode participants = info.path("participants");

        if (!participants.isArray() || participants.isEmpty()) {
            throw new IllegalStateException("Match has no participants: " + matchId);
        }

        savePlayersFromParticipants(participants);

        deletePreviousAnalysis(matchId);
        saveCoreMatch(matchId);

        Map<Short, ParticipantContext> participantContextById = new HashMap<>();

        for (JsonNode participant : participants) {
            ParticipantContext context = buildParticipantContext(source, participant);
            participantContextById.put(context.participantId(), context);

            saveCoreParticipant(context, participant);

            savePlayerMatchStats(context, participant);
            saveFinalItems(context, participant);
            saveLoadout(context, participant);
            saveRuneSelections(context, participant);
        }

        saveItemEvents(source, participantContextById);
        saveSkillOrder(source, participantContextById);

        markAnalyzed(matchId);

        log.info("Match analyzed successfully: matchId='{}'", matchId);
    }

    private MatchSource loadMatchSource(String matchId) {
        return jdbcTemplate.queryForObject("""
                SELECT match_id,
                       raw_match_json::text AS raw_match_json,
                       game_version,
                       queue_id,
                       game_duration_ms
                FROM raw.matches
                WHERE match_id = ?
                """, this::mapMatchSource, matchId);
    }

    private MatchSource mapMatchSource(ResultSet rs, int rowNum) throws SQLException {
        return new MatchSource(
                rs.getString("match_id"),
                rs.getString("raw_match_json"),
                rs.getString("game_version"),
                getInteger(rs, "queue_id"),
                getLong(rs, "game_duration_ms")
        );
    }

    private ParticipantContext buildParticipantContext(MatchSource source, JsonNode participant) {
        short participantId = (short) participant.path("participantId").asInt();

        String puuid = nullableText(participant, "puuid");
        Integer championId = nullableInteger(participant, "championId");
        Integer teamId = nullableInteger(participant, "teamId");
        Boolean win = nullableBoolean(participant, "win");

        String gameVersion = source.gameVersion();
        if (gameVersion == null || gameVersion.isBlank()) {
            gameVersion = nullableText(participant, "gameVersion");
        }

        return new ParticipantContext(
                source.matchId(),
                participantId,
                puuid,
                championId,
                teamId,
                win,
                gameVersion,
                source.queueId(),
                source.gameDurationMs()
        );
    }

    private void savePlayersFromParticipants(JsonNode participants) {
        if (!participants.isArray()) {
            return;
        }

        for (JsonNode participant : participants) {
            String puuid = nullableText(participant, "puuid");

            if (puuid == null || puuid.isBlank()) {
                continue;
            }

            String gameName = nullableText(participant, "riotIdGameName");
            String tagLine = nullableText(participant, "riotIdTagline");

            jdbcTemplate.update("""
                    INSERT INTO raw.players
                    (
                        puuid,
                        game_name,
                        tag_line,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, now(), now())
                    ON CONFLICT (puuid) DO NOTHING
                    """,
                    puuid,
                    gameName,
                    tagLine
            );
        }
    }

    private void saveCoreMatch(String matchId) {
        jdbcTemplate.update("""
                INSERT INTO core.matches
                (
                    match_id,
                    region,
                    platform,
                    data_version,
                    game_creation_ms,
                    game_duration_ms,
                    game_version,
                    queue_id,
                    map_id,
                    game_mode,
                    game_type,
                    season_id,
                    tournament_code,
                    fetched_at
                )
                SELECT
                    match_id,
                    region,
                    platform,
                    data_version,
                    game_creation_ms,
                    game_duration_ms,
                    game_version,
                    queue_id,
                    map_id,
                    game_mode,
                    game_type,
                    season_id,
                    tournament_code,
                    fetched_at
                FROM raw.matches
                WHERE match_id = ?
                ON CONFLICT (match_id) DO UPDATE SET
                    region = EXCLUDED.region,
                    platform = EXCLUDED.platform,
                    data_version = EXCLUDED.data_version,
                    game_creation_ms = EXCLUDED.game_creation_ms,
                    game_duration_ms = EXCLUDED.game_duration_ms,
                    game_version = EXCLUDED.game_version,
                    queue_id = EXCLUDED.queue_id,
                    map_id = EXCLUDED.map_id,
                    game_mode = EXCLUDED.game_mode,
                    game_type = EXCLUDED.game_type,
                    season_id = EXCLUDED.season_id,
                    tournament_code = EXCLUDED.tournament_code,
                    fetched_at = EXCLUDED.fetched_at
                """,
                matchId
        );
    }

    private void saveCoreParticipant(ParticipantContext context, JsonNode participant) {
        jdbcTemplate.update("""
                INSERT INTO core.participants
                (
                    match_id,
                    participant_id,
                    puuid,
                    summoner_id,
                    team_id,
                    champion_id,
                    champion_name,
                    champ_level,
                    role,
                    lane,
                    team_position,
                    individual_position,
                    win,
                    kills,
                    deaths,
                    assists,
                    gold_earned,
                    gold_spent,
                    total_damage_to_champions,
                    total_damage_dealt,
                    total_damage_taken,
                    total_minions_killed,
                    neutral_minions_killed,
                    vision_score,
                    wards_placed,
                    wards_killed,
                    detector_wards_placed,
                    summoner1_id,
                    summoner2_id,
                    perks_json,
                    raw_participant_json,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb,
                        ?::jsonb, ?)
                ON CONFLICT (match_id, participant_id)
                DO UPDATE SET
                    puuid = EXCLUDED.puuid,
                    summoner_id = EXCLUDED.summoner_id,
                    team_id = EXCLUDED.team_id,
                    champion_id = EXCLUDED.champion_id,
                    champion_name = EXCLUDED.champion_name,
                    champ_level = EXCLUDED.champ_level,
                    role = EXCLUDED.role,
                    lane = EXCLUDED.lane,
                    team_position = EXCLUDED.team_position,
                    individual_position = EXCLUDED.individual_position,
                    win = EXCLUDED.win,
                    kills = EXCLUDED.kills,
                    deaths = EXCLUDED.deaths,
                    assists = EXCLUDED.assists,
                    gold_earned = EXCLUDED.gold_earned,
                    gold_spent = EXCLUDED.gold_spent,
                    total_damage_to_champions = EXCLUDED.total_damage_to_champions,
                    total_damage_dealt = EXCLUDED.total_damage_dealt,
                    total_damage_taken = EXCLUDED.total_damage_taken,
                    total_minions_killed = EXCLUDED.total_minions_killed,
                    neutral_minions_killed = EXCLUDED.neutral_minions_killed,
                    vision_score = EXCLUDED.vision_score,
                    wards_placed = EXCLUDED.wards_placed,
                    wards_killed = EXCLUDED.wards_killed,
                    detector_wards_placed = EXCLUDED.detector_wards_placed,
                    summoner1_id = EXCLUDED.summoner1_id,
                    summoner2_id = EXCLUDED.summoner2_id,
                    perks_json = EXCLUDED.perks_json,
                    raw_participant_json = EXCLUDED.raw_participant_json
                """,
                context.matchId(),
                context.participantId(),
                context.puuid(),
                nullableText(participant, "summonerId"),
                context.teamId(),
                context.championId(),
                nullableText(participant, "championName"),
                nullableInteger(participant, "champLevel"),
                nullableText(participant, "role"),
                nullableText(participant, "lane"),
                nullableText(participant, "teamPosition"),
                nullableText(participant, "individualPosition"),
                context.win(),
                nullableInteger(participant, "kills"),
                nullableInteger(participant, "deaths"),
                nullableInteger(participant, "assists"),
                nullableInteger(participant, "goldEarned"),
                nullableInteger(participant, "goldSpent"),
                nullableInteger(participant, "totalDamageDealtToChampions"),
                nullableInteger(participant, "totalDamageDealt"),
                nullableInteger(participant, "totalDamageTaken"),
                nullableInteger(participant, "totalMinionsKilled"),
                nullableInteger(participant, "neutralMinionsKilled"),
                nullableInteger(participant, "visionScore"),
                nullableInteger(participant, "wardsPlaced"),
                nullableInteger(participant, "wardsKilled"),
                nullableInteger(participant, "detectorWardsPlaced"),
                nullableInteger(participant, "summoner1Id"),
                nullableInteger(participant, "summoner2Id"),
                jsonOrNull(participant.get("perks")),
                participant.toString(),
                OffsetDateTime.now()
        );
    }

    private void saveFinalItems(ParticipantContext context, JsonNode participant) {
        for (int slot = 0; slot <= 6; slot++) {
            Integer itemId = nullableInteger(participant, "item" + slot);

            if (itemId == null || itemId == 0) {
                continue;
            }

            OffsetDateTime now = OffsetDateTime.now();

            jdbcTemplate.update("""
                    INSERT INTO core.participant_final_items
                    (
                        match_id,
                        participant_id,
                        item_slot,
                        item_id,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (match_id, participant_id, item_slot)
                    DO UPDATE SET
                        item_id = EXCLUDED.item_id,
                        created_at = EXCLUDED.created_at
                    """,
                    context.matchId(),
                    context.participantId(),
                    slot,
                    itemId,
                    now
            );

            jdbcTemplate.update("""
                    INSERT INTO analyzed.participant_final_items
                    (
                        match_id,
                        participant_id,
                        puuid,
                        champion_id,
                        team_id,
                        win,
                        item_id,
                        item_slot,
                        game_version,
                        queue_id,
                        game_duration_ms,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    context.matchId(),
                    context.participantId(),
                    context.puuid(),
                    context.championId(),
                    context.teamId(),
                    context.win(),
                    itemId,
                    slot,
                    context.gameVersion(),
                    context.queueId(),
                    context.gameDurationMs(),
                    now
            );
        }
    }

    private void saveLoadout(ParticipantContext context, JsonNode participant) {
        JsonNode perks = participant.path("perks");

        RuneLoadout loadout = parseRuneLoadout(perks);

        jdbcTemplate.update("""
                INSERT INTO analyzed.participant_loadouts
                (
                    match_id,
                    participant_id,
                    puuid,
                    champion_id,
                    team_id,
                    win,
                    primary_style_id,
                    secondary_style_id,
                    keystone_id,
                    primary_rune_1_id,
                    primary_rune_2_id,
                    primary_rune_3_id,
                    secondary_rune_1_id,
                    secondary_rune_2_id,
                    stat_offense_id,
                    stat_flex_id,
                    stat_defense_id,
                    summoner1_id,
                    summoner2_id,
                    game_version,
                    queue_id,
                    game_duration_ms,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                context.matchId(),
                context.participantId(),
                context.puuid(),
                context.championId(),
                context.teamId(),
                context.win(),
                loadout.primaryStyleId(),
                loadout.secondaryStyleId(),
                loadout.keystoneId(),
                loadout.primaryRune1Id(),
                loadout.primaryRune2Id(),
                loadout.primaryRune3Id(),
                loadout.secondaryRune1Id(),
                loadout.secondaryRune2Id(),
                loadout.statOffenseId(),
                loadout.statFlexId(),
                loadout.statDefenseId(),
                nullableInteger(participant, "summoner1Id"),
                nullableInteger(participant, "summoner2Id"),
                context.gameVersion(),
                context.queueId(),
                context.gameDurationMs(),
                OffsetDateTime.now()
        );
    }

    private void saveRuneSelections(ParticipantContext context, JsonNode participant) {
        JsonNode styles = participant.path("perks").path("styles");

        if (!styles.isArray()) {
            return;
        }

        for (JsonNode style : styles) {
            Integer styleId = nullableInteger(style, "style");
            String description = nullableText(style, "description");

            String styleType = "UNKNOWN";
            if ("primaryStyle".equalsIgnoreCase(description)) {
                styleType = "PRIMARY";
            } else if ("subStyle".equalsIgnoreCase(description)) {
                styleType = "SECONDARY";
            }

            JsonNode selections = style.path("selections");

            if (!selections.isArray()) {
                continue;
            }

            int order = 0;

            for (JsonNode selection : selections) {
                Integer runeId = nullableInteger(selection, "perk");

                if (runeId == null || runeId == 0) {
                    order++;
                    continue;
                }

                boolean isKeystone = "PRIMARY".equals(styleType) && order == 0;
                OffsetDateTime now = OffsetDateTime.now();

                jdbcTemplate.update("""
                        INSERT INTO core.participant_rune_selections
                        (
                            match_id,
                            participant_id,
                            style_id,
                            style_type,
                            rune_id,
                            rune_slot,
                            selection_order,
                            is_keystone,
                            var1,
                            var2,
                            var3,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (match_id, participant_id, rune_id)
                        DO UPDATE SET
                            style_id = EXCLUDED.style_id,
                            style_type = EXCLUDED.style_type,
                            rune_slot = EXCLUDED.rune_slot,
                            selection_order = EXCLUDED.selection_order,
                            is_keystone = EXCLUDED.is_keystone,
                            var1 = EXCLUDED.var1,
                            var2 = EXCLUDED.var2,
                            var3 = EXCLUDED.var3,
                            created_at = EXCLUDED.created_at
                        """,
                        context.matchId(),
                        context.participantId(),
                        styleId,
                        styleType,
                        runeId,
                        order,
                        order,
                        isKeystone,
                        nullableInteger(selection, "var1"),
                        nullableInteger(selection, "var2"),
                        nullableInteger(selection, "var3"),
                        now
                );

                jdbcTemplate.update("""
                        INSERT INTO analyzed.participant_rune_selections
                        (
                            match_id,
                            participant_id,
                            puuid,
                            champion_id,
                            team_id,
                            win,
                            style_id,
                            style_type,
                            rune_id,
                            rune_slot,
                            selection_order,
                            is_keystone,
                            var1,
                            var2,
                            var3,
                            game_version,
                            queue_id,
                            game_duration_ms,
                            created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (match_id, participant_id, rune_id)
                        DO NOTHING
                        """,
                        context.matchId(),
                        context.participantId(),
                        context.puuid(),
                        context.championId(),
                        context.teamId(),
                        context.win(),
                        styleId,
                        styleType,
                        runeId,
                        order,
                        order,
                        isKeystone,
                        nullableInteger(selection, "var1"),
                        nullableInteger(selection, "var2"),
                        nullableInteger(selection, "var3"),
                        context.gameVersion(),
                        context.queueId(),
                        context.gameDurationMs(),
                        now
                );

                order++;
            }
        }
    }

    private void saveItemEvents(MatchSource source, Map<Short, ParticipantContext> participantContextById) {
        List<TimelineItemEvent> events = jdbcTemplate.query("""
                SELECT match_id,
                       participant_id,
                       item_id,
                       type,
                       ts_ms
                FROM raw.match_timeline_events
                WHERE match_id = ?
                  AND type IN ('ITEM_PURCHASED', 'ITEM_SOLD', 'ITEM_DESTROYED', 'ITEM_UNDO')
                  AND participant_id IS NOT NULL
                  AND item_id IS NOT NULL
                ORDER BY ts_ms ASC
                """, this::mapTimelineItemEvent, source.matchId());

        for (TimelineItemEvent event : events) {
            if (!ITEM_EVENT_TYPES.contains(event.eventType())) {
                continue;
            }

            ParticipantContext context = participantContextById.get(event.participantId());

            if (context == null) {
                continue;
            }

            Integer minute = event.timestampMs() == null ? null : (int) (event.timestampMs() / 60000);
            OffsetDateTime now = OffsetDateTime.now();

            jdbcTemplate.update("""
                    INSERT INTO core.participant_item_events
                    (
                        match_id,
                        participant_id,
                        item_id,
                        event_type,
                        timestamp_ms,
                        minute,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (match_id, participant_id, event_type, item_id, timestamp_ms)
                    DO NOTHING
                    """,
                    context.matchId(),
                    context.participantId(),
                    event.itemId(),
                    event.eventType(),
                    event.timestampMs(),
                    minute,
                    now
            );

            jdbcTemplate.update("""
                    INSERT INTO analyzed.participant_item_events
                    (
                        match_id,
                        participant_id,
                        puuid,
                        champion_id,
                        team_id,
                        win,
                        item_id,
                        event_type,
                        timestamp_ms,
                        minute,
                        game_version,
                        queue_id,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (match_id, participant_id, event_type, item_id, timestamp_ms)
                    DO NOTHING
                    """,
                    context.matchId(),
                    context.participantId(),
                    context.puuid(),
                    context.championId(),
                    context.teamId(),
                    context.win(),
                    event.itemId(),
                    event.eventType(),
                    event.timestampMs(),
                    minute,
                    context.gameVersion(),
                    context.queueId(),
                    now
            );
        }
    }

    private TimelineItemEvent mapTimelineItemEvent(ResultSet rs, int rowNum) throws SQLException {
        return new TimelineItemEvent(
                rs.getString("match_id"),
                rs.getShort("participant_id"),
                rs.getInt("item_id"),
                rs.getString("type"),
                getLong(rs, "ts_ms")
        );
    }

    private void saveSkillOrder(MatchSource source, Map<Short, ParticipantContext> participantContextById) {
        List<TimelineSkillEvent> events = jdbcTemplate.query("""
                SELECT match_id,
                       participant_id,
                       skill_slot,
                       level_up_type,
                       ts_ms
                FROM raw.match_timeline_events
                WHERE match_id = ?
                  AND type = 'SKILL_LEVEL_UP'
                  AND participant_id IS NOT NULL
                  AND skill_slot IS NOT NULL
                ORDER BY participant_id ASC, ts_ms ASC
                """, this::mapTimelineSkillEvent, source.matchId());

        Map<Short, Integer> orderByParticipant = new HashMap<>();

        for (TimelineSkillEvent event : events) {
            ParticipantContext context = participantContextById.get(event.participantId());

            if (context == null) {
                continue;
            }

            int skillOrder = orderByParticipant.merge(event.participantId(), 1, Integer::sum);
            Integer minute = event.timestampMs() == null ? null : (int) (event.timestampMs() / 60000);
            OffsetDateTime now = OffsetDateTime.now();

            jdbcTemplate.update("""
                    INSERT INTO core.participant_skill_order
                    (
                        match_id,
                        participant_id,
                        skill_order,
                        skill_slot,
                        level_up_type,
                        timestamp_ms,
                        minute,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (match_id, participant_id, skill_order)
                    DO UPDATE SET
                        skill_slot = EXCLUDED.skill_slot,
                        level_up_type = EXCLUDED.level_up_type,
                        timestamp_ms = EXCLUDED.timestamp_ms,
                        minute = EXCLUDED.minute,
                        created_at = EXCLUDED.created_at
                    """,
                    context.matchId(),
                    context.participantId(),
                    skillOrder,
                    event.skillSlot(),
                    event.levelUpType(),
                    event.timestampMs(),
                    minute,
                    now
            );

            jdbcTemplate.update("""
                    INSERT INTO analyzed.participant_skill_order
                    (
                        match_id,
                        participant_id,
                        puuid,
                        champion_id,
                        team_id,
                        win,
                        skill_slot,
                        level_up_type,
                        timestamp_ms,
                        minute,
                        skill_order,
                        game_version,
                        queue_id,
                        game_duration_ms,
                        created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (match_id, participant_id, skill_order)
                    DO NOTHING
                    """,
                    context.matchId(),
                    context.participantId(),
                    context.puuid(),
                    context.championId(),
                    context.teamId(),
                    context.win(),
                    event.skillSlot(),
                    event.levelUpType(),
                    event.timestampMs(),
                    minute,
                    skillOrder,
                    context.gameVersion(),
                    context.queueId(),
                    context.gameDurationMs(),
                    now
            );
        }
    }

    private RuneLoadout parseRuneLoadout(JsonNode perks) {
        JsonNode styles = perks.path("styles");
        JsonNode statPerks = perks.path("statPerks");

        Integer primaryStyleId = null;
        Integer secondaryStyleId = null;

        Integer keystoneId = null;
        Integer primaryRune1Id = null;
        Integer primaryRune2Id = null;
        Integer primaryRune3Id = null;

        Integer secondaryRune1Id = null;
        Integer secondaryRune2Id = null;

        if (styles.isArray()) {
            for (JsonNode style : styles) {
                Integer styleId = nullableInteger(style, "style");
                String description = nullableText(style, "description");
                JsonNode selections = style.path("selections");

                boolean primary = "primaryStyle".equalsIgnoreCase(description);
                boolean secondary = "subStyle".equalsIgnoreCase(description);

                if (primary) {
                    primaryStyleId = styleId;

                    keystoneId = runeAt(selections, 0);
                    primaryRune1Id = runeAt(selections, 1);
                    primaryRune2Id = runeAt(selections, 2);
                    primaryRune3Id = runeAt(selections, 3);
                }

                if (secondary) {
                    secondaryStyleId = styleId;

                    secondaryRune1Id = runeAt(selections, 0);
                    secondaryRune2Id = runeAt(selections, 1);
                }
            }
        }

        return new RuneLoadout(
                primaryStyleId,
                secondaryStyleId,
                keystoneId,
                primaryRune1Id,
                primaryRune2Id,
                primaryRune3Id,
                secondaryRune1Id,
                secondaryRune2Id,
                nullableInteger(statPerks, "offense"),
                nullableInteger(statPerks, "flex"),
                nullableInteger(statPerks, "defense")
        );
    }

    private Integer runeAt(JsonNode selections, int index) {
        if (!selections.isArray() || selections.size() <= index) {
            return null;
        }

        return nullableInteger(selections.get(index), "perk");
    }

    private void deletePreviousAnalysis(String matchId) {
        jdbcTemplate.update("DELETE FROM core.participant_item_events WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM core.participant_rune_selections WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM core.participant_final_items WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM core.participant_skill_order WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM core.participants WHERE match_id = ?", matchId);

        jdbcTemplate.update("DELETE FROM analyzed.participant_item_events WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM analyzed.participant_rune_selections WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM analyzed.participant_loadouts WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM analyzed.participant_final_items WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM analyzed.participant_skill_order WHERE match_id = ?", matchId);
        jdbcTemplate.update("DELETE FROM analyzed.player_match_stats WHERE match_id = ?", matchId);
    }

    private boolean hasTimelineEvents(String matchId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM raw.match_timeline_events
                WHERE match_id = ?
                """, Integer.class, matchId);

        return count != null && count > 0;
    }

    private void savePlayerMatchStats(ParticipantContext context, JsonNode participant) {
        jdbcTemplate.update("""
                INSERT INTO analyzed.player_match_stats
                (
                    match_id,
                    participant_id,
                    puuid,
                    riot_game_name,
                    riot_tagline,
                    summoner_name,
                    champion_id,
                    champion_name,
                    team_id,
                    win,
                    kills,
                    deaths,
                    assists,
                    champ_level,
                    gold_earned,
                    gold_spent,
                    total_damage_dealt_to_champions,
                    total_damage_taken,
                    vision_score,
                    wards_placed,
                    wards_killed,
                    total_minions_killed,
                    neutral_minions_killed,
                    summoner1_id,
                    summoner2_id,
                    game_version,
                    queue_id,
                    game_duration_ms,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (match_id, participant_id)
                DO UPDATE SET
                    puuid = EXCLUDED.puuid,
                    riot_game_name = EXCLUDED.riot_game_name,
                    riot_tagline = EXCLUDED.riot_tagline,
                    summoner_name = EXCLUDED.summoner_name,
                    champion_id = EXCLUDED.champion_id,
                    champion_name = EXCLUDED.champion_name,
                    team_id = EXCLUDED.team_id,
                    win = EXCLUDED.win,
                    kills = EXCLUDED.kills,
                    deaths = EXCLUDED.deaths,
                    assists = EXCLUDED.assists,
                    champ_level = EXCLUDED.champ_level,
                    gold_earned = EXCLUDED.gold_earned,
                    gold_spent = EXCLUDED.gold_spent,
                    total_damage_dealt_to_champions = EXCLUDED.total_damage_dealt_to_champions,
                    total_damage_taken = EXCLUDED.total_damage_taken,
                    vision_score = EXCLUDED.vision_score,
                    wards_placed = EXCLUDED.wards_placed,
                    wards_killed = EXCLUDED.wards_killed,
                    total_minions_killed = EXCLUDED.total_minions_killed,
                    neutral_minions_killed = EXCLUDED.neutral_minions_killed,
                    summoner1_id = EXCLUDED.summoner1_id,
                    summoner2_id = EXCLUDED.summoner2_id,
                    game_version = EXCLUDED.game_version,
                    queue_id = EXCLUDED.queue_id,
                    game_duration_ms = EXCLUDED.game_duration_ms
                """,
                context.matchId(),
                context.participantId(),
                context.puuid(),

                nullableText(participant, "riotIdGameName"),
                nullableText(participant, "riotIdTagline"),
                nullableText(participant, "summonerName"),

                context.championId(),
                nullableText(participant, "championName"),

                context.teamId(),
                context.win(),

                nullableInteger(participant, "kills"),
                nullableInteger(participant, "deaths"),
                nullableInteger(participant, "assists"),

                nullableInteger(participant, "champLevel"),

                nullableInteger(participant, "goldEarned"),
                nullableInteger(participant, "goldSpent"),

                nullableInteger(participant, "totalDamageDealtToChampions"),
                nullableInteger(participant, "totalDamageTaken"),

                nullableInteger(participant, "visionScore"),
                nullableInteger(participant, "wardsPlaced"),
                nullableInteger(participant, "wardsKilled"),

                nullableInteger(participant, "totalMinionsKilled"),
                nullableInteger(participant, "neutralMinionsKilled"),

                nullableInteger(participant, "summoner1Id"),
                nullableInteger(participant, "summoner2Id"),

                context.gameVersion(),
                context.queueId(),
                context.gameDurationMs(),

                OffsetDateTime.now()
        );
    }

    private void markProcessing(String matchId) {
        jdbcTemplate.update("""
                UPDATE raw.matches
                SET analysis_status = 'PROCESSING',
                    analysis_attempts = COALESCE(analysis_attempts, 0) + 1,
                    analysis_error = NULL,
                    processing_started_at = now()
                WHERE match_id = ?
                """, matchId);
    }

    private void markWaitingForTimeline(String matchId) {
        jdbcTemplate.update("""
                UPDATE raw.matches
                SET analysis_status = 'WAITING_FOR_TIMELINE',
                    analysis_error = NULL
                WHERE match_id = ?
                """, matchId);
    }

    private void markAnalyzed(String matchId) {
        jdbcTemplate.update("""
                UPDATE raw.matches
                SET analysis_status = 'ANALYZED',
                    analysis_error = NULL,
                    analyzed_at = now(),
                    processing_started_at = NULL
                WHERE match_id = ?
                """, matchId);
    }

    private void markFailed(String matchId, Exception ex) {
        jdbcTemplate.update("""
                UPDATE raw.matches
                SET analysis_status = 'FAILED',
                    analysis_attempts = COALESCE(analysis_attempts, 0) + 1,
                    analysis_error = ?,
                    processing_started_at = NULL
                WHERE match_id = ?
                """, trimError(ex.getMessage()), matchId);
    }

    private String trimError(String message) {
        if (message == null) {
            return "Unknown analysis error";
        }

        if (message.length() <= 1000) {
            return message;
        }

        return message.substring(0, 1000);
    }

    private JsonNode readJson(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse raw match JSON", ex);
        }
    }

    private String jsonOrNull(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }

        return node.toString();
    }

    private String nullableText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        String text = value.asText();

        return text == null || text.isBlank() ? null : text;
    }

    private Integer nullableInteger(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        return value.asInt();
    }

    private Boolean nullableBoolean(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }

        return value.asBoolean();
    }

    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private record MatchSource(
            String matchId,
            String rawMatchJson,
            String gameVersion,
            Integer queueId,
            Long gameDurationMs
    ) {
    }

    private record ParticipantContext(
            String matchId,
            short participantId,
            String puuid,
            Integer championId,
            Integer teamId,
            Boolean win,
            String gameVersion,
            Integer queueId,
            Long gameDurationMs
    ) {
    }

    private record TimelineItemEvent(
            String matchId,
            short participantId,
            Integer itemId,
            String eventType,
            Long timestampMs
    ) {
    }

    private record RuneLoadout(
            Integer primaryStyleId,
            Integer secondaryStyleId,
            Integer keystoneId,
            Integer primaryRune1Id,
            Integer primaryRune2Id,
            Integer primaryRune3Id,
            Integer secondaryRune1Id,
            Integer secondaryRune2Id,
            Integer statOffenseId,
            Integer statFlexId,
            Integer statDefenseId
    ) {
    }
}