# Player Loop and Champion Matchup Builds Design

**Date:** 2026-07-13

**Status:** Approved for implementation planning

## Goal

Turn Riot Stats into a launchable player product through two connected vertical slices:

1. A public player dashboard with optional accounts, saved profiles, actionable recommendations, and trustworthy refresh behavior.
2. Champion matchup build pages derived from stored Riot match data, with clear patch, queue, sample, and confidence context.

The product must remain publicly browsable. Accounts add persistence and personalization but never gate player profiles, champion pages, builds, or matchup data.

## Product Decisions

- Primary audience: ranked players who want to improve.
- Secondary audience: general League players looking up public profiles and champion information.
- Initial platform and routing scope: EUW1 and the Europe regional route.
- Supported queues: Ranked Solo/Duo and Ranked Flex, calculated and displayed separately.
- Frontend architecture: existing static HTML, CSS, and JavaScript served by Spring Boot.
- Backend architecture: existing Spring Boot and PostgreSQL application.
- Account authentication: email/password and Discord OAuth.
- Saved profiles: users may save any public Riot profile; saving never claims ownership.
- Data access: all Riot profile and champion information remains public.
- Build freshness: current and previous patch, weighted 70% and 30% respectively.
- Historical policy: derived statistics are retained; raw match and timeline payloads become archive candidates after 90 days.
- Delivery model: stabilize the current redesign, then deliver the Player Loop and Champion Matchup Builds as separate vertical slices.

## Non-Goals

- No SPA or frontend-framework migration.
- No mobile application.
- No coaching or team workspace.
- No social network or community feed.
- No profile ownership claim without a separate Riot authorization flow.
- No requirement to sign in before viewing public data.
- No runtime aggregation of large matchup datasets during page requests.
- No mixing Solo/Duo and Flex statistics.
- No active-build recommendations based on obsolete items or runes.

## Architecture

Keep the existing Spring Boot, PostgreSQL, and static frontend architecture. Introduce bounded modules with explicit DTOs rather than coupling account, refresh, dashboard, and build behavior inside existing controllers.

### Account and Session Module

Responsibilities:

- Register users with email and password.
- Authenticate users with email/password or Discord OAuth.
- Create, rotate, validate, and expire application sessions.
- Store account preferences.
- Expose the current account state to the static frontend.

Public Riot endpoints must not depend on this module for authorization. Authentication is required only for private account actions such as modifying saved profiles or preferences.

### Saved Profiles Module

Responsibilities:

- Save and unsave any public Riot profile.
- Add an optional private personal label.
- Track save time and last-viewed time.
- Select a default landing profile.
- Prevent duplicate saves for the same user and PUUID.

The interface must use the term `Saved`, not `Owned` or `My Riot account`.

### Player Dashboard Module

Responsibilities:

- Aggregate rank, rank history, recent matches, champion performance, and insights.
- Produce recent-form summaries for 5, 10, and 20 matches.
- Produce champion-pool health.
- Select a maximum of three priority recommendations.
- Include data freshness, sample size, and refresh state.
- Return one frontend-facing dashboard response.

The initial implementation may calculate the response from existing tables with a short cache. Durable dashboard snapshots are introduced when post-session delta comparisons are implemented.

### Refresh Coordinator

Responsibilities:

- Enforce manual refresh cooldowns.
- Deduplicate concurrent refreshes for the same PUUID.
- Respect Riot API rate-limit headers and bounded retry behavior.
- Track queued, running, completed, rate-limited, and failed states.
- Schedule refreshes only for recently active saved profiles.
- Prioritize work using recent views, staleness, cooldown eligibility, and available API budget.

Controllers request refresh work from the coordinator. They do not call Riot refresh operations independently.

### Champion Build Aggregator

Responsibilities:

- Derive builds from stored participants, items, runes, summoner spells, skill order, matches, and timelines.
- Segment data by champion, role, opponent, queue, and patch window.
- Keep Solo/Duo and Flex aggregates independent.
- Weight current-patch observations at 70% and previous-patch observations at 30%.
- Calculate games, wins, weighted score, sample size, and confidence.
- Produce starting items, boots, core item path, situational items, runes, summoner spells, and skill order.
- Version aggregation logic so results can be recalculated safely.

Aggregation runs asynchronously. Build page requests read prepared statistics.

### Archive Module

Responsibilities:

- Identify raw match and timeline payloads older than 90 days.
- Verify required derived statistics exist before archival.
- Compress and move eligible raw payloads out of hot storage.
- Keep derived player, match, and build statistics available indefinitely.
- Support historical rebuild research without placing archived payloads on hot query paths.

Archival must be idempotent and must never remove raw data before derived-data verification succeeds.

## Data Model

### Application Accounts

`app_user`

- `id`
- `email_normalized`
- `password_hash`
- `display_name`
- `status`
- `email_verified_at`
- `created_at`
- `updated_at`

`oauth_identity`

- `id`
- `user_id`
- `provider`
- `provider_subject_id`
- `created_at`
- `last_login_at`

`user_session`

- `id`
- `user_id`
- `token_hash`
- `expires_at`
- `last_used_at`
- `created_at`

`account_action_token`

- `id`
- `user_id`
- `token_type`: email verification or password reset
- `token_hash`
- `expires_at`
- `consumed_at`
- `created_at`

`saved_profile`

- `id`
- `user_id`
- `puuid`
- `personal_label`
- `is_default`
- `saved_at`
- `last_viewed_at`

Enforce uniqueness on normalized email, provider plus provider subject ID, and user plus PUUID.

### Refresh State

The refresh model records:

- PUUID
- request source: manual or scheduled
- state
- requested, started, and completed timestamps
- retry-after time when rate limited
- failure category and safe user-facing message
- aggregation or ingestion version where relevant

Only one active refresh job may exist for a PUUID.

### Build Statistics

Build statistics use a key equivalent to:

`aggregation version + current patch + comparison patch + queue + champion + role + opponent + component`

Stored values include:

- component type
- normalized component value or sequence
- games
- wins
- current-patch games
- previous-patch games
- weighted score
- confidence level
- calculated timestamp

The concrete relational layout may use separate tables for item sequences, rune pages, spells, and skill orders when that improves constraints and query clarity. The public DTO remains stable regardless of the internal table split.

## Player Experience

### Homepage and Search

The primary homepage action accepts a Riot ID in `gameName#tagLine` form, explains EUW1 support, and redirects a successful lookup to the public player dashboard.

Sign-in prompts appear only after the user receives public value, such as when choosing `Save profile`. They must never interrupt browsing.

### Player Dashboard

The initial viewport answers:

1. How am I doing?
2. What is changing?
3. What should I work on next?

Above-the-fold content:

- Player identity and current rank
- Last successful update and sample size
- Recent form for 5, 10, and 20 matches
- Rank movement
- Best-performing and most-played champions
- Champion-pool health
- Three priority recommendations
- Refresh action and visible sync state

Detailed matches, full champion statistics, rank history, and the complete recommendation list remain below or in the existing tabs.

### Accounts and Saved Profiles

The header provides sign-in and account access without displacing global search. Signed-in users may:

- Save and unsave public profiles
- Add private labels
- View saved and recently viewed profiles
- Choose a default profile
- Inspect refresh state

No public page changes visibility based on authentication.

## Champion Matchup Build Experience

Add a `Builds` area to the champion page while preserving the existing overview, abilities, and item statistics.

### Desktop

Use an opponent-first workspace:

- Persistent opponent list
- Role, queue, and patch controls
- One focused matchup build sheet
- Complete recommended build
- Alternative branches only when supported by sufficient samples
- Sample size, confidence, patch range, and observed win rate
- Short explanation of why the build is recommended

### Mobile

Use prioritized stacked modules:

1. Opponent selector
2. Role, queue, and patch filters
3. Core build
4. Runes and summoner spells
5. Skill order
6. Situational items
7. Evidence and confidence

The desktop opponent rail becomes a touch-friendly horizontal strip or searchable selector. A wide comparison table is not required on mobile.

### URL State

Champion, role, opponent, queue, and patch inputs must be represented in the URL. A shared link restores the same build view.

### Confidence and Historical Fallback

Build responses expose:

- active patch window
- queue
- games and wins
- confidence level
- whether historical fallback is in use
- historical patch range when applicable

The exact numerical confidence thresholds are configuration values finalized in the implementation plan after inspecting real EUW1 sample distributions. Until sufficient active samples exist, the UI shows a low-confidence or historical result and never presents it as a current high-confidence recommendation.

## Error and State Handling

All dashboard and build surfaces define:

- Loading
- Empty
- Stale
- Refresh queued
- Refresh running
- Rate limited
- Refresh failed with cached data retained
- Low confidence
- Historical fallback
- Data unavailable

Errors include a recovery action where one exists. Failed refreshes never erase valid cached data.

## Security

- Use a modern adaptive password hash.
- Normalize and uniquely constrain email addresses.
- Rate-limit registration and login.
- Identify Discord users by immutable provider subject ID.
- Store only session token hashes.
- Rotate the session after login.
- Use `HttpOnly` session cookies.
- Use `Secure` cookies in production.
- Use `SameSite=Lax` unless the OAuth callback requires a narrowly scoped exception.
- Apply CSRF protection to state-changing account actions.
- Verify the current application user for every saved-profile mutation.
- Never log passwords, session tokens, OAuth tokens, or Riot API secrets.

Email verification and password reset use short-lived, single-use hashed tokens. Email/password registration must not be enabled in production until outbound email, verification delivery, and password recovery are configured and tested. Discord authentication remains independent of email delivery.

## Accessibility and Responsive Behavior

- All interactive targets are at least 44 by 44 CSS pixels on touch layouts.
- Every control has a visible label and focus state.
- Selected, stale, failed, and confidence states do not rely on color alone.
- Dynamic updates use appropriate live-region announcements.
- Keyboard order follows visual order.
- Motion is limited to meaningful 150-220 ms feedback.
- Reduced-motion preferences disable nonessential transitions.
- Layouts are verified at 375, 768, 1024, and 1440 CSS pixels.
- Champion and item images reserve space to prevent layout shift.

## Testing Strategy

### Backend

Test:

- Registration, login, logout, session rotation, and session expiry
- Email verification, token expiry, token reuse prevention, and password reset
- Discord OAuth linking and repeat login
- CSRF enforcement
- Public endpoint access with and without a session
- Saved-profile authorization and duplicate prevention
- Manual cooldowns
- Scheduled-refresh eligibility
- Refresh deduplication
- Riot rate-limit behavior
- Queue separation
- Patch weighting
- Confidence calculation
- Historical fallback selection
- Archive eligibility, verification, and idempotency

### Frontend

Test:

- Riot ID search and redirect
- Dashboard loading, stale, error, and refresh states
- Save and unsave behavior
- Sign-in prompts that never block public data
- Champion build filtering
- URL restoration and sharing
- Desktop opponent workspace
- Mobile module ordering
- Keyboard navigation and visible focus
- Low-confidence and historical labels

Each vertical slice ends with unit tests, integration tests, responsive visual checks, and a manual run through the application.

## Delivery Sequence

### Stabilization Checkpoint

1. Run the existing frontend tests.
2. Review the uncommitted editorial redesign against its approved specification.
3. Fix only regressions and incomplete states.
4. Reconcile roadmap claims with the current implementation.
5. Commit the redesign as a clean baseline.

### Slice 1: Player Loop

1. Account and session foundation
2. Email/password authentication
3. Discord OAuth
4. Saved public profiles
5. Riot ID first-run search
6. Aggregated player-dashboard endpoint
7. Recent form and champion-pool health
8. Prioritized recommendations
9. Freshness metadata
10. Manual refresh coordinator and cooldown
11. Scheduled refresh for recently active saved profiles
12. Responsive and accessibility verification

Exit criteria:

- A new user can search a Riot ID and use the public dashboard without signing in.
- A user may create an account and save any public profile.
- The dashboard explains recent form and presents three concrete actions.
- Freshness and every refresh outcome are visible.

### Slice 2: Champion Matchup Builds

1. Define the build taxonomy and configurable confidence thresholds.
2. Add champion, role, opponent, queue, and patch-aware aggregation.
3. Add 70/30 current and previous patch weighting.
4. Add historical fallback behavior.
5. Add verified raw-data archival.
6. Expose build and matchup APIs.
7. Add shareable filtered build URLs.
8. Build the desktop opponent workspace.
9. Build the prioritized mobile layout.
10. Add all loading, empty, stale, and confidence states.
11. Verify aggregation correctness and responsive behavior.

Exit criteria:

- A player can choose a champion, role, queue, and opponent.
- The page returns a complete build with visible evidence and confidence.
- Solo/Duo and Flex results remain separate.
- Shared URLs restore the selected build.
- Sparse data is labeled honestly and can use a clearly marked historical fallback.

## Roadmap Updates

Update the product roadmap to include:

- Optional accounts without public-content gating
- Saved-profile semantics
- Email/password and Discord authentication
- Refresh-budget and scheduling policy
- Champion matchup build aggregation
- Patch weighting and confidence
- Raw-data archival
- Historical build fallback
- Shareable filtered build URLs
- Reconciliation of already completed match-card, match-detail, and recommendation work

## Start Recommendation

Begin implementation only after the stabilization checkpoint produces a clean baseline. Write a detailed implementation plan for the checkpoint and Player Loop first. Write the Champion Matchup Builds implementation plan separately after the Player Loop contracts are stable.
