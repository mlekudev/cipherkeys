# CipherKeys — security review write-up

Source review of `mlekudev/cipherkeys` @ `f677166` (v00030), done 2026-08-23.
Full findings live here; the GitHub issue body below is meant to be pasted as-is.

---

## Suggested issue title

`Security review: sender authentication, key-at-rest, and release signing`

## Issue body (copy from here down)

Hi — I've been running CipherKeys and went through the source at `f677166` (v00030).
Writing up what I found. Happy to split this into separate issues if you'd rather track
them individually; I've grouped them so that's easy to do.

**Up front, the thing I think you got most right:** the manifest declares *zero*
permissions — no `INTERNET`, and there's no networking code anywhere in the tree. For a
keyboard that is the property that matters most, and it means none of what follows is
remotely exploitable. Crypto is delegated to PGPainless rather than hand-rolled, the
Gradle wrapper pins a SHA-256 with `validateDistributionUrl=true`, and
`PgpEngine.decryptInternal` checks `metadata.isEncrypted` before returning — that last one
is a trap a lot of PGP frontends fall into, and you avoided it.

The findings below are mostly about *sender authentication* and *key handling*, not about
the encryption itself.

---

### High

**1. Decryption never verifies signatures**

`CipherIME.doDecrypt` (`app/.../ime/CipherIME.kt:551`) calls `PgpEngine.decryptTryAll`,
which returns only the plaintext `String` — `metadata.verifiedSignatures` is never read
(`cipher/.../PgpEngine.kt:112-125`, `326-349`).

The app *creates* inline signatures when encrypting, but nothing checks them on receipt.
Since a public key is public by design, anyone holding mine can encrypt a message to me
that decrypts cleanly and is indistinguishable in the UI from one sent by a trusted
contact.

*Suggested fix:* have `decryptTryAll` return the plaintext plus the verification result,
add the sender's cert via `ConsumerOptions.addVerificationCert()`, and surface signer
identity (or an explicit "unsigned") in the panel after decrypt.

**2. Signature verification on paste checks against the wrong key set**

`onPaste` builds its verification certs from `keyManager.listKeys()` — the user's *own*
keys — rather than from `RecipientManager` (`CipherIME.kt:213`).

So a message signed by an actual correspondent can never verify; it always falls to the
`"Invalid signature"` branch at `:234`. Only messages the user signed themselves show
`"Message valid"`. In practice this trains users to treat the warning banner as noise,
which removes whatever value it had.

Related: when it does succeed, the banner is just `"Message valid"` with no signer
identity (`:226`). A signature that doesn't say *whose* it is doesn't authenticate
anything — with a fix to the key set, any known recipient could sign a message that
displays as generically "valid".

*Suggested fix:* verify against recipient certs, and put the matched user ID in the banner.

**3. Secret keys in plain SharedPreferences + `allowBackup="true"`**

`KeyStore.storeSecretKey` writes the armored secret key ring verbatim into
`SharedPreferences` (`cipher/.../KeyStore.kt:19-37`), and the manifest sets
`android:allowBackup="true"` (`app/src/main/AndroidManifest.xml:5`) with no
`dataExtractionRules` or `fullBackupContent`.

That means Android's backup system — Seedvault on GrapheneOS, Google Drive backup on
stock, plus D2D transfer — copies private keys off the device. They're OpenPGP
passphrase-encrypted (but see #6), so this is an offline-crackable copy rather than an
immediate compromise, but it's a copy the user didn't choose to make.

*Suggested fix:* `android:allowBackup="false"`, or at minimum
`android:dataExtractionRules` excluding `cipherkeys_keystore`. Wrapping key storage in
`EncryptedSharedPreferences` with an Android Keystore-backed master key would additionally
bind the material to the device.

**4. Release APK is signed with the Android debug keystore**

`app/build.gradle.kts:54-63` — `~/.android/debug.keystore`, `storePassword = "android"`,
`keyAlias = "androiddebugkey"`.

The signing key's password is public and the file lives at a well-known path. Anyone who
obtains that file can ship an update Android will accept as the same application — and
since the README recommends Obtainium, that key is the trust anchor for auto-updates on
every install.

*Suggested fix:* a dedicated release keystore with a real password, kept out of the repo
and off shared machines, referenced via env vars or `~/.gradle/gradle.properties`.

**5. "Copy Secret" exports the private key with one tap and no check**

`KeyListScreen.kt:167-176` calls `keyManager.exportSecretKey(key.keyId, "")` and puts the
result straight on the system clipboard. `KeyManager.exportSecretKey`
(`cipher/.../KeyManager.kt:58-62`) accepts a `passphrase` parameter and never uses it, so
the passphrase-shaped argument is decorative. There's no confirmation dialog, and the
button sits directly beside "Copy Public".

No `ClipDescription.EXTRA_IS_SENSITIVE` is set either, so on Android 13+ the clipboard
preview renders the key material and it enters clipboard history.

*Suggested fix:* confirmation dialog, require and actually verify the passphrase, set
`EXTRA_IS_SENSITIVE`, and consider clearing the clip after a timeout.

**6. The passphrase can be signed and committed into the target app**

`handleSend` (`CipherIME.kt:636-662`) is the only send path with no `pendingAction` guard
— compare `onEncryptAction:405` and `onDecryptAction:449`, which both check it.

While the passphrase prompt is open, `composeText` holds the passphrase. Tapping Send
re-enters `CipherUiState.requestPassphrase`, which does
`savedComposeText = state.composeText` — overwriting the saved message with the
passphrase. The subsequent sign then signs *the passphrase* and `commitText`s it into the
target app's input field as a cleartext-signed block. The original message is lost too.

Repro:
1. Select a signing key, no recipients, `cachedPassphrase` cold (fresh after screen-off)
2. Type a message, tap **Send** → passphrase prompt appears
3. Type the passphrase, tap **Send** again (rather than Enter)
4. Type the passphrase again, sign → the first passphrase is signed and committed into the
   app you were typing in

*Suggested fix:* early-return in `handleSend` when `state.pendingAction != null`, routing
to the same dispatch the other two handlers use.

---

### Medium

**7. Decrypted plaintext logged in release builds**

`CipherIME.kt:222` logs `result.plaintext.take(100)`; `:230` logs the *entire* extracted
plaintext (`"onPaste: not verified, extracted=$extracted"`); `:556` logs stored key IDs.
`isMinifyEnabled = false` (`app/build.gradle.kts:49`) with no log-stripping ProGuard rule,
so all 26 `Log.*` calls ship in release. Not readable by other apps without `READ_LOGS`,
but adb and bug reports capture it.

*Suggested fix:* drop the plaintext interpolations, and/or strip `Log.d`/`Log.v` in release
via R8.

**8. Long-press-to-send silently drops the signature**

`encryptAndSend(cachedPassphrase)` (`CipherIME.kt:356`, `639`, `648`) passes a possibly-null
passphrase. `PgpEngine.encryptArmored:68` only signs `if (signKey != null && passphrase != null)`,
so with a cold cache the message is encrypted **unsigned**, with no warning. The tap-Encrypt
path at `:439` correctly prompts first. Same message, different signing behaviour depending
on which control was used and whether the cache happened to be warm.

*Suggested fix:* mirror the `:439` check — if a signing key is selected and the cache is
cold, request the passphrase rather than silently proceeding.

**9. A message merely containing `-----BEGIN PGP MESSAGE-----` is sent in the clear**

`isEncryptedMessage` is a substring test (`CipherIME.kt:729-731`). In `encryptAndSend:496`
a positive match commits the text as-is and fires the editor action. So quoting a
ciphertext block and adding plaintext commentary sends the whole thing unencrypted, while
the UI reports it as already encrypted.

*Suggested fix:* anchor the check to the start of the trimmed text, and require a matching
`-----END PGP MESSAGE-----` with nothing outside the block.

**10. Nothing verifies an imported key is passphrase-protected, or that the passphrase is right**

`KeyStore.storeSecretKey:17` rejects an empty passphrase and then never uses the value
again. An unprotected secret key imported with any non-empty text in the field is stored as
cleartext private key material — which #3 then backs up. A mistyped passphrase also
"succeeds" at import and only fails later at first use.

*Suggested fix:* attempt an unlock with the supplied passphrase before storing, and reject
(or loudly warn about) keys with no S2K protection.

**11. Passphrase cache has no timeout and can't be zeroed**

`cachedPassphrase` (`CipherIME.kt:55`) is cleared only on `ACTION_SCREEN_OFF` (`:397-401`).
It survives app switches and IME switches for as long as the screen stays on, so brief
physical access to an unlocked phone decrypts everything without knowing it. It's also an
immutable `String`, as is `CipherUiState.composeText` (a process-lifetime singleton), so
neither can be wiped and both linger in the heap until GC.

*Suggested fix:* an idle timeout in addition to screen-off; `CharArray`/`ByteArray` cleared
after use where practical.

**12. No `FLAG_SECURE`**

Nothing in the tree sets it. The settings screens display key material and the compose
panel displays decrypted plaintext, but both are screenshot- and recents-thumbnail-visible.
`onCopy` (`CipherIME.kt:195-203`) also copies decrypted text without `EXTRA_IS_SENSITIVE`.

---

### Low / polish

- **`KeyStore.kt:24`** — folding the fingerprint into a `Long` via
  `fold(0L) { acc, b -> (acc shl 8) or ... }` keeps only the trailing 8 bytes. That equals
  the key ID for v4 keys, which is why it works, but it would desync store/delete for v6
  (32-byte) fingerprints.
- **`KeyManager.kt:79-88`** — `listKeys()` hardcodes `created = 0L` and `algorithm = ""`,
  so `KeyCard` renders a blank algorithm (`KeyListScreen.kt:204`).
- **`KeyGenScreen.kt:43`** — no minimum passphrase length; a single character is accepted.
- **`.gitmodules`** declares the `florisboard` submodule but it's empty and absent from
  `settings.gradle.kts` — nothing builds it.
- **README** still links v1.0.0–00019 APKs at `git.smesh.lol`.

---

### Scope note

This is a source review only. I couldn't verify that the APK I installed was built from
this source — there's no reproducible build, and with the debug signing key in #4 there's
nothing binding the two.

---

## If splitting into separate issues

| Issue | Findings |
|---|---|
| Sender authentication is not enforced | 1, 2, 8 |
| Key material at rest and on export | 3, 5, 10 |
| Release signing key | 4 |
| Passphrase handling | 6, 11 |
| Plaintext leaks (log, clipboard, screen) | 7, 12 |
| Encrypted-message detection | 9 |
