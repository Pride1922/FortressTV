-- FortressTV: dedicated RPC to persist a profile's UI language.
-- The interface language is not a parental-control setting, so it must not
-- require adult PIN authorization (fortress_update_profile does).
begin;

create or replace function public.fortress_set_profile_language(
    p_profile_id uuid,
    p_language text
)
returns boolean language plpgsql security definer set search_path = '' as $$
declare
    v_uid uuid := auth.uid();
    v_lang text := case when p_language in ('pt-PT', 'pt') then 'pt-PT' else 'en' end;
begin
    if v_uid is null then raise exception 'Not authenticated'; end if;

    update public.household_profiles set
        language = v_lang,
        updated_at = clock_timestamp()
    where id = p_profile_id and household_id = v_uid;

    if not found then raise exception 'Profile not found'; end if;
    return true;
end;
$$;
revoke all on function public.fortress_set_profile_language(uuid, text) from public, anon;
grant execute on function public.fortress_set_profile_language(uuid, text) to authenticated;

commit;
