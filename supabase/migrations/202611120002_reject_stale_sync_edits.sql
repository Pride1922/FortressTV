-- Migration: 202611120002_reject_stale_sync_edits
-- Migration 9 replaced "newest edit wins" with "last arrival wins", so a device coming back online
-- could overwrite newer favourites/watchlist/progress with its old queued edits. This restores the
-- timestamp check, made robust against wrong device clocks:
--  * an incoming client time more than 5 minutes ahead of the server is clamped to server time, so a
--    fast clock cannot stamp an edit that no later edit can ever beat;
--  * a stored time that is already in the future (written before this migration by such a device)
--    does not block newer edits.
-- The app already sends server-corrected times (ServerClockTracker), so normal edits are unaffected.
-- A rejected stale edit still returns success (with "applied": false) so the app drops it from its
-- queue instead of retrying. Safe to run more than once.
begin;

create or replace function public.fortress_sync_clamp_client_time(p_client_updated_at_ms bigint)
returns bigint language sql stable set search_path = '' as $$
    -- A missing client time counts as "now" (least() alone would ignore it and pick the cap).
    select least(
        coalesce(p_client_updated_at_ms, (extract(epoch from clock_timestamp()) * 1000)::bigint),
        (extract(epoch from clock_timestamp()) * 1000)::bigint + 5 * 60 * 1000
    );
$$;
revoke all on function public.fortress_sync_clamp_client_time(bigint) from public, anon, authenticated;

create or replace function public.fortress_sync_profile_library(
    p_profile_id uuid,
    p_source_id uuid,
    p_item_kind text,
    p_item_id text,
    p_is_favourite boolean,
    p_is_watchlisted boolean,
    p_is_watched boolean,
    p_client_updated_at_ms bigint,
    p_deleted_at timestamptz default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_client_ms bigint := public.fortress_sync_clamp_client_time(p_client_updated_at_ms);
    v_now_ms bigint := (extract(epoch from clock_timestamp()) * 1000)::bigint;
    v_rows integer;
begin
    if v_uid is null then
        raise exception 'Not authenticated';
    end if;

    if not public.fortress_validate_profile_session(p_profile_id) then
        raise exception 'Invalid or expired profile session';
    end if;

    insert into public.profile_library (
        household_id, profile_id, source_id, item_kind, item_id,
        is_favourite, is_watchlisted, is_watched, client_updated_at_ms, deleted_at
    ) values (
        v_uid, p_profile_id, p_source_id, p_item_kind, p_item_id,
        p_is_favourite, p_is_watchlisted, p_is_watched, v_client_ms, p_deleted_at
    )
    on conflict (household_id, profile_id, source_id, item_kind, item_id) do update
    set is_favourite = excluded.is_favourite,
        is_watchlisted = excluded.is_watchlisted,
        is_watched = excluded.is_watched,
        client_updated_at_ms = excluded.client_updated_at_ms,
        deleted_at = excluded.deleted_at
    where profile_library.client_updated_at_ms <= excluded.client_updated_at_ms
       or profile_library.client_updated_at_ms > v_now_ms + 5 * 60 * 1000;

    get diagnostics v_rows = row_count;
    return jsonb_build_object('success', true, 'applied', v_rows > 0);
end;
$$;

create or replace function public.fortress_sync_profile_watch_progress(
    p_profile_id uuid,
    p_source_id uuid,
    p_item_kind text,
    p_item_id text,
    p_position_ms bigint,
    p_duration_ms bigint,
    p_completed boolean,
    p_client_updated_at_ms bigint,
    p_deleted_at timestamptz default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_client_ms bigint := public.fortress_sync_clamp_client_time(p_client_updated_at_ms);
    v_now_ms bigint := (extract(epoch from clock_timestamp()) * 1000)::bigint;
    v_rows integer;
begin
    if v_uid is null then
        raise exception 'Not authenticated';
    end if;

    if not public.fortress_validate_profile_session(p_profile_id) then
        raise exception 'Invalid or expired profile session';
    end if;

    insert into public.profile_watch_progress (
        household_id, profile_id, source_id, item_kind, item_id,
        position_ms, duration_ms, completed, client_updated_at_ms, deleted_at
    ) values (
        v_uid, p_profile_id, p_source_id, p_item_kind, p_item_id,
        p_position_ms, p_duration_ms, p_completed, v_client_ms, p_deleted_at
    )
    on conflict (household_id, profile_id, source_id, item_kind, item_id) do update
    set position_ms = excluded.position_ms,
        duration_ms = excluded.duration_ms,
        completed = excluded.completed,
        client_updated_at_ms = excluded.client_updated_at_ms,
        deleted_at = excluded.deleted_at
    where profile_watch_progress.client_updated_at_ms <= excluded.client_updated_at_ms
       or profile_watch_progress.client_updated_at_ms > v_now_ms + 5 * 60 * 1000;

    get diagnostics v_rows = row_count;
    return jsonb_build_object('success', true, 'applied', v_rows > 0);
end;
$$;

revoke all on function public.fortress_sync_profile_library(uuid, uuid, text, text, boolean, boolean, boolean, bigint, timestamptz) from public, anon;
grant execute on function public.fortress_sync_profile_library(uuid, uuid, text, text, boolean, boolean, boolean, bigint, timestamptz) to authenticated;
revoke all on function public.fortress_sync_profile_watch_progress(uuid, uuid, text, text, bigint, bigint, boolean, bigint, timestamptz) from public, anon;
grant execute on function public.fortress_sync_profile_watch_progress(uuid, uuid, text, text, bigint, bigint, boolean, bigint, timestamptz) to authenticated;

commit;
