# Champion Builds Implementation Plan

> **For implementation:** Use `subagent-driven-development` task by task. Each task must pass its focused tests and both specification reviews before the next task starts.

**Goal:** Add public, backend-calculated champion builds to champion pages using prepared EUW1 snapshots, explicit confidence, and a deterministic no-cross-queue/no-cross-role fallback ladder.

**Architecture:** A scheduled backend pipeline reads the existing `core`, `raw`, and `static` schemas, converts each eligible participant into a build observation, aggregates current/previous-patch cohorts, and atomically publishes immutable JSONB snapshots in a new `builds` schema. Public GET endpoints resolve an exact matchup or safe fallback entirely on the server. The browser only owns filter state, rendering, accessibility, URL synchronization, and last-successful-response caching.

**Tech Stack:** Java 23, Spring Boot 3.3, Spring MVC, `JdbcTemplate`, PostgreSQL 16, Flyway, Jackson, JUnit 5, AssertJ, Testcontainers, plain HTML/CSS/JavaScript.

**Design source:** `docs/superpowers/specs/2026-07-13-champion-builds-design.md`

---

## Global constraints

- Builds are public; no account or session is required.
- Data region is EUW1/Europe.
- Ranked Solo/Duo (`420`) and Ranked Flex (`440`) are never mixed.
- The first publishable queue is Solo/Duo. Flex remains visible but reports insufficient data until its own cohorts qualify.
- The selected patch anchors a two-patch window weighted `0.70` for the selected patch and `0.30` for the immediately previous patch.
- Champion-role is the baseline cohort. Exact champion-role-opponent data is eligible only at 10 games.
- Confidence is `INSUFFICIENT` below 10 games, `LOW` at 10–24, `MEDIUM` at 25–49, and `HIGH` at 50 or more.
- No request handler calls Riot or performs match aggregation.
- All ranking, weighting, confidence, cohort selection, and fallback decisions live on the backend.
- JavaScript may manage URL state, rendering, accessibility, and a last-successful-response cache; it must not calculate build recommendations.
- The fallback order is exact matchup, champion-role, up to two earlier patch baselines, last published snapshot after aggregation failure, browser cache after request failure, then unavailable.
- Fallback never crosses queue or role.
- The existing `GET /api/champions/{championId}/items` endpoint remains a separate legacy “stored item statistics” view.
- Side-by-side aggregation versions are retained. A failed run never replaces a published snapshot.
- Do not add a client framework, ORM entity graph, message broker, distributed lock, or new JavaScript dependency for this feature.

## Required verification environment

Run Maven commands from the repository root in PowerShell with Java 23:

```powershell
$env:JAVA_HOME='C:\Users\egors\.jdks\corretto-23.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

---

### Task 1: Create the snapshot schema and prove its publication invariants

**Files:**

- Create: `src/main/resources/db/migration/V2__create_champion_build_schema.sql`
- Create: `src/test/java/org/main/builds/ChampionBuildMigrationIT.java`

**Interfaces:**

The migration creates exactly two owned tables:

```text
builds.aggregation_run
  id, aggregation_version, anchor_patch, comparison_patch, queue_id,
  input_watermark, state, source_match_count, validation_count, snapshot_count,
  failure_category, started_at, completed_at

builds.champion_build_snapshot
  id, run_id, aggregation_version, payload_schema_version,
  anchor_patch, comparison_patch, queue_id, champion_id, role,
  opponent_champion_id, scope,
  games, wins, anchor_games, comparison_games, confidence,
  input_watermark, source_match_count, calculated_at, published_at,
  publication_state, payload
```

Use `uuid` identifiers, `jsonb` payloads, `timestamptz` timestamps, queue checks limited to `420` and `440`, role checks limited to `TOP`, `JUNGLE`, `MIDDLE`, `BOTTOM`, and `UTILITY`, and state checks with closed value sets. Add a `UNIQUE NULLS NOT DISTINCT` constraint beginning with `run_id` so a cohort cannot duplicate within one run while a later run can prepare a replacement. Add a partial unique index on the logical cohort key for `PUBLISHED` rows, a partial unique index preventing two `RUNNING` runs for the same aggregation version, patch window, and queue, and a partial lookup index over published cohort keys.

- [ ] Write a failing Testcontainers migration test that queries `information_schema.tables` and expects both tables in `builds`.
- [ ] Add failing assertions for the cohort/version unique constraint, the single-running-run partial index, and the published lookup index.
- [ ] Run `./mvnw.cmd "-Dtest=ChampionBuildMigrationIT" test`; confirm it fails because migration V2 does not exist.
- [ ] Implement the migration. Start with `create schema if not exists builds;`; do not change Flyway's default schema or existing V1.
- [ ] Re-run `./mvnw.cmd "-Dtest=ChampionBuildMigrationIT" test`; expect `BUILD SUCCESS` and one passing test class.
- [ ] Commit with `git add src/main/resources/db/migration/V2__create_champion_build_schema.sql src/test/java/org/main/builds/ChampionBuildMigrationIT.java && git commit -m "feat: add champion build snapshot schema"`.

### Task 2: Implement the pure build rules before any database orchestration

**Files:**

- Create: `src/main/java/org/main/builds/model/BuildQueue.java`
- Create: `src/main/java/org/main/builds/model/BuildRole.java`
- Create: `src/main/java/org/main/builds/model/BuildScope.java`
- Create: `src/main/java/org/main/builds/model/BuildConfidence.java`
- Create: `src/main/java/org/main/builds/model/PatchVersion.java`
- Create: `src/main/java/org/main/builds/model/PatchWindow.java`
- Create: `src/main/java/org/main/builds/model/BuildCandidate.java`
- Create: `src/main/java/org/main/builds/BuildProperties.java`
- Create: `src/main/java/org/main/builds/BuildConfiguration.java`
- Create: `src/main/java/org/main/builds/BuildRules.java`
- Create: `src/test/java/org/main/builds/BuildRulesTest.java`
- Modify: `src/main/resources/application.properties`

**Interfaces:**

```java
public enum BuildQueue {
    SOLO_DUO(420), FLEX(440);
    public int id();
    public static BuildQueue fromId(int queueId);
}

public enum BuildRole {
    TOP, JUNGLE, MIDDLE, BOTTOM, UTILITY;
    public static BuildRole fromParticipant(String teamPosition, String individualPosition);
}

public enum BuildConfidence {
    INSUFFICIENT, LOW, MEDIUM, HIGH
}

public record PatchVersion(int major, int minor) implements Comparable<PatchVersion> {
    public static PatchVersion parse(String gameVersion);
    public String displayName();
}

public record PatchWindow(String anchorPatch, String comparisonPatch) {}

public record BuildCandidate<T>(T value, int anchorPicks, int comparisonPicks,
                                int anchorWins, int comparisonWins) {}

public record BuildProperties(
        int aggregationVersion,
        int payloadSchemaVersion,
        int matchupMinGames,
        int mediumConfidenceGames,
        int highConfidenceGames,
        double anchorPatchWeight,
        double comparisonPatchWeight,
        int historicalLookbackPatches,
        Duration startingItemsCutoff,
        int batchSize,
        Duration schedulerDelay,
        boolean schedulerEnabled
) {}

public final class BuildRules {
    public BuildConfidence confidence(int games);
    public double weightedPickScore(BuildCandidate<?> candidate);
    public double weightedWinRate(BuildCandidate<?> candidate);
    public boolean exactMatchupEligible(int games);
    public List<BuildCandidate<?>> rank(List<BuildCandidate<?>> candidates);
}
```

Bind `app.builds.matchup-min-games=10`, `medium-confidence-games=25`, `high-confidence-games=50`, `anchor-patch-weight=0.70`, `comparison-patch-weight=0.30`, and `historical-lookback-patches=2` exactly as named in the design. Also bind aggregation version `1`, payload schema version `1`, starting-item cutoff `PT2M`, batch size `250`, scheduler delay `PT1H`, and scheduler disabled by default. Validate that weights sum to 1.0 and thresholds are ascending in the compact configuration bean.

- [ ] Write failing parameterized tests for all four confidence boundaries: `0`, `9`, `10`, `24`, `25`, `49`, and `50` games.
- [ ] Write failing tests that reject queue IDs other than `420` and `440`, accept only the five canonical participant positions, use `team_position` before `individual_position`, and reject `MID`, blank, and unknown roles rather than guessing.
- [ ] Write failing numeric patch tests proving `16.10` sorts after `16.9`, `16.9.123.456` normalizes to `16.9`, and malformed versions are rejected rather than guessed.
- [ ] Write failing adjacent-window tests proving `16.9` compares only with `16.8`, never skips to `16.7`, and an annual `16.1` boundary resolves to the highest stored `15.x` patch.
- [ ] Write failing weighted-score tests using `7` current-patch picks and `10` previous-patch picks; assert score `7.9`, not a raw total of `17`.
- [ ] Write a deterministic ranking test: weighted pick score descending, weighted win rate descending, then stable numeric/string ID ascending.
- [ ] Run `./mvnw.cmd "-Dtest=BuildRulesTest" test`; confirm compilation/tests fail.
- [ ] Implement the enums, records, configuration, and pure rules without Spring or database calls inside `BuildRules`.
- [ ] Re-run the focused test; expect `BUILD SUCCESS`.
- [ ] Commit with `git add src/main/java/org/main/builds src/test/java/org/main/builds/BuildRulesTest.java src/main/resources/application.properties && git commit -m "feat: define champion build rules"`.

### Task 3: Convert source rows into deterministic build observations

**Files:**

- Create: `src/main/java/org/main/builds/model/BuildObservation.java`
- Create: `src/main/java/org/main/builds/model/ItemPath.java`
- Create: `src/main/java/org/main/builds/model/RunePage.java`
- Create: `src/main/java/org/main/builds/model/SkillPath.java`
- Create: `src/main/java/org/main/builds/source/BuildSourceMatch.java`
- Create: `src/main/java/org/main/builds/source/BuildSourceSelection.java`
- Create: `src/main/java/org/main/builds/source/BuildSourceRepository.java`
- Create: `src/main/java/org/main/builds/source/JdbcBuildSourceRepository.java`
- Create: `src/main/java/org/main/builds/source/ItemCatalog.java`
- Create: `src/main/java/org/main/builds/source/JdbcItemCatalog.java`
- Create: `src/main/java/org/main/builds/extract/ItemSequenceExtractor.java`
- Create: `src/main/java/org/main/builds/extract/RunePageExtractor.java`
- Create: `src/main/java/org/main/builds/extract/SkillPathExtractor.java`
- Create: `src/main/java/org/main/builds/extract/BuildObservationFactory.java`
- Create: `src/test/java/org/main/builds/extract/ItemSequenceExtractorTest.java`
- Create: `src/test/java/org/main/builds/extract/BuildObservationFactoryTest.java`
- Create: `src/test/java/org/main/builds/source/JdbcBuildSourceRepositoryIT.java`
- Create: `src/test/resources/builds/timeline-item-sequence.json`
- Create: `src/test/resources/builds/participant-perks.json`
- Create: `src/test/resources/builds/build-source-it.sql`

**Interfaces:**

```java
public interface BuildSourceRepository {
    Optional<String> findLatestPatch(BuildQueue queue);
    Optional<String> findPreviousMajorLastPatch(BuildQueue queue, int previousMajor);
    BuildSourceSelection selectSource(PatchWindow window, BuildQueue queue);
    List<BuildSourceMatch> loadBatch(List<String> selectedMatchIds);
}

public record BuildSourceSelection(
        PatchWindow window, BuildQueue queue, OffsetDateTime inputWatermark,
        List<String> matchIds
) {}

public record BuildObservation(
        String matchId, String patch, BuildQueue queue, int championId,
        BuildRole role, Integer opponentChampionId, boolean win,
        ItemPath items, RunePage runes, List<Integer> spells, SkillPath skills
) {}

public final class ItemSequenceExtractor {
    public ItemPath extract(JsonNode timeline, int participantId,
                            Set<Integer> finalItemIds, ItemCatalog catalog);
}

public final class BuildObservationFactory {
    public List<BuildObservation> from(BuildSourceMatch match);
}
```

`JdbcBuildSourceRepository` must select only EUW1/Europe matches of at least 10 minutes in the requested queue and patch window, order the selected `match_id` values once, and read all data needed for observations without a Riot client. `selectSource` freezes the exact eligible ID list and its maximum committed `core.matches.fetched_at`; every later batch is sliced from that list, so matches analyzed during the run cannot leak past the recorded watermark. Parse patches through `PatchVersion`. For a minor patch greater than `1`, comparison is exactly `minor - 1` even when that patch has zero stored matches; never skip to an older minor. At an annual `.1` boundary, use the highest stored minor from the preceding major or return insufficient source data when none exists. Pair opponents by the opposite team and the same normalized role. Omit participants whose champion/win state is absent, whose role cannot be normalized, or whose required item, rune, spell, or skill input is invalid. If there is not exactly one same-role opponent, retain the valid participant for its champion-role baseline with `opponentChampionId=null` and exclude it only from exact-matchup aggregation.

The item extractor reads ordered raw timeline JSON because the normalized item-event table lacks a stable source ordinal. Apply purchase, sell, destroy, and undo events in frame/event order. Starting items are the inventory state at the configurable two-minute cutoff; purchases after the cutoff are later purchases and are excluded. Core items are completed non-boot items present in the final inventory, ordered by first acquisition, capped at three. Boots are separate. Consumables, trinkets, components, arena-only items, and items whose `static.items.version` is not the latest synchronized version are excluded through `ItemCatalog`; this prevents stale removed items from becoming new recommendations. Rune stat shards come from `perks_json`; spells and skills come from normalized participant data.

- [ ] Add a timeline fixture containing purchase, sell, destroy, undo, duplicate-timestamp, boot, component, and final completed-item events.
- [ ] Write failing tests asserting the exact starting inventory, final boots, and ordered completed core IDs. The undo must remove the undone purchase, duplicate timestamps must retain JSON source order, a purchase at the cutoff is included, and a later purchase is excluded.
- [ ] Write failing observation tests for role normalization, exact same-role opponent pairing, baseline retention with zero or multiple same-role opponents, missing champion/win exclusion, rune shards, spell ordering, invalid component exclusion, and complete skill order.
- [ ] Write a failing Testcontainers repository test backed by `build-source-it.sql`. Assert EUW1 filtering, 10-minute duration filtering, exact queue isolation, exact adjacent-patch filtering, ascending frozen ID selection, raw timeline mapping, and watermark selection. Insert a new eligible match after `selectSource` and prove it is absent from all batches; do not create an in-memory production repository.
- [ ] Run `./mvnw.cmd "-Dtest=ItemSequenceExtractorTest,BuildObservationFactoryTest" test`; confirm it fails.
- [ ] Implement the smallest extractors and repository needed to pass. Parse each JSON document once per source match.
- [ ] Re-run `./mvnw.cmd "-Dtest=ItemSequenceExtractorTest,BuildObservationFactoryTest,JdbcBuildSourceRepositoryIT" test`; expect `BUILD SUCCESS`.
- [ ] Commit with `git add src/main/java/org/main/builds src/test/java/org/main/builds src/test/resources/builds && git commit -m "feat: extract champion build observations"`.

### Task 4: Aggregate complete frontend-ready snapshot payloads

**Files:**

- Create: `src/main/java/org/main/builds/model/BuildSnapshotPayload.java`
- Create: `src/main/java/org/main/builds/model/BaselineKey.java`
- Create: `src/main/java/org/main/builds/model/AggregationResult.java`
- Create: `src/main/java/org/main/builds/aggregate/BuildAggregator.java`
- Create: `src/main/java/org/main/builds/aggregate/BuildComponentRanker.java`
- Create: `src/test/java/org/main/builds/aggregate/BuildAggregatorTest.java`

**Interfaces:**

```java
public record BuildSnapshotPayload(
        List<BuildChoice> startingItems,
        List<BuildChoice> boots,
        List<BuildChoice> coreItems,
        List<BuildChoice> situationalItems,
        List<BuildChoice> runePages,
        List<BuildChoice> spellPairs,
        List<BuildChoice> skillOrders,
        List<Integer> skillMaxPriority
) {}

public record BuildChoice(
        List<Integer> ids, int games, int wins, double pickRate,
        double winRate, double weightedScore
) {}

public record AggregatedCohort(
        int championId, BuildRole role, Integer opponentChampionId,
        BuildScope scope, int games, int wins, int anchorGames,
        int comparisonGames, BuildConfidence confidence,
        BuildSnapshotPayload payload
) {}

public record AggregationResult(
        List<AggregatedCohort> cohorts,
        Set<BaselineKey> expectedBaselines,
        int sourceObservationCount
) {}

public final class BuildAggregator {
    public AggregationResult aggregate(PatchWindow window, BuildQueue queue,
                                       List<BuildObservation> observations);
}
```

The output is already ranked and display-ready. The frontend must never sort by pick rate or recompute win rate. Generate a champion-role baseline for every expected champion-role key with valid observations. Generate exact opponent cohorts only at 10 games. Emit a situational item only when that individual item candidate appears in at least 10 qualifying games; the parent cohort size is not enough. For each component, rank by the configured weighted pick score, use weighted win rate only as a tie-breaker, then stable ID order. Round API-facing rates once on the backend to two decimals. Skill max priority is derived from the modal first-maxed, second-maxed, third-maxed sequence.

- [ ] Write a failing test with observations from two patches that proves `0.70/0.30` weighting changes the winner relative to raw counts.
- [ ] Add failing tests for the `9` versus `10` game exact-opponent boundary, an individual situational candidate seen in 9 versus 10 games inside a larger cohort, and no Flex/Solo or role mixing.
- [ ] Add a failing tie test that proves stable ordering across repeated runs.
- [ ] Add a failing completeness test covering starting items, boots/core, situational items, runes plus shards, spells, full skill order, and max priority.
- [ ] Run `./mvnw.cmd "-Dtest=BuildAggregatorTest" test`; confirm it fails.
- [ ] Implement aggregation with maps and immutable records; do not add a statistics framework or parallel streams.
- [ ] Re-run the focused test; expect `BUILD SUCCESS`.
- [ ] Commit with `git add src/main/java/org/main/builds src/test/java/org/main/builds/aggregate && git commit -m "feat: aggregate champion build components"`.

### Task 5: Persist runs, publish atomically, and keep the previous snapshot on failure

**Files:**

- Create: `src/main/java/org/main/builds/store/AggregationRun.java`
- Create: `src/main/java/org/main/builds/store/BuildSnapshot.java`
- Create: `src/main/java/org/main/builds/store/BuildSnapshotRepository.java`
- Create: `src/main/java/org/main/builds/store/JdbcBuildSnapshotRepository.java`
- Create: `src/main/java/org/main/builds/store/BuildPublisher.java`
- Create: `src/main/java/org/main/builds/store/BuildSnapshotValidator.java`
- Create: `src/test/java/org/main/builds/store/BuildPublisherIT.java`
- Create: `src/test/java/org/main/builds/store/BuildSnapshotValidatorTest.java`

**Interfaces:**

```java
public interface BuildSnapshotRepository {
    UUID startRun(int version, PatchWindow window, BuildQueue queue,
                  OffsetDateTime watermark);
    void insertSnapshots(UUID runId, List<AggregatedCohort> cohorts,
                         int aggregationVersion, int payloadSchemaVersion,
                         PatchWindow window, BuildQueue queue,
                         OffsetDateTime watermark);
    void publishRun(UUID runId);
    void failRun(UUID runId, String failureCategory);
    Optional<BuildSnapshot> findPublished(BuildLookup lookup);
    List<BuildSnapshot> findHistoricalBaselines(BuildLookup lookup, int limit);
    Optional<AggregationRun> findLatestRun(String patch, BuildQueue queue);
}

public final class BuildPublisher {
    @Transactional
    public void publish(UUID runId, AggregationResult result,
                        int aggregationVersion, int payloadSchemaVersion,
                        PatchWindow window, BuildQueue queue,
                        OffsetDateTime watermark);
}
```

`BuildSnapshotValidator` rejects an empty result, missing expected baselines, duplicate cohort keys, exact cohorts below the matchup threshold, mismatched run/window/queue keys, negative counts, wins above games, `anchorGames + comparisonGames != games`, missing required payload components, and unsupported IDs. `publish` records the passed validation count, inserts every pending row, runs validation before changing publication state, marks older published rows for the same aggregation version/patch/queue as archived, marks the new rows published, then completes the run in one transaction. `failRun` records a bounded category string outside that publication transaction. Never delete the previous published rows during a failed run.

- [ ] Write a failing Testcontainers test for insert-and-publish, then assert the new run is `COMPLETED` and its rows are `PUBLISHED`.
- [ ] Add a failing second-publication test that asserts the prior rows become `ARCHIVED` only after the replacement commits.
- [ ] Add a failing rollback test by publishing an empty cohort list; assert the prior rows remain `PUBLISHED` and the failed run contains no published rows.
- [ ] Add failing validator tests for every rejected invariant, including a missing expected baseline and an incomplete payload; assert validation occurs before any archive update.
- [ ] Add a failing duplicate-running test for the database guard.
- [ ] Run `./mvnw.cmd "-Dtest=BuildSnapshotValidatorTest,BuildPublisherIT" test`; confirm it fails.
- [ ] Implement the JDBC repository using `ObjectMapper` for the single JSONB payload and explicit SQL row mappers. Keep transaction ownership in `BuildPublisher`.
- [ ] Re-run `./mvnw.cmd "-Dtest=BuildSnapshotValidatorTest,BuildPublisherIT" test`; expect `BUILD SUCCESS`.
- [ ] Commit with `git add src/main/java/org/main/builds/store src/test/java/org/main/builds/store && git commit -m "feat: publish champion build snapshots"`.

### Task 6: Orchestrate idempotent scheduled aggregation without request-time work

**Files:**

- Create: `src/main/java/org/main/builds/aggregate/ChampionBuildAggregationService.java`
- Create: `src/main/java/org/main/builds/aggregate/ChampionBuildScheduler.java`
- Create: `src/test/java/org/main/builds/aggregate/ChampionBuildAggregationServiceTest.java`
- Modify: `src/main/resources/application.properties`

**Interfaces:**

```java
public interface ChampionBuildAggregationService {
    AggregationOutcome refresh(BuildQueue queue);
}

public record AggregationOutcome(
        Status status, String patch, int sourceMatches, int snapshots, UUID runId
) {
    public enum Status { PUBLISHED, NO_CHANGE, INSUFFICIENT_SOURCE_DATA, FAILED }
}

public final class ChampionBuildScheduler {
    @Scheduled(fixedDelayString = "${app.builds.scheduler-delay:PT1H}")
    public void refreshSoloDuo();
}
```

The service discovers the latest anchor and resolves its exact adjacent comparison patch, freezes a `BuildSourceSelection`, and returns `NO_CHANGE` when a completed run with the same aggregation version, patch window, queue, and watermark already exists. It loads only the frozen match IDs in batches, builds observations, aggregates, validates, and publishes. An anchor with zero comparison observations still publishes with `comparisonGames=0`; an annual `.1` anchor with no resolvable prior-major patch returns `INSUFFICIENT_SOURCE_DATA`. The first scheduler calls only Solo/Duo; Flex remains an explicit API option whose availability is false until a Flex run is published. Catch the orchestration boundary, record a bounded failure category, and leave the published snapshot untouched. Scheduler defaults to disabled and is enabled by `APP_BUILDS_SCHEDULER_ENABLED=true`.

- [ ] Write failing unit tests with fakes/mocks for a successful publish, same-watermark no-op, absent comparison observations without patch skipping, unresolved annual boundary, empty observations, publisher failure, frozen-source batching, and Solo-only scheduler behavior.
- [ ] Assert that a failure calls `failRun`, does not call a delete/archive method, and returns `FAILED`.
- [ ] Run `./mvnw.cmd "-Dtest=ChampionBuildAggregationServiceTest" test`; confirm it fails.
- [ ] Implement the orchestration and guarded scheduler. Do not introduce a queue, executor, or distributed scheduler.
- [ ] Re-run the focused test; expect `BUILD SUCCESS`.
- [ ] Commit with `git add src/main/java/org/main/builds/aggregate src/test/java/org/main/builds/aggregate src/main/resources/application.properties && git commit -m "feat: schedule champion build aggregation"`.

### Task 7: Resolve fallbacks and expose public read-only APIs

**Files:**

- Create: `src/main/java/org/main/builds/api/ChampionBuildController.java`
- Create: `src/main/java/org/main/builds/api/ChampionBuildService.java`
- Create: `src/main/java/org/main/builds/api/ChampionBuildOptionsResponse.java`
- Create: `src/main/java/org/main/builds/api/ChampionBuildResponse.java`
- Create: `src/main/java/org/main/builds/api/BuildFallbackReason.java`
- Create: `src/main/java/org/main/builds/api/BuildAssetRepository.java`
- Create: `src/main/java/org/main/builds/api/JdbcBuildAssetRepository.java`
- Create: `src/test/java/org/main/builds/api/ChampionBuildServiceTest.java`
- Create: `src/test/java/org/main/builds/api/ChampionBuildControllerTest.java`

**Interfaces:**

```java
@RestController
@RequestMapping("/api/champions/{championId}/builds")
public final class ChampionBuildController {
    @GetMapping("/options")
    public ChampionBuildOptionsResponse options(
            @PathVariable int championId,
            @RequestParam(required = false) Integer queueId,
            @RequestParam(required = false) String patch,
            @RequestParam(required = false) BuildRole role);

    @GetMapping
    public ChampionBuildResponse builds(
            @PathVariable int championId,
            @RequestParam int queueId,
            @RequestParam String patch,
            @RequestParam BuildRole role,
            @RequestParam(required = false) Integer opponentId);
}

public record ChampionBuildOptionsResponse(
        int championId,
        List<QueueOption> queues,
        List<PatchOption> patches,
        List<RoleOption> roles,
        List<OpponentOption> opponents,
        RequestedFilters defaults
) {}

public record RequestedFilters(
        int queueId, String patch, BuildRole role, Integer opponentId
) {}

public record ResolvedFilters(
        int queueId, String anchorPatch, String comparisonPatch,
        BuildRole role, Integer opponentId
) {}

public record ChampionBuildResponse(
        boolean available,
        RequestedFilters requested,
        ResolvedFilters resolved,
        BuildScope resultScope,
        BuildConfidence confidence,
        int games,
        int wins,
        double winRate,
        boolean stale,
        boolean historical,
        BuildFallbackReason fallbackReason,
        String evidenceLabel,
        String explanation,
        OffsetDateTime calculatedAt,
        OffsetDateTime publishedAt,
        DisplayBuildPayload build
) {}

public record DisplayBuildPayload(
        List<DisplayBuildChoice> startingItems,
        List<DisplayBuildChoice> boots,
        List<DisplayBuildChoice> coreItems,
        List<DisplayBuildChoice> situationalItems,
        List<DisplayBuildChoice> runePages,
        List<DisplayBuildChoice> spellPairs,
        List<DisplayBuildChoice> skillOrders,
        List<Integer> skillMaxPriority
) {}
```

The resolver performs this exact server-side sequence while preserving queue and role: exact opponent if eligible; current champion-role baseline; up to two earlier champion-role patch baselines; last published current-key snapshot when the latest aggregation run failed; structured unavailable response. When a newer run failed after a returned snapshot was published, the service marks that response `stale=true` with reason `AGGREGATION_FAILED_USING_LAST_PUBLISHED` without changing its scope. `BuildAssetRepository` enriches item, rune, spell, and champion IDs with labels and image URLs before returning the response. Options applies its optional queue/patch/role filters and returns patches, roles with baseline sample sizes, opponent choices with exact sample sizes, queue labels, per-queue availability, and defaults. Solo defaults to available after its first run; Flex is returned with `available=false` until its own published snapshot exists.

Use `200` for available and structured unavailable results, `400` for unsupported queues/roles/malformed patches, and `404` for an unknown champion. Both routes remain public under the existing GET security rule. Do not modify `ChampionController.getChampionItems` or its service query.

- [ ] Write failing service tests for every fallback rung with the exact reasons `MATCHUP_SAMPLE_TOO_SMALL`, `REQUESTED_PATCH_UNAVAILABLE`, and `AGGREGATION_FAILED_USING_LAST_PUBLISHED`, including the two-patch historical cap and a failed latest run that returns the prior published snapshot with `stale=true`.
- [ ] Add negative tests proving no lookup changes queue or role, and exact opponent below 10 returns the same-role baseline.
- [ ] Write failing MVC tests for public unauthenticated access, filtered option defaults and sample sizes, exact response metadata, Flex unavailable state, unknown champion, invalid query values rejected before repository access, and absence of raw Riot payloads.
- [ ] Run `./mvnw.cmd "-Dtest=ChampionBuildServiceTest,ChampionBuildControllerTest" test`; confirm it fails.
- [ ] Implement the resolver, asset enrichment, response records, and controller. Keep response assembly in the service, not the controller.
- [ ] Re-run the focused tests; expect `BUILD SUCCESS`.
- [ ] Commit with `git add src/main/java/org/main/builds/api src/test/java/org/main/builds/api && git commit -m "feat: expose champion build APIs"`.

### Task 8: Add the champion-page build shell and browser state contract

**Files:**

- Modify: `src/main/resources/static/champion.html`
- Modify: `src/main/resources/static/js/api.js`
- Create: `src/main/resources/static/js/champion-builds.js`
- Modify: `src/main/resources/static/js/champion.js`
- Create: `src/test/java/org/main/frontend/StaticChampionBuildFrontendTest.java`
- Create: `src/test/js/champion-builds.test.cjs`

**Interfaces:**

```javascript
api.getChampionBuildOptions(championId, { queueId, patch, role })
api.getChampionBuild(championId, { queueId, patch, role, opponentId })

const championBuilds = {
    async mount({ championId, root, apiClient, sessionStorage, history, location }),
    render(response),
    renderUnavailable(response),
    renderRequestError(error),
    syncUrl(filters),
    cacheKey(championId, filters)
};
```

Insert a `champion-builds` section before legacy item statistics. It contains queue tabs, patch select, role tabs, a labeled opponent search input, a horizontally scrollable opponent rail, evidence/confidence metadata, and backend-provided component groups for starting items, boots/core, situational items, runes plus shards, spells, skill order, and max priority. Opponent search filters only the backend-returned labels, exposes an empty-result message, supports Arrow keys plus Enter, and refetches options whenever queue, patch, or role changes. Add status regions for loading, stale/fallback, insufficient data, and request error. Preserve the currently rendered response during a failed request. If there is no current response, read the exact filter key from `sessionStorage`; never use a cache entry from another queue, patch, role, opponent, or champion.

`champion.js` continues to load champion identity and legacy items, then mounts `championBuilds`. Query parameters are `queue`, `patch`, `role`, and `opponent`. Use `history.replaceState` for filter changes. No numeric weighting, confidence thresholds, ranking, or build selection may appear in JavaScript.

- [ ] Write a failing static contract test asserting the build section IDs, labeled opponent search and empty state, both API methods, separate script include, URL parameter names, exact cache-key dimensions, `aria-live` status region, and unchanged legacy item hooks.
- [ ] Add assertions that `champion-builds.js` does not contain `0.70`, `0.30`, confidence thresholds, `.sort(`, or win-rate division.
- [ ] Write failing `node:test` cases with injected API, history, location, storage, and renderer fakes for URL restoration, filter changes, option refetch, exact-key cache restoration, stale marking, retry, preservation of the current response on error, and Arrow/Enter opponent selection.
- [ ] Run `./mvnw.cmd "-Dtest=StaticChampionBuildFrontendTest" test`; confirm it fails.
- [ ] Run `node --test src/test/js/champion-builds.test.cjs`; confirm the behavior tests fail before implementation.
- [ ] Add semantic HTML, API methods, and the standalone browser module. Keep all server-provided order intact.
- [ ] Re-run the focused test; expect `BUILD SUCCESS`.
- [ ] Re-run `node --test src/test/js/champion-builds.test.cjs`; expect all behavior tests to pass without third-party packages.
- [ ] Run `node --check src/main/resources/static/js/api.js`, `node --check src/main/resources/static/js/champion.js`, and `node --check src/main/resources/static/js/champion-builds.js`; expect no output and exit code `0`.
- [ ] Commit with `git add src/main/resources/static/champion.html src/main/resources/static/js/api.js src/main/resources/static/js/champion.js src/main/resources/static/js/champion-builds.js src/test/java/org/main/frontend/StaticChampionBuildFrontendTest.java src/test/js/champion-builds.test.cjs && git commit -m "feat: add champion build page behavior"`.

### Task 9: Style all build states and verify accessibility at target widths

**Files:**

- Modify: `src/main/resources/static/css/champion.css`
- Modify: `src/test/java/org/main/frontend/StaticChampionBuildFrontendTest.java`
- Modify: `docs/superpowers/specs/2026-07-13-champion-builds-design.md`

**Interfaces:**

Add CSS hooks for:

```text
.champion-builds
.build-filter-bar
.build-queue-tabs
.build-role-tabs
.build-opponent-rail
.build-evidence
.build-component-grid
.build-item-sequence
.build-rune-page
.build-skill-order
.build-state
.build-state--stale
.build-state--unavailable
```

Use existing purple/cream tokens and existing button/focus conventions. At `375px`, controls wrap, the opponent list scrolls horizontally, and build sequences remain ordered left-to-right. At `768px`, component cards use two columns where space allows. At `1024px` and `1440px`, primary components lead a balanced grid and evidence remains visually subordinate. Respect `prefers-reduced-motion`; every tab/select/opponent control is keyboard reachable with a visible focus state. Do not hide evidence or fallback text on narrow screens.

- [ ] Extend the failing static test to require the named hooks, mobile opponent overflow, visible `:focus-visible`, and a reduced-motion rule.
- [ ] Run `./mvnw.cmd "-Dtest=StaticChampionBuildFrontendTest" test`; confirm the new CSS assertions fail.
- [ ] Implement the responsive styles using the existing design tokens; add no new font, icon library, or animation dependency.
- [ ] Re-run the focused test and all three `node --check` commands; expect success.
- [ ] Start the app with Java 23 and scheduler disabled: `./mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--app.builds.scheduler-enabled=false"`.
- [ ] Inspect `champion.html?id=1` at `375`, `768`, `1024`, and `1440` pixels. Verify keyboard focus order, overflow, loading, unavailable Flex, fallback, stale-cache, and request-error states. Save no generated screenshots to the repository.
- [ ] Update the design spec status to implemented only after these checks; record any deliberate deferral as a concrete bullet under its implementation notes.
- [ ] Commit with `git add src/main/resources/static/css/champion.css src/test/java/org/main/frontend/StaticChampionBuildFrontendTest.java docs/superpowers/specs/2026-07-13-champion-builds-design.md && git commit -m "feat: polish champion build experience"`.

### Task 10: Run the full backend, integration, and browser contract verification

**Files:**

- Modify only files required by failures directly caused by Tasks 1–9.

- [ ] Run `./mvnw.cmd test`; expect `BUILD SUCCESS` for the default unit/static suite.
- [ ] Run `./mvnw.cmd "-Dtest=*IT" test`; expect `BUILD SUCCESS` for all Testcontainers integration tests.
- [ ] Run `./mvnw.cmd checkstyle:check`; expect `BUILD SUCCESS` with zero checkstyle violations.
- [ ] Run `node --check src/main/resources/static/js/api.js`, `node --check src/main/resources/static/js/champion.js`, and `node --check src/main/resources/static/js/champion-builds.js`; expect exit code `0`.
- [ ] Run `node --test src/test/js/champion-builds.test.cjs`; expect all browser-state behavior tests to pass.
- [ ] Query the local database after one enabled aggregation run and verify: one completed Solo run, published baseline rows, exact rows only where `games >= 10`, no published duplicate cohort keys, and no Flex rows created by the Solo scheduler.
- [ ] Stop the server, start it once more with the scheduler disabled, and make direct unauthenticated GET requests to `/api/champions/1/builds/options` and `/api/champions/1/builds?queueId=420&patch=16.9&role=MIDDLE`; verify stable JSON and zero Riot request log entries.
- [ ] Review `git diff main...HEAD` for accidental changes to the legacy item endpoint, unrelated user files, credentials, generated logs, or database dumps.
- [ ] If verification required code changes, add one narrowly scoped regression test per fix and commit with `git commit -m "fix: verify champion build delivery"`.
- [ ] Request a final broad code review covering spec compliance, backend calculation ownership, fallback isolation, publication safety, accessibility, and over-engineering.

## Completion criteria

- A public champion page can select queue, patch, role, and optional opponent and render all specified build components.
- Solo/Duo publishes prepared snapshots; Flex is visible and honestly unavailable until its own data qualifies.
- Exact matchup data is used only at 10 or more games and confidence boundaries match the approved thresholds.
- No fallback crosses queue or role, and historical fallback stops after two earlier patches.
- Failed aggregation leaves the last published snapshot available and marks it stale; failed browser requests preserve the last successful exact-filter response.
- Page requests perform no Riot calls and no match-level aggregation.
- The existing stored item statistics endpoint and UI remain separate and functional.
- Default tests, integration tests, checkstyle, JavaScript syntax checks, and responsive/manual accessibility checks all pass.
