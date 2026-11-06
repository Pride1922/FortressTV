-- Run as postgres against a disposable database after applying migration 1 and 2.
-- Plain SQL assertions; all fixtures roll back.
begin;

insert into auth.users (id) values
    ('10000000-0000-0000-0000-000000000001'),
    ('20000000-0000-0000-0000-000000000002');

set local role authenticated;
set local request.jwt.claim.sub = '10000000-0000-0000-0000-000000000001';

-- 1. Direct table insert/update/delete must be denied
do $$
begin
    begin
        insert into public.household_profiles(id, household_id, name, kind)
        values ('11000000-0000-0000-0000-000000000001', auth.uid(), 'Direct Insert', 'adult');
        raise exception 'Direct insert should have failed';
    exception when insufficient_privilege then null;
    end;
end;
$$;

-- 2. Direct access to parental controls table must be denied
do $$
begin
    begin
        perform * from public.household_parental_controls;
        raise exception 'Direct access to household_parental_controls should have failed';
    exception when insufficient_privilege then null;
    end;
    begin
        perform * from public.household_adult_sessions;
        raise exception 'Direct access to household_adult_sessions should have failed';
    exception when insufficient_privilege then null;
    end;
    begin
        perform * from public.household_active_sessions;
        raise exception 'Direct access to household_active_sessions should have failed';
    exception when insufficient_privilege then null;
    end;
end;
$$;

-- 3. Check initial PIN status: should have no PIN
do $$
declare
    v_status jsonb;
begin
    v_status := public.fortress_get_parental_pin_status();
    if (v_status->>'has_pin')::boolean <> false then
        raise exception 'Expected has_pin = false, got %', v_status;
    end if;
end;
$$;

-- 4. Create first adult profile via RPC (allowed since no PIN set yet)
do $$
declare
    v_pid uuid;
    v_prof record;
begin
    v_pid := public.fortress_create_profile('Parent Profile', 'adult', 'pt-PT', false, null, false);
    select * into v_prof from public.household_profiles where id = v_pid;
    if v_prof.name <> 'Parent Profile' or v_prof.kind <> 'adult' or v_prof.language <> 'pt-PT' then
        raise exception 'Profile not created properly: %', v_prof;
    end if;
end;
$$;

-- 5. Set parental PIN (e.g. '1234')
select public.fortress_set_parental_pin('1234');

-- Verify status now shows has_pin = true
do $$
declare
    v_status jsonb;
begin
    v_status := public.fortress_get_parental_pin_status();
    if (v_status->>'has_pin')::boolean <> true or (v_status->>'remaining_attempts')::int <> 5 then
        raise exception 'Expected has_pin = true, got %', v_status;
    end if;
end;
$$;

-- 6. Creating profile without PIN or unlock token must now FAIL
do $$
begin
    begin
        perform public.fortress_create_profile('Sneaky Child Profile', 'adult');
        raise exception 'Profile creation without PIN should have been rejected';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then
            raise exception 'Unexpected error: %', sqlerrm;
        end if;
    end;
end;
$$;

-- 7. Verify PIN with WRONG code: attempts should decrement
do $$
declare
    v_res jsonb;
begin
    v_res := public.fortress_verify_parental_pin('9999');
    if (v_res->>'verified')::boolean <> false or (v_res->>'remaining_attempts')::int <> 4 then
        raise exception 'Wrong PIN verification failed: %', v_res;
    end if;
end;
$$;

-- 8. Verify PIN with CORRECT code ('1234'): should return unlock token and reset attempts
do $$
declare
    v_res jsonb;
    v_token uuid;
    v_child_id uuid;
    v_prof record;
begin
    v_res := public.fortress_verify_parental_pin('1234');
    if (v_res->>'verified')::boolean <> true or v_res->>'unlock_token' is null then
        raise exception 'PIN verification failed: %', v_res;
    end if;
    v_token := (v_res->>'unlock_token')::uuid;

    -- Create child profile using unlock token
    v_child_id := public.fortress_create_profile(
        p_name => 'Kiddo',
        p_kind => 'child',
        p_language => 'en',
        p_hide_adult_content => false, -- should be forced to true for child
        p_maximum_age => 10,
        p_block_unrated => false,     -- should be forced to true for child
        p_unlock_token => v_token
    );

    select * into v_prof from public.household_profiles where id = v_child_id;
    if v_prof.hide_adult_content <> true or v_prof.block_unrated <> true or v_prof.maximum_age <> 10 then
        raise exception 'Child restrictions not enforced: %', v_prof;
    end if;
end;
$$;

-- 9. Open child profile session: allowed without PIN
do $$
declare
    v_child_id uuid;
    v_session jsonb;
begin
    select id into v_child_id from public.household_profiles where kind = 'child' limit 1;
    v_session := public.fortress_open_profile_session(v_child_id);
    if v_session->>'session_token' is null or v_session->>'kind' <> 'child' then
        raise exception 'Failed to open child profile session: %', v_session;
    end if;
end;
$$;

-- 10. Open adult profile session without PIN/token: must FAIL
do $$
declare
    v_adult_id uuid;
begin
    select id into v_adult_id from public.household_profiles where kind = 'adult' limit 1;
    begin
        perform public.fortress_open_profile_session(v_adult_id);
        raise exception 'Should have required adult authorization to open adult profile';
    exception when others then
        if sqlerrm not like '%Adult authorization required%' then
            raise exception 'Unexpected error: %', sqlerrm;
        end if;
    end;
end;
$$;

-- 11. Open adult profile session with adult unlock token: SUCCEEDS
do $$
declare
    v_adult_id uuid;
    v_res jsonb;
    v_token uuid;
    v_session jsonb;
begin
    v_res := public.fortress_verify_parental_pin('1234');
    v_token := (v_res->>'unlock_token')::uuid;

    select id into v_adult_id from public.household_profiles where kind = 'adult' limit 1;
    v_session := public.fortress_open_profile_session(v_adult_id, p_unlock_token => v_token);
    if v_session->>'session_token' is null or v_session->>'kind' <> 'adult' then
        raise exception 'Failed to open adult profile session: %', v_session;
    end if;
end;
$$;

rollback;
