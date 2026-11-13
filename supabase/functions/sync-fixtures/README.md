# Edge Function: sync-fixtures

This Supabase Edge Function automatically fetches upcoming sports fixtures across multiple sports and syncs them into `public.sports_fixtures`:
- **Football**: Primeira Liga, UEFA Champions League, Premier League, La Liga, Serie A (via TheSportsDB). Correct broadcaster rules applied (e.g. Benfica home matches on BTV, others on Sport TV or DAZN).
- **Motorsport**: Formula 1 Grand Prix calendar and sessions (via Jolpica / Ergast F1 API).
- **Basketball**: NBA games and tournament fixtures.

## Access control

The function is deployed with `--no-verify-jwt`, so it checks a shared secret itself. Every call must send the header `x-sync-secret` with the value of the `SYNC_FIXTURES_SECRET` environment variable:

- wrong or missing header: `401`
- `SYNC_FIXTURES_SECRET` not set on the function: `500` (it never runs unprotected)

Create the secret in Supabase Vault (SQL Editor), so it is never written into SQL or source control:

```sql
select vault.create_secret(encode(extensions.gen_random_bytes(32), 'hex'), 'sync_fixtures_secret');
select decrypted_secret from vault.decrypted_secrets where name = 'sync_fixtures_secret';
```

Copy the value into **Edge Functions → Secrets** as `SYNC_FIXTURES_SECRET` (or `supabase secrets set SYNC_FIXTURES_SECRET=<value>`), then deploy.

## Deploying to Supabase

Run using the Supabase CLI:

```bash
supabase functions deploy sync-fixtures --no-verify-jwt
```

## Running Manually

You can test the function immediately with curl or PowerShell:

```bash
curl -i --location --request POST 'https://<PROJECT_REF>.supabase.co/functions/v1/sync-fixtures' \
  --header "x-sync-secret: $SYNC_FIXTURES_SECRET"
```

## Scheduling the Sync via pg_cron

Requires the `pg_cron` and `pg_net` extensions. In your Supabase SQL Editor, run this snippet to sync every 10 minutes. The secret is read from Vault each time the job runs; do not paste a key into the SQL:

```sql
select cron.schedule(
  'sync-sports-fixtures',
  '*/10 * * * *',
  $$
  select net.http_post(
    url := 'https://<PROJECT_REF>.supabase.co/functions/v1/sync-fixtures',
    headers := jsonb_build_object(
      'x-sync-secret',
      (select decrypted_secret from vault.decrypted_secrets where name = 'sync_fixtures_secret')
    )
  );
  $$
);
```

If an older job exists (for example `sync-sports-fixtures-daily`), list it with `select jobid, jobname, schedule from cron.job;` and remove it with `select cron.unschedule('<jobname>');`. It does not send the secret, so it fails with `401` once this version is deployed.
