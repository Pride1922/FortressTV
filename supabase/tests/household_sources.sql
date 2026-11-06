-- Test household sources authorization, encryption, and adult boundaries
begin;

insert into auth.users (id) values
    ('50000000-0000-0000-0000-000000000001'),
    ('60000000-0000-0000-0000-000000000002')
on conflict do nothing;

insert into public.households (id) values ('50000000-0000-0000-0000-000000000001')
on conflict do nothing;

-- Set test encryption master key (at least 32 chars)
select set_config('app.settings.source_encryption_key', 'test-master-key-32-chars-long-encryption!!', true);

-- 1. Configure parental PIN for household 1
select set_config('request.jwt.claim.sub', '50000000-0000-0000-0000-000000000001', true);
select set_config('role', 'authenticated', true);

select public.fortress_set_parental_pin('1234');

-- 2. Attempt to save source without adult authorization -> must FAIL
do $$
begin
    perform public.fortress_save_household_source(
        gen_random_uuid(),
        'xtream',
        'Unauthorized Xtream',
        true,
        0,
        '{"server_url":"http://example.com","username":"user","password":"pwd"}'::jsonb,
        null,
        null
    );
    raise exception 'Expected authorization failure';
exception when others then
    if sqlerrm not like '%Adult authorization required%' then
        raise;
    end if;
end;
$$;

-- 3. Verify PIN and obtain unlock token, then save source
do $$
declare
    v_verify jsonb;
    v_token uuid;
    v_src_id uuid;
    v_epg_id uuid;
    v_sources jsonb;
begin
    v_verify := public.fortress_verify_parental_pin('1234');
    v_token := (v_verify->>'unlock_token')::uuid;

    v_src_id := public.fortress_save_household_source(
        'aaaaaaaa-0000-0000-0000-000000000001'::uuid,
        'xtream',
        'Authorized Xtream',
        true,
        0,
        '{"server_url":"http://example.com","username":"secret_user","password":"secret_password"}'::jsonb,
        null,
        v_token
    );

    -- Add custom EPG linked to the parent source
    v_epg_id := public.fortress_save_household_source(
        'bbbbbbbb-0000-0000-0000-000000000002'::uuid,
        'custom_epg',
        'Custom EPG Source',
        true,
        1,
        '{"url":"http://epg.example.com/epg.xml.gz"}'::jsonb,
        v_src_id,
        v_token
    );

    v_sources := public.fortress_get_household_sources(v_token);
    if jsonb_array_length(v_sources) <> 2 then
        raise exception 'Expected 2 sources, found %', jsonb_array_length(v_sources);
    end if;

    if (v_sources->0->'config'->>'username') <> 'secret_user' then
        raise exception 'Decrypted username mismatch';
    end if;

    -- 4. Delete parent source -> child EPG must be tombstoned too
    perform public.fortress_delete_household_source(v_src_id, v_token);

    v_sources := public.fortress_get_household_sources(v_token);
    if jsonb_array_length(v_sources) <> 0 then
        raise exception 'Expected 0 active sources after cascading tombstone, found %', jsonb_array_length(v_sources);
    end if;

    -- 5. Stale device attempt to update deleted source -> must FAIL
    begin
        perform public.fortress_save_household_source(
            v_src_id,
            'xtream',
            'Resurrect Attempt',
            true,
            0,
            '{"server_url":"http://example.com","username":"secret_user","password":"secret_password"}'::jsonb,
            null,
            v_token
        );
        raise exception 'Expected resurrection failure';
    exception when others then
        if sqlerrm not like '%Cannot update deleted source%' then
            raise;
        end if;
    end;
end;
$$;

-- 6. Test missing encryption key -> must fail safely
select set_config('app.settings.source_encryption_key', '', true);

do $$
declare
    v_verify jsonb;
    v_token uuid;
begin
    v_verify := public.fortress_verify_parental_pin('1234');
    v_token := (v_verify->>'unlock_token')::uuid;

    begin
        perform public.fortress_get_household_sources(v_token);
        raise exception 'Expected failure on missing encryption key';
    exception when others then
        if sqlerrm not like '%Server source encryption key is not configured%' then
            raise;
        end if;
    end;
end;
$$;

rollback;
