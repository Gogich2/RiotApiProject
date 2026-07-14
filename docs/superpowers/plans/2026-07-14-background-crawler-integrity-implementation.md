# Background Crawler and Data Integrity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run fair, failure-isolated crawler and timeline-integrity maintenance continuously from application startup while softly sharing Riot's two-minute request allowance 50/50.

**Architecture:** Replace the standalone integrity scheduler with one Spring `@Scheduled` coordinator. Store a nullable crawl-attempt timestamp on `raw.players`, select the oldest attempt, expose the existing limiter's live two-minute capacity, and keep quota arithmetic in a small package-private utility so the coordinator can give unused capacity to the other phase.

**Tech Stack:** Java 23, Spring Boot 3.3.3, Spring Scheduling, Spring Data JPA, PostgreSQL 16, Flyway, JUnit 5, Mockito, AssertJ, Testcontainers, Maven Wrapper

## Global Constraints

- Preserve every existing player, match, timeline, frame, and event row.
- Keep `crawlLatestPlayerEUW` and all manual crawler and integrity endpoints unchanged.
- Use the existing `RiotRateLimiter`; every real Riot call must still pass through `acquire()`.
- Use one scheduler process only; do not add distributed locking, leader election, Quartz, or another dependency.
- Record crawl attempts after success, zero new matches, and failure so one player cannot starve the queue.
- Treat existing `NULL` attempt timestamps as never crawled and order them first.
- Use a 60,000 ms initial delay and a 120,000 ms fixed delay.
- Use a 50 percent protected integrity share and one request of best-effort headroom.
- Use these exact properties:
  - `app.scheduler.background-maintenance.enabled=true`
  - `app.scheduler.background-maintenance.initial-delay-ms=60000`
  - `app.scheduler.background-maintenance.fixed-delay-ms=120000`
  - `app.scheduler.background-maintenance.integrity-share-percent=50`
  - `app.scheduler.background-maintenance.headroom-requests=1`
- Add no frontend logic and no new dependency.

---

## File Structure

- `src/main/resources/db/migration/V3__add_player_crawl_attempt.sql` — conditionally adds the legacy `raw.players` column and its rotation index.
- `src/main/java/org/main/persistence/entity/PlayerEntity.java` — maps the crawl-attempt timestamp.
- `src/main/java/org/main/persistence/repository/PlayerRepository.java` — selects the null-first, least-recently-attempted player.
- `src/main/java/org/main/service/CrawlerService.java` — exposes one optional next-player crawl operation.
- `src/main/java/org/main/service/CrawlerServiceImpl.java` — delegates to the existing PUUID crawl and advances the timestamp in `finally`.
- `src/main/java/org/main/client/RiotRateLimiter.java` — reports configured and currently remaining two-minute capacity.
- `src/main/java/org/main/service/scheduler/MaintenanceBudget.java` — contains pure integer quota and crawler-cost calculations.
- `src/main/java/org/main/service/scheduler/BackgroundMaintenanceScheduler.java` — coordinates protected work, borrowing, fallbacks, and logging.
- `src/main/java/org/main/service/scheduler/DataIntegrityScheduler.java` — deleted to prevent duplicate integrity scheduling.
- `src/main/resources/application.properties` — binds the new scheduler defaults and environment overrides.
- Focused tests live beside their corresponding package; the migration test uses a minimal PostgreSQL `raw.players` fixture.

---

### Task 1: Persist and Select the Crawl Rotation Cursor

**Files:**
- Create: `src/main/resources/db/migration/V3__add_player_crawl_attempt.sql`
- Create: `src/test/resources/db/test/create_raw_players.sql`
- Create: `src/test/java/org/main/persistence/CrawlerRotationMigrationIT.java`
- Modify: `src/main/java/org/main/persistence/entity/PlayerEntity.java`
- Modify: `src/main/java/org/main/persistence/repository/PlayerRepository.java`

**Interfaces:**
- Produces: `PlayerEntity.getLastCrawlAttemptAt(): OffsetDateTime`
- Produces: `PlayerEntity.setLastCrawlAttemptAt(OffsetDateTime): void`
- Produces: `PlayerRepository.findNextCrawlCandidate(): Optional<PlayerEntity>`
- Selection order: null attempt first, then attempt ascending, creation ascending, PUUID ascending.

- [ ] **Step 1: Create the PostgreSQL fixture and failing migration test**

Create `src/test/resources/db/test/create_raw_players.sql`:

```sql
create schema if not exists raw;

create table raw.players (
    puuid varchar(128) primary key,
    game_name varchar(128),
    tag_line varchar(64),
    profile_icon_id integer,
    summoner_level integer,
    profile_synced_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null
);
```

Create `CrawlerRotationMigrationIT` with the container and the first test:

```java
package org.main.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
        "app.scheduler.background-maintenance.enabled=false",
        "app.scheduler.match-analysis.enabled=false",
        "app.builds.scheduler-enabled=false"
})
class CrawlerRotationMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("db/test/create_raw_players.sql");

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
}
```

- [ ] **Step 2: Run the migration test and verify it fails**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerRotationMigrationIT test
```

Expected: FAIL because `last_crawl_attempt_at` and `ix_players_crawl_rotation` do not exist.

- [ ] **Step 3: Add the conditional Flyway migration**

Create `V3__add_player_crawl_attempt.sql`:

```sql
do $migration$
begin
    if to_regclass('raw.players') is not null then
        execute 'alter table raw.players '
            || 'add column if not exists last_crawl_attempt_at timestamptz';
        execute 'create index if not exists ix_players_crawl_rotation '
            || 'on raw.players '
            || '(last_crawl_attempt_at asc nulls first, created_at asc, puuid asc)';
    end if;
end
$migration$;
```

The `to_regclass` guard is required because the repository's fresh migration tests create only the newer application schemas; production already contains the legacy `raw.players` table.

- [ ] **Step 4: Run the migration test and verify it passes**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerRotationMigrationIT test
```

Expected: PASS with one nullable column and the expected composite index.

- [ ] **Step 5: Add the failing null-first repository-order test**

Add the repository field and this method to `CrawlerRotationMigrationIT`:

```java
@Autowired
private org.main.persistence.repository.PlayerRepository playerRepository;

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

    assertThat(playerRepository.findNextCrawlCandidate())
            .get()
            .extracting(org.main.persistence.entity.PlayerEntity::getPuuid)
            .isEqualTo("never-old");

    jdbcTemplate.update("""
            update raw.players
            set last_crawl_attempt_at = '2026-01-01T00:00:00Z'
            where puuid = 'never-old'
            """);

    assertThat(playerRepository.findNextCrawlCandidate())
            .get()
            .extracting(org.main.persistence.entity.PlayerEntity::getPuuid)
            .isEqualTo("never-new");
}
```

- [ ] **Step 6: Run the repository test and verify compilation fails**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerRotationMigrationIT test
```

Expected: FAIL to compile because `findNextCrawlCandidate()` does not exist.

- [ ] **Step 7: Map the column and add the explicit null-first query**

Add to `PlayerEntity` beside the existing timestamps:

```java
@Column(name = "last_crawl_attempt_at")
private OffsetDateTime lastCrawlAttemptAt;

public OffsetDateTime getLastCrawlAttemptAt() {
    return lastCrawlAttemptAt;
}

public void setLastCrawlAttemptAt(OffsetDateTime lastCrawlAttemptAt) {
    this.lastCrawlAttemptAt = lastCrawlAttemptAt;
}
```

Add the import and method to `PlayerRepository`:

```java
import org.springframework.data.jpa.repository.Query;

@Query(value = """
        select *
        from raw.players
        order by last_crawl_attempt_at asc nulls first,
                 created_at asc,
                 puuid asc
        limit 1
        """, nativeQuery = true)
Optional<PlayerEntity> findNextCrawlCandidate();
```

- [ ] **Step 8: Run the complete rotation integration test**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerRotationMigrationIT test
```

Expected: PASS for both migration shape and deterministic null-first selection.

- [ ] **Step 9: Commit the data-model slice**

```powershell
git add src/main/resources/db/migration/V3__add_player_crawl_attempt.sql src/test/resources/db/test/create_raw_players.sql src/test/java/org/main/persistence/CrawlerRotationMigrationIT.java src/main/java/org/main/persistence/entity/PlayerEntity.java src/main/java/org/main/persistence/repository/PlayerRepository.java
git commit -m "feat: persist fair crawler rotation"
```

---

### Task 2: Crawl the Least Recently Attempted Player

**Files:**
- Modify: `src/main/java/org/main/service/CrawlerService.java`
- Modify: `src/main/java/org/main/service/CrawlerServiceImpl.java`
- Create: `src/test/java/org/main/service/CrawlerServiceImplRotationTest.java`

**Interfaces:**
- Consumes: `PlayerRepository.findNextCrawlCandidate(): Optional<PlayerEntity>`
- Produces: `CrawlerService.crawlNextPlayerEUW(int limitRaw): Optional<CrawlResultDto>`
- Guarantees: if a candidate exists, `lastCrawlAttemptAt` is persisted in `finally` whether the delegated crawl returns or throws.

- [ ] **Step 1: Write failing tests for empty, successful, and failed rotations**

Create `CrawlerServiceImplRotationTest` using the same mocked constructor dependencies as `CrawlerServiceImplPaginationTest`. Add this shared setup and the three tests:

```java
package org.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.main.client.RiotApiClient;
import org.main.persistence.entity.PlayerEntity;
import org.main.persistence.repository.MatchRepository;
import org.main.persistence.repository.PlayerRepository;
import org.springframework.transaction.support.TransactionTemplate;

class CrawlerServiceImplRotationTest {

    private RiotApiClient riotApiClient;
    private PlayerRepository playerRepository;
    private CrawlerServiceImpl service;

    @BeforeEach
    void setUp() {
        riotApiClient = mock(RiotApiClient.class);
        playerRepository = mock(PlayerRepository.class);
        service = new CrawlerServiceImpl(
                riotApiClient,
                mock(MatchRepository.class),
                playerRepository,
                mock(TimelineIngestService.class),
                mock(IngestLogService.class),
                mock(TransactionTemplate.class)
        );
    }

    @Test
    void returnsEmptyWhenNoPlayerIsStored() {
        when(playerRepository.findNextCrawlCandidate()).thenReturn(Optional.empty());

        assertThat(service.crawlNextPlayerEUW(20)).isEmpty();
    }

    @Test
    void advancesAttemptTimestampWhenCrawlSavesNothing() {
        PlayerEntity player = player("puuid-success");
        when(playerRepository.findNextCrawlCandidate()).thenReturn(Optional.of(player));
        when(riotApiClient.getMatchIdsByPuuidEurope("puuid-success", 0, 20))
                .thenReturn(List.of());
        OffsetDateTime before = OffsetDateTime.now();

        var result = service.crawlNextPlayerEUW(20);

        assertThat(result).isPresent();
        assertThat(result.get().savedNewMatches()).isZero();
        assertThat(player.getLastCrawlAttemptAt()).isAfterOrEqualTo(before);
        verify(playerRepository).save(player);
    }

    @Test
    void advancesAttemptTimestampAndRethrowsWhenCrawlFails() {
        PlayerEntity player = player("puuid-failure");
        when(playerRepository.findNextCrawlCandidate()).thenReturn(Optional.of(player));
        when(riotApiClient.getMatchIdsByPuuidEurope("puuid-failure", 0, 20))
                .thenThrow(new IllegalStateException("Riot unavailable"));

        assertThatThrownBy(() -> service.crawlNextPlayerEUW(20))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Riot unavailable");
        assertThat(player.getLastCrawlAttemptAt()).isNotNull();
        verify(playerRepository).save(player);
    }

    private PlayerEntity player(String puuid) {
        PlayerEntity player = new PlayerEntity();
        player.setPuuid(puuid);
        player.setCreatedAt(OffsetDateTime.now().minusDays(1));
        player.setUpdatedAt(OffsetDateTime.now().minusDays(1));
        return player;
    }
}
```

- [ ] **Step 2: Run the rotation unit test and verify compilation fails**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerServiceImplRotationTest test
```

Expected: FAIL to compile because `crawlNextPlayerEUW(int)` does not exist.

- [ ] **Step 3: Add the optional crawler operation**

Add to `CrawlerService`:

```java
Optional<CrawlResultDto> crawlNextPlayerEUW(int limitRaw);
```

Add `java.util.Optional` to its imports.

Add to `CrawlerServiceImpl`:

```java
@Override
public Optional<CrawlResultDto> crawlNextPlayerEUW(int limitRaw) {
    Optional<PlayerEntity> candidate = playerRepository.findNextCrawlCandidate();

    if (candidate.isEmpty()) {
        log.debug("Scheduled crawl skipped because raw.players is empty");
        return Optional.empty();
    }

    PlayerEntity player = candidate.get();

    try {
        return Optional.of(crawlPuuidEUW(player.getPuuid(), limitRaw));
    } finally {
        player.setLastCrawlAttemptAt(OffsetDateTime.now());
        playerRepository.save(player);
    }
}
```

Add `java.util.Optional` to the implementation imports. Do not modify `crawlLatestPlayerEUW`.

- [ ] **Step 4: Run crawler tests**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerServiceImplRotationTest,CrawlerServiceImplPaginationTest test
```

Expected: PASS; the existing pagination behavior remains unchanged.

- [ ] **Step 5: Commit the crawler-service slice**

```powershell
git add src/main/java/org/main/service/CrawlerService.java src/main/java/org/main/service/CrawlerServiceImpl.java src/test/java/org/main/service/CrawlerServiceImplRotationTest.java
git commit -m "feat: rotate scheduled player crawls"
```

---

### Task 3: Expose Live Capacity and Calculate Safe Background Budgets

**Files:**
- Modify: `src/main/java/org/main/client/RiotRateLimiter.java`
- Create: `src/test/java/org/main/client/RiotRateLimiterTest.java`
- Create: `src/main/java/org/main/service/scheduler/MaintenanceBudget.java`
- Create: `src/test/java/org/main/service/scheduler/MaintenanceBudgetTest.java`

**Interfaces:**
- Produces: `RiotRateLimiter.getPerTwoMinuteLimit(): int`
- Produces: `RiotRateLimiter.remainingTwoMinuteCapacity(): int`
- Produces package-private pure methods:
  - `MaintenanceBudget.cycleBudget(int configuredLimit, int remainingCapacity, int headroom): int`
  - `MaintenanceBudget.integrityProtectedBudget(int cycleBudget, int sharePercent): int`
  - `MaintenanceBudget.crawlerWorstCaseCost(int matchLimit): int`
  - `MaintenanceBudget.maxCrawlerMatches(int requestBudget): int`

- [ ] **Step 1: Write failing rate-limiter capacity tests**

Create `RiotRateLimiterTest`:

```java
package org.main.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiotRateLimiterTest {

    @Test
    void reportsConfiguredAndRemainingTwoMinuteCapacity() {
        RiotRateLimiter limiter = new RiotRateLimiter(true, 5, 85);

        assertThat(limiter.getPerTwoMinuteLimit()).isEqualTo(85);
        assertThat(limiter.remainingTwoMinuteCapacity()).isEqualTo(85);

        limiter.acquire();

        assertThat(limiter.remainingTwoMinuteCapacity()).isEqualTo(84);
    }

    @Test
    void disabledLimiterReportsItsConfiguredCapacity() {
        RiotRateLimiter limiter = new RiotRateLimiter(false, 5, 85);

        limiter.acquire();

        assertThat(limiter.remainingTwoMinuteCapacity()).isEqualTo(85);
    }
}
```

- [ ] **Step 2: Run the limiter test and verify compilation fails**

Run:

```powershell
.\mvnw.cmd -Dtest=RiotRateLimiterTest test
```

Expected: FAIL to compile because both capacity methods are missing.

- [ ] **Step 3: Add synchronized read-only capacity reporting**

Add to `RiotRateLimiter`:

```java
public int getPerTwoMinuteLimit() {
    return perTwoMinutesLimit;
}

public synchronized int remainingTwoMinuteCapacity() {
    if (!enabled) {
        return perTwoMinutesLimit;
    }

    Instant now = Instant.now();
    removeOldEntries(twoMinuteWindow, now.minusSeconds(120));
    return Math.max(0, perTwoMinutesLimit - twoMinuteWindow.size());
}
```

Do not reserve a permit here and do not modify `acquire()`.

- [ ] **Step 4: Run the limiter test and verify it passes**

Run:

```powershell
.\mvnw.cmd -Dtest=RiotRateLimiterTest test
```

Expected: PASS without sleeping because the test acquires only one permit.

- [ ] **Step 5: Write failing pure budget tests**

Create `MaintenanceBudgetTest`:

```java
package org.main.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaintenanceBudgetTest {

    @Test
    void leavesHeadroomAndUsesTheSmallerLiveCapacity() {
        assertThat(MaintenanceBudget.cycleBudget(85, 85, 1)).isEqualTo(84);
        assertThat(MaintenanceBudget.cycleBudget(85, 40, 1)).isEqualTo(39);
        assertThat(MaintenanceBudget.cycleBudget(85, 0, 1)).isZero();
    }

    @Test
    void protectsHalfForIntegrity() {
        assertThat(MaintenanceBudget.integrityProtectedBudget(84, 50)).isEqualTo(42);
        assertThat(MaintenanceBudget.integrityProtectedBudget(39, 50)).isEqualTo(19);
    }

    @Test
    void includesMatchIdPaginationInWorstCaseCrawlerCost() {
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(20)).isEqualTo(41);
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(21)).isEqualTo(44);
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(40)).isEqualTo(82);
        assertThat(MaintenanceBudget.crawlerWorstCaseCost(41)).isEqualTo(85);
    }

    @Test
    void convertsRequestBudgetToTheLargestSafeMatchLimit() {
        assertThat(MaintenanceBudget.maxCrawlerMatches(42)).isEqualTo(20);
        assertThat(MaintenanceBudget.maxCrawlerMatches(84)).isEqualTo(40);
        assertThat(MaintenanceBudget.maxCrawlerMatches(2)).isZero();
    }
}
```

- [ ] **Step 6: Run the budget test and verify compilation fails**

Run:

```powershell
.\mvnw.cmd -Dtest=MaintenanceBudgetTest test
```

Expected: FAIL to compile because `MaintenanceBudget` does not exist.

- [ ] **Step 7: Implement the package-private budget utility**

Create `MaintenanceBudget.java`:

```java
package org.main.service.scheduler;

final class MaintenanceBudget {

    private static final int MATCH_ID_PAGE_SIZE = 20;
    private static final int MAX_CRAWLER_MATCHES = 100;

    private MaintenanceBudget() {
    }

    static int cycleBudget(int configuredLimit, int remainingCapacity, int headroom) {
        int safeHeadroom = Math.max(0, headroom);
        int configuredBudget = Math.max(0, configuredLimit - safeHeadroom);
        int liveBudget = Math.max(0, remainingCapacity - safeHeadroom);
        return Math.min(configuredBudget, liveBudget);
    }

    static int integrityProtectedBudget(int cycleBudget, int sharePercent) {
        int safeShare = Math.max(0, Math.min(100, sharePercent));
        return Math.max(0, cycleBudget) * safeShare / 100;
    }

    static int crawlerWorstCaseCost(int matchLimit) {
        if (matchLimit <= 0) {
            return 0;
        }

        int pageCalls = (matchLimit + MATCH_ID_PAGE_SIZE - 1) / MATCH_ID_PAGE_SIZE;
        return pageCalls + 2 * matchLimit;
    }

    static int maxCrawlerMatches(int requestBudget) {
        int matches = 0;

        while (matches < MAX_CRAWLER_MATCHES
                && crawlerWorstCaseCost(matches + 1) <= requestBudget) {
            matches++;
        }

        return matches;
    }
}
```

- [ ] **Step 8: Run both capacity test classes**

Run:

```powershell
.\mvnw.cmd -Dtest=RiotRateLimiterTest,MaintenanceBudgetTest test
```

Expected: PASS, including the 42-request/20-match and 84-request/40-match boundaries.

- [ ] **Step 9: Commit the quota slice**

```powershell
git add src/main/java/org/main/client/RiotRateLimiter.java src/test/java/org/main/client/RiotRateLimiterTest.java src/main/java/org/main/service/scheduler/MaintenanceBudget.java src/test/java/org/main/service/scheduler/MaintenanceBudgetTest.java
git commit -m "feat: calculate soft background quotas"
```

---

### Task 4: Replace the Integrity Scheduler with the Combined Coordinator

**Files:**
- Delete: `src/main/java/org/main/service/scheduler/DataIntegrityScheduler.java`
- Create: `src/main/java/org/main/service/scheduler/BackgroundMaintenanceScheduler.java`
- Create: `src/test/java/org/main/service/scheduler/BackgroundMaintenanceSchedulerTest.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/java/org/main/account/PlayerLoopMigrationIT.java`
- Modify: `src/test/java/org/main/account/AccountTokenConcurrencyIT.java`
- Modify: `src/test/java/org/main/account/repository/AccountRepositoryIT.java`
- Modify: `src/test/java/org/main/refresh/PlayerRefreshConcurrencyIT.java`
- Modify: `src/test/java/org/main/builds/ChampionBuildMigrationIT.java`

**Interfaces:**
- Consumes: `CrawlerService.crawlNextPlayerEUW(int): Optional<CrawlResultDto>`
- Consumes: `DataIntegrityService.check()` and `repairMissingTimelines(int)`
- Consumes: live limiter capacity and `MaintenanceBudget` calculations from Task 3.
- Produces: `BackgroundMaintenanceScheduler.runMaintenanceCycle(): void`
- Scheduling: fixed delay `${app.scheduler.background-maintenance.fixed-delay-ms:120000}`, initial delay `${app.scheduler.background-maintenance.initial-delay-ms:60000}`.

- [ ] **Step 1: Write failing coordinator tests for protected work and failure isolation**

Create `BackgroundMaintenanceSchedulerTest`:

```java
package org.main.service.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.main.client.RiotRateLimiter;
import org.main.dto.CrawlResultDto;
import org.main.dto.DataIntegrityReportDto;
import org.main.service.CrawlerService;
import org.main.service.DataIntegrityService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BackgroundMaintenanceSchedulerTest {

    @Test
    void usesProtectedIntegrityHalfThenCrawlerAndBorrowedIntegrity() {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);

        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(85, 43, 9);
        when(integrity.check()).thenReturn(report(50), report(8));
        when(integrity.repairMissingTimelines(42)).thenReturn(report(8));
        when(integrity.repairMissingTimelines(8)).thenReturn(report(0));
        when(crawler.crawlNextPlayerEUW(20)).thenReturn(Optional.of(
                new CrawlResultDto("EUW1", null, "puuid", 20, 0, java.util.List.of())
        ));

        scheduler.runMaintenanceCycle();

        verify(integrity).repairMissingTimelines(42);
        verify(crawler).crawlNextPlayerEUW(20);
        verify(integrity).repairMissingTimelines(8);
    }

    @Test
    void integrityFailureDoesNotPreventCrawler() {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);

        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(85, 85, 1);
        when(integrity.check())
                .thenThrow(new IllegalStateException("integrity failed"))
                .thenReturn(report(0));

        scheduler.runMaintenanceCycle();

        verify(crawler).crawlNextPlayerEUW(40);
    }

    @Test
    void crawlerFailureDoesNotPreventBorrowedIntegrityPass() {
        DataIntegrityService integrity = mock(DataIntegrityService.class);
        CrawlerService crawler = mock(CrawlerService.class);
        RiotRateLimiter limiter = mock(RiotRateLimiter.class);
        BackgroundMaintenanceScheduler scheduler =
                new BackgroundMaintenanceScheduler(integrity, crawler, limiter, 50, 1);

        when(limiter.getPerTwoMinuteLimit()).thenReturn(85);
        when(limiter.remainingTwoMinuteCapacity()).thenReturn(85, 43, 9);
        when(integrity.check()).thenReturn(report(50), report(8));
        when(integrity.repairMissingTimelines(42)).thenReturn(report(8));
        when(integrity.repairMissingTimelines(8)).thenReturn(report(0));
        when(crawler.crawlNextPlayerEUW(20))
                .thenThrow(new IllegalStateException("crawler failed"));

        scheduler.runMaintenanceCycle();

        verify(integrity).repairMissingTimelines(42);
        verify(integrity).repairMissingTimelines(8);
    }

    @Test
    void disabledPropertyPreventsSchedulerBeanCreation() {
        new ApplicationContextRunner()
                .withBean(DataIntegrityService.class, () -> mock(DataIntegrityService.class))
                .withBean(CrawlerService.class, () -> mock(CrawlerService.class))
                .withBean(RiotRateLimiter.class, () -> mock(RiotRateLimiter.class))
                .withUserConfiguration(BackgroundMaintenanceScheduler.class)
                .withPropertyValues("app.scheduler.background-maintenance.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(BackgroundMaintenanceScheduler.class));
    }

    private static DataIntegrityReportDto report(long missingTimelines) {
        boolean valid = missingTimelines == 0;
        return new DataIntegrityReportDto(
                100,
                100 - missingTimelines,
                0,
                0,
                missingTimelines,
                0,
                0,
                valid
        );
    }
}
```

- [ ] **Step 2: Run the scheduler test and verify compilation fails**

Run:

```powershell
.\mvnw.cmd -Dtest=BackgroundMaintenanceSchedulerTest test
```

Expected: FAIL to compile because `BackgroundMaintenanceScheduler` does not exist.

- [ ] **Step 3: Implement the combined coordinator**

Create `BackgroundMaintenanceScheduler.java`:

```java
package org.main.service.scheduler;

import java.time.Duration;
import java.time.Instant;
import org.main.client.RiotRateLimiter;
import org.main.dto.CrawlResultDto;
import org.main.dto.DataIntegrityReportDto;
import org.main.service.CrawlerService;
import org.main.service.DataIntegrityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.scheduler.background-maintenance.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BackgroundMaintenanceScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(BackgroundMaintenanceScheduler.class);

    private final DataIntegrityService dataIntegrityService;
    private final CrawlerService crawlerService;
    private final RiotRateLimiter riotRateLimiter;
    private final int integritySharePercent;
    private final int headroomRequests;

    public BackgroundMaintenanceScheduler(
            DataIntegrityService dataIntegrityService,
            CrawlerService crawlerService,
            RiotRateLimiter riotRateLimiter,
            @Value("${app.scheduler.background-maintenance.integrity-share-percent:50}")
            int integritySharePercent,
            @Value("${app.scheduler.background-maintenance.headroom-requests:1}")
            int headroomRequests
    ) {
        this.dataIntegrityService = dataIntegrityService;
        this.crawlerService = crawlerService;
        this.riotRateLimiter = riotRateLimiter;
        this.integritySharePercent = integritySharePercent;
        this.headroomRequests = headroomRequests;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.background-maintenance.fixed-delay-ms:120000}",
            initialDelayString = "${app.scheduler.background-maintenance.initial-delay-ms:60000}"
    )
    public void runMaintenanceCycle() {
        Instant startedAt = Instant.now();
        int initialBudget = availableBackgroundRequests();

        if (initialBudget == 0) {
            log.debug("Background maintenance skipped because no Riot capacity is available");
            return;
        }

        int protectedIntegrityBudget = MaintenanceBudget.integrityProtectedBudget(
                initialBudget,
                integritySharePercent
        );

        int protectedCrawlerBudget = initialBudget - protectedIntegrityBudget;
        IntegrityOutcome protectedIntegrity = runProtectedIntegrity(protectedIntegrityBudget);
        CrawlOutcome crawl = runCrawlerWithAvailableCapacity(protectedCrawlerBudget);
        IntegrityOutcome borrowedIntegrity = runBorrowedIntegrityWithAvailableCapacity();

        log.info(
                "Background maintenance cycle finished: protectedTimelinesRepaired={}, "
                        + "borrowedTimelinesRepaired={}, crawlerPuuid='{}', "
                        + "savedNewMatches={}, crawlerExtraBudget={}, failures={}, durationMs={}",
                protectedIntegrity.repairedRawTimelines(),
                borrowedIntegrity.repairedRawTimelines(),
                crawl.puuid(),
                crawl.savedNewMatches(),
                crawl.extraBudgetOffered(),
                (protectedIntegrity.failed() ? 1 : 0)
                        + (crawl.failed() ? 1 : 0)
                        + (borrowedIntegrity.failed() ? 1 : 0),
                Duration.between(startedAt, Instant.now()).toMillis()
        );
    }

    private IntegrityOutcome runProtectedIntegrity(int requestBudget) {
        if (requestBudget <= 0) {
            return IntegrityOutcome.success(0);
        }

        try {
            DataIntegrityReportDto before = dataIntegrityService.check();

            if (before.valid()) {
                return IntegrityOutcome.success(0);
            }

            DataIntegrityReportDto after =
                    dataIntegrityService.repairMissingTimelines(requestBudget);
            long repaired = Math.max(
                    0,
                    before.matchesWithoutTimelineRaw() - after.matchesWithoutTimelineRaw()
            );
            return IntegrityOutcome.success(repaired);
        } catch (Exception ex) {
            log.error("Protected data integrity phase failed", ex);
            return IntegrityOutcome.failure();
        }
    }

    private CrawlOutcome runCrawlerWithAvailableCapacity(int protectedCrawlerBudget) {
        int availableRequests = availableBackgroundRequests();
        int extraBudgetOffered = Math.max(0, availableRequests - protectedCrawlerBudget);
        int matchLimit = MaintenanceBudget.maxCrawlerMatches(availableRequests);

        if (matchLimit == 0) {
            return CrawlOutcome.empty(0);
        }

        try {
            var result = crawlerService.crawlNextPlayerEUW(matchLimit);

            if (result.isEmpty()) {
                log.debug("Background crawl skipped because no player is stored");
                return CrawlOutcome.empty(0);
            }

            CrawlResultDto crawl = result.get();
            return new CrawlOutcome(
                    crawl.puuid(),
                    crawl.savedNewMatches(),
                    extraBudgetOffered,
                    false
            );
        } catch (Exception ex) {
            log.error("Background crawler phase failed", ex);
            return CrawlOutcome.failure(extraBudgetOffered);
        }
    }

    private IntegrityOutcome runBorrowedIntegrityWithAvailableCapacity() {
        try {
            DataIntegrityReportDto before = dataIntegrityService.check();
            int requestBudget = availableBackgroundRequests();
            int repairLimit = (int) Math.min(
                    before.matchesWithoutTimelineRaw(),
                    (long) requestBudget
            );

            if (repairLimit == 0) {
                return IntegrityOutcome.success(0);
            }

            DataIntegrityReportDto after = dataIntegrityService.repairMissingTimelines(repairLimit);
            long repaired = Math.max(
                    0,
                    before.matchesWithoutTimelineRaw() - after.matchesWithoutTimelineRaw()
            );
            return IntegrityOutcome.success(repaired);
        } catch (Exception ex) {
            log.error("Borrowed data integrity phase failed", ex);
            return IntegrityOutcome.failure();
        }
    }

    private int availableBackgroundRequests() {
        return MaintenanceBudget.cycleBudget(
                riotRateLimiter.getPerTwoMinuteLimit(),
                riotRateLimiter.remainingTwoMinuteCapacity(),
                headroomRequests
        );
    }

    private record IntegrityOutcome(long repairedRawTimelines, boolean failed) {

        private static IntegrityOutcome success(long repairedRawTimelines) {
            return new IntegrityOutcome(repairedRawTimelines, false);
        }

        private static IntegrityOutcome failure() {
            return new IntegrityOutcome(0, true);
        }
    }

    private record CrawlOutcome(
            String puuid,
            int savedNewMatches,
            int extraBudgetOffered,
            boolean failed
    ) {

        private static CrawlOutcome empty(int extraBudgetOffered) {
            return new CrawlOutcome(null, 0, extraBudgetOffered, false);
        }

        private static CrawlOutcome failure(int extraBudgetOffered) {
            return new CrawlOutcome(null, 0, extraBudgetOffered, true);
        }
    }
}
```

Delete `DataIntegrityScheduler.java`; leaving it active would run duplicate integrity work.

- [ ] **Step 4: Bind the exact scheduler properties**

Replace the old data-integrity scheduler line in `application.properties` with:

```properties
app.scheduler.background-maintenance.enabled=${APP_SCHEDULER_BACKGROUND_MAINTENANCE_ENABLED:true}
app.scheduler.background-maintenance.initial-delay-ms=${APP_SCHEDULER_BACKGROUND_MAINTENANCE_INITIAL_DELAY_MS:60000}
app.scheduler.background-maintenance.fixed-delay-ms=${APP_SCHEDULER_BACKGROUND_MAINTENANCE_FIXED_DELAY_MS:120000}
app.scheduler.background-maintenance.integrity-share-percent=${APP_SCHEDULER_BACKGROUND_MAINTENANCE_INTEGRITY_SHARE_PERCENT:50}
app.scheduler.background-maintenance.headroom-requests=${APP_SCHEDULER_BACKGROUND_MAINTENANCE_HEADROOM_REQUESTS:1}
```

Keep `app.scheduler.match-analysis.enabled` unchanged.

- [ ] **Step 5: Update integration-test scheduler overrides**

In these five test files, replace:

```java
"app.scheduler.data-integrity.enabled=false"
```

with:

```java
"app.scheduler.background-maintenance.enabled=false"
```

Files:

- `src/test/java/org/main/account/PlayerLoopMigrationIT.java`
- `src/test/java/org/main/account/AccountTokenConcurrencyIT.java`
- `src/test/java/org/main/account/repository/AccountRepositoryIT.java`
- `src/test/java/org/main/refresh/PlayerRefreshConcurrencyIT.java`
- `src/test/java/org/main/builds/ChampionBuildMigrationIT.java`

- [ ] **Step 6: Run the coordinator and context tests**

Run:

```powershell
.\mvnw.cmd -Dtest=BackgroundMaintenanceSchedulerTest,RepositoryProxyConfigurationTest test
```

Expected: PASS. The disabled property produces no scheduler bean, and interface-proxy startup remains valid.

- [ ] **Step 7: Confirm no old scheduler references remain**

Run:

```powershell
rg -n "DataIntegrityScheduler|app.scheduler.data-integrity" src/main src/test
```

Expected: no output.

- [ ] **Step 8: Commit the coordinator slice**

```powershell
git add src/main/java/org/main/service/scheduler/DataIntegrityScheduler.java src/main/java/org/main/service/scheduler/BackgroundMaintenanceScheduler.java src/test/java/org/main/service/scheduler/BackgroundMaintenanceSchedulerTest.java src/main/resources/application.properties src/test/java/org/main/account/PlayerLoopMigrationIT.java src/test/java/org/main/account/AccountTokenConcurrencyIT.java src/test/java/org/main/account/repository/AccountRepositoryIT.java src/test/java/org/main/refresh/PlayerRefreshConcurrencyIT.java src/test/java/org/main/builds/ChampionBuildMigrationIT.java
git commit -m "feat: run continuous background maintenance"
```

---

### Task 5: Full Verification and Startup Smoke Test

**Files:**
- Verify only; do not add production code in this task.

**Interfaces:**
- Verifies all interfaces and acceptance criteria produced by Tasks 1-4.

- [ ] **Step 1: Run all focused maintenance tests together**

Run:

```powershell
.\mvnw.cmd -Dtest=CrawlerRotationMigrationIT,CrawlerServiceImplRotationTest,CrawlerServiceImplPaginationTest,RiotRateLimiterTest,MaintenanceBudgetTest,BackgroundMaintenanceSchedulerTest test
```

Expected: BUILD SUCCESS with all named tests passing.

- [ ] **Step 2: Run Checkstyle**

Run:

```powershell
.\mvnw.cmd checkstyle:check
```

Expected: BUILD SUCCESS and `0 Checkstyle violations`.

- [ ] **Step 3: Run the full automated test suite**

Run:

```powershell
.\mvnw.cmd test
```

Expected: BUILD SUCCESS with zero failures and zero errors. If Docker is unavailable, report the skipped Testcontainers verification explicitly and do not claim the migration test passed.

- [ ] **Step 4: Start the application with the file-based Riot key overriding stale process state**

Run from PowerShell:

```powershell
$env:RIOT_API_KEY = (Get-Content .env | Where-Object { $_ -match '^RIOT_API_KEY=' } | Select-Object -First 1).Split('=', 2)[1].Trim()
.\mvnw.cmd spring-boot:run
```

Expected within 70 seconds:

- Spring reports that the application started.
- One `Background maintenance cycle finished` log appears after the 60-second initial delay.
- No `401`, `403`, duplicate scheduler, or application-context error appears.
- The selected player's `last_crawl_attempt_at` advances even when no new match is saved.

Stop the server with `Ctrl+C` after observing one complete cycle.

- [ ] **Step 5: Verify the worktree is clean**

Run:

```powershell
git status --short
```

Expected: no output. If a verification command generated files, remove only those known generated artifacts before concluding.
