-- Test Migration 8: Client sync versioning and server-enforced conflict protocol
\set ON_ERROR_STOP on

begin;

insert into auth.users (id) values ('00000000-0000-0000-0000-000000000001') on conflict do nothing;
set local request.jwt.claim.sub = '00000000-0000-0000-0000-000000000001';
insert into public.households (id) values ('00000000-0000-0000-0000-000000000001') on conflict do nothing;

insert into public.household_profiles (id, household_id, name, kind)
values ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Adult', 'adult')
on conflict do nothing;

-- Stub validate_profile_session to return true for test
create or replace function public.fortress_validate_profile_session(p_profile_id uuid) returns boolean as 'select true' language sql;

-- Test 1: Insert initial library item with client timestamp 1000
select public.fortress_sync_profile_library(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'channel',
    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    true,
    false,
    false,
    1000::bigint,
    null
);

-- Verify item is favourite with client_updated_at_ms = 1000
do $$
declare
    v_item public.profile_library%rowtype;
begin
    select * into v_item from public.profile_library
    where item_id = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
    assert v_item.is_favourite = true, 'Item should be favourite';
    assert v_item.client_updated_at_ms = 1000, 'Timestamp should be 1000';
end $$;

-- Test 2: Sequential offline edit with newer timestamp 1005 (unfavouriting)
select public.fortress_sync_profile_library(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'channel',
    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    false,
    false,
    false,
    1005::bigint,
    null
);

-- Verify newer edit won: is_favourite = false, client_updated_at_ms = 1005
do $$
declare
    v_item public.profile_library%rowtype;
begin
    select * into v_item from public.profile_library
    where item_id = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
    assert v_item.is_favourite = false, 'Item should be updated to false';
    assert v_item.client_updated_at_ms = 1005, 'Timestamp should be 1005';
end $$;

-- Test 3: Stale offline edit arriving with older timestamp 950 (favouriting)
select public.fortress_sync_profile_library(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'channel',
    'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    true,
    false,
    false,
    950::bigint,
    null
);

-- Verify stale edit was safely rejected: is_favourite remains false, timestamp remains 1005
do $$
declare
    v_item public.profile_library%rowtype;
begin
    select * into v_item from public.profile_library
    where item_id = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa';
    assert v_item.is_favourite = false, 'Stale edit must not overwrite newer edit';
    assert v_item.client_updated_at_ms = 1005, 'Timestamp must remain 1005';
end $$;

-- Test 4: Watch progress sync with rewind
select public.fortress_sync_profile_watch_progress(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'movie',
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
    50000::bigint,
    100000::bigint,
    false,
    2000::bigint,
    null
);

-- User rewinds to 10000 with newer timestamp 2010
select public.fortress_sync_profile_watch_progress(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'movie',
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
    10000::bigint,
    100000::bigint,
    false,
    2010::bigint,
    null
);

-- Verify rewind succeeded
do $$
declare
    v_prog public.profile_watch_progress%rowtype;
begin
    select * into v_prog from public.profile_watch_progress
    where item_id = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
    assert v_prog.position_ms = 10000, 'Rewind should update position to 10000';
    assert v_prog.client_updated_at_ms = 2010, 'Timestamp should be 2010';
end $$;

-- Test 5: Stale watch progress with older timestamp 1990
select public.fortress_sync_profile_watch_progress(
    '00000000-0000-0000-0000-000000000002'::uuid,
    '00000000-0000-0000-0000-000000000003'::uuid,
    'movie',
    'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
    40000::bigint,
    100000::bigint,
    false,
    1990::bigint,
    null
);

-- Verify stale watch progress was rejected: position remains 10000, timestamp remains 2010
do $$
declare
    v_prog public.profile_watch_progress%rowtype;
begin
    select * into v_prog from public.profile_watch_progress
    where item_id = 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb';
    assert v_prog.position_ms = 10000, 'Stale progress must not overwrite newer position';
    assert v_prog.client_updated_at_ms = 2010, 'Timestamp must remain 2010';
end $$;

rollback;
