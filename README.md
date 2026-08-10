# CipherKeys

Android keyboard with PGP encryption.

## Features

- Full QWERTY keyboard with dark/light theme support
- PGP encryption and decryption directly from the keyboard
- Key generation (Ed25519/Curve25519 and RSA-4096)
- Recipient management with import/export
- Passphrase caching (cleared on device lock)
- Inline compose panel with blinking cursor, tap-to-position, and auto-scroll
- Encrypt-and-send via long-press on enter key
- Long-press number keys on top row
- Haptic and sound feedback on key press (configurable)
- Key press popover showing pressed character above the key
- Shift lock and symbol lock (double-tap or long-press, configurable)
- Auto-capitalize after sentence-ending punctuation
- Configurable key height, key background shading
- Cleartext PGP sign with optional message expiration
- Encrypt-to-self option (auto-add your key to all encryptions)
- Inline recipient picker when encrypting without recipients selected
- Send and long-press Enter encrypt-and-send to app input
- Signature verification on paste with valid/invalid/expired banners
- Passphrase input auto-resets keyboard to lowercase
- Backspace repeat on long-press
- Interactive help system

## Releases

| Version | Date | Download |
|---------|------|----------|
| [v1.0.0](https://git.smesh.lol/cipherkeys-releases) | 2026-07-25 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.0.apk) |
| [v1.0.1](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.1.apk) |
| [v1.0.2](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.2.apk) |
| [v1.0.3](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.3.apk) |
| [v1.0.4](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.4.apk) |
| [v1.0.5](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.5.apk) |
| [v1.0.6](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.6.apk) |
| [v1.0.7](https://git.smesh.lol/cipherkeys-releases) | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.7.apk) |
| [v1.0.8](https://git.smesh.lol/cipherkeys-releases) | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.8.apk) |
| [v1.0.10](https://git.smesh.lol/cipherkeys-releases) | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.10.apk) |
| [v1.0.11](https://git.smesh.lol/cipherkeys-releases) | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.11.apk) |
| [v1.0.12](https://git.smesh.lol/cipherkeys-releases) | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.12.apk) |
| [v1.0.13](https://git.smesh.lol/cipherkeys-releases) | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.13.apk) |
| [v1.0.14](https://git.smesh.lol/cipherkeys-releases) | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/app-v1.0.14.apk) |

## Updating with Obtanium

Use [Obtanium](https://github.com/ImranR98/Obtainium) to get automatic updates:

- **Source type**: JSON
- **URL**: `https://git.smesh.lol/cipherkeys-releases/raw/latest.json`

Obtanium will check this URL for version updates and download the latest APK automatically.

## Build

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## License

Apache 2.0
