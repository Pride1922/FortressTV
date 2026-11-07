-- Migration 13: Manual EPG Mappings across household & EPG Schedule Correction
begin;

-- Table: household_epg_mappings
create table if not exists public.household_epg_mappings (
    household_id uuid not null references public.households(id) on delete cascade,
    iptv_source_id text not null,
    iptv_channel_id text not null,
    epg_source_id text,
    xmltv_channel_id text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (household_id, iptv_source_id, iptv_channel_id)
);

alter table public.household_epg_mappings enable row level security;

drop policy if exists household_epg_mappings_all on public.household_epg_mappings;
create policy household_epg_mappings_all on public.household_epg_mappings
    for all to authenticated
    using (household_id = auth.uid())
    with check (household_id = auth.uid());

-- RPC 1: fortress_save_epg_mapping (Adult-protected)
create or replace function public.fortress_save_epg_mapping(
    p_iptv_source_id text,
    p_iptv_channel_id text,
    p_epg_source_id text,
    p_xmltv_channel_id text,
    p_unlock_token uuid default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to edit channel mappings';
    end if;

    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    insert into public.household_epg_mappings (
        household_id, iptv_source_id, iptv_channel_id, epg_source_id, xmltv_channel_id, updated_at
    ) values (
        v_uid, btrim(p_iptv_source_id), btrim(p_iptv_channel_id), nullif(btrim(p_epg_source_id), ''), btrim(p_xmltv_channel_id), clock_timestamp()
    ) on conflict (household_id, iptv_source_id, iptv_channel_id) do update set
        epg_source_id = excluded.epg_source_id,
        xmltv_channel_id = excluded.xmltv_channel_id,
        updated_at = clock_timestamp();

    return jsonb_build_object('success', true);
end;
$$;
grant execute on function public.fortress_save_epg_mapping(text, text, text, text, uuid) to authenticated;

-- RPC 2: fortress_delete_epg_mapping (Adult-protected)
create or replace function public.fortress_delete_epg_mapping(
    p_iptv_source_id text,
    p_iptv_channel_id text,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to remove channel mappings';
    end if;

    delete from public.household_epg_mappings
    where household_id = v_uid
      and iptv_source_id = btrim(p_iptv_source_id)
      and iptv_channel_id = btrim(p_iptv_channel_id);

    return found;
end;
$$;
grant execute on function public.fortress_delete_epg_mapping(text, text, uuid) to authenticated;

-- RPC 3: fortress_get_epg_mappings (Readable by all household members)
create or replace function public.fortress_get_epg_mappings()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_mappings jsonb;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    select coalesce(jsonb_agg(
        jsonb_build_object(
            'iptv_source_id', m.iptv_source_id,
            'iptv_channel_id', m.iptv_channel_id,
            'epg_source_id', m.epg_source_id,
            'xmltv_channel_id', m.xmltv_channel_id,
            'updated_at', m.updated_at
        )
    ), '[]'::jsonb) into v_mappings
    from public.household_epg_mappings m
    where m.household_id = v_uid;

    return v_mappings;
end;
$$;
grant execute on function public.fortress_get_epg_mappings() to authenticated;

-- RPC 4: fortress_reset_all_epg_mappings (Adult-protected)
create or replace function public.fortress_reset_all_epg_mappings(
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to reset channel mappings';
    end if;

    delete from public.household_epg_mappings
    where household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_reset_all_epg_mappings(uuid) to authenticated;

grant select, insert, update, delete on public.household_epg_mappings to authenticated;

commit;
