package org.main.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.main.builds.source.JdbcItemCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "app.scheduler.background-maintenance.enabled=false",
        "app.scheduler.match-analysis.enabled=false",
        "app.builds.scheduler-enabled=false"
})
class CrawlerRotationMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withInitScript(
            "db/test/create_raw_players.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.main.persistence.repository.PlayerRepository playerRepository;

    @MockBean
    private JdbcItemCatalog jdbcItemCatalog;

    @Test
    void addsNullableCrawlAttemptColumnAndRotationIndex() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = 'raw'
                  and table_name = 'players'
                  and column_name = 'last_crawl_attempt_at'
                  and is_nullable = 'YES'
                """, Integer.class);
        String indexDefinition = jdbcTemplate.queryForObject("""
                select indexdef
                from pg_indexes
                where schemaname = 'raw'
                  and indexname = 'ix_players_crawl_rotation'
                """, String.class);

        assertThat(columnCount).isEqualTo(1);
        assertThat(indexDefinition).contains(
                "last_crawl_attempt_at NULLS FIRST",
                "created_at",
                "puuid"
        );
    }

    @Test
    void selectsNeverAttemptedPlayersBeforeTheOldestAttempt() {
        jdbcTemplate.update("""
                insert into raw.players
                    (puuid, created_at, updated_at, last_crawl_attempt_at)
                values
                    ('attempted', '2019-01-01T00:00:00Z', '2019-01-01T00:00:00Z', '2025-01-01T00:00:00Z'),
                    ('never-new', '2021-01-01T00:00:00Z', '2021-01-01T00:00:00Z', null),
                    ('never-old', '2020-01-01T00:00:00Z', '2020-01-01T00:00:00Z', null)
                """);

        assertThat(playerRepository.findNextCrawlCandidate()).
                get().
                extracting(org.main.persistence.entity.PlayerEntity::getPuuid).
                isEqualTo("never-old");

        jdbcTemplate.update("""
                update raw.players
                set last_crawl_attempt_at = '2026-01-01T00:00:00Z'
                where puuid = 'never-old'
                """);

        assertThat(playerRepository.findNextCrawlCandidate()).
                get().
                extracting(org.main.persistence.entity.PlayerEntity::getPuuid).
                isEqualTo("never-new");
    }
}
