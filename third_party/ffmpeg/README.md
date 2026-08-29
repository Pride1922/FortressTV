# FortressTV FFmpeg decoder

FortressTV uses the official AndroidX Media3 FFmpeg audio-renderer extension as
a fallback when Android has no native decoder for a channel's audio format.
Normal supported formats continue to use Android's hardware decoders.

## Pinned sources

- AndroidX Media3: `1.10.1`
- Media3 commit: `5fb306449733dd71595700c1227ad6087578c559`
- FFmpeg branch: `release/6.0`
- FFmpeg commit: `3f92512fd1fd6f5e6d6eb45a156c352835314d69`
- Android NDK used to build FFmpeg: `26.1.10909125` (r26b)
- Android ABI level: `26`
- Enabled FFmpeg decoder: `mp3`
- Packaged ABIs: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`

Media3 maps `audio/mpeg-L1`, `audio/mpeg-L2` and `audio/mpeg` to FFmpeg's
decoder name `mp3`. This is why the build enables `mp3`, including for RTP 1's
MPEG Layer II audio.

## Rebuild

Install Ubuntu 24.04 under WSL2 and install the prerequisites:

```bash
sudo apt update
sudo apt install -y git curl unzip zip build-essential autoconf automake \
  libtool pkg-config cmake ninja-build openjdk-17-jdk-headless
```

The WSL Android SDK must be available at `$HOME/Android/Sdk` with Android
platform 36 and accepted SDK licences. Then, from Windows PowerShell in the
FortressTV project:

```powershell
.\scripts\Build-Media3Ffmpeg.ps1
```

The script verifies pinned commits, builds FFmpeg for all four Android ABIs,
packages Media3's decoder extension and replaces only:

`app/libs/media3-decoder-ffmpeg-1.10.1-mp3.aar`

## Licence and corresponding source

FFmpeg is licensed under LGPL v2.1 or later. The exact unmodified corresponding
source is included as `ffmpeg-6.0-source.tar.gz`. See `COPYING.LGPLv2.1` and
`build-config.txt` in this directory.
