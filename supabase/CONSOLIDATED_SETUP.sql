-- Consolidated FortressTV Profiles, Parental Control & Session Security Setup
-- Applies the complete schema, tables, indexes, RPCs, and RLS policies (Migrations 2 through 5) idempotently.
begin;

create extension if not exists "pgcrypto" with schema extensions;

-- 1. Tables & Columns
create table if not exists public.household_parental_controls (
    household_id uuid primary key references public.households(id) on delete cascade,
    pin_hash text,
    pin_salt text,
    failed_attempts int not null default 0 check (failed_attempts >= 0),
    locked_until timestamptz,
    updated_at timestamptz not null default clock_timestamp()
);

alter table public.household_parental_controls enable row level security;
revoke all on public.household_parental_controls from public, anon, authenticated;

create table if not exists public.household_adult_sessions (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete cascade,
    auth_session_id text not null default 'default',
    created_at timestamptz not null default clock_timestamp(),
    expires_at timestamptz not null default (clock_timestamp() + interval '15 minutes')
);

alter table public.household_adult_sessions
    add column if not exists auth_session_id text not null default 'default';

create index if not exists household_adult_sessions_auth_lookup
    on public.household_adult_sessions(household_id, auth_session_id, id, expires_at);

alter table public.household_adult_sessions enable row level security;
revoke all on public.household_adult_sessions from public, anon, authenticated;

create table if not exists public.household_active_sessions (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete cascade,
    profile_id uuid not null,
    kind text not null check (kind in ('adult', 'child')),
    auth_session_id text not null default 'default',
    created_at timestamptz not null default clock_timestamp(),
    expires_at timestamptz not null default (clock_timestamp() + interval '7 days')
);

alter table public.household_active_sessions
    add column if not exists auth_session_id text not null default 'default';

create index if not exists household_active_sessions_auth_lookup
    on public.household_active_sessions(household_id, auth_session_id, id, profile_id, expires_at);

alter table public.household_active_sessions enable row level security;
revoke all on public.household_active_sessions from public, anon, authenticated;

-- 2. Drop obsolete function signatures
drop function if exists public.fortress_is_adult_authorized(uuid, uuid, text);
drop function if exists public.fortress_set_parental_pin(text, text, uuid);
drop function if exists public.fortress_remove_parental_pin(text, uuid);
drop function if exists public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid, text);
drop function if exists public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid, text);
drop function if exists public.fortress_delete_profile(uuid, uuid, text);
drop function if exists public.fortress_open_profile_session(uuid, text, uuid);
drop function if exists public.fortress_leave_adult_mode();

-- 3. Core cryptographic and session-binding helpers
create or replace function public.fortress_hash_pin(p_salt text, p_pin text)
returns text language sql immutable security definer set search_path = '' as $$
    select encode(extensions.digest(p_salt || ':' || p_pin, 'sha256'), 'hex');
$$;
revoke all on function public.fortress_hash_pin(text, text) from public, anon, authenticated;

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

create or replace function public.fortress_get_pin_status()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_locked boolean;
    v_seconds int := 0;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    select * into v_ctrl from public.household_parental_controls where household_id = v_uid;
    if not found or v_ctrl.pin_hash is null then
        return jsonb_build_object(
            'has_pin', false,
            'is_locked', false,
            'remaining_attempts', 5,
            'lockout_seconds', 0
        );
    end if;

    v_locked := (v_ctrl.locked_until is not null and v_ctrl.locked_until > v_now);
    if v_locked then
        v_seconds := greatest(1, ceil(extract(epoch from (v_ctrl.locked_until - v_now))))::int;
    end if;

    return jsonb_build_object(
        'has_pin', true,
        'is_locked', v_locked,
        'remaining_attempts', case when v_locked then 0 else greatest(0, 5 - v_ctrl.failed_attempts) end,
        'lockout_seconds', v_seconds
    );
end;
$$;
grant execute on function public.fortress_get_pin_status() to authenticated;

-- 4. PIN verification and management
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

create or replace function public.fortress_set_parental_pin(
    p_pin text,
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
    if p_pin is null or length(trim(p_pin)) < 4 or length(trim(p_pin)) > 12 or trim(p_pin) !~ '^[0-9]+$' then
        raise exception 'PIN must be between 4 and 12 digits';
    end if;

    v_salt := gen_random_uuid()::text;
    v_hash := public.fortress_hash_pin(v_salt, trim(p_pin));

    insert into public.household_parental_controls (
        household_id, pin_hash, pin_salt, failed_attempts, locked_until, updated_at
    ) values (
        v_uid, v_hash, v_salt, 0, null, clock_timestamp()
    )
    on conflict (household_id) do update set
        pin_hash = excluded.pin_hash,
        pin_salt = excluded.pin_salt,
        failed_attempts = 0,
        locked_until = null,
        updated_at = excluded.updated_at;

    delete from public.household_adult_sessions where household_id = v_uid;
    delete from public.household_active_sessions where household_id = v_uid and kind = 'adult';

    return true;
end;
$$;
grant execute on function public.fortress_set_parental_pin(text, uuid) to authenticated;

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

    update public.household_parental_controls
    set pin_hash = null,
        pin_salt = null,
        failed_attempts = 0,
        locked_until = null,
        updated_at = clock_timestamp()
    where household_id = v_uid;

    delete from public.household_adult_sessions where household_id = v_uid;
    delete from public.household_active_sessions where household_id = v_uid and kind = 'adult';

    return true;
end;
$$;
grant execute on function public.fortress_remove_parental_pin(uuid) to authenticated;

-- 5. Profile management
create or replace function public.fortress_create_profile(
    p_name text,
    p_kind text default 'adult',
    p_language text default 'en',
    p_hide_adult_content boolean default null,
    p_maximum_age int default null,
    p_block_unrated boolean default null,
    p_unlock_token uuid default null
)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_id uuid;
    v_is_child boolean := (p_kind = 'child');
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
        hide_adult_content, maximum_age, block_unrated
    ) values (
        v_uid,
        trim(p_name),
        p_kind,
        coalesce(p_language, 'en'),
        case when v_is_child then true else coalesce(p_hide_adult_content, false) end,
        case when v_is_child then coalesce(p_maximum_age, 12) else null end,
        case when v_is_child then true else coalesce(p_block_unrated, false) end
    ) returning id into v_id;

    return v_id;
end;
$$;
grant execute on function public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid) to authenticated;

create or replace function public.fortress_update_profile(
    p_profile_id uuid,
    p_name text,
    p_kind text,
    p_language text default 'en',
    p_hide_adult_content boolean default null,
    p_maximum_age int default null,
    p_block_unrated boolean default null,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_is_child boolean := (p_kind = 'child');
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to update profile';
    end if;

    if not exists (select 1 from public.household_profiles where id = p_profile_id and household_id = v_uid) then
        raise exception 'Profile not found';
    end if;

    if p_kind = 'child' then
        delete from public.household_active_sessions where household_id = v_uid and profile_id = p_profile_id;
    end if;

    update public.household_profiles
    set name = trim(p_name),
        kind = p_kind,
        language = coalesce(p_language, 'en'),
        hide_adult_content = case when v_is_child then true else coalesce(p_hide_adult_content, false) end,
        maximum_age = case when v_is_child then coalesce(p_maximum_age, 12) else null end,
        block_unrated = case when v_is_child then true else coalesce(p_block_unrated, false) end,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid) to authenticated;

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

    if v_kind = 'child' and not p_hide_adult_content then
        raise exception 'Child profiles must hide adult content';
    end if;

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

create or replace function public.fortress_set_profile_language(
    p_profile_id uuid,
    p_language text
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_lang text := case when p_language in ('pt-PT', 'pt') then 'pt-PT' else 'en' end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    update public.household_profiles set
        language = v_lang,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    if not found then raise exception 'Profile not found'; end if;
    return true;
end;
$$;
revoke all on function public.fortress_set_profile_language(uuid, text) from public, anon;
grant execute on function public.fortress_set_profile_language(uuid, text) to authenticated;

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
    if (select count(*) from public.household_profiles where household_id = v_uid) <= 1 then
        raise exception 'Cannot delete the only profile';
    end if;

    delete from public.household_profiles where id = p_profile_id and household_id = v_uid;
    return true;
end;
$$;
grant execute on function public.fortress_delete_profile(uuid, uuid) to authenticated;

-- 6. Profile session management and validation
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

-- 7. Adult access departure & targeted revocation
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

-- 8. Enforce scoped profile sessions on library and watch progress via RLS
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

create or replace function public.fortress_reset_parental_pin_after_recovery(
    p_new_pin text
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_salt text;
    v_hash text;
begin
    if v_uid is null then
        raise exception 'Not authenticated';
    end if;

    if p_new_pin is null or length(trim(p_new_pin)) < 4 or length(trim(p_new_pin)) > 12 or trim(p_new_pin) !~ '^[0-9]+$' then
        raise exception 'PIN must be between 4 and 12 digits';
    end if;

    v_salt := gen_random_uuid()::text;
    v_hash := public.fortress_hash_pin(v_salt, trim(p_new_pin));

    insert into public.household_parental_controls (
        household_id, pin_hash, pin_salt, failed_attempts, locked_until, updated_at
    ) values (
        v_uid, v_hash, v_salt, 0, null, clock_timestamp()
    )
    on conflict (household_id) do update set
        pin_hash = excluded.pin_hash,
        pin_salt = excluded.pin_salt,
        failed_attempts = 0,
        locked_until = null,
        updated_at = excluded.updated_at;

    delete from public.household_adult_sessions where household_id = v_uid;
    delete from public.household_active_sessions where household_id = v_uid and kind = 'adult';

    return true;
end;
$$;
grant execute on function public.fortress_reset_parental_pin_after_recovery(text) to authenticated;

-- 18. Sports Fixtures and Calendar
create table if not exists public.sports_fixtures (
  id text primary key,
  sport text not null default 'football',
  competition text not null,
  competition_logo_url text,
  home_team text not null,
  away_team text not null,
  home_team_logo_url text,
  away_team_logo_url text,
  kickoff_utc timestamptz not null,
  broadcaster_official text not null,
  status text not null default 'scheduled' check (status in ('scheduled', 'live', 'finished', 'postponed')),
  minute text,
  home_score int,
  away_score int,
  venue text,
  updated_at timestamptz not null default now()
);

create index if not exists idx_sports_fixtures_kickoff_status
  on public.sports_fixtures (kickoff_utc asc, status);

create index if not exists idx_sports_fixtures_sport
  on public.sports_fixtures (sport, kickoff_utc asc);

alter table public.sports_fixtures enable row level security;

drop policy if exists sports_fixtures_read_all on public.sports_fixtures;
create policy sports_fixtures_read_all on public.sports_fixtures
  for select
  using (true);

grant select on public.sports_fixtures to anon, authenticated;

commit;
