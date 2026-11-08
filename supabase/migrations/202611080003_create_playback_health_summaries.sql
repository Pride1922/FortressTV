-- Migration 17: Create playback_health_summaries for connection quality telemetry
begin;

create table if not exists public.playback_health_summaries (
  id uuid primary key default gen_random_uuid(),
  session_id text not null unique,
  household_id uuid not null references public.households(id) on delete cascade,
  profile_id uuid not null,
  channel_id text not null default '',
  stream_kind text not null check (stream_kind in ('live', 'catchup', 'vod')),
  provider_id text,
  started_at timestamptz not null,
  ended_at timestamptz not null,
  startup_ms bigint not null,
  stall_count int not null,
  total_stall_ms bigint not null,
  avg_bitrate_kbps int not null,
  dropped_frames_pct real not null,
  player_error_code text,
  region text not null default 'unknown',
  completed boolean not null,
  inserted_at timestamptz not null default now()
);

create index if not exists idx_playback_health_channel_region_time
  on public.playback_health_summaries (channel_id, region, inserted_at desc);

create index if not exists idx_playback_health_household_time
  on public.playback_health_summaries (household_id, inserted_at desc);

alter table public.playback_health_summaries enable row level security;

drop policy if exists playback_health_summaries_household_all on public.playback_health_summaries;
create policy playback_health_summaries_household_all on public.playback_health_summaries
  for all to authenticated
  using (household_id = auth.uid())
  with check (household_id = auth.uid());

grant all on public.playback_health_summaries to authenticated;

commit;
