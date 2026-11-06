# FortressTV Development Guide

This file contains the permanent working rules for contributors and coding agents in this repository.

## Product

FortressTV is a private, content-neutral Android IPTV and media player. It does not ship channels, movies, credentials, playlists, EPG data, or links to copyrighted content. Users provide sources they are authorized to access.

The primary target is an Android tablet, with Android TV and D-pad support considered in future UI decisions. The application currently supports M3U/M3U8 playlists, XMLTV EPG data, AndroidX Media3 playback, an FFmpeg audio-decoder extension, fullscreen playback, and fullscreen-only Picture-in-Picture.

## Repository and release paths

- Repository: `C:\Users\fabio\AndroidStudioProjects\FortressTV`
- Git remote: `https://github.com/Pride1922/FortressTV.git`
- Main branch: `main`
- Local release directory: `C:\Users\fabio\Nextcloud\FortressTV`
- Stable public APK filename: `fortresstv.apk`
- OTA metadata filename: `version.json`
- Release command: `scripts\New-FortressTvRelease.ps1`
- GitHub Actions build alternative: see `GITHUB_ACTIONS.md`. The manual signed-build workflow stages a release bundle; it does not publish to Nextcloud or change the live update feed.

Never rename the published APK. Nextcloud must overwrite `fortresstv.apk` so the established share URL remains valid.

## Working rules

- Preserve user changes and do not overwrite unrelated work.
- Use `rg` for repository searches and `apply_patch` for manual file edits.
- Infer routine implementation details from the repository and continue through verification. Ask a focused question only when the answer would materially change the product behavior or expand the authorized scope.
- Keep responses concise and lead with the result. Use lists or tables only when they make sequential or comparative information clearer.
- Calibrate verification to the change. Run the required checks once after the implementation is stable; repeat or broaden them only after further changes, failures, or unresolved risk.
- Delegate independent work to subagents only when the user requests delegation or the active agent instructions explicitly allow it.
- Do not commit, push, install to a device, publish an APK, or modify OTA metadata unless the user explicitly requests that action.
- Never put playlist URLs, credentials, tokens, signing passwords, private EPG links, or provider data in Git, logs, tests, screenshots, or documentation.
- Keep `keystore.properties` and signing material private and untracked.
- Do not silently change the product into a content-provider client. New protocols must be implemented for legitimate user-provided services and remain content-neutral.
- Prefer small, reviewable commits. Keep feature work separate from version-bump commits.

## Language and UI

- Every user-visible feature must include English and Portuguese (Portugal), `pt-PT`, text in the same change.
- Do not use Brazilian Portuguese terminology when a Portuguese (Portugal) form differs.
- Keep tablet layouts touch-friendly and responsive in portrait and landscape.
- Keep future D-pad focus and Android TV navigation possible; avoid UI structures that inherently require touch.
- Preserve the original order of channels and categories within a source unless the user explicitly customizes it.
- Picture-in-Picture is fullscreen-only. Pressing Home from inline playback must pause playback instead of opening PiP.
- Closing PiP must stop playback promptly. Restoring the PiP window must continue playback.

## Architecture and data

- Treat provider/source data as replaceable cache data.
- Store user data separately from source data so refreshes cannot erase favorites, watch progress, manual EPG mappings, hidden items, or custom organization.
- Every library item must retain a stable source identifier and source-local identifier. Never use only a display name as identity.
- Multiple enabled sources must coexist in one library; do not return to a single globally active source model.
- Refresh, cache, and report errors independently per source. One failing source must not remove or block other sources.
- Keep credentials encrypted with the existing Android KeyStore-backed storage approach.
- Database and preference migrations must preserve existing installations and saved playlists.

## Performance requirements

Performance is a product requirement, not an optional cleanup step. See `PERFORMANCE.md` for existing measurements and benchmark procedures.

- A large VOD catalogue must not slow the Live TV channel list or EPG.
- Parse, refresh, cache, and index sources off the main thread.
- Do not parse every source or load every episode at application startup.
- Use lazy lists/grids, stable keys, visible-item image loading, pagination or incremental publication, and bounded in-memory state.
- Cache each source separately and avoid replacing an already usable catalogue while another source refreshes.
- Keep XMLTV handling limited to the application window currently defined by the EPG implementation, even when the upstream grabber supplies more days.
- Add or update macrobenchmarks for changes that affect startup, channel scrolling, EPG opening/scrolling, or large catalogue browsing.
- Document meaningful before/after performance measurements in `PERFORMANCE.md`.

## Playback requirements

- Use AndroidX Media3 as the primary playback engine.
- Preserve the FFmpeg extension fallback used by streams with otherwise unsupported MPEG audio.
- Do not allow invisible background playback. Playback may continue outside the app only through visible Android Picture-in-Picture behavior.
- Release or pause player resources promptly when playback is closed, the app leaves the foreground without PiP, or PiP is dismissed.
- New player options must be tested against existing live playback, channel switching, fullscreen controls, brightness/volume gestures, and PiP.

## Verification

Choose verification proportional to the change. Before committing normal application changes, run at minimum:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug
```

For UI or packaging changes, also run:

```powershell
.\gradlew.bat :app:assembleDebug
```

For performance-sensitive changes, run the relevant tests and macrobenchmarks on the explicitly selected physical device. Multiple Android devices may be connected; never rely on an implicit ADB target.

## Release procedure

Only release when explicitly requested.

1. Confirm the worktree is clean except for the intended release change.
2. Increment `versionCode` for every published APK.
3. Use semantic pre-1.0 versions: `0.MINOR.PATCH`. Do not jump major versions for ordinary tests.
4. Set `versionName` and commit the version bump separately, for example `Prepare FortressTV 0.1.5`.
5. Push the intended commits to `origin/main`.
6. Run `scripts\New-FortressTvRelease.ps1` with accurate release notes.
7. The script must build the signed release, verify its signature, overwrite `fortresstv.apk`, wait for the public Nextcloud APK checksum to match, and only then replace `version.json`.
8. Confirm the local APK SHA-256 exactly matches `version.json`.
9. Report the version, version code, commit, APK path, size, and SHA-256.

Do not install or push a release to a tablet unless the user separately requests it.
