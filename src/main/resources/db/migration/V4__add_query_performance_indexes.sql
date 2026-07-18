do $$
begin
    if to_regclass('raw.matches') is not null then
        execute 'create index if not exists ix_raw_matches_analysis_queue
                 on raw.matches (analysis_status, fetched_at)
                 where raw_match_json is not null';
    end if;

    if to_regclass('core.participants') is not null then
        execute 'create index if not exists ix_core_participants_puuid_match
                 on core.participants (puuid, match_id)';
        execute 'create index if not exists ix_core_participants_champion
                 on core.participants (champion_id)';
    end if;

    if to_regclass('raw.match_timeline_frames') is not null then
        execute 'create index if not exists ix_timeline_frames_match
                 on raw.match_timeline_frames (match_id)';
    end if;

    if to_regclass('raw.match_timeline_events') is not null then
        execute 'create index if not exists ix_timeline_events_match
                 on raw.match_timeline_events (match_id)';
    end if;

    if to_regclass('raw.players') is not null then
        execute 'create index if not exists ix_raw_players_missing_profile
                 on raw.players (updated_at, puuid)
                 where profile_icon_id is null and puuid is not null and puuid <> ''''';
    end if;

    if to_regclass('raw.league_entries') is not null then
        execute 'create index if not exists ix_raw_league_entries_puuid
                 on raw.league_entries (puuid)';
    end if;
end
$$;
