# Player Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a public EUW1 player dashboard with optional email/password and Discord accounts, saved public profiles, recent-form analysis, three priority recommendations, and rate-limit-aware refresh behavior.

**Architecture:** Keep the existing Spring Boot, PostgreSQL, and static HTML/CSS/JavaScript stack. Add an `app` PostgreSQL schema for account and refresh state, use Spring Security for exploit protection and OAuth2 client behavior, authenticate application requests with hashed opaque session tokens, and compose the dashboard from existing frontend statistics queries behind a new focused service.

**Tech Stack:** Java 23, Spring Boot 3.3.3, Spring Security 6, Spring OAuth2 Client, Spring Data JPA, Flyway, PostgreSQL, Jakarta Validation, JavaMail, static HTML/CSS/JavaScript, JUnit 5, Mockito, MockMvc, Testcontainers, Maven 3.9.11

## Global Constraints

- Every Riot player profile, dashboard, champion page, and API read remains public.
- Authentication gates only account mutations, saved-profile mutations, and private preferences.
- Initial Riot platform is EUW1 using the Europe regional route.
- Saved profiles are bookmarks and never claim Riot profile ownership.
- Authentication supports email/password and Discord OAuth authorization-code login.
- Email/password registration is disabled in production until verification and password-reset email delivery are configured and tested.
- Manual refresh default cooldown is 120 seconds.
- Scheduled refresh considers profiles viewed within 14 days and not successfully refreshed within 6 hours.
- One active refresh job is allowed per PUUID.
- Existing routes, editorial purple/cream tokens, navigation labels, keyboard support, and reduced-motion behavior are preserved.
- All new backend behavior follows TDD and each task ends in an independently testable commit.
- Do not add Champion Matchup Builds in this plan.

## Official Contracts

- Spring Security OAuth2 Login custom-provider configuration: `https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html`
- Spring Security session persistence: `https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html`
- Discord OAuth2 authorization-code flow: `https://discord.com/developers/docs/topics/oauth2`
- Discord current-user resource and `identify`/`email` scopes: `https://docs.discord.com/developers/resources/user`

## File Structure

### Build and schema

- Modify `pom.xml`: security, validation, OAuth2 client, Flyway, mail, and Testcontainers dependencies.
- Modify `src/main/resources/application.properties`: auth, Discord, Flyway, cookie, email, and refresh defaults.
- Create `src/main/resources/db/migration/V1__create_player_loop_schema.sql`: `app` schema tables and indexes.
- Modify `mvnw.cmd`: handle a normal non-symlink `.m2` directory on Windows.

### Account boundary

- Create `src/main/java/org/main/account/entity/AppUserEntity.java`: application user record.
- Create `src/main/java/org/main/account/entity/OAuthIdentityEntity.java`: Discord identity link.
- Create `src/main/java/org/main/account/entity/UserSessionEntity.java`: hashed opaque session.
- Create `src/main/java/org/main/account/entity/AccountActionTokenEntity.java`: verification and reset tokens.
- Create `src/main/java/org/main/account/entity/SavedProfileEntity.java`: private user bookmark.
- Create `src/main/java/org/main/account/repository/*.java`: one repository per entity.
- Create `src/main/java/org/main/account/dto/*.java`: validated request and stable response records.
- Create `src/main/java/org/main/account/service/PasswordAuthService.java`: registration, verification, login, and reset.
- Create `src/main/java/org/main/account/service/AccountTokenService.java`: short-lived single-use action tokens.
- Create `src/main/java/org/main/account/service/SessionService.java`: issue, resolve, rotate, and revoke sessions.
- Create `src/main/java/org/main/account/service/DiscordAccountService.java`: map Discord subject to an application user.
- Create `src/main/java/org/main/account/service/SavedProfileService.java`: user-scoped bookmark behavior.
- Create `src/main/java/org/main/account/mail/AccountMailService.java`: verification and reset delivery boundary.
- Create `src/main/java/org/main/account/web/AuthController.java`: registration/login/logout/current-user/reset endpoints.
- Create `src/main/java/org/main/account/web/SavedProfileController.java`: private saved-profile endpoints.

### Security boundary

- Create `src/main/java/org/main/config/SecurityConfig.java`: public route policy, CSRF, OAuth2 login, and password encoder.
- Create `src/main/java/org/main/account/security/AppPrincipal.java`: authenticated application-user identity.
- Create `src/main/java/org/main/account/security/AppSessionAuthenticationFilter.java`: session-cookie resolution.
- Create `src/main/java/org/main/account/security/DiscordOAuth2UserService.java`: Discord user-info mapping.
- Create `src/main/java/org/main/account/security/DiscordAuthenticationSuccessHandler.java`: issue app session and redirect.
- Create `src/main/java/org/main/account/security/AuthRateLimiter.java`: bounded per-IP registration/login attempts.

### Refresh boundary

- Create `src/main/java/org/main/refresh/entity/PlayerRefreshJobEntity.java`: durable refresh state.
- Create `src/main/java/org/main/refresh/repository/PlayerRefreshJobRepository.java`: active/latest/eligible job queries.
- Create `src/main/java/org/main/refresh/dto/PlayerRefreshStatusDto.java`: public refresh state.
- Create `src/main/java/org/main/refresh/service/PlayerRefreshCoordinator.java`: enqueue, cooldown, and deduplication.
- Create `src/main/java/org/main/refresh/service/PlayerRefreshWorker.java`: Riot crawl plus rank refresh execution.
- Create `src/main/java/org/main/refresh/scheduler/SavedProfileRefreshScheduler.java`: recently active saved-profile selection.
- Create `src/main/java/org/main/refresh/web/PlayerRefreshController.java`: public status and manual refresh endpoints.

### Dashboard boundary

- Create `src/main/java/org/main/dto/frontend/RecentFormDto.java`.
- Create `src/main/java/org/main/dto/frontend/PlayerRankSummaryDto.java`.
- Create `src/main/java/org/main/dto/frontend/ChampionPoolHealthDto.java`.
- Create `src/main/java/org/main/dto/frontend/PlayerFreshnessDto.java`.
- Create `src/main/java/org/main/dto/frontend/PlayerDashboardDto.java`.
- Create `src/main/java/org/main/service/frontend/PlayerDashboardService.java`.
- Create `src/main/java/org/main/service/frontend/PlayerDashboardServiceImpl.java`.
- Modify `src/main/java/org/main/controller/frontend/PlayerController.java`: add dashboard endpoint only.
- Create `src/main/java/org/main/controller/frontend/RiotIdResolveController.java`: public Riot ID resolution.
- Create `src/main/java/org/main/service/frontend/RiotIdResolveService.java`: resolve an unknown Riot ID without performing page-request aggregation.
- Create `src/main/java/org/main/dto/frontend/RiotIdResolveRequest.java`.
- Create `src/main/java/org/main/dto/frontend/RiotIdResolveResponse.java`.

### Static frontend

- Modify `src/main/resources/static/index.html`: Riot-ID-first form and account affordance.
- Modify `src/main/resources/static/player.html`: dashboard containers, save action, freshness, and refresh state.
- Create `src/main/resources/static/account.html`: sign-in, registration, verification, reset, and saved profiles.
- Modify `src/main/resources/static/js/api.js`: CSRF-aware requests and new APIs.
- Modify `src/main/resources/static/js/home.js`: Riot ID resolution flow.
- Modify `src/main/resources/static/js/player.js`: dashboard, save, and refresh rendering.
- Create `src/main/resources/static/js/account.js`: account and saved-profile behavior.
- Modify `src/main/resources/static/css/components.css`: auth, freshness, and recent-form primitives.
- Modify `src/main/resources/static/css/player.css`: dashboard layout.
- Modify `src/main/resources/static/css/layout.css`: account header controls and responsive states.

---

### Task 1: Stabilize and Commit the Existing Editorial Redesign

**Files:**
- Modify: `mvnw.cmd`
- Modify: `src/main/resources/static/js/api.js`
- Verify: `src/test/java/org/main/frontend/StaticFrontendRedesignPlanTest.java`
- Commit existing redesign files already listed by `git status --short` under `src/main/resources/static/`

**Interfaces:**
- Consumes: existing Maven wrapper distribution URL in `.mvn/wrapper/maven-wrapper.properties`.
- Produces: a clean, tested frontend baseline and a working Windows wrapper command.

- [ ] **Step 1: Add a failing wrapper smoke check**

Run:

```powershell
cmd /c mvnw.cmd --version
```

Expected before the fix: exit 1 with `Cannot index into a null array`.

- [ ] **Step 2: Fix the normal-directory branch in `mvnw.cmd`**

Replace the unsafe target index check with:

```powershell
$mavenM2Item = Get-Item $MAVEN_M2_PATH
$mavenM2Target = $mavenM2Item.Target
if (-not $mavenM2Target -or $mavenM2Target.Count -eq 0) {
  $MAVEN_WRAPPER_DISTS = "$MAVEN_M2_PATH/wrapper/dists"
} else {
  $MAVEN_WRAPPER_DISTS = $mavenM2Target[0] + "/wrapper/dists"
}
```

- [ ] **Step 3: Remove the existing trailing whitespace**

Change `src/main/resources/static/js/api.js` to:

```javascript
    async getChampions() {
        return fetchJson(buildApiUrl('/champions'));
    },
```

- [ ] **Step 4: Verify wrapper and editorial frontend**

Run:

```powershell
cmd /c mvnw.cmd --version
cmd /c mvnw.cmd -Dtest=StaticFrontendRedesignPlanTest test
cmd /c mvnw.cmd checkstyle:check
```

Expected: Maven 3.9.11, 5 frontend tests pass, 0 Checkstyle violations.

- [ ] **Step 5: Commit only the verified redesign baseline**

Use an explicit path list. Do not stage `.idea/`, `.superpowers/`, or unrelated documentation.

```powershell
git add .mvn/wrapper/maven-wrapper.properties mvnw mvnw.cmd src/main/resources/static src/test/java/org/main/frontend/StaticFrontendRedesignPlanTest.java
git commit -m "feat: stabilize editorial frontend redesign"
```

### Task 2: Add Security Dependencies and the `app` Schema

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/db/migration/V1__create_player_loop_schema.sql`
- Create: `src/test/java/org/main/account/PlayerLoopMigrationIT.java`

**Interfaces:**
- Consumes: existing PostgreSQL datasource.
- Produces: schema `app` and tables `app_user`, `oauth_identity`, `user_session`, `account_action_token`, `saved_profile`, and `player_refresh_job`.

- [ ] **Step 1: Write the migration integration test**

```java
@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "app.scheduler.data-integrity.enabled=false"
})
@ActiveProfiles("migration-test")
class PlayerLoopMigrationIT {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsAllPlayerLoopTables() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'app'
                  and table_name in ('app_user', 'oauth_identity', 'user_session',
                                     'account_action_token', 'saved_profile', 'player_refresh_job')
                """, Integer.class);
        assertThat(count).isEqualTo(6);
    }
}
```

- [ ] **Step 2: Run the test and confirm dependency failures**

Run: `cmd /c mvnw.cmd -Dtest=PlayerLoopMigrationIT test`

Expected: test compilation fails because Testcontainers and security/Flyway dependencies are absent.

- [ ] **Step 3: Add dependencies**

Add starters for `security`, `oauth2-client`, `validation`, `mail`, and `flyway-core`; add runtime `flyway-database-postgresql`; add test-scope `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, and `org.testcontainers:postgresql`.

- [ ] **Step 4: Add exact configuration defaults**

```properties
spring.flyway.enabled=true
spring.flyway.default-schema=app
spring.flyway.schemas=app
spring.flyway.create-schemas=true

app.auth.session-cookie-name=RIOT_STATS_SESSION
app.auth.session-duration=30d
app.auth.secure-cookie=${APP_AUTH_SECURE_COOKIE:false}
app.auth.email-enabled=${APP_AUTH_EMAIL_ENABLED:false}
app.auth.frontend-base-url=${APP_FRONTEND_BASE_URL:http://localhost:8080}
app.auth.discord-enabled=${APP_AUTH_DISCORD_ENABLED:false}

spring.security.oauth2.client.registration.discord.client-id=${DISCORD_CLIENT_ID:disabled}
spring.security.oauth2.client.registration.discord.client-secret=${DISCORD_CLIENT_SECRET:disabled}
spring.security.oauth2.client.registration.discord.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.discord.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}
spring.security.oauth2.client.registration.discord.scope=identify,email
spring.security.oauth2.client.provider.discord.authorization-uri=https://discord.com/oauth2/authorize
spring.security.oauth2.client.provider.discord.token-uri=https://discord.com/api/oauth2/token
spring.security.oauth2.client.provider.discord.user-info-uri=https://discord.com/api/users/@me
spring.security.oauth2.client.provider.discord.user-name-attribute=id

app.refresh.manual-cooldown=120s
app.refresh.scheduled-min-age=6h
app.refresh.saved-profile-active-window=14d
app.refresh.scheduled-batch-size=5
```

- [ ] **Step 5: Create the migration**

The migration must use UUID primary keys, `timestamptz`, unique normalized email, unique provider/subject, unique user/PUUID, hashed token columns, expiry/consumed columns, refresh state constraints, and this partial index:

```sql
create unique index uq_player_refresh_active
    on app.player_refresh_job (puuid)
    where state in ('QUEUED', 'RUNNING');
```

- [ ] **Step 6: Run migration and baseline tests**

Run:

```powershell
cmd /c mvnw.cmd -Dtest=PlayerLoopMigrationIT test
cmd /c mvnw.cmd -Dtest=StaticFrontendRedesignPlanTest test
```

Expected: both test classes pass.

- [ ] **Step 7: Commit**

```powershell
git add pom.xml src/main/resources/application.properties src/main/resources/db/migration src/test/java/org/main/account/PlayerLoopMigrationIT.java
git commit -m "build: add player loop security schema"
```

### Task 3: Implement Account Entities and Repositories

**Files:**
- Create: `src/main/java/org/main/account/entity/*.java`
- Create: `src/main/java/org/main/account/repository/*.java`
- Create: `src/test/java/org/main/account/repository/AccountRepositoryIT.java`

**Interfaces:**
- Produces: `AppUserRepository.findByEmailNormalized(String)`, `OAuthIdentityRepository.findByProviderAndProviderSubjectId(String,String)`, `UserSessionRepository.findActiveByTokenHash(String,OffsetDateTime)`, `AccountActionTokenRepository.findUsableByTokenHashAndTokenType(...)`, and `SavedProfileRepository.findByUserIdOrderByLastViewedAtDesc(UUID)`.

- [ ] **Step 1: Write repository tests**

Test unique normalized emails, unique Discord subjects, unique user/PUUID saves, active-session expiry filtering, and single-use action-token filtering with fixed timestamps.

- [ ] **Step 2: Run and verify failure**

Run: `cmd /c mvnw.cmd -Dtest=AccountRepositoryIT test`

Expected: compilation fails because account entities and repositories do not exist.

- [ ] **Step 3: Implement focused entities**

Use `@Table(schema = "app")`, UUID identifiers generated in Java, `OffsetDateTime`, explicit column names, no bidirectional collections, and enums:

```java
public enum AppUserStatus { PENDING_VERIFICATION, ACTIVE, DISABLED }
public enum AccountTokenType { EMAIL_VERIFICATION, PASSWORD_RESET }
```

- [ ] **Step 4: Implement repositories with exact queries**

```java
Optional<AppUserEntity> findByEmailNormalized(String emailNormalized);
Optional<OAuthIdentityEntity> findByProviderAndProviderSubjectId(
        String provider, String providerSubjectId);
List<SavedProfileEntity> findByUserIdOrderByLastViewedAtDesc(UUID userId);
Optional<SavedProfileEntity> findByUserIdAndPuuid(UUID userId, String puuid);
```

Use JPQL for active session and usable token queries so expiry and consumed state are checked in the database.

- [ ] **Step 5: Run tests and commit**

Run: `cmd /c mvnw.cmd -Dtest=AccountRepositoryIT test`

Expected: all repository tests pass.

```powershell
git add src/main/java/org/main/account/entity src/main/java/org/main/account/repository src/test/java/org/main/account/repository
git commit -m "feat: add application account persistence"
```

### Task 4: Implement Opaque Sessions and Public-First Security

**Files:**
- Create: `src/main/java/org/main/config/SecurityConfig.java`
- Create: `src/main/java/org/main/account/security/AppPrincipal.java`
- Create: `src/main/java/org/main/account/security/AppSessionAuthenticationFilter.java`
- Create: `src/main/java/org/main/account/security/AuthRateLimiter.java`
- Create: `src/main/java/org/main/account/service/SessionService.java`
- Create: `src/test/java/org/main/account/security/SecurityConfigWebMvcTest.java`
- Create: `src/test/java/org/main/account/service/SessionServiceTest.java`

**Interfaces:**
- Produces: `SessionIssue issue(UUID userId)`, `Optional<AppPrincipal> resolve(String rawToken)`, `void revoke(String rawToken)`, and `void revokeAll(UUID userId)`.
- Cookie: `RIOT_STATS_SESSION`, `HttpOnly`, `SameSite=Lax`, path `/`, 30-day maximum age, `Secure` from configuration.

- [ ] **Step 1: Write failing public/private route tests**

Use MockMvc to prove `GET /api/players/example/summary` remains public, `GET /api/auth/me` returns anonymous state, and `POST /api/account/saved-profiles` requires an authenticated principal plus CSRF.

- [ ] **Step 2: Write failing session tests**

Use a fixed `Clock` and capture repository writes. Assert raw tokens are at least 32 random bytes, only SHA-256 hashes are stored, expired tokens do not resolve, and rotation revokes the previous session.

- [ ] **Step 3: Implement security configuration**

Configure:

```java
authorize.requestMatchers("/api/account/**").authenticated();
authorize.requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**").permitAll();
authorize.requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll();
authorize.requestMatchers(HttpMethod.GET, "/api/**").permitAll();
authorize.anyRequest().permitAll();
```

Use `CookieCsrfTokenRepository.withHttpOnlyFalse()` for the `XSRF-TOKEN` cookie and require `X-XSRF-TOKEN` on state-changing requests.
Expose `GET /api/auth/csrf` as `CsrfResponse(token, headerName)` so an anonymous browser can initialize CSRF before public POST requests.

- [ ] **Step 4: Implement session resolution filter**

The filter reads only the configured cookie, resolves a session, creates `UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())`, and never logs the raw token.

- [ ] **Step 5: Run tests and commit**

Run:

```powershell
cmd /c mvnw.cmd -Dtest=SessionServiceTest,SecurityConfigWebMvcTest test
```

Expected: all session and route-policy tests pass.

```powershell
git add src/main/java/org/main/config/SecurityConfig.java src/main/java/org/main/account/security src/main/java/org/main/account/service/SessionService.java src/test/java/org/main/account
git commit -m "feat: add secure application sessions"
```

### Task 5: Implement Email Registration, Verification, Login, and Reset

**Files:**
- Create: `src/main/java/org/main/account/dto/Auth*.java`
- Create: `src/main/java/org/main/account/service/PasswordAuthService.java`
- Create: `src/main/java/org/main/account/service/AccountTokenService.java`
- Create: `src/main/java/org/main/account/mail/AccountMailService.java`
- Create: `src/main/java/org/main/account/mail/SmtpAccountMailService.java`
- Create: `src/main/java/org/main/account/web/AuthController.java`
- Create: `src/test/java/org/main/account/service/PasswordAuthServiceTest.java`
- Create: `src/test/java/org/main/account/web/AuthControllerWebMvcTest.java`

**Interfaces:**
- `POST /api/auth/register` consumes `RegisterRequest(email, password, displayName)`.
- `POST /api/auth/verify-email` consumes `TokenRequest(token)`.
- `POST /api/auth/login` consumes `LoginRequest(email, password)`.
- `POST /api/auth/logout` revokes the current session.
- `GET /api/auth/me` returns `CurrentUserResponse(authenticated, user)`.
- `POST /api/auth/password-reset/request` consumes `PasswordResetRequest(email)` and always returns 202.
- `POST /api/auth/password-reset/confirm` consumes `PasswordResetConfirmRequest(token,newPassword)`.

- [ ] **Step 1: Write failing service tests**

Cover normalized email, BCrypt cost 12 verification, duplicate email, disabled email registration, pending verification, successful verification, invalid credentials with the same public error, one-time reset token use, expired tokens, and revocation of all sessions after reset.

- [ ] **Step 2: Implement validated request records**

Use Jakarta validation:

```java
public record RegisterRequest(
        @Email @NotBlank String email,
        @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 60) String displayName) { }
```

- [ ] **Step 3: Implement token and mail boundaries**

Verification tokens expire after 24 hours. Reset tokens expire after 30 minutes. Store hashes only. `AccountMailService` receives the raw token only long enough to construct the configured frontend URL.

- [ ] **Step 4: Implement controller and rate limiting**

Registration and login call `AuthRateLimiter.check(clientIp, action)`. Return 429 with a safe retry message after 5 attempts per 10-minute window. Reset request always returns 202 to prevent account enumeration.

- [ ] **Step 5: Run tests and commit**

Run:

```powershell
cmd /c mvnw.cmd -Dtest=PasswordAuthServiceTest,AuthControllerWebMvcTest test
```

Expected: all auth service and controller tests pass.

```powershell
git add src/main/java/org/main/account/dto src/main/java/org/main/account/service src/main/java/org/main/account/mail src/main/java/org/main/account/web/AuthController.java src/test/java/org/main/account
git commit -m "feat: add email account authentication"
```

### Task 6: Add Discord OAuth Login

**Files:**
- Create: `src/main/java/org/main/account/security/DiscordOAuth2UserService.java`
- Create: `src/main/java/org/main/account/security/DiscordAuthenticationSuccessHandler.java`
- Create: `src/main/java/org/main/account/service/DiscordAccountService.java`
- Modify: `src/main/java/org/main/config/SecurityConfig.java`
- Create: `src/test/java/org/main/account/service/DiscordAccountServiceTest.java`
- Create: `src/test/java/org/main/account/security/DiscordAuthenticationSuccessHandlerTest.java`

**Interfaces:**
- Consumes Discord user attributes `id`, `email`, `verified`, `global_name`, and `username` from `/users/@me`.
- Produces or reuses an `oauth_identity(provider="discord", providerSubjectId=id)` and issues the same application session cookie as password login.

- [ ] **Step 1: Write failing mapping tests**

Cover repeat login by immutable Discord ID, changed username, missing email, unverified email, verified-email matching to an existing active user, and disabled Discord configuration.

- [ ] **Step 2: Implement custom provider mapping**

Never key by Discord username. Use `id` as the provider subject. Require Discord to return a verified email. When that normalized verified email matches an existing active user, attach the Discord identity to that user and record the link time; otherwise create a new active Discord-backed user. Reject missing or unverified email with the generic OAuth error redirect.

- [ ] **Step 3: Implement success handler**

Issue the opaque app session, clear transient OAuth authorization state, and redirect to `/account.html?oauth=success`. On mapping failure, redirect to `/account.html?oauth=error` without exposing provider tokens.

- [ ] **Step 4: Run tests and commit**

Run: `cmd /c mvnw.cmd -Dtest=DiscordAccountServiceTest,DiscordAuthenticationSuccessHandlerTest test`

Expected: all Discord mapping and handler tests pass.

```powershell
git add src/main/java/org/main/account/security src/main/java/org/main/account/service/DiscordAccountService.java src/main/java/org/main/config/SecurityConfig.java src/test/java/org/main/account
git commit -m "feat: add Discord account login"
```

### Task 7: Add Saved Public Profiles

**Files:**
- Create: `src/main/java/org/main/account/dto/SavedProfile*.java`
- Create: `src/main/java/org/main/account/service/SavedProfileService.java`
- Create: `src/main/java/org/main/account/web/SavedProfileController.java`
- Modify: `src/main/java/org/main/persistence/repository/PlayerRepository.java`
- Create: `src/test/java/org/main/account/service/SavedProfileServiceTest.java`
- Create: `src/test/java/org/main/account/web/SavedProfileControllerWebMvcTest.java`

**Interfaces:**
- `GET /api/account/saved-profiles`
- `POST /api/account/saved-profiles` consumes `SaveProfileRequest(puuid,personalLabel)`.
- `PATCH /api/account/saved-profiles/{id}` consumes `UpdateSavedProfileRequest(personalLabel,isDefault)`.
- `DELETE /api/account/saved-profiles/{id}` returns 204.
- Produces `SavedProfileDto(id,puuid,gameName,tagLine,profileIconId,personalLabel,isDefault,savedAt,lastViewedAt)`.

- [ ] **Step 1: Write failing service tests**

Cover saving any existing public profile, rejecting an unknown PUUID, duplicate prevention, user isolation, private labels, one default per user, unsaving, and last-viewed ordering.

- [ ] **Step 2: Implement user-scoped service methods**

Every mutation accepts `UUID currentUserId` and queries by both resource ID and user ID. Never accept a user ID from the request body.

- [ ] **Step 3: Implement controller tests and controller**

Prove anonymous mutation returns 401, missing CSRF returns 403, authenticated valid save returns 201, and another user's saved-profile ID returns 404.

- [ ] **Step 4: Run tests and commit**

Run: `cmd /c mvnw.cmd -Dtest=SavedProfileServiceTest,SavedProfileControllerWebMvcTest test`

Expected: all saved-profile tests pass.

```powershell
git add src/main/java/org/main/account src/main/java/org/main/persistence/repository/PlayerRepository.java src/test/java/org/main/account
git commit -m "feat: add saved public profiles"
```

### Task 8: Add Durable Refresh Coordination

**Files:**
- Create: `src/main/java/org/main/refresh/entity/RefreshState.java`
- Create: `src/main/java/org/main/refresh/entity/RefreshSource.java`
- Create: `src/main/java/org/main/refresh/entity/PlayerRefreshJobEntity.java`
- Create: `src/main/java/org/main/refresh/repository/PlayerRefreshJobRepository.java`
- Create: `src/main/java/org/main/refresh/dto/PlayerRefreshStatusDto.java`
- Create: `src/main/java/org/main/refresh/service/PlayerRefreshCoordinator.java`
- Create: `src/main/java/org/main/refresh/service/PlayerRefreshWorker.java`
- Create: `src/main/java/org/main/refresh/scheduler/SavedProfileRefreshScheduler.java`
- Create: `src/main/java/org/main/refresh/web/PlayerRefreshController.java`
- Modify: `src/main/java/org/main/RiotPractice.java`
- Create: `src/test/java/org/main/refresh/PlayerRefreshCoordinatorTest.java`
- Create: `src/test/java/org/main/refresh/SavedProfileRefreshSchedulerTest.java`
- Create: `src/test/java/org/main/refresh/PlayerRefreshControllerWebMvcTest.java`

**Interfaces:**
- `POST /api/players/{puuid}/refresh` returns 202 and `PlayerRefreshStatusDto`.
- `GET /api/players/{puuid}/refresh-status` returns the latest state.
- States: `QUEUED`, `RUNNING`, `COMPLETED`, `RATE_LIMITED`, `FAILED`.
- Worker calls `CrawlerService.crawlPuuidEUW(puuid, 20)` and `RankEnrichmentService.enrichRanksForPuuidEuw(puuid)`.

- [ ] **Step 1: Write failing coordinator tests**

Use a fixed `Clock`. Cover 120-second manual cooldown, active-job deduplication, existing cached data preservation, transitions, safe failure messages, and latest-status mapping.

- [ ] **Step 2: Implement transactional enqueue and worker**

Persist `QUEUED` before asynchronous execution. The partial unique index is the final deduplication guard. Map rate-limit exceptions to `RATE_LIMITED` with `retryAfter`; map other exceptions to `FAILED` without deleting existing player data.

- [ ] **Step 3: Implement scheduled selection**

Run every 15 minutes. Select at most 5 saved profiles whose `last_viewed_at` is within 14 days and whose latest successful refresh is older than 6 hours. Reuse the coordinator so cooldown and deduplication rules remain centralized.

- [ ] **Step 4: Replace the synchronous rank-only endpoint**

Keep `/api/players/{puuid}/refresh-ranks` temporarily as a deprecated compatibility endpoint that delegates to the coordinator and returns 202. Remove its direct call to `RankEnrichmentService`.

- [ ] **Step 5: Run tests and commit**

Run:

```powershell
cmd /c mvnw.cmd -Dtest=PlayerRefreshCoordinatorTest,SavedProfileRefreshSchedulerTest,PlayerRefreshControllerWebMvcTest test
```

Expected: all refresh tests pass.

```powershell
git add src/main/java/org/main/refresh src/main/java/org/main/RiotPractice.java src/main/java/org/main/controller/frontend/PlayerRankController.java src/test/java/org/main/refresh
git commit -m "feat: coordinate player data refresh"
```

### Task 9: Add the Aggregated Player Dashboard API

**Files:**
- Create: `src/main/java/org/main/dto/frontend/RecentFormDto.java`
- Create: `src/main/java/org/main/dto/frontend/PlayerRankSummaryDto.java`
- Create: `src/main/java/org/main/dto/frontend/ChampionPoolHealthDto.java`
- Create: `src/main/java/org/main/dto/frontend/PlayerFreshnessDto.java`
- Create: `src/main/java/org/main/dto/frontend/PlayerDashboardDto.java`
- Create: `src/main/java/org/main/service/frontend/PlayerDashboardService.java`
- Create: `src/main/java/org/main/service/frontend/PlayerDashboardServiceImpl.java`
- Modify: `src/main/java/org/main/controller/frontend/PlayerController.java`
- Create: `src/main/java/org/main/controller/frontend/RiotIdResolveController.java`
- Create: `src/main/java/org/main/service/frontend/RiotIdResolveService.java`
- Create: `src/main/java/org/main/dto/frontend/RiotIdResolveRequest.java`
- Create: `src/main/java/org/main/dto/frontend/RiotIdResolveResponse.java`
- Create: `src/test/java/org/main/service/frontend/PlayerDashboardServiceTest.java`
- Create: `src/test/java/org/main/controller/frontend/PlayerDashboardControllerWebMvcTest.java`

**Interfaces:**

```java
public record PlayerDashboardDto(
        PlayerSummaryDto player,
        List<RecentFormDto> recentForm,
        List<PlayerRankSummaryDto> ranks,
        List<PlayerChampionStatsDto> championPool,
        ChampionPoolHealthDto championPoolHealth,
        List<PlayerInsightDto> priorities,
        PlayerFreshnessDto freshness,
        PlayerRefreshStatusDto refresh) { }
```

- `GET /api/players/{puuid}/dashboard`
- `POST /api/players/resolve` consumes `RiotIdResolveRequest(gameName,tagLine)` and returns `RiotIdResolveResponse(puuid,gameName,tagLine,refresh)`.

- [ ] **Step 1: Write failing calculation tests**

Use match fixtures to prove windows of 5, 10, and 20; wins/losses; win rate; average KDA using `(kills + assists) / max(1,deaths)`; three-priority cap ordered by descending score then newest creation time; and freshness from the newest player, rank, or match timestamp.

- [ ] **Step 2: Define champion-pool health**

For the last 20 matches: `FOCUSED` for 1-3 unique champions, `BALANCED` for 4-5, and `OVEREXTENDED` for 6 or more. Include games analyzed and a plain-language message.

- [ ] **Step 3: Implement dashboard composition**

Reuse `FrontendStatsService` and repositories; do not duplicate existing SQL. Cap recent matches at 20, champion pool at 10, and priorities at 3.

- [ ] **Step 4: Implement Riot ID resolution**

Validate `gameName` and `tagLine` separately. If `PlayerRepository.findByGameNameIgnoreCaseAndTagLineIgnoreCase` finds a player, return it without a Riot call. Otherwise `RiotIdResolveService` calls only `RiotApiClient.getAccountByRiotIdEurope`, persists the returned PUUID and Riot ID in `raw.players`, then asks `PlayerRefreshCoordinator` to enqueue match and rank ingestion. Return the PUUID plus queued refresh state without waiting for the crawl.

- [ ] **Step 5: Run tests and commit**

Run:

```powershell
cmd /c mvnw.cmd -Dtest=PlayerDashboardServiceTest,PlayerDashboardControllerWebMvcTest test
```

Expected: all dashboard and resolver tests pass.

```powershell
git add src/main/java/org/main/dto/frontend src/main/java/org/main/service/frontend src/main/java/org/main/controller/frontend src/test/java/org/main
git commit -m "feat: add player dashboard API"
```

### Task 10: Add CSRF-Aware Frontend API and Riot-ID-First Search

**Files:**
- Modify: `src/main/resources/static/js/api.js`
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/js/home.js`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/test/java/org/main/frontend/StaticFrontendRedesignPlanTest.java`

**Interfaces:**
- Produces `api.resolveRiotId(gameName,tagLine)`, `api.getCurrentUser()`, `ensureCsrfToken()`, and automatic `X-XSRF-TOKEN` for non-GET requests.

- [ ] **Step 1: Add failing static contract tests**

Assert the homepage contains labeled `gameName`, `tagLine`, and submit controls; `home.js` redirects to `player.html?puuid=...`; and `api.js` initializes `/api/auth/csrf`, reads `XSRF-TOKEN`, and sets `X-XSRF-TOKEN` for state-changing calls.

- [ ] **Step 2: Implement CSRF-aware request building**

```javascript
function getCookie(name) {
    return document.cookie.split('; ')
        .find(row => row.startsWith(`${name}=`))
        ?.split('=')[1] || '';
}

async function ensureCsrfToken() {
    if (!getCookie('XSRF-TOKEN')) {
        await fetchJson(buildApiUrl('/auth/csrf'));
    }
}

function buildRequestHeaders(url, options = {}) {
    const method = (options.method || 'GET').toUpperCase();
    const headers = { Accept: 'application/json', ...(options.headers || {}) };
    if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
        headers['X-XSRF-TOKEN'] = decodeURIComponent(getCookie('XSRF-TOKEN'));
        headers['Content-Type'] = 'application/json';
    }
    return headers;
}
```

Every API method that sends a state-changing request calls `await ensureCsrfToken()` before `fetchJson`.

- [ ] **Step 3: Implement the Riot ID form states**

Provide idle, submitting, not-found, rate-limited, and success states. Keep the existing global database search available as secondary discovery.

- [ ] **Step 4: Run tests and commit**

Run: `cmd /c mvnw.cmd -Dtest=StaticFrontendRedesignPlanTest test`

Expected: frontend contract tests pass.

```powershell
git add src/main/resources/static/index.html src/main/resources/static/js/api.js src/main/resources/static/js/home.js src/main/resources/static/css/components.css src/test/java/org/main/frontend/StaticFrontendRedesignPlanTest.java
git commit -m "feat: add Riot ID onboarding flow"
```

### Task 11: Add Account and Saved-Profile Frontend

**Files:**
- Create: `src/main/resources/static/account.html`
- Create: `src/main/resources/static/js/account.js`
- Modify: `src/main/resources/static/js/api.js`
- Modify: `src/main/resources/static/css/components.css`
- Modify: `src/main/resources/static/css/layout.css`
- Modify: all static page headers to include the same account entry point.
- Create: `src/test/java/org/main/frontend/StaticAccountFrontendTest.java`

**Interfaces:**
- Uses auth and saved-profile APIs from Tasks 5-7.
- Produces public header states `Sign in` and signed-in display name without hiding navigation or search.

- [ ] **Step 1: Write failing static frontend tests**

Assert visible labels, password autocomplete attributes, Discord link `/oauth2/authorization/discord`, no content-gating redirect, saved-profile list, save labels, and account controls on every static page.

- [ ] **Step 2: Implement account page**

Use separate sign-in and registration sections, verification/reset states driven by query parameters, inline errors, disabled submit controls during requests, and `aria-live="polite"` status regions.

- [ ] **Step 3: Implement shared account-header bootstrap**

Create one function in `account.js` that calls `/api/auth/me`, renders `Sign in` for anonymous users, and renders display name plus `Saved profiles` for authenticated users. It must never redirect anonymous users.

- [ ] **Step 4: Run tests and commit**

Run: `cmd /c mvnw.cmd -Dtest=StaticAccountFrontendTest,StaticFrontendRedesignPlanTest test`

Expected: both frontend test classes pass.

```powershell
git add src/main/resources/static src/test/java/org/main/frontend
git commit -m "feat: add optional account experience"
```

### Task 12: Render the Player Dashboard, Save Action, and Refresh State

**Files:**
- Modify: `src/main/resources/static/player.html`
- Modify: `src/main/resources/static/js/player.js`
- Modify: `src/main/resources/static/js/api.js`
- Modify: `src/main/resources/static/css/player.css`
- Modify: `src/main/resources/static/css/components.css`
- Create: `src/test/java/org/main/frontend/StaticPlayerDashboardTest.java`

**Interfaces:**
- Uses `GET /api/players/{puuid}/dashboard`, saved-profile APIs, `POST /api/players/{puuid}/refresh`, and refresh status.

- [ ] **Step 1: Write failing dashboard contract tests**

Assert containers for identity/rank/freshness, 5/10/20 recent form, champion-pool health, three priorities, save action, refresh status, and an `aria-live` refresh announcement.

- [ ] **Step 2: Change initial loading to dashboard-first**

Fetch the dashboard once for above-the-fold content. Preserve existing tabs and load detailed matches, rank history, and full insights on demand.

- [ ] **Step 3: Render exact state hierarchy**

Above the fold order:

1. Identity, rank, and last update
2. Recent form for 5, 10, and 20 games
3. Champion-pool health and top champions
4. Three priority recommendations
5. Refresh and save controls

Render explicit `queued`, `running`, `rate limited`, `failed with cached data`, and `completed` messages. Poll only while queued or running, stop after completion/failure, and preserve the last successful dashboard.

- [ ] **Step 4: Implement save behavior**

Anonymous click opens the account page with `returnTo` pointing to the current public profile. Authenticated click saves or unsaves without leaving the page. The profile remains visible in both cases.

- [ ] **Step 5: Verify responsive and reduced-motion behavior**

Check 375, 768, 1024, and 1440 CSS-pixel widths. All touch targets are at least 44 pixels; no horizontal scroll; focus is visible; reduced motion removes nonessential transitions.

- [ ] **Step 6: Run tests and commit**

Run:

```powershell
cmd /c mvnw.cmd -Dtest=StaticPlayerDashboardTest,StaticFrontendRedesignPlanTest test
cmd /c mvnw.cmd checkstyle:check
```

Expected: all static tests and Checkstyle pass.

```powershell
git add src/main/resources/static/player.html src/main/resources/static/js/player.js src/main/resources/static/js/api.js src/main/resources/static/css/player.css src/main/resources/static/css/components.css src/test/java/org/main/frontend
git commit -m "feat: add player dashboard experience"
```

### Task 13: Full Verification and Operations Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/deployment.md`
- Modify: `.env.example` if present; otherwise create `.env.example` with names only and no secrets.
- Create: `src/test/java/org/main/PlayerLoopPublicAccessIT.java`

**Interfaces:**
- Produces documented environment variables, Discord callback URL, SMTP requirements, refresh defaults, and public-access guarantees.

- [ ] **Step 1: Write the public-access integration test**

Verify anonymous 200 responses for homepage, player page, dashboard, champion API, and match API; verify anonymous 401 for saved-profile mutation; verify missing CSRF 403 for authenticated mutation.

- [ ] **Step 2: Document exact environment variables**

Document `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `APP_AUTH_DISCORD_ENABLED`, `APP_AUTH_EMAIL_ENABLED`, `APP_AUTH_SECURE_COOKIE`, `APP_FRONTEND_BASE_URL`, Spring mail settings, and refresh timing overrides. State that email auth must remain disabled in production until verification and reset email are tested.

- [ ] **Step 3: Run the complete verification suite**

Run:

```powershell
cmd /c mvnw.cmd test
cmd /c mvnw.cmd checkstyle:check
git diff --check
```

Expected: all tests pass, 0 Checkstyle violations, and no whitespace errors in files changed by this plan.

- [ ] **Step 4: Manual smoke test**

Run the application against PostgreSQL and verify:

1. Anonymous Riot ID search reaches a public dashboard.
2. Anonymous profile and champion browsing works.
3. Email registration sends verification when enabled.
4. Discord login creates or reuses the immutable Discord identity.
5. Signed-in user saves and labels a public profile.
6. Manual refresh shows queued, running, and completion state.
7. A second immediate refresh shows cooldown feedback.
8. Failed or rate-limited refresh retains cached dashboard data.

- [ ] **Step 5: Commit**

```powershell
git add README.md docs/deployment.md .env.example src/test/java/org/main/PlayerLoopPublicAccessIT.java
git commit -m "docs: document player loop operations"
```

## Completion Gate

The Player Loop is complete only when:

- The complete Maven test suite passes.
- Checkstyle reports zero violations.
- Public player and champion data works without authentication.
- Email/password and Discord login both issue the same opaque application session.
- Verification and reset email are tested before email registration is enabled in production.
- Saved profiles are private bookmarks with no ownership wording.
- The dashboard displays recent form, champion-pool health, three priorities, freshness, and refresh status.
- Refresh cooldown, deduplication, scheduling, rate-limit behavior, and cached-data preservation are tested.
- Responsive and keyboard checks pass at all specified widths.
- Champion Matchup Builds remain outside this implementation and receive a separate plan.
