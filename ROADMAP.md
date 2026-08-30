# FortressTV Roadmap

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
- A second source may provide Movies and Series without replacing Live TV.
- Each source can be assigned a role: Automatic, Live TV, Movies, Series, Movies and Series, or Everything.
- Source order is user-controlled. Within a source, provider category and item order are preserved by default.
- Favorites and watch history span all enabled sources.
- Provider catalogue data and user-library data are stored separately.
- Large VOD catalogues are lazy, cached, and indexed; they are not fully materialized at startup.
- Picture-in-Picture remains fullscreen-only.
- English and Portuguese (Portugal) ship together for every feature.

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
- Source type
- Enabled state
- Library role
- User-controlled priority/order
- Encrypted connection configuration
- Independent refresh policy and last-success metadata
- Independent cache generation
- Independent error state

Each imported item needs a stable identity based on its source ID and source-local ID. Display names and stream URLs alone are not sufficiently stable identifiers.

Initial supported source paths:

1. Existing M3U/M3U8 URL or local file for Live TV.
2. Additional M3U/M3U8 sources with manual or heuristic content classification when metadata is limited.
3. A richer VOD source type after confirming the legitimate provider format. Xtream-compatible APIs may be considered because they expose Movies, Series, seasons, episodes, artwork, and metadata explicitly.

Stalker/MAC portal support is not planned at this stage.

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

### Phase 5 — Library refinement

- Hidden items and categories
- Category reorder/rename where safe
- Manual EPG assignment
- Audio and subtitle track selection
- Playback retry and network recovery
- Backup and restore with explicit treatment of credentials
- D-pad and Android TV navigation
- Sanitized playback diagnostics
- Advanced per-source headers, Referer, User-Agent, and timeout options

## Deferred features

These are intentionally outside the first multi-source/VOD delivery:

- Multi-view playback
- Background audio without visible PiP
- Local or NAS recording
- Provider-specific catch-up and timeshift archives
- TMDB/IMDb enrichment
- Stalker/MAC portals
- Automatic duplicate merging across providers

They may be reconsidered after the multi-source library is stable and measured.

## Next decision

Before implementing the VOD parser, identify the VOD source format without exposing real credentials:

- M3U/M3U8 URL or local file
- Xtream-compatible server URL plus username/password
- Another documented, authorized API

The format determines whether FortressTV receives proper Movie/Series metadata or must rely on configurable M3U classification rules.

