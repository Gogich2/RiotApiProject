# Editorial Reset Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Riot Stats static frontend into an editorial homepage plus utilitarian research pages that feel like a new website.

**Architecture:** Keep the Spring-served static HTML/CSS/JS stack, but treat the existing frontend markup as replaceable where the new information architecture requires it. Reuse the current data endpoints and most loader behavior, while rebuilding the shared shell, homepage sequencing, listing page scan rhythm, and dossier page structure around the approved editorial reset design.

**Tech Stack:** Static HTML, CSS, vanilla JavaScript, Spring Boot static resources, Maven wrapper for local app startup, Node for JavaScript syntax checks

## Global Constraints

- editorial homepage
- patch-and-meta-led narrative
- premium black visual system
- full visual reset
- utilitarian internal research pages
- conservative mobile density
- preserve touch-safe targets
- visible keyboard focus states
- strong text contrast
- reduced-motion-respecting transitions
- preserve existing page routing
- preserve existing API usage where practical
- preserve existing search and detail loading logic where practical
- allow DOM hook changes if the new markup requires them
- allow broader HTML restructuring where needed to support the new site architecture
- no backend redesign
- no frontend framework migration
- no major new product features unrelated to the approved site structure
- existing backend baseline failure in `org.main.cucumber.CucumberTest` is out of scope; verification for this plan is frontend route, rendering, and interaction focused

---

## File Structure

### Shared shell

- Modify: `src/main/resources/static/css/base.css`
  - Replace the current token set with the premium-black editorial palette, typography tokens, motion tokens, and border/surface rules.
- Modify: `src/main/resources/static/css/layout.css`
  - Rebuild the sticky header, shared page frame, editorial homepage layout, internal tool-page layout, and responsive breakpoints.
- Modify: `src/main/resources/static/css/components.css`
  - Rebuild shared buttons, nav links, tables, cards, rails, stat strips, section dividers, search surfaces, and state components.
- Modify: `src/main/resources/static/css/main.css`
  - Keep import order explicit if any new shared file ordering is needed; otherwise leave as the shared entrypoint only.

### Homepage

- Modify: `src/main/resources/static/index.html`
  - Replace the dashboard-first homepage structure with issue-cover sections and stable hook containers for the editorial modules.
- Modify: `src/main/resources/static/js/home.js`
  - Keep current overview and leaderboard fetch behavior, but rewrite render output for the new issue-cover sections.

### Listings

- Modify: `src/main/resources/static/players.html`
  - Convert the page into a scouting board with compact research tables and a small context header.
- Modify: `src/main/resources/static/js/players.js`
  - Keep leaderboard loading, but render denser rows and a compact board summary.
- Modify: `src/main/resources/static/champions.html`
  - Convert the page into a role-aware research directory with a tighter filter/control frame.
- Modify: `src/main/resources/static/js/champions.js`
  - Keep champion loading and sort/filter behavior, but rewrite the grid card markup and directory summary output.
- Modify: `src/main/resources/static/css/champion.css`
  - Own champion listing and champion dossier visual rules that do not belong in shared components.

### Dossiers and match fallback

- Modify: `src/main/resources/static/player.html`
  - Replace the current detail layout with a dossier structure: identity, summary, evidence, and supporting sections.
- Modify: `src/main/resources/static/js/player.js`
  - Keep the current data loading graph and tab behavior, but rewrite hero, stats, recommendations, and match-card output for the new dossier layout.
- Modify: `src/main/resources/static/champion.html`
  - Replace the current detail layout with a champion dossier structure.
- Modify: `src/main/resources/static/js/champion.js`
  - Keep champion, item, and ability loading, but rewrite output for the new dossier layout.
- Modify: `src/main/resources/static/match.html`
  - Restyle the standalone fallback page so it reads like supporting evidence, not a leftover shell.
- Modify: `src/main/resources/static/css/player.css`
  - Own dossier layout, match-card layout, rank modules, and tab framing.
- Modify: `src/main/resources/static/css/recommendations.css`
  - Align recommendation cards with the dossier system.
- Modify: `src/main/resources/static/css/match-details.css`
  - Align inline and standalone match details with the new evidence styling.

### Verification

- Verify syntax:
  - `src/main/resources/static/js/home.js`
  - `src/main/resources/static/js/players.js`
  - `src/main/resources/static/js/champions.js`
  - `src/main/resources/static/js/player.js`
  - `src/main/resources/static/js/champion.js`
- Verify pages:
  - `index.html`
  - `players.html`
  - `champions.html`
  - live `player.html`
  - live `champion.html`
  - `match.html`

## Task 1: Rebuild the shared editorial shell

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

**Interfaces:**
- Consumes: Existing route names, existing script includes, existing shared classes `.site-header`, `.site-nav`, `.search`, `.page`, `.button`, `.table`, `.error-box`, `.empty-box`
- Produces:
  - shared shell classes `.site-header__inner`, `.site-brand`, `.site-search`, `.page-shell`, `.page-shell--editorial`, `.page-shell--tool`
  - shared layout classes `.section-band`, `.section-divider`, `.section-kicker`, `.metric-strip`, `.data-panel`, `.rail-card`
  - stable page root classes for later tasks: `.home-cover`, `.players-board`, `.champions-board`, `.player-dossier`, `.champion-dossier`, `.match-fallback`

- [ ] **Step 1: Replace the shared header and page root markup across all HTML entry pages**

Use this shell pattern in each page, adjusting `aria-current` and the `<main>` modifier per route:

```html
<header class="site-header">
    <div class="site-header__inner">
        <a class="site-brand" href="index.html">
            <span class="site-brand__mark">Riot Stats</span>
            <span class="site-brand__edition">Meta report</span>
        </a>

        <nav class="site-nav" aria-label="Primary">
            <a class="site-nav__link" href="index.html" aria-current="page">Home</a>
            <a class="site-nav__link" href="players.html">Players</a>
            <a class="site-nav__link" href="champions.html">Champions</a>
        </nav>

        <div class="site-search">
            <label class="site-search__label" for="globalSearchInput">Search</label>
            <div class="search">
                <input class="search__input" id="globalSearchInput" type="text" placeholder="Search player or champion...">
                <div class="search__results" id="globalSearchResults"></div>
            </div>
        </div>
    </div>
</header>

<main class="page-shell page-shell--tool players-board">
    <!-- page-specific content -->
</main>
```

Apply page root classes like this:

```html
<main class="page-shell page-shell--editorial home-cover">
```

```html
<main class="page-shell page-shell--tool champion-dossier">
```

- [ ] **Step 2: Replace the token system in `base.css` with the editorial reset foundation**

Add or replace the `:root` block with:

```css
:root {
    --font-family-base: Inter, "Segoe UI", Arial, sans-serif;
    --font-family-display: Inter, "Segoe UI", Arial, sans-serif;

    --color-bg-app: #05070b;
    --color-bg-elevated: #0b0e14;
    --color-bg-surface: #10141c;
    --color-bg-surface-2: #151a24;
    --color-bg-surface-3: #1b2230;

    --color-text-primary: #f5f2ea;
    --color-text-secondary: rgba(245, 242, 234, 0.76);
    --color-text-muted: rgba(245, 242, 234, 0.52);
    --color-text-disabled: rgba(245, 242, 234, 0.34);

    --color-border-subtle: rgba(245, 242, 234, 0.08);
    --color-border-default: rgba(245, 242, 234, 0.12);
    --color-border-strong: rgba(245, 242, 234, 0.2);

    --color-accent: #8e6cff;
    --color-accent-strong: #a88dff;
    --color-accent-soft: rgba(142, 108, 255, 0.14);
    --color-accent-border: rgba(142, 108, 255, 0.34);

    --shadow-panel: 0 18px 40px rgba(0, 0, 0, 0.24);
    --shadow-overlay: 0 28px 60px rgba(0, 0, 0, 0.36);

    --radius-panel: 8px;
    --radius-control: 6px;
    --radius-pill: 999px;

    --space-2: 8px;
    --space-3: 12px;
    --space-4: 16px;
    --space-5: 20px;
    --space-6: 24px;
    --space-8: 32px;
    --space-10: 40px;
    --space-12: 48px;

    --transition-fast: 160ms ease;
    --transition-base: 220ms ease;
}
```

Update `body` to the new surface model:

```css
body {
    margin: 0;
    min-height: 100vh;
    font-family: var(--font-family-base);
    background:
        radial-gradient(circle at top center, rgba(142, 108, 255, 0.08), transparent 24%),
        linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 18%),
        var(--color-bg-app);
    color: var(--color-text-primary);
}
```

- [ ] **Step 3: Rewrite the shared frame rules in `layout.css`**

Add the new shell and responsive layout rules:

```css
.site-header {
    position: sticky;
    top: 0;
    z-index: 40;
    border-bottom: 1px solid var(--color-border-subtle);
    backdrop-filter: blur(18px);
    background: rgba(5, 7, 11, 0.82);
}

.site-header__inner,
.page-shell {
    width: min(1280px, calc(100% - 40px));
    margin: 0 auto;
}

.site-header__inner {
    display: grid;
    grid-template-columns: auto auto minmax(260px, 360px);
    align-items: center;
    gap: var(--space-5);
    min-height: 76px;
}

.page-shell {
    padding: var(--space-8) 0 var(--space-12);
}

.page-shell--editorial {
    display: grid;
    gap: var(--space-10);
}

.page-shell--tool {
    display: grid;
    gap: var(--space-8);
}

@media (max-width: 960px) {
    .site-header__inner {
        grid-template-columns: 1fr auto;
        padding: var(--space-3) 0;
    }

    .site-search {
        grid-column: 1 / -1;
    }
}

@media (max-width: 640px) {
    .site-header__inner,
    .page-shell {
        width: min(100% - 24px, 1280px);
    }
}
```

- [ ] **Step 4: Replace shared component primitives in `components.css`**

Add the new nav, control, panel, and table primitives:

```css
.site-nav__link,
.button,
.role-sort-button,
.player-tabs__button {
    transition:
        color var(--transition-fast),
        border-color var(--transition-fast),
        background-color var(--transition-fast),
        transform var(--transition-fast);
}

.button:active,
.role-sort-button:active,
.player-tabs__button:active,
.site-nav__link:active {
    transform: scale(0.98);
}

.data-panel,
.rail-card,
.table-wrapper {
    border: 1px solid var(--color-border-default);
    background: var(--color-bg-surface);
    box-shadow: var(--shadow-panel);
}

.table thead th {
    color: var(--color-text-secondary);
    font-size: 0.75rem;
    letter-spacing: 0.08em;
    text-transform: uppercase;
}

.table tbody tr:hover {
    background: rgba(255, 255, 255, 0.02);
}
```

- [ ] **Step 5: Run a route shell smoke test before moving to page-specific redesign tasks**

Run:

```powershell
@(
  'http://localhost:8081/index.html',
  'http://localhost:8081/players.html',
  'http://localhost:8081/champions.html',
  'http://localhost:8081/player.html?puuid=test',
  'http://localhost:8081/champion.html?id=1',
  'http://localhost:8081/match.html'
) | ForEach-Object {
  try {
    $response = Invoke-WebRequest -UseBasicParsing $_
    '{0} -> {1}' -f $_, [int]$response.StatusCode
  } catch {
    '{0} -> FAIL' -f $_
  }
}
```

Expected:

```text
http://localhost:8081/index.html -> 200
http://localhost:8081/players.html -> 200
http://localhost:8081/champions.html -> 200
http://localhost:8081/player.html?puuid=test -> 200
http://localhost:8081/champion.html?id=1 -> 200
http://localhost:8081/match.html -> 200
```

- [ ] **Step 6: Commit the shared shell reset**

```bash
git add src/main/resources/static/css/base.css src/main/resources/static/css/layout.css src/main/resources/static/css/components.css src/main/resources/static/index.html src/main/resources/static/players.html src/main/resources/static/player.html src/main/resources/static/champions.html src/main/resources/static/champion.html src/main/resources/static/match.html
git commit -m "feat: rebuild editorial site shell"
```

## Task 2: Turn the homepage into an issue cover

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/js/home.js`
- Modify: `src/main/resources/static/css/layout.css`
- Modify: `src/main/resources/static/css/components.css`

**Interfaces:**
- Consumes:
  - `api.getOverview(): Promise<Overview>`
  - `api.getPlayerLeaderboards(): Promise<Leaderboards>`
  - existing helpers `formatNumber`, `formatPercent`
- Produces:
  - homepage hook containers `#issueContextRail`, `#metaMovers`, `#standoutBoard`, `#patchSnapshot`, `#priorityReads`
  - render functions `renderIssueContext(overview, leaderboards)`, `renderMetaMovers(overview)`, `renderStandoutBoard(leaderboards)`, `renderPatchSnapshot(overview)`, `renderPriorityReads(overview, leaderboards)`

- [ ] **Step 1: Replace `index.html` with issue-cover section hooks**

Use this page body structure:

```html
<main class="page-shell page-shell--editorial home-cover">
    <section class="issue-hero">
        <div class="issue-hero__lead" id="issueLeadStory">
            <span class="section-kicker">Current issue</span>
            <h1 class="issue-hero__title">Loading the latest meta read...</h1>
            <p class="issue-hero__summary">The homepage lead story will render once the overview data arrives.</p>
            <div class="issue-hero__actions">
                <a class="button" href="players.html">Open player scouting</a>
                <a class="button button--secondary" href="champions.html">Browse champions</a>
            </div>
        </div>

        <aside class="issue-hero__rail" id="issueContextRail"></aside>
    </section>

    <section class="issue-grid">
        <article class="section-band" id="metaMovers"></article>
        <article class="section-band" id="standoutBoard"></article>
    </section>

    <section class="issue-grid issue-grid--supporting">
        <article class="section-band" id="patchSnapshot"></article>
        <article class="section-band" id="priorityReads"></article>
    </section>

    <section class="issue-evidence">
        <article class="data-panel">
            <div class="section-divider">
                <span class="section-kicker">Volume</span>
                <h2>Most played champions</h2>
            </div>
            <div class="table-wrapper">
                <table class="table">
                    <thead>
                    <tr><th>Champion</th><th>Games</th><th>Wins</th><th>Win rate</th></tr>
                    </thead>
                    <tbody id="popularChampionsBody"></tbody>
                </table>
            </div>
        </article>

        <article class="data-panel">
            <div class="section-divider">
                <span class="section-kicker">Efficiency</span>
                <h2>Highest win rates</h2>
            </div>
            <div class="table-wrapper">
                <table class="table">
                    <thead>
                    <tr><th>Champion</th><th>Games</th><th>Wins</th><th>Win rate</th></tr>
                    </thead>
                    <tbody id="bestChampionsBody"></tbody>
                </table>
            </div>
        </article>
    </section>
</main>
```

- [ ] **Step 2: Add homepage-specific editorial layout rules**

Add these rules to shared CSS:

```css
.issue-hero {
    display: grid;
    grid-template-columns: minmax(0, 1.5fr) minmax(280px, 0.8fr);
    gap: var(--space-8);
    align-items: start;
}

.issue-grid {
    display: grid;
    grid-template-columns: 1.15fr 0.85fr;
    gap: var(--space-6);
}

.issue-evidence {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--space-6);
}

@media (max-width: 960px) {
    .issue-hero,
    .issue-grid,
    .issue-evidence {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 3: Rewrite `home.js` to render the new issue-cover modules**

Replace the top-level `DOMContentLoaded` body with:

```javascript
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const [overview, leaderboards] = await Promise.all([
            api.getOverview(),
            api.getPlayerLeaderboards().catch(() => null)
        ]);

        renderIssueContext(overview, leaderboards);
        renderMetaMovers(overview);
        renderStandoutBoard(leaderboards);
        renderPatchSnapshot(overview);
        renderPriorityReads(overview, leaderboards);
        renderChampionTable('popularChampionsBody', overview.mostPopularChampions || []);
        renderChampionTable('bestChampionsBody', overview.bestWinrateChampions || []);
    } catch (error) {
        console.error('Could not load homepage issue data:', error);
        renderIssueErrorState();
    }
});
```

Add the new renderers:

```javascript
function renderIssueContext(overview, leaderboards) {
    const rail = document.getElementById('issueContextRail');
    const lead = document.getElementById('issueLeadStory');
    const bestChampion = overview?.bestWinrateChampions?.[0];
    const bestPlayer = leaderboards?.bestPlayers?.[0];

    if (lead) {
        lead.innerHTML = `
            <span class="section-kicker">Current issue</span>
            <h1 class="issue-hero__title">${escapeHtml(bestChampion?.championName || 'Meta report')}</h1>
            <p class="issue-hero__summary">
                ${escapeHtml(buildLeadSummary(overview, bestChampion, bestPlayer))}
            </p>
            <div class="issue-hero__actions">
                <a class="button" href="players.html">Open player scouting</a>
                <a class="button button--secondary" href="champions.html">Browse champions</a>
            </div>
        `;
    }

    if (rail) {
        rail.innerHTML = `
            <article class="rail-card">
                <span class="section-kicker">Dataset</span>
                <strong>${formatNumber(overview?.totalMatches || 0)} matches</strong>
                <p>${formatNumber(overview?.totalPlayers || 0)} tracked players in the current local sample.</p>
            </article>
            <article class="rail-card">
                <span class="section-kicker">Watchlist</span>
                <strong>${escapeHtml(getPlayerDisplayName(bestPlayer))}</strong>
                <p>${bestPlayer ? `${formatPercent(bestPlayer.winrate)} win rate across ${formatNumber(bestPlayer.matches)} matches.` : 'Leaderboard data is unavailable right now.'}</p>
            </article>
        `;
    }
}
```

```javascript
function renderMetaMovers(overview) {
    const container = document.getElementById('metaMovers');
    const popular = overview?.mostPopularChampions?.slice(0, 3) || [];

    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="section-divider">
            <span class="section-kicker">Meta movers</span>
            <h2>Priority champions in the current sample</h2>
        </div>
        <div class="metric-strip">
            ${popular.map(champion => `
                <a class="metric-strip__item" href="champion.html?id=${encodeURIComponent(champion.championId)}">
                    <span>${escapeHtml(champion.championName || 'Unknown')}</span>
                    <strong>${formatPercent(champion.winrate)}</strong>
                </a>
            `).join('')}
        </div>
    `;
}
```

Add matching helpers:

```javascript
function buildLeadSummary(overview, bestChampion, bestPlayer) {
    const championName = bestChampion?.championName || 'the current champion pool';
    const playerName = bestPlayer ? getPlayerDisplayName(bestPlayer) : 'the player board';

    return `${championName} is setting the strongest tone in a sample of ${formatNumber(overview?.totalMatches || 0)} ranked matches, while ${playerName} leads the current scouting view.`;
}

function renderIssueErrorState() {
    const lead = document.getElementById('issueLeadStory');
    const rail = document.getElementById('issueContextRail');

    if (lead) {
        lead.innerHTML = `<div class="error-box">Could not load the current issue.</div>`;
    }

    if (rail) {
        rail.innerHTML = `<div class="error-box">Could not load issue context.</div>`;
    }
}
```

- [ ] **Step 4: Run a syntax check on the rewritten homepage script**

Run:

```powershell
node --check src/main/resources/static/js/home.js
```

Expected:

```text
[no output]
```

- [ ] **Step 5: Verify the homepage renders the new issue-cover shape**

Run:

```powershell
$response = Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/index.html'
$response.StatusCode
$response.Content -match 'issueLeadStory'
$response.Content -match 'metaMovers'
$response.Content -match 'patchSnapshot'
```

Expected:

```text
200
True
True
True
```

- [ ] **Step 6: Commit the homepage reset**

```bash
git add src/main/resources/static/index.html src/main/resources/static/js/home.js src/main/resources/static/css/layout.css src/main/resources/static/css/components.css
git commit -m "feat: turn homepage into editorial issue cover"
```

## Task 3: Rebuild the research listing pages

**Files:**
- Modify: `src/main/resources/static/players.html`
- Modify: `src/main/resources/static/js/players.js`
- Modify: `src/main/resources/static/champions.html`
- Modify: `src/main/resources/static/js/champions.js`
- Modify: `src/main/resources/static/css/champion.css`
- Modify: `src/main/resources/static/css/components.css`

**Interfaces:**
- Consumes:
  - `api.getPlayerLeaderboards(): Promise<Leaderboards>`
  - `api.getChampions(): Promise<ChampionSummary[]>`
  - existing helper functions `formatNumber`, `formatPercent`, `escapeHtml`, `formatChampionDisplayName`
- Produces:
  - players board hook `#playersBoardSummary`
  - champions board hook `#championDirectorySummary`
  - render functions `renderPlayersBoardSummary(leaderboards)`, `renderChampionDirectorySummary(count, selectedRole, query)`

- [ ] **Step 1: Restructure `players.html` into a scouting board**

Replace the current page body with:

```html
<main class="page-shell page-shell--tool players-board">
    <section class="tool-hero">
        <div>
            <span class="section-kicker">Scouting board</span>
            <h1>Player intelligence</h1>
            <p>Rank, compare, and open dossiers for the most important accounts in the local sample.</p>
        </div>
        <aside class="rail-card" id="playersBoardSummary"></aside>
    </section>

    <section class="tool-grid">
        <article class="data-panel">
            <div class="section-divider">
                <span class="section-kicker">Efficiency</span>
                <h2>Best players</h2>
            </div>
            <div class="table-wrapper">
                <table class="table">
                    <thead>
                    <tr><th>Player</th><th>Matches</th><th>Wins</th><th>Win rate</th><th>KDA</th></tr>
                    </thead>
                    <tbody id="bestPlayersBody"></tbody>
                </table>
            </div>
        </article>

        <article class="data-panel">
            <div class="section-divider">
                <span class="section-kicker">Volume</span>
                <h2>Most active players</h2>
            </div>
            <div class="table-wrapper">
                <table class="table">
                    <thead>
                    <tr><th>Player</th><th>Matches</th><th>Wins</th><th>Win rate</th><th>KDA</th></tr>
                    </thead>
                    <tbody id="mostActivePlayersBody"></tbody>
                </table>
            </div>
        </article>
    </section>
</main>
```

- [ ] **Step 2: Rewrite `players.js` for denser scouting rows and board summary**

Use this load flow:

```javascript
document.addEventListener('DOMContentLoaded', async () => {
    try {
        const leaderboards = await api.getPlayerLeaderboards();
        renderPlayersBoardSummary(leaderboards);
        renderLeaderboardTable('bestPlayersBody', leaderboards.bestPlayers || [], 'winrate');
        renderLeaderboardTable('mostActivePlayersBody', leaderboards.mostActivePlayers || [], 'activity');
    } catch (error) {
        renderPlayersBoardSummary(null);
        renderLeaderboardError('bestPlayersBody', 'Could not load best players.');
        renderLeaderboardError('mostActivePlayersBody', 'Could not load active players.');
    }
});
```

Render the summary rail:

```javascript
function renderPlayersBoardSummary(leaderboards) {
    const container = document.getElementById('playersBoardSummary');
    const best = leaderboards?.bestPlayers?.[0];
    const active = leaderboards?.mostActivePlayers?.[0];

    if (!container) {
        return;
    }

    container.innerHTML = `
        <span class="section-kicker">Board context</span>
        <strong>${escapeHtml(getPlayerDisplayName(best || active))}</strong>
        <p>${best ? `${formatPercent(best.winrate)} win rate leader` : 'Leaderboard data unavailable'}${active ? ` with ${formatNumber(active.matches)} matches on the activity board.` : '.'}</p>
    `;
}
```

- [ ] **Step 3: Restructure `champions.html` into a research directory**

Use this content structure:

```html
<main class="page-shell page-shell--tool champions-board">
    <section class="tool-hero">
        <div>
            <span class="section-kicker">Meta browser</span>
            <h1>Champion research</h1>
            <p>Filter the pool by role, compare usage and efficiency, and open individual dossiers.</p>
        </div>
        <aside class="rail-card" id="championDirectorySummary"></aside>
    </section>

    <section class="data-panel">
        <div class="section-divider">
            <div>
                <span class="section-kicker">Directory</span>
                <h2>Champion list</h2>
            </div>
            <div class="champion-role-buttons" id="championRoleButtons" aria-label="Role sorting"></div>
        </div>
        <div class="directory-toolbar">
            <p class="section-meta" id="championListMeta">Loading champion pool...</p>
            <input class="filter-input champion-filter" id="championFilterInput" type="text" placeholder="Filter champions by name...">
        </div>
        <div class="champion-grid" id="championsGrid"></div>
    </section>
</main>
```

- [ ] **Step 4: Rewrite `champions.js` to render research cards and directory summary**

Keep filtering and role-button behavior, but replace the render output:

```javascript
function renderChampions(champions) {
    const container = document.getElementById('championsGrid');

    if (!container) {
        return;
    }

    if (!champions || champions.length === 0) {
        container.innerHTML = `<div class="empty-box">No champions found.</div>`;
        return;
    }

    container.innerHTML = champions.map(champion => `
        <a class="champion-research-card" href="champion.html?id=${encodeURIComponent(champion.championId)}">
            <div class="champion-research-card__identity">
                ${champion.imageUrl ? `<img class="champion-research-card__image" src="${escapeHtml(champion.imageUrl)}" alt="${escapeHtml(formatChampionDisplayName(champion.championName))}" onerror="this.onerror=null; this.remove();">` : ''}
                <div>
                    <strong>${escapeHtml(formatChampionDisplayName(champion.championName))}</strong>
                    <span>${escapeHtml(formatRoleLabel(champion.primaryRole) || 'Unassigned')}</span>
                </div>
            </div>
            <div class="champion-research-card__metrics">
                <span><label>Games</label><strong>${formatNumber(champion.games)}</strong></span>
                <span><label>Wins</label><strong>${formatNumber(champion.wins)}</strong></span>
                <span><label>Win rate</label><strong>${formatPercent(champion.winrate)}</strong></span>
            </div>
        </a>
    `).join('');
}
```

Add the board summary:

```javascript
function renderChampionDirectorySummary(count, selectedRole, query) {
    const container = document.getElementById('championDirectorySummary');

    if (!container) {
        return;
    }

    const roleLabel = !selectedRole || selectedRole === 'default'
        ? 'All roles'
        : formatRoleLabel(selectedRole);
    const queryText = query ? `Filtered by "${query}".` : 'No active name filter.';

    container.innerHTML = `
        <span class="section-kicker">Directory context</span>
        <strong>${formatNumber(count)} champions</strong>
        <p>${escapeHtml(roleLabel)} priority. ${escapeHtml(queryText)}</p>
    `;
}
```

Call it from `renderVisibleChampions(...)` after `renderChampionListMeta(...)`.

- [ ] **Step 5: Add the listing-page visual rules**

Add to `components.css` and `champion.css`:

```css
.tool-hero,
.tool-grid {
    display: grid;
    gap: var(--space-6);
}

.tool-grid {
    grid-template-columns: 1fr 1fr;
}

.champion-research-card {
    display: grid;
    gap: var(--space-4);
    padding: var(--space-4);
    border: 1px solid var(--color-border-default);
    background: var(--color-bg-surface);
    text-decoration: none;
    color: inherit;
}

@media (max-width: 960px) {
    .tool-grid {
        grid-template-columns: 1fr;
    }
}
```

- [ ] **Step 6: Run syntax checks for listing scripts**

Run:

```powershell
node --check src/main/resources/static/js/players.js
node --check src/main/resources/static/js/champions.js
```

Expected:

```text
[no output]
[no output]
```

- [ ] **Step 7: Verify the listing pages**

Run:

```powershell
$players = Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/players.html'
$champions = Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/champions.html'
[int]$players.StatusCode
[int]$champions.StatusCode
$players.Content -match 'playersBoardSummary'
$champions.Content -match 'championDirectorySummary'
```

Expected:

```text
200
200
True
True
```

- [ ] **Step 8: Commit the listing page reset**

```bash
git add src/main/resources/static/players.html src/main/resources/static/js/players.js src/main/resources/static/champions.html src/main/resources/static/js/champions.js src/main/resources/static/css/champion.css src/main/resources/static/css/components.css
git commit -m "feat: rebuild research listing pages"
```

## Task 4: Rebuild the dossier pages and match fallback

**Files:**
- Modify: `src/main/resources/static/player.html`
- Modify: `src/main/resources/static/js/player.js`
- Modify: `src/main/resources/static/champion.html`
- Modify: `src/main/resources/static/js/champion.js`
- Modify: `src/main/resources/static/match.html`
- Modify: `src/main/resources/static/css/player.css`
- Modify: `src/main/resources/static/css/recommendations.css`
- Modify: `src/main/resources/static/css/match-details.css`
- Modify: `src/main/resources/static/css/champion.css`

**Interfaces:**
- Consumes:
  - `api.getPlayerSummary(puuid)`, `api.getPlayerMatches(puuid, 20)`, `api.getPlayerChampions(puuid)`, `api.getPlayerRanks(puuid)`, `api.getPlayerRankHistory(puuid)`, `api.getPlayerInsights(puuid)`
  - `api.getChampion(id)`, `api.getChampionItems(id)`
  - `window.MatchDetailsView.renderInto(...)`
- Produces:
  - player dossier hooks `#playerHero`, `#playerStats`, `#playerEvidenceRail`, `#playerMatchesBody`, `#playerInsightsSummary`, `#playerInsights`
  - champion dossier hooks `#championHero`, `#championStats`, `#championAbilities`, `#championItemsBody`
  - player renderers `renderPlayerHero(summary)`, `renderPlayerStats(summary)`, `renderPlayerMatches(matches)`, `renderDetailedRecommendations(insights)`
  - champion renderers `renderChampionHero(champion)`, `renderChampionStats(summary)`, `renderChampionItems(items)`

- [ ] **Step 1: Replace `player.html` with dossier-first structure**

Use this page body:

```html
<main class="page-shell page-shell--tool player-dossier">
    <section class="player-dossier__hero" id="playerHero"></section>

    <section class="metric-strip metric-strip--cards" id="playerStats"></section>

    <div class="player-dossier__layout">
        <section class="player-dossier__main">
            <div class="player-tabs" id="playerTabs">
                <button class="player-tabs__button player-tabs__button--active" type="button" data-player-tab-button="overview">Overview</button>
                <button class="player-tabs__button" type="button" data-player-tab-button="champions">Champion Stats</button>
                <button class="player-tabs__button" type="button" data-player-tab-button="recommendations">Recommendations</button>
            </div>

            <section class="data-panel" data-player-tab-panel="overview">
                <div class="section-divider">
                    <span class="section-kicker">Recent evidence</span>
                    <h2>Recent matches</h2>
                </div>
                <div class="player-match-list" id="playerMatchesBody"></div>
            </section>

            <section class="data-panel" data-player-tab-panel="champions">
                <div class="section-divider">
                    <span class="section-kicker">Champion pool</span>
                    <h2>Champion statistics</h2>
                </div>
                <div class="player-champions" id="playerChampions"></div>
            </section>

            <section class="data-panel player-recommendations" data-player-tab-panel="recommendations">
                <div class="section-divider">
                    <span class="section-kicker">Recommendations</span>
                    <h2>Detailed recommendations</h2>
                </div>
                <div class="insights" id="playerInsights"></div>
            </section>
        </section>

        <aside class="player-dossier__side" id="playerEvidenceRail">
            <section class="data-panel">
                <div class="section-divider">
                    <span class="section-kicker">Ranked performance</span>
                    <h2>Ranks</h2>
                </div>
                <button class="button button--secondary" id="refreshRanksButton" type="button">Refresh rank</button>
                <div class="rank-grid" id="playerRanks"></div>
            </section>
            <section class="data-panel">
                <div class="section-divider">
                    <span class="section-kicker">Progression</span>
                    <h2>Rank history</h2>
                </div>
                <div class="rank-history-chart" id="playerRankChart"></div>
                <div class="rank-history" id="playerRankHistory"></div>
            </section>
            <section class="data-panel">
                <div class="section-divider">
                    <span class="section-kicker">Summary</span>
                    <h2>Recommendations</h2>
                </div>
                <div class="insights" id="playerInsightsSummary"></div>
            </section>
        </aside>
    </div>
</main>
```

- [ ] **Step 2: Update `player.js` to render a dossier hero and evidence-first match cards**

Keep the current `DOMContentLoaded` flow and tab logic, but replace the hero renderer with:

```javascript
function renderPlayerHero(summary) {
    const container = document.getElementById('playerHero');
    const displayName = getPlayerDisplayName(summary);

    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="dossier-identity">
            ${summary.profileIconUrl ? `<img class="dossier-identity__icon" src="${escapeHtml(summary.profileIconUrl)}" alt="${escapeHtml(displayName)}" onerror="this.onerror=null; this.remove();">` : ''}
            <div>
                <span class="section-kicker">Player dossier</span>
                <h1>${escapeHtml(displayName)}</h1>
                <p>${formatNumber(summary.matches)} matches, ${formatPercent(summary.winrate)} win rate, ${formatKda(summary)} average KDA.</p>
            </div>
        </div>
    `;
}
```

Replace the stat strip renderer with:

```javascript
function renderPlayerStats(summary) {
    const container = document.getElementById('playerStats');

    if (!container) {
        return;
    }

    container.innerHTML = `
        <article class="stat-chip"><span>Matches</span><strong>${formatNumber(summary.matches)}</strong></article>
        <article class="stat-chip"><span>Wins</span><strong>${formatNumber(summary.wins)}</strong></article>
        <article class="stat-chip"><span>Win rate</span><strong>${formatPercent(summary.winrate)}</strong></article>
        <article class="stat-chip"><span>KDA</span><strong>${formatKda(summary)}</strong></article>
    `;
}
```

- [ ] **Step 3: Replace player dossier CSS in `player.css`, `recommendations.css`, and `match-details.css`**

Add the dossier layout foundation:

```css
.player-dossier__layout {
    display: grid;
    grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
    gap: var(--space-6);
    align-items: start;
}

.player-dossier__side {
    display: grid;
    gap: var(--space-6);
}

.player-match-card {
    border: 1px solid var(--color-border-default);
    background: var(--color-bg-surface);
}

@media (max-width: 960px) {
    .player-dossier__layout {
        grid-template-columns: 1fr;
    }
}
```

Add recommendation alignment:

```css
.insight-card {
    border: 1px solid var(--color-border-default);
    background: var(--color-bg-surface);
}
```

Align match detail surfaces:

```css
.match-details,
.match-page-content {
    border: 1px solid var(--color-border-default);
    background: var(--color-bg-surface);
}
```

- [ ] **Step 4: Replace `champion.html` with a dossier structure**

Use this page body:

```html
<main class="page-shell page-shell--tool champion-dossier">
    <section class="champion-dossier__hero" id="championHero"></section>
    <section class="metric-strip metric-strip--cards" id="championStats"></section>

    <div class="champion-dossier__layout">
        <section class="data-panel">
            <div class="section-divider">
                <span class="section-kicker">Ability profile</span>
                <h2>Abilities</h2>
            </div>
            <div class="abilities" id="championAbilities"></div>
        </section>

        <section class="data-panel">
            <div class="section-divider">
                <span class="section-kicker">Item profile</span>
                <h2>Item statistics</h2>
            </div>
            <div class="table-wrapper">
                <table class="table">
                    <thead>
                    <tr><th>Item</th><th>Games</th><th>Wins</th><th>Win rate</th><th>Pick rate</th></tr>
                    </thead>
                    <tbody id="championItemsBody"></tbody>
                </table>
            </div>
        </section>
    </div>
</main>
```

- [ ] **Step 5: Rewrite `champion.js` hero output for the dossier system**

Keep the existing loading flow, but replace `renderChampionHero(...)` with:

```javascript
function renderChampionHero(champion) {
    const championName = formatChampionDisplayName(champion.championName);
    const summary = champion.summary || {};
    const container = document.getElementById('championHero');

    if (!container) {
        return;
    }

    container.innerHTML = `
        <div class="dossier-identity dossier-identity--champion">
            ${champion.imageUrl ? `<img class="dossier-identity__icon" src="${escapeHtml(champion.imageUrl)}" alt="${escapeHtml(championName)}">` : ''}
            <div>
                <span class="section-kicker">Champion dossier</span>
                <h1>${escapeHtml(championName)}</h1>
                <p>${escapeHtml(champion.title || '')}</p>
                <div class="metric-strip">
                    <span class="metric-strip__item">${escapeHtml(formatRoleLabel(champion.primaryRole) || 'Role pending')}</span>
                    <span class="metric-strip__item">${formatNumber(summary.games || 0)} games</span>
                    <span class="metric-strip__item">${formatPercent(summary.winrate || 0)} win rate</span>
                </div>
            </div>
        </div>
    `;
}
```

Keep `renderChampionStats(...)` but change the output to the same `stat-chip` pattern used by player stats.

- [ ] **Step 6: Restyle the fallback match page**

Replace the `match.html` content with:

```html
<main class="page-shell page-shell--tool match-fallback">
    <section class="tool-hero">
        <div>
            <span class="section-kicker">Supporting evidence</span>
            <h1>Match record</h1>
            <p>This fallback page remains available when a match needs to open outside the player dossier flow.</p>
        </div>
    </section>
    <div class="match-page-content" id="matchPageContent"></div>
</main>
```

- [ ] **Step 7: Run syntax checks for detail-page scripts**

Run:

```powershell
node --check src/main/resources/static/js/player.js
node --check src/main/resources/static/js/champion.js
```

Expected:

```text
[no output]
[no output]
```

- [ ] **Step 8: Verify live dossier and fallback pages**

Run:

```powershell
$player = Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/player.html?puuid=JZcylN07qPxswxmFYRPLQh2UBdSR77wVL5jkq7_An3r03J1deVqJ5DmIqfChkYAWsG334czpmsZIzw'
$champion = Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/champion.html?id=51'
$match = Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/match.html'
[int]$player.StatusCode
[int]$champion.StatusCode
[int]$match.StatusCode
$player.Content -match 'playerHero'
$champion.Content -match 'championHero'
```

Expected:

```text
200
200
200
True
True
```

- [ ] **Step 9: Commit the dossier reset**

```bash
git add src/main/resources/static/player.html src/main/resources/static/js/player.js src/main/resources/static/champion.html src/main/resources/static/js/champion.js src/main/resources/static/match.html src/main/resources/static/css/player.css src/main/resources/static/css/recommendations.css src/main/resources/static/css/match-details.css src/main/resources/static/css/champion.css
git commit -m "feat: rebuild dossier pages and match fallback"
```

## Self-Review Notes

### Spec coverage

- homepage issue-cover identity: Task 2
- homepage narrative sequencing: Task 2
- utilitarian internal pages: Tasks 3 and 4
- full visual reset: Tasks 1 through 4
- conservative mobile density: Tasks 1, 3, and 4 responsive rules
- routing and API preservation: Tasks 2 through 4 interfaces
- graceful loading/error behavior: Tasks 2 through 4 preserve `.error-box` and `.empty-box` behavior

### Placeholder scan

- Checked for unfinished placeholders and removed any unresolved markers from the plan body.

### Type and interface consistency

- Existing API entry points are reused unchanged.
- Existing page IDs that scripts rely on are preserved unless the task explicitly introduces a new stable hook.
- New homepage and board summary hooks are defined in the task that creates them before later steps reference them.
