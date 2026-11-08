-- One Supabase Auth account owns one household. Profiles are not Auth users.
begin;

create table if not exists public.households (
    id uuid primary key references auth.users(id) on delete cascade,
    created_at timestamptz not null default now()
);

create table if not exists public.household_profiles (
    id uuid primary key default gen_random_uuid(),
    household_id uuid not null references public.households(id) on delete cascade,
    name text not null check (length(btrim(name)) between 1 and 60),
    kind text not null check (kind in ('adult', 'child')),
    language text not null default 'en' check (language in ('en', 'pt-PT')),
    hide_adult_content boolean not null default true,
    -- Null means no age ceiling; unknown ratings default to blocked.
    maximum_age smallint check (maximum_age between 0 and 18),
    block_unrated boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (household_id, id),
    check (kind <> 'child' or (hide_adult_content and block_unrated and maximum_age is not null))
);

-- Only opaque identity and user state belong here. Never upload stream URLs,
-- source configuration, provider credentials, titles, artwork URLs or EPG data.
create table if not exists public.profile_library (
    household_id uuid not null,
    profile_id uuid not null,
    source_id uuid not null,
    item_kind text not null check (item_kind in ('channel', 'movie', 'series', 'episode')),
    item_id text not null check (item_id ~ '^[0-9a-f]{64}$'),
    is_favourite boolean not null default false,
    is_watchlisted boolean not null default false,
    is_watched boolean not null default false,
    deleted_at timestamptz,
    updated_at timestamptz not null default now(),
    primary key (household_id, profile_id, source_id, item_kind, item_id),
    foreign key (household_id, profile_id)
        references public.household_profiles(household_id, id) on delete cascade
);

create table if not exists public.profile_watch_progress (
    household_id uuid not null,
    profile_id uuid not null,
    source_id uuid not null,
    item_kind text not null check (item_kind in ('movie', 'episode')),
    item_id text not null check (item_id ~ '^[0-9a-f]{64}$'),
    position_ms bigint not null default 0 check (position_ms >= 0),
    duration_ms bigint not null default 0 check (duration_ms >= 0),
    completed boolean not null default false,
    deleted_at timestamptz,
    updated_at timestamptz not null default now(),
    primary key (household_id, profile_id, source_id, item_kind, item_id),
    foreign key (household_id, profile_id)
        references public.household_profiles(household_id, id) on delete cascade,
    check (duration_ms = 0 or position_ms <= duration_ms)
);

create index if not exists profile_library_sync on public.profile_library(household_id, profile_id, updated_at);
create index if not exists profile_progress_sync on public.profile_watch_progress(household_id, profile_id, updated_at);
create index if not exists household_profiles_owner on public.household_profiles(household_id);

-- Invoker trigger: no RLS bypass or RPC privileges are needed.
create or replace function public.fortress_stamp_updated_at() returns trigger
language plpgsql set search_path = '' as $$
begin
    new.updated_at := clock_timestamp();
    return new;
end;
$$;
revoke all on function public.fortress_stamp_updated_at() from public, anon, authenticated;

drop trigger if exists fortress_profiles_updated on public.household_profiles;
create trigger fortress_profiles_updated before insert or update on public.household_profiles
    for each row execute function public.fortress_stamp_updated_at();

drop trigger if exists fortress_library_updated on public.profile_library;
create trigger fortress_library_updated before insert or update on public.profile_library
    for each row execute function public.fortress_stamp_updated_at();

drop trigger if exists fortress_progress_updated on public.profile_watch_progress;
create trigger fortress_progress_updated before insert or update on public.profile_watch_progress
    for each row execute function public.fortress_stamp_updated_at();

alter table public.households enable row level security;
alter table public.household_profiles enable row level security;
alter table public.profile_library enable row level security;
alter table public.profile_watch_progress enable row level security;

-- Explicit grants also override Supabase's default table privileges.
revoke all on public.households, public.household_profiles,
    public.profile_library, public.profile_watch_progress from public, anon, authenticated;
grant select, insert on public.households to authenticated;
grant select, insert, delete on public.household_profiles to authenticated;
grant update (name, kind, language, hide_adult_content, maximum_age, block_unrated)
    on public.household_profiles to authenticated;
grant select, insert on public.profile_library, public.profile_watch_progress to authenticated;
grant update (is_favourite, is_watchlisted, is_watched, deleted_at)
    on public.profile_library to authenticated;
grant update (position_ms, duration_ms, completed, deleted_at)
    on public.profile_watch_progress to authenticated;

drop policy if exists household_read on public.households;
create policy household_read on public.households for select to authenticated
    using (id = (select auth.uid()));

drop policy if exists household_create on public.households;
create policy household_create on public.households for insert to authenticated
    with check (id = (select auth.uid()));

drop policy if exists profiles_owner on public.household_profiles;
create policy profiles_owner on public.household_profiles for all to authenticated
    using (household_id = (select auth.uid()))
    with check (household_id = (select auth.uid()));

drop policy if exists library_owner on public.profile_library;
create policy library_owner on public.profile_library for all to authenticated
    using (household_id = (select auth.uid()))
    with check (household_id = (select auth.uid()));

drop policy if exists progress_owner on public.profile_watch_progress;
create policy progress_owner on public.profile_watch_progress for all to authenticated
    using (household_id = (select auth.uid()))
    with check (household_id = (select auth.uid()));

-- Library/progress removals use tombstones, not DELETE. Household deletion is
-- an authenticated server account-deletion flow through auth.users, later.
commit;
