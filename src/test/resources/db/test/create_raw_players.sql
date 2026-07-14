create schema if not exists raw;

create table raw.players (
    puuid varchar(128) primary key,
    game_name varchar(128),
    tag_line varchar(64),
    profile_icon_id integer,
    summoner_level integer,
    profile_synced_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null
);
