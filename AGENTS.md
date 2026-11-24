# FortressTV Development Guide

This file contains the permanent working rules for contributors and coding agents in this repository.

## Product

FortressTV is a private, content-neutral Android IPTV and media player. It does not ship channels, movies, credentials, playlists, EPG data, or links to copyrighted content. Users provide sources they are authorized to access.

The primary target is an Android tablet. Phones and Android TV share the same application and APK; TV launcher support, focus helpers, remote playback keys, and local source transfer are implemented. Complete D-pad workflow and physical-device validation remain ongoing. Preserve both touch and remote navigation when changing the UI.

The application supports multiple M3U/M3U8 and Xtream sources, XMLTV with automatic/manual channel mapping, live and VOD playback through AndroidX Media3 with the FFmpeg audio-decoder extension, fullscreen-only Picture-in-Picture, source-supported catch-up, sleep timer, and next-episode autoplay. Home, favourites, watchlists, followed series, watched state, and resume progress are implemented, as are optional household accounts/profiles, parental controls, cloud sync, metadata integrations, Trakt, and OpenSubtitles.

Use `ROADMAP.md` for implementation status, `DESIGN_ROADMAP.md` for UX proposals, and `ANDROID_TV_PLAN.md` for remaining TV/Connect work. Existing code is the implementation reference; a planning document is not evidence that a feature is missing or validated. Update affected documentation alongside meaningful feature changes, distinguishing implemented behavior, device verification, and future proposals.

## Repository and release paths

- Repository: `C:\Users\fabio\AndroidStudioProjects\FortressTV`
- Git remote: `https://github.com/Pride1922/FortressTV.git`
- Main branch: `main`
- Release channel: GitHub Releases (`https://github.com/Pride1922/FortressTV/releases`)
- Stable public APK filename: `fortresstv.apk`
- OTA metadata filename: `version.json`
- Release command: `scripts\New-FortressTvRelease.ps1`
- GitHub Actions build: see `GITHUB_ACTIONS.md`. The manual signed-build workflow builds, signs, and publishes the release to GitHub Releases.

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
- Preserve D-pad focus and Android TV navigation; primary actions must not inherently require touch. Keep focus visible and restore focus and scroll position when returning from details or playback.
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
- External metadata, subtitle, sports, and Trakt services must remain optional. Service outages or missing keys must not block source browsing or playback. Do not embed personal service keys in the application.
- Local TV source transfer is distinct from the planned Connect playback handoff and remote-control protocol. Preserve session expiry, encrypted source payloads, attempt limits, receiving-device confirmation, and secret redaction when changing transfer code.

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

For TV UI changes, verify affected workflows with D-pad-only input, including visible focus, Back behavior, and focus restoration. Do not describe a TV milestone as verified based only on compilation or the presence of focus helpers.

For documentation-only changes, check the diff, relative links, and implementation claims against the relevant code; Android builds and device tests are not required. Do not mark proposed features as agreed priorities without a product decision.

## Release procedure

Only release when explicitly requested.

1. Confirm the worktree is clean except for the intended release change.
2. Increment `versionCode` for every published APK.
3. Set `versionName` and commit the version bump separately, for example `Prepare FortressTV 1.0.0-RC10`.
4. Push the intended commits to `origin/main`.
5. Run `scripts\New-FortressTvRelease.ps1` with accurate release notes.
6. The script builds the signed release, verifies its signature with apksigner, writes `version.json`, and publishes the release directly to GitHub Releases via `gh release create`.
7. Confirm the local APK SHA-256 exactly matches `version.json` and the GitHub Release asset.
8. Report the version, version code, commit, download URL, size, and SHA-256.

Do not install or push a release to a tablet unless the user separately requests it.
