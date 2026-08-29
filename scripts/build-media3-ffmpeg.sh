#!/usr/bin/env bash
set -euo pipefail

MEDIA3_VERSION="1.10.1"
MEDIA3_COMMIT="5fb306449733dd71595700c1227ad6087578c559"
FFMPEG_COMMIT="3f92512fd1fd6f5e6d6eb45a156c352835314d69"
NDK_ARCHIVE="android-ndk-r26b-linux.zip"
NDK_DIRECTORY="android-ndk-r26b"
NDK_URL="https://dl.google.com/android/repository/${NDK_ARCHIVE}"
ANDROID_ABI_LEVEL="26"
ENABLED_DECODER="mp3"

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"
CODEC_ROOT="${FORTRESSTV_CODEC_HOME:-${HOME}/fortresstv-codec}"
MEDIA_ROOT="${CODEC_ROOT}/media"
FFMPEG_MODULE="${MEDIA_ROOT}/libraries/decoder_ffmpeg/src/main"
FFMPEG_ROOT="${FFMPEG_MODULE}/jni/ffmpeg"
NDK_ROOT="${CODEC_ROOT}/${NDK_DIRECTORY}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${HOME}/Android/Sdk}"
OUTPUT_AAR="${PROJECT_ROOT}/app/libs/media3-decoder-ffmpeg-1.10.1-mp3.aar"

for command_name in git curl unzip make cmake ninja java; do
  command -v "${command_name}" >/dev/null 2>&1 || {
    echo "Missing required command: ${command_name}" >&2
    exit 1
  }
done

if [[ ! -d "${ANDROID_SDK_ROOT}/platforms/android-36" ]]; then
  echo "Android SDK platform 36 is missing from ${ANDROID_SDK_ROOT}." >&2
  exit 1
fi

mkdir -p "${CODEC_ROOT}" "${PROJECT_ROOT}/app/libs"

if [[ ! -d "${MEDIA_ROOT}/.git" ]]; then
  git clone --branch "${MEDIA3_VERSION}" --depth 1 \
    https://github.com/androidx/media.git "${MEDIA_ROOT}"
fi

actual_media3_commit="$(git -C "${MEDIA_ROOT}" rev-parse HEAD)"
if [[ "${actual_media3_commit}" != "${MEDIA3_COMMIT}" ]]; then
  echo "Media3 checkout mismatch: ${actual_media3_commit}" >&2
  echo "Expected: ${MEDIA3_COMMIT}" >&2
  exit 1
fi

if [[ ! -d "${NDK_ROOT}" ]]; then
  curl -fL --retry 3 -o "${CODEC_ROOT}/${NDK_ARCHIVE}" "${NDK_URL}"
  unzip -q "${CODEC_ROOT}/${NDK_ARCHIVE}" -d "${CODEC_ROOT}"
fi

if [[ ! -d "${FFMPEG_ROOT}/.git" ]]; then
  git clone --branch release/6.0 --depth 1 \
    https://git.ffmpeg.org/ffmpeg.git "${FFMPEG_ROOT}"
fi

actual_ffmpeg_commit="$(git -C "${FFMPEG_ROOT}" rev-parse HEAD)"
if [[ "${actual_ffmpeg_commit}" != "${FFMPEG_COMMIT}" ]]; then
  echo "FFmpeg checkout mismatch: ${actual_ffmpeg_commit}" >&2
  echo "Expected: ${FFMPEG_COMMIT}" >&2
  exit 1
fi

(
  cd "${FFMPEG_MODULE}/jni"
  ./build_ffmpeg.sh \
    "${FFMPEG_MODULE}" \
    "${NDK_ROOT}" \
    linux-x86_64 \
    "${ANDROID_ABI_LEVEL}" \
    "${ENABLED_DECODER}"
)

(
  cd "${MEDIA_ROOT}"
  ANDROID_HOME="${ANDROID_SDK_ROOT}" \
  ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT}" \
    ./gradlew lib-decoder-ffmpeg:clean lib-decoder-ffmpeg:assembleRelease
)

SOURCE_AAR="${MEDIA_ROOT}/libraries/decoder_ffmpeg/buildout/outputs/aar/lib-decoder-ffmpeg-release.aar"
install -m 0644 "${SOURCE_AAR}" "${OUTPUT_AAR}"

echo "Built ${OUTPUT_AAR}"
sha256sum "${OUTPUT_AAR}"
