package org.main.builds.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.main.builds.model.AggregatedCohort;
import org.main.builds.model.AggregationResult;
import org.main.builds.model.BaselineKey;
import org.main.builds.model.BuildChoice;
import org.main.builds.model.BuildConfidence;
import org.main.builds.model.BuildQueue;
import org.main.builds.model.BuildRole;
import org.main.builds.model.BuildScope;
import org.main.builds.model.BuildSnapshotPayload;
import org.main.builds.model.PatchWindow;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BuildPublisherIT {

    private static final PatchWindow WINDOW = new PatchWindow("16.13", "16.12");

    private static final OffsetDateTime WATERMARK = OffsetDateTime.parse("2026-07-01T12:00:00Z");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private AnnotationConfigApplicationContext context;

    private JdbcTemplate jdbc;

    private BuildSnapshotRepository repository;

    private BuildPublisher publisher;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        jdbc = context.getBean(JdbcTemplate.class);
        jdbc.update("delete from builds.champion_build_snapshot");
        jdbc.update("delete from builds.aggregation_run");
        repository = context.getBean(BuildSnapshotRepository.class);
        publisher = context.getBean(BuildPublisher.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void insertsPublishesCompletesAndRoundTripsImmutablePayload() {
        UUID runId = repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);
        AggregationResult result = result(12, payload());

        publisher.publish(runId, result, 1, 1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK, 3);

        AggregationRun run = repository.findLatestRun("16.13", BuildQueue.SOLO_DUO).orElseThrow();
        BuildSnapshot snapshot = repository.findPublished(lookup()).orElseThrow();
        assertThat(run.id()).isEqualTo(runId);
        assertThat(run.state()).isEqualTo("COMPLETED");
        assertThat(run.sourceMatchCount()).isEqualTo(3);
        assertThat(run.validationCount()).isEqualTo(1);
        assertThat(run.snapshotCount()).isEqualTo(1);
        assertThat(run.completedAt()).isEqualTo(OffsetDateTime.parse("2026-07-01T13:00:00Z"));
        assertThat(snapshot.runId()).isEqualTo(runId);
        assertThat(snapshot.sourceMatchCount()).isEqualTo(3);
        assertThat(snapshot.publicationState()).isEqualTo("PUBLISHED");
        assertThat(snapshot.payload()).isEqualTo(payload());
    }

    @Test
    void replacementArchivesThePriorRowsOnlyWhenTheReplacementCommits() {
        UUID first = publish(result(12, payload()));
        UUID second = repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1));

        publisher.publish(second, result(15, payload()), 1, 1, WINDOW,
                BuildQueue.SOLO_DUO, WATERMARK.plusHours(1), 15);

        assertThat(repository.findPublished(lookup())).get().extracting(BuildSnapshot::runId).
                isEqualTo(second);
        assertThat(jdbc.queryForObject("""
                select publication_state from builds.champion_build_snapshot where run_id = ?
                """, String.class, first)).isEqualTo("ARCHIVED");
    }

    @Test
    void failedValidationLeavesThePriorPublicationAndNoRowsForTheFailedRun() {
        UUID published = publish(result(12, payload()));
        UUID failed = repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1));

        assertThatThrownBy(() -> publisher.publish(failed,
                new AggregationResult(List.of(), Set.of(), 0), 1, 1, WINDOW,
                BuildQueue.SOLO_DUO, WATERMARK.plusHours(1), 0)).
                isInstanceOf(IllegalArgumentException.class);
        repository.failRun(failed, "VALIDATION:empty-result-category-that-is-safely-bounded-xxxxxxxxxxxx");

        assertThat(repository.findPublished(lookup())).get().extracting(BuildSnapshot::runId).
                isEqualTo(published);
        assertThat(repository.findLatestRun("16.13", BuildQueue.SOLO_DUO)).get().satisfies(run -> {
            assertThat(run.id()).isEqualTo(failed);
            assertThat(run.state()).isEqualTo("FAILED");
            assertThat(run.failureCategory()).hasSizeLessThanOrEqualTo(64);
        });
        assertThat(jdbc.queryForObject("""
                select count(*) from builds.champion_build_snapshot where run_id = ?
                """, Integer.class, failed)).isZero();
    }

    @Test
    void incompletePayloadIsValidatedBeforeAnyArchiveOrInsertUpdate() {
        UUID published = publish(result(12, payload()));
        UUID failed = repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1));
        BuildSnapshotPayload incomplete = new BuildSnapshotPayload(
                List.of(), payload().boots(), payload().coreItems(), List.of(),
                payload().runePages(), payload().spellPairs(), payload().skillOrders(),
                payload().skillMaxPriority());

        assertThatThrownBy(() -> publisher.publish(failed, result(12, incomplete), 1, 1,
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1), 12)).
                isInstanceOf(IllegalArgumentException.class);

        assertThat(repository.findPublished(lookup())).get().extracting(BuildSnapshot::runId).
                isEqualTo(published);
        assertThat(jdbc.queryForObject("""
                select count(*) from builds.champion_build_snapshot where run_id = ?
                """, Integer.class, failed)).isZero();
    }

    @Test
    void databaseGuardRejectsDuplicateRunningWindows() {
        repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);

        assertThatThrownBy(() -> repository.startRun(
                1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusSeconds(1))).
                isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void publicationFailureRollsBackTheArchiveAndPendingRows() {
        UUID published = publish(result(12, payload()));
        UUID replacement = repository.startRun(
                1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1));
        context.getBean(FailurePoint.class).value = "publishRun";

        assertThatThrownBy(() -> publisher.publish(replacement, result(15, payload()),
                1, 1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1), 15)).
                isInstanceOf(IllegalStateException.class).
                hasMessage("forced publication failure");

        assertThat(repository.findPublished(lookup())).get().extracting(BuildSnapshot::runId).
                isEqualTo(published);
        assertThat(jdbc.queryForObject("""
                select count(*) from builds.champion_build_snapshot where run_id = ?
                """, Integer.class, replacement)).isZero();
        assertThat(jdbc.queryForObject("""
                select state from builds.aggregation_run where id = ?
                """, String.class, replacement)).isEqualTo("RUNNING");
    }

    @Test
    void insertionFailureRollsBackPendingRowsAndRunCounts() {
        UUID published = publish(result(12, payload()));
        UUID replacement = repository.startRun(
                1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1));
        context.getBean(FailurePoint.class).value = "insertSnapshots";

        assertThatThrownBy(() -> publisher.publish(replacement, result(15, payload()),
                1, 1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1), 15)).
                isInstanceOf(IllegalStateException.class).
                hasMessage("forced insertion failure");

        assertThat(repository.findPublished(lookup())).get().extracting(BuildSnapshot::runId).
                isEqualTo(published);
        assertThat(jdbc.queryForObject("""
                select count(*) from builds.champion_build_snapshot where run_id = ?
                """, Integer.class, replacement)).isZero();
        assertThat(jdbc.queryForMap("""
                select source_match_count, validation_count, snapshot_count
                from builds.aggregation_run where id = ?
                """, replacement).values()).containsOnly(0);
    }

    @Test
    void publishesTheExactRunWhenTwoAggregationVersionsAreRunning() {
        UUID versionOne = repository.startRun(
                1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);
        UUID versionTwo = repository.startRun(
                2, WINDOW, BuildQueue.SOLO_DUO, WATERMARK.plusHours(1));

        publisher.publish(versionOne, result(12, payload()), 1, 1,
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK, 12);

        assertThat(repository.findRun(versionOne)).get().extracting(AggregationRun::state).
                isEqualTo("COMPLETED");
        assertThat(repository.findRun(versionTwo)).get().extracting(AggregationRun::state).
                isEqualTo("RUNNING");
    }

    @Test
    void persistsTheValidationCountReturnedByTheValidator() {
        context.getBean(PassedCountValidator.class).forcedCount = 37;
        UUID runId = repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);

        publisher.publish(runId, result(12, payload()), 1, 1,
                WINDOW, BuildQueue.SOLO_DUO, WATERMARK, 12);

        assertThat(repository.findRun(runId)).get().extracting(AggregationRun::validationCount).
                isEqualTo(37);
    }

    @Test
    void findsAnExactOpponentSnapshotWithANonNullLookupKey() {
        AggregationResult result = resultWithExact(payload());
        UUID runId = repository.startRun(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK);
        publisher.publish(runId, result, 1, 1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK, 12);

        assertThat(repository.findPublished(new BuildLookup(
                1, WINDOW, BuildQueue.SOLO_DUO, 22, BuildRole.MIDDLE, 55))).
                get().satisfies(snapshot -> {
                    assertThat(snapshot.runId()).isEqualTo(runId);
                    assertThat(snapshot.scope()).isEqualTo(BuildScope.EXACT_MATCHUP);
                    assertThat(snapshot.opponentChampionId()).isEqualTo(55);
                });
    }

    @Test
    void historicalBaselinesUseNumericPatchOrderingAndLimit() {
        publish(1, new PatchWindow("16.9", "16.8"), BuildQueue.SOLO_DUO,
                WATERMARK, result(12, payload()));
        publish(1, new PatchWindow("16.10", "16.9"), BuildQueue.SOLO_DUO,
                WATERMARK.plusHours(1), result(12, payload()));
        publish(1, new PatchWindow("16.11", "16.10"), BuildQueue.SOLO_DUO,
                WATERMARK.plusHours(2), result(12, payload()));

        List<BuildSnapshot> historical = repository.findHistoricalBaselines(
                new BuildLookup(1, new PatchWindow("16.12", "16.11"),
                        BuildQueue.SOLO_DUO, 22, BuildRole.MIDDLE, null), 2);

        assertThat(historical).extracting(snapshot -> snapshot.window().anchorPatch()).
                containsExactly("16.11", "16.10");
    }

    @Test
    void historicalBaselinesRejectRowsOutsideAdjacentPatchLookback() {
        publish(1, new PatchWindow("16.2", "16.1"), BuildQueue.SOLO_DUO,
                WATERMARK, result(12, payload()));

        List<BuildSnapshot> historical = repository.findHistoricalBaselines(
                new BuildLookup(1, new PatchWindow("16.9", "16.8"),
                        BuildQueue.SOLO_DUO, 22, BuildRole.MIDDLE, null), 2);

        assertThat(historical).isEmpty();
    }

    @Test
    void replacementArchivesOnlyTheSameVersionWindowAndQueue() {
        UUID replaced = publish(1, WINDOW, BuildQueue.SOLO_DUO,
                WATERMARK, result(12, payload()));
        UUID otherVersion = publish(2, WINDOW, BuildQueue.SOLO_DUO,
                WATERMARK.plusHours(1), result(12, payload()));
        UUID otherWindow = publish(1, new PatchWindow("16.12", "16.11"),
                BuildQueue.SOLO_DUO, WATERMARK.plusHours(2), result(12, payload()));
        UUID otherQueue = publish(1, WINDOW, BuildQueue.FLEX,
                WATERMARK.plusHours(3), result(12, payload()));
        UUID replacement = publish(1, WINDOW, BuildQueue.SOLO_DUO,
                WATERMARK.plusHours(4), result(15, payload()));

        assertThat(publicationStates(replaced, otherVersion, otherWindow,
                otherQueue, replacement)).containsExactly(
                "ARCHIVED", "PUBLISHED", "PUBLISHED", "PUBLISHED", "PUBLISHED");
    }

    private UUID publish(AggregationResult result) {
        return publish(1, WINDOW, BuildQueue.SOLO_DUO, WATERMARK, result);
    }

    private UUID publish(
            int version,
            PatchWindow window,
            BuildQueue queue,
            OffsetDateTime watermark,
            AggregationResult result
    ) {
        UUID runId = repository.startRun(version, window, queue, watermark);
        publisher.publish(runId, result, version, 1, window, queue, watermark,
                result.sourceObservationCount());
        return runId;
    }

    private BuildLookup lookup() {
        return new BuildLookup(1, WINDOW, BuildQueue.SOLO_DUO,
                22, BuildRole.MIDDLE, null);
    }

    private AggregationResult result(int games, BuildSnapshotPayload payload) {
        AggregatedCohort cohort = new AggregatedCohort(22, BuildRole.MIDDLE, null,
                BuildScope.CHAMPION_ROLE, games, games / 2, games, 0,
                BuildConfidence.LOW, payload);
        return new AggregationResult(List.of(cohort),
                Set.of(new BaselineKey(22, BuildRole.MIDDLE)), games);
    }

    private AggregationResult resultWithExact(BuildSnapshotPayload payload) {
        AggregatedCohort baseline = result(12, payload).cohorts().getFirst();
        AggregatedCohort exact = new AggregatedCohort(22, BuildRole.MIDDLE, 55,
                BuildScope.EXACT_MATCHUP, 10, 5, 10, 0,
                BuildConfidence.LOW, payload);
        return new AggregationResult(List.of(baseline, exact),
                Set.of(new BaselineKey(22, BuildRole.MIDDLE)), 12);
    }

    private List<String> publicationStates(UUID... runIds) {
        return java.util.Arrays.stream(runIds).
                map(runId -> jdbc.queryForObject("""
                        select publication_state
                        from builds.champion_build_snapshot
                        where run_id = ? and opponent_champion_id is null
                        """, String.class, runId)).
                toList();
    }

    private BuildSnapshotPayload payload() {
        return new BuildSnapshotPayload(
                List.of(choice(List.of(1055))), List.of(choice(List.of(3006))),
                List.of(choice(List.of(6672, 3031))), List.of(),
                List.of(choice(List.of(8000, 8005, 9111, 9104, 8014,
                        8300, 8304, 8347, 5005, 5008, 5002))),
                List.of(choice(List.of(4, 14))),
                List.of(choice(List.of(1, 2, 3, 1, 1, 4))), List.of(1, 2, 3));
    }

    private BuildChoice choice(List<Integer> ids) {
        return new BuildChoice(ids, 12, 6, 1.0, 0.5, 8.4);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).
                    defaultSchema("app").schemas("app").createSchemas(true).load().migrate();
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-01T13:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        FailurePoint failurePoint() {
            return new FailurePoint();
        }

        @Bean
        BuildSnapshotRepository repository(
                JdbcTemplate jdbc,
                ObjectMapper mapper,
                Clock clock,
                FailurePoint failurePoint
        ) {
            BuildSnapshotRepository delegate = new JdbcBuildSnapshotRepository(
                    jdbc, mapper, clock);
            return (BuildSnapshotRepository) Proxy.newProxyInstance(
                    BuildSnapshotRepository.class.getClassLoader(),
                    new Class<?>[]{BuildSnapshotRepository.class},
                    (proxy, method, arguments) -> {
                        try {
                            Object result = method.invoke(delegate, arguments);
                            if (method.getName().equals(failurePoint.value)) {
                                String operation = method.getName().equals("publishRun")
                                        ? "publication" : "insertion";
                                throw new IllegalStateException(
                                        "forced " + operation + " failure");
                            }
                            return result;
                        } catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    });
        }

        @Bean
        PassedCountValidator validator() {
            return new PassedCountValidator();
        }

        @Bean
        BuildPublisher publisher(BuildSnapshotRepository repository,
                                 BuildSnapshotValidator validator) {
            return new BuildPublisher(repository, validator);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    static final class FailurePoint {

        private String value;
    }

    static final class PassedCountValidator extends BuildSnapshotValidator {

        private Integer forcedCount;

        private PassedCountValidator() {
            super(10);
        }

        @Override
        public int validate(
                AggregationRun run,
                UUID runId,
                AggregationResult result,
                int aggregationVersion,
                PatchWindow window,
                BuildQueue queue,
                OffsetDateTime watermark
        ) {
            int passed = super.validate(run, runId, result, aggregationVersion,
                    window, queue, watermark);
            return forcedCount == null ? passed : forcedCount;
        }
    }
}
