-- Read-only assertions against catalogued expressions: safe on a hosted project
-- as well as the disposable CI database. No customer rows are read or modified.
begin;
do $$
declare
  tier_expression text;
  dimensions_expression text;
  variant_expression text;
  sample record;
  actual_tier text;
  valid boolean;
begin
  if not (select relrowsecurity from pg_class where oid = 'public.playback_health_summaries'::regclass) then
    raise exception 'Telemetry RLS must remain enabled';
  end if;
  select pg_get_expr(d.adbin, d.adrelid) into strict tier_expression
    from pg_attribute a join pg_attrdef d on d.adrelid = a.attrelid and d.adnum = a.attnum
    where a.attrelid = 'public.playback_health_summaries'::regclass and a.attname = 'quality_tier' and a.attgenerated = 's';
  select pg_get_expr(conbin, conrelid) into strict dimensions_expression from pg_constraint
    where conrelid = 'public.playback_health_summaries'::regclass and conname = 'playback_health_video_dimensions' and convalidated;
  select pg_get_expr(conbin, conrelid) into strict variant_expression from pg_constraint
    where conrelid = 'public.playback_health_summaries'::regclass and conname = 'playback_health_stream_variant_format' and convalidated;

  for sample in select * from (values
    (null::integer, null::integer, null::text), (720, 576, 'SD'), (1280, 720, 'HD'),
    (1920, 1080, 'FHD'), (2560, 1440, '2K'), (3840, 2160, '4K'), (2160, 3840, '4K')
  ) as samples(w, h, expected)
  loop
    execute format('select %s from (values ($1::integer, $2::integer)) as q(video_width, video_height)', tier_expression)
      into actual_tier using sample.w, sample.h;
    if actual_tier is distinct from sample.expected then raise exception 'Incorrect quality tier for %x%', sample.w, sample.h; end if;
  end loop;

  for sample in select * from (values
    (null::integer, null::integer, null::timestamptz, null::text, true),
    (3840, 2160, '2026-11-24T12:00:00Z'::timestamptz, repeat('a', 64), true),
    (null, 2160, '2026-11-24T12:00:00Z'::timestamptz, repeat('a', 64), false),
    (3840, null, '2026-11-24T12:00:00Z'::timestamptz, repeat('a', 64), false),
    (0, 2160, '2026-11-24T12:00:00Z'::timestamptz, repeat('a', 64), false),
    (3840, 2160, null, repeat('a', 64), false),
    (3840, 2160, '2026-11-24T12:00:00Z'::timestamptz, null, false)
  ) as samples(w, h, observed, variant, expected)
  loop
    execute format('select %s from (values ($1::integer, $2::integer, $3::timestamptz, $4::text)) as q(video_width, video_height, quality_observed_at, stream_variant_id)', dimensions_expression)
      into valid using sample.w, sample.h, sample.observed, sample.variant;
    if valid is distinct from sample.expected then raise exception 'Invalid dimension constraint behavior'; end if;
  end loop;

  execute format('select %s from (values ($1::text)) as q(stream_variant_id)', variant_expression) into valid using 'not-an-opaque-identifier';
  if valid is distinct from false then raise exception 'Raw stream identifiers must be rejected'; end if;
  if not exists (select 1 from pg_index where indexrelid = 'public.idx_playback_health_variant_quality'::regclass and indisvalid) then
    raise exception 'Missing variant quality index';
  end if;
end
$$;
rollback;
