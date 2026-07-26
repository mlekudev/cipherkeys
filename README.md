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
- Shift lock and symbol lock (long-press)
- Backspace repeat on long-press
- Interactive help system

## Releases

| Version | Date |
|---------|------|
| [v1.0.0](https://orly/cipherkeys-releases) | 2026-07-25 |

## Build

```bash
export ANDROID_HOME=~/Android/Sdk
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## License

Apache 2.0
