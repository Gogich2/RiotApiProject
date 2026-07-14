do $migration$
begin
    if to_regclass('raw.players') is not null then
        execute 'alter table raw.players '
            || 'add column if not exists last_crawl_attempt_at timestamptz';
        execute 'create index if not exists ix_players_crawl_rotation '
            || 'on raw.players '
            || '(last_crawl_attempt_at asc nulls first, created_at asc, puuid asc)';
    end if;
end
$migration$;
