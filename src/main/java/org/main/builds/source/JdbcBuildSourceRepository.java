package org.main.builds.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchVersion;
import org.main.builds.model.PatchWindow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public final class JdbcBuildSourceRepository implements BuildSourceRepository {

    private static final String ELIGIBLE_WHERE = """
            region = 'europe'
            AND platform = 'EUW1'
            AND game_duration_ms >= 600000
            AND queue_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    public JdbcBuildSourceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<String> findLatestPatch(BuildQueue queue) {
        return storedPatches(queue).stream().max(PatchVersion::compareTo).map(PatchVersion::displayName);
    }

    @Override
    public Optional<String> findPreviousMajorLastPatch(BuildQueue queue, int previousMajor) {
        return storedPatches(queue).stream().
                filter(patch -> patch.major() == previousMajor).
                max(PatchVersion::compareTo).
                map(PatchVersion::displayName);
    }

    @Override
    public BuildSourceSelection selectSource(PatchWindow window, BuildQueue queue) {
        Set<String> patches = Set.of(window.anchorPatch(), window.comparisonPatch());
        List<SelectedRow> rows = jdbcTemplate.query("""
                SELECT match_id, game_version, fetched_at
                FROM core.matches
                WHERE
                """ + ELIGIBLE_WHERE, this::mapSelectedRow, queue.id()).stream().
                filter(row -> patches.contains(row.patch())).
                sorted(Comparator.comparing(SelectedRow::matchId)).
                toList();
        OffsetDateTime watermark = rows.stream().map(SelectedRow::fetchedAt).
                max(OffsetDateTime::compareTo).orElse(null);
        return new BuildSourceSelection(window, queue, watermark,
                rows.stream().map(SelectedRow::matchId).toList());
    }

    @Override
    public List<BuildSourceMatch> loadBatch(List<String> selectedMatchIds) {
        if (selectedMatchIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(selectedMatchIds.size(), "?"));
        Object[] arguments = selectedMatchIds.toArray();
        Map<String, MatchRow> matches = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT m.match_id, m.game_version, m.queue_id,
                       t.raw_timeline_json::text AS timeline
                FROM core.matches m
                LEFT JOIN raw.match_timeline_raw t ON t.match_id = m.match_id
                WHERE m.match_id IN (""" + placeholders + ") ORDER BY m.match_id",
                resultSet -> {
                    String matchId = resultSet.getString("match_id");
                    matches.put(matchId, new MatchRow(matchId,
                            PatchVersion.parse(resultSet.getString("game_version")).displayName(),
                            BuildQueue.fromId(resultSet.getInt("queue_id")),
                            readJson(resultSet.getString("timeline"))));
                }, arguments);

        Map<ParticipantKey, ParticipantBuilder> participants = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT match_id, participant_id, team_id, champion_id,
                       team_position, individual_position, win,
                       summoner1_id, summoner2_id, perks_json::text AS perks
                FROM core.participants
                WHERE match_id IN (""" + placeholders + ") ORDER BY match_id, participant_id",
                resultSet -> {
                    ParticipantKey key = key(resultSet);
                    participants.put(key, new ParticipantBuilder(
                            resultSet.getInt("participant_id"), nullableInteger(resultSet, "team_id"),
                            nullableInteger(resultSet, "champion_id"), resultSet.getString("team_position"),
                            resultSet.getString("individual_position"), nullableBoolean(resultSet, "win"),
                            nullableInteger(resultSet, "summoner1_id"),
                            nullableInteger(resultSet, "summoner2_id"),
                            readJson(resultSet.getString("perks"))));
                }, arguments);
        jdbcTemplate.query("""
                SELECT match_id, participant_id, item_id
                FROM core.participant_final_items
                WHERE match_id IN (""" + placeholders + ") ORDER BY match_id, participant_id, item_slot",
                resultSet -> {
                    ParticipantBuilder participant = participants.get(key(resultSet));
                    Integer itemId = nullableInteger(resultSet, "item_id");
                    if (participant != null && itemId != null && itemId > 0) {
                        participant.finalItems.add(itemId);
                    }
                }, arguments);
        jdbcTemplate.query("""
                SELECT match_id, participant_id, skill_slot
                FROM core.participant_skill_order
                WHERE match_id IN (""" + placeholders + ") ORDER BY match_id, participant_id, skill_order",
                resultSet -> {
                    ParticipantBuilder participant = participants.get(key(resultSet));
                    if (participant != null) {
                        participant.skills.add(nullableInteger(resultSet, "skill_slot"));
                    }
                }, arguments);

        Map<String, List<BuildSourceMatch.Participant>> byMatch = new HashMap<>();
        participants.forEach((key, value) -> byMatch.
                computeIfAbsent(key.matchId(), unused -> new ArrayList<>()).
                add(value.build()));
        return matches.values().stream().
                map(match -> new BuildSourceMatch(match.matchId(), match.patch(), match.queue(),
                        match.timeline(), byMatch.getOrDefault(match.matchId(), List.of()))).
                toList();
    }

    private List<PatchVersion> storedPatches(BuildQueue queue) {
        return jdbcTemplate.queryForList("""
                SELECT game_version FROM core.matches WHERE
                """ + ELIGIBLE_WHERE,
                String.class, queue.id()).stream().map(PatchVersion::parse).distinct().toList();
    }

    private SelectedRow mapSelectedRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SelectedRow(resultSet.getString("match_id"),
                PatchVersion.parse(resultSet.getString("game_version")).displayName(),
                resultSet.getObject("fetched_at", OffsetDateTime.class));
    }

    private ParticipantKey key(ResultSet resultSet) throws SQLException {
        return new ParticipantKey(resultSet.getString("match_id"), resultSet.getInt("participant_id"));
    }

    private JsonNode readJson(String json) {
        try {
            return json == null ? objectMapper.nullNode() : objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid build source JSON", exception);
        }
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(ResultSet resultSet, String column) throws SQLException {
        boolean value = resultSet.getBoolean(column);
        return resultSet.wasNull() ? null : value;
    }

    private record SelectedRow(String matchId, String patch, OffsetDateTime fetchedAt) {
    }

    private record MatchRow(String matchId, String patch, BuildQueue queue, JsonNode timeline) {
    }

    private record ParticipantKey(String matchId, int participantId) {
    }

    private static final class ParticipantBuilder {

        private final int participantId;

        private final Integer teamId;

        private final Integer championId;

        private final String teamPosition;

        private final String individualPosition;

        private final Boolean win;

        private final Integer summoner1Id;

        private final Integer summoner2Id;

        private final JsonNode perks;

        private final Set<Integer> finalItems = new LinkedHashSet<>();

        private final List<Integer> skills = new ArrayList<>();

        private ParticipantBuilder(int participantId, Integer teamId, Integer championId,
                                   String teamPosition, String individualPosition, Boolean win,
                                   Integer summoner1Id, Integer summoner2Id, JsonNode perks) {
            this.participantId = participantId;
            this.teamId = teamId;
            this.championId = championId;
            this.teamPosition = teamPosition;
            this.individualPosition = individualPosition;
            this.win = win;
            this.summoner1Id = summoner1Id;
            this.summoner2Id = summoner2Id;
            this.perks = perks;
        }

        private BuildSourceMatch.Participant build() {
            List<Integer> spells = summoner1Id == null || summoner2Id == null
                    ? List.of() : List.of(summoner1Id, summoner2Id);
            List<Integer> validSkills = skills.stream().anyMatch(java.util.Objects::isNull)
                    ? List.of() : skills;
            return new BuildSourceMatch.Participant(participantId, teamId, championId,
                    teamPosition, individualPosition, win, finalItems, perks, spells, validSkills);
        }
    }
}
