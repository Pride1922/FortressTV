-- Regression test suite for Migration 5:
-- Securely revoking adult authorizations for a specific target Auth session within the household.
begin;

insert into auth.users (id) values
    ('10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002');

set local role authenticated;
set local request.jwt.claim.sub = '10000000-0000-0000-0000-000000000001';

-- Create an adult and a child profile for user 1
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

do $$
declare
    v_res jsonb;
    v_adult_id uuid;
    v_s1_unlock uuid;
    v_s1_adult_session jsonb;
    v_s1_adult_token uuid;
    v_s2_unlock uuid;
    v_s2_adult_session jsonb;
    v_s2_adult_token uuid;
begin
    select id into v_adult_id from public.household_profiles where kind = 'adult' limit 1;

    -- 1. Session S1 (Session A) verifies PIN and opens adult session
    perform set_config('request.jwt.claim.session_id', 'session-device-A', true);
    v_res := public.fortress_verify_parental_pin('1234');
    v_s1_unlock := (v_res->>'unlock_token')::uuid;

    v_s1_adult_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_s1_unlock);
    v_s1_adult_token := (v_s1_adult_session->>'session_token')::uuid;

    -- Add item to library with S1
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s1_adult_token::text)::text, true);
    insert into public.profile_library (household_id, profile_id, source_id, item_kind, item_id, is_favourite)
    values (auth.uid(), v_adult_id, gen_random_uuid(), 'movie', repeat('1', 64), true);

    -- 2. Session S2 (Session B) on the SAME account verifies PIN and opens adult session
    perform set_config('request.jwt.claim.session_id', 'session-device-B', true);
    v_res := public.fortress_verify_parental_pin('1234');
    v_s2_unlock := (v_res->>'unlock_token')::uuid;

    v_s2_adult_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_s2_unlock);
    v_s2_adult_token := (v_s2_adult_session->>'session_token')::uuid;

    -- Verify S2 can read adult library
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s2_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Session B should be able to read adult library';
    end if;

    -- 3. Now simulate cleanup from Session B revoking Session A specifically:
    -- perform fortress_leave_adult_mode with p_target_auth_session_id = 'session-device-A'
    perform set_config('request.jwt.claim.session_id', 'session-device-B', true);
    perform public.fortress_leave_adult_mode(p_target_auth_session_id => 'session-device-A');

    -- 4. VERIFY: Session A's adult unlock is revoked
    perform set_config('request.jwt.claim.session_id', 'session-device-A', true);
    begin
        perform public.fortress_create_profile('Should Fail S1', 'adult', 'en', false, null, false, p_unlock_token => v_s1_unlock);
        raise exception 'Session A unlock should have been revoked';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then raise; end if;
    end;

    -- VERIFY: Session A's adult active session is revoked
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s1_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 0 then
        raise exception 'Session A active session should be revoked and see 0 rows';
    end if;

    -- 5. CRITICAL: Session B MUST RETAIN its adult unlock and adult session!
    perform set_config('request.jwt.claim.session_id', 'session-device-B', true);
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s2_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Session B should NOT have lost adult access when Session A was revoked';
    end if;

    -- Session B's unlock token is also still valid
    perform public.fortress_update_profile(
        p_profile_id => v_adult_id,
        p_name => 'Parent Profile Updated By S2',
        p_kind => 'adult',
        p_language => 'pt-PT',
        p_hide_adult_content => false,
        p_maximum_age => null,
        p_block_unrated => false,
        p_unlock_token => v_s2_unlock
    );

    -- 6. Isolation check: another household (user 2) cannot revoke Session B of user 1
    perform set_config('request.jwt.claim.sub', '20000000-0000-0000-0000-000000000002', true);
    perform set_config('request.jwt.claim.session_id', 'session-attacker', true);
    perform public.fortress_leave_adult_mode(p_target_auth_session_id => 'session-device-B');

    -- Switch back to user 1 / session B: access is still intact!
    perform set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
    perform set_config('request.jwt.claim.session_id', 'session-device-B', true);
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_s2_adult_token::text)::text, true);
    if (select count(*) from public.profile_library where profile_id = v_adult_id) <> 1 then
        raise exception 'Attacker in user 2 should not be able to revoke user 1 Session B';
    end if;
end;
$$;

rollback;
