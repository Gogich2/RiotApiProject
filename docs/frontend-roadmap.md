# Frontend and Analysis Roadmap

## Current status summary

- Player page: Done for summary, recent matches, champion stats, current ranks, rank refresh, rank history list/chart,
  and recommendations tab. Better match cards and match details are still planned.
- Champion page: Partially done. Hero, lore, summary stats, abilities, and item statistics with names/icons are live.
  Advanced analysis and champion-related match details are still planned.
- Home page: Partially done. User-facing overview stats, top champions, and search are available, but cleanup and
  polish are still ongoing.
- Recommendation UI and analysis: Partially done. Readable labels, icons, summary cards, and player-page integration
  exist. Deeper grouping, severity, localization, and richer Python rules are still planned.
- Backend/API support: Done for the current frontend scope. Current player/champion/search endpoints used by the
  frontend are implemented. Further match detail and advanced analysis endpoints are still planned.

---

## 1. Player page, ranks + rank history

Goal:
Show current player rank and rank history on the player page.

Status:
Done.

Done:
- API methods for player ranks and rank history are implemented.
- Ranked cards are shown on the player page.
- Rank history section is implemented.
- Solo/Duo and Flex are displayed separately.
- Unranked state is handled.
- Rank refresh workflow is implemented.

Remaining:
- Minor UI polish can continue together with broader player page improvements.

Files:
- src/main/resources/static/js/api.js
- src/main/resources/static/js/player.js
- src/main/resources/static/player.html
- src/main/resources/static/css/main.css

---

## 2. Images for champions, items, abilities

Goal:
Make all pages more visual by showing images next to names and IDs.

Status:
Partially done.

Done:
- Champion icons are shown in user-facing areas that already use champion stats.
- Item icons are shown on the champion page item table.
- Ability section exists on the champion page.

Remaining:
- Continue image coverage on future pages such as match details and all champions.
- Verify and polish champion ability data/icon loading.
- Improve missing-image handling where needed without overcomplicating the UI.

Files:
- src/main/resources/static/js/player.js
- src/main/resources/static/js/champion.js
- src/main/resources/static/js/api.js
- src/main/resources/static/css/main.css

---

## 3. Home page cleanup

Goal:
Replace development-oriented statistics with user-facing statistics.

Status:
Partially done.

Done:
- User-facing overview stats are implemented or partially implemented.
- Top champions by popularity are implemented.
- Top champions by winrate are implemented.
- Search call-to-action/global search is implemented or partially implemented.

Remaining:
- Finish cleanup of any remaining development-oriented metrics.
- Polish home page wording and presentation.

Files:
- src/main/resources/static/index.html
- src/main/resources/static/js/home.js
- src/main/resources/static/css/main.css
- possibly backend stats DTO/service

---

## 4. All champions page

Goal:
Create a page with all champions and links to champion detail pages.

Status:
Planned.

Tasks:
- Add /champions.html.
- Add champions grid.
- Add search/filter by champion name.
- Show champion icon, name, games, winrate.
- Link each champion to /champion.html?id=...

Files:
- src/main/resources/static/champions.html
- src/main/resources/static/js/champions.js
- src/main/resources/static/css/main.css
- backend champion list endpoint if needed

---

## 5. Python analysis recommendations, priority diploma block

Goal:
Expand the recommendation module so the diploma has a strong analysis part.

Recommendation groups:
- Vision recommendations.
- KDA and deaths recommendations.
- Gold and farm recommendations.
- Item timing recommendations.
- Rune recommendations.
- Skill order recommendations.
- Champion-specific recommendations.
- Role-based comparison recommendations.

Files:
- ml/src/insight_generator.py
- ml/src/rules/vision_insights.py
- ml/src/rules/item_timing_insights.py
- ml/src/rules/rune_insights.py
- ml/src/rules/summoner_spell_insights.py
- ml/src/rules/skill_order_insights.py
- ml/src/rules/champion_performance_insights.py

Status:
In progress.

Done:
- Existing insights are already surfaced in the UI through the player page recommendations tab.
- Recommendation summaries are visible in the UI and backed by existing insight endpoints.

Remaining:
- Richer Python recommendation rules are still in progress.
- Expand rule coverage for vision, KDA, gold/farm, item timing, runes, skill order, champion-specific logic, and
  role-based comparison.
- Improve the analytical depth so this section carries more of the diploma focus.

## 5.1. Improve recommendation UI

Goal:
Make recommendations look user-facing instead of technical database output.

Tasks:
- Replace technical insight types like VISION_WEAKNESS with readable labels.
- Add icons/images for recommendation categories.
- Add visual severity/score badges.
- Group recommendations by category.
- Later add Ukrainian localization for recommendation titles and descriptions.

Example:
- VISION_WEAKNESS -> Vision control
- KDA_WEAKNESS -> Fight survival
- FARM_WEAKNESS -> Farming
- ITEM_BUILD_WEAKNESS -> Item build
- RUNE_WEAKNESS -> Runes
- SKILL_ORDER_WEAKNESS -> Skill order

Files:
- src/main/resources/static/js/player.js
- src/main/resources/static/css/main.css
- src/main/resources/static/img/insights/

Status:
Partially done.

Done:
- Technical insight types are converted into more readable labels.
- Recommendation icons are implemented.
- Recommendations tab exists on the player page.
- Recommendation summary cards are implemented.

Remaining:
- Add severity/score badges.
- Group recommendations more clearly by category.
- Add later localization support.

---

## 6. Champion page, first upgrade

Goal:
Improve champion page with better statistics.

Status:
Partially done.

Done:
- Champion hero is implemented.
- Champion splash, icon, title, and lore are implemented.
- Champion summary stats are implemented.
- Ability section exists.
- Item statistics table is implemented.
- Item names are shown instead of only item IDs.
- Item icons are shown using Data Dragon image URLs.
- Item endpoint failure no longer breaks the whole champion page.
- Compact champion names are formatted for display.

Remaining:
- Add best players on champion if still needed.
- Improve champion-specific visual analysis beyond the current summary and item table.

Files:
- src/main/resources/static/champion.html
- src/main/resources/static/js/champion.js
- src/main/resources/static/css/main.css
- backend champion stats endpoints

---

## 6.1. Champion page, match details preview

Goal:
Allow users to inspect matches related to a specific champion from the champion page.

Status:
Planned.

Tasks:
- Add recent matches section to champion page.
- Show match result, player, KDA, duration, queue and patch.
- Add champion icon and basic visual styling.
- Add "View match details" action for each match.
- Create or reuse match details endpoint.
- Show all participants in selected match.
- Show champions, players, final items and runes for each participant.
- Highlight the searched champion/player inside match details.
- Later add match timeline highlights if needed.

Files:
- src/main/resources/static/champion.html
- src/main/resources/static/js/champion.js
- src/main/resources/static/css/main.css
- src/main/resources/static/match.html
- src/main/resources/static/js/match.js
- backend match details DTO/service/controller

---

## 7. Champion page, advanced analysis

Goal:
Show deeper champion-specific patterns.

Status:
Planned.

Planned work:
- Best rune combinations.
- Best skill order.
- Best item purchase order.
- Champion performance by role.
- Champion performance by queue.

Files:
- backend stats service
- src/main/resources/static/js/champion.js
- src/main/resources/static/css/main.css

---

## 8. Player page, better match cards

Goal:
Replace raw match table with visual match cards.

Status:
Planned.

Done:
- Recent matches are implemented in table form.

Remaining:
- Replace the table with richer match cards.
- Add final items and stronger visual hierarchy.
- Add link/button to match details.

Tasks:
- Show champion icon and result.
- Show KDA visually.
- Show duration and queue.
- Show final items.
- Add button/link to match details.

Files:
- src/main/resources/static/player.html
- src/main/resources/static/js/player.js
- src/main/resources/static/css/main.css
- backend match details endpoint

---

## 9. Match details page/section

Goal:
Allow viewing full match participants and their builds.

Status:
Planned.

Tasks:
- Show both teams.
- Show all champions and players.
- Show final items.
- Show runes.
- Show win/loss team highlight.
- Add link from player match cards.

Files:
- src/main/resources/static/match.html
- src/main/resources/static/js/match.js
- src/main/resources/static/css/main.css
- backend match details endpoint

---

## 10. Python analysis recommendations, second package

Goal:
Use items, runes, skill order and match details to generate richer recommendations.

Status:
Planned.

Remaining:
- Compare player builds against winning builds.
- Detect weak first item patterns.
- Detect weak rune setups.
- Detect unusual skill orders.
- Generate champion-specific advice.
- Generate role-specific advice.

Tasks:
- Compare player builds against winning builds.
- Detect weak first item patterns.
- Detect weak rune setups.
- Detect unusual skill orders.
- Generate champion-specific advice.
- Generate role-specific advice.
