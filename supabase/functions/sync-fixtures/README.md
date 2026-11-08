# Edge Function: sync-fixtures

This Supabase Edge Function automatically fetches upcoming sports fixtures across multiple sports and syncs them into `public.sports_fixtures`:
- **Football**: Primeira Liga, UEFA Champions League, Premier League, La Liga, Serie A (via TheSportsDB). Correct broadcaster rules applied (e.g. Benfica home matches on BTV, others on Sport TV or DAZN).
- **Motorsport**: Formula 1 Grand Prix calendar and sessions (via Jolpica / Ergast F1 API).
- **Basketball**: NBA games and tournament fixtures.

## Deploying to Supabase

Run using the Supabase CLI:

```bash
supabase functions deploy sync-fixtures --no-verify-jwt
```

## Running Manually

You can test the function immediately with curl or PowerShell:

```bash
curl -i --location --request POST 'https://<PROJECT_REF>.supabase.co/functions/v1/sync-fixtures' \
  --header 'Authorization: Bearer <SUPABASE_SERVICE_ROLE_KEY>'
```

## Scheduling Daily Sync via pg_cron

In your Supabase SQL Editor, run this snippet to schedule automatic sync every day at 04:00 UTC:

```sql
select cron.schedule(
  'sync-sports-fixtures-daily',
  '0 4 * * *',
  $$
  select net.http_post(
    url := 'https://<PROJECT_REF>.supabase.co/functions/v1/sync-fixtures',
    headers := jsonb_build_object(
      'Authorization', 'Bearer ' || '<SUPABASE_SERVICE_ROLE_KEY>'
    )
  );
  $$
);
```
