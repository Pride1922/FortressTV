# Build FortressTV with GitHub Actions

GitHub Actions can build and sign the APK. Nextcloud remains the app's update host: the app checks the existing shared `version.json`, compares `versionCode`, downloads the linked APK, verifies its SHA-256, and checks its package name and signing certificate before opening Android's installer.

The original Windows release process is documented in `AGENTS.md` and implemented in `scripts/New-FortressTvRelease.ps1`. It copies files to `C:\Users\fabio\Nextcloud\FortressTV`, waits until the public APK download has the new checksum, and only then replaces `version.json`. That sync folder connects the release process to the Nextcloud server. Server deployment, backups, and networking are not documented in this repository.

## Workflows

- **Android checks** runs on pushes to `main`, pull requests, and manual dispatch. It compiles Kotlin, runs unit tests and lint, and saves a debug APK as a downloadable workflow artifact. A debug APK is for development and cannot update existing release-signed installations.
- **Build signed release** runs manually on `main`. It runs checks, loads the original signing key from GitHub secrets, builds and verifies a signed release APK, and saves a Nextcloud release bundle. It does not upload to Nextcloud, publish a GitHub Release, edit the live update feed, bump versions, or install to devices.

Workflow artifacts are downloaded from the completed run's Actions page by users with repository access. They are not the app's public update endpoint. See [GitHub's artifact documentation](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts).

## One-time signing setup from the Windows PC

Use the same keystore, alias, and passwords referenced by the working Windows `keystore.properties`. Do not generate a replacement key for existing installations.

In this repository's **Settings → Secrets and variables → Actions**, create these repository secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded original release keystore |
| `ANDROID_KEYSTORE_PASSWORD` | Existing keystore password |
| `ANDROID_KEY_ALIAS` | Existing key alias |
| `ANDROID_KEY_PASSWORD` | Existing key password |

To put the encoded keystore on the Windows clipboard without printing it, substitute the real local keystore path:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path 'C:\path\to\release.jks').Path)) | Set-Clipboard
```

Paste the clipboard directly into the `ANDROID_KEYSTORE_BASE64` secret. Keep keystores, encoded keystores, and passwords out of chat, commits, workflow logs, and artifacts.

Under **Variables**, create `FORTRESSTV_APK_DOWNLOAD_URL` using the existing stable HTTPS APK share URL from the Windows release script's `DownloadUrl`. This must serve `fortresstv.apk`; it is not the URL of `version.json`. No Nextcloud username, password, or app password is needed because the workflow only prepares downloadable files.

## Generate a new version

1. Make and test the intended app changes.
2. Increase `versionCode` in `app/build.gradle.kts` above every previously published APK, and set the intended `versionName`. Keep the version change in a separate commit as required by `AGENTS.md`.
3. Push the reviewed changes and version commit to `main` when authorized.
4. Open **Actions → Build signed release → Run workflow**, choose `main`, and enter release notes.
5. After success, download the `FortressTV-release-…` artifact and extract it.

The bundle contains:

```text
fortresstv.apk
version.json
source/
  ffmpeg-6.0-source.tar.gz
  COPYING.LGPLv2.1
  build-config.txt
  README.md
  THIRD_PARTY_NOTICES.md
```

The JSON records the committed app version, version code, release notes, stable APK URL, and SHA-256 of the bundled APK. Every workflow run builds the version already committed in the repository; a run number is not an Android version code. Rebuilding the current published version does not create an installable update.

## Publish through the existing Nextcloud shares

1. Upload the bundled `fortresstv.apk`, overwriting the existing file in the established shared location. Preserve its filename and share link.
2. Upload the bundled `source/` files alongside it.
3. Download the APK through the existing public share URL and calculate its SHA-256. It must exactly match the bundle's `version.json`. Wait for synchronization to finish before continuing.
4. Only after that match, upload `version.json`, overwriting its existing shared file and preserving its share link.
5. Test update detection and installation on an explicitly selected Android device when authorized.

Do not place both files into an automatically synced folder at once: the smaller JSON can become public before the APK finishes syncing. A future WebDAV publishing job can automate this order, but it needs separate Nextcloud access configuration and authorization to publish.
