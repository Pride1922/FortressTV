-- Regression test suite for Migration 4:
-- Auth session binding, child entry adult revocation, and device isolation.
begin;

insert into auth.users (id) values
    ('10000000-0000-0000-0000-000000000001');

set local role authenticated;
set local request.jwt.claim.sub = '10000000-0000-0000-0000-000000000001';

-- Create an adult and a child profile
do $$
declare
    v_adult_id uuid;
    v_child_id uuid;
begin
    v_adult_id := public.fortress_create_profile('Parent Profile', 'adult', 'pt-PT');
    v_child_id := public.fortress_create_profile('Kid Profile', 'child', 'en');
end;
$$;

-- Configure PIN
select public.fortress_set_parental_pin('1234');

-- 1. Device 1 (Session S1) verifies PIN and opens adult profile session
set local request.jwt.claim.session_id = 'session-device-1';
do $$
declare
    v_res jsonb;
    v_adult_id uuid;
    v_child_id uuid;
    v_s1_unlock uuid;
    v_s1_adult_session jsonb;
    v_s1_adult_token uuid;
    v_s2_unlock uuid;
    v_s2_adult_session jsonb;
    v_s2_adult_token uuid;
    v_s1_child_session jsonb;
begin
    select id into v_adult_id from public.household_profiles where kind = 'adult' limit 1;
    select id into v_child_id from public.household_profiles where kind = 'child' limit 1;

    -- Device 1 verifies PIN -> obtains adult unlock token
    v_res := public.fortress_verify_parental_pin('1234');
    v_s1_unlock := (v_res->>'unlock_token')::uuid;

    -- Device 1 opens adult profile session
    v_s1_adult_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_s1_unlock);
    v_s1_adult_token := (v_s1_adult_session->>'session_token')::uuid;

    -- Verify Device 1 adult session is active on library
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s1_adult_token::text)::text, true);
    insert into public.profile_library (household_id, profile_id, source_id, item_kind, item_id, is_favourite)
    values (auth.uid(), v_adult_id, gen_random_uuid(), 'movie', repeat('1', 64), true);

    -- 2. Device 2 (Session S2) verifies PIN and opens adult profile session
    perform set_config('request.jwt.claim.session_id', 'session-device-2', true);
    v_res := public.fortress_verify_parental_pin('1234');
    v_s2_unlock := (v_res->>'unlock_token')::uuid;

    v_s2_adult_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_s2_unlock);
    v_s2_adult_token := (v_s2_adult_session->>'session_token')::uuid;

    -- Verify Device 2 adult session is active on library
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s2_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Device 2 should be able to read adult library';
    end if;

    -- 3. Device 1 (Session S1) switches to child mode
    perform set_config('request.jwt.claim.session_id', 'session-device-1', true);
    v_s1_child_session := public.fortress_open_profile_session(v_child_id);

    -- A. Device 1's previous adult unlock token MUST now be invalidated
    begin
        perform public.fortress_create_profile('Should Fail', 'adult', 'en', false, null, false, p_unlock_token => v_s1_unlock);
        raise exception 'Device 1 old unlock token should have been revoked upon entering child mode';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then raise; end if;
    end;

    -- B. Device 1's previous adult profile session token MUST now be invalidated
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s1_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 0 then
        raise exception 'Device 1 old adult session should be revoked and see 0 rows';
    end if;

    -- 4. CRITICAL: Device 2 (Session S2) MUST RETAIN its adult access!
    perform set_config('request.jwt.claim.session_id', 'session-device-2', true);
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s2_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Device 2 should NOT have lost adult access when Device 1 entered child mode';
    end if;

    -- Device 2's unlock token is also still valid
    perform public.fortress_update_profile(
        p_profile_id => v_adult_id,
        p_name => 'Parent Profile Updated',
        p_kind => 'adult',
        p_language => 'pt-PT',
        p_hide_adult_content => false,
        p_maximum_age => null,
        p_block_unrated => false,
        p_unlock_token => v_s2_unlock
    );

    -- 5. Cross-session token stealing must fail:
    -- Device 1 cannot use Device 2's session token
    perform set_config('request.jwt.claim.session_id', 'session-device-1', true);
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s2_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 0 then
        raise exception 'Device 1 should NOT be able to use Device 2 session token';
    end if;
end;
$$;

rollback;
