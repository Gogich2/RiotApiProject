package org.main.builds;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "app.scheduler.data-integrity.enabled=false",
        "app.scheduler.match-analysis.enabled=false"
})
class ChampionBuildMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsSnapshotTablesAndPublicationIndexes() {
        List<String> tables = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = 'builds'
                order by table_name
                """, String.class);

        Map<String, String> indexes = jdbcTemplate.query("""
                select indexname, indexdef
                from pg_indexes
                where schemaname = 'builds'
                """, resultSet -> {
                    var definitions = new java.util.HashMap<String, String>();
                    while (resultSet.next()) {
                        definitions.put(resultSet.getString("indexname"), resultSet.getString("indexdef"));
                    }
                    return definitions;
                });

        assertThat(tables).containsExactly("aggregation_run", "champion_build_snapshot");
        assertThat(indexes.get("uq_champion_build_snapshot_run_cohort")).contains(
                "CREATE UNIQUE INDEX",
                "(run_id, aggregation_version, anchor_patch, comparison_patch, queue_id, "
                        + "champion_id, role, opponent_champion_id) NULLS NOT DISTINCT");
        assertThat(indexes.get("uq_champion_build_snapshot_published_cohort")).contains(
                "CREATE UNIQUE INDEX",
                "(aggregation_version, anchor_patch, comparison_patch, queue_id, champion_id, "
                        + "role, opponent_champion_id) NULLS NOT DISTINCT",
                "publication_state", "PUBLISHED");
        assertThat(indexes.get("uq_aggregation_run_running_window")).contains(
                "CREATE UNIQUE INDEX",
                "(aggregation_version, anchor_patch, comparison_patch, queue_id)",
                "state", "RUNNING");
        assertThat(indexes.get("ix_champion_build_snapshot_published_lookup")).contains(
                "(queue_id, anchor_patch, champion_id, role, opponent_champion_id, "
                        + "aggregation_version, comparison_patch)",
                "publication_state", "PUBLISHED");
    }
}
