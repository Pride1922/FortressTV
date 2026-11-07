# FortressTV accounts: schema and implementation plan

Status: the database foundation and cloud sync extensions have been applied to the
project. Migration `202611060002_profiles_and_parental_pin.sql` introduced
parental controls and profile RPCs. Corrective migration
`202611060003_harden_profile_authorization.sql` hardened authorization: it eliminated
PIN rollback on failed protected RPCs, serialized attempts with row-level locking
(`SELECT ... FOR UPDATE`), required valid adult unlock tokens for management RPCs,
enforced scoped profile sessions on library and watch progress tables via RLS,
and guaranteed atomic invalidation on PIN changes. Migration
`202611060004_bind_sessions_and_child_invalidation.sql` bound unlocks and active profile
sessions to the device's authenticated session (`auth_session_id`), server-invalidated
adult unlocks and active adult profile sessions upon entering child mode for that device,
and exposed `fortress_leave_adult_mode()`. Migration
`202611060005_revoke_target_auth_session.sql` added targeted revocation (`p_target_auth_session_id`)
to `fortress_leave_adult_mode()` and `fortress_revoke_adult_session()`. Migration
`202611060006_reset_parental_pin_after_recovery.sql` handled PIN resets following recovery.
Migration `202611060007_household_sources.sql` added AES-256 encrypted source backup and restore.
Migration `202611060008_client_sync_versioning.sql` and `202611060009_clock_skew_and_versioning.sql`
added server-managed sequence numbers and monotonic clock resolution for durable synchronization.
Migration `202611060010_categories_preferences_history.sql` added categories customization and history sync.
Migration `202611060011_shared_sources.sql` added cross-household source sharing.
Migration `202611060012_opensubtitles_and_extended_preferences.sql` added encrypted OpenSubtitles credential storage and subtitle preferences.
Migration `202611060013_epg_mappings_and_correction.sql` added custom EPG mappings synchronization.
Migration `202611060014_opensubtitles_auth_fix.sql` eliminated PIN barriers on profile session acquisition and OpenSubtitles sync so auto-resumed profiles sync favorites, watchlist, and subtitle accounts seamlessly. Alternatively, `CONSOLIDATED_SETUP.sql` contains the complete cumulative schema for fresh deployments.

## Findings from the Android project

| Existing component | Current behaviour | Integration requirement |
| --- | --- | --- |
| `SourceSettingsStore` | AES-GCM with Android KeyStore; encrypted playlists and Xtream credentials; global language and parental settings | Keep sources local; scope language and restrictions to household profile |
| `UserLibraryStore` | Separate preferences for favourites, watchlists, watched items, recent channels and source category customisation | Add household/profile namespace without overwriting legacy preferences |
| `VodProgressStore` | Separate JSON store; source/item identities; at most 100 resume entries; removes entries below 10 seconds or at 95% completion | Preserve resume rules, retain explicit completion and removal events for sync |
| `PlaylistProfile` | A configured content source, not a person | Keep terminology and identifiers separate from `HouseholdProfile` |
| `BackupManager` | Exports/restores sources, parental settings and user state together | Version backup format and restore into an explicitly chosen profile; never send backups to Supabase |
| `AppStrings` / `MainActivity` | English/Portuguese strings and UI state in Compose | Add both languages together; reload state atomically on profile switch |

Live IDs already include source and source-local identity. M3U fallback IDs hash
the stream identity; Xtream items use provider-local IDs. Never identify an item
by display name alone. Source refreshes and caches remain independent of user state.

## Database contract and access boundary

`households.id` equals the owning `auth.users.id`: one account, one household.
The authenticated client creates its household idempotently after login; signup
does not need an Auth trigger. `household_profiles` holds adult/child settings
and `en` / `pt-PT` language. Children require adult-content filtering, unrated
blocking and an age ceiling. Account deletion cascades through all household rows.

`profile_library` holds favourites, watchlists/followed series and watched flags.
`profile_watch_progress` holds resume positions and completion. Both require a
composite household/profile foreign key, so an owner cannot attach their state to
another household's profile. RLS compares household ownership with `auth.uid()`
on reads and writes; anonymous access is revoked. Identity columns cannot be
updated. Deletions of library/progress state use `deleted_at` tombstones.
With these column grants, use insert-with-ignore-duplicates followed by a PATCH
of mutable fields, retried from the durable outbox. A full-row PostgREST upsert
would attempt to update protected identity columns and is intentionally denied.

Cloud `source_id` is an opaque UUID persisted in a local source mapping, shared
only when a user explicitly links the same source on another device. Do not
match sources by name or upload endpoint fingerprints. `item_id` is lowercase
hex SHA-256 of the exact source-local identifier (UTF-8), scoped by item kind and
source UUID. Persist the mapping; never regenerate it on refresh. Keep existing
local library keys. No source URLs, usernames, passwords, EPG, media catalogue,
titles or artwork are included in the cloud payload. Use explicit DTO allowlists.

Timestamps are server-stamped. Sync will use bounded per-profile pages with a
timestamp plus primary-key tie breaker, overlapping pulls, idempotent merges and
a durable local outbox. The initial contract is last server-accepted write wins,
including progress rewinds; it is not maximum-position-wins. Do not purge
tombstones until a separate retention/resynchronisation protocol exists. Add
optimistic concurrency/version handling before supporting concurrent offline
edits that must reject stale writes. Realtime is not required initially.

**Parental security boundary:** all profiles share the owner's Auth session.
These RLS policies isolate accounts, not siblings or adults from children. An
owner JWT can edit restrictions directly. Do not claim server-enforced parental
security with this schema. Before stage 3 ships, add a private PIN-verifier store
(never readable by clients), server-side salted hashing, persisted attempt limits,
and short-lived adult unlocks. Route profile management/restriction writes through
validated RPCs and revoke their direct write grants. A server-enforced child
boundary also requires scoped profile sessions with every policy/RPC validating
the session; a client-supplied profile ID or editable user metadata is insufficient.
Keep PINs out of JWTs and logs. PIN reset requires account reauthentication.
Content ratings are provider metadata, so unknown ratings must fail closed for
children; preserve the existing local filter until the new gate is complete.

## Staged delivery

1. **Development Supabase project and database.** Create a Free project in the
   intended region in the Supabase dashboard. Save project URL and publishable
   key in local, untracked configuration. Android will use these public values
   plus the signed-in user's JWT. Never embed service-role/secret keys in Android,
   GitHub artifacts or source control. Enable email confirmation and configure
   recovery redirect allowlists. Apply the migration and run the SQL tests in a
   disposable development database. No production data migration at this stage.
   Configure SMTP before real-user rollout; exercise signup and recovery delivery
   locally first and translate the email templates into English and pt-PT.
2. **Authentication.** Add a repository interface and Supabase client behind an
   optional account feature. Registration, login, logout and recovery need loading,
   offline, expired-session and validation states. Use PKCE and validated Android
   App Links for recovery; secure token storage with KeyStore. Reauthentication
   for destructive operations. Logout stops playback, clears in-memory account
   state and tokens, and retains isolated local data and encrypted sources.
   Account switching must never expose the previous household's cached state.
3. **Profiles and parental control.** Implement the server PIN/session boundary
   above first, then touch-friendly, focusable profile selection and management.
   Require adult unlock to enter adult profiles, change child restrictions,
   manage profiles, restore backups or leave child mode. Persist an active profile
   per account/device; language switches with it. Stop playback before switching.
   A child must not bypass the gate by restarting, offline mode or logout.
4. **Local migration and sync.** Add a transactional local database for profile
   state and outbox. On upgrade copy legacy data into a local default adult profile
   once, retain original preferences, and checkpoint migration only after durable
   success. On first login explicitly choose the adult profile receiving the
   legacy state; merge once with an account/profile migration marker. Never copy
   legacy history to every child or automatically upload it to a second account.
   Preserve hidden categories, category order, recent channels and manual EPG
   mappings locally even where not yet synced. Map legacy `pt` to `pt-PT` at the
   boundary. Source caches and credentials stay in their existing stores. Fetch
   only the active profile, sync off the main thread, debounce progress, and keep
   offline playback independent of network success. Derive metadata from local
   catalogues; unavailable sources leave saved state intact.
5. **Verification and build.** Test two accounts and multiple profiles, offline
   edits, logout/relogin, recovery, account deletion, crash/retry migration,
   tombstones, profile switching during playback, backups, missing sources, and
   PIN rate limits/bypass attempts. Run compile, unit tests, lint and assemble.
   Add relevant startup/catalogue macrobenchmarks on an explicitly selected
   physical device and record results in `PERFORMANCE.md`. Then use the existing
   GitHub Actions checks and signed-build workflow when requested. A signed
   artifact does not publish Nextcloud or change the live OTA feed.

Every UI stage includes English and Portuguese (Portugal), including errors and
recovery emails. Initial vocabulary: Account / Conta; Household / Agregado
familiar; Profile / Perfil; Child profile / Perfil de criança; Favourites /
Favoritos; Watch progress / Progresso de visualização; Parental controls /
Controlo parental; Forgot password? / Esqueceu-se da palavra-passe?; Sign out /
Terminar sessão. Keep existing translation conventions in `AppStrings`.

## Apply and verify

Use the SQL editor on a new development project to apply
`migrations/202611060001_households.sql`,
`migrations/202611060002_profiles_and_parental_pin.sql`,
`migrations/202611060003_harden_profile_authorization.sql`, and
`migrations/202611060004_bind_sessions_and_child_invalidation.sql` in sequence.
Alternatively use the Supabase CLI migration workflow after linking a
development project.

Run `tests/household_access.sql`, `tests/profiles_and_parental_pin.sql`,
`tests/profile_authorization_hardening.sql`, and
`tests/session_binding_and_child_invalidation.sql` as
postgres on a disposable Supabase database (SQL editor or
`psql -v ON_ERROR_STOP=1 -f ...`). They insert synthetic Auth users inside a
transaction, test table grants, RLS, parental controls, PIN verification,
attempt limits, adult unlocks, scoped profile sessions, and session binding with
child mode adult access revocation, and roll everything back.
The runner must stop on the first error. Never run fixtures against production.

Verified on 6 November 2026: all four migrations and access test scripts passed on a
disposable PostgreSQL 17 container with minimal `auth.users`, `auth.uid()`, `anon`
and `authenticated` stubs. This validates SQL constraints, grants, RLS, and RPCs;
it does not validate hosted Supabase Auth, PostgREST, email delivery or Android integration.
Repeat against the development Supabase project before connecting the app.

References: [Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security),
[API keys](https://supabase.com/docs/guides/getting-started/api-keys),
[password auth](https://supabase.com/docs/guides/auth/passwords).
