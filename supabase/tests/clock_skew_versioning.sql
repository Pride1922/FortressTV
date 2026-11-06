-- Test Migration 9: Last-write-wins atomic RPC upsert
\set ON_ERROR_STOP on

begin;

insert into auth.users (id) values ('00000000-0000-0000-0000-000000000001') on conflict do nothing;
insert into public.households (id) values ('00000000-0000-0000-0000-000000000001') on conflict do nothing;

insert into public.household_profiles (id, household_id, name, kind)
values ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Adult', 'adult')
on conflict do nothing;

create or replace function public.fortress_validate_profile_session(p_profile_id uuid) returns boolean as 'select true' language sql;
select set_config('request.jwt.claim.sub', '00000000-0000-0000-0000-000000000001', true);

-- Test 1: Device A marks item as favorite
select public.fortress_sync_profile_library(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'movie',
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
    true,
    true,
    false,
    1000::bigint,
    null
);

do $$
declare
    v_item public.profile_library%rowtype;
begin
    select * into v_item from public.profile_library
    where item_id = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
    assert v_item.is_favourite = true, 'Item should be saved as favourite';
end $$;

-- Test 2: Device B unfavorites item; last write wins
select public.fortress_sync_profile_library(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'movie',
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
    false,
    false,
    false,
    2000::bigint,
    null
);

do $$
declare
    v_item public.profile_library%rowtype;
begin
    select * into v_item from public.profile_library
    where item_id = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
    assert v_item.is_favourite = false, 'Last write wins must update is_favourite to false';
end $$;

-- Test 3: Watch progress upsert
select public.fortress_sync_profile_watch_progress(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'episode',
    'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
    60000::bigint,
    120000::bigint,
    false,
    1000::bigint,
    null
);

do $$
declare
    v_prog public.profile_watch_progress%rowtype;
begin
    select * into v_prog from public.profile_watch_progress
    where item_id = 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc';
    assert v_prog.position_ms = 60000, 'Position saved';
end $$;

-- Test 4: Watch progress update
select public.fortress_sync_profile_watch_progress(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'episode',
    'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc',
    90000::bigint,
    120000::bigint,
    false,
    2000::bigint,
    null
);

do $$
declare
    v_prog public.profile_watch_progress%rowtype;
begin
    select * into v_prog from public.profile_watch_progress
    where item_id = 'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc';
    assert v_prog.position_ms = 90000, 'Newer position applied';
end $$;

rollback;
