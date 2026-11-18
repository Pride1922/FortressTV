-- Synthetic accounts and configurations only. All changes roll back.
begin;
insert into auth.users (id) values
 ('91000000-0000-0000-0000-000000000001'),
 ('91000000-0000-0000-0000-000000000002');
select set_config('app.settings.source_encryption_key', 'synthetic-isolation-test-key-at-least-32-characters', true);
select set_config('request.jwt.claim.sub', '91000000-0000-0000-0000-000000000001', true);
set local role authenticated;
select public.fortress_save_household_source(
 '92000000-0000-0000-0000-000000000001', 'remote_url', 'Isolation test', true, 0,
 '{"url":"https://example.invalid/test.m3u"}'::jsonb);
select set_config('request.jwt.claim.sub', '91000000-0000-0000-0000-000000000002', true);
do $$
begin
 if public.fortress_get_household_sources() <> '[]'::jsonb then
  raise exception 'Cross-account read exposed sources';
 end if;
 begin
  perform public.fortress_save_household_source(
   '92000000-0000-0000-0000-000000000001', 'remote_url', 'Overwrite', true, 0, '{}'::jsonb);
  raise exception 'Cross-account update was allowed';
 exception when others then
  if sqlerrm not like '%does not belong to this household%' then raise; end if;
 end;
 begin
  perform public.fortress_save_household_source(
   '92000000-0000-0000-0000-000000000002', 'custom_epg', 'Foreign parent', true, 0,
   '{}'::jsonb, '92000000-0000-0000-0000-000000000001');
  raise exception 'Cross-account parent was allowed';
 exception when others then
  if sqlerrm not like '%does not belong to this household%' then raise; end if;
 end;
 if public.fortress_delete_household_source('92000000-0000-0000-0000-000000000001') then
  raise exception 'Cross-account deletion was allowed';
 end if;
end;
$$;
select set_config('request.jwt.claim.sub', '91000000-0000-0000-0000-000000000001', true);
do $$
declare sources jsonb;
begin
 sources := public.fortress_get_household_sources();
 if jsonb_array_length(sources) <> 1 or sources->0->>'display_name' <> 'Isolation test' then
  raise exception 'Owner source was altered or hidden';
 end if;
 perform public.fortress_save_household_source(
  '92000000-0000-0000-0000-000000000002', 'custom_epg', 'Own parent', true, 1,
  '{}'::jsonb, '92000000-0000-0000-0000-000000000001');
 if not public.fortress_delete_household_source('92000000-0000-0000-0000-000000000001') then
  raise exception 'Owner deletion failed';
 end if;
 if public.fortress_get_household_sources() <> '[]'::jsonb then
  raise exception 'Owner cascade failed';
 end if;
end;
$$;
rollback;
