# Deployment Notes

## Local Spring Boot mode

Run the backend and the built-in static frontend:

```bash
mvn clean package
java -jar target/RiotApiPractice-1.0-SNAPSHOT.jar
```

Open:

```text
http://localhost:8080
```

In this mode the frontend uses relative API URLs such as `/api/search`, so no extra configuration is required.

## Experimental GitHub Pages + ngrok mode

This repository also contains a static copy of the frontend under `docs/` for GitHub Pages experiments.

### 1. Run the backend locally

Start Spring Boot on port `8080` as usual.

### 2. Expose the backend with ngrok

Example:

```bash
ngrok http 8080
```

Copy the generated HTTPS forwarding URL, for example:

```text
https://example-name.ngrok-free.app
```

Do not commit a personal ngrok URL into the repository.

### 3. Configure the frontend API base URL

The frontend now supports two modes:

- default local mode: no config, uses relative `/api/...`
- hosted mode: set an external backend base URL and the frontend will call `{baseUrl}/api/...`

The simplest experimental setup is to configure it in browser storage:

```js
localStorage.setItem('riot-stats-api-base-url', 'https://example-name.ngrok-free.app');
```

To switch back to local relative mode:

```js
localStorage.removeItem('riot-stats-api-base-url');
```

Optional alternative:

```js
window.RIOT_STATS_CONFIG = {
  apiBaseUrl: 'https://example-name.ngrok-free.app'
};
```

If you use `window.RIOT_STATS_CONFIG`, load it before `js/api.js`.

### 4. Test the hosted frontend

Open the GitHub Pages site and verify these endpoints through the UI:

- `/api/search`
- `/api/players/{puuid}/summary`
- `/api/players/{puuid}/matches`
- `/api/players/{puuid}/champions`
- `/api/players/{puuid}/ranks`
- `/api/players/{puuid}/rank-history`
- `/api/players/{puuid}/insights`
- `/api/champions/{championId}`
- `/api/champions/{championId}/items`

Suggested manual checks:

1. Search for a player or champion from the home page.
2. Open a player page and verify summary, matches, champions, ranks, rank history, and insights.
3. Open a champion page and verify hero, abilities, and item statistics.

## Leaderboard refresh

Player leaderboard data is backed by the existing materialized view:

```text
analyzed.player_leaderboard_stats
```

Refresh it manually when needed:

```sql
REFRESH MATERIALIZED VIEW analyzed.player_leaderboard_stats;
```

If concurrent refresh is needed and the unique index exists:

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY analyzed.player_leaderboard_stats;
```

## CORS notes

The backend CORS allowlist is configurable through:

```properties
app.cors.allowed-origins
```

Default development-oriented origins include:

- `http://localhost:8080`
- `http://127.0.0.1:8080`
- `http://localhost:5500`
- `http://127.0.0.1:5500`
- `https://gogich2.github.io`
- `null`

For a different GitHub Pages origin, override `APP_CORS_ALLOWED_ORIGINS`.
