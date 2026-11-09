-- FortressTV: Household OMDb API Key storage and RPCs
begin;

create table if not exists public.household_omdb (
    household_id uuid primary key references public.households(id) on delete cascade,
    enabled boolean not null default false,
    encrypted_credentials text not null,
    updated_at timestamptz not null default now()
);

alter table public.household_omdb enable row level security;
revoke all on public.household_omdb from public, anon, authenticated;

-- RPC: fortress_save_omdb
create or replace function public.fortress_save_omdb(
    p_enabled boolean,
    p_api_key text,
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
        'api_key', coalesce(btrim(p_api_key), '')
    );
    v_encrypted := encode(extensions.pgp_sym_encrypt(v_creds::text, v_key, 'cipher-algo=aes256'), 'base64');

    insert into public.household_omdb (
        household_id, enabled, encrypted_credentials, updated_at
    ) values (
        v_uid, coalesce(p_enabled, false),
        v_encrypted, clock_timestamp()
    ) on conflict (household_id) do update set
        enabled = excluded.enabled,
        encrypted_credentials = excluded.encrypted_credentials,
        updated_at = clock_timestamp();

    return jsonb_build_object('success', true);
end;
$$;
grant execute on function public.fortress_save_omdb(boolean, text, uuid) to authenticated;

-- RPC: fortress_get_omdb
create or replace function public.fortress_get_omdb(
    p_unlock_token uuid default null
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_key text;
    v_rec record;
    v_creds jsonb;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    select * into v_rec from public.household_omdb
    where household_id = v_uid;

    if not found then
        return jsonb_build_object('configured', false);
    end if;

    v_key := public.fortress_get_source_master_key();
    v_creds := extensions.pgp_sym_decrypt(decode(v_rec.encrypted_credentials, 'base64'), v_key, 'cipher-algo=aes256')::jsonb;

    return jsonb_build_object(
        'configured', true,
        'enabled', v_rec.enabled,
        'api_key', coalesce(v_creds->>'api_key', ''),
        'updated_at', v_rec.updated_at
    );
end;
$$;
grant execute on function public.fortress_get_omdb(uuid) to authenticated;

-- RPC: fortress_delete_omdb
create or replace function public.fortress_delete_omdb(
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    delete from public.household_omdb where household_id = v_uid;
    return true;
end;
$$;
grant execute on function public.fortress_delete_omdb(uuid) to authenticated;

commit;
