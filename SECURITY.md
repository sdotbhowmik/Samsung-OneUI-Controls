# Security Policy

## Supported versions

| Version | Supported |
| ------- | --------- |
| 1.x     | ✅        |

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Use GitHub's
private vulnerability reporting:

1. Open the repository's **Security** tab.
2. Choose **Report a vulnerability**.
3. Describe the issue, the device/Android version and reproduction steps.

You can expect an acknowledgement within a few days. There is no bug bounty.

## Security model

- The app requests **no internet permission**. It cannot send data off the
  device.
- **Notification access** is used only to read active media sessions and send
  transport/seek commands. The app does not read or store notification content.
- The **accessibility service** is used only to filter the volume up/down keys
  and draw the control overlay. It does not read screen content or perform
  gestures on other apps.
- Preferences are stored locally in `SharedPreferences`.
- Release APKs are signed with a private key that is **not** committed to this
  repository. Builds from source (`assembleDebug`) use the standard Android
  debug key.
- The app does not request the "draw over other apps" permission; the overlay
  uses `TYPE_ACCESSIBILITY_OVERLAY` from the accessibility service.

## Verifying a release

```bash
apksigner verify --print-certs OneUIControls-1.0.apk
```

Every release asset is built from the tagged commit in this repository.
