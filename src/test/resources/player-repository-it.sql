create schema if not exists raw;

create table raw.players (
    puuid varchar(128) primary key,
    game_name varchar(128),
    tag_line varchar(64),
    profile_icon_id integer,
    summoner_level integer,
    profile_synced_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_crawl_attempt_at timestamptz
);

create table raw.league_entries (
    id bigserial primary key,
    puuid varchar(128)
);

insert into raw.players (puuid, profile_icon_id, created_at, updated_at) values
    ('profile-missing-old', null, now() - interval '2 hours', now() - interval '2 hours'),
    ('profile-missing-new', null, now() - interval '1 hour', now() - interval '1 hour'),
    ('profile-present', 10, now(), now()),
    ('rank-present', null, now(), now());

insert into raw.league_entries (puuid) values ('rank-present');
