-- FortressTV: OpenSubtitles permissions adjustment
-- Removes adult pin unlock gate from OpenSubtitles so subtitles credentials
-- sync seamlessly across devices without requiring PIN entry.

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
