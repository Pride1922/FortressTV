-- Migration 3: Harden profile authorization, enforce session-scoped data access,
-- prevent PIN rollback bypasses, and serialize verification.
begin;

-- 1. Drop obsolete RPC overloads that accepted direct PIN guessing
drop function if exists public.fortress_is_adult_authorized(uuid, uuid, text);
drop function if exists public.fortress_set_parental_pin(text, text, uuid);
drop function if exists public.fortress_remove_parental_pin(text, uuid);
drop function if exists public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid, text);
drop function if exists public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid, text);
drop function if exists public.fortress_delete_profile(uuid, uuid, text);
drop function if exists public.fortress_open_profile_session(uuid, text, uuid);

-- 2. Authorization check accepting ONLY valid, unexpired adult unlock tokens (no PIN guessing)
create or replace function public.fortress_is_adult_authorized(
    p_household_id uuid,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
begin
    select * into v_ctrl from public.household_parental_controls where household_id = p_household_id;
    if not found or v_ctrl.pin_hash is null then
        return true;
    end if;

    if v_ctrl.locked_until is not null and v_ctrl.locked_until > v_now then
        return false;
    end if;

    if p_unlock_token is not null then
        if exists (
            select 1 from public.household_adult_sessions
            where id = p_unlock_token
              and household_id = p_household_id
              and expires_at > v_now
        ) then
            return true;
        end if;
    end if;

    return false;
end;
$$;
revoke all on function public.fortress_is_adult_authorized(uuid, uuid) from public, anon, authenticated;

-- 3. Dedicated PIN verification RPC with row locking (SELECT ... FOR UPDATE) and non-rolling-back failure counters
create or replace function public.fortress_verify_parental_pin(p_pin text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_hash text;
    v_token uuid;
    v_expires timestamptz;
    v_failed int;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    -- Serialize concurrent verification attempts on this household
    select * into v_ctrl
    from public.household_parental_controls
    where household_id = v_uid
    for update;

    if not found or v_ctrl.pin_hash is null then
        v_expires := v_now + interval '15 minutes';
        insert into public.household_adult_sessions (household_id, expires_at)
        values (v_uid, v_expires) returning id into v_token;
        return jsonb_build_object('verified', true, 'unlock_token', v_token, 'expires_at', v_expires);
    end if;

    if v_ctrl.locked_until is not null and v_ctrl.locked_until > v_now then
        return jsonb_build_object(
            'verified', false,
            'locked', true,
            'remaining_attempts', 0,
            'lock_seconds', greatest(1, ceil(extract(epoch from (v_ctrl.locked_until - v_now))))::int
        );
    end if;

    if p_pin is null or length(trim(p_pin)) < 4 or length(trim(p_pin)) > 12 or trim(p_pin) !~ '^[0-9]+$' then
        v_failed := v_ctrl.failed_attempts + 1;
        update public.household_parental_controls
        set failed_attempts = v_failed,
            locked_until = case when v_failed >= 5 then v_now + interval '15 minutes' else null end,
            updated_at = v_now
        where household_id = v_uid;

        return jsonb_build_object(
            'verified', false,
            'locked', (v_failed >= 5),
            'remaining_attempts', greatest(0, 5 - v_failed),
            'lock_seconds', case when v_failed >= 5 then 900 else 0 end
        );
    end if;

    v_hash := public.fortress_hash_pin(v_ctrl.pin_salt, trim(p_pin));
    if v_hash = v_ctrl.pin_hash then
        update public.household_parental_controls
        set failed_attempts = 0, locked_until = null, updated_at = v_now
        where household_id = v_uid;

        v_expires := v_now + interval '15 minutes';
        insert into public.household_adult_sessions (household_id, expires_at)
        values (v_uid, v_expires) returning id into v_token;

        return jsonb_build_object('verified', true, 'unlock_token', v_token, 'expires_at', v_expires);
    else
        v_failed := v_ctrl.failed_attempts + 1;
        update public.household_parental_controls
        set failed_attempts = v_failed,
            locked_until = case when v_failed >= 5 then v_now + interval '15 minutes' else null end,
            updated_at = v_now
        where household_id = v_uid;

        return jsonb_build_object(
            'verified', false,
            'locked', (v_failed >= 5),
            'remaining_attempts', greatest(0, 5 - v_failed),
            'lock_seconds', case when v_failed >= 5 then 900 else 0 end
        );
    end if;
end;
$$;
grant execute on function public.fortress_verify_parental_pin(text) to authenticated;

-- 4. Set parental PIN: requires adult authorization if PIN already exists; invalidates adult sessions on change
create or replace function public.fortress_set_parental_pin(
    p_new_pin text,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_salt text;
    v_hash text;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to configure parental PIN';
    end if;

    v_salt := gen_random_uuid()::text;
    v_hash := public.fortress_hash_pin(v_salt, p_new_pin);

    insert into public.household_parental_controls (
        household_id, pin_salt, pin_hash, failed_attempts, locked_until, updated_at
    ) values (
        v_uid, v_salt, v_hash, 0, null, clock_timestamp()
    ) on conflict (household_id) do update
    set pin_salt = excluded.pin_salt,
        pin_hash = excluded.pin_hash,
        failed_attempts = 0,
        locked_until = null,
        updated_at = clock_timestamp();

    -- Invalidate all existing adult unlocks and adult profile sessions
    delete from public.household_adult_sessions where household_id = v_uid;
    delete from public.household_active_sessions where household_id = v_uid and kind = 'adult';

    return true;
end;
$$;
grant execute on function public.fortress_set_parental_pin(text, uuid) to authenticated;

-- 5. Remove parental PIN: requires adult authorization; invalidates adult unlocks and sessions
create or replace function public.fortress_remove_parental_pin(
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to remove parental PIN';
    end if;

    delete from public.household_parental_controls where household_id = v_uid;
    delete from public.household_adult_sessions where household_id = v_uid;
    delete from public.household_active_sessions where household_id = v_uid and kind = 'adult';

    return true;
end;
$$;
grant execute on function public.fortress_remove_parental_pin(uuid) to authenticated;

-- 6. Create profile: adult authorization check, server-enforced child restrictions
create or replace function public.fortress_create_profile(
    p_name text,
    p_kind text,
    p_language text default 'en',
    p_hide_adult_content boolean default true,
    p_maximum_age int default null,
    p_block_unrated boolean default true,
    p_unlock_token uuid default null
)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_profile_id uuid;
    v_effective_hide boolean := coalesce(p_hide_adult_content, true);
    v_effective_block boolean := coalesce(p_block_unrated, true);
    v_effective_age smallint := case when p_maximum_age is not null then p_maximum_age::smallint else null end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to create profile';
    end if;

    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    if p_kind = 'child' then
        v_effective_hide := true;
        v_effective_block := true;
        if v_effective_age is null then
            v_effective_age := 12;
        end if;
    end if;

    insert into public.household_profiles (
        household_id, name, kind, language, hide_adult_content, maximum_age, block_unrated
    ) values (
        v_uid, btrim(p_name), p_kind, coalesce(p_language, 'en'), v_effective_hide, v_effective_age, v_effective_block
    ) returning id into v_profile_id;

    return v_profile_id;
end;
$$;
grant execute on function public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid) to authenticated;

-- 7. Update profile: adult authorization check, server-enforced child restrictions
create or replace function public.fortress_update_profile(
    p_profile_id uuid,
    p_name text,
    p_kind text,
    p_language text default 'en',
    p_hide_adult_content boolean default true,
    p_maximum_age int default null,
    p_block_unrated boolean default true,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_effective_hide boolean := coalesce(p_hide_adult_content, true);
    v_effective_block boolean := coalesce(p_block_unrated, true);
    v_effective_age smallint := case when p_maximum_age is not null then p_maximum_age::smallint else null end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to update profile';
    end if;

    if not exists (select 1 from public.household_profiles where id = p_profile_id and household_id = v_uid) then
        raise exception 'Profile not found';
    end if;

    if p_kind = 'child' then
        v_effective_hide := true;
        v_effective_block := true;
        if v_effective_age is null then
            v_effective_age := 12;
        end if;
        -- Invalidate active sessions for this profile if changed to child
        delete from public.household_active_sessions where household_id = v_uid and profile_id = p_profile_id;
    end if;

    update public.household_profiles
    set name = btrim(p_name),
        kind = p_kind,
        language = coalesce(p_language, 'en'),
        hide_adult_content = v_effective_hide,
        maximum_age = v_effective_age,
        block_unrated = v_effective_block,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid) to authenticated;

-- 8. Delete profile: adult authorization check
create or replace function public.fortress_delete_profile(
    p_profile_id uuid,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to delete profile';
    end if;

    delete from public.household_profiles where id = p_profile_id and household_id = v_uid;
    return true;
end;
$$;
grant execute on function public.fortress_delete_profile(uuid, uuid) to authenticated;

-- 9. Open profile session: adult profile requires adult authorization; child profile opens freely
create or replace function public.fortress_open_profile_session(
    p_profile_id uuid,
    p_unlock_token uuid default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_prof record;
    v_session_id uuid;
    v_now timestamptz := clock_timestamp();
    v_expires timestamptz := v_now + interval '7 days';
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    select * into v_prof from public.household_profiles where id = p_profile_id and household_id = v_uid;
    if not found then
        raise exception 'Profile not found';
    end if;

    if v_prof.kind = 'adult' then
        if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
            raise exception 'Adult authorization required to select adult profile';
        end if;
    end if;

    insert into public.household_active_sessions (household_id, profile_id, kind, expires_at)
    values (v_uid, p_profile_id, v_prof.kind, v_expires)
    returning id into v_session_id;

    return jsonb_build_object(
        'session_token', v_session_id,
        'profile_id', p_profile_id,
        'kind', v_prof.kind,
        'language', v_prof.language,
        'hide_adult_content', v_prof.hide_adult_content,
        'maximum_age', v_prof.maximum_age,
        'block_unrated', v_prof.block_unrated,
        'expires_at', v_expires
    );
end;
$$;
grant execute on function public.fortress_open_profile_session(uuid, uuid) to authenticated;

-- 10. Server-enforced session boundary on library and watch progress tables
create or replace function public.fortress_validate_profile_session(p_profile_id uuid)
returns boolean language plpgsql stable security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_header_text text;
    v_session_id uuid;
begin
    if v_uid is null or p_profile_id is null then
        return false;
    end if;

    begin
        v_header_text := current_setting('request.headers', true)::json->>'x-profile-session';
    exception when others then
        return false;
    end;

    if v_header_text is null or v_header_text !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' then
        return false;
    end if;

    v_session_id := v_header_text::uuid;

    return exists (
        select 1 from public.household_active_sessions
        where id = v_session_id
          and household_id = v_uid
          and profile_id = p_profile_id
          and expires_at > clock_timestamp()
    );
end;
$$;
revoke all on function public.fortress_validate_profile_session(uuid) from public, anon;
grant execute on function public.fortress_validate_profile_session(uuid) to authenticated;

drop policy if exists library_owner on public.profile_library;
drop policy if exists library_session_access on public.profile_library;
create policy library_session_access on public.profile_library for all to authenticated
    using (
        household_id = (select auth.uid())
        and public.fortress_validate_profile_session(profile_id)
    )
    with check (
        household_id = (select auth.uid())
        and public.fortress_validate_profile_session(profile_id)
    );

drop policy if exists progress_owner on public.profile_watch_progress;
drop policy if exists progress_session_access on public.profile_watch_progress;
create policy progress_session_access on public.profile_watch_progress for all to authenticated
    using (
        household_id = (select auth.uid())
        and public.fortress_validate_profile_session(profile_id)
    )
    with check (
        household_id = (select auth.uid())
        and public.fortress_validate_profile_session(profile_id)
    );

commit;
