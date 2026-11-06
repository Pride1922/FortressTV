# Account stage: configuration and testing

The Android app now has Settings → Account (Definições → Conta): registration,
email confirmation, password login, local logout and password recovery. Accounts
are optional. This stage does not create/select household profiles or sync the
existing device library. No playlists or Xtream credentials are uploaded.

## Supabase dashboard setup

Email signup and email confirmation are enabled on the supplied development
project (verified through the public Auth settings endpoint on 6 November 2026).
The app uses email codes for confirmation and recovery, which also work on TV.
It does not accept login tokens through deep links. This replaces the earlier
plan to use browser redirects/PKCE; no owned web domain is needed for this stage.

In Authentication → Email Templates, replace the **Confirm sign up** body with:

```html
<h2>Confirm your FortressTV account / Confirme a sua conta FortressTV</h2>
<p>Enter this code in FortressTV to confirm your email address.</p>
<p>Introduza este código no FortressTV para confirmar o seu endereço de e-mail.</p>
<p><strong>{{ .Token }}</strong></p>
<p>If you did not request this, ignore this email.</p>
<p>Se não fez este pedido, ignore este e-mail.</p>
```

Suggested subject: `FortressTV — Confirm account / Confirmar conta`.

Replace the **Reset password** body with:

```html
<h2>Reset your FortressTV password / Redefina a sua palavra-passe FortressTV</h2>
<p>Enter this code in FortressTV to choose a new password. Never share this code.</p>
<p>Introduza este código no FortressTV para escolher uma nova palavra-passe. Nunca partilhe este código.</p>
<p><strong>{{ .Token }}</strong></p>
<p>If you did not request this, ignore this email.</p>
<p>Se não fez este pedido, ignore este e-mail.</p>
```

Suggested subject: `FortressTV — Password recovery / Recuperação de palavra-passe`.

Use the codes in the app; do not add the default confirmation links to these
templates. The new-password screen requires a recovery code verified by Supabase.
Recovery sessions stay in memory only and are revoked after the password change;
the user then signs in with the new password. Errors and password-reset requests
do not disclose whether the email exists. Registration may intentionally present
the same confirmation step for an already registered address.

The default Supabase mail service restricts delivery to authorised addresses and
has tight rate limits. For testing with your own address, check the project's
email delivery configuration. Configure custom SMTP before inviting other users;
do not disable email confirmation to work around delivery errors.

## Local and GitHub Actions configuration

The supplied project URL and publishable key are saved in the ignored root file
`supabase.properties`. Its format is:

```properties
projectUrl=https://YOUR_PROJECT_REF.supabase.co
publishableKey=YOUR_PUBLISHABLE_KEY
```

Use the API project URL, not its dashboard URL. Never put a service-role or secret
key here. Only `sb_publishable_` keys are accepted by this client configuration.
Session tokens are encrypted with a separate Android KeyStore AES-GCM key, excluded
from Android backup and from FortressTV's existing backup format. Passwords and
email codes are never persisted. Source credential encryption is unchanged.

For GitHub Actions, set these **repository variables** under Settings → Secrets
and variables → Actions → Variables:

| Variable | Value |
| --- | --- |
| `SUPABASE_URL` | Your API project URL |
| `SUPABASE_PUBLISHABLE_KEY` | Your publishable key |

Both existing Android workflows pass these public client values to Gradle.
Environment variables override the local file. Builds without configuration still
work as local players and display that account access is unavailable. Keys are
embedded in configured APKs by design; security depends on RLS and the user JWT.
No workflow has been dispatched and no release/version metadata has changed.

## Manual device checks before shipping

Automated verification on 6 November 2026 passed: `compileDebugKotlin`,
`testDebugUnitTest` (186 tests, including six account tests), `lintDebug` (no errors;
115 warnings), and `assembleDebug`. These ran in an isolated Ubuntu container
with JDK 21 and Android SDK 36.1. The development project's public Auth settings
responded successfully and anonymous access to all four household tables was
denied. Real-email and physical-device checks below remain outstanding.

1. In both English and Portuguese (Portugal), create an account, enter the emailed
   code and sign in. Check duplicate signup, wrong/expired code and resend limits.
2. Restart the app and reopen Account: the encrypted session should restore.
   Check expired refresh tokens and offline restoration. No catalogue fetch is
   added to startup; account storage/network work starts when Account is opened.
3. Request password recovery, enter the code, choose matching passwords of at
   least eight characters, then sign in with the new password. Check incorrect
   codes, resend, cancellation and server password policy errors.
4. Start playback, open Account and sign out. Playback must close, sources and
   local favourites/progress must remain, and Account must show the login form.
   Offline logout clears this device's tokens and reports that remote session
   revocation could not be confirmed. It does not sign out other devices.
5. Verify portrait/landscape, keyboard visibility, D-pad navigation and narrow
   layouts. Profiles and account-specific library isolation remain a later stage;
   the screen explicitly identifies the library as local device data.

References: [email template tokens](https://supabase.com/docs/guides/auth/auth-email-templates),
[Auth REST contract](https://github.com/supabase/auth/blob/master/openapi.yaml),
[SMTP](https://supabase.com/docs/guides/auth/auth-smtp).
