begin;

-- Both preferences RPC signatures use this household-scoped conflict target.
-- The primary key on profile_id alone cannot satisfy that conflict target.
create unique index if not exists profile_preferences_household_profile_uidx
    on public.profile_preferences (household_id, profile_id);

commit;
