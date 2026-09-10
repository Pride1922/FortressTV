# FortressTV RC3 performance profiling plan

## Objective

Profile FortressTV `1.0.0-RC3` (`versionCode 28`, commit `7f105675`) on the primary
physical tablet before promoting the same code to the official release. The run should
detect regressions in startup, large-library navigation, EPG rendering, playback, PiP,
memory use, and cache restoration.

The official release should contain no functional changes after this run. If a fix is
required, publish another candidate and repeat the affected scenarios plus the smoke run.

## Release gates

The candidate passes when all of the following are true:

- No crash, ANR, frozen UI, black playback surface, or invisible background playback.
- No repeatable regression greater than 15% in P95 frame time against the matching
  dataset baseline in `PERFORMANCE.md`.
- Cold startup median stays at or below 650 ms and warm startup median at or below 225 ms.
- Channel-list and VOD-grid P95 CPU frame time stays below 12 ms.
- EPG vertical P95 stays below 12 ms, horizontal P95 below 16 ms, and opening P95 below
  30 ms when using the original 2026-08-30 comparison dataset.
- With the approximately 14,400-channel dataset, EPG P95 stays below 60 ms. Results from
  a different playlist/EPG pairing must be recorded separately, not compared directly.
- Memory reaches a plateau during a 60-minute playback session. After returning to the
  channel browser and allowing one minute for cleanup, total PSS must be no more than 15%
  above the post-startup settled value, unless a Perfetto trace shows retained cache data
  with a deliberate bound.
- Ten fullscreen/PiP/fullscreen cycles and fifty channel changes complete without a black
  surface, stuck audio, duplicated players, or steadily increasing memory.

A single outlier is rerun. A repeatable gate failure is a No-Go until explained and fixed.

## Controlled setup

1. Use the explicitly selected physical tablet; never rely on the default ADB device.
2. Keep it plugged in, above 50% battery, at a stable room temperature, and with battery
   saver disabled. Close unrelated applications and wait five minutes after a hot run.
3. Record device model, Android build, display refresh rate, free storage, app version,
   commit, playlist item count, EPG programme count, matched-channel count, and network type.
4. Use the same real, authorized source data for comparisons. Never copy source URLs,
   credentials, tokens, or provider data into reports, traces, screenshots, or Git.
5. Let source refresh and image loading settle before warm/cache benchmarks. Record thermal
   state before and after each group; discard throttled runs.
6. Disable animations only if the historical baseline used the same setting.

## Phase 1: automated macrobenchmarks

Build the profileable target and benchmark APKs without uninstalling the configured app:

```powershell
.\gradlew.bat :app:assembleBenchmark :macrobenchmark:assembleBenchmark
```

Set explicit local variables for the SDK and selected device, then update both APKs in place:

```powershell
$fortressAdb = "<android-sdk>\platform-tools\adb.exe"
$fortressTablet = "<exact-tablet-serial>"
& $fortressAdb -s $fortressTablet install -r "app\build\outputs\apk\benchmark\app-benchmark.apk"
& $fortressAdb -s $fortressTablet install -r "macrobenchmark\build\outputs\apk\benchmark\macrobenchmark-benchmark.apk"
```

Run each existing benchmark separately so failures and thermal effects remain attributable:

- `coldStartup`
- `warmStartup`
- `channelListFling`
- `programmeGuideOpen`
- `programmeGuideFling`
- `programmeGuideVerticalStress`
- `programmeGuideHorizontalStress`
- `moviesOpen`
- `moviesGridFling`
- `seriesOpen`
- `seriesGridFling`

Use the data-preserving instrumentation command documented in `macrobenchmark/README.md`.
Each benchmark already performs five iterations. Repeat the whole group once if a gate is
missed or device temperature changed materially. Save benchmark JSON and Perfetto traces in
a dated local results directory; commit only sanitized aggregate measurements.

## Phase 2: cache and network behavior

Run these scenarios with the large playlist and real three-day EPG window:

| Scenario | Preparation | Measure |
| --- | --- | --- |
| Parsed-cache startup | Complete one successful online refresh, force-stop, relaunch | Time until channel list is usable |
| Offline cache restore | After a successful refresh, disable network, force-stop, relaunch | Time to usable cached library; correct error state |
| Cold source parse | Use an intentionally controlled test source/cache state | Parse duration, peak memory, main-thread stalls |
| Failed source refresh | Keep another source cached and make one test source unavailable | Other sources remain usable; failure is isolated |
| Network recovery | Restore connectivity and retry | Recovery time, duplicate entries, memory return |

Do not delete production user data to create the cold-cache case. Use a disposable test source
or a backed-up test installation state.

## Phase 3: playback and lifecycle stress

Use at least one HLS live stream, one stream requiring the FFmpeg audio fallback, and one
series episode with a following episode.

1. Measure time from channel tap to first rendered frame for ten channel starts.
2. Perform fifty channel changes at a realistic pace; note median and slowest start.
3. Play continuously for 60 minutes, sampling `dumpsys meminfo` at 0, 10, 30, and 60 minutes.
4. Perform ten fullscreen → PiP → fullscreen cycles while the stream is playing.
5. Close PiP five times and verify playback and player resources stop promptly.
6. Send the app to the background from inline playback and confirm it pauses without PiP.
7. During an episode, verify the next-episode countdown begins around 45 seconds remaining,
   pauses with playback, can be cancelled, and does not create overlapping player instances.
8. Rotate portrait/landscape ten times across browsing and VOD playback.

Capture a Perfetto/System Trace for any black frame, audio-only playback, pause longer than
two seconds, or memory step that does not recover.

## Phase 4: manual UX responsiveness

Rate each action as immediate, noticeable, or disruptive and record any action over one second:

- First channel list render and fast vertical flings with logos and EPG loaded.
- Programme Guide open, vertical/horizontal stress, and changing all guide days.
- Fullscreen channel drawer open, scroll, search, and channel selection during playback.
- Movies and Series open, repeated grid flings, search typing, season open, and episode start.
- Rapid navigation between Live TV, Movies, Series, and Settings.
- Backup creation/restore progress UI using a representative large backup.

## Evidence and reporting

Append a dated RC3 section to `PERFORMANCE.md` containing:

- Device/build and sanitized dataset sizes.
- Cold/warm startup medians.
- P50, P95, P99, and P95 frame-overrun values for every valid benchmark.
- Playback startup median/maximum, channel-switch results, and PiP cycle result.
- Memory samples and settled-memory comparison.
- Cache online/offline timings, thermal notes, failures, and trace locations.
- A final `PASS`, `PASS WITH ACCEPTED VARIANCE`, or `FAIL` verdict for each phase.

Do not average incompatible datasets. Compare the original dataset with its 2026-08-30/31
baseline and report the large 14,400-channel dataset as its own series.

## Recommended execution order

1. Automated macrobenchmarks while the tablet is cool.
2. Cache/network scenarios.
3. Playback and PiP stress.
4. Sixty-minute soak test.
5. Manual responsiveness pass and results write-up.

Estimated hands-on time is 60–90 minutes plus the 60-minute soak. Most macrobenchmark and
soak time is unattended.
