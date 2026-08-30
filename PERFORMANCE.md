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

## Release check

Before publishing, test fast vertical flings with EPG and logos loaded, open and scroll the
programme guide, change guide days, open the fullscreen channel drawer, and confirm that
playback is not interrupted while browsing.
