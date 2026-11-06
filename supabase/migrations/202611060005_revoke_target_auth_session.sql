-- Migration 5: Allow securely revoking adult authorizations for a specific target Auth session within the household
begin;

-- Drop the 0-arg overload so PostgREST resolves the function cleanly with or without arguments
drop function if exists public.fortress_leave_adult_mode();

create or replace function public.fortress_leave_adult_mode(
    p_target_auth_session_id text default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_target text;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    if p_target_auth_session_id is not null and trim(p_target_auth_session_id) <> '' then
        v_target := trim(p_target_auth_session_id);
    else
        v_target := public.fortress_current_auth_session_id();
    end if;

    delete from public.household_adult_sessions
    where household_id = v_uid and auth_session_id = v_target;

    delete from public.household_active_sessions
    where household_id = v_uid and auth_session_id = v_target and kind = 'adult';

    return true;
end;
$$;
revoke all on function public.fortress_leave_adult_mode(text) from public, anon;
grant execute on function public.fortress_leave_adult_mode(text) to authenticated;

create or replace function public.fortress_revoke_adult_session(
    p_target_auth_session_id text
)
returns boolean language plpgsql security definer set search_path = '' as $$
begin
    return public.fortress_leave_adult_mode(p_target_auth_session_id);
end;
$$;
revoke all on function public.fortress_revoke_adult_session(text) from public, anon;
grant execute on function public.fortress_revoke_adult_session(text) to authenticated;

commit;
