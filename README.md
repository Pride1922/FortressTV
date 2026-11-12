# FortressTV

FortressTV is an Android IPTV and media player for your own playlists. Watch live TV, browse movies and series, and pick up where you left off. The app includes no channels, films, playlists, or provider accounts. Bring a source you’re authorized to use.

[Download the latest APK](https://github.com/Pride1922/FortressTV/releases/latest/download/fortresstv.apk) · [Release notes](https://github.com/Pride1922/FortressTV/releases)

## What you can do

- Add M3U/M3U8 playlists or Xtream sources and use several sources together.
- Browse live channels with an XMLTV programme guide, save favourites, and organise your library.
- Browse movies and series, keep track of playback with Continue Watching, and choose your preferred audio and subtitle languages.
- Watch in fullscreen or use Picture-in-Picture from fullscreen playback.
- Connect TMDB and OMDb for metadata, and Trakt for watch tracking and recommendations.

FortressTV supports English and Portuguese (Portugal). It’s designed for Android phones and tablets. Android TV support is still being tested.

## Install and set up

1. On an Android device running **Android 8.0 or newer**, download and open `fortresstv.apk`. If prompted, allow APK installation from the app you used to open the file.
2. Open **Settings → Playlists** and add your playlist or Xtream source. Use **Everything** for a source that should appear in Live TV, Films, and Series. Use **Live TV** for a channel-only source.
3. If you have an XMLTV guide or want to connect metadata services, configure them in **Settings**.

## Build from source

Open the project in Android Studio. The GitHub Actions build uses **JDK 21**, **Android SDK 36.1**, and **Build Tools 36.0.0**.

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `.\gradlew.bat` in place of `./gradlew`. The debug APK is written to `app/build/outputs/apk/debug/`. Use the signed release APK when updating an existing release installation.

See [GitHub Actions and releases](GITHUB_ACTIONS.md), [performance and benchmarks](PERFORMANCE.md), and [third-party notices](THIRD_PARTY_NOTICES.md).
