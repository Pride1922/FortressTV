<p align="center">
  <img src="app/src/main/res/drawable-nodpi/fortress_tv_logo.png" alt="FortressTV shield logo" width="160">
</p>

<h1 align="center">FortressTV</h1>

<p align="center">
  <strong>Your sources. Your library. Your player.</strong><br>
  Live TV, movies, and series together on Android.
</p>

<p align="center">
  <a href="https://github.com/Pride1922/FortressTV/releases/latest/download/fortresstv.apk"><strong>Download APK</strong></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/Pride1922/FortressTV/releases">Releases</a>
  &nbsp;·&nbsp;
  <a href="ROADMAP.md">Roadmap</a>
  &nbsp;·&nbsp;
  <a href="https://github.com/Pride1922/FortressTV/issues">Report an issue</a>
</p>

<p align="center">
  Android 8.0+ &nbsp;|&nbsp; Phones, tablets &amp; Android TV &nbsp;|&nbsp; English &amp; Português (Portugal)
</p>

---

FortressTV brings your own IPTV and media sources into one organised library. Watch live channels with a programme guide, browse films and series, and pick up where you left off across your devices with optional account sync.

**FortressTV is a player, not a content service.** It includes no channels, films, playlists, provider accounts, or EPG feeds. Add sources you are authorised to access.

## One home for your viewing

- **Live TV with a guide.** Browse channels, see what is on now and next, and explore an XMLTV programme guide with automatic and manual channel matching.
- **Movies and series.** Browse poster grids, search and sort your catalogues, open title details, and load episodes when you need them.
- **Pick up where you left off.** Resume movies and episodes, track watched titles, and move to the next episode with optional autoplay.
- **Make it yours.** Save favourite channels, build a movie watchlist, follow series, and customise Home shelves and category visibility/order.
- **Use several sources together.** Combine M3U/M3U8 playlists and Xtream sources, with independent refreshes and caches.
- **Choose a version.** Select a matching movie version before playback, or choose an alternative live channel from fullscreen controls. Your choice is remembered locally per profile.
- **Find something quickly.** Search across channels, movies, and series from Home, or return to a recent channel.
- **Find a setting.** Search Settings in English or Portuguese and jump to the relevant control or group.

## Playback that fits the moment

| Feature | What you can do |
|---|---|
| Fullscreen and Picture-in-Picture | Watch fullscreen, then use PiP on supported devices. PiP is available from fullscreen playback only. |
| Audio and subtitles | Select tracks, set preferred languages, and optionally search for subtitles through OpenSubtitles. Adjust supported VOD subtitle timing in 100 ms steps, saved per title and profile. |
| Sleep timer | Stop playback after a timer or at supported programme/episode boundaries. |
| Catch-up | Watch archived programmes when your configured source supports them. Availability and retention depend on the source. |
| Quality and resilience | Choose available video quality options, return to the live edge, and adjust playback resilience settings. |
| Connection health | Inspect startup and buffering information to understand playback problems. |

Playback uses **AndroidX Media3**, with an **FFmpeg audio-decoder extension** for additional audio compatibility. Quality choices and available tracks depend on the stream.

## Optional extras

Connect only the services you want to use. Core source browsing and playback do not require metadata or discovery integrations.

| Integration | Adds |
|---|---|
| TMDB | Artwork, descriptions, cast, genres, and trailer references where available. |
| OMDb | Additional title information and ratings where available. |
| TheIntroDB | Intro, recap, and credits skip markers for supported titles. |
| Trakt | Watch tracking, watchlist/history synchronisation, recommendations, and popular titles. |
| OpenSubtitles | Online subtitle search and downloads. |
| Household account | Profiles, parental controls, encrypted source backup/restore, and library/playback-progress synchronisation. |

External services may require your own account or API credentials. Recommendations do not provide playable media: watching a title requires an available item in one of your sources. Cloud features require the account backend; see [account setup](supabase/ACCOUNT_SETUP.md) for your own builds.

## Get started

1. **Download** [the latest APK](https://github.com/Pride1922/FortressTV/releases/latest/download/fortresstv.apk) on a device running Android 8.0 or newer.
2. **Install** `fortresstv.apk`. If Android asks, allow APK installation from the app you used to open the file.
3. **Add a source** under **Settings → Playlists**: a remote/local M3U playlist or an Xtream source. Select **Everything** for a source covering Live TV, movies, and series, or choose a narrower role.
4. **Add your guide** under **Settings → EPG** if you have an XMLTV feed.
5. **Start watching.** Metadata services, online subtitles, and account sync can be configured later in Settings.

Use the signed release APK when updating an existing release installation. The app also includes an update checker; Android asks you to confirm installation.

## Android TV

FortressTV uses the same APK on phones, tablets, and Android TV. The app includes a TV launcher entry, focus handling, remote playback keys, and local source transfer so you can send source settings to a TV and confirm the import there.

**Android TV support is still being validated.** Complete D-pad workflows and device compatibility remain ongoing work. Source transfer is available; Play on TV, playback handoff, and a phone-based TV remote remain planned. See the [Android TV and Connect plan](ANDROID_TV_PLAN.md).

## Privacy and source control

- Source credentials use Android KeyStore-backed encrypted storage.
- Source caches are kept separate from user-library data so refreshes can preserve favourites and progress.
- Multiple enabled sources retain their own identity, configuration, and refresh state.
- Cloud sync and external integrations are optional. Enabling them involves communication with the corresponding services.
- Playback outside the app requires visible PiP; invisible background audio is not a supported mode.

When reporting a problem, remove playlist URLs, usernames, passwords, tokens, private guide links, and provider data from logs and screenshots.

## Build from source

Open the project in Android Studio. The GitHub Actions build uses **JDK 21**, **Android SDK 36.1**, and **Build Tools 36.0.0**.

```sh
./gradlew :app:assembleDebug
```

Run the standard application checks:

```sh
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `.\gradlew.bat` in place of `./gradlew`. The debug APK is written to `app/build/outputs/apk/debug/`. Keep signing credentials and private service configuration out of Git.

See [GitHub Actions and releases](GITHUB_ACTIONS.md) for build and signing details, and [AGENTS.md](AGENTS.md) for contribution rules and verification requirements.

## Explore the project

| Documentation | Contents |
|---|---|
| [Product roadmap](ROADMAP.md) | Implemented capabilities, remaining work, and ideas awaiting a decision. |
| [Design roadmap](DESIGN_ROADMAP.md) | UI direction and remaining interaction refinements. |
| [Android TV plan](ANDROID_TV_PLAN.md) | TV validation and planned Connect features. |
| [Performance](PERFORMANCE.md) | Measurements and benchmark procedures. |
| [Account setup](supabase/ACCOUNT_SETUP.md) | Backend configuration for account-enabled builds. |
| [Third-party notices](THIRD_PARTY_NOTICES.md) | Dependency attribution and licensing information, including FFmpeg. |

Found a bug or have a focused feature idea? [Open an issue](https://github.com/Pride1922/FortressTV/issues) with your device model, Android version, app version, and steps to reproduce it, without sharing private source details.
