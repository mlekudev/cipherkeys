# CipherKeys

Android keyboard with PGP encryption.

After installing, enable in **Settings > System > Keyboard > On-screen keyboard > CipherKeys**.

**Latest release**: [cipherkeys 00019](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v00019.apk)

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
| v1.0.0 | 2026-07-25 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.0.apk) |
| v1.0.1 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.1.apk) |
| v1.0.2 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.2.apk) |
| v1.0.3 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.3.apk) |
| v1.0.4 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.4.apk) |
| v1.0.5 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.5.apk) |
| v1.0.6 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.6.apk) |
| v1.0.7 | 2026-07-26 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.7.apk) |
| v1.0.8 | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.8.apk) |
| v1.0.10 | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.10.apk) |
| v1.0.11 | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.11.apk) |
| v1.0.12 | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.12.apk) |
| v1.0.13 | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.13.apk) |
| v1.0.14 | 2026-08-09 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.14.apk) |
| v1.0.15 | 2026-08-11 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.15.apk) |
| v1.0.16 | 2026-08-11 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.16.apk) |
| v1.0.17 | 2026-08-11 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.17.apk) |
| v1.0.18 | 2026-08-11 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v1.0.18.apk) |
| 00019 | 2026-08-11 | [APK](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v00019.apk) |

## Updating with Obtanium

Add `https://github.com/mlekudev/cipherkeys-releases` in Obtanium. Zero configuration - autodetects via GitHub releases.

## Build

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## License

Apache 2.0
