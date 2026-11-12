-- Regression test for Migration 202611120001:
-- The post-recovery parental PIN reset must require a fresh recovery code or
-- password sign-in, not just any signed-in session.
begin;

insert into auth.users (id) values ('30000000-0000-0000-0000-000000000003');

set local role authenticated;
set local request.jwt.claim.sub = '30000000-0000-0000-0000-000000000003';

select public.fortress_set_parental_pin('1234');

do $$
declare
    v_blocked boolean;
    v_now bigint := extract(epoch from clock_timestamp())::bigint;
begin
    -- 1. Plain session with no amr claim is rejected.
    v_blocked := false;
    begin
        perform public.fortress_reset_parental_pin_after_recovery('5678');
    exception when others then
        v_blocked := true;
    end;
    if not v_blocked then raise exception 'FAIL: reset allowed without re-authentication'; end if;

    -- 2. A long-lived session (password sign-in an hour ago) is rejected.
    perform set_config('request.jwt.claims', jsonb_build_object('amr',
        jsonb_build_array(jsonb_build_object('method', 'password', 'timestamp', v_now - 3600)))::text, true);
    v_blocked := false;
    begin
        perform public.fortress_reset_parental_pin_after_recovery('5678');
    exception when others then
        v_blocked := true;
    end;
    if not v_blocked then raise exception 'FAIL: reset allowed with a stale sign-in'; end if;

    -- 3. Other recent methods (e.g. a fresh sign-up code) are rejected.
    perform set_config('request.jwt.claims', jsonb_build_object('amr',
        jsonb_build_array(jsonb_build_object('method', 'otp', 'timestamp', v_now)))::text, true);
    v_blocked := false;
    begin
        perform public.fortress_reset_parental_pin_after_recovery('5678');
    exception when others then
        v_blocked := true;
    end;
    if not v_blocked then raise exception 'FAIL: reset allowed with a non-recovery method'; end if;

    -- 4. A fresh recovery code is accepted and the new PIN works.
    perform set_config('request.jwt.claims', jsonb_build_object('amr',
        jsonb_build_array(jsonb_build_object('method', 'recovery', 'timestamp', v_now)))::text, true);
    if not public.fortress_reset_parental_pin_after_recovery('5678') then
        raise exception 'FAIL: reset rejected after fresh recovery';
    end if;

    -- 5. A fresh password sign-in is accepted.
    perform set_config('request.jwt.claims', jsonb_build_object('amr',
        jsonb_build_array(jsonb_build_object('method', 'password', 'timestamp', v_now - 60)))::text, true);
    if not public.fortress_reset_parental_pin_after_recovery('9012') then
        raise exception 'FAIL: reset rejected after fresh password sign-in';
    end if;

    raise notice 'PASS: parental PIN reset requires recent re-authentication';
end;
$$;

rollback;
