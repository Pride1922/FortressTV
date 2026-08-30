# FortressTV performance roadmap

Performance is a core product requirement. Changes to channel browsing, XMLTV parsing,
programme-guide rendering, logos, or playback should be tested on the target tablet with
a real multi-day EPG before release.

## Implemented

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

## Next profiling targets

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

## Release check

Before publishing, test fast vertical flings with EPG and logos loaded, open and scroll the
programme guide, change guide days, open the fullscreen channel drawer, and confirm that
playback is not interrupted while browsing.
