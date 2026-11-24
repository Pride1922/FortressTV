# FortressTV Roadmap

Reviewed against the repository on 24 November 2026. Implemented means a code path exists; it does not certify every device, provider, or service combination. This document separates current capabilities from remaining work and unapproved ideas.

## Product direction

FortressTV is a private, content-neutral player for user-provided, authorised sources. Multiple enabled sources contribute to one library without replacing each other. The primary navigation is:

```text
Home | Live TV | Movies | Series | Favorites
```

Phones, tablets, and Android TV share one application and APK. Tablet usability, source independence, saved user data, and Live TV/EPG performance remain requirements for every change. English and Portuguese (Portugal) ship together.

## Implemented capabilities

| Area | Current capabilities | Main implementation references |
|---|---|---|
| Sources | Multiple M3U/M3U8 and Xtream sources; roles, enable/disable, ordering, independent caches/refreshes, encrypted credentials | `source/`, `playlist/`, `xtream/`, `SourceSyncWorkflow.kt` |
| Live TV and EPG | Channel browsing, XMLTV, automatic matching, manual channel associations, schedule correction, configurable guide window, unmatched-channel tools | `ChannelBrowser.kt`, `ProgrammeGuideScreen.kt`, `EpgDialogs.kt`, `EpgTab.kt`, `epg/` |
| User library | Favourite channels, Movie Watchlist, followed series, watched state, resume progress, recent channels, per-source category hiding and ordering with cloud restoration | `library/`, `FavoriteLibraryScreen.kt`, `ManageCategoriesDialog.kt` |
| Home | Continue Watching, next episodes, favourite/recent channels, unified search, configurable shelves and startup destination | `HomeScreen.kt`, `HomeShelvesStateHolder.kt`, `ui/CustomizeHomeDialog.kt` |
| Movies and series | Cached catalogues, poster grids, search/sort, details, lazy episode loading, resume/restart, next episode and autoplay | `XtreamMoviesScreen.kt`, `XtreamSeriesScreen.kt`, `SeriesEpisodesDialog.kt`, `VodPlayerDialog.kt` |
| Playback | Media3 with FFmpeg audio fallback, fullscreen controls, fullscreen-only PiP, audio/subtitle selection and language defaults, quality selection, Go Live, resilience controls, sleep timer | `FullscreenPlayer.kt`, `VodPlayerDialog.kt`, `PlaybackSleepTimer.kt`, `player/` |
| Catch-up | Archive playback when supported by the configured source; availability and retention are source-dependent | `FortressLibrary.kt`, `xtream/XtreamClient.kt` |
| Accounts and profiles | Optional household account, parental controls and PINs, encrypted cloud source backup/restore, library and VOD progress sync | `account/`, `profile/`, `library/CloudLibrarySyncService.kt`, `source/CloudSourceSyncService.kt` |
| Metadata and discovery | Optional TMDB/OMDb metadata and ratings, TheIntroDB skip markers, trailers, Trakt authentication, scrobbling, watchlist/history sync, recommendations and popular titles | `tmdb/`, `omdb/`, `introdb/`, `trakt/` |
| Subtitles | Track selection and optional OpenSubtitles search/download | `subtitles/`, `VodPlayerDialog.kt` |
| Sports | Home/guide sports surfaces and event-to-channel matching; fixture availability depends on configured services and EPG data | `sports/` |
| Playback health | Connection health indicators/details, playback metrics, and live-channel failover logic | `telemetry/`, `player/ChannelFailoverResolver.kt` |
| Android TV foundation | TV launcher/banner, form-factor detection, shared focus helpers and remote player key handling | `AndroidManifest.xml`, `ui/AppFormFactor.kt`, `ui/TvFocus.kt`, `ui/TvPlayerKeyDispatcher.kt` |
| TV source setup | Local source transfer with QR/PIN setup, discovery/native send, session authentication, encrypted source payloads and receiving-device confirmation | `TvReceiveSourceDialog.kt`, `transfer/` |

Implementation references are under `app/src/main/java/com/fortresstv/player/`, except the manifest under `app/src/main/`.

Category visibility is not arbitrary per-item hiding. Playback health is not a complete source health dashboard. Catch-up is access to source archives, not local recording or a general-purpose DVR. Catalogue display deduplication and live failover do not imply general identity merging across sources.

TV source transfer is a setup/import feature. Persistent Connect pairing, Play on TV, resume-position handoff, and phone-based remote playback controls remain planned. Complete D-pad workflow and device validation also remain ongoing; see [the Android TV plan](ANDROID_TV_PLAN.md).

## Original delivery phases

The original phases are retained here as a compact history of delivered areas, rather than a backlog of missing features:

| Phase | Delivered scope |
|---|---|
| 0 — Baseline | Live TV, XMLTV, playback, PiP, OTA, and performance infrastructure |
| 1 — Multi-source | Enabled sources, roles/order, independent caches and source filtering |
| 2 — User library | Favourites, watchlists, followed series, watched state, and Continue Watching |
| 3 — Movies | Xtream movie catalogues, browsing, search, playback, and watched status |
| 4 — Series | Series catalogues, lazy episodes, followed state, and next unwatched episode |
| 5 — Refinement | Adult filtering, EPG matching tools, source editing, and unified favourites |
| 6 — Accounts | Household profiles, parental controls, source backup/restore, and cross-device library/progress sync |
| 7 — Enrichment | Optional metadata, skip markers, Trakt discovery and watch tracking |

The implemented table above also covers later work that was not represented accurately by the original phases.

## Remaining work

Approved sequence (24 November 2026): validate the language-selection fix, add explicit movie/live version selection with profile preferences, add saved subtitle timing, then add searchable Settings. Recording is not part of this work.

Explicit movie/live version selection is implemented; matching alternatives remain distinct source items. Movie version discovery runs on demand off the main thread, and choices are saved locally per profile. Tablet/TV interaction validation remains outstanding.

VOD subtitle timing is implemented for subtitles supported by Media3's extraction-time parsers: ±5 seconds in 100 ms steps, reset, and local persistence per source item/profile. Changes reprepare the same media at the current position to reparse buffered cues and may briefly buffer. Live captions and unsupported subtitle formats do not expose timing controls. Device playback/lifecycle validation remains outstanding.

Settings search is implemented with a local bilingual index of controls/groups, accent-insensitive matching, and navigation that scrolls to and highlights the destination. No credentials, source details, or other user-entered values are indexed. Device keyboard/D-pad validation remains outstanding.

Playback telemetry now captures the last valid decoder resolution after a frame is rendered, with an observation timestamp and an opaque identifier scoped to the source/item/stream variant. Supabase derives SD/HD/FHD/2K/4K from the dimensions. Legacy summaries remain valid, and collection uses the existing telemetry toggle and bounded upload queue. This stores observations; it does not sync preferred versions or add pre-playback quality badges.

Channel lists now show a last-played quality badge from those measurements. A bounded, household/profile-isolated cache updates during playback and survives restarts. With telemetry enabled, account sync can restore up to 500 recent measured sessions, at most once per minute, without per-row network requests. No measurement means no badge; this does not infer quality from channel names or sync preferred versions. The badge scrolling macrobenchmark is added; physical-device timing remains to be measured.

Existing directions, subject to product prioritisation:

1. Complete Android TV D-pad workflow audits, focus restoration, Back behavior, and emulator/physical-device validation against [ANDROID_TV_PLAN.md](ANDROID_TV_PLAN.md).
2. Extend local TV setup into persistent Connect pairing, playback handoff, and remote control. The current source-transfer protocol must not be treated as a completed remote-control protocol.
3. Refine individual channel/movie/series hiding and category rename/custom organisation beyond current category visibility/order controls.
4. Evaluate advanced per-source request headers, Referer, User-Agent, and timeout controls against the existing request pipeline.
5. Validate the implemented subtitle timing controls on devices and assess remaining layout/control refinements in [DESIGN_ROADMAP.md](DESIGN_ROADMAP.md).
6. Continue measuring startup, channel/EPG scrolling, and large-catalogue browsing under [PERFORMANCE.md](PERFORMANCE.md); document measured changes rather than inferring performance from implementation alone.

## Ideas awaiting a product decision

These were discussed as possibilities and are not approved implementation milestones:

- Programme reminders from the EPG.
- Personal collections per profile.
- Remembered player choices per channel or series.
- A consolidated per-source health dashboard.
- Movie discovery filtered by available viewing time.

## Deferred scope

- Multi-view playback.
- Background audio without visible PiP.
- Local or NAS recording.
- Stalker/MAC portals.
- General automatic identity merging across providers.

Reconsider these only after an explicit product decision and an assessment of playback, privacy, and performance implications.

## Architecture and delivery constraints

- Keep provider data as replaceable cache data. Store favourites, watched state, progress, category preferences, and manual EPG mappings separately so refreshes cannot erase them.
- Retain stable source and source-local identities. Display names alone are not identities.
- Preserve source order and source-local category/item order unless the user customises them. Failures and refreshes remain independent per source.
- Restore useful caches before remote refresh finishes. Parse/index off the main thread, bound memory/image work, and load VOD pages/episodes lazily. Large VOD catalogues must not slow Live TV or EPG.
- Keep credentials private using the existing encrypted storage approach, and preserve existing installations through migrations.
- External metadata, Trakt, subtitle, sports, and cloud integrations remain optional. Missing credentials or service outages must not block source browsing or playback. Discovery services do not supply playable media.
- Keep fullscreen-only PiP and prompt pause/stop when playback is closed or the app leaves the foreground without visible PiP.
- Follow [AGENTS.md](AGENTS.md) for verification and authorisation boundaries. Documentation status is not evidence of device testing or permission to release.

## Related documentation

- [README and setup](README.md)
- [Design and UX roadmap](DESIGN_ROADMAP.md)
- [Android TV and Connect plan](ANDROID_TV_PLAN.md)
- [Performance measurements](PERFORMANCE.md)
- [Account backend setup](supabase/ACCOUNT_SETUP.md)
- [Builds and releases](GITHUB_ACTIONS.md)
