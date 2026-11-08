-- Migration 15: Fix hide_adult_content persistence and dedicated atomic RPC
begin;

drop function if exists public.fortress_create_profile;
drop function if exists public.fortress_update_profile;

-- 1. fortress_create_profile: ensure hide_adult_content is properly persisted on creation
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
    v_hide_adult boolean := case when v_is_child then true else coalesce(p_hide_adult_content, false) end;
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
        household_id, name, kind, language,
        hide_adult_content, maximum_age, block_unrated, avatar_url
    ) values (
        v_uid, btrim(p_name), p_kind, v_lang,
        v_hide_adult,
        case when v_is_child then coalesce(p_maximum_age, 12) else null end,
        case when v_is_child then true else coalesce(p_block_unrated, false) end,
        p_avatar_url
    ) returning id into v_id;

    return v_id;
end;
$$;
grant execute on function public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid, text) to authenticated;

-- 2. fortress_update_profile: ensure hide_adult_content is properly updated
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
    v_hide_adult boolean := case when v_is_child then true else coalesce(p_hide_adult_content, false) end;
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
        hide_adult_content = v_hide_adult,
        maximum_age = case when v_is_child then coalesce(p_maximum_age, 12) else null end,
        block_unrated = case when v_is_child then true else coalesce(p_block_unrated, false) end,
        avatar_url = case when p_avatar_url is not null then p_avatar_url else avatar_url end,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid, text) to authenticated;

-- 3. fortress_set_profile_hide_adult_content: dedicated atomic RPC to toggle adult content filter
create or replace function public.fortress_set_profile_hide_adult_content(
    p_profile_id uuid,
    p_hide_adult_content boolean,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_kind text;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    select kind into v_kind from public.household_profiles
    where id = p_profile_id and household_id = v_uid;
    if not found then raise exception 'Profile not found'; end if;

    -- Child profiles cannot view adult content
    if v_kind = 'child' and not p_hide_adult_content then
        raise exception 'Child profiles must hide adult content';
    end if;

    -- If disabling the adult content filter, require adult PIN authorization
    if not p_hide_adult_content then
        if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
            raise exception 'Adult authorization required to disable adult content filtering';
        end if;
    end if;

    update public.household_profiles set
        hide_adult_content = p_hide_adult_content,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_set_profile_hide_adult_content(uuid, boolean, uuid) to authenticated;

commit;
