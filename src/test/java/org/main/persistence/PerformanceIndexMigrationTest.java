package org.main.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PerformanceIndexMigrationTest {

    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration",
            "V4__add_query_performance_indexes.sql"
    );

    @Test
    void migrationGuardsExternalSchemasAndAddsFrequentQueryIndexes() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("to_regclass('raw.matches')");
        assertThat(sql).contains("ix_raw_matches_analysis_queue");
        assertThat(sql).contains("ix_core_participants_puuid_match");
        assertThat(sql).contains("ix_core_participants_champion");
        assertThat(sql).contains("ix_timeline_frames_match");
        assertThat(sql).contains("ix_timeline_events_match");
        assertThat(sql).contains("ix_raw_players_missing_profile");
        assertThat(sql).contains("ix_raw_league_entries_puuid");
        assertThat(sql).doesNotContain("drop table", "drop index");
    }
}
