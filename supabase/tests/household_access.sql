-- Run as postgres against a disposable database after migrations.
-- Plain SQL assertions; no pgTAP dependency. Every fixture is rolled back.
begin;

insert into auth.users (id) values
    ('10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002');

-- Seed fixture data directly as superuser
insert into public.households(id) values ('10000000-0000-0000-0000-000000000001');
insert into public.household_profiles(id, household_id, name, kind, language)
values ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Adult A', 'adult', 'pt-PT');

insert into public.household_active_sessions(id, household_id, profile_id, kind, auth_session_id)
values ('33333333-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001', 'adult', '10000000-0000-0000-0000-000000000001');

insert into public.profile_library(household_id, profile_id, source_id, item_kind, item_id, is_favourite)
values ('10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001',
    '33000000-0000-0000-0000-000000000003', 'channel', repeat('a',64), true);
insert into public.profile_watch_progress(household_id, profile_id, source_id, item_kind, item_id, position_ms)
values ('10000000-0000-0000-0000-000000000001', '11000000-0000-0000-0000-000000000001',
    '33000000-0000-0000-0000-000000000003', 'movie', repeat('b',64), 15000);

insert into public.households(id) values ('20000000-0000-0000-0000-000000000002');
insert into public.household_profiles(id, household_id, name, kind, maximum_age)
values ('22000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'Child B', 'child', 12);

insert into public.household_active_sessions(id, household_id, profile_id, kind, auth_session_id)
values ('33333333-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '22000000-0000-0000-0000-000000000002', 'child', '20000000-0000-0000-0000-000000000002');

-- Switch to User 2
set local role authenticated;
set local request.jwt.claim.sub = '20000000-0000-0000-0000-000000000002';
select set_config('request.headers', '{"x-profile-session": "33333333-0000-0000-0000-000000000002"}', true);

do $$
begin
    if (select count(*) from public.households) <> 1
       or (select count(*) from public.household_profiles) <> 1
       or exists(select from public.profile_library)
       or exists(select from public.profile_watch_progress) then
        raise exception 'Cross-household read';
    end if;
    update public.profile_library set is_favourite = false;
    if found then raise exception 'Cross-household library update'; end if;
    update public.profile_watch_progress set position_ms = 1;
    if found then raise exception 'Cross-household progress update'; end if;
    begin
        delete from public.household_profiles where id = '11000000-0000-0000-0000-000000000001';
        if found then raise exception 'Cross-household profile delete'; end if;
    exception when insufficient_privilege then null;
    end;
    begin
        insert into public.household_profiles(household_id, name, kind)
        values ('10000000-0000-0000-0000-000000000001', 'Intruder', 'adult');
        raise exception 'Cross-household profile insert accepted';
    exception when insufficient_privilege then null;
    end;
    begin
        insert into public.profile_watch_progress(household_id, profile_id, source_id, item_kind, item_id)
        values ('10000000-0000-0000-0000-000000000001',
            '11000000-0000-0000-0000-000000000001',
            '33000000-0000-0000-0000-000000000003', 'movie', repeat('d',64));
        raise exception 'Cross-household progress insert accepted';
    exception when insufficient_privilege then null;
    end;
    begin
        insert into public.profile_library(household_id, profile_id, source_id, item_kind, item_id)
        values ('10000000-0000-0000-0000-000000000001',
            '11000000-0000-0000-0000-000000000001',
            '33000000-0000-0000-0000-000000000003', 'movie', repeat('d',64));
        raise exception 'Cross-household library insert accepted';
    exception when insufficient_privilege then null;
    end;
    begin
        insert into public.profile_library(household_id, profile_id, source_id, item_kind, item_id)
        values (auth.uid(), '11000000-0000-0000-0000-000000000001',
            '33000000-0000-0000-0000-000000000003', 'movie', repeat('c',64));
        raise exception 'Foreign household profile attached';
    exception when foreign_key_violation then null;
              when insufficient_privilege then null;
    end;
    begin
        update public.household_profiles set household_id = '10000000-0000-0000-0000-000000000001';
        raise exception 'Ownership transfer accepted';
    exception when insufficient_privilege then null;
    end;
    begin
        update public.household_profiles set hide_adult_content = false;
        raise exception 'Unsafe child restrictions accepted';
    exception when insufficient_privilege then null;
              when check_violation then null;
    end;
end;
$$;

-- Switch to User 1 with User 1's profile session
set local request.jwt.claim.sub = '10000000-0000-0000-0000-000000000001';
select set_config('request.headers', '{"x-profile-session": "33333333-0000-0000-0000-000000000001"}', true);

update public.profile_library set is_favourite = false, deleted_at = now();
update public.profile_watch_progress set position_ms = 20000;
do $$
begin
    if not exists(select from public.profile_library where not is_favourite and deleted_at is not null)
        or not exists(select from public.profile_watch_progress where position_ms = 20000) then
        raise exception 'Owner writes failed';
    end if;
    begin
        delete from public.profile_library;
        raise exception 'Hard delete accepted';
    exception when insufficient_privilege then null;
    end;
end;
$$;

-- Authenticated role without a user JWT must see no household.
set local request.jwt.claim.sub = '';
do $$
begin
    if exists(select from public.households) or exists(select from public.household_profiles)
       or exists(select from public.profile_library) or exists(select from public.profile_watch_progress) then
        raise exception 'Missing JWT exposed data';
    end if;
end;
$$;

set local role anon;
do $$
begin
    begin
        perform * from public.households;
        raise exception 'Anonymous access accepted';
    exception when insufficient_privilege then null;
    end;
end;
$$;

rollback;
