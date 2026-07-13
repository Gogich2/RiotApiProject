# Website Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the full Riot Stats static frontend into a dark-first, responsive, relatively simple analyst console using electric purple `#6800FF` and cream `#FFF9EB`.

**Architecture:** Keep the current Spring-served static HTML/CSS/JS architecture and drive the redesign primarily through shared tokens, layout rules, and shared component styles. Use small, targeted HTML adjustments only where responsiveness or hierarchy requires them, and keep JavaScript changes limited to homepage summary wiring plus any DOM hook updates needed by the revised markup.

**Tech Stack:** Static HTML, CSS, vanilla JavaScript, Spring Boot static resources, Maven for local app startup and verification

## Global Constraints

- Electric purple: `#6800FF`
- Cream: `#FFF9EB`
- Purple is the only dominant accent.
- Cream is used for readable text and selective contrast, not large cream background blocks.
- Keep the current static HTML/CSS/JS stack.
- Keep the current page set and routing model.
- Existing JavaScript behavior stays intact unless small DOM adjustments are required.
- Change primarily through shared tokens in `base.css`.
- Change primarily through shared shell/layout rules in `layout.css`.
- Change primarily through shared components in `components.css`.
- Use targeted page-specific CSS adjustments where necessary.
- Use minimal HTML changes where hierarchy or responsiveness genuinely requires them.
- Avoid adding a new frontend framework.
- Avoid large JS rewrites.
- Avoid decorative complexity that hurts data readability.
- Avoid adding many new homepage sections.
- Major feature additions beyond the single `Meta snapshot` homepage module are out of scope.

---

## File Structure

### Shared foundation

- Modify: `src/main/resources/static/css/base.css`
  - Replace the current teal/gold-dark token system with the dark-first purple/cream token system.
- Modify: `src/main/resources/static/css/layout.css`
  - Rebuild the header, page gutters, section spacing, responsive grid breakpoints, and shared page shell behavior.
- Modify: `src/main/resources/static/css/components.css`
  - Restyle buttons, cards, tables, inputs, utility states, hero modules, and homepage summary modules.

### Homepage

- Modify: `src/main/resources/static/index.html`
  - Add markup hooks for the simplified hero visual summary and the new `Meta snapshot` module.
- Modify: `src/main/resources/static/js/home.js`
  - Populate the new homepage summary module and adapt hero/overview rendering to the revised structure.

### Listing pages

- Modify: `src/main/resources/static/players.html`
  - Tighten listing page hierarchy and table framing.
- Modify: `src/main/resources/static/champions.html`
  - Improve filter/header structure and responsive role controls.
- Modify: `src/main/resources/static/css/champion.css`
  - Restyle champion directory cards, role controls, and champion detail surfaces.

### Detail pages

- Modify: `src/main/resources/static/player.html`
  - Improve section ordering, tabs framing, and responsive layout hooks.
- Modify: `src/main/resources/static/champion.html`
  - Tighten champion hero structure and section rhythm.
- Modify: `src/main/resources/static/match.html`
  - Restyle fallback match page shell and make it consistent with the redesign.
- Modify: `src/main/resources/static/css/player.css`
  - Restyle player detail layout, rank modules, match cards, and tabs for mobile-first stacking.
- Modify: `src/main/resources/static/css/recommendations.css`
  - Align recommendation cards and empty/error states with the new component language.
- Modify: `src/main/resources/static/css/match-details.css`
  - Simplify match detail backgrounds, spacing, and dense data sections.

### Verification

- Verify manually via local app pages:
  - `http://localhost:8080/index.html`
  - `http://localhost:8080/players.html`
  - `http://localhost:8080/champions.html`
  - `player.html` reached by clicking any populated player row from `players.html`
  - `champion.html` reached by clicking any populated champion card from `champions.html`
  - `match.html` reached from an existing player match link or by using any live `matchId` and `puuid` already exposed by the UI

## Task 1: Rebuild the shared design tokens and app shell

**Files:**
- Modify: `src/main/resources/static/css/base.css`
- Modify: `src/main/resources/static/css/layout.css`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/players.html`
- Modify: `src/main/resources/static/player.html`
- Modify: `src/main/resources/static/champions.html`
- Modify: `src/main/resources/static/champion.html`
- Modify: `src/main/resources/static/match.html`
- Test: manual browser verification on all pages after CSS foundation changes

**Interfaces:**
- Consumes: Existing shared classes `.site-header`, `.site-nav`, `.page`, `.hero`, `.stats-grid`, `.dashboard-panel`, `.table-wrapper`, `.table`, `.button`, `.search`, `.filter-input`
- Produces: Updated shared visual contract for all pages without changing route names or script includes

- [ ] **Step 1: Add the new shared token palette in `base.css`**

```css
:root {
    --font-family-base: Inter, "Segoe UI", Arial, sans-serif;

    --color-bg-app-rgb: 10, 7, 20;
    --color-bg-app: #0a0714;
    --color-bg-canvas: #120c24;

    --color-surface-1: #151025;
    --color-surface-2: #1b1430;
    --color-surface-3: #241a3c;
    --color-surface-4: #2d2150;
    --color-surface-hover: #31255a;
    --color-surface-hover-strong: #3b2b6f;

    --color-border-subtle: rgba(255, 249, 235, 0.08);
    --color-border-default: rgba(255, 249, 235, 0.14);
    --color-border-strong: rgba(255, 249, 235, 0.22);
    --color-border-accent: rgba(104, 0, 255, 0.58);
    --color-border-accent-soft: rgba(104, 0, 255, 0.28);

    --color-text-primary: #fff9eb;
    --color-text-secondary: rgba(255, 249, 235, 0.78);
    --color-text-muted: rgba(255, 249, 235, 0.56);
    --color-text-inverse: #fff9eb;

    --color-link: #b691ff;
    --color-accent: #6800ff;
    --color-accent-hover: #7d29ff;
    --color-accent-soft: rgba(104, 0, 255, 0.16);
    --color-accent-soft-strong: rgba(104, 0, 255, 0.28);
    --color-accent-text: #ede3ff;
    --color-focus: #8f4dff;
    --color-highlight: #fff9eb;
    --color-highlight-soft: rgba(255, 249, 235, 0.1);
    --color-highlight-text: #fff9eb;

    --shadow-overlay: 0 24px 48px rgba(2, 0, 8, 0.46);
    --shadow-card: 0 18px 36px rgba(0, 0, 0, 0.24);
    --shadow-subtle: 0 10px 20px rgba(0, 0, 0, 0.18);
}
```

- [ ] **Step 2: Rework the page background and shared text baseline in `base.css`**

```css
body {
    position: relative;
    min-height: 100vh;
    isolation: isolate;
    margin: 0;
    font-family: var(--font-family-base);
    background:
        radial-gradient(circle at top right, rgba(104, 0, 255, 0.18), transparent 28%),
        linear-gradient(180deg, rgba(255, 249, 235, 0.04) 0, transparent 18%),
        var(--color-bg-app);
    color: var(--color-text-primary);
}

body::before {
    content: "";
    position: fixed;
    inset: 0;
    z-index: 0;
    pointer-events: none;
    opacity: 0.36;
    background:
        linear-gradient(180deg, transparent 0, rgba(0, 0, 0, 0.26) 100%),
        radial-gradient(circle at 15% 20%, rgba(104, 0, 255, 0.08), transparent 18%),
        repeating-linear-gradient(
            90deg,
            transparent 0 48px,
            rgba(255, 249, 235, 0.025) 48px 49px
        );
}
```

- [ ] **Step 3: Rebuild the shared header and page shell in `layout.css`**

```css
.site-header {
    position: sticky;
    top: 0;
    z-index: 20;
    display: grid;
    grid-template-columns: auto auto minmax(240px, 360px);
    align-items: center;
    gap: 18px;
    padding: 18px 24px;
    backdrop-filter: blur(20px);
    background: rgba(var(--color-bg-app-rgb), 0.82);
    border-bottom: 1px solid var(--color-border-subtle);
}

.page {
    position: relative;
    z-index: 1;
    width: min(1240px, calc(100% - 32px));
    margin: 0 auto;
    padding: 28px 0 72px;
}

@media (max-width: 920px) {
    .site-header {
        grid-template-columns: 1fr auto;
    }

    .search {
        grid-column: 1 / -1;
        width: 100%;
        margin-left: 0;
    }
}

@media (max-width: 640px) {
    .page {
        width: min(100% - 24px, 1240px);
        padding: 20px 0 48px;
    }

    .site-header {
        grid-template-columns: 1fr;
        gap: 14px;
        padding: 14px 12px;
    }
}
```

- [ ] **Step 4: Unify buttons, inputs, panels, stats, and tables in `components.css`**

```css
.button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-height: 44px;
    padding: 0 18px;
    border: 1px solid transparent;
    border-radius: var(--radius-pill);
    background: linear-gradient(135deg, #6800ff 0, #8f4dff 100%);
    color: #fff9eb;
    font-size: 14px;
    font-weight: 800;
    box-shadow: 0 10px 24px rgba(104, 0, 255, 0.22);
}

.button--secondary,
.filter-input,
.search__input,
.dashboard-panel,
.stat-card,
.table-wrapper {
    background: rgba(27, 20, 48, 0.92);
    border: 1px solid var(--color-border-default);
}

.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

@media (max-width: 900px) {
    .stats-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }
}

@media (max-width: 560px) {
    .stats-grid {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 5: Run the app and manually verify the new shell is stable**

Run:

```bash
.\mvnw.cmd spring-boot:run
```

Expected:

```text
Application starts successfully and the shared header, page gutters, buttons, tables, and stat cards render with the purple/cream dark theme on all pages.
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/css/base.css src/main/resources/static/css/layout.css src/main/resources/static/css/components.css src/main/resources/static/index.html src/main/resources/static/players.html src/main/resources/static/player.html src/main/resources/static/champions.html src/main/resources/static/champion.html src/main/resources/static/match.html
git commit -m "feat: rebuild riot stats shared design foundation"
```

## Task 2: Redesign the homepage and add the Meta snapshot module

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/static/css/layout.css`
- Modify: `src/main/resources/static/js/home.js`
- Test: manual browser verification on `index.html`

**Interfaces:**
- Consumes: `api.getOverview(): Promise<OverviewStatsDto>`, `api.getPlayerLeaderboards(): Promise<PlayerLeaderboardResponseDto>`, existing `renderChampionTable(elementId, champions)`
- Produces:
  - `renderMetaSnapshot(overview, leaderboards): void`
  - `buildMetaSnapshotCards(overview, leaderboards): string`
  - New homepage DOM hook `#metaSnapshot`

- [ ] **Step 1: Add the Meta snapshot container and simplify the homepage hero markup in `index.html`**

```html
<section class="home-hero">
    <div class="home-hero__content">
        <span class="home-hero__eyebrow">Summoner's Rift intelligence</span>
        <h1 class="hero__title">See the shape of every match, not just the result.</h1>
        <p class="hero__text">
            A single local surface for ranked player scouting, champion efficiency, and match-by-match trend reading.
        </p>
        <div class="hero-actions">
            <a class="button" href="players.html">Open players</a>
            <a class="button button--secondary" href="champions.html">Browse champions</a>
        </div>
    </div>

    <aside class="home-hero__summary" id="homeHeroSummary">
        <article class="hero-summary-card">
            <span class="hero-summary-card__label">Current mode</span>
            <strong class="hero-summary-card__value">Local match intelligence</strong>
            <p class="hero-summary-card__text">Ranked sample, player dossiers, and champion efficiency in one surface.</p>
        </article>
    </aside>
</section>

<section class="meta-snapshot" id="metaSnapshot" aria-label="Meta snapshot"></section>
```

- [ ] **Step 2: Add homepage-specific summary styles in `components.css`**

```css
.meta-snapshot {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
    margin-bottom: 22px;
}

.meta-snapshot__card,
.hero-summary-card {
    padding: 18px;
    border-radius: var(--radius-lg);
    background:
        linear-gradient(180deg, rgba(255, 249, 235, 0.03), transparent),
        rgba(27, 20, 48, 0.94);
    border: 1px solid var(--color-border-default);
}

@media (max-width: 900px) {
    .meta-snapshot {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 3: Implement the Meta snapshot renderer in `home.js`**

```js
function renderMetaSnapshot(overview, leaderboards) {
    const container = document.getElementById('metaSnapshot');

    if (!container) {
        return;
    }

    container.innerHTML = buildMetaSnapshotCards(overview, leaderboards);
}

function buildMetaSnapshotCards(overview, leaderboards) {
    const bestChampion = overview?.bestWinrateChampions?.[0];
    const mostPlayedChampion = overview?.mostPopularChampions?.[0];
    const hottestPlayer = leaderboards?.bestPlayers?.[0] || leaderboards?.mostActivePlayers?.[0];

    return [
        buildMetaCard('Best win rate', bestChampion?.championName || 'Unavailable', bestChampion ? `${formatPercent(bestChampion.winrate)} win rate` : 'No champion data'),
        buildMetaCard('Most played', mostPlayedChampion?.championName || 'Unavailable', mostPlayedChampion ? `${formatNumber(mostPlayedChampion.games)} games` : 'No champion data'),
        buildMetaCard('Hot player', getPlayerDisplayName(hottestPlayer), hottestPlayer ? `${formatNumber(hottestPlayer.matches)} matches` : 'No player data')
    ].join('');
}

function buildMetaCard(label, title, meta) {
    return `
        <article class="meta-snapshot__card">
            <span class="meta-snapshot__label">${escapeHtml(label)}</span>
            <strong class="meta-snapshot__title">${escapeHtml(title || 'Unavailable')}</strong>
            <p class="meta-snapshot__meta">${escapeHtml(meta)}</p>
        </article>
    `;
}
```

- [ ] **Step 4: Wire the new summary renderer into the homepage boot flow in `home.js`**

```js
document.addEventListener('DOMContentLoaded', async () => {
    let overview = null;
    let leaderboards = null;

    try {
        [overview, leaderboards] = await Promise.all([
            api.getOverview(),
            api.getPlayerLeaderboards().catch(() => null)
        ]);

        renderOverviewStats(overview);
        renderMetaSnapshot(overview, leaderboards);
        renderChampionTable('popularChampionsBody', overview.mostPopularChampions || []);
        renderChampionTable('bestChampionsBody', overview.bestWinrateChampions || []);
        renderCoverage(overview);
        renderLeaderboardSpotlight(leaderboards);
        renderHomePlayers('homeTopPlayers', leaderboards?.bestPlayers || [], 'winrate');
        renderHomePlayers('homeMostActivePlayers', leaderboards?.mostActivePlayers || [], 'matches');
    } catch (error) {
        document.getElementById('overviewStats').innerHTML = `<div class="error-box">Could not load overview stats.</div>`;
        renderMetaSnapshot(null, null);
    }
});
```

- [ ] **Step 5: Verify the homepage on mobile, tablet, and desktop**

Run:

```bash
.\mvnw.cmd spring-boot:run
```

Expected:

```text
The homepage hero reads as a two-column command surface on desktop, stacks cleanly on smaller widths, and shows a three-card Meta snapshot module directly after the hero.
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/index.html src/main/resources/static/css/components.css src/main/resources/static/css/layout.css src/main/resources/static/js/home.js
git commit -m "feat: redesign homepage summary surfaces"
```

## Task 3: Redesign the listing pages for players and champions

**Files:**
- Modify: `src/main/resources/static/players.html`
- Modify: `src/main/resources/static/champions.html`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/static/css/champion.css`
- Modify: `src/main/resources/static/js/champions.js`
- Test: manual browser verification on `players.html` and `champions.html`

**Interfaces:**
- Consumes:
  - `renderLeaderboardTable(elementId, players): void`
  - `renderVisibleChampions(champions, filterInput, selectedRole): void`
  - Existing IDs `#bestPlayersBody`, `#mostActivePlayersBody`, `#championsGrid`, `#championRoleButtons`, `#championFilterInput`
- Produces:
  - Responsive listing page shells with no API contract changes
  - Optional helper `renderChampionListMeta(count, selectedRole, query): void` retained with updated copy layout

- [ ] **Step 1: Tighten the players listing hero and table framing in `players.html`**

```html
<main class="page page--listing">
    <section class="hero page-hero page-hero--compact">
        <span class="page-hero__eyebrow">Scouting board</span>
        <h1 class="hero__title">Player leaderboards</h1>
        <p class="hero__text">
            Compare the strongest local performers, identify stable accounts, and jump into full player dossiers.
        </p>
    </section>

    <div class="leaderboard-grid leaderboard-grid--balanced">
        <!-- existing two leaderboard sections remain -->
    </div>
</main>
```

- [ ] **Step 2: Reframe the champions filter/header structure in `champions.html`**

```html
<div class="champion-toolbar">
    <p class="section-copy champion-toolbar__meta" id="championListMeta">Loading champion pool...</p>
    <input class="filter-input champion-filter" id="championFilterInput" type="text" placeholder="Filter champions by name...">
</div>
<div class="champion-role-buttons" id="championRoleButtons" aria-label="Role sorting">
    <!-- existing role buttons remain -->
</div>
<div class="champion-grid" id="championsGrid"></div>
```

- [ ] **Step 3: Update listing-page CSS for balanced density and responsive controls**

```css
.leaderboard-grid,
.champion-toolbar {
    display: grid;
    gap: 14px;
}

.champion-toolbar {
    grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
    align-items: center;
    margin-bottom: 14px;
}

.champion-role-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.champion-card {
    border: 1px solid var(--color-border-default);
    background: rgba(27, 20, 48, 0.92);
}

@media (max-width: 760px) {
    .champion-toolbar {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 4: Keep champions JS behavior but make the role meta copy match the new UI**

```js
function renderChampionListMeta(count, selectedRole, query) {
    const meta = document.getElementById('championListMeta');

    if (!meta) {
        return;
    }

    const roleLabel = !selectedRole || selectedRole === 'default'
        ? 'all roles'
        : `${formatRoleLabel(selectedRole)} priority`;
    const queryLabel = query ? ` for "${query}"` : '';

    meta.textContent = `${formatNumber(count)} champions visible across ${roleLabel}${queryLabel}.`;
}
```

- [ ] **Step 5: Verify both listing pages across responsive widths**

Run:

```bash
.\mvnw.cmd spring-boot:run
```

Expected:

```text
Players and champions pages show cleaner hero sections, better-spaced filters, readable tables and cards, and no cramped controls on tablet or mobile widths.
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/players.html src/main/resources/static/champions.html src/main/resources/static/css/components.css src/main/resources/static/css/champion.css src/main/resources/static/js/champions.js
git commit -m "feat: redesign listing pages for players and champions"
```

## Task 4: Redesign the player, champion, and match detail pages

**Files:**
- Modify: `src/main/resources/static/player.html`
- Modify: `src/main/resources/static/champion.html`
- Modify: `src/main/resources/static/match.html`
- Modify: `src/main/resources/static/css/player.css`
- Modify: `src/main/resources/static/css/champion.css`
- Modify: `src/main/resources/static/css/recommendations.css`
- Modify: `src/main/resources/static/css/match-details.css`
- Test: manual browser verification on `player.html`, `champion.html`, and `match.html`

**Interfaces:**
- Consumes:
  - `renderPlayerHero(summary): void`
  - `renderPlayerStats(summary): void`
  - `renderPlayerMatches(matches): void`
  - `renderChampionHero(champion): void`
  - `renderChampionStats(summary): void`
  - `window.MatchDetailsView.renderInto(panel, details): void`
- Produces:
  - Detail-page responsive layouts with the same DOM IDs used by existing JS
  - Stronger section ordering and calmer dense-data surfaces

- [ ] **Step 1: Tighten the static detail-page shells in `player.html`, `champion.html`, and `match.html`**

```html
<main class="page page--detail">
    <section class="hero hero--detail" id="playerHero"></section>
    <section class="stats-grid stats-grid--detail" id="playerStats"></section>
    <!-- existing overview / tabs / content remain -->
</main>
```

```html
<main class="page page--detail">
    <section class="champion-hero champion-hero--detail" id="championHero"></section>
    <section class="stats-grid stats-grid--detail" id="championStats"></section>
    <!-- existing abilities and item statistics remain -->
</main>
```

```html
<main class="page page--detail">
    <section class="hero page-hero page-hero--compact">
        <span class="page-hero__eyebrow">Fallback view</span>
        <h1 class="hero__title">Match details</h1>
        <p class="hero__text">Player pages open match details inline. This page remains available as a direct fallback.</p>
    </section>
    <div class="match-page-content" id="matchPageContent"></div>
</main>
```

- [ ] **Step 2: Rebuild player detail layout for single-column mobile flow in `player.css`**

```css
.player-overview {
    display: grid;
    grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
    gap: 18px;
}

.player-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    padding: 8px;
    border-radius: var(--radius-pill);
    background: rgba(27, 20, 48, 0.92);
    border: 1px solid var(--color-border-default);
}

@media (max-width: 900px) {
    .player-overview {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 3: Rebuild champion detail modules and ability/item surfaces in `champion.css`**

```css
.champion-hero,
.abilities,
.ability-card,
.item-cell {
    color: var(--color-text-primary);
}

.abilities {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.ability-card,
.champion-hero__summary-card {
    background: rgba(27, 20, 48, 0.92);
    border: 1px solid var(--color-border-default);
}

@media (max-width: 720px) {
    .abilities {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 4: Calm dense recommendations and match details in `recommendations.css` and `match-details.css`**

```css
.insight-card,
.match-details-panel,
.match-team-card,
.timeline-event-card {
    background: rgba(27, 20, 48, 0.9);
    border: 1px solid var(--color-border-subtle);
}

.insight-card__title,
.match-section-title {
    color: var(--color-text-primary);
}

.insight-card__meta,
.match-section-copy {
    color: var(--color-text-secondary);
}
```

- [ ] **Step 5: Verify all detail pages with real data and narrow widths**

Run:

```bash
.\mvnw.cmd spring-boot:run
```

Expected:

```text
Player, champion, and fallback match pages keep existing data behavior while reading in a clearer top-to-bottom hierarchy, with no squeezed side-by-side panels on mobile.
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/player.html src/main/resources/static/champion.html src/main/resources/static/match.html src/main/resources/static/css/player.css src/main/resources/static/css/champion.css src/main/resources/static/css/recommendations.css src/main/resources/static/css/match-details.css
git commit -m "feat: redesign detail pages for player champion and match views"
```

## Task 5: Final responsive polish and cross-page verification

**Files:**
- Modify: `src/main/resources/static/css/layout.css`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/static/css/player.css`
- Modify: `src/main/resources/static/css/champion.css`
- Modify: `src/main/resources/static/css/recommendations.css`
- Modify: `src/main/resources/static/css/match-details.css`
- Test: manual verification across all target pages and viewports

**Interfaces:**
- Consumes: All updated shared/page-specific CSS from Tasks 1-4
- Produces: Final responsive tuning for gutters, overflow, tap targets, table spacing, and state consistency

- [ ] **Step 1: Tune breakpoint-specific spacing, overflow, and tap targets**

```css
@media (max-width: 640px) {
    .table th,
    .table td {
        padding: 12px 10px;
    }

    .button,
    .player-tabs__button,
    .role-sort-button,
    .site-nav__link {
        min-height: 44px;
    }

    .hero__title {
        font-size: clamp(30px, 10vw, 42px);
    }
}
```

- [ ] **Step 2: Normalize empty, loading, and error states across pages**

```css
.error-box,
.empty-box {
    padding: 16px;
    border-radius: var(--radius-lg);
    background: rgba(27, 20, 48, 0.92);
    border: 1px solid var(--color-border-default);
    color: var(--color-text-secondary);
}

.error-box {
    border-color: rgba(255, 96, 132, 0.32);
    color: #ffd7df;
}
```

- [ ] **Step 3: Run full manual verification across required pages**

Run:

```bash
.\mvnw.cmd spring-boot:run
```

Expected:

```text
Home, players, player detail, champions, champion detail, and match fallback all reflect the purple/cream identity, preserve data behavior, and remain readable on mobile, tablet, and desktop widths.
```

- [ ] **Step 4: Capture final diff review**

Run:

```bash
git diff -- src/main/resources/static/index.html src/main/resources/static/players.html src/main/resources/static/player.html src/main/resources/static/champions.html src/main/resources/static/champion.html src/main/resources/static/match.html src/main/resources/static/css/base.css src/main/resources/static/css/layout.css src/main/resources/static/css/components.css src/main/resources/static/css/player.css src/main/resources/static/css/champion.css src/main/resources/static/css/recommendations.css src/main/resources/static/css/match-details.css src/main/resources/static/js/home.js src/main/resources/static/js/champions.js
```

Expected:

```text
Only the planned frontend redesign files are changed, with no unrelated backend or API modifications.
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html src/main/resources/static/players.html src/main/resources/static/player.html src/main/resources/static/champions.html src/main/resources/static/champion.html src/main/resources/static/match.html src/main/resources/static/css/base.css src/main/resources/static/css/layout.css src/main/resources/static/css/components.css src/main/resources/static/css/player.css src/main/resources/static/css/champion.css src/main/resources/static/css/recommendations.css src/main/resources/static/css/match-details.css src/main/resources/static/js/home.js src/main/resources/static/js/champions.js
git commit -m "feat: finalize responsive riot stats redesign"
```
