-- Measured decoder output, never inferred from source names or manifest maxima.
-- Filename matches the migration version recorded by the hosted Supabase project.
-- Nullable additions preserve old clients and queued legacy telemetry.
begin;
set local lock_timeout = '5s';
set local statement_timeout = '30s';

alter table public.playback_health_summaries
  add column if not exists stream_variant_id text,
  add column if not exists video_width integer,
  add column if not exists video_height integer,
  add column if not exists quality_observed_at timestamptz,
  add column if not exists quality_tier text generated always as (
    case
      when video_width is null or video_height is null then null
      when least(video_width, video_height) >= 2160 or greatest(video_width, video_height) >= 3840 then '4K'
      when least(video_width, video_height) >= 1440 or greatest(video_width, video_height) >= 2560 then '2K'
      when least(video_width, video_height) >= 1080 or greatest(video_width, video_height) >= 1920 then 'FHD'
      when least(video_width, video_height) >= 720 or greatest(video_width, video_height) >= 1280 then 'HD'
      else 'SD'
    end
  ) stored;

do $$
begin
  if not exists (select 1 from pg_constraint where conrelid = 'public.playback_health_summaries'::regclass and conname = 'playback_health_stream_variant_format') then
    alter table public.playback_health_summaries add constraint playback_health_stream_variant_format
      check (stream_variant_id is null or stream_variant_id ~ '^[0-9a-f]{64}$');
  end if;
  if not exists (select 1 from pg_constraint where conrelid = 'public.playback_health_summaries'::regclass and conname = 'playback_health_video_dimensions') then
    alter table public.playback_health_summaries add constraint playback_health_video_dimensions check (
      (video_width is null and video_height is null and quality_observed_at is null)
      or (
        video_width is not null and video_height is not null
        and video_width between 1 and 32768 and video_height between 1 and 32768
        and quality_observed_at is not null and quality_observed_at > '1970-01-01T00:00:00Z'::timestamptz
        and stream_variant_id is not null
      )
    );
  end if;
end
$$;

create index if not exists idx_playback_health_variant_quality
  on public.playback_health_summaries (household_id, provider_id, stream_variant_id, quality_observed_at desc)
  where stream_kind = 'live' and quality_observed_at is not null;

comment on column public.playback_health_summaries.stream_variant_id is
  'SHA-256 of source/item/stream identity. No raw URL or credentials. Separate quality observations per source variant.';
comment on column public.playback_health_summaries.quality_observed_at is
  'Client time of the last valid decoder-size observation after a video frame was rendered; not the maximum advertised resolution.';

notify pgrst, 'reload schema';
commit;
