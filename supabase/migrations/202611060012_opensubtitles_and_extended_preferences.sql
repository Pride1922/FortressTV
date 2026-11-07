-- Migration 12: Sync for OpenSubtitles credentials & extended profile preferences (startup_destination, epg_time_shift_hours)
begin;

-- 1. Extend profile_preferences table with startup_destination and epg_time_shift_hours
alter table public.profile_preferences
    add column if not exists startup_destination text not null default 'home',
    add column if not exists epg_time_shift_hours int not null default 0;

-- Update fortress_save_profile_preferences to accept startup_destination and epg_time_shift_hours
create or replace function public.fortress_save_profile_preferences(
    p_profile_id uuid,
    p_audio_lang text default 'default',
    p_subtitle_lang text default 'off',
    p_sub_fallback boolean default true,
    p_startup_destination text default 'home',
    p_epg_time_shift_hours int default 0
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    if not exists (select 1 from public.household_profiles where household_id = v_uid and id = p_profile_id) then
        raise exception 'Profile does not belong to this household';
    end if;

    insert into public.profile_preferences (
        household_id, profile_id, preferred_audio_language,
        preferred_subtitle_language, subtitle_fallback_english,
        startup_destination, epg_time_shift_hours, updated_at
    ) values (
        v_uid, p_profile_id,
        coalesce(p_audio_lang, 'default'),
        coalesce(p_subtitle_lang, 'off'),
        coalesce(p_sub_fallback, true),
        coalesce(p_startup_destination, 'home'),
        coalesce(p_epg_time_shift_hours, 0),
        now()
    )
    on conflict (household_id, profile_id) do update set
        preferred_audio_language = excluded.preferred_audio_language,
        preferred_subtitle_language = excluded.preferred_subtitle_language,
        subtitle_fallback_english = excluded.subtitle_fallback_english,
        startup_destination = excluded.startup_destination,
        epg_time_shift_hours = excluded.epg_time_shift_hours,
        updated_at = excluded.updated_at;

    return jsonb_build_object('success', true);
end;
$$;
grant execute on function public.fortress_save_profile_preferences(uuid, text, text, boolean, text, int) to authenticated;

-- Update fortress_get_profile_preferences to return startup_destination and epg_time_shift_hours
create or replace function public.fortress_get_profile_preferences(
    p_profile_id uuid
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_pref record;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    select * into v_pref from public.profile_preferences
    where household_id = v_uid and profile_id = p_profile_id;

    if not found then
        return jsonb_build_object(
            'preferred_audio_language', 'default',
            'preferred_subtitle_language', 'off',
            'subtitle_fallback_english', true,
            'startup_destination', 'home',
            'epg_time_shift_hours', 0
        );
    end if;

    return jsonb_build_object(
        'preferred_audio_language', v_pref.preferred_audio_language,
        'preferred_subtitle_language', v_pref.preferred_subtitle_language,
        'subtitle_fallback_english', v_pref.subtitle_fallback_english,
        'startup_destination', coalesce(v_pref.startup_destination, 'home'),
        'epg_time_shift_hours', coalesce(v_pref.epg_time_shift_hours, 0)
    );
end;
$$;
grant execute on function public.fortress_get_profile_preferences(uuid) to authenticated;

-- 2. OpenSubtitles Encrypted Storage
create table if not exists public.household_opensubtitles (
    household_id uuid primary key references public.households(id) on delete cascade,
    enabled boolean not null default false,
    prefer_pt_pt boolean not null default true,
    auto_search boolean not null default true,
    encrypted_credentials text not null,
    updated_at timestamptz not null default now()
);

alter table public.household_opensubtitles enable row level security;
revoke all on public.household_opensubtitles from public, anon, authenticated;

-- RPC: fortress_save_opensubtitles
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
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to manage subtitle credentials';
    end if;

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

-- RPC: fortress_get_opensubtitles
create or replace function public.fortress_get_opensubtitles(
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

    select * into v_rec from public.household_opensubtitles
    where household_id = v_uid;

    if not found then
        return jsonb_build_object('configured', false);
    end if;

    v_key := public.fortress_get_source_master_key();
    v_creds := extensions.pgp_sym_decrypt(decode(v_rec.encrypted_credentials, 'base64'), v_key, 'cipher-algo=aes256')::jsonb;

    return jsonb_build_object(
        'configured', true,
        'enabled', v_rec.enabled,
        'prefer_pt_pt', v_rec.prefer_pt_pt,
        'auto_search', v_rec.auto_search,
        'api_key', coalesce(v_creds->>'api_key', ''),
        'username', coalesce(v_creds->>'username', ''),
        'password', coalesce(v_creds->>'password', ''),
        'updated_at', v_rec.updated_at
    );
end;
$$;
grant execute on function public.fortress_get_opensubtitles(uuid) to authenticated;

-- RPC: fortress_delete_opensubtitles
create or replace function public.fortress_delete_opensubtitles(
    p_unlock_token uuid default null
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;
    if not public.fortress_is_adult_authorized(v_uid, p_unlock_token) then
        raise exception 'Adult authorization required to remove subtitle credentials';
    end if;

    delete from public.household_opensubtitles where household_id = v_uid;
    return found;
end;
$$;
grant execute on function public.fortress_delete_opensubtitles(uuid) to authenticated;

commit;
