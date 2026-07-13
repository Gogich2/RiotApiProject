# Riot Stats Editorial Reset Design

Date: 2026-07-12
Project: RiotApiPractice frontend
Scope: Full static frontend redesign with a new homepage concept, new visual system, and reorganized internal page roles
Supersedes: `docs/superpowers/specs/2026-07-11-website-redesign-design.md`

## Goal

Rebuild the Riot Stats frontend so it reads as a new website rather than a refined version of the current one.

The approved direction is:

- editorial homepage
- patch-and-meta-led narrative
- premium black visual system
- full visual reset
- utilitarian internal research pages
- conservative mobile density

The site should feel like a weekly meta report on first load, then turn into a precise research tool after the first click.

## Product Framing

The site becomes two experiences in one shell:

1. `index.html` is the issue cover.
   It answers what changed, what matters now, and where to investigate next.
2. All other pages are research tools.
   They prioritize scan speed, drill-down, and evidence over atmosphere.

This split is intentional. The homepage carries identity. The internal pages carry utility.

## Page Roles

### `index.html`

Editorial front page for the current patch and meta state.

Primary jobs:

- frame the current meta in one strong story
- surface the most important movement signals
- route users quickly into players and champions research

### `players.html`

Scouting board for comparing accounts.

Primary jobs:

- rank and filter players quickly
- expose useful comparison signals at table speed
- act as the main entry into player dossiers

### `champions.html`

Meta browser for champion performance and role presence.

Primary jobs:

- expose role-based browsing
- make efficiency and pick presence easy to compare
- act as the main entry into champion dossiers

### `player.html`

Player dossier.

Primary jobs:

- establish player identity immediately
- show summary performance first
- let the user inspect trends, match history, and recommendation output in a clean order

### `champion.html`

Champion dossier.

Primary jobs:

- establish champion identity immediately
- show strength and role context first
- let the user inspect builds, recommendations, and matchup-adjacent detail in ordered sections

### `match.html`

Supporting evidence surface for a single match.

Primary jobs:

- present details cleanly
- inherit the new visual system
- stay subordinate to player and champion pages

## Information Architecture

### Global navigation

Primary navigation remains simple:

- Home
- Players
- Champions

The nav should reflect the split between publication and tools:

- `Home` reads like the current issue
- `Players` and `Champions` read like research sections

Global search remains in the header and keeps its current behavior, but it should be visually integrated into the new shell instead of treated as a floating utility afterthought.

### Homepage sequencing

The homepage should follow this order:

1. Lead story hero
2. Companion rail with live issue context
3. Meta movers / notable shifts
4. Standout board
5. Patch snapshot / coverage metrics
6. Priority links into players and champions
7. Denser supporting tables only after the narrative layer is established

This is a deliberate break from the current dashboard-first ordering.

## Homepage Design

### Above the fold

The first viewport should have one dominant lead area and one narrower rail.

Lead area:

- short eyebrow
- hard headline tied to current meta conditions
- one short analysis paragraph
- primary action to players
- secondary action to champions

Companion rail:

- dataset freshness
- sample size / coverage
- mode or patch context
- one compact watchlist signal

The hero must feel editorial, not promotional and not like a generic SaaS dashboard.

### Supporting modules

Homepage modules should be asymmetric and role-specific rather than a field of equal cards.

Required module types:

- `Meta movers`
- `Standout board`
- `Patch snapshot`
- `Priority reads`

These modules may still be powered by existing backend data, but their presentation should read like issue sections rather than repeated dashboard panels.

### Narrative rule

The first viewport tells a story.
The second viewport proves it.
Only then should the page become denser.

## Internal Pages

### Listing pages

`players.html` and `champions.html` should become compact research surfaces.

Shared rules:

- tighter headers
- faster access to filters
- stronger row or card scan rhythm
- less visual weight on containers
- clearer ranking and comparison cues

Specific expectations:

- `players.html` should bias toward table behavior over showcase behavior
- `champions.html` should preserve champion recognition while reducing soft, repetitive card framing

### Dossier pages

`player.html` and `champion.html` should be structured as dossiers.

Required ordering:

1. identity header
2. decisive summary metrics
3. primary evidence sections
4. deeper supporting sections

The page should feel like a research note set, not a pile of decorated modules.

### Match page

`match.html` should be visually aligned with the new system but remain comparatively lean.

Its function is inspection, not storytelling.

## Visual System

### Tone

The visual system is a full reset:

- premium black
- high contrast
- restrained accent use
- minimal glow
- minimal ornamental gradients
- no heavy fantasy or gaming chrome

### Color behavior

Accent color should be selective and meaningful.

Use it for:

- primary actions
- active states
- key analytic emphasis
- compact labels or highlights

Do not use it as constant ambient decoration.

The site should rely mostly on:

- black and off-black backgrounds
- graphite and dark neutral surfaces
- strong ivory or near-ivory text
- muted separators and rules

### Typography

Typography should carry more of the hierarchy than surfaces.

Rules:

- homepage gets strong display treatment
- internal pages use tighter title and label scales
- labels and metadata become cleaner and more deliberate
- numeric areas should use tabular numerals where appropriate
- data contrast should come from weight, spacing, and alignment before color

### Surface model

The system should reduce generic card repetition.

Prefer:

- section bands
- rails
- dividers
- list modules
- table framing

Use borders more than shadows.
Use shadows only where they materially help separation.

## Components

### Header

The header remains sticky and shared, but should be rebuilt as a sharper frame:

- strong brand lockup
- restrained nav styling
- integrated search field
- clear active state
- touch-safe mobile stacking

### Buttons and controls

Buttons should feel crisp and immediate.

Rules:

- one clear primary action style
- subordinate secondary style
- compact quiet action style
- visible hover, active, and focus states
- no soft oversized button chrome

### Tables and lists

Tables are central to the product and should become the sharpest elements in the internal pages.

Rules:

- clearer headers
- tighter row rhythm
- stronger numeric alignment
- quieter secondary text
- direct hover and selected states

### Search and filters

Search and filter controls should read as tool controls, not as decorative panels.

Rules:

- compact dimensions
- strong focus state
- consistent alignment with list/table modules
- responsive reflow without awkward wrapping

### States

Loading, empty, and error states must inherit the new editorial-to-research system.

Rules:

- homepage states should preserve the issue framing
- internal states should stay concise and operational
- avoid generic placeholder blocks that break tone

## Data And Behavior

The redesign stays within the existing static architecture:

- Spring-served HTML
- shared CSS
- vanilla JavaScript
- existing API endpoints

Behavioral guidance:

- preserve existing page routing
- preserve existing API usage where practical
- preserve existing search and detail loading logic where practical
- allow DOM hook changes if the new markup requires them
- allow broader HTML restructuring where needed to support the new site architecture

This is not a backend redesign.
This is not a framework migration.
This is not limited to token swaps and CSS touch-ups.

## Responsive And Accessibility Requirements

### Responsive behavior

Mobile should stay conservative.

Rules:

- preserve touch-safe targets
- reduce dead space, not usability
- keep homepage hierarchy intact on small screens
- allow desktop and tablet to carry most of the visual density
- keep dense tables scroll-safe when necessary

### Accessibility

Required:

- visible keyboard focus states
- strong text contrast
- clear hierarchy in headings and labels
- readable mobile text sizing
- reduced-motion-respecting transitions

## Error Handling And Content Resilience

The new layout must degrade cleanly when live data is sparse or delayed.

Requirements:

- hero companion rail must still render with partial data
- homepage modules should have graceful empty and loading states
- players and champions listings should remain usable even when supporting summary modules are empty
- dossier sections should not collapse into broken spacing when one metric group is missing
- match page should keep its inspection layout even with limited payload detail

## Verification

Success criteria:

- the site looks like a different product on first load
- homepage and internal pages have intentionally different roles
- homepage reads like a current issue, not a dashboard
- internal pages read like research tools, not marketing sections
- mobile remains touch-safe
- the redesign works within the existing Spring static stack

Manual verification targets:

- `index.html`
- `players.html`
- `champions.html`
- live `player.html`
- live `champion.html`
- `match.html`

Review emphasis:

- first-viewport identity
- hierarchy quality
- scan speed on internal pages
- consistency of shared controls
- graceful empty/loading behavior

## Non-Goals

Out of scope for this redesign:

- backend model changes
- new application routes
- a frontend framework migration
- major new product features unrelated to the approved site structure
- decorative motion systems that compete with data readability
