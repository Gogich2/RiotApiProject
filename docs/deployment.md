# Deployment Notes

## Public access and optional accounts

Player, champion, match, search, and dashboard reads are public. Visitors do not
need an account to browse data or use the Riot ID lookup. Accounts only provide
private saved-profile bookmarks and personal labels.

State-changing requests require a CSRF token from `GET /api/auth/csrf`. Routes
under `/api/account/**` additionally require the opaque application session
cookie. Production deployments with accounts or state-changing actions must
serve the frontend and backend on the same origin so session and CSRF cookies
remain first-party.

Copy `.env.example` to `.env` for local development. Never commit `.env`.

## Required runtime configuration

Core settings:

| Variable | Purpose |
| --- | --- |
| `RIOT_API_KEY` | Riot development or production API key. |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL. |
| `SPRING_DATASOURCE_USERNAME` | PostgreSQL user. |
| `SPRING_DATASOURCE_PASSWORD` | PostgreSQL password. |
| `APP_FRONTEND_BASE_URL` | Public website origin used in verification and reset links. |
| `APP_AUTH_SECURE_COOKIE` | Set `true` for every HTTPS deployment. |

Flyway creates and migrates the `app` schema. Hibernate validates the existing
database structure; it does not create production tables.

## Discord login

Discord login is off unless `APP_AUTH_DISCORD_ENABLED=true`. Configure:

- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `APP_AUTH_DISCORD_ENABLED=true`

Register this exact redirect in the Discord application, replacing the origin
with the public backend origin:

```text
https://your-domain.example/login/oauth2/code/discord
```

The Discord user ID is the immutable external identity. Repeated logins reuse
the same application account and issue the same kind of opaque session used by
email login.

## Email login and SMTP

Email registration is off unless `APP_AUTH_EMAIL_ENABLED=true`. Configure these
Spring Mail environment variables before enabling it:

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH`
- `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE`

Keep email authentication disabled in production until both verification mail
and password-reset mail have been delivered and completed successfully against
the production SMTP provider and public `APP_FRONTEND_BASE_URL`.

## Refresh timing and Riot limits

Defaults are deliberately conservative for a standard Riot API development key:

| Variable | Default | Purpose |
| --- | --- | --- |
| `APP_REFRESH_MANUAL_COOLDOWN` | `120s` | Minimum delay between manual refreshes for a player. |
| `APP_REFRESH_SCHEDULED_MIN_AGE` | `6h` | Age before cached player data becomes eligible for scheduled refresh. |
| `APP_REFRESH_SAVED_PROFILE_ACTIVE_WINDOW` | `14d` | Recently used saved profiles considered by the scheduler. |
| `APP_REFRESH_SCHEDULED_BATCH_SIZE` | `5` | Maximum profiles queued in one scheduler pass. |
| `APP_REFRESH_SCHEDULER_DELAY` | `PT15M` | Delay between scheduler passes. |
| `RIOT_RATE_LIMIT_PER_SECOND` | `5` | Local short-window request ceiling. |
| `RIOT_RATE_LIMIT_PER_TWO_MINUTES` | `85` | Local long-window request ceiling. |
| `RIOT_RATE_LIMIT_MAX_RETRIES` | `5` | Retry ceiling for rate-limited Riot calls. |

The local ceilings should stay below the limits shown for the deployed Riot
key. Refresh jobs are deduplicated; queued and running jobs are polled, while
completed, failed, and rate-limited states stop polling. Failure never deletes
the last successful dashboard data.

## Production smoke checklist

Run this checklist after deploying with a real PostgreSQL database and Riot key:

1. While signed out, search a Riot ID and reach its public dashboard.
2. While signed out, open player, champion, and match details.
3. If email accounts are enabled, register a disposable address and complete
   both the verification and password-reset links.
4. If Discord login is enabled, sign in twice and confirm the same saved-profile
   list is returned both times.
5. Sign in, save a public profile, add a personal label, and remove it again.
6. Refresh a player and observe queued, running, then completed status.
7. Refresh the same player immediately and confirm cooldown feedback.
8. Simulate or observe a failed/rate-limited refresh and confirm cached dashboard
   data stays visible.

Do not enable an account provider merely because application startup succeeds;
complete the provider-specific checks above first.

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

This legacy split-origin mode is **public read-only**. It can call public GET
endpoints, but it does not support Riot ID submission, refresh requests, login,
or saved-profile changes. Those features intentionally require same-origin CSRF
and session cookies. Use the built-in static frontend for the complete product.

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

Suggested read-only checks:

1. Use the header search to find an already stored player or champion.
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
