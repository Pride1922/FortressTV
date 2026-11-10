# FortressTV Macrobenchmarks

Run these benchmarks on the physical Android tablet. Emulator results are not representative.

## Prerequisites

1. Connect the tablet with USB or wireless debugging.
2. Install/open the normal signed FortressTV build and configure the real playlist and EPG.
3. Let the parsed EPG cache finish once, then close the app.
4. Keep the tablet plugged in, with a stable temperature and no other active applications.

The benchmark build uses the configured release signing key when available, preserving the
installed app's data. Do not clear FortressTV storage before the scrolling benchmarks.

## Build

Do not run `connectedBenchmarkAndroidTest` against a configured installation. The Android
test runner can uninstall the target APK during cleanup, which deletes FortressTV app data.

Build the benchmark APKs without installing them:

```powershell
.\gradlew.bat :app:assembleBenchmark :macrobenchmark:assembleBenchmark
```

## Data-preserving device run

Select one exact physical-device serial and update both APKs in place. Never uninstall the
FortressTV package or clear its data:

```powershell
$adb = "<android-sdk>\platform-tools\adb.exe"
$serial = "<tablet-adb-serial>"
& $adb -s $serial install -r "app\build\outputs\apk\benchmark\app-benchmark.apk"
& $adb -s $serial install -r "macrobenchmark\build\outputs\apk\benchmark\macrobenchmark-benchmark.apk"
& $adb -s $serial shell am instrument -w `
    -e class "com.fortresstv.player.macrobenchmark.FortressTvBenchmark#programmeGuideOpen" `
    com.fortresstv.player.macrobenchmark/androidx.test.runner.AndroidJUnitRunner
```

Replace the class argument with another benchmark method when needed. Direct instrumentation
writes Perfetto traces to
`/storage/emulated/0/Android/media/com.fortresstv.player.macrobenchmark/`; pull required traces
with `adb pull`. Installing with `-r` preserves the configured sources and caches.

The suite measures cold startup, warm startup, channel-list frame timing, programme-guide
frame timing, Movies/Series opening, and Movies/Series grid flinging. The catalogue tests
require an enabled Xtream source with non-empty Movies and Series catalogues. Compare median
and high-percentile frame times between commits; do not judge a change from a single run.

The startup tests require a configured library and an unlocked profile. They wait for
the configured Home or Live TV destination and collect both initial and full display
time. To compare cached startup, populate the cache once on the same build, then run
`coldStartup` online and offline with the same sources and startup destination.
