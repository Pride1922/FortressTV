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

-- Seed fixture examples for immediate validation (current week)
insert into public.sports_fixtures (
  id, sport, competition, competition_logo_url, home_team, away_team,
  home_team_logo_url, away_team_logo_url, kickoff_utc, broadcaster_official,
  status, minute, home_score, away_score, venue
) values
(
  'fixture-pl-2026-benfica-porto',
  'football',
  'Primeira Liga',
  'https://upload.wikimedia.org/wikipedia/pt/e/ee/Liga_Portugal_Betclic_logo.png',
  'SL Benfica',
  'FC Porto',
  'https://upload.wikimedia.org/wikipedia/pt/2/2a/Sport_Lisboa_e_Benfica_logo.png',
  'https://upload.wikimedia.org/wikipedia/pt/1/1b/FC_Porto.png',
  now() + interval '2 hours',
  'Sport TV 1',
  'scheduled',
  null,
  null,
  null,
  'Estádio da Luz, Lisboa'
),
(
  'fixture-pl-2026-sporting-braga',
  'football',
  'Primeira Liga',
  'https://upload.wikimedia.org/wikipedia/pt/e/ee/Liga_Portugal_Betclic_logo.png',
  'Sporting CP',
  'SC Braga',
  'https://upload.wikimedia.org/wikipedia/pt/1/13/Sporting_Clube_de_Portugal_%28logo%29.png',
  'https://upload.wikimedia.org/wikipedia/pt/7/79/Sporting_Clube_de_Braga_logo.png',
  now() + interval '5 hours',
  'Sport TV 1',
  'scheduled',
  null,
  null,
  null,
  'Estádio José Alvalade, Lisboa'
),
(
  'fixture-ucl-2026-arsenal-realmadrid',
  'football',
  'UEFA Champions League',
  'https://upload.wikimedia.org/wikipedia/en/b/bf/UEFA_Champions_League_logo_2.svg',
  'Arsenal',
  'Real Madrid',
  'https://upload.wikimedia.org/wikipedia/en/5/53/Arsenal_FC.svg',
  'https://upload.wikimedia.org/wikipedia/en/5/56/Real_Madrid_CF.svg',
  now() + interval '1 day 7 hours',
  'DAZN 1',
  'scheduled',
  null,
  null,
  null,
  'Emirates Stadium, Londres'
),
(
  'fixture-f1-2026-interlagos',
  'motorsport',
  'Formula 1',
  'https://upload.wikimedia.org/wikipedia/commons/3/33/F1.svg',
  'GP São Paulo (Interlagos)',
  'Corrida Principal',
  'https://upload.wikimedia.org/wikipedia/commons/3/33/F1.svg',
  'https://upload.wikimedia.org/wikipedia/commons/3/33/F1.svg',
  now() + interval '3 hours',
  'Sport TV 4',
  'scheduled',
  null,
  null,
  null,
  'Autódromo José Carlos Pace'
)
on conflict (id) do update set
  kickoff_utc = excluded.kickoff_utc,
  status = excluded.status,
  updated_at = now();

commit;
