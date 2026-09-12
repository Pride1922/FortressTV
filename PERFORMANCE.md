# FortressTV performance roadmap

Performance is a core product requirement. Changes to channel browsing, XMLTV parsing,
programme-guide rendering, logos, or playback should be tested on the target tablet with
a real multi-day EPG before release.

## Implemented

- Playback uses separate live and VOD buffer profiles with Automatic, Stable, and Data Saver modes.
- Live stall recovery preserves slowly arriving data, waits while the network is unavailable, and uses bounded backoff only when playback, buffer, and transfer progress have all stopped.
- Playback diagnostics record sanitized stream type, adaptive-track availability, selected resolution and bitrate, bandwidth estimates, startup readiness, rebuffer count, network type, and Media3 error codes under `FortressTV.QoE`; source URLs and credentials are excluded.
- Wi-Fi and cellular handovers trigger one controlled recovery when a player is buffering or failed.

- Channel lists use `LazyColumn`, so only visible and prefetched channels are composed.
- Channel rows have stable keys and reusable content types.
- Current-programme presentation is cached per channel until the guide clock changes.
- Fullscreen and main channel lists share the optimized EPG row implementation.
- Channel-logo crossfades are disabled and logos use low-cost filtering in scrolling lists.
- XMLTV parsing runs on a background dispatcher and discards obsolete/far-future entries.
- XMLTV retains the previous six hours and three calendar days (today plus two), even when
  the source provides more; the remote source is still refreshed at most once every 24 hours.
- Programme-grid block preparation runs on a background dispatcher.
- The three-day programme grid is indexed in the background one day at a time. Today is
  published first; prepared days use a cached date/channel lookup when selected.
- EPG rows compose only programmes inside the visible horizontal time window plus a small
  prefetch buffer, rather than every block across the full 24-hour canvas.
- Today's EPG rows are published progressively in small per-frame batches, avoiding a
  single long composition frame when the guide opens; future days remain immutable maps.
- Timeline labels and programme blocks use a compact render window until horizontal
  navigation begins, then switch to a wider two-hour bucket with stable programme keys.
- Parsed EPG data is stored in a versioned, compressed, atomic disk cache and reused when
  the source fingerprint is unchanged. Missing, stale, or corrupt caches fall back to XML.
- Resident programme entries keep parsed epoch timestamps only; duplicate raw XML date
  strings are discarded after parsing to reduce guide memory use.
- Movies and Series filtering and sorting run together on a background dispatcher; only the
  latest query, category, and sort result is published to the UI.
- Movie progress is indexed once per progress update instead of scanning the full progress
  history while composing each visible poster card.
- Macrobenchmarks cover Movies and Series opening and repeated grid flinging with real catalogues.
- Live TV computes EPG match counts and unmatched-channel diagnostics together on a background
  dispatcher instead of resolving every channel twice on the main thread.
- EPG channel resolution is cached per `tvg-id` and channel name, so a 14,400-channel playlist
  is normalized once per loaded guide rather than again in each screen and diagnostic pass.
- Programme Guide opening prepares only the selected day. Tomorrow and the following day are
  indexed on demand when selected instead of scanning the full playlist three times up front.
- Parsed M3U playlists are stored in a versioned, compressed, atomic per-source cache and reused
  while the raw source fingerprint is unchanged. Missing, stale, or corrupt caches fall back to
  the M3U parser.
- Stable M3U item identifiers are computed during the initial parse and persisted, avoiding a
  SHA-256 identity calculation for every channel during each subsequent source merge.

## Next profiling targets

- Run the playback resilience matrix on an explicitly selected physical tablet with adaptive HLS, fixed HLS, MPEG-TS, and VOD sources. Measure startup time, rebuffer ratio, recovery time, channel-switch time, and live offset under gradual bandwidth reduction, short outages, and Wi-Fi/cellular handover.

- Replace the programme guide's two mutually synchronized vertical lists with a shared
  vertical scrolling model if profiling shows scroll feedback or dropped frames.
- Consider retaining programme descriptions outside the always-resident schedule model.
- Add a short search debounce only if testing with very large playlists shows typing lag.
- Add a Macrobenchmark/profileable build and record frame timing for the channel list,
  fullscreen drawer, programme guide, and guide day changes.

## Measurement

The `macrobenchmark` module measures cold/warm startup and frame timing for channel and EPG
scrolling on the physical tablet. See `macrobenchmark/README.md` for prerequisites and commands.

### POCO baseline (2026-08-30)

Measured on the configured Android 16 POCO with 89 channels and the real three-day in-app
EPG window. Values below are CPU frame duration unless labelled as frame overrun.

| Scenario | P50 | P95 | P99 | P95 overrun |
| --- | ---: | ---: | ---: | ---: |
| Channel-list fling | 3.6 ms | 4.8 ms | 5.2 ms | -1.4 ms |
| EPG single vertical fling | 4.7 ms | 7.3 ms | 10.0 ms | 1.1 ms |
| EPG repeated vertical stress | 4.7 ms | 6.6 ms | 10.6 ms | 0.0 ms |
| EPG repeated horizontal stress | 5.9 ms | 19.2 ms | 26.5 ms | 15.7 ms |
| Opening the EPG | 5.9 ms | 38.3 ms | 47.8 ms | 33.1 ms |

Cold initial display was 511.5 ms median and warm initial display was 157.2 ms median.
Vertical channel/EPG scrolling is healthy. The next optimization targets are EPG opening
and horizontal timeline movement; changes should retain the vertical baseline.

### EPG optimization result (2026-08-30)

Measured with the same tablet, playlist, EPG, five iterations, and readiness-aware tests:

| Scenario | Baseline P95 | Optimized P95 | Baseline P99 | Optimized P99 | Baseline P95 overrun | Optimized P95 overrun |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Opening the EPG | 38.3 ms | 23.2 ms | 47.8 ms | 32.1 ms | 33.1 ms | 17.2 ms |
| Horizontal stress | 19.2 ms | 11.0 ms | 26.5 ms | 28.3 ms | 15.7 ms | 8.8 ms |
| Vertical stress | 6.6 ms | 7.0 ms | 10.6 ms | 10.3 ms | 0.0 ms | 0.0 ms |

Opening and horizontal P95 costs are substantially lower. Vertical performance remains
within measurement variance, and its P99 slightly improved. The horizontal P99 CPU value
is 1.8 ms higher, while its P99 deadline overrun remains effectively unchanged; stable
keys prevent the larger boundary spikes observed during intermediate experiments.

### VOD catalogue baseline (2026-08-31)

Measured on the same POCO with the configured real Xtream catalogue, five iterations, and
the profileable benchmark build:

| Scenario | CPU P50 | CPU P95 | CPU P99 | P95 overrun |
| --- | ---: | ---: | ---: | ---: |
| Movies grid repeated fling | 4.5 ms | 7.1 ms | 9.1 ms | 1.4 ms |
| Opening Movies | 3.4 ms | 17.3 ms | 21.3 ms | 11.1 ms |
| Opening Series | 3.6 ms | 20.3 ms | 21.8 ms | 16.4 ms |

Movies scrolling is healthy; opening Movies and Series is the next VOD optimization target.
The configured Series catalogue did not produce a scrollable grid, so Series fling timing was
not valid. Catalogue fling benchmarks now skip non-scrollable grids instead of reporting a
missing-frame failure.

A focused five-iteration run with the newly configured partial Portuguese EPG measured opening
at 52.4 ms CPU P95, 55.6 ms P99, and 43.5 ms P95 deadline overrun. Only four frames were rendered
per opening because relatively few channels had matched programme data. The opening cost is
repeatable, but this different dataset is not a clean regression comparison with the 2026-08-30
baseline. The five Perfetto traces are stored under
`macrobenchmark/build/outputs/manual/programmeGuideOpen-2026-08-31/`.

### Large playlist result (2026-08-31)

Measured on the POCO with approximately 14,400 channels and the partial Portuguese EPG. The
optimized EPG frame timing was 48.1 ms CPU P95 and 54.5 ms P99. An earlier 211.6-second end-to-end
run included a transient server failure and manual Retry, so it is not a valid parsed-cache
baseline and must not be used for a before/after claim. A controlled online/offline startup test
is still required to quantify parsed-cache restoration independently from network availability.

## Release check

### Three-day EPG interaction follow-up (2026-09-01)

Tested on the connected tablet with approximately 14,447 live entries and a three-day XMLTV
guide. Only 23 playlist channels matched the supplied guide. Day partitions are now retained
and pre-indexed for the configured guide window, channel matching runs off the main thread, and
the horizontal timeline position is preserved while changing days.

After the change, the logged current-day partition load was 0.0 ms and programme-block indexing
was 5.3 ms for 2,198 programmes. A single-scroller experiment reduced one short `gfxinfo` sample's
janky-frame percentage, but caused transient channel/programme row misalignment and incorrect
touch scrolling in the fixed channel pane, so it was rejected. The guide retains the paired lazy
lists used by RC1.5 for row alignment and correct interaction. Further guide rendering work should
be validated without compromising those behaviours.

Before publishing, test fast vertical flings with EPG and logos loaded, open and scroll the
programme guide, change guide days, open the fullscreen channel drawer, and confirm that
playback is not interrupted while browsing.

### RC3 automated profile (2026-09-09)

Profiled `1.0.0-RC3` (`versionCode 28`, commit `7f105675`) on the Android 16 POCO
`25099RP08G`. The tablet started at 28.0 C and finished at 30.0 C with no reported thermal
throttling. The configured installation and caches were preserved. Each valid macrobenchmark
used five iterations.

| Scenario | Result | P50 | P95 | P99 | P95 overrun | Gate |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| Cold startup | 552.8 ms median | - | - | - | - | Pass (<650 ms) |
| Warm startup | 159.1 ms median | - | - | - | - | Pass (<225 ms) |
| Channel-list fling | Valid | 4.3 ms | 6.6 ms | 9.8 ms | 0.6 ms | Pass |
| EPG open | Valid | 3.3 ms | 10.8 ms | 15.9 ms | 7.8 ms | Pass |
| EPG single fling | Valid | 4.8 ms | 11.0 ms | 19.7 ms | 5.2 ms | Pass |
| EPG vertical stress | Valid | 4.9 ms | 10.3 ms | 15.0 ms | 4.6 ms | Pass |
| EPG horizontal stress | Valid | 5.9 ms | 14.2 ms | 25.3 ms | 9.7 ms | Pass |
| Movies open, controlled rerun | Valid | 4.3 ms | 15.8 ms | 22.6 ms | 12.7 ms | Pass |
| Series open | Valid | 4.0 ms | 18.2 ms | 22.2 ms | 13.4 ms | Pass |
| Movies grid fling | No rendered frames | - | - | - | - | Not measured |
| Series grid fling | No rendered frames | - | - | - | - | Not measured |

The first Movies-open group measured 20.5 ms P95. Its required controlled rerun measured
15.8 ms P95, below the 17.3 ms catalogue baseline, so the first result is treated as run
variance. Both catalogue fling scenarios reached a non-scrollable grid state and produced no
RenderThread slices; these are invalid measurements rather than application failures.

The settled app process immediately after restoring and launching the signed RC3 release used
220,283 KB total PSS and 350,212 KB total RSS. This is a reference point only, not a leak result.
A playback channel must be selected before the planned 60-minute memory soak, channel-switch,
and fullscreen/PiP stress checks can be completed.

Automated profile verdict: **PASS WITH COVERAGE GAPS**. All valid startup and frame-timing
measurements passed the absolute RC3 gates. Remaining release evidence is the hands-on playback
and PiP stress pass, the long memory soak, and catalogue fling timing with sufficiently large
visible Movies and Series grids. The controlled Movies rerun traces are stored under
`macrobenchmark/build/outputs/manual/rc3-2026-09-09/`.
