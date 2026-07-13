# Champion Builds Design

**Date:** 2026-07-13

**Status:** Implemented (2026-07-13)

**Parent design:** `2026-07-13-player-loop-matchup-builds-design.md`

## Implementation Notes

- The champion build interface keeps the existing purple/cream dark tactical
  tokens and uses the native HTML, CSS, and JavaScript stack without new
  dependencies, fonts, or icons.
- Deterministic Chrome inspection covered mocked loading, unavailable Flex,
  champion-role fallback, stale-cache, and retained request-error states at
  375, 768, 1024, and 1440 CSS pixels. Every inspected width had zero document
  overflow, 44-pixel minimum controls, a solid 2-pixel keyboard focus outline,
  visible evidence, preserved backend ordering, and readable state copy.
- The opponent rail remains horizontally scrollable, including at 375 pixels.
  Component layout progresses from one column at 375 to two at 768 and a
  balanced 12-column composition at 1024 and 1440. Max-priority skills remain
  a compact left-to-right sequence at every target width.
- Interaction transitions name exact properties and remain below 300ms.
  Reduced-motion inspection reported a 0.001-second transition duration.
- The full compiled JUnit suite is green: 201 tests pass, the 32 focused
  final-review regressions pass, and Checkstyle reports zero violations. Live
  application and database-backed browser verification remains deferred
  because background process approval is unavailable at the current usage
  limit, the local database has not yet applied the build migration, and the
  Docker engine required by Testcontainers is stopped. Deterministic inspection
  used the real page and CSS with mocked public API responses in installed
  Chrome.
- Final review corrections bound history to adjacent same-major patches, use a
  cross-queue patch only as a time anchor for an unavailable queue, round
  component percentages once at the API boundary, and persist the frozen
  source match-ID count. Independent re-review approved the corrections with
  no remaining findings.

## Goal

Add trustworthy, public champion builds to the existing champion page. Builds
are derived from stored EUW match data, calculated on the backend, and served
as prepared snapshots. The first release prioritizes useful Ranked Solo/Duo
champion-role builds while exposing exact matchup results only when their
sample supports them.

The experience must never imply that sparse observations are authoritative.
Every response identifies its real scope, sample, patch window, queue, and
confidence.

## Binding Product Decisions

- Public build data is available without an account.
- Initial region and route are EUW1 and Europe.
- Ranked Solo/Duo (`420`) and Ranked Flex (`440`) remain separate.
- Ranked Solo/Duo is the first publishable queue.
- Flex remains visible but returns an explicit insufficient-data state until
  its own sample is adequate.
- Queue data is never merged to manufacture confidence.
- The selected patch is the anchor of a two-patch window.
- Anchor-patch observations receive 70% weight and the immediately previous
  patch receives 30% weight.
- Champion-role builds are the baseline product.
- Exact opponent builds publish only when at least 10 qualifying games exist.
- Raw-data archival follows the visible builds release and does not block it.
- Existing unscoped champion item statistics remain a separate legacy surface
  and never power build recommendations.
- Results are described as observed builds, not objectively best builds.

## Evidence Behind the Rollout

The local EUW corpus contains:

- 13,690 matches
- 137,162 participants
- 868,721 final-item rows
- 818,460 rune-selection rows
- 2,037,980 skill-order rows
- 172 current champion records
- 705 current item records

For patch 16.9, Ranked Solo/Duo champion-role cohorts have a median of 10
games, and 303 of 589 cohorts have at least 10 games. Exact
champion-role-opponent cohorts have a median of 2 games, and only 610 of
11,330 cohorts have at least 10 games. No current Flex matchup cohort reaches
10 games.

This evidence makes a champion-role baseline useful now while requiring exact
matchups and Flex to remain threshold-gated.

## Delivery Scope

### First Release

- Prepared Ranked Solo/Duo champion-role snapshots
- Threshold-gated exact matchup snapshots
- Starting items
- Boots and core item path
- Situational items only when independently supported
- Full rune page, including stat shards
- Summoner-spell pair
- Skill order and skill-max priority
- Games, wins, observed win rate, patch range, confidence, and calculation time
- Role, queue, patch, and opponent controls
- Shareable URL state
- Desktop opponent workspace and prioritized mobile layout
- Loading, stale, fallback, insufficient-data, cached-error, and unavailable
  states

### Follow-up Work

- Increased Flex corpus collection and later Flex publication
- More exact-matchup coverage
- Additional alternative and situational branches
- Verified archival of raw payloads older than 90 days
- Rank-tier segmentation after rank-at-match provenance exists
- Additional regions after the EUW pipeline is stable

## Architecture

Build requests never aggregate large datasets and never call Riot APIs. A
backend batch aggregator reads validated stored data, calculates complete
results, and transactionally publishes versioned snapshots. Public endpoints
perform indexed reads against those snapshots.

The architecture has four bounded responsibilities:

1. `BuildAggregationService` selects qualifying cohorts and calculates build
   payloads.
2. `BuildSnapshotRepository` stores and retrieves published snapshots.
3. `ChampionBuildService` resolves exact, baseline, historical, and retained
   fallbacks.
4. `ChampionBuildController` validates public filters and returns frontend
   DTOs.

The existing champion item-statistics query remains unchanged and separate.

## Backend Ownership

The backend owns all product and statistical calculations:

- Canonical role selection
- Opposing-laner pairing
- Patch parsing and adjacent-patch selection
- 70/30 weighting
- Cohort eligibility
- Item ownership and purchase-sequence reconstruction
- Rune, spell, and skill combinations
- Pick frequency and observed win rate
- Confidence classification
- Exact-matchup threshold decisions
- Fallback selection
- Build-component ordering
- Availability of roles, queues, patches, and opponents
- Evidence text and safe explanation fields

The frontend only manages URL state, requests prepared responses, renders
backend-provided modules, preserves an already received response during an
error, and handles accessible interaction states. JavaScript must not calculate
weights, confidence, fallback scope, win rate, or build rankings.

## Source Eligibility

A match contributes only when all of these conditions hold:

- Region is EUW and platform is EUW1.
- Queue is exactly `420` for the initial published dataset.
- Patch can be normalized to `major.minor`.
- Duration is at least 10 minutes.
- Participant champion and win state are present.
- Participant has one canonical role from `TOP`, `JUNGLE`, `MIDDLE`,
  `BOTTOM`, or `UTILITY`.
- Exactly one opponent on the other team has the same canonical role.
- Required item, rune, spell, and skill inputs pass component validation.

Rows missing scope metadata are excluded. The aggregator never guesses a
queue, patch, role, or opponent. Excluded legacy rows remain stored and are
not mutated or deleted.

## Canonical Role and Opponent

Canonical role uses `team_position` first and `individual_position` second.
The value must be one of the five supported positions. `lane` and the legacy
`role` field are diagnostic inputs only and never silently override a valid
position.

An opponent is the unique participant in the same match who:

- Is on the other team
- Has the same canonical role

If zero or more than one opponent satisfies the rule, the participant is
excluded from exact-matchup aggregation but may still contribute to a
champion-role baseline when its own canonical role is valid.

## Patch Window and Weighting

The URL patch value is the anchor patch. For anchor `16.9`, the comparison
patch is `16.8`. Backend patch ordering uses parsed numeric major and minor
values, not lexicographic strings.

Each qualifying anchor-patch observation contributes weight `0.70`. Each
qualifying comparison-patch observation contributes weight `0.30`.

For each candidate component or sequence, the backend stores:

- Raw games and wins
- Anchor-patch games and wins
- Comparison-patch games and wins
- Weighted observations
- Weighted pick frequency
- Weighted observed win rate

Candidates rank primarily by weighted pick frequency. Weighted observed win
rate is a tie-breaker after the minimum sample requirement is satisfied. This
prevents a rare lucky sequence from outranking a commonly observed build.

## Confidence Model

Thresholds are configurable but launch with these values:

- Fewer than 10 games: `INSUFFICIENT`
- 10 through 24 games: `LOW`
- 25 through 49 games: `MEDIUM`
- 50 or more games: `HIGH`

Configuration properties:

- `app.builds.matchup-min-games=10`
- `app.builds.medium-confidence-games=25`
- `app.builds.high-confidence-games=50`
- `app.builds.anchor-patch-weight=0.70`
- `app.builds.comparison-patch-weight=0.30`
- `app.builds.historical-lookback-patches=2`

The DTO includes both the confidence label and raw sample size. Confidence is
never communicated by color alone.

## Build Taxonomy

### Starting Items

Reconstruct the inventory owned after ordered early purchases and undo/sell
processing. The implementation reads ordered raw timeline events or adds a
stable source ordinal before relying on normalized event rows. Trinkets and
later purchases are excluded.

### Boots

Select completed boots independently from the core path. Boot components are
not presented as completed boots.

### Core Path

Select the most commonly observed ordered sequence of up to three completed
items. Trinkets, consumables, duplicate event artifacts, and components that
upgrade into the selected completed item are excluded.

### Situational Items

Publish situational items only when their own qualifying sample is at least
10 games. They are labeled as alternatives and never appended merely because
they appear in a single high-win observation.

### Runes

Return the primary style, keystone, primary selections, secondary style,
secondary selections, and three stat shards. Stat shards are normalized from
`perks.statPerks` before snapshot publication.

### Summoner Spells

Return an unordered spell pair normalized by ID for aggregation and ordered
for display by a stable backend rule.

### Skills

Return the observed level-by-level order and the derived skill-max priority.
The backend ignores invalid skill slots and incomplete orders that cannot
support a stable sequence.

## Snapshot Model

Use one prepared snapshot row per cohort with a JSONB build payload. The
unique key is:

`aggregation_version + anchor_patch + comparison_patch + queue_id + champion_id + role + opponent_champion_id`

`opponent_champion_id` is null for the champion-role baseline.

The row also stores:

- Publication state
- Scope
- Raw games and wins
- Anchor and comparison sample counts
- Confidence
- Input watermark
- Source match count
- Calculated time
- Published time
- Payload schema version

An aggregation-run record stores version, patch window, queue, input
watermark, start and completion times, state, source counts, validation
counts, and a safe failure category.

Snapshots are calculated beside the currently published version. Publication
switches transactionally only after validation succeeds. A failed run never
deletes or replaces the previous published version.

## Fallback Ladder

Fallbacks are explicit and never cross queues or roles.

1. **Exact matchup:** Return `EXACT_MATCHUP` when the requested opponent
   cohort has at least 10 qualifying games.
2. **Champion-role baseline:** If an opponent is requested but the exact
   sample is insufficient, retain the opponent selection and return
   `CHAMPION_ROLE_FALLBACK` with reason `MATCHUP_SAMPLE_TOO_SMALL`.
3. **Historical baseline:** If the requested anchor patch has no valid
   champion-role snapshot, search at most two earlier anchor patches for the
   same queue, champion, and role. Return `HISTORICAL_CHAMPION_ROLE` with the
   actual patch range and reason `REQUESTED_PATCH_UNAVAILABLE`.
4. **Last published snapshot:** If the newest aggregation run fails, keep
   serving the last published snapshot with `stale=true` and reason
   `AGGREGATION_FAILED_USING_LAST_PUBLISHED`.
5. **Client-retained response:** If a later API request fails, keep the
   currently rendered successful response. The frontend may restore the last
   successful public response for the same URL filters from `sessionStorage`,
   mark it stale, show the request error, and provide Retry.
6. **Unavailable:** If none of the above exists, return a structured
   data-unavailable response. The page shows no fabricated build.

No fallback substitutes Solo/Duo data for Flex, another role for the selected
role, or another champion for the selected champion. The UI may offer an
explicit action to switch to an available queue or role.

## Public API

### Build Options

`GET /api/champions/{championId}/builds/options`

Returns backend-calculated availability:

- Supported queues and queue state
- Anchor patches
- Roles and baseline sample sizes
- Opponents and exact-matchup sample sizes for the selected role and patch
- Default role
- Default anchor patch

Optional query inputs are `queueId`, `patch`, and `role`. Invalid values
return a safe validation error.

### Build Result

`GET /api/champions/{championId}/builds`

Required query inputs:

- `queueId`
- `patch`
- `role`

Optional query input:

- `opponentId`

The response includes:

- Requested filters
- Effective filters and patch range
- `resultScope`
- `fallbackReason`
- Games, wins, observed win rate, and confidence
- `stale` and historical flags
- Calculation and publication times
- Ordered starting items, boots, core path, and situational items
- Rune page and stat shards
- Spell pair
- Skill order and max priority
- Backend-generated evidence labels and explanation

The controller performs validation and delegates all resolution to the
backend service. It contains no aggregation queries.

## Champion Page Experience

Add a public `Builds` area while preserving champion overview, abilities, and
stored item statistics.

Opening a champion defaults to the strongest available Ranked Solo/Duo role
baseline. Opponent selection is optional.

### Desktop

- Persistent searchable opponent rail
- Role, queue, and patch controls
- One focused build sheet
- Core build first
- Runes, spells, and skills next
- Situational branches only when supported
- Evidence, confidence, patch range, and fallback explanation visible without
  opening a modal

### Mobile

1. Role, queue, and patch controls
2. Searchable touch-friendly opponent selector
3. Core build
4. Runes and summoner spells
5. Skill order
6. Situational items
7. Evidence and confidence

The interface does not require a wide comparison table.

### URL State

The champion page represents state with:

- Existing champion identifier
- `role`
- `queue`
- `patch`
- Optional `opponent`

A shared URL restores the same requested filters. The response may still
identify a fallback when the exact scope is unavailable.

## Error and Empty States

The page defines:

- Initial loading skeleton
- Role unavailable
- Exact matchup sample insufficient
- Flex insufficient
- Historical fallback
- Stale last-published snapshot
- Network or server error with retained response
- No cached response and data unavailable

Changing filters must not blank a valid build before the replacement request
succeeds. During a request, the old build remains visible with a loading
overlay or status. On failure it remains visible, is marked stale, and exposes
a Retry action.

## Accessibility and Motion

- Touch targets are at least 44 by 44 CSS pixels.
- Every selector has a persistent visible label.
- Keyboard order follows visual order.
- Opponent results support keyboard selection.
- Loading and result changes use a polite live region.
- Error announcements are assertive only when the action cannot continue.
- Selected, stale, fallback, unavailable, and confidence states use text and
  icons in addition to color.
- Meaningful feedback lasts 150 to 220 milliseconds.
- Reduced-motion preferences disable nonessential transitions.
- Champion and item images reserve dimensions to prevent layout shift.
- Layouts are verified at 375, 768, 1024, and 1440 CSS pixels.

## Legacy and Historical Data

The existing `/api/champions/{championId}/items` result mixes stored patches,
queues, roles, and opponents. It remains labeled `Stored item statistics` and
is never used as a fallback for the scoped Builds area.

New snapshots backfill only from eligible rows and include an aggregation
version plus input watermark. Old analyzed player-insight data remains
untouched. Missing scope is excluded instead of defaulted.

Derived snapshots remain available after their source patch stops being
current. Raw match and timeline archival is implemented separately after the
visible product is stable. Archival must verify that required derived data
exists before moving raw payloads out of hot storage.

## Rate-Limit Policy

Build aggregation and public build reads make zero Riot API calls. Corpus
growth uses the existing shared refresh and crawler budget, with queues kept
separate. No page request initiates corpus crawling.

The current process-local limiter remains acceptable for one application
instance. Horizontal deployment requires a shared budget before multiple
instances may perform Riot ingestion concurrently.

## Security and Privacy

- All build endpoints are public read-only endpoints.
- No account or saved-profile data appears in build snapshots.
- Invalid filters are rejected before repository access.
- Raw Riot payloads are never returned by build endpoints.
- Error responses contain safe categories and never SQL, raw JSON, secrets,
  or stack traces.

## Testing Strategy

### Backend Unit Tests

Use deterministic fixtures to verify:

- Numeric patch ordering
- 70/30 weighting
- Queue isolation
- Canonical role selection
- Unique opposing-role pairing
- Match-duration and completeness filtering
- Ordered purchase reconstruction, including undo and sell behavior
- Completed-item filtering
- Rune pages and stat shards
- Spell-pair normalization
- Skill sequence and max priority
- Frequency-first ranking and win-rate tie-breaks
- Confidence boundaries at 9, 10, 24, 25, 49, and 50 games
- Exact-to-baseline fallback
- Historical lookback limit
- No cross-role or cross-queue fallback
- Last-published preservation after aggregation failure

### PostgreSQL Integration Tests

Verify:

- Snapshot uniqueness
- Side-by-side aggregation versions
- Transactional publication
- Failed-run preservation
- Input watermark persistence
- Indexed snapshot reads
- Concurrent aggregation safety
- JSON payload round-trip

### Controller Tests

Verify:

- Public access without a session
- Filter validation
- Exact result response
- Champion-role fallback response
- Historical and stale metadata
- Structured unavailable response

### Frontend Tests

Verify:

- URL restoration and sharing
- Backend-provided ordering is preserved
- No statistical calculations exist in JavaScript
- Opponent selection and fallback copy
- Flex unavailable state
- Existing content remains during loading and errors
- `sessionStorage` response restoration is visibly stale
- Keyboard navigation, focus states, and live-region updates
- Mobile module order

### Visual and Manual Verification

- Verify 375, 768, 1024, and 1440 CSS-pixel layouts.
- Verify long champion, opponent, item, and rune names.
- Verify missing images and partial components.
- Verify reduced motion.
- Run one champion-role baseline, one qualifying exact matchup, one sparse
  matchup fallback, one Flex unavailable state, and one retained-error state
  against the local application.

## Exit Criteria

- A public visitor can open a champion and view a complete Ranked Solo/Duo
  champion-role build.
- The visitor can select role, queue, patch, and opponent.
- A qualifying exact matchup is clearly distinguished from a baseline
  fallback.
- Flex remains separate and honestly unavailable when its sample is too low.
- Every result exposes sample, confidence, patch range, and calculation time.
- Shared URLs restore requested filters.
- Backend owns every statistical and fallback decision.
- Errors retain the last valid build when one exists.
- No request-time aggregation or Riot API call occurs.

## Non-Goals

- No SPA or frontend-framework migration
- No account requirement
- No queue mixing
- No rank-tier recommendations without rank-at-match provenance
- No claim that observed builds are universally optimal
- No synchronous aggregation on page requests
- No automatic corpus crawl from the build page
- No raw-data archival in the first visible release
