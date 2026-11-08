-- Migration 2: Secure parental controls, non-readable PIN store, and profile RPCs.
-- Protects children from adults within an account by revoking direct profile writes
-- and enforcing server-side salted PIN verification, attempt limits, and short-lived unlocks.
begin;

-- Revoke direct table mutations from authenticated clients on household_profiles.
-- Authenticated users retain SELECT (protected by household_id = auth.uid() RLS).
revoke insert, update, delete on public.household_profiles from authenticated;

-- Private PIN-verifier store. NEVER readable or writable directly by clients.
create table if not exists public.household_parental_controls (
    household_id uuid primary key references public.households(id) on delete cascade,
    pin_salt text not null,
    pin_hash text not null,
    failed_attempts int not null default 0 check (failed_attempts >= 0),
    locked_until timestamptz,
    updated_at timestamptz not null default now()
);

alter table public.household_parental_controls enable row level security;
revoke all on public.household_parental_controls from public, anon, authenticated;

-- Short-lived adult unlocks issued upon valid PIN verification.
create table if not exists public.household_adult_sessions (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete cascade,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '15 minutes')
);

create index if not exists household_adult_sessions_lookup
    on public.household_adult_sessions(household_id, id, expires_at);

alter table public.household_adult_sessions enable row level security;
revoke all on public.household_adult_sessions from public, anon, authenticated;

-- Scoped profile sessions foundation.
create table if not exists public.household_active_sessions (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete cascade,
    profile_id uuid not null,
    kind text not null check (kind in ('adult', 'child')),
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '7 days'),
    foreign key (household_id, profile_id)
        references public.household_profiles(household_id, id) on delete cascade
);

create index if not exists household_active_sessions_lookup
    on public.household_active_sessions(household_id, id, profile_id, expires_at);

alter table public.household_active_sessions enable row level security;
revoke all on public.household_active_sessions from public, anon, authenticated;

-- Private iterative salted PIN hashing (10,000 SHA-256 rounds).
create or replace function public.fortress_hash_pin(p_salt text, p_pin text)
returns text language plpgsql security definer set search_path = '' as $$
declare
    v_digest bytea;
    v_i int;
begin
    if p_pin is null or length(p_pin) < 4 or length(p_pin) > 12 or p_pin !~ '^[0-9]+$' then
        raise exception 'PIN must be between 4 and 12 digits';
    end if;
    v_digest := (p_salt || ':' || p_pin)::bytea;
    for v_i in 1..10000 loop
        v_digest := sha256(v_digest);
    end loop;
    return encode(v_digest, 'hex');
end;
$$;
revoke all on function public.fortress_hash_pin(text, text) from public, anon, authenticated;

-- Private authorization check for adult actions.
create or replace function public.fortress_is_adult_authorized(
    p_household_id uuid,
    p_unlock_token uuid default null,
    p_pin text default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_hash text;
begin
    select * into v_ctrl from public.household_parental_controls where household_id = p_household_id;
    if not found or v_ctrl.pin_hash is null then
        return true;
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

    if v_ctrl.locked_until is not null and v_ctrl.locked_until > v_now then
        return false;
    end if;

    if p_pin is not null then
        v_hash := public.fortress_hash_pin(v_ctrl.pin_salt, p_pin);
        if v_hash = v_ctrl.pin_hash then
            update public.household_parental_controls
            set failed_attempts = 0, locked_until = null, updated_at = v_now
            where household_id = p_household_id;
            return true;
        else
            v_ctrl.failed_attempts := v_ctrl.failed_attempts + 1;
            update public.household_parental_controls
            set failed_attempts = v_ctrl.failed_attempts,
                locked_until = case when v_ctrl.failed_attempts >= 5 then v_now + interval '15 minutes' else null end,
                updated_at = v_now
            where household_id = p_household_id;
            return false;
        end if;
    end if;

    return false;
end;
$$;
revoke all on function public.fortress_is_adult_authorized(uuid, uuid, text) from public, anon, authenticated;

-- RPC 1: Check parental PIN status without disclosing the PIN or hash.
create or replace function public.fortress_get_parental_pin_status()
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_is_locked boolean := false;
    v_lock_seconds int := 0;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    select * into v_ctrl from public.household_parental_controls where household_id = v_uid;
    if not found or v_ctrl.pin_hash is null then
        return jsonb_build_object(
            'has_pin', false,
            'is_locked', false,
            'remaining_attempts', 5,
            'lock_seconds', 0
        );
    end if;

    if v_ctrl.locked_until is not null and v_ctrl.locked_until > v_now then
        v_is_locked := true;
        v_lock_seconds := greatest(0, ceil(extract(epoch from (v_ctrl.locked_until - v_now))))::int;
    end if;

    return jsonb_build_object(
        'has_pin', true,
        'is_locked', v_is_locked,
        'remaining_attempts', greatest(0, 5 - v_ctrl.failed_attempts),
        'lock_seconds', v_lock_seconds
    );
end;
$$;
grant execute on function public.fortress_get_parental_pin_status() to authenticated;

-- RPC 2: Verify PIN and obtain a short-lived adult unlock token.
create or replace function public.fortress_verify_parental_pin(p_pin text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_ctrl record;
    v_now timestamptz := clock_timestamp();
    v_hash text;
    v_token uuid;
    v_expires timestamptz;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    select * into v_ctrl from public.household_parental_controls where household_id = v_uid;
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
            'lock_seconds', ceil(extract(epoch from (v_ctrl.locked_until - v_now)))::int
        );
    end if;

    v_hash := public.fortress_hash_pin(v_ctrl.pin_salt, p_pin);
    if v_hash = v_ctrl.pin_hash then
        update public.household_parental_controls
        set failed_attempts = 0, locked_until = null, updated_at = v_now
        where household_id = v_uid;

        v_expires := v_now + interval '15 minutes';
        insert into public.household_adult_sessions (household_id, expires_at)
        values (v_uid, v_expires) returning id into v_token;

        return jsonb_build_object('verified', true, 'unlock_token', v_token, 'expires_at', v_expires);
    else
        v_ctrl.failed_attempts := v_ctrl.failed_attempts + 1;
        update public.household_parental_controls
        set failed_attempts = v_ctrl.failed_attempts,
            locked_until = case when v_ctrl.failed_attempts >= 5 then v_now + interval '15 minutes' else null end,
            updated_at = v_now
        where household_id = v_uid;

        return jsonb_build_object(
            'verified', false,
            'locked', (v_ctrl.failed_attempts >= 5),
            'remaining_attempts', greatest(0, 5 - v_ctrl.failed_attempts),
            'lock_seconds', case when v_ctrl.failed_attempts >= 5 then 900 else 0 end
        );
    end if;
end;
$$;
grant execute on function public.fortress_verify_parental_pin(text) to authenticated;

-- RPC 3: Set or change parental PIN.
create or replace function public.fortress_set_parental_pin(
    p_new_pin text,
    p_old_pin text default null,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_salt text;
    v_hash text;
    v_authorized boolean;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    insert into public.households (id) values (v_uid) on conflict (id) do nothing;

    v_authorized := public.fortress_is_adult_authorized(v_uid, p_unlock_token, p_old_pin);
    if not v_authorized then
        raise exception 'Unauthorized to configure parental PIN';
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

    return true;
end;
$$;
grant execute on function public.fortress_set_parental_pin(text, text, uuid) to authenticated;

-- RPC 4: Remove parental PIN.
create or replace function public.fortress_remove_parental_pin(
    p_pin text default null,
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_authorized boolean;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    v_authorized := public.fortress_is_adult_authorized(v_uid, p_unlock_token, p_pin);
    if not v_authorized then
        raise exception 'Unauthorized to remove parental PIN';
    end if;

    delete from public.household_parental_controls where household_id = v_uid;
    delete from public.household_adult_sessions where household_id = v_uid;
    return true;
end;
$$;
grant execute on function public.fortress_remove_parental_pin(text, uuid) to authenticated;

-- RPC 5: Create profile with server-enforced child restrictions and adult authorization.
drop function if exists public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid, text);
drop function if exists public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid);
create or replace function public.fortress_create_profile(
    p_name text,
    p_kind text,
    p_language text default 'en',
    p_hide_adult_content boolean default true,
    p_maximum_age int default null,
    p_block_unrated boolean default true,
    p_unlock_token uuid default null,
    p_pin text default null
)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_profile_id uuid;
    v_effective_hide boolean := p_hide_adult_content;
    v_effective_block boolean := p_block_unrated;
    v_effective_age smallint := case when p_maximum_age is not null then p_maximum_age::smallint else null end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token, p_pin) then
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
        v_uid, btrim(p_name), p_kind, p_language, v_effective_hide, v_effective_age, v_effective_block
    ) returning id into v_profile_id;

    return v_profile_id;
end;
$$;
grant execute on function public.fortress_create_profile(text, text, text, boolean, int, boolean, uuid, text) to authenticated;

-- RPC 6: Update profile with server-enforced child restrictions and adult authorization.
drop function if exists public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid, text);
drop function if exists public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid);
create or replace function public.fortress_update_profile(
    p_profile_id uuid,
    p_name text,
    p_kind text,
    p_language text,
    p_hide_adult_content boolean,
    p_maximum_age int,
    p_block_unrated boolean,
    p_unlock_token uuid default null,
    p_pin text default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_effective_hide boolean := p_hide_adult_content;
    v_effective_block boolean := p_block_unrated;
    v_effective_age smallint := case when p_maximum_age is not null then p_maximum_age::smallint else null end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token, p_pin) then
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
    end if;

    update public.household_profiles
    set name = btrim(p_name),
        kind = p_kind,
        language = p_language,
        hide_adult_content = v_effective_hide,
        maximum_age = v_effective_age,
        block_unrated = v_effective_block,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    return true;
end;
$$;
grant execute on function public.fortress_update_profile(uuid, text, text, text, boolean, int, boolean, uuid, text) to authenticated;

-- RPC 7: Delete profile with adult authorization.
create or replace function public.fortress_delete_profile(
    p_profile_id uuid,
    p_unlock_token uuid default null,
    p_pin text default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token, p_pin) then
        raise exception 'Adult authorization required to delete profile';
    end if;

    delete from public.household_profiles where id = p_profile_id and household_id = v_uid;
    return true;
end;
$$;
grant execute on function public.fortress_delete_profile(uuid, uuid, text) to authenticated;

-- RPC 8: Open scoped profile session. Child profiles can be opened freely;
-- adult profiles require adult PIN or unlock token.
create or replace function public.fortress_open_profile_session(
    p_profile_id uuid,
    p_pin text default null,
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
        if not public.fortress_is_adult_authorized(v_uid, p_unlock_token, p_pin) then
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
grant execute on function public.fortress_open_profile_session(uuid, text, uuid) to authenticated;

commit;
