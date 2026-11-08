-- Migration 18: Create sports_fixtures for Sports Hub and Live Matches
begin;

create table if not exists public.sports_fixtures (
  id text primary key,
  sport text not null default 'football',
  competition text not null,
  competition_logo_url text,
  home_team text not null,
  away_team text not null,
  home_team_logo_url text,
  away_team_logo_url text,
  kickoff_utc timestamptz not null,
  broadcaster_official text not null,
  status text not null default 'scheduled' check (status in ('scheduled', 'live', 'finished', 'postponed')),
  minute text,
  home_score int,
  away_score int,
  venue text,
  updated_at timestamptz not null default now()
);

create index if not exists idx_sports_fixtures_kickoff_status
  on public.sports_fixtures (kickoff_utc asc, status);

create index if not exists idx_sports_fixtures_sport
  on public.sports_fixtures (sport, kickoff_utc asc);

alter table public.sports_fixtures enable row level security;

drop policy if exists sports_fixtures_read_all on public.sports_fixtures;
create policy sports_fixtures_read_all on public.sports_fixtures
  for select
  using (true);

grant select on public.sports_fixtures to anon, authenticated;

commit;
