# FortressTV performance roadmap

Performance is a core product requirement. Changes to channel browsing, XMLTV parsing,
programme-guide rendering, logos, or playback should be tested on the target tablet with
a real multi-day EPG before release.

## Implemented

- Playback uses separate live and VOD buffer profiles with Automatic, Stable, and Data Saver modes.
- Live stall recovery preserves slowly arriving data, waits while the network is unavailable, and uses bounded backoff only when playback, buffer, and transfer progress have all stopped.
- Playback diagnostics record sanitized stream type, adaptive-track availability, selected resolution and bitrate, bandwidth estimates, startup readiness, rebuffer count, network type, and Media3 error codes under `FortressTV.QoE`; source URLs and credentials are excluded.
- Live playback diagnostics also report input frame-rate metadata, dropped-frame events,
  cumulative decoder counters, and ten-second frame-submission timestamp samples.
  Submission samples describe frames handed to the surface, not measured display refreshes;
  unknown input frame rates must not be interpreted as zero FPS.
- Wi-Fi and cellular handovers trigger one controlled recovery when a player is buffering or failed.

- Channel lists use `LazyColumn`, so only visible and prefetched channels are composed.
- Channel rows have stable keys and reusable content types.
- Current-programme presentation is cached per channel until the guide clock changes.
- Fullscreen and main channel lists share the optimized EPG row implementation.
- Channel-logo crossfades are disabled and logos use low-cost filtering in scrolling lists.
- XMLTV parsing runs on a background dispatcher and discards obsolete/far-future entries.
- XMLTV retains the previous six hours and three calendar days (today plus two), even when
  the source provides more. Launch, resume, and day rollover can refresh a remote guide in
  the background. A complete guide downloaded today is reused. If the guide still lacks a
  day, a later resume can retry after one hour. Failed downloads preserve the usable cache.
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

## Mi Box playback investigation — 2026-09-12

On the explicitly selected MIBOX4 (Android 9), the display was in 1080p60 mode.
A user-selected live H.264 1080p stream reported unknown frame-rate metadata.
Repeated ten-second samples measured 24.97–25.00 submitted frames per second,
with 40,000 microsecond presentation steps. Decoder counters advanced from
407 rendered / 403 skipped to 658 rendered / 654 skipped over approximately ten
seconds, with zero dropped buffers. These are submission/decoder measurements,
not physical display FPS. The skipped-buffer count needs further investigation;
it does not establish that half the source's distinct pictures are missing.
There is no before/after comparison proving a recent application regression yet.
Compare the same channel at 50 Hz and inspect decoder output timestamps before
changing playback policy.

On 2026-09-13 the same selected Mi Box delivered a user-selected stream at
49.97–50.05 submitted FPS with 20,000 microsecond presentation steps and zero
dropped or skipped buffers, while its display remained at 1080p60. A subsequent
720p stream also settled at 50 FPS. The previous day's 25 FPS / skipped-buffer
behavior is therefore not universal across channels. A same-channel 50 Hz
display comparison was then performed: Android confirmed 1080p50, but the user
reported both tested channels still looked rough. Matching refresh rate alone is
therefore insufficient. The second channel again showed 25 FPS with about one
skipped buffer per rendered buffer.

A local, unvalidated experiment disables Media3's identical-release-time buffer
skip only for live playback on MIBOX4 / oneday / Android 9 using an OMX.amlogic
decoder. Other devices and audio renderers retain their existing behavior.
Compare both channels at 50 Hz and require visible motion improvement before
retaining this workaround; counter changes alone do not demonstrate a fix.

After installing the experiment, the 720p case remained near 50 submitted FPS
with zero skips/drops. After switching to 1080p, rendered buffers advanced from
423 to 924 in 10.041 seconds (about 49.90 output buffers/s), again with zero
skips/drops. Previously the 1080p case showed about 25 rendered and 25 skipped
buffers/s. Display output remained at 50 Hz. This confirms the skip rule changes
buffer delivery; it does not confirm distinct-picture rate or visible smoothness.
The user reported smoother motion with the experiment at 50 Hz. After restoring
the user's original 1080p60 display mode (verified through Android), the 1080p
rendered counter advanced from 858 to 1360 in 10.023 seconds, with zero skipped
or dropped buffers. The user also confirmed smoother motion after restoring
1080p60. The exact historical RC regression has not been isolated.

## Next profiling targets

### RC8 remote-startup recovery correction

The live buffering monitor now repeats its observation window after partial
progress or an absent network route, instead of ending after a WAIT decision.
It uses fresh deltas for each window, preserves progressing buffers, stops
after retry exhaustion, and rechecks playback state after recovery backoff.
Network validation is no longer a prerequisite for attempting a stream request;
the presence of a default route allows the existing bounded recovery policy to
try the actual source. Default-network capability transitions also notify recovery,
including the first connection after starting offline and late validation.
Duplicate capability callbacks do not repeatedly trigger recovery.

Eight startup/network regression tests cover partial data followed by silence,
continued buffer progress, disconnected startup, retry exhaustion, pause/error,
coroutine cancellation, initial connectivity and network handover. These are
deterministic logic tests, not evidence of successful playback on the reporting
user's overseas 5G carrier. Physical cellular and Wi-Fi playback checks remain
required before publishing the correction. The buffering label is now neutral
in English and pt-PT; elapsed buffering time does not diagnose an unstable link.

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

### RC19 startup baseline (2026-11-10)

The Android 16 tablet running RC19 drew its first frame in **503 ms median**
across seven process-cold launches (437–650 ms). Home was visible by the first
accessibility sample in three separate launches (5.32–5.35 s from the ADB
launch command). The sampler waits for UI idle and adds overhead, so those
observations give an upper bound for Home readiness.

The updated startup benchmarks report initial and full display time. Full display
marks the first frame with the configured library destination available for input.
Home shelves and EPG can populate after that frame. Compare populated-cache runs
online and offline. For Xtream, allow one successful Live TV load after upgrading
to populate the endpoint-bound cache before measuring cache restoration.

### RC20 startup changes on the same tablet (2026-11-10)

The local RC20 benchmark build drew its first frame in **449 ms median**
across seven process-cold ADB launches (403–556 ms). With the same launch
command, RC19 took 503 ms median. The median was 54 ms lower (10.7%).

The configured Home destination passed all three Macrobenchmark scenarios
with the populated caches. Each scenario ran five iterations. Thermal status
was normal after every run.

| Scenario | First frame median | Home ready median | Home ready range |
| --- | ---: | ---: | ---: |
| Cold online, no precompilation | 686.6 ms | 1,235.3 ms | 1,184.9–1,290.8 ms |
| Warm online, full compilation | 281.7 ms | 760.4 ms | 620.0–1,082.0 ms |
| Cold with app networking blocked, no precompilation | 657.4 ms | 1,194.3 ms | 1,170.5–1,297.9 ms |

The network-blocked run used Android's per-app debug firewall while Wi-Fi and
ADB remained available. The prior firewall state was restored afterward.
The RC19 accessibility sample provides only an upper bound, so there is no
direct before-and-after comparison for Home readiness.

### RC22 live channel switching on the tablet (2026-11-10)

An instrumented benchmark build measured live channel changes in Automatic mode on
the Android 16 tablet. It recorded selection, media source preparation, the first
network payload bytes delivered to the player, and Media3's first rendered frame.

Across 24 switches between three configured Xtream channels, 22 reached a first
frame, one ended with a bad HTTP status, and one reached the 15-second observation
limit without stream bytes. The control served the same paced synthetic 720p
H.264/AAC MPEG-TS fixture under two channel entries on tablet loopback. All 12
control switches reached a first frame.

| Source | Selection to prepare median | Selection to bytes median | Bytes to frame median | Selection to frame median | Selection to frame P95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Xtream | 18 ms | 238 ms | 170 ms | 414 ms | 9,494 ms |
| Tablet loopback | 21 ms | 132 ms | 478 ms | 606 ms | 623 ms |

The percentiles use nearest rank across completed switches. The two slowest
completed Xtream switches spent 9,335 ms and 11,282 ms reaching the first bytes,
followed by 159 ms and 153 ms to render a frame. The wait for bytes includes the
network path, server response, and any request retries. The timings cannot
separate those contributions. The control's frame times reflect its own stream
format and delivery pacing.

Three additional Xtream trials held playback for at least 30 seconds after the
first frame or the 15-second cutoff. Two reached a frame and logged no completed
rebuffer during the hold. The other delivered no bytes before the next switch.

The follow-up benchmark traces the start and end of each HTTP open, numeric
response status, and Media3's retry decisions before the first frame. It uses
the same Media3 HTTP transport as regular playback. An open duration combines
connection setup, redirects, and the response wait. Retry delays describe the
policy decision, while the next open timestamp shows when a new request starts.

In the 24-switch Xtream follow-up, 20 switches reached a frame, with a median of
455 ms and a completed-switch P95 of 1,447 ms. The slowest completed switch
took 3,271 ms, including 2,885 ms in its first HTTP open. One switch hit the
15-second observation cutoff. Its first HTTP open later logged a timeout at
20,156 ms without a response status. The other three incomplete switches
received HTTP 403 on the same channel, in 89–129 ms of HTTP open time. That
channel also completed four switches. The only logged retry decisions declined
to retry the 403 responses. No retry backoff was observed in this sample.

Automatic live playback now gives the first HTTP transfer six-second connect
and read timeouts. A socket timeout while opening that transfer gets one
immediate retry using the regular timeouts. Later opens use the regular
timeouts, including recovery after a read stall on the first transfer.

On the physical tablet, a loopback fixture withheld the first response for one
channel. Its first HTTP open timed out after 6,004 ms, and Media3 started the
retry 1 ms after the zero-delay retry decision. The retry returned HTTP 200 in
82 ms and the first frame arrived 6,648 ms after selection. Two subsequent
ordinary switches rendered in 610 and 612 ms. The player entered fullscreen
and returned to the inline channel list during the control run.

With the fix installed, all 24 Xtream switches reached a first frame. The median
was 415 ms and the P95 was 2,125 ms. The slowest switch took 11,437 ms. Its first
HTTP open timed out after 6,158 ms, the retry began 1 ms after the retry decision,
and the retry took 5,096 ms to open successfully. No HTTP 403 occurred in this
run. The two Xtream runs sampled intermittent upstream behavior, so their
completion counts alone cannot establish a change in the long-term error rate.

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
