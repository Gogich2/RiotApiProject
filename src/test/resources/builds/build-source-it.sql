DROP SCHEMA IF EXISTS core CASCADE;
DROP SCHEMA IF EXISTS raw CASCADE;
DROP SCHEMA IF EXISTS static CASCADE;
CREATE SCHEMA core;
CREATE SCHEMA raw;
CREATE SCHEMA static;

CREATE TABLE core.matches (
  match_id varchar(64) PRIMARY KEY,
  region varchar(16) NOT NULL,
  platform varchar(16) NOT NULL,
  game_duration_ms bigint NOT NULL,
  game_version varchar(32) NOT NULL,
  queue_id integer NOT NULL,
  fetched_at timestamptz NOT NULL
);
CREATE TABLE core.participants (
  match_id varchar(64) NOT NULL,
  participant_id integer NOT NULL,
  team_id integer,
  champion_id integer,
  team_position varchar(32),
  individual_position varchar(32),
  win boolean,
  summoner1_id integer,
  summoner2_id integer,
  perks_json jsonb,
  PRIMARY KEY (match_id, participant_id)
);
CREATE TABLE core.participant_final_items (
  match_id varchar(64) NOT NULL,
  participant_id integer NOT NULL,
  item_slot integer NOT NULL,
  item_id integer,
  PRIMARY KEY (match_id, participant_id, item_slot)
);
CREATE TABLE core.participant_skill_order (
  match_id varchar(64) NOT NULL,
  participant_id integer NOT NULL,
  skill_order integer NOT NULL,
  skill_slot integer,
  PRIMARY KEY (match_id, participant_id, skill_order)
);
CREATE TABLE raw.match_timeline_raw (
  match_id varchar(64) PRIMARY KEY,
  raw_timeline_json jsonb,
  fetched_at timestamptz NOT NULL
);
CREATE TABLE static.items (
  item_id integer NOT NULL,
  version varchar(32) NOT NULL,
  tags jsonb,
  maps jsonb,
  raw_json jsonb,
  PRIMARY KEY (item_id, version)
);

INSERT INTO core.matches VALUES
 ('EUW1_a','europe','EUW1',600000,'16.12.1',420,'2026-07-01T00:00:00Z'),
 ('EUW1_b','europe','EUW1',700000,'16.13.2',420,'2026-07-02T00:00:00Z'),
 ('EUW1_c','europe','EUW1',700000,'15.24.1',420,'2026-06-01T00:00:00Z'),
 ('EUW1_bad_region','americas','EUW1',700000,'16.13.1',420,'2026-07-03T00:00:00Z'),
 ('EUN1_bad_platform','europe','EUN1',700000,'16.13.1',420,'2026-07-03T00:00:00Z'),
 ('EUW1_short','europe','EUW1',599999,'16.13.1',420,'2026-07-03T00:00:00Z'),
 ('EUW1_flex','europe','EUW1',700000,'16.13.1',440,'2026-07-03T00:00:00Z'),
 ('EUW1_old','europe','EUW1',700000,'16.11.9',420,'2026-07-03T00:00:00Z');

INSERT INTO core.participants VALUES
 ('EUW1_a',1,100,11,'TOP',NULL,true,4,14,
  '{"styles":[{"description":"primaryStyle","style":8000,"selections":[{"perk":8005},{"perk":9111},{"perk":9104},{"perk":8014}]},{"description":"subStyle","style":8300,"selections":[{"perk":8304},{"perk":8347}]}],"statPerks":{"offense":5005,"flex":5008,"defense":5002}}'),
 ('EUW1_a',2,200,22,'TOP',NULL,false,4,12,
  '{"styles":[{"description":"primaryStyle","style":8000,"selections":[{"perk":8005},{"perk":9111},{"perk":9104},{"perk":8014}]},{"description":"subStyle","style":8300,"selections":[{"perk":8304},{"perk":8347}]}],"statPerks":{"offense":5005,"flex":5008,"defense":5002}}');
INSERT INTO core.participant_final_items VALUES
 ('EUW1_a',1,0,1055),('EUW1_a',1,1,3006),('EUW1_a',1,2,6672),
 ('EUW1_a',2,0,1055),('EUW1_a',2,1,3006),('EUW1_a',2,2,6672);
INSERT INTO core.participant_skill_order VALUES
 ('EUW1_a',1,1,1),('EUW1_a',1,2,2),('EUW1_a',1,3,3),
 ('EUW1_a',2,1,1),('EUW1_a',2,2,2),('EUW1_a',2,3,3);
INSERT INTO raw.match_timeline_raw VALUES
 ('EUW1_a','{"info":{"frames":[{"events":[{"type":"ITEM_PURCHASED","participantId":1,"itemId":1055,"timestamp":1000}]}]}}','2026-07-01T00:01:00Z'),
 ('EUW1_b','{"info":{"frames":[]}}','2026-07-02T00:01:00Z');

INSERT INTO static.items VALUES
 (6672,'16.12.1','["Damage"]','{"11":true}','{"gold":{"purchasable":true},"into":[]}'),
 (9999,'16.11.1','["Damage"]','{"11":true}','{"gold":{"purchasable":true},"into":[]}'),
 (1001,'16.12.1','["Boots"]','{"11":true}','{"gold":{"purchasable":true},"into":["3006"]}'),
 (3006,'16.12.1','["Boots"]','{"11":true}','{"gold":{"purchasable":true},"into":[]}');
