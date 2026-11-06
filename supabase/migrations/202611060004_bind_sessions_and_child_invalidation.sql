-- Migration 4: Bind adult unlocks and profile sessions to authenticated Auth sessions,
-- invalidate adult authorizations when entering child mode, and enforce session-scoped boundaries.
begin;

-- 1. Helper to extract the authenticated GoTrue Auth session ID from JWT claims.
-- Falls back to sub (user ID) if session_id claim is not present.
create or replace function public.fortress_current_auth_session_id()
returns text language plpgsql stable security definer set search_path = '' as $$
declare
    v_session text;
    v_claims text;
begin
    begin
        v_session := current_setting('request.jwt.claim.session_id', true);
    exception when others then
        v_session := null;
    end;
    if v_session is not null and v_session <> '' then
        return v_session;
    end if;

    begin
        v_claims := current_setting('request.jwt.claims', true);
        if v_claims is not null and v_claims <> '' then
            v_session := (v_claims::jsonb)->>'session_id';
            if v_session is not null and v_session <> '' then
                return v_session;
            end if;
        end if;
    exception when others then
        null;
    end;

    begin
        v_session := current_setting('request.jwt.claim.sub', true);
    exception when others then
        v_session := null;
    end;
    if v_session is not null and v_session <> '' then
        return v_session;
    end if;

    raise exception 'No authenticated session';
end;
$$;
revoke all on function public.fortress_current_auth_session_id() from public, anon;
grant execute on function public.fortress_current_auth_session_id() to authenticated;

-- 2. Add auth_session_id column to adult unlocks and active profile sessions
alter table public.household_adult_sessions
    add column if not exists auth_session_id text not null default 'default';

alter table public.household_active_sessions
    add column if not exists auth_session_id text not null default 'default';

create index if not exists household_adult_sessions_auth_lookup
    on public.household_adult_sessions(household_id, auth_session_id, id, expires_at);

create index if not exists household_active_sessions_auth_lookup
    on public.household_active_sessions(household_id, auth_session_id, id, profile_id, expires_at);

-- 3. Authorization check validated against current Auth session
create or replace function public.fortress_is_adult_authorized(
    p_household_id uuid,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_auth_session text;
begin
    select * into v_ctrl from public.household_parental_controls where household_id = p_household_id;
    if not found or v_ctrl.pin_hash is null then
        return true;
    end if;

    if v_ctrl.locked_until is not null and v_ctrl.locked_until > v_now then
        return false;
    end if;

    if p_unlock_token is not null then
        begin
            v_auth_session := public.fortress_current_auth_session_id();
        exception when others then
            return false;
        end;

        if exists (
            select 1 from public.household_adult_sessions
            where id = p_unlock_token
              and household_id = p_household_id
              and auth_session_id = v_auth_session
              and expires_at > v_now
        ) then
            return true;
        end if;
    end if;

    return false;
end;
$$;
revoke all on function public.fortress_is_adult_authorized(uuid, uuid) from public, anon, authenticated;

-- 4. PIN verification binds adult unlock to current Auth session
create or replace function public.fortress_verify_parental_pin(p_pin text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_auth_session text;
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_hash text;
    v_token uuid;
    v_expires timestamptz;
    v_failed int;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    v_auth_session := public.fortress_current_auth_session_id();

    select * into v_ctrl
    from public.household_parental_controls
    where household_id = v_uid
    for update;

    if not found or v_ctrl.pin_hash is null then
        v_expires := v_now + interval '15 minutes';
        insert into public.household_adult_sessions (household_id, auth_session_id, expires_at)
        values (v_uid, v_auth_session, v_expires) returning id into v_token;
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
        insert into public.household_adult_sessions (household_id, auth_session_id, expires_at)
        values (v_uid, v_auth_session, v_expires) returning id into v_token;

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

-- 5. Open profile session: invalidates adult authorizations for this Auth session upon entering child mode
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
        if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
            raise exception 'Adult authorization required to select adult profile';
        end if;
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
        'expires_at', v_expires
    );
end;
$$;
grant execute on function public.fortress_open_profile_session(uuid, uuid) to authenticated;

-- 6. Dedicated RPC to leave adult mode / revoke adult authorizations for this Auth session
create or replace function public.fortress_leave_adult_mode()
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_auth_session text;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    v_auth_session := public.fortress_current_auth_session_id();

    delete from public.household_adult_sessions
    where household_id = v_uid and auth_session_id = v_auth_session;

    delete from public.household_active_sessions
    where household_id = v_uid and auth_session_id = v_auth_session and kind = 'adult';

    return true;
end;
$$;
grant execute on function public.fortress_leave_adult_mode() to authenticated;

-- 7. Validate profile session with Auth session binding
create or replace function public.fortress_validate_profile_session(p_profile_id uuid)
returns boolean language plpgsql stable security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_auth_session text;
    v_header_text text;
    v_session_id uuid;
begin
    if v_uid is null or p_profile_id is null then
        return false;
    end if;

    begin
        v_auth_session := public.fortress_current_auth_session_id();
    exception when others then
        return false;
    end;

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
          and auth_session_id = v_auth_session
          and expires_at > clock_timestamp()
    );
end;
$$;
revoke all on function public.fortress_validate_profile_session(uuid) from public, anon;
grant execute on function public.fortress_validate_profile_session(uuid) to authenticated;

commit;
