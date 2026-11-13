#!/usr/bin/env bash
# Generates the FortressTV Baseline Profile on the device or emulator that adb sees and copies
# the result to work/baseline-profile/.
#
# It installs with `adb install -r` and never uninstalls, so it does not delete the data of an
# installed app that is signed with the same key. (The Gradle task
# `:app:generateReleaseBaselineProfile` does the same work in one step, but goes through the
# Android test runner, which can uninstall the app when it finishes.)
#
# Usage: bash scripts/generate-baseline-profile.sh        (ADB and ANDROID_SERIAL are honoured)
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ADB:-adb}"
APP_ID="com.fortresstv.player"
TEST_ID="com.fortresstv.player.baselineprofile"
OUT="work/baseline-profile"

# The app build the generator runs against is not minified, so the profile has real class names.
bash ./gradlew --no-daemon --max-workers=2 \
    :app:assembleNonMinifiedRelease :baselineprofile:assembleNonMinifiedRelease

app_apk="$(find app/build/outputs/apk/nonMinifiedRelease -name '*.apk' | head -n 1)"
test_apk="$(find baselineprofile/build/outputs/apk/nonMinifiedRelease -name '*.apk' | head -n 1)"
if [ -z "$app_apk" ] || [ -z "$test_apk" ]; then
    echo "Could not find the nonMinifiedRelease APKs." >&2
    exit 1
fi

"$ADB" wait-for-device
"$ADB" install -r "$app_apk"
"$ADB" install -r "$test_apk"

rm -rf "$OUT"
mkdir -p "$OUT/device"
"$ADB" shell rm -rf "/sdcard/Android/media/$TEST_ID" || true

# `am instrument` exits with 0 even when the test fails, so the output is checked below.
"$ADB" shell am instrument -w \
    -e class "$TEST_ID.BaselineProfileGenerator" \
    -e androidx.benchmark.enabledRules BaselineProfile \
    -e androidx.benchmark.suppressErrors EMULATOR \
    "$TEST_ID/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/instrumentation.txt"

if ! grep -q '^OK (' "$OUT/instrumentation.txt"; then
    echo "The Baseline Profile generator did not finish successfully." >&2
    exit 1
fi

# The benchmark library writes its results to the test app's media directory.
"$ADB" pull "/sdcard/Android/media/$TEST_ID/." "$OUT/device/"

baseline="$(find "$OUT/device" -name '*baseline-prof*.txt' | sort | head -n 1)"
if [ -z "$baseline" ]; then
    echo "No baseline profile was written by the generator." >&2
    exit 1
fi
cp "$baseline" "$OUT/baseline-prof.txt"

startup="$(find "$OUT/device" -name '*startup-prof*.txt' | sort | head -n 1)"
if [ -n "$startup" ]; then
    cp "$startup" "$OUT/startup-prof.txt"
fi

# A profile that does not name the app's own classes is useless (for example one recorded from
# a minified build): fail instead of handing it out.
own_rules="$(grep -c 'Lcom/fortresstv/player/' "$OUT/baseline-prof.txt" || true)"
total_rules="$(wc -l < "$OUT/baseline-prof.txt")"
echo "Baseline profile: $total_rules rules, $own_rules for com.fortresstv.player"
if [ "$own_rules" -lt 20 ]; then
    echo "The profile contains almost no FortressTV classes; not using it." >&2
    exit 1
fi
if ! grep -q 'Lcom/fortresstv/player/HomeScreenKt' "$OUT/baseline-prof.txt"; then
    echo "Note: Home is not in this profile (no source was configured on the device)."
fi
echo "Written to $OUT/baseline-prof.txt"
