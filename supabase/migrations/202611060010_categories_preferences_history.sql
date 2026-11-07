-- Migration 10: Sync for hidden categories, category order, audio/subtitle preferences, and recent channels
begin;

-- Table 1: Category customizations per profile and source
create table if not exists public.profile_source_categories (
    household_id uuid not null references public.households(id) on delete cascade,
    profile_id uuid not null references public.household_profiles(id) on delete cascade,
    source_id uuid not null,
    hidden_categories jsonb not null default '[]'::jsonb,
    category_order jsonb not null default '[]'::jsonb,
    updated_at timestamptz not null default now(),
    primary key (household_id, profile_id, source_id)
);

alter table public.profile_source_categories enable row level security;

drop policy if exists profile_source_categories_household_all on public.profile_source_categories;
create policy profile_source_categories_household_all on public.profile_source_categories
    for all to authenticated
    using (household_id = auth.uid())
    with check (household_id = auth.uid());

-- Table 2: Audio & subtitle playback preferences per profile
create table if not exists public.profile_preferences (
    household_id uuid not null references public.households(id) on delete cascade,
    profile_id uuid primary key references public.household_profiles(id) on delete cascade,
    preferred_audio_language text not null default 'default',
    preferred_subtitle_language text not null default 'off',
    subtitle_fallback_english boolean not null default true,
    updated_at timestamptz not null default now()
);

alter table public.profile_preferences enable row level security;

drop policy if exists profile_preferences_household_all on public.profile_preferences;
create policy profile_preferences_household_all on public.profile_preferences
    for all to authenticated
    using (household_id = auth.uid())
    with check (household_id = auth.uid());

-- Table 3: Recent channel viewing history per profile (bounded)
create table if not exists public.profile_recent_channels (
    household_id uuid not null references public.households(id) on delete cascade,
    profile_id uuid primary key references public.household_profiles(id) on delete cascade,
    recent_channels jsonb not null default '[]'::jsonb,
    updated_at timestamptz not null default now()
);

alter table public.profile_recent_channels enable row level security;

drop policy if exists profile_recent_channels_household_all on public.profile_recent_channels;
create policy profile_recent_channels_household_all on public.profile_recent_channels
    for all to authenticated
    using (household_id = auth.uid())
    with check (household_id = auth.uid());

-- RPC 1: Upsert categories for a profile and source
create or replace function public.fortress_save_profile_categories(
    p_profile_id uuid,
    p_source_id uuid,
    p_hidden_categories jsonb,
    p_category_order jsonb
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    if not exists (select 1 from public.household_profiles where household_id = v_uid and id = p_profile_id) then
        raise exception 'Profile does not belong to this household';
    end if;

    insert into public.profile_source_categories (
        household_id, profile_id, source_id, hidden_categories, category_order, updated_at
    ) values (
        v_uid, p_profile_id, p_source_id,
        coalesce(p_hidden_categories, '[]'::jsonb),
        coalesce(p_category_order, '[]'::jsonb),
        now()
    )
    on conflict (household_id, profile_id, source_id) do update set
        hidden_categories = excluded.hidden_categories,
        category_order = excluded.category_order,
        updated_at = excluded.updated_at;

    return jsonb_build_object('success', true);
end;
$$;

-- RPC 2: Get categories for all sources of a profile
create or replace function public.fortress_get_profile_categories(
    p_profile_id uuid
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_result jsonb;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    select coalesce(jsonb_agg(
        jsonb_build_object(
            'source_id', source_id,
            'hidden_categories', hidden_categories,
            'category_order', category_order,
            'updated_at', updated_at
        )
    ), '[]'::jsonb)
    into v_result
    from public.profile_source_categories
    where household_id = v_uid and profile_id = p_profile_id;

    return v_result;
end;
$$;

-- RPC 3: Upsert audio & subtitle preferences for a profile
create or replace function public.fortress_save_profile_preferences(
    p_profile_id uuid,
    p_audio_lang text,
    p_subtitle_lang text,
    p_sub_fallback boolean
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
        preferred_subtitle_language, subtitle_fallback_english, updated_at
    ) values (
        v_uid, p_profile_id,
        coalesce(p_audio_lang, 'default'),
        coalesce(p_subtitle_lang, 'off'),
        coalesce(p_sub_fallback, true),
        now()
    )
    on conflict (household_id, profile_id) do update set
        preferred_audio_language = excluded.preferred_audio_language,
        preferred_subtitle_language = excluded.preferred_subtitle_language,
        subtitle_fallback_english = excluded.subtitle_fallback_english,
        updated_at = excluded.updated_at;

    return jsonb_build_object('success', true);
end;
$$;

-- RPC 4: Get preferences for a profile
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
            'subtitle_fallback_english', true
        );
    end if;

    return jsonb_build_object(
        'preferred_audio_language', v_pref.preferred_audio_language,
        'preferred_subtitle_language', v_pref.preferred_subtitle_language,
        'subtitle_fallback_english', v_pref.subtitle_fallback_english
    );
end;
$$;

-- RPC 5: Upsert recent channels for a profile (bounded to 20)
create or replace function public.fortress_save_profile_recent_channels(
    p_profile_id uuid,
    p_recent_channels jsonb
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    if not exists (select 1 from public.household_profiles where household_id = v_uid and id = p_profile_id) then
        raise exception 'Profile does not belong to this household';
    end if;

    insert into public.profile_recent_channels (
        household_id, profile_id, recent_channels, updated_at
    ) values (
        v_uid, p_profile_id,
        coalesce(p_recent_channels, '[]'::jsonb),
        now()
    )
    on conflict (household_id, profile_id) do update set
        recent_channels = excluded.recent_channels,
        updated_at = excluded.updated_at;

    return jsonb_build_object('success', true);
end;
$$;

-- RPC 6: Clear recent channels for a profile
create or replace function public.fortress_clear_profile_recent_channels(
    p_profile_id uuid
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    update public.profile_recent_channels
    set recent_channels = '[]'::jsonb, updated_at = now()
    where household_id = v_uid and profile_id = p_profile_id;

    return jsonb_build_object('success', true);
end;
$$;

-- RPC 7: Get recent channels for a profile
create or replace function public.fortress_get_profile_recent_channels(
    p_profile_id uuid
)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_channels jsonb;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    select recent_channels into v_channels from public.profile_recent_channels
    where household_id = v_uid and profile_id = p_profile_id;

    return coalesce(v_channels, '[]'::jsonb);
end;
$$;

-- Grants
grant execute on function public.fortress_save_profile_categories(uuid, uuid, jsonb, jsonb) to authenticated;
grant execute on function public.fortress_get_profile_categories(uuid) to authenticated;
grant execute on function public.fortress_save_profile_preferences(uuid, text, text, boolean) to authenticated;
grant execute on function public.fortress_get_profile_preferences(uuid) to authenticated;
grant execute on function public.fortress_save_profile_recent_channels(uuid, jsonb) to authenticated;
grant execute on function public.fortress_clear_profile_recent_channels(uuid) to authenticated;
grant execute on function public.fortress_get_profile_recent_channels(uuid) to authenticated;

grant select, insert, update, delete on public.profile_source_categories to authenticated;
grant select, insert, update, delete on public.profile_preferences to authenticated;
grant select, insert, update, delete on public.profile_recent_channels to authenticated;

commit;
