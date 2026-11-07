-- FortressTV: Frictionless profile sessions and OpenSubtitles permissions adjustment
-- 1. Removes adult PIN gate from fortress_open_profile_session so authenticated profiles
--    can sync favorites, watchlist and progress seamlessly across devices.
-- 2. Removes adult PIN gate from fortress_save_opensubtitles so subtitle credentials sync seamlessly.

create or replace function public.fortress_open_profile_session(
    p_profile_id uuid,
    p_unlock_token uuid default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_auth_session text;
    v_prof record;
    v_session_id uuid;
    v_now timestamptz := clock_timestamp();
    v_expires timestamptz := v_now + interval '7 days';
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    v_auth_session := public.fortress_current_auth_session_id();

    select * into v_prof from public.household_profiles where id = p_profile_id and household_id = v_uid;
    if not found then
        raise exception 'Profile not found';
    end if;

    if v_prof.kind = 'child' then
        -- Entering child mode: revoke adult unlock and adult profile sessions
        -- for THIS authenticated Auth session / device only.
        delete from public.household_adult_sessions
        where household_id = v_uid and auth_session_id = v_auth_session;

        delete from public.household_active_sessions
        where household_id = v_uid and auth_session_id = v_auth_session and kind = 'adult';
    end if;

    insert into public.household_active_sessions (household_id, profile_id, kind, auth_session_id, expires_at)
    values (v_uid, p_profile_id, v_prof.kind, v_auth_session, v_expires)
    returning id into v_session_id;

    return jsonb_build_object(
        'session_token', v_session_id,
        'profile_id', p_profile_id,
        'kind', v_prof.kind,
        'language', v_prof.language,
        'hide_adult_content', v_prof.hide_adult_content,
        'maximum_age', v_prof.maximum_age,
        'block_unrated', v_prof.block_unrated,
        'avatar_url', v_prof.avatar_url,
        'expires_at', v_expires
    );
end;
$$;
grant execute on function public.fortress_open_profile_session(uuid, uuid) to authenticated;

create or replace function public.fortress_save_opensubtitles(
    p_enabled boolean,
    p_api_key text,
    p_username text,
    p_password text,
    p_prefer_pt_pt boolean default true,
    p_auto_search boolean default true,
    p_unlock_token uuid default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_key text;
    v_encrypted text;
    v_creds jsonb;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    v_key := public.fortress_get_source_master_key();
    v_creds := jsonb_build_object(
        'api_key', coalesce(btrim(p_api_key), ''),
        'username', coalesce(btrim(p_username), ''),
        'password', coalesce(p_password, '')
    );
    v_encrypted := encode(extensions.pgp_sym_encrypt(v_creds::text, v_key, 'cipher-algo=aes256'), 'base64');

    insert into public.household_opensubtitles (
        household_id, enabled, prefer_pt_pt, auto_search, encrypted_credentials, updated_at
    ) values (
        v_uid, coalesce(p_enabled, false), coalesce(p_prefer_pt_pt, true),
        coalesce(p_auto_search, true), v_encrypted, clock_timestamp()
    ) on conflict (household_id) do update set
        enabled = excluded.enabled,
        prefer_pt_pt = excluded.prefer_pt_pt,
        auto_search = excluded.auto_search,
        encrypted_credentials = excluded.encrypted_credentials,
        updated_at = clock_timestamp();

    return jsonb_build_object('success', true);
end;
$$;
grant execute on function public.fortress_save_opensubtitles(boolean, text, text, text, boolean, boolean, uuid) to authenticated;

create or replace function public.fortress_delete_opensubtitles(
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    delete from public.household_opensubtitles where household_id = v_uid;
    return true;
end;
$$;
grant execute on function public.fortress_delete_opensubtitles(uuid) to authenticated;
