create schema if not exists builds;

create table builds.aggregation_run (
    id uuid primary key,
    aggregation_version integer not null,
    anchor_patch varchar(16) not null,
    comparison_patch varchar(16) not null,
    queue_id integer not null,
    input_watermark timestamptz not null,
    state varchar(16) not null,
    source_match_count integer not null,
    validation_count integer not null,
    snapshot_count integer not null,
    failure_category varchar(64),
    started_at timestamptz not null,
    completed_at timestamptz,
    constraint ck_aggregation_run_queue check (queue_id in (420, 440)),
    constraint ck_aggregation_run_state check (state in ('RUNNING', 'COMPLETED', 'FAILED'))
);

create table builds.champion_build_snapshot (
    id uuid primary key,
    run_id uuid not null references builds.aggregation_run(id),
    aggregation_version integer not null,
    payload_schema_version integer not null,
    anchor_patch varchar(16) not null,
    comparison_patch varchar(16) not null,
    queue_id integer not null,
    champion_id integer not null,
    role varchar(16) not null,
    opponent_champion_id integer,
    scope varchar(32) not null,
    games integer not null,
    wins integer not null,
    anchor_games integer not null,
    comparison_games integer not null,
    confidence varchar(16) not null,
    input_watermark timestamptz not null,
    source_match_count integer not null,
    calculated_at timestamptz not null,
    published_at timestamptz,
    publication_state varchar(16) not null,
    payload jsonb not null,
    constraint ck_champion_build_snapshot_queue check (queue_id in (420, 440)),
    constraint ck_champion_build_snapshot_role check (
        role in ('TOP', 'JUNGLE', 'MIDDLE', 'BOTTOM', 'UTILITY')
    ),
    constraint ck_champion_build_snapshot_scope check (
        scope in ('CHAMPION_ROLE', 'EXACT_MATCHUP')
    ),
    constraint ck_champion_build_snapshot_confidence check (
        confidence in ('INSUFFICIENT', 'LOW', 'MEDIUM', 'HIGH')
    ),
    constraint ck_champion_build_snapshot_publication_state check (
        publication_state in ('PENDING', 'PUBLISHED', 'ARCHIVED')
    ),
    constraint uq_champion_build_snapshot_run_cohort unique nulls not distinct (
        run_id,
        aggregation_version,
        anchor_patch,
        comparison_patch,
        queue_id,
        champion_id,
        role,
        opponent_champion_id
    )
);

create unique index uq_aggregation_run_running_window
    on builds.aggregation_run (
        aggregation_version,
        anchor_patch,
        comparison_patch,
        queue_id
    )
    where state = 'RUNNING';

create unique index uq_champion_build_snapshot_published_cohort
    on builds.champion_build_snapshot (
        aggregation_version,
        anchor_patch,
        comparison_patch,
        queue_id,
        champion_id,
        role,
        opponent_champion_id
    ) nulls not distinct
    where publication_state = 'PUBLISHED';

create index ix_champion_build_snapshot_published_lookup
    on builds.champion_build_snapshot (
        queue_id,
        anchor_patch,
        champion_id,
        role,
        opponent_champion_id,
        aggregation_version,
        comparison_patch
    )
    where publication_state = 'PUBLISHED';
