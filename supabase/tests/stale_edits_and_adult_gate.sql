-- Regression tests for 202611120002 (stale sync edits) and 202611120003 (adult profile gate).
begin;

insert into auth.users (id) values ('40000000-0000-0000-0000-000000000004');
set local role authenticated;
set local request.jwt.claim.sub = '40000000-0000-0000-0000-000000000004';
set local request.jwt.claim.session_id = 'device-tv';

-- Part 1: stale edits and clock skew -------------------------------------------------------------
do $$
declare
    v_adult uuid := public.fortress_create_profile('Parent', 'adult', 'en');
    v_source uuid := gen_random_uuid();
    v_item text := repeat('c', 64);
    v_now bigint := (extract(epoch from clock_timestamp()) * 1000)::bigint;
    v_res jsonb;
    v_row record;
begin
    v_res := public.fortress_open_profile_session(v_adult);
    perform set_config('request.headers', jsonb_build_object('x-profile-session', v_res->>'session_token')::text, true);

    -- A newer edit applies; an older one arriving later is ignored but still reports success.
    perform public.fortress_sync_profile_library(v_adult, v_source, 'movie', v_item, true, true, false, v_now - 1000);
    perform public.fortress_sync_profile_library(v_adult, v_source, 'movie', v_item, true, true, true, v_now);
    v_res := public.fortress_sync_profile_library(v_adult, v_source, 'movie', v_item, true, true, false, v_now - 500);
    select * into v_row from public.profile_library where item_id = v_item;
    if not v_row.is_watched then raise exception 'FAIL: stale edit reset watched'; end if;
    if (v_res->>'success')::boolean is not true or (v_res->>'applied')::boolean is not false then
        raise exception 'FAIL: stale edit should report success=true applied=false, got %', v_res;
    end if;

    -- A device clock a day ahead is clamped to server time, so it cannot block later edits.
    perform public.fortress_sync_profile_library(v_adult, v_source, 'movie', v_item, true, true, false, v_now + 86400000);
    select * into v_row from public.profile_library where item_id = v_item;
    if v_row.client_updated_at_ms > v_now + 6 * 60 * 1000 then
        raise exception 'FAIL: future client time was stored unclamped: %', v_row.client_updated_at_ms;
    end if;

    -- A future time stored before this migration does not block a newer edit.
    update public.profile_library set client_updated_at_ms = v_now + 86400000 where item_id = v_item;
    perform public.fortress_sync_profile_library(v_adult, v_source, 'movie', v_item, true, false, true, v_now + 1000);
    select * into v_row from public.profile_library where item_id = v_item;
    if v_row.is_watchlisted then raise exception 'FAIL: stored future time blocked a newer edit'; end if;

    -- Watch progress follows the same rule.
    perform public.fortress_sync_profile_watch_progress(v_adult, v_source, 'movie', v_item, 90000, 100000, false, v_now);
    perform public.fortress_sync_profile_watch_progress(v_adult, v_source, 'movie', v_item, 10000, 100000, false, v_now - 1);
    select * into v_row from public.profile_watch_progress where item_id = v_item;
    if v_row.position_ms <> 90000 then raise exception 'FAIL: stale progress overwrote newer position'; end if;

    raise notice 'PASS: stale edits are ignored and clock skew is contained';
end;
$$;

-- Part 2: adult profile gate ---------------------------------------------------------------------
do $$
declare
    v_adult uuid;
    v_child uuid := public.fortress_create_profile('Kid', 'child', 'en', true, 12, true);
    v_token uuid;
    v_blocked boolean;
begin
    select id into v_adult from public.household_profiles where kind = 'adult' limit 1;
    perform public.fortress_set_parental_pin('2468');

    -- Device already in adult mode (or starting fresh): no PIN needed, so syncing keeps working.
    perform public.fortress_open_profile_session(v_adult);

    -- The TV switches to the child profile.
    perform public.fortress_open_profile_session(v_child);

    -- Another device of the household is not in child mode: it still opens the adult profile.
    perform set_config('request.jwt.claim.session_id', 'device-phone', true);
    perform public.fortress_open_profile_session(v_adult);

    -- On the TV, leaving child mode without the PIN is refused.
    perform set_config('request.jwt.claim.session_id', 'device-tv', true);
    v_blocked := false;
    begin
        perform public.fortress_open_profile_session(v_adult);
    exception when others then
        v_blocked := sqlerrm like '%Adult authorization required%';
    end;
    if not v_blocked then raise exception 'FAIL: child device opened the adult profile without the PIN'; end if;

    -- With the PIN it works, and afterwards the TV is in adult mode again (no PIN on the next open).
    v_token := (public.fortress_verify_parental_pin('2468')->>'unlock_token')::uuid;
    perform public.fortress_open_profile_session(v_adult, v_token);
    perform public.fortress_leave_adult_mode();
    perform public.fortress_open_profile_session(v_adult);

    raise notice 'PASS: adult profile needs the PIN only when leaving child mode on that device';
end;
$$;

rollback;
