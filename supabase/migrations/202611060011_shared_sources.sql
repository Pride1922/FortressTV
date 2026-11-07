-- Migration 11: Shared sources across households and removal of read restriction
-- Xtream & custom EPG sources are shared across households so all profiles/households can watch TV.
-- Saving and deleting sources still requires adult PIN authorization if configured.
begin;

-- 1. Ensure foreign key on parent_source_id does not require matching household_id
alter table public.household_sources
    drop constraint if exists household_sources_household_id_parent_source_id_fkey;

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'household_sources_parent_source_id_fkey'
    ) then
        alter table public.household_sources
            add constraint household_sources_parent_source_id_fkey
            foreign key (parent_source_id) references public.household_sources(id) on delete cascade;
    end if;
end;
$$;

-- 2. fortress_get_household_sources: readable by all authenticated users across households
create or replace function public.fortress_get_household_sources(
    p_unlock_token uuid default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_key text;
    v_sources jsonb;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    v_key := public.fortress_get_source_master_key();

    select coalesce(jsonb_agg(
        jsonb_build_object(
            'id', s.id,
            'source_type', s.source_type,
            'display_name', s.display_name,
            'enabled', s.enabled,
            'source_order', s.source_order,
            'parent_source_id', s.parent_source_id,
            'config', extensions.pgp_sym_decrypt(decode(s.encrypted_config, 'base64'), v_key, 'cipher-algo=aes256')::jsonb,
            'deleted_at', s.deleted_at,
            'updated_at', s.updated_at
        ) order by s.source_order asc, s.created_at asc
    ), '[]'::jsonb) into v_sources
    from public.household_sources s
    where s.deleted_at is null;

    return v_sources;
end;
$$;
grant execute on function public.fortress_get_household_sources(uuid) to authenticated;

-- 3. fortress_save_household_source: requires adult authorization to save/modify shared sources
create or replace function public.fortress_save_household_source(
    p_source_id uuid,
    p_source_type text,
    p_display_name text,
    p_enabled boolean,
    p_source_order int,
    p_config jsonb,
    p_parent_source_id uuid default null,
    p_unlock_token uuid default null
)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_key text;
    v_encrypted text;
    v_existing_deleted_at timestamptz;
    v_result_id uuid;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to manage source connections';
    end if;

    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    if p_parent_source_id is not null then
        if not exists (
            select 1 from public.household_sources
            where id = p_parent_source_id and deleted_at is null
        ) then
            raise exception 'Parent source does not exist';
        end if;
    end if;

    if p_source_id is not null then
        select deleted_at into v_existing_deleted_at
        from public.household_sources
        where id = p_source_id;

        if v_existing_deleted_at is not null then
            raise exception 'Cannot update deleted source. Re-create source with new ID if intended.';
        end if;
    end if;

    v_key := public.fortress_get_source_master_key();
    v_encrypted := encode(extensions.pgp_sym_encrypt(p_config::text, v_key, 'cipher-algo=aes256'), 'base64');

    insert into public.household_sources (
        id, household_id, source_type, display_name, enabled, source_order,
        encrypted_config, parent_source_id, deleted_at, updated_at
    ) values (
        coalesce(p_source_id, gen_random_uuid()),
        v_uid,
        p_source_type,
        btrim(p_display_name),
        coalesce(p_enabled, true),
        coalesce(p_source_order, 0),
        v_encrypted,
        p_parent_source_id,
        null,
        clock_timestamp()
    ) on conflict (id) do update set
        source_type = excluded.source_type,
        display_name = excluded.display_name,
        enabled = excluded.enabled,
        source_order = excluded.source_order,
        encrypted_config = excluded.encrypted_config,
        parent_source_id = excluded.parent_source_id,
        deleted_at = null,
        updated_at = clock_timestamp()
    returning id into v_result_id;

    return v_result_id;
end;
$$;
grant execute on function public.fortress_save_household_source(uuid, text, text, boolean, int, jsonb, uuid, uuid) to authenticated;

-- 4. fortress_delete_household_source: requires adult authorization to remove shared sources
create or replace function public.fortress_delete_household_source(
    p_source_id uuid,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to remove source connections';
    end if;

    update public.household_sources
    set deleted_at = clock_timestamp(),
        updated_at = clock_timestamp()
    where id = p_source_id or parent_source_id = p_source_id;

    return found;
end;
$$;
grant execute on function public.fortress_delete_household_source(uuid, uuid) to authenticated;

-- 5. fortress_create_profile & fortress_update_profile: avatar_url persistence
alter table public.household_profiles
    add column if not exists avatar_url text;

create or replace function public.fortress_create_profile(
    p_name text,
    p_kind text default 'adult',
    p_language text default 'en',
    p_hide_adult_content boolean default null,
    p_maximum_age int default null,
    p_block_unrated boolean default null,
    p_unlock_token uuid default null,
    p_avatar_url text default null
)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_id uuid;
    v_is_child boolean := (p_kind = 'child');
    v_lang text := case when p_language in ('pt-PT', 'pt') then 'pt-PT' else 'en' end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to create profile';
    end if;
    if (select count(*) from public.household_profiles where household_id = v_uid) >= 10 then
        raise exception 'Maximum profile count reached';
    end if;

    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    insert into public.household_profiles (
        household_id, name, is_adult, content_rating_limit, avatar_url, language
    ) values (
        v_uid, btrim(p_name), not v_is_child,
        case when v_is_child then coalesce(p_maximum_age, 12)::text else null end,
        p_avatar_url,
        v_lang
    ) returning id into v_id;

    return v_id;
end;
$$;
grant execute on function public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid, text) to authenticated;

create or replace function public.fortress_update_profile(
    p_profile_id uuid,
    p_name text,
    p_kind text,
    p_language text default 'en',
    p_hide_adult_content boolean default null,
    p_maximum_age int default null,
    p_block_unrated boolean default null,
    p_unlock_token uuid default null,
    p_avatar_url text default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_is_child boolean := (p_kind = 'child');
    v_lang text := case when p_language in ('pt-PT', 'pt') then 'pt-PT' else 'en' end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to update profile';
    end if;

    if not exists (select 1 from public.household_profiles where id = p_profile_id and household_id = v_uid) then
        raise exception 'Profile not found';
    end if;

    update public.household_profiles set
        name = btrim(p_name),
        kind = p_kind,
        language = v_lang,
        hide_adult_content = case when v_is_child then true else coalesce(p_hide_adult_content, false) end,
        maximum_age = case when v_is_child then coalesce(p_maximum_age, 12) else null end,
        block_unrated = case when v_is_child then true else coalesce(p_block_unrated, false) end,
        avatar_url = case when p_avatar_url is not null then p_avatar_url else avatar_url end,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid, text) to authenticated;

commit;
