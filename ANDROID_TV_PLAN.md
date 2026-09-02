# FortressTV Android TV plan

## Status

This document defines the implementation and test plan for making the existing FortressTV application a complete Android TV experience.

Android TV support will use the existing application rather than a separate companion app:

- Application ID: `com.fortresstv.player`
- Published APK: `fortresstv.apk`
- OTA metadata: `version.json`
- Signing key: unchanged
- Tablet support: preserved
- Android TV support: added to the same APK

The initial installation is performed by sideloading. Subsequent releases continue through the existing FortressTV OTA updater. Android TV support does not depend on Google Play distribution.

## Goals

FortressTV should:

1. Appear in the Android TV launcher with appropriate TV artwork.
2. Be completely usable with a D-pad remote and no touchscreen.
3. Preserve the existing tablet interface and touch interactions.
4. Keep Live TV, Movies, Series, Favorites, Settings, EPG, and playback accessible on TV.
5. Retain the original source, category, and channel order.
6. Preserve Media3 playback and the FFmpeg audio-decoder fallback.
7. Stop playback when the user closes the player or leaves the application.
8. Continue using the existing signed APK and OTA update process.
9. Allow a FortressTV phone or tablet to discover, pair with, configure, and control FortressTV on the TV.

## Non-goals

The initial Android TV implementation will not:

- Create a separate Android TV package or APK.
- Depend on the Google Play Store.
- Add background audio playback.
- Add concurrent or multi-view playback.
- Replace the existing tablet layouts wholesale.
- Add provider content, credentials, playlists, or EPG data.
- Automatically install an update without Android package-installer confirmation.

## Manifest and packaging

Android TV capabilities should be declared as optional so the same APK remains installable on tablets:

```xml
<uses-feature
    android:name="android.software.leanback"
    android:required="false" />

<uses-feature
    android:name="android.hardware.touchscreen"
    android:required="false" />
```

The launcher activity needs both ordinary and TV launcher categories:

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
    <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
</intent-filter>
```

If device testing shows that sharing one intent filter causes launcher-specific problems, use an activity alias for the TV launcher entry while retaining the same application and package.

The application must provide a landscape TV banner with readable branding at launcher distance. Launcher icons, the banner, and any user-visible descriptions must be suitable for both English and Portuguese (Portugal) installations.

## Runtime form-factor detection

Runtime behavior should be selected through a small, testable form-factor abstraction rather than scattered `uiMode` checks:

```kotlin
enum class AppFormFactor {
    HANDHELD,
    TELEVISION,
}
```

TV detection should primarily use `Configuration.UI_MODE_TYPE_TELEVISION`. Input capability can be considered separately because some TV devices have touch or pointer input and some non-TV devices accept D-pad events.

The application must never assume that:

- A TV has a touchscreen.
- A remote has numeric keys.
- A system or hardware Back button is always present.
- Every D-pad includes media transport buttons.
- All TV displays expose the same overscan or system-bar area.

## Navigation and focus

Every interactive element must be reachable, actionable, and escapable using only:

- D-pad Up, Down, Left, and Right
- D-pad Center/Enter
- Back

Optional remote keys should be supported where available:

- Play/Pause
- Stop
- Previous/Next
- Rewind/Fast-forward
- Channel Up/Down
- Numeric keys
- Menu/Settings

### Focus rules

- A visible focus indicator is mandatory; color alone should not be the only indicator.
- Focus must move predictably according to the visual layout.
- Opening a screen places focus on the most useful element, not an invisible container.
- Returning to a list restores the previously focused item and scroll position.
- Opening and closing sidebars, dialogs, filters, and menus restores focus to their trigger.
- Lazy lists and grids use stable keys so focus does not jump after background updates.
- Focus must not escape behind a modal dialog.
- There must be no dead ends requiring touch, swipe, or pointer input.
- Long-press behavior must have an ordinary button or menu alternative.

Explicit focus traversal should be added only where Compose's spatial focus search is ambiguous. Hard-coded focus chains that break when the layout changes should be avoided.

## Screen behavior

### Primary navigation

The shared navigation remains:

```text
Live TV | Movies | Series | Favorites | Settings
```

On TV, the collapsible sidebar must:

- Open and close using predictable Left/Right or Back behavior.
- Show the focused destination clearly.
- Retain focus and destination state after playback.
- Avoid forcing focus through every navigation item when moving between content rows.

### Live TV

- Categories and channels must be navigable independently using the D-pad.
- Selecting a channel starts playback without requiring a second touch-style action.
- Channel Up/Down changes channel during playback where the remote exposes those keys.
- Back follows a consistent order: dismiss controls, leave fullscreen/player, then return to navigation.
- The current channel and current programme remain visible where space permits.
- Rapid D-pad movement must not initiate uncontrolled logo or EPG work.

### Programme guide

- Fixed channel rows and programme rows must remain vertically aligned.
- D-pad Left/Right moves between programmes.
- D-pad Up/Down moves between channels.
- Selecting a programme shows details or plays the live channel, depending on whether the programme is currently airing.
- Day selection must be reachable without traversing the entire programme grid.
- Focused programmes scroll into view without destabilizing the synchronized vertical lists.

The existing programme-guide performance baseline must not regress. Focus changes should not force composition of the entire day.

### Movies and Series

- Poster grids must have clear focus scaling or borders without causing layout jumps.
- Returning from details or playback restores the focused poster and grid position.
- Search must be usable with the system TV keyboard.
- Movie, season, and episode actions must expose direct focused buttons.
- Watched state, progress, Watchlist, followed Series, and Next Unwatched Episode remain available.
- Episode lists continue to load lazily.

### Favorites

- Favorite channels, Movie Watchlist, and followed Series remain separate, understandable sections.
- Empty-state actions must be reachable with a remote.
- Removing an item must place focus on a logical neighboring item rather than losing focus.

### Settings

- Every toggle, selector, source editor, password visibility action, backup action, and update action must work with D-pad Center/Enter.
- Long forms must automatically scroll the focused field into view above the TV keyboard.
- Destructive operations retain confirmation dialogs.
- All new text ships in English and Portuguese (Portugal).

## Playback controls

The Android TV player should reuse the existing Media3 player and FFmpeg extension while presenting a remote-first controller.

Minimum actions:

- Play/Pause
- Stop/Close
- Previous/next channel for Live TV
- Seek backward/forward for VOD
- Timeline seeking for Movies and episodes
- Audio-track selection
- Subtitle-track selection when implemented
- Player information and sanitized error state

Recommended remote behavior:

| Input | Live TV | VOD/episode |
|---|---|---|
| Center/Enter | Show or hide controls | Show or hide controls |
| Play/Pause | Toggle playback | Toggle playback |
| Left/Right with controls visible | Select controls | Select controls or seek |
| Channel Up/Down | Previous/next channel | No action |
| Back | Controls, then player, then screen | Controls, then player, then screen |

Brightness and volume swipe gestures remain available on touch devices but must not be required on TV. Hardware/system volume continues to control TV volume.

Picture-in-Picture remains fullscreen-only on supported handheld devices. TV mode should not offer PiP unless a tested TV platform provides a meaningful, supported experience. Leaving the foreground without visible PiP must pause or stop playback according to the existing lifecycle rules.

## Initial installation

### ADB over USB or network

During development, install to an explicitly selected device:

```powershell
adb devices
adb -s <TV_SERIAL> install -r "C:\Users\fabio\Nextcloud\FortressTV\fortresstv.apk"
```

For a TV configured for wireless debugging:

```powershell
adb connect <TV_IP>:<PORT>
adb -s <TV_IP>:<PORT> install -r "C:\Users\fabio\Nextcloud\FortressTV\fortresstv.apk"
```

Never rely on an implicit ADB target because multiple Android devices may be connected.

### Without ADB

The signed APK may be transferred using:

- A USB drive and TV file manager
- A TV browser or downloader
- A local-network file-transfer tool

The user enables **Install unknown apps** only for the application that opens the APK, then confirms installation using the remote. Exact settings paths vary by TV manufacturer and Android version.

No playlist URL, credential, private EPG URL, signing secret, or provider data may be included in installation documentation or screenshots.

## OTA updates

The existing OTA contract remains unchanged:

1. FortressTV checks `version.json`.
2. It downloads `fortresstv.apk`.
3. Android verifies that the application ID and signing certificate match the installed app.
4. The system package installer asks the user to confirm the update.
5. The existing application data is retained.

The update prompt, download status, error messages, and package-installer handoff must be usable with a D-pad. The updater must not assume that a touch-capable browser is present.

The published APK must continue to overwrite the stable `fortresstv.apk` filename so its established share URL remains valid.

## FortressTV Connect

FortressTV Connect allows the existing phone or tablet application to communicate with FortressTV running on Android TV over the local network. The phone or tablet acts as a private remote and source manager; the TV remains the playback device and fetches media directly from the configured provider.

The primary actions are:

- Discover a nearby FortressTV television.
- Pair the two devices using a one-time code or QR code.
- Transfer an authorized source configuration securely.
- Select **Play on TV** / **Reproduzir na TV** for a channel, Movie, Series episode, or favorite.
- Control play/pause, seeking, channel changes, track selection, overlays, and playback closure from the phone or tablet.
- Transfer the current VOD resume position to the TV when playback is handed off.
- Disconnect or forget a paired device from either side.

### Connection model

The first implementation should use a local-network connection with no cloud relay:

```text
Phone or tablet                              Android TV
┌──────────────────────┐                     ┌──────────────────────┐
│ FortressTV           │   mDNS discovery    │ FortressTV           │
│                      │ ──────────────────▶ │                      │
│ Connect controller   │   authenticated     │ Connect receiver     │
│ Source configuration │ ◀═════════════════▶ │ TV playback session  │
│ Remote controls      │   encrypted link    │ Media3 player        │
└──────────────────────┘                     └──────────────────────┘
```

Local discovery should use Android Network Service Discovery (NSD/mDNS) or an equivalent LAN discovery mechanism. Manual IP entry may be offered as a fallback for networks that block multicast. Discovery must not transmit credentials, playlist URLs, viewing history, or item titles in service descriptions.

The TV should perform playback itself rather than receiving a continuous video stream from the tablet. This avoids double bandwidth, uses the TV's decoder directly, and allows the tablet to disconnect after handing off playback without interrupting the TV.

### Pairing and trust

Pairing must require approval on both devices. Someone with LAN access must not be able to control the TV or retrieve sources.

Recommended pairing flow:

1. The TV opens **Connect a device** / **Ligar um dispositivo** from Settings.
2. The TV displays a short-lived numeric code and, optionally, a QR code containing only connection bootstrap data.
3. The phone chooses the discovered TV and enters or scans the code.
4. The devices establish long-term identities and exchange public keys.
5. The TV displays the requesting device name and asks the user to allow or reject it.
6. Both devices store the paired identity in private application storage.

Pairing codes must expire, be rate-limited, and be invalidated immediately after success or cancellation. Removing a paired device revokes its credentials and terminates active control sessions.

The connection must authenticate both devices and encrypt commands and transferred configuration. Plain HTTP, unauthenticated WebSockets, permanent shared PINs, and trust based only on a local IP address are not acceptable.

### Source and playlist transfer

Sending a complete parsed playlist for every playback request would be slow, expose more private data than needed, and duplicate large catalogues in memory. FortressTV Connect should support two distinct operations:

#### Add source to TV

The user explicitly chooses **Send source to TV** / **Enviar fonte para a TV**. The tablet sends the minimum source configuration required for the TV to fetch and cache that source itself.

- The receiving TV shows the source name, type, and sending device before confirmation.
- Credentials and private URLs travel only through the authenticated encrypted connection.
- Credentials are encrypted again using the TV's existing Android KeyStore-backed storage before the transfer session is discarded.
- The TV assigns or reconciles a stable source ID without overwriting an unrelated source.
- Existing TV sources are not silently replaced.
- A failed transfer cannot delete the previous usable source or cache.
- Logs and error reports must redact URLs, usernames, passwords, tokens, headers, and provider responses containing credentials.

#### Play on TV

After the TV has the source, the tablet should normally send only:

- Protocol version
- Stable source ID
- Stable source-local item ID
- Content kind: Live channel, Movie, or episode
- Requested start or resume position when applicable
- A unique command ID for safe retry handling

The TV resolves the current stream information from its own source and cache. If the source is absent, the TV can return a structured `SOURCE_NOT_AVAILABLE` response and offer an explicit source-transfer flow. It must not silently request or copy all tablet data.

An optional one-time playback descriptor may be evaluated later for ephemeral playback. It must be encrypted, short-lived, kept out of persistent logs, and never used to bypass the normal secure source store.

### Remote controls and state

The controller may send:

- Play, pause, stop, and close
- Previous or next Live TV channel
- Seek to a VOD position
- Select an audio or subtitle track
- Open or close the EPG/player overlay
- Request the current sanitized playback state

The TV is authoritative for playback state. It returns bounded updates containing the current item identity, title suitable for display, playing/buffering/error state, duration, position, and available controls. It must not continuously send full catalogues or sensitive stream details as playback state.

Every command includes an identifier so reconnects and retries cannot accidentally execute the same toggle or channel change twice. The receiver validates commands against the current session and content type; for example, seek commands are rejected for a non-seekable live stream.

Remote control must not create invisible background playback. If the TV player is closed, the app leaves the permitted foreground playback state, or the TV session is terminated, playback stops according to the existing lifecycle rules.

### User experience and strings

At minimum, the same change must add English and Portuguese (Portugal) text for:

| English | Portuguese (Portugal) |
|---|---|
| Connect to TV | Ligar à TV |
| Play on TV | Reproduzir na TV |
| Send source to TV | Enviar fonte para a TV |
| Connect a device | Ligar um dispositivo |
| Paired devices | Dispositivos emparelhados |
| Forget device | Esquecer dispositivo |
| Connection code | Código de ligação |
| Waiting for approval | A aguardar aprovação |
| Source not available on TV | Fonte indisponível na TV |

The tablet must make the target TV unambiguous before sending a source or starting playback. The TV must show a non-intrusive confirmation when remote playback starts and retain local remote-control authority at all times.

### Compatibility and availability

- Both devices must be on a mutually reachable local network unless a future transport is explicitly designed and reviewed.
- Guest Wi-Fi and client-isolated networks may prevent discovery or connection.
- Android background restrictions may stop discovery when FortressTV is not visible; the first version should require the TV app to be open or visibly waiting for pairing.
- FortressTV Connect is versioned independently at the protocol level so newer apps can reject or downgrade unsupported commands cleanly.
- A Connect failure must never prevent ordinary on-device browsing or playback.
- The feature is disabled by default until the user opens the pairing flow.

## Implementation milestones

### Milestone 1: TV discovery and navigation audit

- Add optional Leanback and touchscreen manifest declarations.
- Add the TV launcher category and banner.
- Add centralized TV detection.
- Launch successfully on an Android TV emulator and one physical TV/device.
- Inventory every screen for unreachable or invisible focus.

Success means the application launches from the TV home screen and primary navigation is usable without touch.

### Milestone 2: Core focus system

- Define shared focus visuals for buttons, rows, cards, tabs, and text fields.
- Fix sidebar and top-level destination traversal.
- Restore focus and scroll position after navigation.
- Add Compose UI tests for essential focus routes.

Success means Live TV, Movies, Series, Favorites, and Settings can be entered and exited using only a remote.

### Milestone 3: Live TV and EPG

- Complete category/channel focus behavior.
- Add remote channel switching.
- Make fullscreen controls remote-first.
- Add programme-grid spatial navigation and reliable focus restoration.
- Validate large-playlist and EPG performance.

Success means a user can choose and watch a channel, browse the guide, switch channels, and close playback without touch.

### Milestone 4: Movies, Series, and search

- Complete poster-grid and details focus.
- Support the TV keyboard for search.
- Restore grid, series, season, and episode positions.
- Verify VOD seeking, resume progress, and watched state.

Success means a user can find and complete Movie and Series playback using only a remote.

### Milestone 5: Settings, source setup, and OTA

- Make source setup and editing remote-accessible.
- Verify password fields and the TV keyboard.
- Verify backup and restore flows.
- Make update prompts and errors D-pad accessible.
- Test an actual same-key OTA upgrade without publishing it.

Success means the application can be configured and updated without connecting a mouse or touchscreen.

### Milestone 6: FortressTV Connect discovery and pairing

- Add versioned local discovery and manual-address fallback.
- Implement expiring code/QR pairing and mutual device authentication.
- Add paired-device management on TV and handheld layouts.
- Add bilingual Connect screens and errors.
- Verify that an unpaired LAN client cannot issue commands or retrieve information.

Success means a phone or tablet can discover and securely pair with a TV, reconnect after application restart, and be revoked from either device.

### Milestone 7: Play on TV and remote control

- Send one existing source to the TV after explicit confirmation.
- Resolve a selected item by stable source and source-local identifiers.
- Start Live TV and VOD playback from **Play on TV**.
- Add playback state, play/pause, stop, seek, and channel controls.
- Preserve VOD resume position during handoff.
- Verify disconnect, duplicate-command, stale-session, and network-loss behavior.

Success means a user can select content on the tablet, play it on the TV, control it remotely, and disconnect without exposing credentials or leaving playback running invisibly.

## Test matrix

| Area | Required coverage |
|---|---|
| Device | Android TV emulator and explicitly selected physical TV/box |
| Resolution | 720p, 1080p, and 4K where available |
| Input | Basic D-pad, Back, media keys, optional numeric/channel keys |
| Launcher | Cold launch, relaunch, return from player, process restoration |
| Live TV | Categories, channel list, fullscreen, channel switching, errors |
| EPG | Open, vertical movement, horizontal movement, day changes, details |
| Movies | Grid, search, details, Watchlist, resume/restart, seeking |
| Series | Grid, seasons, lazy episodes, followed state, next episode |
| Settings | Forms, dialogs, source editing, backup/restore, About |
| Lifecycle | Home, app switching, sleep/wake, network loss, process death |
| OTA | Download, failure, confirmation, same-key upgrade, retained data |
| Connect discovery | Multicast discovery, manual IP fallback, isolated network failure |
| Connect security | Expired code, incorrect code, rate limiting, unpaired client, revoked device |
| Source transfer | Add, reject, duplicate source, interrupted transfer, credential redaction |
| Remote playback | Live, VOD resume, seek, channel change, duplicate command, disconnect |
| Compatibility | Matching protocol, older controller, older receiver, unsupported command |
| Performance | Startup, Live TV scrolling, EPG, Movie and Series grids |

## Verification

Before committing normal Android TV changes, run:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

UI changes must also be tested using a D-pad-only Android TV emulator session. Performance-sensitive changes must run the relevant macrobenchmarks on an explicitly selected physical device and record meaningful results in `PERFORMANCE.md`.

Before a TV-capable release, manually verify:

- The launcher banner and app entry render correctly.
- No primary workflow requires touch.
- Focus is always visible and restores logically.
- Back never traps the user or exits unexpectedly.
- Playback stops promptly when closed.
- Live TV and EPG responsiveness remain acceptable with a large catalogue.
- The package installer can be operated with the target remote.
- The release remains fully usable on the existing tablet target.
- An unpaired device cannot control playback or obtain source information.
- Pairing secrets, credentials, private URLs, and provider data do not appear in logs.
- Network loss and remote disconnection do not leave invisible playback running.

## First implementation boundary

The first implementation change should stop after Milestone 1 and the navigation audit. It must not publish an APK, alter OTA metadata, install to a device, or change the release version unless those actions are separately requested.
