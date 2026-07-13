package org.main.builds.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcBuildSnapshotRepository implements BuildSnapshotRepository {

    private static final String SNAPSHOT_COLUMNS = """
            id, run_id, aggregation_version, payload_schema_version,
            anchor_patch, comparison_patch, queue_id, champion_id, role,
            opponent_champion_id, scope, games, wins, anchor_games, comparison_games,
            confidence, input_watermark, source_match_count, calculated_at, published_at,
            publication_state, payload
            """;

    private final JdbcTemplate jdbc;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    public JdbcBuildSnapshotRepository(JdbcTemplate jdbc, ObjectMapper objectMapper, Clock clock) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public UUID startRun(int version, PatchWindow window, BuildQueue queue,
                         OffsetDateTime watermark) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into builds.aggregation_run (
                    id, aggregation_version, anchor_patch, comparison_patch, queue_id,
                    input_watermark, state, source_match_count, validation_count,
                    snapshot_count, started_at
                ) values (?, ?, ?, ?, ?, ?, 'RUNNING', 0, 0, 0, ?)
                """, id, version, window.anchorPatch(), window.comparisonPatch(), queue.id(),
                watermark, now());
        return id;
    }

    @Override
    public void insertSnapshots(
            UUID runId,
            List<AggregatedCohort> cohorts,
            int aggregationVersion,
            int payloadSchemaVersion,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark,
            int validationCount
    ) {
        int sourceMatchCount = cohorts.stream().
                filter(cohort -> cohort.scope() == BuildScope.CHAMPION_ROLE).
                mapToInt(AggregatedCohort::games).
                sum();
        OffsetDateTime calculatedAt = now();
        jdbc.batchUpdate("""
                insert into builds.champion_build_snapshot (
                    id, run_id, aggregation_version, payload_schema_version,
                    anchor_patch, comparison_patch, queue_id, champion_id, role,
                    opponent_champion_id, scope, games, wins, anchor_games,
                    comparison_games, confidence, input_watermark, source_match_count,
                    calculated_at, publication_state, payload
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                          'PENDING', ?::jsonb)
                """, cohorts, cohorts.size(), (statement, cohort) -> setSnapshotInsert(
                        statement, cohort, runId, aggregationVersion, payloadSchemaVersion,
                        window, queue, watermark, sourceMatchCount, calculatedAt));
        int updated = jdbc.update("""
                update builds.aggregation_run
                set source_match_count = ?, validation_count = ?, snapshot_count = ?
                where id = ? and state = 'RUNNING'
                """, sourceMatchCount, validationCount, cohorts.size(), runId);
        requireOne(updated, "Running aggregation run was not found");
    }

    @Override
    public void publishRun(UUID runId) {
        jdbc.update("""
                update builds.champion_build_snapshot old
                set publication_state = 'ARCHIVED'
                from builds.aggregation_run replacement
                where replacement.id = ?
                  and replacement.state = 'RUNNING'
                  and old.run_id <> replacement.id
                  and old.publication_state = 'PUBLISHED'
                  and old.aggregation_version = replacement.aggregation_version
                  and old.anchor_patch = replacement.anchor_patch
                  and old.comparison_patch = replacement.comparison_patch
                  and old.queue_id = replacement.queue_id
                """, runId);
        int published = jdbc.update("""
                update builds.champion_build_snapshot
                set publication_state = 'PUBLISHED', published_at = ?
                where run_id = ? and publication_state = 'PENDING'
                """, now(), runId);
        if (published == 0) {
            throw new IllegalStateException("Run contains no pending snapshots");
        }
        int completed = jdbc.update("""
                update builds.aggregation_run
                set state = 'COMPLETED', completed_at = ?
                where id = ? and state = 'RUNNING' and snapshot_count = ?
                """, now(), runId, published);
        requireOne(completed, "Aggregation run could not be completed");
    }

    @Override
    public void failRun(UUID runId, String failureCategory) {
        if (failureCategory == null || failureCategory.isBlank()) {
            throw new IllegalArgumentException("Failure category is required");
        }
        String bounded = failureCategory.length() <= 64
                ? failureCategory : failureCategory.substring(0, 64);
        int updated = jdbc.update("""
                update builds.aggregation_run
                set state = 'FAILED', failure_category = ?, completed_at = ?
                where id = ? and state = 'RUNNING'
                """, bounded, now(), runId);
        requireOne(updated, "Running aggregation run was not found");
    }

    @Override
    public Optional<BuildSnapshot> findPublished(BuildLookup lookup) {
        List<BuildSnapshot> rows = jdbc.query("""
                select %s
                from builds.champion_build_snapshot
                where aggregation_version = ? and anchor_patch = ? and comparison_patch = ?
                  and queue_id = ? and champion_id = ? and role = ?
                  and opponent_champion_id is not distinct from ?
                  and publication_state = 'PUBLISHED'
                """.formatted(SNAPSHOT_COLUMNS), this::mapSnapshot,
                lookup.aggregationVersion(), lookup.window().anchorPatch(),
                lookup.window().comparisonPatch(), lookup.queue().id(), lookup.championId(),
                lookup.role().name(), lookup.opponentChampionId());
        return rows.stream().findFirst();
    }

    @Override
    public List<BuildSnapshot> findHistoricalBaselines(BuildLookup lookup, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jdbc.query("""
                select %s
                from builds.champion_build_snapshot
                where aggregation_version = ? and queue_id = ? and champion_id = ?
                  and role = ? and opponent_champion_id is null
                  and publication_state = 'PUBLISHED'
                  and (split_part(anchor_patch, '.', 1)::integer,
                       split_part(anchor_patch, '.', 2)::integer) < (?, ?)
                order by split_part(anchor_patch, '.', 1)::integer desc,
                         split_part(anchor_patch, '.', 2)::integer desc
                limit ?
                """.formatted(SNAPSHOT_COLUMNS), this::mapSnapshot,
                lookup.aggregationVersion(), lookup.queue().id(), lookup.championId(),
                lookup.role().name(), patchPart(lookup.window().anchorPatch(), 0),
                patchPart(lookup.window().anchorPatch(), 1), limit);
    }

    @Override
    public Optional<AggregationRun> findRun(UUID runId) {
        List<AggregationRun> rows = jdbc.query("""
                select id, aggregation_version, anchor_patch, comparison_patch, queue_id,
                       input_watermark, state, source_match_count, validation_count,
                       snapshot_count, failure_category, started_at, completed_at
                from builds.aggregation_run
                where id = ?
                """, this::mapRun, runId);
        return rows.stream().findFirst();
    }

    @Override
    public Optional<AggregationRun> findLatestRun(String patch, BuildQueue queue) {
        List<AggregationRun> rows = jdbc.query("""
                select id, aggregation_version, anchor_patch, comparison_patch, queue_id,
                       input_watermark, state, source_match_count, validation_count,
                       snapshot_count, failure_category, started_at, completed_at
                from builds.aggregation_run
                where anchor_patch = ? and queue_id = ?
                order by (state = 'RUNNING') desc, input_watermark desc,
                         started_at desc, id desc
                limit 1
                """, this::mapRun, patch, queue.id());
        return rows.stream().findFirst();
    }

    private void setSnapshotInsert(
            PreparedStatement statement,
            AggregatedCohort cohort,
            UUID runId,
            int aggregationVersion,
            int payloadSchemaVersion,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark,
            int sourceMatchCount,
            OffsetDateTime calculatedAt
    ) throws SQLException {
        statement.setObject(1, UUID.randomUUID());
        statement.setObject(2, runId);
        statement.setInt(3, aggregationVersion);
        statement.setInt(4, payloadSchemaVersion);
        statement.setString(5, window.anchorPatch());
        statement.setString(6, window.comparisonPatch());
        statement.setInt(7, queue.id());
        statement.setInt(8, cohort.championId());
        statement.setString(9, cohort.role().name());
        if (cohort.opponentChampionId() == null) {
            statement.setNull(10, Types.INTEGER);
        } else {
            statement.setInt(10, cohort.opponentChampionId());
        }
        statement.setString(11, cohort.scope().name());
        statement.setInt(12, cohort.games());
        statement.setInt(13, cohort.wins());
        statement.setInt(14, cohort.anchorGames());
        statement.setInt(15, cohort.comparisonGames());
        statement.setString(16, cohort.confidence().name());
        statement.setObject(17, watermark);
        statement.setInt(18, sourceMatchCount);
        statement.setObject(19, calculatedAt);
        statement.setString(20, writePayload(cohort.payload()));
    }

    private AggregationRun mapRun(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AggregationRun(
                resultSet.getObject("id", UUID.class),
                resultSet.getInt("aggregation_version"),
                new PatchWindow(resultSet.getString("anchor_patch"),
                        resultSet.getString("comparison_patch")),
                BuildQueue.fromId(resultSet.getInt("queue_id")),
                resultSet.getObject("input_watermark", OffsetDateTime.class),
                resultSet.getString("state"),
                resultSet.getInt("source_match_count"),
                resultSet.getInt("validation_count"),
                resultSet.getInt("snapshot_count"),
                resultSet.getString("failure_category"),
                resultSet.getObject("started_at", OffsetDateTime.class),
                resultSet.getObject("completed_at", OffsetDateTime.class));
    }

    private BuildSnapshot mapSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        return new BuildSnapshot(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("run_id", UUID.class),
                resultSet.getInt("aggregation_version"),
                resultSet.getInt("payload_schema_version"),
                new PatchWindow(resultSet.getString("anchor_patch"),
                        resultSet.getString("comparison_patch")),
                BuildQueue.fromId(resultSet.getInt("queue_id")),
                resultSet.getInt("champion_id"),
                BuildRole.valueOf(resultSet.getString("role")),
                resultSet.getObject("opponent_champion_id", Integer.class),
                BuildScope.valueOf(resultSet.getString("scope")),
                resultSet.getInt("games"),
                resultSet.getInt("wins"),
                resultSet.getInt("anchor_games"),
                resultSet.getInt("comparison_games"),
                org.main.builds.model.BuildConfidence.valueOf(
                        resultSet.getString("confidence")),
                resultSet.getObject("input_watermark", OffsetDateTime.class),
                resultSet.getInt("source_match_count"),
                resultSet.getObject("calculated_at", OffsetDateTime.class),
                resultSet.getObject("published_at", OffsetDateTime.class),
                resultSet.getString("publication_state"),
                readPayload(resultSet.getString("payload")));
    }

    private String writePayload(BuildSnapshotPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot serialize build payload", exception);
        }
    }

    private BuildSnapshotPayload readPayload(String json) {
        try {
            return objectMapper.readValue(json, BuildSnapshotPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize build payload", exception);
        }
    }

    private int patchPart(String patch, int index) {
        return Integer.parseInt(patch.split("\\.")[index]);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private void requireOne(int updated, String message) {
        if (updated != 1) {
            throw new IllegalStateException(message);
        }
    }
}
