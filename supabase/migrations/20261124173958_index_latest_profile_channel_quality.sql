-- Version matches the hosted migration history.
begin;
set local lock_timeout = '5s';
set local statement_timeout = '30s';

-- Bounded profile-wide badge restoration, independent of individual source refreshes.
create index if not exists idx_playback_health_profile_quality
  on public.playback_health_summaries (household_id, profile_id, quality_observed_at desc)
  where stream_kind = 'live' and quality_observed_at is not null;

commit;
