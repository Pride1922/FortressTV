-- Safe to run more than once: the Supabase GitHub integration re-applies migrations that were
-- first run by hand in the SQL editor (they are missing from its migration history).
begin;

create table if not exists public.profile_trakt_config (
    household_id uuid not null,
    profile_id uuid not null,
    client_id text not null check (length(client_id) between 20 and 256),
    primary key (household_id, profile_id),
    foreign key (household_id, profile_id) references public.household_profiles(household_id, id) on delete cascade
);

alter table public.profile_trakt_config enable row level security;
revoke all on public.profile_trakt_config from public, anon, authenticated;

create or replace function public.fortress_get_trakt_client_id(p_profile_id uuid)
returns jsonb language sql stable security definer set search_path = '' as $$
    select jsonb_build_object('client_id', coalesce((
        select client_id from public.profile_trakt_config
        where household_id = auth.uid() and profile_id = p_profile_id
    ), ''));
$$;

create or replace function public.fortress_save_trakt_client_id(p_profile_id uuid, p_client_id text)
returns void language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null or not exists (
        select 1 from public.household_profiles where household_id = auth.uid() and id = p_profile_id
    ) then
        raise exception 'Profile does not belong to this household';
    end if;

    insert into public.profile_trakt_config (household_id, profile_id, client_id)
    values (auth.uid(), p_profile_id, btrim(p_client_id))
    on conflict (household_id, profile_id) do nothing;
end;
$$;

revoke all on function public.fortress_get_trakt_client_id(uuid) from public, anon;
revoke all on function public.fortress_save_trakt_client_id(uuid, text) from public, anon;
grant execute on function public.fortress_get_trakt_client_id(uuid) to authenticated;
grant execute on function public.fortress_save_trakt_client_id(uuid, text) to authenticated;

commit;
