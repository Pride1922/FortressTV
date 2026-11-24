# FortressTV Design & UX Roadmap

## Implementation status — 24 November 2026

This document retains the original UX proposals, not a list of entirely missing features. Use [ROADMAP.md](ROADMAP.md) as the current feature inventory. Implementation does not imply completed device validation.

| Original area | Current status | Remaining scope |
|---|---|---|
| Home and unified hub | Home shelves, Continue Watching, next episodes, favourite/recent channels, unified search, startup destination, and shelf customisation are implemented. | Refine interaction and measure performance. |
| Live TV workspace | Responsive channel browsing, docked-player paths, and fullscreen quick-zap controls exist. | Validate continuity, responsive layouts, and remote focus across supported devices. |
| Series and VOD | Details/artwork, season/episode browsing, progress, watched state, sorting, and catalogue navigation exist. | Audit exact visual specifications below before treating them as delivered. |
| Player controls | Audio/subtitle selection, quality controls, Go Live, resilience settings, sleep timer, next-episode autoplay, explicit live/movie version selection, and saved VOD subtitle timing exist. | Validate supported subtitle formats and timing changes on devices; assess the remaining flyout presentation targets. |
| Visual polish | Shared theme and TV focus helpers exist. | Blur, ambient artwork colouring, typography, and motion below remain design targets unless individually verified. |

The detailed sections below describe intended interactions and visual treatments. Exact thresholds, gesture mappings, default values, and layouts must be checked against code before implementation or documentation changes. They are not acceptance-test results. New features require English and Portuguese (Portugal), touch and D-pad accessibility, and the verification defined in [AGENTS.md](AGENTS.md).

This document outlines the UI/UX evolution of FortressTV while preserving private credential storage, optional external integrations, multi-source independence, offline cache resilience, and hardware efficiency.

---

## Strategic Vision

FortressTV combines the immediacy of traditional linear broadcast television with the rich discovery experience of modern streaming platforms. Rather than forcing users into siloed content buckets, the interface adapts dynamically to screen sizes, viewing postures (handheld, kickstand, television D-pad), and user habits.

---

## 5-Phase Delivery Roadmap

```text
Phase 1: Home & Unified Hub
   ├── Continue Watching (VOD in-progress)
   ├── Next Up (Followed series next unwatched episode)
   ├── Favorite Channels Now (Live EPG progress strip)
   ├── Recent Channels (Quick re-zap)
   ├── Unified Search (Omni-search channels, movies, series)
   └── Configurable Startup Screen (Home vs. Live TV)

Phase 2: Live TV Workspace
   ├── Responsive Tri-Pane Landscape Layout (Sources -> Channels -> Player)
   ├── Docked Video inside EPG (Seamless timetable browsing)
   ├── Quick-Zap Carousel Drawer (Bottom tray during playback)
   └── Zero-Interruption Channel Browsing

Phase 3: Series & VOD Presentation
   ├── Hero Spotlight Stage (Backdrop fanart, metadata badges, ratings)
   ├── Series Hub (Season horizontal pill switcher)
   ├── 16:9 Episode Cards with Watch Progress
   └── High-Density Fast-Scroller for Large Catalogues

Phase 4: Player Controls & OSD
   ├── Streamlined Audio & Subtitle Flyout Drawer
   ├── Real-Time Subtitle Delay Calibration (-5s to +5s slider)
   ├── Manual Adaptive Stream Quality Picker
   ├── "Go Live" Catchup Action for Drifted HLS Streams
   └── Temporary Network Resilience Profile Override

Phase 5: Visual Polish & Expressive Depth
   ├── Glassmorphic Translucent Overlays with Gaussian Blur
   ├── Ambient Color Scrim (Tints derived from active poster/logo)
   ├── Tabular Clocks & Timestamps (Zero-jitter layout)
   └── Directional Spring Motion & D-pad Elevation Physics
```

---

## Phase 1: Home & Unified Hub (Detailed Specification)

### Objectives
1. Provide an immediate, personalized media overview upon launch.
2. Unify locally stored user data (`VodProgressStore`, `UserLibraryStore`, `DefaultEpgRepository`) without network overhead.
3. Allow users to configure their startup destination so traditional TV watchers can still boot directly into Live TV.

### UI Components

#### 1. Top Bar & Unified Search
- Prominent Fortress shield mark and title.
- Quick action pills: Settings, Update notification (if available).
- Integrated search bar: Filters simultaneously across:
  - Live Channels (`ChannelSearch`)
  - Movies in loaded Xtream catalogs
  - Series in loaded Xtream catalogs
- Instant grouped results with content type badges.

#### 2. Continue Watching Shelf
- Displays in-progress Movies and Series Episodes from `VodProgressStore`.
- Card details:
  - 16:9 or 2:3 card format depending on item type.
  - Linear progress bar showing `progressFraction`.
  - Episode badge (e.g. `S1 E3`) or Movie duration remaining.
  - Tap to resume at saved millisecond position.
  - Overflow/long-press menu: "Mark as Watched" or "Remove from In Progress".

#### 3. Next Up (Series)
- Computes the next unwatched episode for followed series.
- Displays series title, episode title, season/episode number, and thumbnail.
- One-tap immediate stream playback.

#### 4. Favorite Channels Now
- Horizontal scroll of the user's favorite channels.
- Cards feature:
  - High-res channel logo or fall-back badge.
  - Channel name.
  - Current airing programme from local EPG snapshot.
  - Real-time progress bar indicating show completion percentage.
  - Next upcoming programme name and start time.
- Clicking any card opens Live TV tuned directly to that channel.

#### 5. Recent Channels
- Horizontal strip of recently watched channels (from `UserLibraryStore.recentChannelIds()`).
- Allows rapid re-zapping to recently visited channels.

#### 6. Startup Screen Configuration
- Setting in **General Settings**:
  - `Home`: Opens the media dashboard on launch (default).
  - `Live TV`: Boots straight into the last played or first live channel.
- Fully localized in English (`en`) and Portuguese (`pt-PT`).

---

## Phase 2: Live TV Workspace

### Responsive Layout (Tablets & Foldables)
- **Compact (< 700dp)**: Vertical stack with pinned video surface and bottom channel list.
- **Medium / Expanded (≥ 700dp)**: Three-pane layout:
  - Left pane: Categories and sources (collapsible).
  - Center pane: Channels with current programme line.
  - Right pane: Live video surface, current/upcoming show synopsis, audio/subtitle toggles.

### Picture-in-EPG
- When opening the programme guide, active stream docks to a persistent picture-in-preview tile in the top corner.
- Timetable navigates smoothly below without stopping or reloading the player.

### Quick-Zap Tray
- During fullscreen playback, an edge swipe or D-pad Up brings up a translucent overlay row of favorite channels without interrupting playback.

---

## Phase 3: Series & VOD Presentation

### Hero Spotlight
- Dynamic backdrop banner updating on item focus.
- Displays metadata pill chips: Resolution (`4K`, `FHD`, `SD`), Audio (`5.1`, `Stereo`), Release Year, Genre, Rating.

### Series Architecture
- Dedicated series landing page:
  - Season pill selector with count of unwatched episodes.
  - Episode list with 16:9 preview frames, runtime, and progress indicators.
  - "Next Up" hero banner if partly watched.

---

## Phase 4: Player Controls & OSD

### Streamlined Flyout Drawers
- Replaces modal dialogs with sleek side drawers for audio streams and subtitles.
- Real-Time subtitle offset slider with ±100ms precision steps.
- Bitrate/resolution picker for multi-variant HLS streams.

### Stream Resilience Controls
- "Go Live" pill button for drifted live streams.
- Temporary buffer booster toggle when watching over unstable cellular networks.

---

## Phase 5: Visual Polish & Atmosphere

- **Tonal Elevation**: Refined dark theme palette (`#0B0F17` base with `#182231` glassmorphic cards).
- **Ambient Glow**: Soft background luminance matched to active stream branding.
- **Tabular Figures**: Tabular numbers for clocks, elapsed playback times, and EPG timestamps.
- **Focus & Motion**: Compose spring animations calibrated for touch flings and D-pad remote responsiveness.
