# Changelog

All notable changes to this project are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
this project uses [Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-10-25

Initial public release.

### Added

- One UI-style translucent control sheet with pill volume sliders.
- Quick Settings **Controls** tile that opens the panel over any app.
- Media card with album art, title/artist, seek bar, previous / play-pause /
  next and source-app shortcut.
- Volume control for Media, Ringtone, Notifications, Calls, System and Alarm,
  with mute toggles and percentage readouts.
- Optional hardware volume-key takeover through an accessibility service,
  including long-press Vol− to mute and an instant on/off switch.
- Ongoing notification-shade media card with artwork, transport buttons and
  media volume controls.
- Dark / light / system themes, five accent colours, haptics and configurable
  auto-dismiss.
- Permission onboarding and quick-panel tile setup inside the app.
- Live media session support via `NotificationListenerService` and
  `MediaSessionManager`.
- Output-device label (speaker / headphones / Bluetooth) with a shortcut to
  Bluetooth settings.

### Notes

- No root, no internet permission, no analytics.
- Samsung's SystemUI cannot host third-party widgets; the tile + overlay +
  notification card are the supported alternative.
- Built for the Galaxy A13 on One UI 6.1 (Android 14), with a minimum of
  Android 8.0.
