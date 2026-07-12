create table app.app_user (
    id uuid primary key,
    email_normalized varchar(320) not null,
    password_hash varchar(100),
    display_name varchar(60) not null,
    status varchar(32) not null,
    email_verified_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uq_app_user_email unique (email_normalized),
    constraint ck_app_user_status check (
        status in ('PENDING_VERIFICATION', 'ACTIVE', 'DISABLED')
    )
);

create table app.oauth_identity (
    id uuid primary key,
    user_id uuid not null references app.app_user(id) on delete cascade,
    provider varchar(32) not null,
    provider_subject_id varchar(128) not null,
    created_at timestamptz not null,
    last_login_at timestamptz not null,
    constraint uq_oauth_identity_provider_subject unique (provider, provider_subject_id)
);

create index ix_oauth_identity_user on app.oauth_identity(user_id);

create table app.user_session (
    id uuid primary key,
    user_id uuid not null references app.app_user(id) on delete cascade,
    token_hash varchar(64) not null,
    expires_at timestamptz not null,
    last_used_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null,
    constraint uq_user_session_token_hash unique (token_hash)
);

create index ix_user_session_user on app.user_session(user_id);
create index ix_user_session_expiry on app.user_session(expires_at);

create table app.account_action_token (
    id uuid primary key,
    user_id uuid not null references app.app_user(id) on delete cascade,
    token_type varchar(32) not null,
    token_hash varchar(64) not null,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null,
    constraint uq_account_action_token_hash unique (token_hash),
    constraint ck_account_action_token_type check (
        token_type in ('EMAIL_VERIFICATION', 'PASSWORD_RESET')
    )
);

create index ix_account_action_token_user_type
    on app.account_action_token(user_id, token_type);

create table app.saved_profile (
    id uuid primary key,
    user_id uuid not null references app.app_user(id) on delete cascade,
    puuid varchar(128) not null,
    personal_label varchar(80),
    is_default boolean not null default false,
    saved_at timestamptz not null,
    last_viewed_at timestamptz not null,
    constraint uq_saved_profile_user_puuid unique (user_id, puuid)
);

create index ix_saved_profile_user_last_viewed
    on app.saved_profile(user_id, last_viewed_at desc);
create index ix_saved_profile_recent_activity
    on app.saved_profile(last_viewed_at desc);
create unique index uq_saved_profile_one_default
    on app.saved_profile(user_id)
    where is_default;

create table app.player_refresh_job (
    id uuid primary key,
    puuid varchar(128) not null,
    source varchar(16) not null,
    state varchar(16) not null,
    requested_at timestamptz not null,
    started_at timestamptz,
    completed_at timestamptz,
    retry_after timestamptz,
    failure_category varchar(64),
    user_message varchar(240),
    constraint ck_player_refresh_source check (source in ('MANUAL', 'SCHEDULED', 'RESOLVE')),
    constraint ck_player_refresh_state check (
        state in ('QUEUED', 'RUNNING', 'COMPLETED', 'RATE_LIMITED', 'FAILED')
    )
);

create index ix_player_refresh_job_puuid_requested
    on app.player_refresh_job(puuid, requested_at desc);
create unique index uq_player_refresh_active
    on app.player_refresh_job(puuid)
    where state in ('QUEUED', 'RUNNING');
