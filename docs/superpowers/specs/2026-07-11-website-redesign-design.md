# Website Redesign Design

Date: 2026-07-11
Project: RiotApiPractice frontend
Scope: Whole static site redesign across shared tokens, shared layout/components, homepage, listing pages, and detail pages

## Goal

Reimagine the existing website so it feels modern, responsive, and relatively simple while adopting a dark-first visual system built around:

- Electric purple: `#6800FF`
- Cream: `#FFF9EB`

The redesign should improve clarity and responsiveness across the full site without changing the frontend stack or overcomplicating the interface.

## Current Context

The frontend is served as static HTML, CSS, and JavaScript from Spring resources. The redesign will work within the existing structure:

- Shared design tokens: `src/main/resources/static/css/base.css`
- Shared layout styles: `src/main/resources/static/css/layout.css`
- Shared components: `src/main/resources/static/css/components.css`
- Entry pages: `index.html`, `players.html`, `player.html`, `champions.html`, `champion.html`, `match.html`

The current design already has a dashboard structure, shared tokens, and reusable panels. It needs a more coherent visual language, better responsive behavior, and a cleaner hierarchy.

## Design Direction

Chosen direction: `Analyst Console`

This direction keeps the site information-first and tool-like rather than decorative. It uses strong brand color selectively, keeps surfaces disciplined, and prioritizes scanability for tables, stats, and rankings.

Characteristics:

- dark-first UI
- balanced density rather than ultra-minimal or highly dramatic
- restrained, intentional use of glow and gradients
- strong type hierarchy and spacing rhythm
- consistent component system across all pages

## Visual System

### Color roles

The palette will be reorganized around fewer, clearer roles:

- `#6800FF` as the primary accent for actions, active states, focus, and key highlights
- `#FFF9EB` as the main high-contrast text/light tone
- near-black and purple-black surfaces for the page canvas and panels
- subtle violet-tinted borders and separators

Usage rules:

- Purple is the only dominant accent.
- Cream is used for readable text and selective contrast, not large cream background blocks.
- Secondary states should be built from surface contrast, typography, and borders rather than introducing extra accent colors.

### Surface ladder

The site should use a stable three-level surface system:

1. Page background: deep purple-black canvas
2. Section/panel surface: slightly raised violet-tinted dark panels
3. Interactive/inner surface: tables, stat cards, search panels, and compact modules

This creates depth without relying on heavy shadows or glass effects.

### Typography

Typography should do more of the hierarchy work than card decoration:

- strong, clean headings
- restrained body copy
- compact uppercase labels for metadata
- improved contrast between primary values and supporting text

Large numbers and key entity names should be easier to scan than they are in the current build.

## Layout Shell

### Header

The sticky header remains, but becomes cleaner and more systematic:

- three desktop zones: brand, navigation, search
- search aligned as a clear utility area rather than a squeezed afterthought
- active navigation uses purple emphasis with restrained contrast
- mobile/tablet header stacks into a compact two-row layout so search can span full width

### Page width and spacing

The site keeps a constrained content width but uses more disciplined gutters and spacing:

- tighter mobile gutters
- comfortable tablet spacing
- consistent desktop max width
- fewer visually heavy framed sections

Section rhythm should come from spacing and typography first, not from wrapping everything in loud panels.

### Responsive grids

Explicit responsive behaviors:

- hero layouts collapse from two columns to one
- stat grids move from 4 columns to 2 to 1
- side-by-side dashboard panels stack cleanly on narrower widths
- dense detail sections become a stable single-column flow on mobile
- tables remain scroll-safe where needed, with tighter responsive cell spacing

## Homepage

The homepage remains an overview and routing surface, not a dense content dump.

### Hero

The homepage hero should become a calm command surface:

- left column: message, short supporting copy, primary actions
- right column: product-style summary panel instead of decorative imagery
- restrained purple emphasis, not a highly illustrative gaming hero

The right-hand side should feel like a sharp status snapshot rather than marketing art.

### Quick links

The existing quick links remain but are cleaned up:

- more uniform structure
- less visually heavy treatment
- clearer hover/focus states
- stronger title hierarchy and shorter supporting descriptions

### Overview stats

Stat cards should read faster:

- more consistent heights
- stronger numeric emphasis
- quieter labels and secondary text
- purple reserved for emphasis instead of filling entire cards

### New module: Meta snapshot

Add one compact summary module near the top of the homepage, after the hero and before the heavier tables.

Contents:

- strongest win-rate champion
- most-played champion
- hottest player or most active account

Purpose:

- adds one high-value summary layer without overloading the homepage
- improves homepage scannability
- adapts cleanly to mobile as a 1-column or compact stacked strip

### Lower content blocks

The existing table and player sections stay, but the panel styling becomes lighter so the data carries more visual weight than the containers.

## Inner Pages

### Listing pages

Applies to:

- `players.html`
- `champions.html`

Design intent:

- cleaner filter/search surfaces
- stronger row readability
- more consistent spacing and table rhythm
- clearer hover states and selected emphasis
- less visual clutter around table containers

These pages should feel like efficient browsing tools.

### Detail pages

Applies to:

- `player.html`
- `champion.html`
- `match.html`

Design intent:

- summary first
- performance modules second
- deeper breakdowns after that
- consistent visual rhythm across all sections

Dense areas, especially match details, should be simplified through:

- quieter backgrounds
- clearer section separation
- less competing accent usage
- more deliberate stacking at smaller widths

## Components

The redesign should unify shared component behavior across pages.

### Buttons

Three clear action tiers:

- primary purple button
- secondary dark-outline button
- quiet text/link action

### Inputs and search

Search and inputs should feel cleaner and more intentional:

- lower-noise resting state
- strong visible focus ring
- better integration with dark surfaces
- responsive full-width behavior where needed

### Panels and cards

Panels remain part of the design, but should be visually lighter:

- fewer gradients
- restrained borders
- subtle elevation
- less "card for every block" feeling

### Tables

Tables remain central to the product. Improvements should focus on:

- row readability
- better header contrast
- tighter responsive spacing
- cleaner hover states
- scroll containment on small screens

### Utility states

Loading, empty, and error states must be redesigned into the same visual language so they do not feel disconnected from the rest of the UI.

## Interaction and Motion

Motion should stay understated and functional:

- short, crisp hover/focus transitions
- light elevation or border change on interaction
- no heavy animation system
- no broad neon glow or decorative motion

The UI should feel responsive and modern without drawing attention to the transitions themselves.

## Implementation Boundaries

The redesign should be implemented conservatively inside the current codebase.

### Keep

- the current static HTML/CSS/JS stack
- the current page set and routing model
- existing JavaScript behavior unless small DOM adjustments are required

### Change primarily through

- shared tokens in `base.css`
- shared shell/layout rules in `layout.css`
- shared components in `components.css`
- targeted page-specific CSS adjustments where necessary
- minimal HTML changes where hierarchy or responsiveness genuinely requires them

### Avoid

- adding a new frontend framework
- large JS rewrites
- decorative complexity that hurts data readability
- adding many new homepage sections

## Success Criteria

The redesign is successful if:

- the whole site clearly reflects the purple/cream identity
- the interface feels modern but still simple
- mobile/tablet layouts are intentionally responsive rather than compressed desktop views
- homepage summary quality improves without adding much more content
- shared components feel coherent across all pages
- dense data views become easier to scan

## Verification Approach

Implementation should be checked at minimum on:

- homepage
- players listing
- player detail
- champions listing
- champion detail
- match detail

Viewport coverage:

- mobile
- tablet
- desktop

Things to verify:

- header responsiveness
- search usability
- table overflow behavior
- card/grid stacking
- readability of text against dark surfaces
- consistency of purple action states and cream text contrast

## Out of Scope

- major feature additions beyond the single `Meta snapshot` homepage module
- backend API changes
- new client-side architecture
- content expansion that turns the homepage into a full dashboard wall
