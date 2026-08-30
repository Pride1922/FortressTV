# FortressTV Macrobenchmarks

Run these benchmarks on the physical Android tablet. Emulator results are not representative.

## Prerequisites

1. Connect the tablet with USB or wireless debugging.
2. Install/open the normal signed FortressTV build and configure the real playlist and EPG.
3. Let the parsed EPG cache finish once, then close the app.
4. Keep the tablet plugged in, with a stable temperature and no other active applications.

The benchmark build uses the configured release signing key when available, preserving the
installed app's data. Do not clear FortressTV storage before the scrolling benchmarks.

## Run

From PowerShell in the project root:

```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
```

If more than one Android device is connected, select exactly one first:

```powershell
$env:ANDROID_SERIAL = "<tablet-adb-serial>"
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
Remove-Item Env:ANDROID_SERIAL
```

Run one benchmark class or method with Android Studio's gutter action when iterating.
Results and Perfetto traces are written under `macrobenchmark/build/outputs/`.

The suite measures cold startup, warm startup, channel-list frame timing, and programme-guide
frame timing. Compare median and high-percentile frame times between commits; do not judge a
change from a single run.
