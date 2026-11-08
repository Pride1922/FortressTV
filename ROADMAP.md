# FortressTV Roadmap

## Current status

- Phase 0: complete
- Phase 1: complete — multiple enabled sources, independent caches, source roles/order, deterministic Live TV merging, source filters, and Uncategorized navigation
- Phase 2: complete — primary library navigation, persistent favorite channels, Movie Watchlist, followed Series, persistent watched/unwatched state, and VOD Continue Watching (resume position & auto-complete at 95%)
- Phase 3: complete — encrypted Xtream sources, cached Movie catalogues, poster grids, search, Movie playback, and watched status
- Phase 4: complete — Xtream Series catalogues, lazy episode loading/playback, followed series, watched episodes, and Next Unwatched Episode detection
- Phase 5: complete — Adult (XXX) content filtering, EPG smart normalized auto-matching (fuzzy fallback for channels missing tvg-id), unmatched channels exporter, source credential/role editor with password visibility toggles, unified Favorites across Live TV, Movies, and Series
- Phase 6: complete — Optional cloud account (Supabase), household profiles with parental controls, encrypted cloud source backup/restore, cross-device favorites and watchlist synchronization, and OpenSubtitles cloud sync with password-masked API key
- Phase 7: planned — External Metadata & Media Enrichment (BYOK - Bring Your Own Keys: TMDB, OMDb, TheIntroDB)

## Product direction

FortressTV will evolve from a strong Live TV player into a unified, private media library that can use multiple authorized sources at the same time.

The intended primary navigation is:

```text
Live TV | Movies | Series | Favorites
```

Users should not have to switch the entire application between a Live TV source and a VOD source. Multiple enabled sources contribute to one library while retaining independent configuration, refresh state, cache, ordering, errors, and identity.

## Agreed product decisions

- Multiple sources can be enabled simultaneously.
- The existing M3U/XMLTV Live TV experience remains supported and fast.
- Xtream Codes API source type fully supported for Live TV, Movies, and Series with encrypted credentials.
- A second source may provide Movies and Series without replacing Live TV.
- Each source can be assigned a role: Automatic, Live TV, Movies, Series, Movies and Series, or Everything.
- Source order is user-controlled. Within a source, provider category and item order are preserved by default.
- Favorites and watch history span all enabled sources.
- Provider catalogue data and user-library data are stored separately.
- Large VOD catalogues are lazy, cached, and indexed; they are not fully materialized at startup.
- Picture-in-Picture remains fullscreen-only.
- English and Portuguese (Portugal) ship together for every feature.
- Adult/XXX content filter toggle hides adult categories and streams across Live TV, Movies, Series, and EPG.
- Smart EPG name matching (`EpgMatcher`) normalizes channel names (stripping country prefixes like `PT:`, quality tags like `FHD`/`4K`/`HEVC`, and accents) to auto-match XMLTV display-names when `tvg-id` is absent.

## User-library concepts

The UI may present a combined Favorites destination, but actions should use language appropriate to each content type:

- Live channel: Favorite
- Movie: Add to Watchlist
- Series: Follow series
- Episode: Watched or unwatched

Persistent user-library data will eventually include:

- Favorite channels
- Movie Watchlist
- Followed series
- Episode watched state
- Resume position for Movies and episodes
- Recently watched items
- Live channel watch counts
- Hidden channels/items and custom organization
- Manual EPG assignments

Refreshes, renamed items, reordered playlists, and temporary source failures must not erase this data.

## Source model

Each configured source needs:

- Stable source ID
- User-visible name
- Source type (`M3U_REMOTE`, `M3U_LOCAL`, `XTREAM`)
- Enabled state
- Library role (`AUTOMATIC`, `LIVE_TV`, `MOVIES`, `SERIES`, `MOVIES_AND_SERIES`, `EVERYTHING`)
- User-controlled priority/order
- Encrypted connection configuration (Android KeyStore-backed AES-GCM)
- Independent refresh policy and last-success metadata
- Independent cache generation
- Independent error state

Each imported item needs a stable identity based on its source ID and source-local ID. Display names and stream URLs alone are not sufficiently stable identifiers.

Supported source paths:

1. M3U/M3U8 URL or local file for Live TV, Movies, and Series.
2. Xtream Codes API servers (Server URL, Username, Password) with lazy episode resolution.

## Library model

Conceptually, the catalogue contains:

```text
Library item
├── Live channel
├── Movie
└── Series
    └── Season
        └── Episode
```

Common metadata should be normalized without discarding source-specific fields. Live channels keep original channel/category order and EPG identifiers. Movies and Series may include artwork, synopsis, year, genre, duration, rating, season number, episode number, and stream information when the source supplies them.

## Performance architecture

Live TV and EPG responsiveness must not regress when VOD is added.

- Maintain independent disk caches per source.
- Restore a usable cached library before remote refresh finishes.
- Refresh each source independently and off the main thread.
- Publish parsed results incrementally or transactionally without clearing working data first.
- Load Movie and Series pages lazily.
- Fetch artwork only for visible or near-visible items through the existing bounded Coil caches.
- Do not fetch all seasons and episodes at startup; fetch/cache them when a Series is opened unless the source API provides an efficient catalogue endpoint.
- Build a local search index rather than composing every result during search.
- Bound in-memory lists and derived UI state.
- Benchmark cold/warm startup, Live TV scrolling, EPG opening, Movie grid scrolling, Series opening, and search with realistically large fixture catalogues.

Implemented VOD catalogue safeguards:

- Movie and Series grids use stable keys and reusable content types.
- Poster requests decode to a bounded card-sized bitmap and bitmap decoding has bounded parallelism.
- Search is debounced and catalogue filtering runs away from the main thread.
- Movie and Series scroll positions survive switching between primary library destinations.
- Provider category identity includes the source ID so category-number collisions cannot merge unrelated providers.
- Remote M3U, XMLTV, and Xtream requests retry transient failures up to three attempts with backoff, then expose a manual Retry action.
- A shared collapsible library sidebar provides Live TV, Movies, Series, Favorites, and Settings navigation; Live TV retains a separate contextual filter panel.
- Settings are organized as General, Playlists, EPG, and About; user preferences are kept separate from version, updates, security, and legal information.
- About includes a prominent bilingual notice that FortressTV supplies no playlists or streaming content and is intended for authorized sources.

## Delivery phases

### Phase 0 — Preserve the baseline

- Keep the existing Live TV, XMLTV, playback, PiP, OTA, and performance behavior covered by tests.
- Add migrations rather than replacing current saved settings.
- Record current benchmarks before large structural changes.

### Phase 1 — Multi-source foundation

- Replace the single-active-playlist assumption with multiple enabled sources.
- Add source role, enabled state, ordering, independent status, and cache identity.
- Migrate existing installations automatically: the current active playlist becomes an enabled Live TV source; other saved playlists remain available without data loss.
- Merge enabled Live TV sources in deterministic source/category/item order.
- Ensure one failed source cannot block the library.

### Phase 2 — Unified user library and channel Favorites

- Introduce persistent user-library storage with stable item references.
- Add favorite/unfavorite actions to channel rows and player interfaces.
- Add Favorites to primary navigation.
- Add channel watch counts and Recently Watched without autoplaying content at launch.
- Define behavior for missing or temporarily unavailable source items.

### Phase 3 — Movies

- Add a lazy Movies catalogue with categories, poster grid, search, and details.
- Add Watchlist, playback progress, resume/restart, recently watched, and watched state.
- Reuse Media3 while keeping VOD playback state separate from Live TV state.

### Phase 4 — Series

- Add Series catalogue, details, seasons, and lazy episode loading.
- Add Follow Series, watched/unwatched episodes, resume progress, Continue Watching, and next-episode navigation.
- Avoid automatically downloading every episode record for every Series.

### Phase 5 — Library refinement & Advanced Features

Completed in Phase 5:
- Adult content (XXX) filtering across Live TV, Movies, Series, and EPG
- EPG smart normalized auto-matching (`EpgMatcher`) and export of unmatched channels
- Persistent watched/unwatched items and Next Unwatched Episode detection
- Unified Favorites sections for channels, Movie Watchlist, and followed Series

### Phase 6 — Cloud Accounts, Profiles & Cross-Device Sync

Completed in Phase 6:
- Optional Supabase household account and profile architecture
- Household profiles with parental controls (Adult & Child profiles, age ceilings, unrated blocking, PIN protection)
- Encrypted cloud source backup and restore with AES-256 (`household_sources`)
- Cross-device library synchronization for favorite channels, movie watchlist, followed series, and VOD progress
- OpenSubtitles account and API key cloud synchronization with password-masked input and toggle
- Automatic profile session management on device launch and cross-device ID-to-hash catalog reconnection

### Phase 7 — External Metadata & Media Enrichment (BYOK - Bring Your Own Keys)

The app ships without any API keys. You can enter your own free keys in the app under **Settings > API keys** (`Definições > Chaves de API`):

| Service | Used for | Key |
|---|---|---|
| **TMDB** | posters, descriptions in your language, cast, trailers, automatic genres, episode info, similar titles | free, see below |
| **OMDb** | IMDb and Rotten Tomatoes ratings | free, see below |
| **TheIntroDB** | intro/recap/credits times | no key needed |

#### How to obtain free user keys:
- **TMDB (The Movie Database)**: Create a free account at [themoviedb.org](https://www.themoviedb.org/), navigate to *Settings > API*, and generate a personal API Read Access Token. Enables localized Portuguese (Portugal) synopses, HD backdrops, cast & crew, genres, and trailers.
- **OMDb (Open Movie Database)**: Request a free personal API key at [omdbapi.com](https://www.omdbapi.com/apikey.aspx) (provides 1,000 daily requests for IMDb and Rotten Tomatoes scores).
- **TheIntroDB**: Open-source community timestamps database; requires no key or registration. Powers automatic "Skip Intro", "Skip Recap", and "Skip Credits" action pills during series playback.

#### Privacy and Storage Rules:
- All external API keys are stored in encrypted private preferences (`AndroidKeyStore`-backed AES-GCM) and synced to the user's private cloud profile if cloud sync is enabled.
- External enrichment runs completely off the main thread with bounded disk caching; missing keys or API outages never block or slow down catalogue browsing or provider streams.
- Preserves offline fallback: if no key is provided or the device is offline, the app continues to display provider metadata and Cinemeta artwork fallbacks.

Prioritized Next:
1. D-pad and Android TV navigation improvements
2. Recently Watched and live-channel watch counts
3. Hidden channels, movies, series, and categories
4. Manual EPG channel assignment
5. Advanced per-source headers, Referer, User-Agent, and timeout options
6. Category reorder/rename where safe
7. Audio and subtitle track selection
8. Sanitized playback diagnostics

## Deferred features

These are intentionally outside the current delivery scope:

- Multi-view playback
- Background audio without visible PiP
- Local or NAS recording
- Provider-specific catch-up and timeshift archives
- Stalker/MAC portals
- Automatic duplicate merging across providers

They may be reconsidered after the multi-source library is stable and measured.
