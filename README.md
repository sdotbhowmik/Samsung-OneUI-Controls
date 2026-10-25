# One UI Controls

[![Latest release](https://img.shields.io/github/v/release/sdotbhowmik/Samsung-OneUI-Controls?style=flat-square)](https://github.com/sdotbhowmik/Samsung-OneUI-Controls/releases/latest)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)
[![Android 8+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)

**One UI 7-style volume and media controls for Samsung phones that are still on One UI 6.1** — built with a Galaxy A13 in mind.

Samsung's newest One UI has a redesigned volume panel and a big media card in the quick panel. This app brings that experience to older devices without root, without custom ROMs, and without touching system files.

> **Download:** [**OneUIControls-1.0.apk**](https://github.com/sdotbhowmik/Samsung-OneUI-Controls/releases/latest) (6.2 MB, Android 8.0+)

---

## Features

- **One UI-style control sheet** — dark translucent squircle panel, pill sliders, spring animation and haptics.
- **Quick panel tile** — add **Controls** to Samsung's quick panel; tap it to open the panel over any app.
- **Media card** — album art, title/artist, live seek bar, previous / play-pause / next, and a source-app shortcut.
- **Per-stream volume** — Media, Ringtone, Notifications, Calls, System and Alarm, with mute toggles and percentages.
- **Hardware volume keys** — optionally show this panel instead of Samsung's volume UI. Supports long-press Vol− to mute.
- **Notification shade card** — an ongoing One UI-styled card with artwork, transport buttons and media volume controls.
- **Themes** — system / dark / light, five accent colours, adjustable auto-dismiss and haptics.
- **No network** — the APK requests no internet permission and contains no analytics.

## Download & install

1. Download **[OneUIControls-1.0.apk](https://github.com/sdotbhowmik/Samsung-OneUI-Controls/releases/latest)** to the phone.
2. Tap the APK in **My Files** and allow **Install unknown apps** for the file manager.
3. Open **One UI Controls** and grant the permissions you want:
   - **Notification access** — read and control media sessions.
   - **Accessibility service** — let the volume keys open this panel (optional).
   - **Notifications** — show the shade card.
4. Tap **Add tile**, or open the quick panel, tap the edit/pencil icon, and drag **Controls** to the first page.

### Samsung / Android blockers

- **Auto Blocker (One UI 6.1)** may block the APK. Go to **Settings → Security and privacy → Auto Blocker** and turn it off while installing, then turn it back on.
- **"Restricted setting"** when enabling accessibility or notification access is Android's sideloading protection. Open **Settings → Apps → One UI Controls → ⋮ → Allow restricted settings**, then enable the service again. Installing over `adb` usually avoids this.

If the phone is connected over USB with debugging enabled:

```bash
adb install -r OneUIControls-1.0.apk
```

## How to use

- Tap the **Controls** tile to open the panel.
- Drag the volume pills; use the hardware keys while the panel is open.
- Tap a stream icon to mute or unmute that stream.
- **More streams** reveals calls, system and alarm volume.
- Tap the artwork to open the music app.
- Tap the output label (e.g. *Phone speaker*) to open Bluetooth settings.
- The panel closes after 3–10 seconds of inactivity (configurable), when you tap outside, or with Back.

## Permissions and privacy

| Permission | Why |
|---|---|
| **Notification access** | Required by Android to read media metadata and send play/pause/next/seek commands to other apps. |
| **Accessibility service** | Used only to observe the volume up/down keys and to draw the overlay panel. Can be switched off in the app. |
| **Post notifications** | Shows the ongoing media card in the notification shade. |

There is **no `INTERNET` permission** in the manifest. No accounts, analytics, ads or telemetry. All preferences are stored locally in `SharedPreferences`.

## What this app cannot do

Samsung's quick panel, volume panel and media player live in **SystemUI**, which is signed by Samsung. A sideloaded app cannot inject widgets into it, replace it, or add a card to the real quick panel without root or Samsung's keys. This app therefore uses the public Android building blocks:

- A **Quick Settings tile** (real, inside Samsung's quick panel) that opens the panel.
- An **accessibility overlay** when the volume keys are used.
- A **custom notification** for a card that actually lives in the notification shade.

Limitations that follow from this:

- The tile is a normal fixed-size tile; the sliders open in the panel sheet.
- While volume-key takeover is on, Samsung's double-press shortcuts (for example double-press Vol− for the camera) will not fire. Turn the toggle off to restore them instantly.
- Media control works with apps that publish a media session (Spotify, YouTube Music, Samsung Music, VLC, …). Apps with private audio playback cannot be controlled.
- Media output switching opens Bluetooth settings instead of the system output picker, which is not a public API.
- The panel is not part of Samsung's quick panel animation; it is a fast overlay that looks and behaves like it.

## Compatibility

- **Designed for:** Samsung Galaxy A13, One UI 6.1 (Android 14).
- **Minimum:** Android 8.0 (API 26).
- **Target:** Android 15 (API 35).
- Built with Kotlin, Jetpack Compose and Material 3.

## Build from source

Requirements: JDK 17, Android SDK platform 35 + build-tools 35. The Gradle wrapper is included.

```bash
git clone https://github.com/sdotbhowmik/Samsung-OneUI-Controls.git
cd Samsung-OneUI-Controls

# Debug APK (no signing setup needed)
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

For a signed release build, create a **`keystore.properties`** in the project root (it is git-ignored):

```properties
storeFile=keystore/my-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Then:

```bash
./gradlew :app:assembleRelease
```

Without `keystore.properties`, the release APK is built unsigned.

> **OneDrive note:** if the project is inside a OneDrive folder, Gradle writes outputs to
> `%USERPROFILE%\.virtualkeys-build` instead of `app/build` to avoid OneDrive file locks
> on Windows. This is handled automatically by the root build script.

## Project structure

```
app/src/main/java/com/virtualkeys/oneui/
├── MainActivity.kt                  settings, permission checklist, tile setup
├── VirtualKeysApp.kt                wires repositories and the shade card
├── Permissions.kt                   permission status helpers
├── audio/
│   ├── VolumeRepository.kt          per-stream volume via AudioManager
│   └── AudioOutput.kt               speaker / headphones / Bluetooth label
├── media/MediaRepository.kt         media sessions, metadata, transport, seek
├── service/
│   ├── VolumeKeyService.kt          accessibility volume-key filtering
│   ├── ControlTileService.kt        quick settings tile
│   └── MediaListenerService.kt      media session listener
├── notif/
│   ├── ShadeCardManager.kt          RemoteViews notification card
│   └── ShadeActionReceiver.kt       card button actions
├── overlay/PanelOverlay.kt          accessibility overlay host
├── ui/
│   ├── PanelScreen.kt               One UI-style panel (Compose)
│   ├── PanelActivity.kt             translucent panel host for the tile
│   ├── PanelInteraction.kt          auto-dismiss keep-alive
│   └── theme/Theme.kt               palette, accents, squircle shapes
└── res/                             icons, notification layouts, a11y config
```

## Troubleshooting

- **APK will not install** — turn off Samsung Auto Blocker temporarily; allow "Install unknown apps" for the file manager.
- **Accessibility/notification access shows "Restricted setting"** — Settings → Apps → One UI Controls → ⋮ → Allow restricted settings.
- **No media card** — check that Notification access is **On** and that a media app is actually playing; reopen the app after granting access.
- **Volume keys still open Samsung's panel** — enable the accessibility service, then toggle **Volume key panel** off and on. Make sure battery optimisation is not disabling the service.
- **Shade card is missing** — allow notifications for the app and turn on **Shade media card** in Controls.
- **Panel closes too quickly** — raise **Auto-dismiss** in the app.
- **Tile not in the quick panel** — swipe down twice, tap edit, and drag **Controls** out of the available tiles.

## FAQ

**Does it need root?**
No. It uses public Android APIs only.

**Will my Samsung volume panel come back?**
Yes. Turn off **Volume key panel** in the app, or disable the accessibility service. Samsung's UI is never modified.

**Can it control Spotify / YouTube Music / Samsung Music?**
Yes, through Android media sessions, as long as notification access is granted.

**Does it collect any data?**
No. There is no internet permission, no analytics and no account.

**Why does it need notification access?**
That is Android's gate for reading metadata and controlling playback in other apps' media sessions.

**Can it switch audio output?**
It shows the current output (speaker / headphones / Bluetooth). Tapping it opens Bluetooth settings; a true in-app output picker is not available to third-party apps.

## Contributing

Issues and pull requests are welcome. Please keep changes consistent with the existing Kotlin/Compose style and mention the device and One UI version you tested on. Run `./gradlew :app:lintDebug` before opening a PR.

## Credits

Built with [Jetpack Compose](https://developer.android.com/jetpack/compose), AndroidX MediaSession, `NotificationListenerService`, `AccessibilityService` and `TileService`.

## License

[MIT](LICENSE) © 2026 Subrata K. Bhowmik.

## Disclaimer

This is an independent personal project. It is **not affiliated with, endorsed by or sponsored by Samsung**. Samsung, Galaxy and One UI are trademarks of Samsung Electronics Co., Ltd. Use at your own risk; the app changes how your volume keys behave while enabled, and you can disable that behaviour at any time.
