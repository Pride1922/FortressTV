-- Comprehensive security test suite for profile authorization hardening,
-- PIN attempt preservation, lockout enforcement, and session-scoped RLS.
begin;

insert into auth.users (id) values
    ('10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002');

set local role authenticated;
set local request.jwt.claim.sub = '10000000-0000-0000-0000-000000000001';

-- 1. Initial state: create household and initial adult profile
do $$
declare
    v_adult_id uuid;
begin
    v_adult_id := public.fortress_create_profile('Adult One', 'adult', 'pt-PT');
    if v_adult_id is null then raise exception 'Failed to create initial adult profile'; end if;
end;
$$;

-- 2. Set parental PIN to '5678'
select public.fortress_set_parental_pin('5678');

-- 3. Verify PIN guessing with wrong PINs: attempts must decrement and persist
do $$
declare
    v_res jsonb;
    v_i int;
begin
    -- 4 wrong attempts
    for v_i in 1..4 loop
        v_res := public.fortress_verify_parental_pin('0000');
        if (v_res->>'verified')::boolean <> false then
            raise exception 'Expected verified=false on wrong pin';
        end if;
        if (v_res->>'remaining_attempts')::int <> (5 - v_i) then
            raise exception 'Attempt %: expected % remaining, got %', v_i, (5 - v_i), v_res;
        end if;
        if (v_res->>'locked')::boolean <> false then
            raise exception 'Should not be locked yet at attempt %', v_i;
        end if;
    end loop;

    -- 5th wrong attempt triggers lockout
    v_res := public.fortress_verify_parental_pin('0000');
    if (v_res->>'locked')::boolean <> true or (v_res->>'remaining_attempts')::int <> 0 then
        raise exception '5th attempt should lock out: %', v_res;
    end if;
    if (v_res->>'lock_seconds')::int <= 0 then
        raise exception 'Expected positive lock_seconds on lockout: %', v_res;
    end if;

    -- 6th attempt while locked: stays locked, even with correct PIN
    v_res := public.fortress_verify_parental_pin('5678');
    if (v_res->>'locked')::boolean <> true or (v_res->>'verified')::boolean <> false then
        raise exception 'Locked state should reject even correct PIN: %', v_res;
    end if;
end;
$$;

-- 4. Protected RPCs MUST reject calls without valid unlock token and cannot be used to guess PINs
do $$
begin
    begin
        perform public.fortress_remove_parental_pin();
        raise exception 'remove_parental_pin without token should fail';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then
            raise exception 'Unexpected error: %', sqlerrm;
        end if;
    end;

    begin
        perform public.fortress_create_profile('Kid', 'child');
        raise exception 'create_profile without token should fail when PIN set';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then
            raise exception 'Unexpected error: %', sqlerrm;
        end if;
    end;

    begin
        perform public.fortress_set_parental_pin('9999');
        raise exception 'set_parental_pin without token should fail when PIN set';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then
            raise exception 'Unexpected error: %', sqlerrm;
        end if;
    end;
end;
$$;

-- Reset lockout for subsequent testing by switching back to superuser temporarily
reset role;
update public.household_parental_controls
set failed_attempts = 0, locked_until = null
where household_id = '10000000-0000-0000-0000-000000000001';
set local role authenticated;
set local request.jwt.claim.sub = '10000000-0000-0000-0000-000000000001';

-- 5. Obtain valid adult unlock token with correct PIN
do $$
declare
    v_res jsonb;
    v_token uuid;
    v_child_id uuid;
    v_adult_id uuid;
    v_child_session jsonb;
    v_adult_session jsonb;
    v_child_token uuid;
    v_adult_token uuid;
    v_header text;
begin
    v_res := public.fortress_verify_parental_pin('5678');
    if (v_res->>'verified')::boolean <> true then
        raise exception 'PIN verification failed: %', v_res;
    end if;
    v_token := (v_res->>'unlock_token')::uuid;

    -- Create child profile using unlock token
    v_child_id := public.fortress_create_profile(
        p_name => 'Child One',
        p_kind => 'child',
        p_language => 'pt-PT',
        p_hide_adult_content => false,
        p_maximum_age => 8,
        p_block_unrated => false,
        p_unlock_token => v_token
    );

    select id into v_adult_id from public.household_profiles where kind = 'adult' limit 1;

    -- Adult profile session requires unlock token
    v_adult_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_token);
    v_adult_token := (v_adult_session->>'session_token')::uuid;

    -- Test adult library access while in adult mode
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_adult_token::text)::text, true);

    insert into public.profile_library (household_id, profile_id, source_id, item_kind, item_id, is_favourite)
    values (auth.uid(), v_adult_id, gen_random_uuid(), 'movie', repeat('2', 64), true);

    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Adult session should be able to read adult profile library';
    end if;

    -- Child profile session can be opened without unlock token (enters child mode)
    v_child_session := public.fortress_open_profile_session(v_child_id);
    v_child_token := (v_child_session->>'session_token')::uuid;

    -- Now test RLS on profile_library and profile_watch_progress:
    -- Case A: No session header -> direct insert must FAIL
    perform set_config('request.headers', '{}', true);
    begin
        insert into public.profile_library (household_id, profile_id, source_id, item_kind, item_id, is_favourite)
        values (auth.uid(), v_child_id, gen_random_uuid(), 'movie', repeat('1', 64), true);
        raise exception 'Insert without session header should have failed RLS';
    exception when others then
        if sqlerrm like '%Insert without session header%' then raise; end if;
    end;

    -- Case B: Set child session header -> inserting into child profile library SUCCEEDS
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_child_token::text)::text, true);

    insert into public.profile_library (household_id, profile_id, source_id, item_kind, item_id, is_favourite)
    values (auth.uid(), v_child_id, gen_random_uuid(), 'movie', repeat('1', 64), true);

    insert into public.profile_watch_progress (household_id, profile_id, source_id, item_kind, item_id, position_ms)
    values (auth.uid(), v_child_id, gen_random_uuid(), 'movie', repeat('1', 64), 5000);

    -- Child session CAN read child profile data
    if (select count(*) from public.profile_library where profile_id = v_child_id) <> 1 then
        raise exception 'Child should be able to read own profile library';
    end if;

    -- Case C: Child session attempting to insert or read adult profile data MUST BE DENIED
    begin
        insert into public.profile_library (household_id, profile_id, source_id, item_kind, item_id, is_favourite)
        values (auth.uid(), v_adult_id, gen_random_uuid(), 'movie', repeat('3', 64), true);
        raise exception 'Child session should NOT be able to insert into adult profile library';
    exception when others then
        if sqlerrm like '%Child session should NOT%' then raise; end if;
    end;

    -- Case D: Old adult session token was invalidated by child mode entry and now sees 0 rows
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 0 then
        raise exception 'Old adult session should see 0 rows after child mode entered';
    end if;

    -- Case E: Fake/forged session header MUST BE DENIED
    perform set_config('request.headers', jsonb_build_object('x-profile-session', gen_random_uuid()::text)::text, true);
    if (select count(*) from public.profile_library) <> 0 then
        raise exception 'Forged session token should see 0 rows';
    end if;
end;
$$;

-- 6. Test PIN change invalidates previous adult unlock tokens and adult active sessions
do $$
declare
    v_res jsonb;
    v_token uuid;
    v_adult_id uuid;
    v_adult_session jsonb;
    v_session_token uuid;
begin
    -- Verify PIN to get unlock token
    v_res := public.fortress_verify_parental_pin('5678');
    v_token := (v_res->>'unlock_token')::uuid;

    select id into v_adult_id from public.household_profiles where kind = 'adult' limit 1;
    v_adult_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_token);
    v_session_token := (v_adult_session->>'session_token')::uuid;

    -- Verify session token works
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_session_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Adult session should be active before PIN change';
    end if;

    -- Change PIN to '9876' using valid unlock token
    perform public.fortress_set_parental_pin('9876', p_unlock_token => v_token);

    -- Old unlock token must now be invalid
    begin
        perform public.fortress_remove_parental_pin(p_unlock_token => v_token);
        raise exception 'Old unlock token should be invalid after PIN change';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then
            raise exception 'Unexpected error: %', sqlerrm;
        end if;
    end;

    -- Old adult session token must now be invalid on profile_library access
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_session_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 0 then
        raise exception 'Adult session should have been invalidated after PIN change';
    end if;
end;
$$;

rollback;
