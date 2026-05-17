# Frontend and Analysis Roadmap

## 1. Player page, ranks + rank history

Goal:
Show current player rank and rank history on the player page.

Tasks:
- Add API methods for player ranks and rank history.
- Add ranked cards to player page.
- Add rank history section.
- Display Solo/Duo and Flex separately.
- Show Unranked when no rank exists.

Files:
- src/main/resources/static/js/api.js
- src/main/resources/static/js/player.js
- src/main/resources/static/player.html
- src/main/resources/static/css/main.css

Status:
In progress.

---

## 2. Images for champions, items, abilities

Goal:
Make all pages more visual by showing images next to names and IDs.

Tasks:
- Show champion icons near champion names.
- Show item icons instead of raw item IDs.
- Show ability icons on champion page.
- Add fallback styling for missing images.

Files:
- src/main/resources/static/js/player.js
- src/main/resources/static/js/champion.js
- src/main/resources/static/js/api.js
- src/main/resources/static/css/main.css

Status:
Planned.

---

## 3. Home page cleanup

Goal:
Replace development-oriented statistics with user-facing statistics.

Tasks:
- Remove average match duration.
- Remove technical/dev statistics.
- Keep analyzed matches and players.
- Add top champions by popularity.
- Add top champions by winrate.
- Add clear search call-to-action.

Files:
- src/main/resources/static/index.html
- src/main/resources/static/js/home.js
- src/main/resources/static/css/main.css
- possibly backend stats DTO/service

Status:
Planned.

---

## 4. All champions page

Goal:
Create a page with all champions and links to champion detail pages.

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

Status:
Planned.

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
Planned, high priority.

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
Planned.

---

## 6. Champion page, first upgrade

Goal:
Improve champion page with better statistics.

Tasks:
- Add best players on champion.
- Add visualized winrate.
- Add better item statistics display.
- Show item icons.

Files:
- src/main/resources/static/champion.html
- src/main/resources/static/js/champion.js
- src/main/resources/static/css/main.css
- backend champion stats endpoints

Status:
Planned.
---

## 6.1. Champion page, match details preview

Goal:
Allow users to inspect matches related to a specific champion from the champion page.

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

Status:
Planned.

---

## 7. Champion page, advanced analysis

Goal:
Show deeper champion-specific patterns.

Tasks:
- Best rune combinations.
- Best skill order.
- Best item purchase order.
- Champion performance by role.
- Champion performance by queue.

Files:
- backend stats service
- src/main/resources/static/js/champion.js
- src/main/resources/static/css/main.css

Status:
Planned.

---

## 8. Player page, better match cards

Goal:
Replace raw match table with visual match cards.

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

Status:
Planned.

---

## 9. Match details page/section

Goal:
Allow viewing full match participants and their builds.

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

Status:
Planned.

---

## 10. Python analysis recommendations, second package

Goal:
Use items, runes, skill order and match details to generate richer recommendations.

Tasks:
- Compare player builds against winning builds.
- Detect weak first item patterns.
- Detect weak rune setups.
- Detect unusual skill orders.
- Generate champion-specific advice.
- Generate role-specific advice.

Status:
Planned.