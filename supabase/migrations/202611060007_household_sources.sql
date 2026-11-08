-- Migration 7: Household-owned Xtream and custom EPG source connections
-- Uses pgcrypto PGP symmetric encryption with AES-256 cipher (RFC 4880 with integrity packet check).
-- Fail-closed key management: if server encryption key is missing or short, operation fails safely.
-- Enforces household ownership, foreign key consistency, and adult authorization.
begin;

create table if not exists public.household_sources (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete cascade,
    source_type text not null check (source_type in ('xtream', 'custom_epg')),
    display_name text not null check (length(btrim(display_name)) between 1 and 100),
    enabled boolean not null default true,
    source_order int not null default 0,
    encrypted_config text not null,
    parent_source_id uuid,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (household_id, id),
    foreign key (household_id, parent_source_id)
        references public.household_sources(household_id, id) on delete cascade
);

create index if not exists household_sources_sync_idx
    on public.household_sources(household_id, updated_at);

drop trigger if exists fortress_sources_updated on public.household_sources;
create trigger fortress_sources_updated before insert or update on public.household_sources
    for each row execute function public.fortress_stamp_updated_at();

alter table public.household_sources enable row level security;
revoke all on public.household_sources from public, anon, authenticated;

-- Fail-closed server-side encryption key lookup.
-- Encryption keys must never be committed or embedded in client APKs.
-- Configured via PostgreSQL configuration setting 'app.settings.source_encryption_key'
-- (e.g. Supabase Vault / Postgres custom settings).
create or replace function public.fortress_get_source_master_key()
returns text language plpgsql stable security definer set search_path = '' as $$
declare
    v_key text;
begin
    begin
        v_key := current_setting('app.settings.source_encryption_key', true);
    exception when others then
        v_key := null;
    end;

    if v_key is null or length(trim(v_key)) < 32 then
        raise exception 'Server source encryption key is not configured or insufficient (minimum 32 characters required)';
    end if;

    return trim(v_key);
end;
$$;
revoke all on function public.fortress_get_source_master_key() from public, anon, authenticated;

-- RPC 1: Retrieve all active household sources (decrypted), requiring adult authorization
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
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to access source connections';
    end if;

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
    where s.household_id = v_uid and s.deleted_at is null;

    return v_sources;
end;
$$;
grant execute on function public.fortress_get_household_sources(uuid) to authenticated;

-- RPC 2: Upsert household source connection, requiring adult authorization
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

    -- Validate parent source if specified
    if p_parent_source_id is not null then
        if not exists (
            select 1 from public.household_sources
            where household_id = v_uid and id = p_parent_source_id and deleted_at is null
        ) then
            raise exception 'Parent source does not exist or does not belong to this household';
        end if;
    end if;

    -- Prevent stale devices from resurrecting deleted sources
    if p_source_id is not null then
        select deleted_at into v_existing_deleted_at
        from public.household_sources
        where household_id = v_uid and id = p_source_id;

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
    ) on conflict (household_id, id) do update set
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

-- RPC 3: Delete (tombstone) household source connection, requiring adult authorization
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

    -- Tombstone the source and any associated custom EPG child sources
    update public.household_sources
    set deleted_at = clock_timestamp(),
        updated_at = clock_timestamp()
    where household_id = v_uid and (id = p_source_id or parent_source_id = p_source_id);

    return found;
end;
$$;
grant execute on function public.fortress_delete_household_source(uuid, uuid) to authenticated;

commit;
