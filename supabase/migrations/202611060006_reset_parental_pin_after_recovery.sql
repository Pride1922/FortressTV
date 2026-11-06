-- Migration: 202611060006_reset_parental_pin_after_recovery
-- Description: Allow household owner to reset parental PIN after verifying email OTP recovery

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

grant execute on function public.fortress_reset_parental_pin_after_recovery(text) to authenticated;
