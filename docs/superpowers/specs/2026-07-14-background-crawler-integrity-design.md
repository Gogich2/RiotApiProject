# Background Crawler and Data Integrity Design

**Status:** Approved for implementation planning  
**Date:** 2026-07-14

## Summary

Run timeline integrity repair and player crawling continuously as one Spring-managed background maintenance cycle. The cycle starts 60 seconds after application startup, waits two minutes after each completion, gives both workloads a protected half of Riot's two-minute request allowance, and lets either workload borrow capacity the other did not use.

The crawler rotates through all players stored in `raw.players`, starting with players that have never been attempted. All quota and rotation decisions remain on the backend. Existing player, match, and timeline data is preserved.

## Goals

- Start background crawler and integrity maintenance with the application.
- Run at most one maintenance cycle at a time in one application instance.
- Give integrity and crawling approximately half of the configured two-minute Riot limit when both have work.
- Reuse unused capacity without exceeding the existing global Riot limiter.
- Rotate fairly through old and new stored players.
- Continue useful work when either phase fails.
- Keep calculations, scheduling, and fallback behavior on the backend.

## Non-goals

- Distributed locking for multiple application instances.
- Replacing Spring scheduling with Quartz or another job framework.
- Deleting, archiving, or rewriting existing match data.
- Creating separate hard rate-limit lanes inside every Riot API call.
- Changing manual crawler endpoints.

## Selected Approach

Use one maintenance coordinator built with the existing Spring `@Scheduled` infrastructure. This is smaller than two independent quota-aware schedulers and makes the order, borrowing, and failure isolation explicit.

The coordinator owns orchestration only. Existing services continue to own integrity repair, crawling, persistence, Riot API calls, retries, and the global sliding-window limit.

## Components

### Background maintenance coordinator

A single conditional scheduler will:

1. Read current two-minute capacity from `RiotRateLimiter`.
2. Run the protected integrity phase.
3. Run the crawler phase with its protected share plus unused integrity capacity.
4. Offer actual unused crawler capacity back to integrity for one bounded extra pass.
5. Log one cycle summary.

It uses a fixed delay of two minutes and an initial delay of 60 seconds. Fixed delay is measured after completion, so a slow cycle cannot overlap the next cycle in the same process.

The existing standalone `DataIntegrityScheduler` will be replaced by this coordinator to prevent duplicate scheduled integrity work. Manual integrity endpoints remain available.

### Crawler service

Add a crawler operation that selects the least recently attempted stored player and delegates to the existing PUUID crawler. It updates the selected player's attempt timestamp in a `finally` path, including when the Riot request fails or finds no new matches. A bad or fully synchronized player therefore cannot starve the rotation.

The existing `crawlLatestPlayerEUW` behavior and public endpoint remain unchanged.

### Riot rate limiter

Keep the current global sliding-window limiter as the authority for all Riot calls. Add a synchronized, read-only query for currently available two-minute capacity. The query prunes expired timestamps using the same logic as `acquire()` and reports capacity without reserving it.

Because foreground traffic may consume capacity after the coordinator reads it, the value is advisory. `acquire()` still prevents an actual limit violation and may delay background work into the next rolling window.

## Player Rotation and Existing Data

Add nullable `last_crawl_attempt_at` to `raw.players` through the next Flyway migration and add an index that supports candidate selection. Do not repurpose `updated_at`: that field describes player record changes and is also changed by profile enrichment.

Candidate order is:

1. `last_crawl_attempt_at IS NULL` first;
2. oldest `last_crawl_attempt_at` first;
3. oldest `created_at` first;
4. PUUID as a deterministic tie-breaker.

Existing rows receive `NULL`, meaning "never attempted." They are crawled before rows that already have an attempt timestamp. No old player, match, timeline, frame, or event data is deleted.

The timestamp records an attempt rather than success. Transient failures return to the candidate pool after the rest of the stored players have had a turn.

## Soft 50/50 Request Budget

The configured regular two-minute limit is currently 85. One request is kept as best-effort headroom, leaving at most 84 requests for a full background cycle. If other traffic has already used part of the rolling window, the coordinator works from the smaller currently available amount.

Given configured limit `L`, current remaining capacity `R`, and configured headroom `H`, the cycle budget is:

```text
B = max(0, min(L - H, R - H))
```

For a cycle budget `B`:

- integrity protected budget: `floor(B / 2)`;
- crawler protected budget: `B - integrity protected budget`.

Only missing raw timelines require Riot calls during integrity repair. Rebuilding frames or events from an already stored raw timeline is local work and does not reduce the Riot request budget.

The initial integrity pass is bounded by both its protected budget and the number of matches missing raw timelines. The coordinator then reads remaining capacity again. Unused integrity capacity is added to the crawler budget.

Crawler sizing uses a worst-case backend calculation. Crawling `N` match IDs costs at most:

```text
ceil(N / 20) match-ID page calls + N match-detail calls + N timeline calls
```

The coordinator chooses the largest `N` whose worst-case cost fits the crawler's available request budget. With a full window and 42 integrity calls, this produces the normal 20-match crawl. With no integrity API work, it can safely grow to 40 matches under the 84-request background ceiling.

After crawling, the coordinator reads actual remaining capacity from the limiter. If the crawler used less than its allowance and missing raw timelines remain, integrity receives one additional repair pass bounded by the remaining capacity minus headroom. This makes borrowing work in both directions without adding named limiter lanes.

The one-request headroom is not a hard reservation. Concurrent foreground calls can consume it; the global limiter remains the final safety mechanism.

## Cycle Data Flow

1. The scheduler wakes after its configured fixed delay.
2. It obtains an integrity report and current limiter capacity.
3. It repairs the protected number of missing raw timelines and performs bounded local frame/event repair through the existing integrity service.
4. It selects the least recently attempted player.
5. It calculates the safe crawler match limit on the backend.
6. It invokes the existing PUUID crawler.
7. It records `last_crawl_attempt_at` even if the crawl fails or saves zero matches.
8. It gives any actual remaining crawler capacity to one extra integrity pass when useful.
9. It emits a summary containing selected player, repaired counts, saved matches, borrowed capacity, failures, and cycle duration.

When there are no players, the crawler phase is skipped normally. When integrity reports no gaps, its capacity is available to the crawler.

## Error Handling and Fallbacks

- Integrity and crawler phases have separate exception boundaries. One failure does not suppress the other phase.
- Riot `429` and transient errors continue through the existing retry and `Retry-After` behavior.
- After retries are exhausted, the failure is logged and the next scheduled cycle continues normally.
- A failed crawl still advances the attempt timestamp so one player cannot monopolize every cycle.
- An empty player table and a fully valid integrity report are normal no-work outcomes, not scheduler failures.
- The global limiter handles capacity races and prevents over-limit calls even when the coordinator's advisory capacity becomes stale.
- No distributed lock is added. The supported design assumes one running backend instance; multi-instance deployment requires a later locking or leader-election design.

## Configuration

Expose application properties with these exact names and defaults:

- `app.scheduler.background-maintenance.enabled=true`;
- `app.scheduler.background-maintenance.initial-delay-ms=60000`;
- `app.scheduler.background-maintenance.fixed-delay-ms=120000`;
- `app.scheduler.background-maintenance.integrity-share-percent=50`;
- `app.scheduler.background-maintenance.headroom-requests=1`.

The scheduler can be disabled for tests, local troubleshooting, or deployments that run maintenance elsewhere. Existing Riot limiter properties remain the source of the per-second and two-minute limits.

## Testing

Small focused tests will cover:

- old rows with `NULL` attempt timestamps sort before attempted rows;
- deterministic least-recently-attempted selection;
- attempt timestamps advance after success, zero saved matches, and failure;
- quota calculation at full capacity, partial capacity, and zero capacity;
- worst-case crawler sizing across the 20-item page boundary;
- the normal 42-integrity/20-match split under an 85-request limit;
- integrity-to-crawler and crawler-to-integrity borrowing;
- one-request best-effort headroom;
- integrity failure does not prevent crawling;
- crawler failure does not prevent the fallback integrity pass;
- empty player and no-gap outcomes;
- scheduler conditional disablement;
- fixed-delay and initial-delay property bindings.

Existing crawler, integrity, rate-limiter, repository, and application-context tests must continue to pass.

## Acceptance Criteria

- Starting the application schedules one combined maintenance job after 60 seconds.
- Completed cycles are followed by a two-minute delay and never overlap in one process.
- Both workloads receive a protected half of available background capacity when both need it.
- Unused capacity can be borrowed in either direction.
- Actual Riot requests still pass through the existing global limiter.
- Stored players rotate by oldest attempt, with all pre-migration players processed first.
- Failures are isolated and retried through later cycles without blocking the rotation.
- Existing data and manual endpoints remain intact.
