# Repository Slimming and Performance Design

## Goal

Reduce repository size and avoidable runtime work while preserving every existing endpoint, admin capability, and user-visible behavior.

## Scope

The implementation will:

- stop tracking reproducible Maven caches, Graphify output, and runtime logs;
- keep all crawler, dataset, integrity, analysis, static-data, account, build, and frontend APIs;
- batch database writes performed during match analysis and timeline ingestion;
- select integrity-repair candidates in PostgreSQL instead of loading every player;
- reuse player-dashboard data in the browser instead of immediately requesting the same data again;
- add non-destructive Flyway migrations for missing performance indexes;
- remove only trivial delegation or duplication where behavior and public API compatibility remain unchanged.

The implementation will not rewrite Git history, remove potentially useful features, replace Spring Boot, or introduce new runtime dependencies.

## Repository Hygiene

`target-cache/`, `graphify-out/`, `logs/`, and generated build output are local artifacts. They will be added to `.gitignore` and removed from the current Git index. Maven remains the source of truth for dependencies, and Graphify output remains locally regenerable. Historical removal of large blobs is deliberately excluded because it rewrites shared Git history and requires a separate migration decision.

## Match Analysis Persistence

`MatchAnalysisServiceImpl` will preserve its transaction-per-match boundary and existing analysis states. Per-participant and per-component writes will be collected and sent through `JdbcTemplate.batchUpdate`. The existing SQL conflict behavior remains unchanged. Deletes may be consolidated only when the resulting transaction has the same all-or-nothing semantics.

Batch conversion will be incremental: players, participants, final items, runes, item events, and skill order each receive focused tests that verify parameter grouping and empty-input behavior before implementation changes.

## Timeline Persistence

Timeline parsing will construct all frame and event entities first. Repositories will receive one `saveAll` call per entity type instead of one frame save and one event batch per frame. Existing delete-before-rebuild behavior and transaction boundaries remain unchanged.

Malformed or absent frame arrays continue to produce the current warning/skip behavior. Empty event lists do not perform unnecessary writes.

## Integrity Repair Selection

`PlayerRepository` will expose bounded queries for:

- players with a usable PUUID and no profile icon;
- players with a usable PUUID and no current rank data.

The database applies the requested limit. Repair services no longer call `findAll()` for these operations. Existing maximum limit normalization, error isolation per player, and result counters remain compatible.

## Dashboard Data Flow

`PlayerDashboardDto` will include the recent matches already loaded to calculate recent form. The browser will render those matches directly during initial dashboard load. Rank history remains a separate request because it is not part of the current dashboard calculation. Champion-tab behavior remains available; the initial champion pool is reused where it satisfies the tab payload, otherwise the full endpoint is requested lazily.

Queue switches invalidate queue-specific browser caches and render data returned by the new dashboard response. Existing endpoints remain supported for direct clients.

## Database Indexes and Migrations

A new Flyway migration will add indexes with `IF NOT EXISTS` for the predicates and joins demonstrably used by the application, including match analysis status ordering, participant lookup by player and champion, timeline lookup by match, and player-profile repair selection.

The migration will not create or replace the existing `raw`, `core`, `static`, or `analyzed` tables because their authoritative production definitions are not present in the repository. Full schema ownership is a separate migration project; this change only adds safe indexes to tables that already exist. Test database fixtures will be updated only where required to exercise repository queries.

## Complexity Reductions

Single-implementation interfaces remain because removing them across the entire application would create a large compatibility diff for limited benefit. Swagger, Actuator, Cucumber, admin controllers, and development tooling remain. The implementation may remove forwarding methods only when callers can inject the existing target service directly without changing an HTTP contract.

No large service will be split merely to reduce file length. Changes must reduce actual work, duplication, or code—not redistribute it.

## Error Handling and Compatibility

All current HTTP paths and response behavior remain available. Database batches execute inside existing transactions, so any batch failure rolls back the match or timeline exactly as an individual-write failure does today. Repair loops continue processing after a single Riot API failure.

No new background concurrency is introduced; Riot rate limiting remains authoritative.

## Testing

Every behavior change follows RED → GREEN:

- repository tests prove bounded candidate selection;
- service tests prove repair no longer loads all players;
- timeline tests prove frames and events are saved in two batches;
- match-analysis tests prove grouped batch parameters;
- controller/DTO and JavaScript-facing tests prove dashboard compatibility;
- migration tests or Testcontainers integration tests verify indexes can be applied repeatedly where practical.

After each component, its focused Maven tests run. Completion requires the full Maven test suite on Java 23 with zero failures and a clean source diff review.

## Success Criteria

- all 217 existing tests continue to pass, plus new regression tests;
- all existing API endpoints remain present;
- `target-cache/`, `graphify-out/`, and runtime logs are no longer tracked in the current tree;
- timeline persistence uses at most one frame batch and one event batch per rebuild;
- integrity repair candidate selection is limited in SQL;
- initial player dashboard no longer immediately refetches recent matches already loaded by the server;
- match analysis uses JDBC batches for repeated row writes;
- no new runtime dependency is added.
