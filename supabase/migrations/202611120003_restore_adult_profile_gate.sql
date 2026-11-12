-- Migration: 202611120003_restore_adult_profile_gate
-- Migration 0014 removed the server check on opening an adult profile, so the parental PIN was
-- enforced only by the app. 0014 did that because a device that starts directly in an adult profile
-- opens its profile session without a PIN, and the old blanket check broke syncing there.
--
-- This restores the check with the same rule the app's profile picker uses: the PIN (an adult
-- unlock token) is required only when this device (Auth session) is currently in a child profile.
-- A device already in adult mode, or starting fresh, still opens adult sessions without friction.
-- Opening an adult profile ends the device's child sessions, as opening a child profile already
-- ends its adult ones. Safe to run more than once.
begin;

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

    if v_prof.kind = 'adult' then
        -- Leaving child mode on this device needs the PIN. fortress_is_adult_authorized is true when
        -- no PIN is set, and false while PIN entry is locked out.
        if exists (
            select 1 from public.household_active_sessions
            where household_id = v_uid and auth_session_id = v_auth_session
              and kind = 'child' and expires_at > v_now
        ) and not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
            raise exception 'Adult authorization required to select adult profile';
        end if;

        delete from public.household_active_sessions
        where household_id = v_uid and auth_session_id = v_auth_session and kind = 'child';
    elsif v_prof.kind = 'child' then
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

revoke all on function public.fortress_open_profile_session(uuid, uuid) from public, anon;
grant execute on function public.fortress_open_profile_session(uuid, uuid) to authenticated;

commit;
