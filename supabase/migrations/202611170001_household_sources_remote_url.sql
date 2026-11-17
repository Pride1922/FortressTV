-- Allow M3U/M3U8 remote URL playlists alongside Xtream and custom EPG sources
begin;

alter table public.household_sources
    drop constraint if exists household_sources_source_type_check;

alter table public.household_sources
    add constraint household_sources_source_type_check
    check (source_type in ('xtream', 'remote_url', 'custom_epg'));

commit;
