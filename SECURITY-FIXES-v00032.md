# CipherKeys v00032 — Security Audit & Fixes

A third-party source review of v00030 surfaced 12 findings plus a handful of polish
items. Everything below is fixed in v00032, which also ships an encrypted
configuration backup/restore feature to smooth the signing-key migration.

> **TL;DR for existing users:** v00032 changes the APK signing key. Install
> v00031 first and use **Settings → Backup & Restore → Export** to save your
> keys, recipients, and settings, then install v00032 and **Import** them.
> v00031 download: `https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v00031.apk`

---

## What was already solid

- **Zero permissions** — no `INTERNET`, no networking code anywhere in the tree.
  For a keyboard this is the property that matters most, and it means none of the
  findings below were remotely exploitable.
- Crypto is delegated to **PGPainless** (not hand-rolled).
- Gradle wrapper pins a SHA-256 with `validateDistributionUrl=true`.
- `decryptInternal` checks `metadata.isEncrypted` before returning — a trap many
  PGP frontends fall into.

---

## High severity

### 1. Decryption never verified signatures — FIXED
`doDecrypt` returned only plaintext and never read `metadata.verifiedSignatures`,
so a message from anyone holding your public key was indistinguishable in the UI
from one sent by a trusted contact.
→ Decrypt now adds the sender's cert via `addVerificationCert()`, verifies the
signature, and surfaces the signer's identity (or an explicit "Message is not
signed" warning) in the panel.

### 2. Paste verification used the wrong key set — FIXED
`onPaste` built verification certs from the user's *own* keys instead of the
recipient list, so a message signed by a real correspondent could never verify,
and the banner was generic "Message valid".
→ Paste verification now uses recipient public keys and shows *whose* signature
it is.

### 3. Secret keys in plain SharedPreferences + `allowBackup="true"` — FIXED
Armored secret key rings were stored verbatim in plain prefs and Android's
backup (Seedvault / Google Drive / D2D) copied them off-device.
→ Key storage now uses **EncryptedSharedPreferences** (Android Keystore-backed
AES-256-GCM master key) with a one-time migration from the old plain store, and
`allowBackup="false"`.

### 4. Release APK signed with the Android debug keystore — FIXED
`storePassword = "android"` at a well-known path meant anyone with that public
keystore could ship an update Android accepts as the same app.
→ A dedicated release keystore (RSA-4096) with a real password is now used,
referenced out-of-repo via environment variables / `~/.gradle/gradle.properties`.

### 5. "Copy Secret" exported the private key in one tap — FIXED
No confirmation, no passphrase check, no sensitive-clipboard flag, sitting right
beside "Copy Public".
→ Now a confirmation dialog that *actually verifies the passphrase* (PGPainless
unlock), marks the clip `EXTRA_IS_SENSITIVE`, and clears it after 30 s.

### 6. The passphrase could be signed and committed into the target app — FIXED
`handleSend` was the only send path without a `pendingAction` guard, so a
passphrase could overwrite the saved message and get signed/committed as a
cleartext-signed block.
→ `handleSend` now early-dispatches through the same pending-action routing as
the encrypt/decrypt handlers.

---

## Medium severity

### 7. Plaintext logged in release builds — FIXED
`Log.*` calls interpolated decrypted plaintext and key IDs, and release wasn't
minified so all logs shipped.
→ Plaintext/key-ID interpolations removed from log statements.

### 8. Long-press-to-send silently dropped the signature — FIXED
`encryptAndSend(cachedPassphrase)` signed only when the cache was warm, so the
same message could be encrypted unsigned with no warning.
→ If a signing key is selected and the cache is cold, send now prompts for the
passphrase instead of silently proceeding unsigned.

### 9. A message merely *containing* the PGP armor header was sent in the clear — FIXED
The `isEncryptedMessage` check was a bare substring test, so quoted ciphertext
plus commentary passed as "already encrypted".
→ The check is anchored to the trimmed start and requires a matching
`-----END PGP MESSAGE-----`.

### 10. Import never verified the passphrase — FIXED
An unprotected key (or a mistyped passphrase) was accepted at import and only
failed later.
→ Import now attempts a real unlock with the supplied passphrase and rejects
keys with no S2K protection.

### 11. Passphrase cache had no timeout — FIXED
It cleared only on screen-off, surviving app/IME switches.
→ Added a 5-minute idle timeout in addition to the screen-off clear.

### 12. No `FLAG_SECURE` — FIXED
Key material and decrypted plaintext were screenshot- and recents-visible.
→ `FLAG_SECURE` set on the settings activity; decrypted-text copy is marked
sensitive on the clipboard.

---

## Low / polish

- `listKeys()` now fills in `created` and `algorithm` (was blank in the UI).
- Key generation requires a passphrase of at least 8 characters.
- Removed the unused `florisboard` git submodule.
- Fixed stale README APK links.
- (Documented, not changed) the v4 key-ID fold is correct for all key types this
  app generates/supports; it would only desync for v6 keys, which aren't used.

---

## New feature: encrypted configuration backup & restore

Because the signing key changes in this release, v00032 adds a way to move your
configuration:

- **Export** — Settings → Backup & Restore → Export. Encrypts all secret keys,
  recipients, and preferences (PGP-symmetric, password) into one file saved via
  the system file picker.
- **Import** — Settings → Backup & Restore → Import. Picks a file, prompts for
  the password, and restores everything (skipping duplicates).

---

## Signing key migration (one-time)

v00032 is signed with a new key, so existing installs can't update in place.

1. Install **v00031** (same signature as v00030, adds backup/restore) and
   **Export** your configuration.
2. Install **v00032** (uninstall/reinstall, then) and **Import** it.

Obtainium and Zapstore will offer v00032 directly; do the v00031 backup first if
you have existing keys you want to keep.
