# Repository Slimming and Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce tracked repository size and database/browser overhead without removing or changing any existing HTTP capability.

**Architecture:** Preserve the Spring MVC monolith and its public interfaces. Replace repeated persistence calls with existing Spring JDBC/JPA batching, move repair filtering into PostgreSQL, reuse dashboard data already computed by the server, and keep repository hygiene changes separate from behavior changes.

**Tech Stack:** Java 23, Spring Boot 3.3.3, Spring Data JPA, JdbcTemplate, PostgreSQL 16, Flyway, JUnit 5, Mockito, MockMvc, plain JavaScript.

## Global Constraints

- Preserve every crawler, dataset, integrity, analysis, static-data, account, build, and frontend endpoint.
- Do not rewrite Git history.
- Do not add runtime dependencies.
- Keep transaction-per-match and delete-before-rebuild semantics.
- Every production behavior change must follow RED → GREEN.
- Run Maven with `JAVA_HOME=C:\Users\egors\.jdks\corretto-23.0.2`.

---

### Task 1: Stop Tracking Reproducible Artifacts

**Files:**
- Modify: `.gitignore`
- Remove from Git index: `target-cache/**`, `graphify-out/**`, `logs/**`

**Interfaces:**
- Consumes: Maven Wrapper and Graphify CLI regeneration behavior.
- Produces: a source-only working tree without local caches in Git.

- [ ] **Step 1: Record the current tracked artifact totals**

Run:

```powershell
git ls-files target-cache graphify-out logs | Measure-Object
git count-objects -vH
```

Expected: thousands of tracked cache files and a pack around 500 MB.

- [ ] **Step 2: Add explicit root ignores**

Append these entries to `.gitignore`:

```gitignore
/target-cache/
/graphify-out/
/logs/
```

- [ ] **Step 3: Remove artifacts from the Git index and working tree**

Resolve each target to the repository root first, then remove only these three exact directories. Recreate empty local directories only when a runtime command requires them; ignored directories do not need placeholders.

- [ ] **Step 4: Verify repository hygiene**

Run:

```powershell
git check-ignore target-cache graphify-out logs
git ls-files target-cache graphify-out logs
```

Expected: all three paths are ignored and the tracked-file command prints nothing.

- [ ] **Step 5: Commit**

```powershell
git add .gitignore
git add -u -- target-cache graphify-out logs
git commit -m "chore: stop tracking generated artifacts"
```

---

### Task 2: Batch Timeline Frames and Events

**Files:**
- Create: `src/test/java/org/main/service/TimelineIngestServiceImplTest.java`
- Modify: `src/main/java/org/main/service/TimelineIngestServiceImpl.java`

**Interfaces:**
- Consumes: `MatchTimelineFrameRepository.saveAll(Iterable)` and `MatchTimelineEventRepository.saveAll(Iterable)`.
- Produces: `rebuildTimelineProjections(String, JsonNode)` behavior with one frame batch and one event batch.

- [ ] **Step 1: Write the failing batch test**

Create a Mockito test that stores raw timeline JSON with two frames and three events, calls `repairTimelineFromRaw("EUW1_1")`, captures both `saveAll` arguments, and asserts:

```java
assertThat(frames.getValue()).hasSize(2);
assertThat(events.getValue()).hasSize(3);
verify(frameRepository, times(1)).saveAll(any());
verify(eventRepository, times(1)).saveAll(any());
verify(frameRepository, never()).save(any());
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
./mvnw.cmd -Dtest=TimelineIngestServiceImplTest test
```

Expected: FAIL because frames are currently saved individually and events are saved once per frame.

- [ ] **Step 3: Implement the minimum batching change**

In `rebuildTimelineProjections`, collect entities before persistence:

```java
List<MatchTimelineFrameEntity> frameEntities = new ArrayList<>();
List<MatchTimelineEventEntity> eventEntities = new ArrayList<>();
int frameNo = 0;
for (JsonNode frame : frames) {
    frameEntities.add(buildFrameEntity(matchId, frameNo, frame));
    addFrameEvents(eventEntities, matchId, frameNo, frame);
    frameNo++;
}
if (!frameEntities.isEmpty()) {
    timelineFrameRepository.saveAll(frameEntities);
}
if (!eventEntities.isEmpty()) {
    timelineEventRepository.saveAll(eventEntities);
}
```

Convert `saveFrame` to `buildFrameEntity` and `saveFrameEvents` to a collector method. Keep deletes, flushes, validation, and transaction annotations unchanged.

- [ ] **Step 4: Run focused and service tests**

```powershell
./mvnw.cmd -Dtest=TimelineIngestServiceImplTest,DataIntegrityServiceImplTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/org/main/service/TimelineIngestServiceImpl.java src/test/java/org/main/service/TimelineIngestServiceImplTest.java
git commit -m "perf: batch timeline projection writes"
```

---

### Task 3: Bound Integrity Repair Queries in PostgreSQL

**Files:**
- Modify: `src/main/java/org/main/persistence/repository/PlayerRepository.java`
- Modify: `src/main/java/org/main/service/DataIntegrityServiceImpl.java`
- Modify: `src/test/java/org/main/service/DataIntegrityServiceImplTest.java`
- Create: `src/test/java/org/main/persistence/repository/PlayerRepositoryIT.java`

**Interfaces:**
- Produces: `List<PlayerEntity> findPlayersMissingProfiles(Pageable pageable)`.
- Produces: `List<PlayerEntity> findPlayersMissingRanks(Pageable pageable)`.
- Consumes: `PageRequest.of(0, limit)` from `DataIntegrityServiceImpl`.

- [ ] **Step 1: Write failing service tests**

Add tests that call each repair method and verify bounded repository methods are used while `findAll()` is not:

```java
verify(playerRepository).findPlayersMissingProfiles(PageRequest.of(0, 25));
verify(playerRepository, never()).findAll();
```

Use the equivalent assertion for missing ranks.

- [ ] **Step 2: Verify RED**

```powershell
./mvnw.cmd -Dtest=DataIntegrityServiceImplTest test
```

Expected: compilation failure because the bounded repository methods do not exist.

- [ ] **Step 3: Add repository queries**

Use JPQL for profile selection and native SQL for rank absence:

```java
@Query("""
        select player
        from PlayerEntity player
        where player.puuid is not null
          and player.puuid <> ''
          and player.profileIconId is null
        order by player.updatedAt asc, player.puuid asc
        """)
List<PlayerEntity> findPlayersMissingProfiles(Pageable pageable);

@Query(value = """
        select player.*
        from raw.players player
        where player.puuid is not null
          and player.puuid <> ''
          and not exists (
              select 1 from raw.league_entries entry where entry.puuid = player.puuid
          )
        order by player.updated_at asc, player.puuid asc
        """, nativeQuery = true)
List<PlayerEntity> findPlayersMissingRanks(Pageable pageable);
```

- [ ] **Step 4: Replace both `findAll()` pipelines**

Use the repository result directly and retain the existing per-player try/catch and counters:

```java
List<PlayerEntity> players = playerRepository.findPlayersMissingProfiles(PageRequest.of(0, limit));
```

- [ ] **Step 5: Add repository integration coverage**

Create fixture rows covering blank PUUID, existing profile, existing rank, and two valid candidates. Assert both filtering and page size. Reuse the project's PostgreSQL Testcontainers pattern.

- [ ] **Step 6: Run tests**

```powershell
./mvnw.cmd -Dtest=DataIntegrityServiceImplTest,PlayerRepositoryIT test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/org/main/persistence/repository/PlayerRepository.java src/main/java/org/main/service/DataIntegrityServiceImpl.java src/test/java/org/main/service/DataIntegrityServiceImplTest.java src/test/java/org/main/persistence/repository/PlayerRepositoryIT.java
git commit -m "perf: bound integrity repair candidate queries"
```

---

### Task 4: Reuse Recent Matches from the Dashboard Response

**Files:**
- Modify: `src/main/java/org/main/dto/frontend/PlayerDashboardDto.java`
- Modify: `src/main/java/org/main/service/frontend/PlayerDashboardServiceImpl.java`
- Modify: `src/test/java/org/main/service/frontend/PlayerDashboardServiceTest.java`
- Modify: `src/test/java/org/main/controller/frontend/PlayerDashboardControllerWebMvcTest.java`
- Modify: `src/main/resources/static/js/player.js`
- Modify: `src/test/java/org/main/frontend/StaticPlayerDashboardTest.java`

**Interfaces:**
- Produces: `PlayerDashboardDto.recentMatches()` as `List<PlayerRecentMatchDto>`.
- Consumes: existing `renderPlayerMatches(List)` browser function.

- [ ] **Step 1: Write failing Java tests**

Assert that the service returns exactly the matches it already loaded:

```java
assertThat(dashboard.recentMatches()).containsExactlyElementsOf(matches());
```

Add a controller JSON assertion for `$.recentMatches`.

- [ ] **Step 2: Write the failing static frontend assertion**

Extend `StaticPlayerDashboardTest` to require `renderPlayerMatches(dashboard.recentMatches || [])` and to reject the initial overview call to `api.getPlayerMatches`.

- [ ] **Step 3: Verify RED**

```powershell
./mvnw.cmd -Dtest=PlayerDashboardServiceTest,PlayerDashboardControllerWebMvcTest,StaticPlayerDashboardTest test
```

Expected: FAIL because the DTO has no `recentMatches` field and `player.js` refetches it.

- [ ] **Step 4: Add matches to the DTO and service response**

Add this component after `recentForm`:

```java
List<PlayerRecentMatchDto> recentMatches,
```

Pass the already loaded `matches` list when constructing `PlayerDashboardDto`.

- [ ] **Step 5: Reuse the payload in JavaScript**

In `renderDashboard`:

```javascript
renderPlayerMatches(dashboard.recentMatches || []);
```

For the initial overview tab, request only rank history. On queue change, clear the queue-specific loaded key and render the new dashboard matches before loading optional tab data.

- [ ] **Step 6: Run focused tests**

```powershell
./mvnw.cmd -Dtest=PlayerDashboardServiceTest,PlayerDashboardControllerWebMvcTest,StaticPlayerDashboardTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/org/main/dto/frontend/PlayerDashboardDto.java src/main/java/org/main/service/frontend/PlayerDashboardServiceImpl.java src/test/java/org/main/service/frontend/PlayerDashboardServiceTest.java src/test/java/org/main/controller/frontend/PlayerDashboardControllerWebMvcTest.java src/main/resources/static/js/player.js src/test/java/org/main/frontend/StaticPlayerDashboardTest.java
git commit -m "perf: reuse dashboard match payload"
```

---

### Task 5: Batch Match Analysis Writes

**Files:**
- Create: `src/test/java/org/main/service/analysis/MatchAnalysisServiceImplTest.java`
- Modify: `src/main/java/org/main/service/analysis/MatchAnalysisServiceImpl.java`

**Interfaces:**
- Consumes: `JdbcTemplate.batchUpdate(String, List<Object[]>)`.
- Produces: the same core rows and analysis status with bounded JDBC calls.

- [ ] **Step 1: Add a failing participant/item/rune batch test**

Construct one Riot match JSON with two participants, final items, and rune selections. Stub source loading and timeline presence, execute `processMatch`, and capture `batchUpdate` argument lists. Assert player and participant batch sizes are 2 and item/rune batches contain the expected rows.

- [ ] **Step 2: Verify RED**

```powershell
./mvnw.cmd -Dtest=MatchAnalysisServiceImplTest test
```

Expected: FAIL because current code invokes `jdbcTemplate.update` inside loops.

- [ ] **Step 3: Batch player upserts**

Build `List<Object[]>` for usable PUUIDs and call one `batchUpdate` using the current `INSERT ... ON CONFLICT` SQL. Skip the call for an empty list.

- [ ] **Step 4: Batch participants, final items, and runes**

Parse each participant once into `ParticipantContext`, then append parameter arrays to three lists. Submit one batch per table. Preserve every current column, conflict target, and timestamp value.

- [ ] **Step 5: Batch item events and skill order**

Reuse the existing loaded event lists, map valid entries to arrays, and submit one batch for each destination table. Invalid participant references continue to be skipped exactly as today.

- [ ] **Step 6: Consolidate deletes without changing transaction behavior**

Replace five Java calls with one SQL string containing five statements only if PostgreSQL JDBC accepts it under the existing test configuration. Otherwise retain the five deletes; batching repeated inserts is the material optimization.

- [ ] **Step 7: Run focused tests**

```powershell
./mvnw.cmd -Dtest=MatchAnalysisServiceImplTest,MatchAnalysisSchedulerTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/org/main/service/analysis/MatchAnalysisServiceImpl.java src/test/java/org/main/service/analysis/MatchAnalysisServiceImplTest.java
git commit -m "perf: batch match analysis writes"
```

---

### Task 6: Add Safe Performance Indexes

**Files:**
- Create: `src/main/resources/db/migration/V4__add_query_performance_indexes.sql`
- Create: `src/test/java/org/main/persistence/PerformanceIndexMigrationIT.java`

**Interfaces:**
- Consumes: existing `raw`, `core`, and timeline tables.
- Produces: idempotent indexes for current query predicates.

- [ ] **Step 1: Write the failing migration integration test**

Start PostgreSQL with the minimum existing test schemas/tables, run the migration twice, and query `pg_indexes` for these names:

```text
ix_raw_matches_analysis_queue
ix_core_participants_puuid_match
ix_core_participants_champion
ix_timeline_frames_match
ix_timeline_events_match
ix_raw_players_missing_profile
```

- [ ] **Step 2: Verify RED**

```powershell
./mvnw.cmd -Dtest=PerformanceIndexMigrationIT test
```

Expected: FAIL because V4 and its indexes do not exist.

- [ ] **Step 3: Add non-destructive indexes**

Use only `CREATE INDEX IF NOT EXISTS`, including:

```sql
create index if not exists ix_raw_matches_analysis_queue
    on raw.matches (analysis_status, fetched_at)
    where raw_match_json is not null;

create index if not exists ix_core_participants_puuid_match
    on core.participants (puuid, match_id);

create index if not exists ix_core_participants_champion
    on core.participants (champion_id);

create index if not exists ix_timeline_frames_match
    on raw.match_timeline_frames (match_id);

create index if not exists ix_timeline_events_match
    on raw.match_timeline_events (match_id);

create index if not exists ix_raw_players_missing_profile
    on raw.players (updated_at, puuid)
    where profile_icon_id is null and puuid is not null and puuid <> '';
```

Before finalizing table names, verify them against entity `@Table` declarations and repository SQL.

- [ ] **Step 4: Run migration tests**

```powershell
./mvnw.cmd -Dtest=PerformanceIndexMigrationIT test
```

Expected: PASS on both first and repeated application.

- [ ] **Step 5: Commit**

```powershell
git add src/main/resources/db/migration/V4__add_query_performance_indexes.sql src/test/java/org/main/persistence/PerformanceIndexMigrationIT.java
git commit -m "perf: index frequent database queries"
```

---

### Task 7: Remove Trivial Delegation and Verify Compatibility

**Files:**
- Modify: `src/main/java/org/main/service/frontend/FrontendStatsService.java`
- Modify: `src/main/java/org/main/service/frontend/FrontendStatsServiceImpl.java`
- Modify: `src/main/java/org/main/controller/frontend/MatchController.java`
- Modify: matching controller/service tests

**Interfaces:**
- Consumes: `MatchDetailsService.getMatchDetails(String, String)`.
- Preserves: `GET /api/matches/{matchId}/details`.

- [ ] **Step 1: Change the controller test first**

Mock `MatchDetailsService` directly and verify the unchanged endpoint delegates to it. Run the test before production changes; it must fail because the controller currently depends on `FrontendStatsService`.

- [ ] **Step 2: Inject `MatchDetailsService` directly**

Remove only the forwarding method from `FrontendStatsService` and `FrontendStatsServiceImpl`. Do not remove either service or any HTTP path.

- [ ] **Step 3: Run frontend service/controller tests**

```powershell
./mvnw.cmd -Dtest='org.main.controller.frontend.*Test,org.main.service.frontend.*Test' test
```

Expected: PASS.

- [ ] **Step 4: Commit**

```powershell
git add src/main/java/org/main/service/frontend/FrontendStatsService.java src/main/java/org/main/service/frontend/FrontendStatsServiceImpl.java src/main/java/org/main/controller/frontend/MatchController.java src/test/java
git commit -m "refactor: remove match details forwarding layer"
```

---

### Task 8: Full Verification and Measurements

**Files:**
- Modify only if verification reveals a regression.

**Interfaces:**
- Produces: evidence that the optimized repository preserves behavior.

- [ ] **Step 1: Run Checkstyle**

```powershell
./mvnw.cmd checkstyle:check
```

Expected: zero violations.

- [ ] **Step 2: Run all tests**

```powershell
./mvnw.cmd test
```

Expected: at least 217 tests, zero failures, zero errors.

- [ ] **Step 3: Build the application**

```powershell
./mvnw.cmd package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Inspect contracts and diff**

```powershell
rg -n "@(Get|Post|Put|Delete|Patch|Request)Mapping" src/main/java/org/main
git diff --check HEAD~7..HEAD
git status --short
```

Expected: all original endpoint mappings remain, no whitespace errors, no unintended generated files.

- [ ] **Step 5: Measure repository reduction**

```powershell
git ls-files | Measure-Object
git ls-files target-cache graphify-out logs
git count-objects -vH
```

Expected: no tracked artifact paths. Note that pack size remains large until a separately approved history rewrite.

- [ ] **Step 6: Record final outcome**

Report test count, build status, changed files, removed tracked bytes, remaining history size, and any optimization deferred because live `EXPLAIN ANALYZE` evidence was unavailable.
