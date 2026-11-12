-- Migration: 202611120001_require_recovery_for_pin_reset
-- The post-recovery PIN reset previously accepted any signed-in session, so
-- anyone holding the TV could replace the parental PIN without knowing it.
-- It now requires an access token from a fresh proof of account ownership:
-- an email recovery code or a password sign-in (amr method "recovery" or
-- "password") made within the last 10 minutes. A long-lived TV session that
-- was refreshed keeps its original sign-in time and is rejected.
begin;

create or replace function public.fortress_has_recent_reauthentication()
returns boolean language plpgsql stable security definer set search_path = '' as $$
declare
    v_claims jsonb;
begin
    begin
        v_claims := nullif(current_setting('request.jwt.claims', true), '')::jsonb;
    exception when others then
        return false;
    end;
    if v_claims is null or jsonb_typeof(v_claims->'amr') <> 'array' then
        return false;
    end if;

    return exists (
        select 1
        from jsonb_array_elements(v_claims->'amr') as entry
        where jsonb_typeof(entry) = 'object'
          and entry->>'method' in ('recovery', 'password')
          and (entry->>'timestamp') ~ '^[0-9]{1,12}$'
          and to_timestamp((entry->>'timestamp')::bigint) > clock_timestamp() - interval '10 minutes'
    );
end;
$$;
revoke all on function public.fortress_has_recent_reauthentication() from public, anon, authenticated;

create or replace function public.fortress_reset_parental_pin_after_recovery(
    p_new_pin text
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_salt text;
    v_hash text;
begin
    if v_uid is null then
        raise exception 'Not authenticated';
    end if;

    if not public.fortress_has_recent_reauthentication() then
        raise exception 'Recent re-authentication required';
    end if;

    if p_new_pin is null or length(trim(p_new_pin)) < 4 or length(trim(p_new_pin)) > 12 or trim(p_new_pin) !~ '^[0-9]+$' then
        raise exception 'PIN must be between 4 and 12 digits';
    end if;

    v_salt := gen_random_uuid()::text;
    v_hash := public.fortress_hash_pin(v_salt, trim(p_new_pin));

    insert into public.household_parental_controls (
        household_id, pin_hash, pin_salt, failed_attempts, locked_until, updated_at
    ) values (
        v_uid, v_hash, v_salt, 0, null, clock_timestamp()
    )
    on conflict (household_id) do update set
        pin_hash = excluded.pin_hash,
        pin_salt = excluded.pin_salt,
        failed_attempts = 0,
        locked_until = null,
        updated_at = excluded.updated_at;

    delete from public.household_adult_sessions where household_id = v_uid;
    delete from public.household_active_sessions where household_id = v_uid and kind = 'adult';

    return true;
end;
$$;

revoke all on function public.fortress_reset_parental_pin_after_recovery(text) from public, anon;
grant execute on function public.fortress_reset_parental_pin_after_recovery(text) to authenticated;

commit;
