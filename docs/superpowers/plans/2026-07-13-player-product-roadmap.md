# Riot Stats Player Product Development Plan

> **For agentic workers:** This is a product-development roadmap, not a single narrow implementation plan. Use it to decide what to build next and then break each phase into its own implementation plan before touching code.

**Goal:** Turn the current Riot stats crawler plus analysis UI into a player-facing product that ranked self-improvement players want to return to after every play session, while remaining simple enough for general League players to understand and use quickly.

**Architecture:** Keep the current Spring Boot + PostgreSQL + static frontend architecture for the first product release. Build the product around a player-centric flow: search a Riot ID, land on a player dashboard, see current form, review recent matches, understand prioritized weaknesses, and get concrete next-step recommendations. Add product depth through better frontend-facing APIs, stronger analysis outputs, freshness/status handling, and a tighter onboarding and return loop rather than by multiplying page count.

**Tech Stack:** Java 23, Spring Boot, PostgreSQL, JDBC/JPA, Riot API, Data Dragon, static HTML/CSS/JS frontend, Maven, GitHub Actions

## Global Constraints

- Primary audience: ranked self-improvement players.
- Secondary audience: general League players who want simple summaries and searchable profiles.
- Do not turn the product into a coach/scout platform before the player-centric loop is strong.
- Do not add new frontend frameworks for the first product release.
- Prefer extending the current `/api` frontend controllers and static pages over introducing a separate SPA.
- Freshness, trust, and insight quality matter more than more pages.
- New features must clearly support one of these questions:
  - `How am I doing?`
  - `What am I doing wrong?`
  - `What should I do next game?`
- Build one player profile flow well before broadening into social/community features.
- Every phase must produce a usable, testable increment that could be shown to players.

---

## Product Summary

### Product Positioning

The product should be positioned as a `personal ranked improvement companion` with `easy-to-share public stat pages` as a secondary use case.

This means the product should prioritize:

1. fast Riot ID lookup
2. a player dashboard as the default experience
3. recent-form tracking
4. trustworthy refresh/freshness states
5. prioritized, actionable insights
6. post-match return reasons

### What the Project Already Has

The current project already includes the technical basis for a real product:

- Riot API ingestion and rate-limited client logic
- persistence for matches, players, ranks, timelines, and static data
- frontend-facing controllers for:
  - overview stats
  - search
  - champions
  - player summary
  - player matches
  - player champions
  - player insights
  - player ranks and rank history
  - match details
- static frontend pages for:
  - home
  - players
  - player detail
  - champions
  - champion detail
  - match detail
- analysis scheduling and data-integrity infrastructure

### Main Product Gaps

The current project still behaves more like an internal stats browser than a player product because it lacks:

- a strong player-owned entry flow
- freshness and trust UX
- insight prioritization and actionability
- a habit-forming post-match return loop
- lightweight social/share outputs
- a clean path from first visit to repeated use

---

## Development Principles

### Principle 1: Optimize for player return, not page breadth

Do not spend the next major cycle creating many more top-level pages. The primary goal is to make one player profile experience so useful that ranked players return after playing.

### Principle 2: Insight quality beats feature count

Weak or vague recommendations will damage trust faster than missing features. Product work must improve evidence quality, prioritization, and clarity of action.

### Principle 3: Freshness is part of the feature

Stats without visible sample size, update time, and refresh state will feel unreliable. Freshness is not an operational detail; it is part of the user experience.

### Principle 4: Keep the first release technically conservative

The current architecture is good enough to reach a real product milestone. Avoid a frontend rewrite or major platform migration before the player loop is validated.

### Principle 5: Ship in player-facing increments

Each phase should result in something a real player can use and react to:

- first useful dashboard
- first trustworthy refresh experience
- first actionable recommendation loop
- first shareable summary

---

## Workstreams

The roadmap should be managed across six workstreams:

1. `Player Experience`
2. `Insights and Analysis Quality`
3. `Search and Onboarding`
4. `Freshness, Data Trust, and Sync`
5. `Retention and Sharing`
6. `Platform Readiness`

These workstreams should move in parallel where possible, but player experience and insight quality should drive priority decisions.

---

## Phase 0: Product Foundation and Decision Lock-In

**Objective:** Align the project around one clear product shape before building more surface area.

**Outcome:** The team agrees on the first real product release as a player-centric dashboard product for solo/duo ranked improvement.

### Deliverables

- product brief
- v1 audience definition
- feature prioritization matrix
- first-release success metrics
- release scope definition

### Required Decisions

- Supported regions for v1
- Supported queues for v1
- Whether the first release is public profile only or includes saved profiles
- Minimum acceptable sample size for recommendations
- How often player data is refreshed
- Whether player refresh is pull-only or partially automated

### Must-Have Product Definitions

- `Primary use case`: "A ranked player searches their Riot ID, sees current form, recent trends, and what to fix next."
- `Secondary use case`: "A casual League player searches a Riot ID or champion and gets a clean, understandable public summary."
- `Not v1`: coaching collaboration, teams/clubs, monetization, deep social features, mobile app

### Suggested Repo Outputs

- Add: `docs/product/vision.md`
- Add: `docs/product/v1-scope.md`
- Add: `docs/product/success-metrics.md`

### Exit Criteria

- One-page product brief is written
- v1 feature list is frozen
- Nice-to-have features are explicitly moved out of v1

---

## Phase 1: Make One Player Profile Immediately Valuable

**Objective:** Turn the current player detail experience into the main product entry point.

**Outcome:** A player can search their Riot ID and land on a dashboard that clearly answers how they are doing and what they should look at next.

### User Experience Goals

- Search should feel Riot-ID first, not database-browser first.
- The default player page should lead with form, trend, and next actions.
- A player should understand their current state in under 30 seconds.

### Features to Build

#### 1. Riot ID first-run flow

Add:

- explicit Riot ID entry pattern: `gameName#tagLine`
- region selector if the backend supports multiple routes
- empty-state onboarding on the homepage
- direct redirect to the searched player profile

#### 2. Player dashboard summary

Reframe the player page around:

- rank and rank change
- recent-form summary
- win/loss trend over last 5/10/20 games
- best current champions
- weakest current pattern
- top 3 recommended improvements

#### 3. Champion pool health

Show:

- most-played champions
- highest-performing champions
- unstable champions
- pool overextension warning if champion spread is too wide

#### 4. Recent match review strip

Improve recent matches into a product surface:

- quick-read row cards
- result + role + champion + key KPI
- identify repeat failure patterns
- jump to detailed match view

### Backend / Data Work

Likely touched areas:

- `src/main/java/org/main/controller/frontend/PlayerController.java`
- `src/main/java/org/main/controller/frontend/PlayerRankController.java`
- `src/main/java/org/main/controller/frontend/FrontendSearchController.java`
- `src/main/java/org/main/service/frontend/FrontendStatsService.java`
- `src/main/java/org/main/service/frontend/FrontendStatsServiceImpl.java`
- rank and match summary SQL inside `FrontendStatsServiceImpl`

New likely DTOs:

- `PlayerDashboardDto`
- `PlayerTrendDto`
- `PlayerChampionPoolHealthDto`
- `PlayerPriorityInsightDto`

### Frontend Work

Likely touched areas:

- `src/main/resources/static/index.html`
- `src/main/resources/static/player.html`
- `src/main/resources/static/js/search.js`
- `src/main/resources/static/js/home.js`
- `src/main/resources/static/js/player.js`
- shared CSS for dashboard summary modules

### Success Metrics

- player can reach their profile in under 2 interactions
- player page explains current form without scrolling deep
- at least one recommendation is visible above the fold

### Exit Criteria

- Riot ID search is the primary entry flow
- player page is clearly a dashboard, not just a data dump
- recent form, champion pool, and top actions are visible immediately

---

## Phase 2: Turn Insights into Real Recommendations

**Objective:** Transform raw insights into trusted, actionable coaching-like guidance.

**Outcome:** The product can tell a player what they are doing wrong and what to change next game in a way that feels evidence-based.

### Product Requirement

Every recommendation must include:

- `priority`
- `category`
- `evidence`
- `recommended action`
- `why it matters`

### Insight Taxonomy

Define a stable insight model across categories:

- laning
- farming / CS
- vision
- deaths / positioning
- champion pool
- macro / objective play
- consistency / streak risk
- queue or champion-specific issues

### Recommendation Shape

Replace vague outputs with:

- clear titles
- one concrete action
- confidence/sample-size framing
- short supporting evidence

Example:

- Bad: `Low vision score`
- Good: `In your last 12 jungle losses, control wards were purchased later and less often than in your wins. Buy your first control ward before minute 8.`

### Backend / Analysis Work

Likely touched areas:

- `src/main/java/org/main/service/analysis/MatchAnalysisService.java`
- `src/main/java/org/main/service/analysis/MatchAnalysisServiceImpl.java`
- `src/main/java/org/main/service/frontend/FrontendStatsServiceImpl.java`
- any SQL sourcing `analyzed.player_insights`

Potential additions:

- insight severity calculation
- evidence/sample-size fields
- comparison to personal baseline
- comparison to win vs loss behavior
- comparison by champion or role

### Data Work

Needed improvements:

- stronger role inference
- queue segmentation
- champion-specific vs global player trends
- minimum sample thresholds
- confidence scoring

### Frontend Work

Player insight surfaces should show:

- top 3 priorities first
- grouped recommendations below
- "why this is showing up"
- "what to do next game"

Likely touched areas:

- `src/main/resources/static/js/player.js`
- `src/main/resources/static/css/recommendations.css`
- `src/main/resources/static/player.html`

### Success Metrics

- each profile has a maximum of 3 priority items above the fold
- insight cards are concrete enough to summarize in one sentence
- low-sample or weak-confidence recommendations are visually downgraded

### Exit Criteria

- insights are prioritized and actionable
- weak data is clearly labeled
- players can identify one next-game behavior change from the page

---

## Phase 3: Add Freshness, Refresh, and Trust UX

**Objective:** Make players trust the data.

**Outcome:** Players can see what data the product is based on, how fresh it is, and what is happening when they refresh.

### Features to Build

#### 1. Freshness metadata

Show on the player dashboard:

- last updated at
- based on last N ranked matches
- patch/version if available
- sample-size warnings

#### 2. Refresh workflow

Add:

- refresh button states
- loading/progress states
- retry/failure states
- success confirmation

#### 3. Sync status surface

Expose whether the system is:

- using cached local data
- updating ranks
- updating recent matches
- blocked by API/rate limits

### Backend Work

Likely touched areas:

- `src/main/java/org/main/service/RankEnrichmentService.java`
- `src/main/java/org/main/service/RankEnrichmentServiceImpl.java`
- `src/main/java/org/main/service/IngestLogService.java`
- `src/main/java/org/main/service/IngestLogServiceImpl.java`
- `src/main/java/org/main/controller/frontend/PlayerRankController.java`
- possibly new frontend sync-status endpoints

Likely new DTOs:

- `PlayerRefreshStatusDto`
- `PlayerDataFreshnessDto`

### Frontend Work

Likely touched areas:

- `src/main/resources/static/js/player.js`
- `src/main/resources/static/player.html`
- shared state components in CSS

### Success Metrics

- player can tell whether data is fresh within 3 seconds
- refresh actions always return a visible completion state
- stale-data scenarios are explained, not silently ignored

### Exit Criteria

- freshness is visible
- refresh is reliable from a user perspective
- trust-damaging ambiguity is removed

---

## Phase 4: Build the Post-Match Return Loop

**Objective:** Give players a reason to come back after every play session.

**Outcome:** The product becomes habit-forming instead of one-time searchable.

### Core Loop

After a player returns:

- show what changed since the last refresh
- show whether current priorities improved or worsened
- highlight new rank movement
- highlight new champion trend movement
- update recommendations accordingly

### Features to Build

#### 1. Session delta summary

Add:

- `since last update`
- `last 5 matches vs previous 5`
- changed recommendation status
- changed rank or ladder state

#### 2. Form tracker

Add:

- current streak context
- stability vs volatility
- trend up / flat / down

#### 3. Recommendation follow-through

Track:

- whether a previous recommendation improved
- whether it worsened
- whether there is not enough data yet

### Backend Work

Potential additions:

- historical snapshots of derived player summary state
- delta comparison query logic
- recommendation status change model

Likely touched areas:

- `FrontendStatsServiceImpl`
- analysis tables or snapshot tables
- rank history and ingest log integration

### Frontend Work

Likely touched areas:

- `player.html`
- `player.js`
- dashboard summary modules

### Success Metrics

- player sees a meaningful delta summary after new matches are loaded
- the page explains not just current state but direction of change

### Exit Criteria

- product supports a clear "play -> return -> review -> adjust" loop

---

## Phase 5: Make the Product Easy for General Players

**Objective:** Reduce friction and make the product understandable for users who are not deep stat nerds.

**Outcome:** A casual player can search a profile and understand the result quickly without prior context.

### Features to Build

- better empty states
- simpler copy on home and player pages
- one-screen summary before dense detail
- glossary/tooltips for dense metrics
- cleaner fallback states when data is thin

### UX Rules

- leading modules should be plain-language summaries
- advanced sections should come later
- no jargon-only surfaces above the fold

### Frontend Work

Likely touched areas:

- homepage copy and structure
- player page information hierarchy
- tooltips / helper text
- search and no-result states

### Success Metrics

- first-time user can understand the page without reading documentation
- key recommendations are expressed in plain language

### Exit Criteria

- the product is usable by both self-improvement and general players

---

## Phase 6: Add Sharing and Lightweight Virality

**Objective:** Make outputs easy to share so the product can spread organically.

**Outcome:** Players can share profile snapshots, match recaps, or trend summaries in Discord or social channels.

### Features to Build

- public profile links
- shareable player summary cards
- shareable match recap surface
- champion pool snapshot share block

### Product Rule

Shared outputs must look good as images and be understandable without the rest of the app.

### Backend / Frontend Work

Possible approaches:

- printable/share-friendly HTML views
- exportable card-style components
- image capture route later if needed

### Success Metrics

- profile and match views can be shared as links
- the shared view stands on its own without needing explanation

### Exit Criteria

- the product supports link and image-style sharing for the most valuable surfaces

---

## Phase 7: Platform Readiness and Product Operations

**Objective:** Make the product reliable enough to support real users.

**Outcome:** The app is operationally stable and measurable.

### Must-Have Areas

#### 1. Reliability

- refresh queue reliability
- graceful Riot API failure handling
- clear retry/backoff behavior
- data integrity repair paths for user-facing flows

#### 2. Performance

- faster profile load for hot paths
- API query optimization for dashboard endpoints
- loading-state UX for slower pages

#### 3. Product analytics

Track:

- search success rate
- refresh usage
- player page retention
- most used modules
- recommendation click/open behavior

#### 4. Deployment maturity

- consistent environment setup
- runtime requirements documented correctly
- production runbooks aligned with the actual user-facing app

### Exit Criteria

- product behavior is observable
- failures are explainable
- common flows are performant enough for repeated use

---

## Recommended Delivery Order

Build in this order:

1. `Phase 0` product lock-in
2. `Phase 1` player dashboard value
3. `Phase 2` insight quality
4. `Phase 3` freshness and trust
5. `Phase 4` return loop
6. `Phase 5` simplification for general players
7. `Phase 6` sharing
8. `Phase 7` platform readiness hardening

Do not move to Phase 6 before Phases 1 to 4 are good enough to retain a player.

---

## Suggested First Release Scope

### In Scope for the First Real Product Release

- Riot ID search flow
- player dashboard landing page
- recent-form summary
- rank and trend summary
- top 3 prioritized recommendations
- champion pool health
- visible freshness and refresh states
- recent match review flow

### Explicitly Out of Scope for the First Release

- mobile app
- subscriptions/monetization
- coach/team workspaces
- highly social community features
- a complete OP.GG-style global stats platform
- large frontend architecture rewrite

---

## Launch Readiness Criteria

The product is ready for a meaningful early launch when all of the following are true:

- a player can search their Riot ID and reach a useful profile quickly
- the player page explains current form clearly
- recommendations are concrete and evidence-based
- freshness/update state is visible and understandable
- recent matches and detailed match review are trustworthy
- the player has a reason to return after playing again

If any of those are missing, the project is still a strong technical system but not yet a complete player product.

---

## Recommended Next Planning Breakdown

This roadmap should be broken into separate implementation plans in this order:

1. `Player Dashboard MVP`
2. `Actionable Insights Refactor`
3. `Freshness and Refresh UX`
4. `Post-Match Return Loop`
5. `General Player Onboarding Simplification`
6. `Sharing Surfaces`
7. `Product Analytics and Hardening`

Each of those plans should identify:

- exact backend files
- exact frontend files
- required DTO and endpoint changes
- SQL/query work
- testing strategy
- rollout and verification steps
