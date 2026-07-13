package org.main.builds.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.PatchWindow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcBuildSourceRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;

    private JdbcBuildSourceRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource("builds/build-source-it.sql")).
                execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcBuildSourceRepository(jdbc, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void freezesEligibleIdsAndMapsOnlyThoseIdsAfterNewAnalysisCommits() {
        assertThat(repository.findLatestPatch(BuildQueue.SOLO_DUO)).contains("16.13");
        assertThat(repository.findPreviousMajorLastPatch(BuildQueue.SOLO_DUO, 15)).contains("15.24");

        BuildSourceSelection selected = repository.selectSource(
                new PatchWindow("16.13", "16.12"), BuildQueue.SOLO_DUO);

        assertThat(selected.matchIds()).containsExactly("EUW1_a", "EUW1_b");
        assertThat(selected.inputWatermark()).isEqualTo(OffsetDateTime.parse("2026-07-02T00:00:00Z"));

        jdbc.update("""
                INSERT INTO core.matches VALUES
                ('EUW1_aa','europe','EUW1',700000,'16.13.3',420,'2026-07-04T00:00:00Z')
                """);
        jdbc.update("""
                INSERT INTO raw.match_timeline_raw VALUES
                ('EUW1_aa','{"info":{"frames":[]}}','2026-07-04T00:01:00Z')
                """);

        List<BuildSourceMatch> batch = repository.loadBatch(selected.matchIds());

        assertThat(batch).extracting(BuildSourceMatch::matchId).containsExactly("EUW1_a", "EUW1_b");
        assertThat(batch.getFirst().timeline().path("info").path("frames")).hasSize(1);
        assertThat(batch.getFirst().participants()).hasSize(2);
    }

    @Test
    void itemCatalogUsesOnlyTheLatestSynchronizedVersionAndCompletedItems() {
        JdbcItemCatalog catalog = new JdbcItemCatalog(jdbc);

        assertThat(catalog.isCompletedCoreItem(6672)).isTrue();
        assertThat(catalog.isCompletedCoreItem(9999)).isFalse();
        assertThat(catalog.isCompletedBoot(3006)).isTrue();
        assertThat(catalog.isCompletedBoot(1001)).isFalse();
    }
}
