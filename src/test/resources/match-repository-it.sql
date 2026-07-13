CREATE TYPE region_route AS ENUM ('europe');
CREATE TYPE platform_shard AS ENUM ('EUW1', 'EUN1', 'RU', 'TR');

CREATE SCHEMA raw;

CREATE TABLE raw.matches (
    match_id VARCHAR(64) PRIMARY KEY,
    region region_route NOT NULL,
    platform platform_shard,
    raw_match_json JSONB,
    fetched_at TIMESTAMPTZ NOT NULL
);
