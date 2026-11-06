-- Migration 8: Client sync versioning and server-enforced conflict protocol
-- Fixes clock disparity by storing client_updated_at_ms and comparing client clock against client clock.
-- Provides atomic conflict resolution for profile library and watch progress.

begin;

alter table public.profile_library
    add column if not exists client_updated_at_ms bigint not null default 0;

alter table public.profile_watch_progress
    add column if not exists client_updated_at_ms bigint not null default 0;

grant update (is_favourite, is_watchlisted, is_watched, deleted_at, client_updated_at_ms)
    on public.profile_library to authenticated;

grant update (position_ms, duration_ms, completed, deleted_at, client_updated_at_ms)
    on public.profile_watch_progress to authenticated;

-- RPC: fortress_sync_profile_library
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
        p_is_favourite, p_is_watchlisted, p_is_watched, p_client_updated_at_ms, p_deleted_at
    )
    on conflict (household_id, profile_id, source_id, item_kind, item_id) do update
    set is_favourite = excluded.is_favourite,
        is_watchlisted = excluded.is_watchlisted,
        is_watched = excluded.is_watched,
        client_updated_at_ms = excluded.client_updated_at_ms,
        deleted_at = excluded.deleted_at
    where profile_library.client_updated_at_ms <= excluded.client_updated_at_ms;

    return jsonb_build_object('success', true);
end;
$$;

revoke all on function public.fortress_sync_profile_library(uuid, uuid, text, text, boolean, boolean, boolean, bigint, timestamptz) from public, anon;
grant execute on function public.fortress_sync_profile_library(uuid, uuid, text, text, boolean, boolean, boolean, bigint, timestamptz) to authenticated;

-- RPC: fortress_sync_profile_watch_progress
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
        p_position_ms, p_duration_ms, p_completed, p_client_updated_at_ms, p_deleted_at
    )
    on conflict (household_id, profile_id, source_id, item_kind, item_id) do update
    set position_ms = excluded.position_ms,
        duration_ms = excluded.duration_ms,
        completed = excluded.completed,
        client_updated_at_ms = excluded.client_updated_at_ms,
        deleted_at = excluded.deleted_at
    where profile_watch_progress.client_updated_at_ms <= excluded.client_updated_at_ms;

    return jsonb_build_object('success', true);
end;
$$;

revoke all on function public.fortress_sync_profile_watch_progress(uuid, uuid, text, text, bigint, bigint, boolean, bigint, timestamptz) from public, anon;
grant execute on function public.fortress_sync_profile_watch_progress(uuid, uuid, text, text, bigint, bigint, boolean, bigint, timestamptz) to authenticated;

commit;
