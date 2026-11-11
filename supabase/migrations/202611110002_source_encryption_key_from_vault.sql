begin;

create or replace function public.fortress_get_source_master_key()
returns text
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
    encryption_key text := btrim(current_setting('app.settings.source_encryption_key', true));
begin
    if coalesce(length(encryption_key), 0) < 32
        and to_regclass('vault.decrypted_secrets') is not null then
        select btrim(secret.decrypted_secret)
        into encryption_key
        from vault.decrypted_secrets as secret
        where secret.name = 'fortresstv_source_encryption_key';
    end if;

    if coalesce(length(encryption_key), 0) < 32 then
        raise exception 'Server source encryption key is not configured or insufficient (minimum 32 characters required)';
    end if;

    return encryption_key;
end;
$$;

revoke all on function public.fortress_get_source_master_key() from public, anon, authenticated;

commit;
