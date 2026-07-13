# Graph Report - .  (2026-07-13)

## Corpus Check
- 374 files · ~211,939 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2859 nodes · 7339 edges · 145 communities (129 shown, 16 thin omitted)
- Extraction: 86% EXTRACTED · 14% INFERRED · 0% AMBIGUOUS · INFERRED: 1003 edges (avg confidence: 0.8)
- Token cost: 46,600 input · 12,250 output

## Community Hubs (Navigation)
- Champion Match Metrics
- Player Dashboard Analytics
- Saved Profile API
- Account Authentication DTOs
- Match Detail Frontend
- Match Analysis Pipeline
- Build Aggregation Configuration
- Player Rank Persistence
- Python Insights Service
- Build API Queue
- Champion Page API
- Player Dashboard API
- Account Token Persistence
- Riot API Client
- Timeline Event Persistence
- Build Aggregation Rules
- Player Rank API
- Account Token Mail
- Build Snapshot Model
- Riot ID Resolution
- Search API
- Repository Proxy Startup
- Aggregation Outcome Flow
- Match Search Repository
- League Snapshot Persistence
- Champion Builds Frontend
- Match Data JPA Tests
- Build Aggregator Tests
- Refresh Job State
- Champion Builds Delivery
- Riot HTTP Client
- Application User Persistence
- Data Integrity Tools
- Transaction Configuration Tests
- Crawler API
- OAuth Identity Security
- Build Fallback Reasons
- Build Scoring Rules
- Build Source Extraction
- Static Data API
- CSRF Protection
- Build Source Repository
- User Session Repository
- User Session Persistence
- Discord OAuth Handlers
- Global Error Handling
- Build Display Assets
- Build Snapshot Publication
- Balanced Dataset Crawler
- Build Service Tests
- OAuth User Profile
- Summoner Persistence
- Ingest Log Persistence
- Refresh Coordination
- Security Filter Chain
- Item Catalog Rules
- Frontend API Client
- Crawler Cucumber Tests
- Timeline Frame Persistence
- SMTP Account Mail
- Saved Profile Service
- Timeline Repository Cleanup
- Security MVC Tests
- OAuth User Service
- Build Options API
- Docs API Client
- Refresh Job Persistence
- Aggregation Status Model
- Build Asset Repository
- Player Dashboard DTOs
- Refresh Status DTOs
- Homepage Meta Snapshot
- Saved Profile MVC Tests
- Parameterized Build Tests
- Refresh Cooldown API
- Champion Builds Architecture
- Ingest Logging Service
- Account Frontend
- Website Redesign System
- User Account Status
- Ingest Log Service
- Champion Detail Frontend
- Champion List Frontend
- Raw Timeline Persistence
- Cucumber Spring Setup
- Player Product Roadmap
- JPA Repository Interfaces
- Maven Wrapper
- Token Concurrency Tests
- Build Scheduler
- Timeline Frame Identity
- Refresh Controller Tests
- Redesign Plan Tests
- Build Dashboard UX
- Docs Player Frontend
- Player Loop Architecture
- Request Logging Filter
- Refresh Concurrency Tests
- Localization Configuration
- Editorial Design System
- Build Source Integration
- Match API
- Player Migration Tests
- Build Controller Tests
- Build Migration Tests
- Build Frontend Contract
- Optional Account Design
- Patch Version Model
- Build Controller API
- Rune Page Extraction
- Analysis Status Model
- Player Schema Migration
- Account Frontend Tests
- Player Dashboard Tests
- Build Source Fixtures
- Spring Boot Entry
- Web CORS Configuration
- Dashboard Hero Artwork
- Match Map Artwork
- Player Leaderboard Frontend
- Cucumber Test Suite
- IntelliJ Proxy Fix
- Project Architecture Overview
- Summoner Name Normalization
- CI Security Pipeline
- Build Schema Migration
- Champion Builds Progress
- Backup Runner Script
- Development Runner Script
- Precommit Hook
- Production Runner Script
- Match Repository Fixture
- Maven Project Metadata

## God Nodes (most connected - your core abstractions)
1. `BuildQueue` - 72 edges
2. `PatchWindow` - 65 edges
3. `BuildRole` - 54 edges
4. `LeagueEntryEntity` - 53 edges
5. `AppUserEntity` - 45 edges
6. `SessionService` - 45 edges
7. `MatchTimelineEventEntity` - 43 edges
8. `PlayerRepository` - 43 edges
9. `LeagueEntrySnapshotEntity` - 40 edges
10. `PlayerEntity` - 38 edges

## Surprising Connections (you probably didn't know these)
- `Public Riot ID Entry Flow` --implements--> `Public-First Player Loop`  [INFERRED]
  src/main/resources/static/index.html → docs/superpowers/plans/2026-07-13-player-loop-implementation.md
- `Three Priority Recommendations` --implements--> `Insight Quality Over Feature Count`  [INFERRED]
  src/main/resources/static/player.html → docs/superpowers/plans/2026-07-13-player-product-roadmap.md
- `Champion Build Workspace` --implements--> `Champion Builds Design`  [INFERRED]
  src/main/resources/static/champion.html → docs/superpowers/specs/2026-07-13-champion-builds-design.md
- `Stored Item Statistics Surface` --conceptually_related_to--> `Prepared Build Snapshots`  [INFERRED]
  src/main/resources/static/champion.html → docs/superpowers/specs/2026-07-13-champion-builds-design.md
- `Champion Build API Contract` --conceptually_related_to--> `Swagger OpenAPI Documentation`  [INFERRED]
  .superpowers/sdd/task-7-brief.md → docs/api/README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Champion Build Delivery Pipeline** — _superpowers_sdd_task_3_brief_deterministic_build_observations, _superpowers_sdd_task_4_brief_frontend_ready_snapshot_aggregation, _superpowers_sdd_task_5_brief_atomic_snapshot_publication, _superpowers_sdd_task_6_brief_scheduled_aggregation [EXTRACTED 1.00]
- **Champion Matchup Layout Directions** — _superpowers_brainstorm_362_1783896368_content_champion_matchup_layout_v2_scout, _superpowers_brainstorm_362_1783896368_content_champion_matchup_layout_v2_field_guide, _superpowers_brainstorm_362_1783896368_content_champion_matchup_layout_v2_matchup_lens [EXTRACTED 1.00]
- **Champion Build Verification Layers** — _github_workflows_ci_test_suite, _github_workflows_ci_owasp_dependency_check, _superpowers_sdd_task_10_brief_final_verification, _superpowers_sdd_task_10_report_final_verification_results [INFERRED 0.85]
- **Champion Build Delivery Layers** — docs_superpowers_plans_2026_07_13_champion_builds_implementation_snapshot_aggregation_pipeline, _superpowers_sdd_task_7_brief_champion_build_api_contract, _superpowers_sdd_task_8_brief_champion_build_browser_contract, _superpowers_sdd_task_9_brief_responsive_build_ui_contract [INFERRED 0.85]
- **Riot Stats Operational Documentation** — docs_architecture_layered_riot_api_architecture, docs_backup_backup_and_restore_strategy, docs_deployment_application_deployment_guide, docs_update_windows_production_update_and_rollback [INFERRED 0.75]
- **Player Loop Product System** — docs_superpowers_plans_2026_07_13_player_loop_implementation_opaque_application_sessions, docs_superpowers_plans_2026_07_13_player_loop_implementation_durable_refresh_coordination, docs_superpowers_plans_2026_07_13_player_loop_implementation_aggregated_player_dashboard, docs_superpowers_plans_2026_07_13_player_loop_implementation_optional_saved_profiles, src_main_resources_static_index_public_riot_id_entry_flow, src_main_resources_static_player_queue_separated_player_dashboard [EXTRACTED 1.00]
- **Trustworthy Champion Build Delivery** — docs_superpowers_specs_2026_07_13_champion_builds_design_prepared_build_snapshots, docs_superpowers_specs_2026_07_13_champion_builds_design_champion_role_baseline, docs_superpowers_specs_2026_07_13_champion_builds_design_threshold_gated_exact_matchups, docs_superpowers_specs_2026_07_13_champion_builds_design_explicit_fallback_ladder, docs_superpowers_specs_2026_07_13_champion_builds_design_atomic_snapshot_publication, src_main_resources_static_champion_champion_build_workspace [EXTRACTED 1.00]
- **Editorial and Research Site Shell** — docs_superpowers_specs_2026_07_12_editorial_reset_design_issue_cover_and_research_tools, src_main_resources_static_index_riot_stats_home_page, src_main_resources_static_players_riot_stats_players_page, src_main_resources_static_champions_riot_stats_champions_page, src_main_resources_static_player_riot_stats_player_page, src_main_resources_static_champion_riot_stats_champion_page, src_main_resources_static_match_riot_stats_match_details_page [INFERRED 0.95]
- **Analytical League Dashboard Hero** — src_main_resources_static_img_ui_dashboard_hero_v2_summoners_rift_battlefield, src_main_resources_static_img_ui_dashboard_hero_v2_opposing_team_bases, src_main_resources_static_img_ui_dashboard_hero_v2_tactical_analytics_overlays, src_main_resources_static_img_ui_dashboard_hero_v2_cool_warm_competitive_split, src_main_resources_static_img_ui_dashboard_hero_v2_central_negative_space [INFERRED 0.95]
- **Match Map Visual Layers** — src_main_resources_static_img_ui_match_map_three_lane_map_topology, src_main_resources_static_img_ui_match_map_opposing_team_bases, src_main_resources_static_img_ui_match_map_lane_tower_positions, src_main_resources_static_img_ui_match_map_blue_and_red_event_markers, src_main_resources_static_img_ui_match_map_river_and_jungle_zones [EXTRACTED 1.00]

## Communities (145 total, 16 thin omitted)

### Community 0 - "Champion Match Metrics"
Cohesion: 0.06
Nodes (26): ChampionAbilityDto, MatchDetailsDto, MatchMetricsDto, MatchParticipantDto, MatchParticipantItemEventDto, MatchParticipantRuneDto, MatchParticipantSkillOrderDto, MatchSummaryDto (+18 more)

### Community 1 - "Player Dashboard Analytics"
Cohesion: 0.06
Nodes (82): bindRankChartTooltip(), cachedPlayerInsights, calculateGamesChange(), calculateLpChange(), collapseExpandedMatchCard(), collapseMatchCard(), compareInsightsForDisplay(), escapeHtml() (+74 more)

### Community 2 - "Saved Profile API"
Cohesion: 0.06
Nodes (25): DeleteMapping, PatchMapping, SavedProfileDto, SaveProfileRequest, UpdateSavedProfileRequest, Entity, Table, SavedProfileEntity (+17 more)

### Community 3 - "Account Authentication DTOs"
Cohesion: 0.06
Nodes (26): AuthErrorResponse, AuthUserResponse, CurrentUserResponse, LoginRequest, PasswordResetConfirmRequest, PasswordResetRequest, RegisterRequest, TokenRequest (+18 more)

### Community 4 - "Match Detail Frontend"
Cohesion: 0.09
Nodes (64): escapeHtml(), formatDateTime(), formatItemEventVerb(), formatKdaValue(), formatMatchDuration(), formatMinute(), formatNumberValue(), formatParticipantRef() (+56 more)

### Community 5 - "Match Analysis Pipeline"
Cohesion: 0.07
Nodes (22): AnalysisController, PostMapping, RestController, Component, ConditionalOnProperty, Logger, Scheduled, MatchAnalysisScheduler (+14 more)

### Community 6 - "Build Aggregation Configuration"
Cohesion: 0.08
Nodes (23): ConfigurationProperties, EnableConfigurationProperties, ChampionBuildAggregationService, DefaultChampionBuildAggregationService, BuildConfiguration, Bean, Configuration, JdbcTemplate (+15 more)

### Community 7 - "Player Rank Persistence"
Cohesion: 0.08
Nodes (13): Entity, Table, LeagueEntryEntity, PlatformShard, EUN1, EUW1, RU, TR (+5 more)

### Community 8 - "Python Insights Service"
Cohesion: 0.08
Nodes (44): clear_old_insights(), find_stale_player_puuids(), generate_all_insights(), generate_candidate_insights(), generate_insights_for_player(), insight_sort_key(), limit_per_type(), limit_player_total() (+36 more)

### Community 9 - "Build API Queue"
Cohesion: 0.10
Nodes (17): PreparedStatement, BuildQueue, FLEX, SOLO_DUO, BuildScope, CHAMPION_ROLE, EXACT_MATCHUP, PatchWindow (+9 more)

### Community 10 - "Champion Page API"
Cohesion: 0.08
Nodes (21): ChampionController, GetMapping, RequestMapping, RestController, FrontendSearchController, GetMapping, RequestMapping, RestController (+13 more)

### Community 11 - "Player Dashboard API"
Cohesion: 0.09
Nodes (18): GetMapping, RequestMapping, RestController, PlayerController, ChampionPoolHealthDto, PlayerChampionStatsDto, PlayerDashboardDto, PlayerFreshnessDto (+10 more)

### Community 12 - "Account Token Persistence"
Cohesion: 0.10
Nodes (17): Lock, AccountActionTokenEntity, Entity, Table, AccountTokenType, EMAIL_VERIFICATION, PASSWORD_RESET, AccountActionTokenRepository (+9 more)

### Community 13 - "Riot API Client"
Cohesion: 0.12
Nodes (15): JsonNode, RiotApiClient, CrawlResultDto, CrawlerServiceImpl, Logger, Override, Service, TransactionTemplate (+7 more)

### Community 14 - "Timeline Event Persistence"
Cohesion: 0.07
Nodes (3): Entity, Table, MatchTimelineEventEntity

### Community 15 - "Build Aggregation Rules"
Cohesion: 0.13
Nodes (16): ExactKey, OpponentIdentity, ValidParticipant, BuildObservation, BuildRole, BOTTOM, JUNGLE, MIDDLE (+8 more)

### Community 16 - "Player Rank API"
Cohesion: 0.10
Nodes (20): GetMapping, PostMapping, ResponseEntity, RestController, PlayerRankController, LeagueEntryRepository, LeagueEntrySnapshotRepository, PlayerDashboardService (+12 more)

### Community 17 - "Account Token Mail"
Cohesion: 0.11
Nodes (12): AccountMailService, DuplicateEmailException, EmailRegistrationDisabledException, InvalidCredentialsException, PasswordEncoder, Service, PasswordAuthService, BeforeEach (+4 more)

### Community 18 - "Build Snapshot Model"
Cohesion: 0.20
Nodes (9): AggregatedCohort, AggregationResult, BaselineKey, BuildChoice, BuildSnapshotPayload, BuildSnapshotValidator, CohortKey, BuildSnapshotValidatorTest (+1 more)

### Community 19 - "Riot ID Resolution"
Cohesion: 0.12
Nodes (17): PostMapping, RequestMapping, RestController, RiotIdResolveController, RiotIdResolveRequest, RiotIdResolveResponse, Repository, PlayerRepository (+9 more)

### Community 20 - "Search API"
Cohesion: 0.13
Nodes (7): GetMapping, RequestMapping, RestController, SearchController, Entity, Table, PlayerEntity

### Community 21 - "Repository Proxy Startup"
Cohesion: 0.07
Nodes (33): Interface Repository Proxies, JDK Dynamic Proxy Requirement, Repository Proxy Configuration Test, Startup Proxy Fix, Startup Proxy Review Package, Champion Builds Completion Criteria, Champion Builds Final Verification, Environment-Blocked Verification Checks (+25 more)

### Community 22 - "Aggregation Outcome Flow"
Cohesion: 0.18
Nodes (6): AggregationOutcome, Override, BuildSourceSelection, ChampionBuildAggregationServiceTest, ExtendWith, Test

### Community 23 - "Match Search Repository"
Cohesion: 0.12
Nodes (11): Query, Repository, MatchRepository, DataIntegrityServiceImpl, JsonNode, Logger, Override, Service (+3 more)

### Community 24 - "League Snapshot Persistence"
Cohesion: 0.09
Nodes (3): Entity, Table, LeagueEntrySnapshotEntity

### Community 25 - "Champion Builds Frontend"
Cohesion: 0.19
Nodes (32): applyDefaults(), bindEvents(), cacheKey(), changePrimaryFilter(), element(), escapeHtml(), fetchBuild(), fetchOptions() (+24 more)

### Community 26 - "Match Data JPA Tests"
Cohesion: 0.13
Nodes (12): AutoConfigureTestDatabase, DataJpaTest, Entity, Table, MatchEntity, RegionRoute, europe, DirtiesContext (+4 more)

### Community 27 - "Build Aggregator Tests"
Cohesion: 0.11
Nodes (11): BuildAggregator, BuildComponentRanker, Counts, Ids, Override, BuildRules, BuildConfidence, HIGH (+3 more)

### Community 29 - "Champion Builds Delivery"
Cohesion: 0.08
Nodes (30): Idempotent Scheduled Aggregation, Champion Build API Contract, Champion Build API Implementation, Champion Build Browser Contract, Champion Build Browser Implementation, Responsive Build UI Contract, Accessible Build UI Verification, Swagger OpenAPI Documentation (+22 more)

### Community 30 - "Riot HTTP Client"
Cohesion: 0.14
Nodes (11): PostConstruct, Component, JsonNode, Logger, Override, ResponseEntity, RestTemplate, RiotApiHttpClient (+3 more)

### Community 31 - "Application User Persistence"
Cohesion: 0.20
Nodes (4): AppUserEntity, Entity, Table, Transactional

### Community 32 - "Data Integrity Tools"
Cohesion: 0.14
Nodes (13): DataIntegrityController, GetMapping, PostMapping, RestController, DataIntegrityReportDto, PlayerProfileRepairResultDto, RankRepairResultDto, DataIntegrityService (+5 more)

### Community 33 - "Transaction Configuration Tests"
Cohesion: 0.13
Nodes (15): AfterEach, AnnotationConfigApplicationContext, DataSource, EnableTransactionManagement, PlatformTransactionManager, FailurePoint, Bean, BeforeEach (+7 more)

### Community 34 - "Crawler API"
Cohesion: 0.13
Nodes (13): Operation, CrawlerController, Logger, PostMapping, RequestMapping, RestController, DatasetCrawlerController, GetMapping (+5 more)

### Community 35 - "OAuth Identity Security"
Cohesion: 0.15
Nodes (9): OAuthIdentityRepository, DiscordAccountException, DiscordAccountService, Service, Transactional, DiscordAccountServiceTest, BeforeEach, ExtendWith (+1 more)

### Community 36 - "Build Fallback Reasons"
Cohesion: 0.12
Nodes (13): BuildFallbackReason, AGGREGATION_FAILED_USING_LAST_PUBLISHED, DATA_UNAVAILABLE, MATCHUP_SAMPLE_TOO_SMALL, NONE, REQUESTED_PATCH_UNAVAILABLE, ChampionBuildResponse, ChampionBuildService (+5 more)

### Community 37 - "Build Scoring Rules"
Cohesion: 0.15
Nodes (3): BuildCandidate, BuildRulesTest, Test

### Community 38 - "Build Source Extraction"
Cohesion: 0.19
Nodes (7): BuildSourceMatch, JsonNode, Participant, BuildObservationFactoryTest, JsonNode, ObjectMapper, Test

### Community 39 - "Static Data API"
Cohesion: 0.16
Nodes (11): PostMapping, RestController, StaticDataController, DataDragonSyncService, DataDragonSyncServiceImpl, JdbcTemplate, JsonNode, ObjectMapper (+3 more)

### Community 40 - "CSRF Protection"
Cohesion: 0.16
Nodes (12): CsrfToken, CsrfController, CsrfResponse, GetMapping, RequestMapping, RestController, AuthControllerWebMvcTest, Import (+4 more)

### Community 41 - "Build Source Repository"
Cohesion: 0.15
Nodes (12): JdbcTemplate, JsonNode, ObjectMapper, Override, Repository, ResultSet, JdbcBuildSourceRepository, MatchRow (+4 more)

### Community 42 - "User Session Repository"
Cohesion: 0.16
Nodes (12): SecureRandom, Modifying, Query, UserSessionRepository, Component, OpaqueTokenCodec, Service, Transactional (+4 more)

### Community 43 - "User Session Persistence"
Cohesion: 0.15
Nodes (4): Entity, Table, UserSessionEntity, Test

### Community 44 - "Discord OAuth Handlers"
Cohesion: 0.15
Nodes (15): AuthenticationException, AuthenticationFailureHandler, AuthenticationSuccessHandler, ResponseCookie, DiscordAuthenticationSuccessHandler, Authentication, Component, HttpServletRequest (+7 more)

### Community 45 - "Global Error Handling"
Cohesion: 0.25
Nodes (12): HttpStatus, MethodArgumentNotValidException, RestControllerAdvice, ApiErrorResponse, GlobalExceptionHandler, ExceptionHandler, HttpServletRequest, Logger (+4 more)

### Community 46 - "Build Display Assets"
Cohesion: 0.19
Nodes (10): DisplayAsset, JdbcTemplate, Override, Repository, JdbcBuildAssetRepository, BeforeEach, JdbcTemplate, SuppressWarnings (+2 more)

### Community 47 - "Build Snapshot Publication"
Cohesion: 0.33
Nodes (3): BuildLookup, BuildPublisherIT, Test

### Community 48 - "Balanced Dataset Crawler"
Cohesion: 0.16
Nodes (7): ExternalServiceException, BalancedDatasetCrawlerServiceImpl, JsonNode, Logger, ObjectMapper, Override, Service

### Community 50 - "OAuth User Profile"
Cohesion: 0.17
Nodes (4): Entity, Table, OAuthIdentityEntity, AppPrincipal

### Community 51 - "Summoner Persistence"
Cohesion: 0.10
Nodes (5): Entity, Table, SummonerEntity, Repository, SummonerRepository

### Community 52 - "Ingest Log Persistence"
Cohesion: 0.09
Nodes (3): IngestLogEntity, Entity, Table

### Community 53 - "Refresh Coordination"
Cohesion: 0.20
Nodes (9): PlayerRefreshJobRepository, Service, TaskExecutor, PlayerRefreshCoordinator, BeforeEach, ExtendWith, TaskExecutor, Test (+1 more)

### Community 54 - "Security Filter Chain"
Cohesion: 0.17
Nodes (13): HttpSecurity, SecurityFilterChain, AppSessionAuthenticationFilter, Component, FilterChain, HttpServletRequest, HttpServletResponse, Override (+5 more)

### Community 55 - "Item Catalog Rules"
Cohesion: 0.25
Nodes (7): ItemDefinition, JdbcTemplate, JsonNode, ObjectMapper, Override, Repository, JdbcItemCatalog

### Community 56 - "Frontend API Client"
Cohesion: 0.18
Nodes (13): api, ApiRequestError, appendQueryParams(), buildApiUrl(), buildRequestHeaders(), ensureCsrfToken(), fetchJson(), getConfiguredApiBaseUrl() (+5 more)

### Community 57 - "Crawler Cucumber Tests"
Cohesion: 0.20
Nodes (7): Given, CrawlSteps, TransactionTemplate, CrawlerServiceImplPaginationTest, Test, Then, When

### Community 58 - "Timeline Frame Persistence"
Cohesion: 0.16
Nodes (4): IdClass, Entity, Table, MatchTimelineFrameEntity

### Community 59 - "SMTP Account Mail"
Cohesion: 0.20
Nodes (10): JavaMailSender, ObjectProvider, Override, Service, SmtpAccountMailService, ExtendWith, JavaMailSender, ObjectProvider (+2 more)

### Community 60 - "Saved Profile Service"
Cohesion: 0.20
Nodes (9): Query, SavedProfileRepository, Component, ConditionalOnProperty, Scheduled, SavedProfileRefreshScheduler, ExtendWith, Test (+1 more)

### Community 61 - "Timeline Repository Cleanup"
Cohesion: 0.22
Nodes (8): MatchTimelineFrameRepository, Repository, MatchTimelineRawRepository, JsonNode, Logger, ObjectMapper, Service, TimelineIngestServiceImpl

### Community 62 - "Security MVC Tests"
Cohesion: 0.20
Nodes (11): GetMapping, Import, MockMvc, PostMapping, ResponseEntity, RestController, Test, UsernamePasswordAuthenticationToken (+3 more)

### Community 63 - "OAuth User Service"
Cohesion: 0.24
Nodes (9): DefaultOAuth2UserService, GrantedAuthority, OAuth2User, OAuth2UserRequest, OAuth2UserService, DiscordOAuth2User, DiscordOAuth2UserService, Override (+1 more)

### Community 64 - "Build Options API"
Cohesion: 0.20
Nodes (5): ChampionBuildOptionsResponse, OpponentOption, PatchOption, QueueOption, RoleOption

### Community 65 - "Docs API Client"
Cohesion: 0.20
Nodes (10): api, appendQueryParams(), buildApiUrl(), buildRequestHeaders(), fetchJson(), getConfiguredApiBaseUrl(), isNgrokUrl(), NGROK_HOST_MARKERS (+2 more)

### Community 66 - "Refresh Job Persistence"
Cohesion: 0.18
Nodes (6): HttpStatusCodeException, Entity, Table, Service, PlayerRefreshWorker, CrawlerService

### Community 67 - "Aggregation Status Model"
Cohesion: 0.18
Nodes (10): Status, FAILED, INSUFFICIENT_SOURCE_DATA, NO_CHANGE, PUBLISHED, CrawlerControllerWebMvcTest, Import, MockMvc (+2 more)

### Community 68 - "Build Asset Repository"
Cohesion: 0.16
Nodes (4): BuildAssetRepository, BeforeEach, Test, RepositoryProxyConfigurationTest

### Community 69 - "Player Dashboard DTOs"
Cohesion: 0.20
Nodes (5): PlayerSummaryDto, BeforeEach, ExtendWith, Test, PlayerDashboardServiceTest

### Community 70 - "Refresh Status DTOs"
Cohesion: 0.15
Nodes (12): RefreshSource, MANUAL, RESOLVE, SCHEDULED, RefreshState, COMPLETED, FAILED, QUEUED (+4 more)

### Community 71 - "Homepage Meta Snapshot"
Cohesion: 0.25
Nodes (12): buildMetaCard(), buildMetaSnapshotCards(), escapeHtml(), formatHomeValue(), formatKda(), getPlayerDisplayName(), initializeRiotIdForm(), renderChampionTable() (+4 more)

### Community 72 - "Saved Profile MVC Tests"
Cohesion: 0.29
Nodes (6): Import, MockMvc, Test, UsernamePasswordAuthenticationToken, WebMvcTest, SavedProfileControllerWebMvcTest

### Community 73 - "Parameterized Build Tests"
Cohesion: 0.22
Nodes (6): Arguments, CsvSource, MethodSource, NullSource, ParameterizedTest, ValueSource

### Community 74 - "Refresh Cooldown API"
Cohesion: 0.22
Nodes (8): RefreshCooldownException, ExceptionHandler, GetMapping, PostMapping, RequestMapping, ResponseEntity, RestController, PlayerRefreshController

### Community 75 - "Champion Builds Architecture"
Cohesion: 0.18
Nodes (13): Atomic Snapshot Publication, Backend Calculation Ownership, Champion Builds Design, Champion-Role Baseline, Explicit Build Fallback Ladder, Prepared Build Snapshots, Threshold-Gated Exact Matchups, Champion Matchup Builds Vertical Slice (+5 more)

### Community 76 - "Ingest Logging Service"
Cohesion: 0.31
Nodes (5): IngestLogServiceImpl, JdbcTemplate, Logger, Override, Service

### Community 77 - "Account Frontend"
Cohesion: 0.29
Nodes (12): bindAccountForm(), bindSavedProfileActions(), bootstrapAccountHeader(), escapeAccountHtml(), initializeAccountForms(), initializeTokenAction(), renderAccountEntry(), renderSavedProfiles() (+4 more)

### Community 78 - "Website Redesign System"
Cohesion: 0.17
Nodes (12): Analyst Console, Purple and Cream Visual System, Static Frontend Conservation, Three-Level Surface Ladder, Website Redesign Design, Utilitarian Internal Research Pages, Riot Stats Champions Page, Role-Filtered Champion Directory (+4 more)

### Community 79 - "User Account Status"
Cohesion: 0.24
Nodes (10): AppUserStatus, ACTIVE, DISABLED, PENDING_VERIFICATION, AccountRepositoryIT, DirtiesContext, PostgreSQLContainer, SpringBootTest (+2 more)

### Community 80 - "Ingest Log Service"
Cohesion: 0.29
Nodes (3): IngestLogService, Override, Transactional

### Community 81 - "Champion Detail Frontend"
Cohesion: 0.27
Nodes (10): escapeHtml(), formatChampionDisplayName(), formatDecimal(), formatRoleLabel(), getAbilityDisplayName(), getItemDisplayName(), renderChampionAbilities(), renderChampionHero() (+2 more)

### Community 82 - "Champion List Frontend"
Cohesion: 0.33
Nodes (10): buildRoleIcon(), compareChampions(), escapeHtml(), formatChampionDisplayName(), formatRoleLabel(), getRoleSortWeight(), renderChampionListMeta(), renderChampions() (+2 more)

### Community 83 - "Raw Timeline Persistence"
Cohesion: 0.27
Nodes (3): Entity, Table, MatchTimelineRawEntity

### Community 84 - "Cucumber Spring Setup"
Cohesion: 0.36
Nodes (9): CucumberContextConfiguration, EnableAutoConfiguration, SpringBootConfiguration, CucumberSpringConfig, Import, JdbcTemplate, SpringBootTest, TransactionTemplate (+1 more)

### Community 85 - "Player Product Roadmap"
Cohesion: 0.20
Nodes (10): Insight Quality Over Feature Count, Personal Ranked Improvement Companion, Player-Facing Incremental Delivery, Post-Match Return Loop, Riot Stats Player Product Development Plan, Analyzed Insight Data Contract, ML Insights Module, Rule-Based Player Insight Generator (+2 more)

### Community 86 - "JPA Repository Interfaces"
Cohesion: 0.29
Nodes (5): JpaRepository, IngestLogRepository, Repository, Repository, MatchTimelineEventRepository

### Community 87 - "Maven Wrapper"
Cohesion: 0.33
Nodes (6): mvnw script, clean(), die(), exec_maven(), set_java_home(), verbose()

### Community 88 - "Token Concurrency Tests"
Cohesion: 0.36
Nodes (7): AppUserRepository, AccountTokenConcurrencyIT, DirtiesContext, PostgreSQLContainer, SpringBootTest, Test, Testcontainers

### Community 89 - "Build Scheduler"
Cohesion: 0.27
Nodes (5): ChampionBuildScheduler, Component, ConditionalOnProperty, Scheduled, fromId()

### Community 90 - "Timeline Frame Identity"
Cohesion: 0.22
Nodes (3): Override, MatchTimelineFrameId, Repository

### Community 91 - "Refresh Controller Tests"
Cohesion: 0.36
Nodes (5): Import, MockMvc, Test, WebMvcTest, PlayerRefreshControllerWebMvcTest

### Community 93 - "Build Dashboard UX"
Cohesion: 0.25
Nodes (9): Build Dashboard, Matchup Comparison Matrix, Matchup-First Workspace, Desktop Matchup-First Mobile Dashboard Recommendation, Confidence-First Matchup UI, Field Guide Layout, Matchup Lens Layout, Scout Layout (+1 more)

### Community 94 - "Docs Player Frontend"
Cohesion: 0.39
Nodes (7): escapeHtml(), formatDecimal(), formatKda(), renderPlayerHero(), renderPlayerInsights(), renderPlayerMatches(), renderPlayerStats()

### Community 95 - "Player Loop Architecture"
Cohesion: 0.22
Nodes (9): Aggregated Player Dashboard, Durable Refresh Coordination, Opaque Application Sessions, Player Loop Implementation Plan, Public-First Player Loop, Freshness as a Product Feature, Queue-Separated Player Dashboard, Riot Stats Player Page (+1 more)

### Community 96 - "Request Logging Filter"
Cohesion: 0.36
Nodes (7): OncePerRequestFilter, Component, FilterChain, HttpServletRequest, HttpServletResponse, Override, RequestLoggingFilter

### Community 97 - "Refresh Concurrency Tests"
Cohesion: 0.39
Nodes (6): DirtiesContext, PostgreSQLContainer, SpringBootTest, Test, Testcontainers, PlayerRefreshConcurrencyIT

### Community 98 - "Localization Configuration"
Cohesion: 0.43
Nodes (5): AcceptHeaderLocaleResolver, Bean, Configuration, MessageSource, LocaleConfig

### Community 99 - "Editorial Design System"
Cohesion: 0.25
Nodes (8): Conservative Mobile Density, First Viewport Tells a Story, Issue Cover and Research Tools, Premium Black Visual System, Riot Stats Editorial Reset Design, Local Meta Overview, Public Riot ID Entry Flow, Riot Stats Home Page

### Community 100 - "Build Source Integration"
Cohesion: 0.43
Nodes (5): BeforeEach, JdbcTemplate, PostgreSQLContainer, Testcontainers, JdbcBuildSourceRepositoryIT

### Community 101 - "Match API"
Cohesion: 0.36
Nodes (4): GetMapping, RequestMapping, RestController, MatchController

### Community 102 - "Player Migration Tests"
Cohesion: 0.43
Nodes (6): JdbcTemplate, PostgreSQLContainer, SpringBootTest, Test, Testcontainers, PlayerLoopMigrationIT

### Community 103 - "Build Controller Tests"
Cohesion: 0.43
Nodes (5): ChampionBuildControllerTest, Import, MockMvc, Test, WebMvcTest

### Community 104 - "Build Migration Tests"
Cohesion: 0.43
Nodes (6): ChampionBuildMigrationIT, JdbcTemplate, PostgreSQLContainer, SpringBootTest, Test, Testcontainers

### Community 106 - "Optional Account Design"
Cohesion: 0.33
Nodes (7): Optional Saved Profiles, Player Loop and Champion Matchup Builds Design, Player Loop Vertical Slice, Public Browsability with Optional Accounts, Optional Account Access, Riot Stats Account Page, Saved Profile Bookmarks

### Community 107 - "Patch Version Model"
Cohesion: 0.38
Nodes (3): Pattern, Override, PatchVersion

### Community 108 - "Build Controller API"
Cohesion: 0.43
Nodes (4): ChampionBuildController, GetMapping, RequestMapping, RestController

### Community 110 - "Analysis Status Model"
Cohesion: 0.29
Nodes (6): AnalysisStatus, ANALYZED, FAILED, NEW, PROCESSING, WAITING_FOR_TIMELINE

### Community 111 - "Player Schema Migration"
Cohesion: 0.48
Nodes (6): app.account_action_token, app.app_user, app.oauth_identity, app.player_refresh_job, app.saved_profile, app.user_session

### Community 114 - "Build Source Fixtures"
Cohesion: 0.29
Nodes (6): core.matches, core.participant_final_items, core.participant_skill_order, core.participants, raw.match_timeline_raw, static.items

### Community 115 - "Spring Boot Entry"
Cohesion: 0.53
Nodes (4): EnableScheduling, SpringBootApplication, Logger, RiotPractice

### Community 116 - "Web CORS Configuration"
Cohesion: 0.53
Nodes (4): Bean, Configuration, WebConfig, WebMvcConfigurer

### Community 117 - "Dashboard Hero Artwork"
Cohesion: 0.47
Nodes (6): Central Negative Space for Hero Content, Cool-to-Warm Competitive Color Split, Dashboard Hero V2, Opposing Blue and Red Team Bases, Summoner's Rift Battlefield, Tactical Analytics Overlays

### Community 118 - "Match Map Artwork"
Cohesion: 0.47
Nodes (6): Blue and Red Timeline Event Markers, Lane Tower Positions, Match Timeline Map, Opposing Team Bases, River and Jungle Zones, Three-Lane Map Topology

### Community 119 - "Player Leaderboard Frontend"
Cohesion: 0.60
Nodes (5): escapeHtml(), formatKda(), getPlayerDisplayName(), renderLeaderboardError(), renderLeaderboardTable()

### Community 120 - "Cucumber Test Suite"
Cohesion: 0.70
Nodes (4): ConfigurationParameter, SelectClasspathResource, CucumberTest, Suite

### Community 122 - "IntelliJ Proxy Fix"
Cohesion: 0.40
Nodes (5): IntelliJ Startup Proxy Fix Implementation Plan, Repository Proxy Configuration Regression, CGLIB Final Repository Failure, IntelliJ Startup Proxy Fix, JDK Interface Proxy Decision

### Community 123 - "Project Architecture Overview"
Cohesion: 0.50
Nodes (4): Player Improvement Loop, Java Spring PostgreSQL Stack, Riot API Data Pipeline, RiotApiProject

### Community 125 - "CI Security Pipeline"
Cohesion: 0.67
Nodes (3): CI Pipeline, OWASP Dependency Check, Unit and Cucumber Test Suite

## Knowledge Gaps
- **102 isolated node(s):** `NGROK_HOST_MARKERS`, `api`, `backup-run.sh script`, `dev-run.sh script`, `prod-run.sh script` (+97 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `NotFoundException` connect `Saved Profile API` to `Champion Match Metrics`, `Crawler API`, `Build Fallback Reasons`, `Build Asset Repository`, `Build Controller Tests`, `Saved Profile MVC Tests`, `Build API Queue`, `Champion Page API`, `Global Error Handling`, `Riot API Client`, `Riot ID Resolution`, `Refresh Coordination`, `Refresh Controller Tests`?**
  _High betweenness centrality (0.109) - this node is a cross-community bridge._
- **Why does `SessionService` connect `User Session Repository` to `Account Authentication DTOs`, `Aggregation Status Model`, `Build Controller Tests`, `CSRF Protection`, `Saved Profile MVC Tests`, `User Session Persistence`, `Discord OAuth Handlers`, `Player Rank API`, `Account Token Mail`, `Security Filter Chain`, `Token Concurrency Tests`, `Refresh Controller Tests`, `Security MVC Tests`?**
  _High betweenness centrality (0.076) - this node is a cross-community bridge._
- **Why does `PlayerRepository` connect `Riot ID Resolution` to `Saved Profile API`, `Crawler API`, `Aggregation Status Model`, `Player Dashboard DTOs`, `Champion Page API`, `Player Dashboard API`, `Riot API Client`, `Balanced Dataset Crawler`, `Search API`, `Cucumber Spring Setup`, `JPA Repository Interfaces`, `Match Search Repository`, `Saved Profile Service`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `PatchWindow` (e.g. with `.annualPatchWindowUsesTheHighestStoredPatchFromThePreviousMajor()` and `.minorPatchWindowUsesTheExactAdjacentPatchEvenWhenItIsNotStored()`) actually correct?**
  _`PatchWindow` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `LeagueEntryEntity` (e.g. with `.calculatesRecentWindowsChampionHealthPrioritiesAndFreshness()` and `.usesFlexDataWithoutMixingItWithSoloDuo()`) actually correct?**
  _`LeagueEntryEntity` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `NGROK_HOST_MARKERS`, `api`, `backup-run.sh script` to the rest of the system?**
  _102 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Champion Match Metrics` be split into smaller, more focused modules?**
  _Cohesion score 0.057703081232493 - nodes in this community are weakly interconnected._