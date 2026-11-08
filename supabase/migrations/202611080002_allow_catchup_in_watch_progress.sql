-- Migration 16: Allow catchup item kind in profile watch progress
begin;

alter table public.profile_watch_progress
    drop constraint if exists profile_watch_progress_item_kind_check;

alter table public.profile_watch_progress
    add constraint profile_watch_progress_item_kind_check
    check (item_kind in ('movie', 'episode', 'catchup'));

commit;
