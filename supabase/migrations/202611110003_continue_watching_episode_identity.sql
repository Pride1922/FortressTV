begin;

alter table public.profile_watch_progress
    add column if not exists series_id integer,
    add column if not exists season_number integer,
    add column if not exists episode_number integer;

create or replace function public.fortress_sync_episode_progress(
    p_profile_id uuid,
    p_source_id uuid,
    p_item_id text,
    p_series_id integer,
    p_season_number integer,
    p_episode_number integer,
    p_position_ms bigint default null,
    p_duration_ms bigint default null,
    p_completed boolean default null,
    p_client_updated_at_ms bigint default null,
    p_deleted_at timestamptz default null
)
returns void language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null or public.fortress_validate_profile_session(p_profile_id) is not true then
        raise exception 'Invalid or expired profile session';
    end if;

    if p_client_updated_at_ms is not null then
        perform public.fortress_sync_profile_watch_progress(
            p_profile_id, p_source_id, 'episode', p_item_id, p_position_ms,
            p_duration_ms, p_completed, p_client_updated_at_ms, p_deleted_at
        );
    end if;

    update public.profile_watch_progress
    set series_id = coalesce(series_id, p_series_id),
        season_number = coalesce(season_number, p_season_number),
        episode_number = coalesce(episode_number, p_episode_number)
    where household_id = auth.uid()
      and profile_id = p_profile_id
      and source_id = p_source_id
      and item_kind = 'episode'
      and item_id = p_item_id
      and (series_id is null or series_id = p_series_id)
      and (series_id is null
           or (season_number is null and p_season_number is not null)
           or (episode_number is null and p_episode_number is not null));
end;
$$;

revoke all on function public.fortress_sync_episode_progress(uuid, uuid, text, integer, integer, integer, bigint, bigint, boolean, bigint, timestamptz) from public, anon;
grant execute on function public.fortress_sync_episode_progress(uuid, uuid, text, integer, integer, integer, bigint, bigint, boolean, bigint, timestamptz) to authenticated;

commit;
