# CipherKeys Release Process

## Versioning
Zero-padded monotonic counter. Increment `projectVersionCode` and set `projectVersionName` to 5-digit zero-padded version (e.g., `00019`). Lexicographically sortable.

## Version Bump
1. Increment `projectVersionCode` (e.g., 18 → 19) and `projectVersionName` (e.g., `00018` → `00019`) in `gradle.properties`
2. Build: `./gradlew assembleDebug`
3. Copy APK: `cp app/build/outputs/apk/debug/app-debug.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v00019.apk`

## README Update (cipherkeys repo)
1. Update the "Latest release" link at the top of `README.md` with new version
2. Add new version row to releases table
3. Commit and push: `git add README.md && git commit -m 'v<VERSION>: bump' && git push`

## Obtanium Release (cipherkeys-releases repo)
1. Copy APK: already done in version bump step
2. Commit, tag, push:
   ```
   git add cipherkeys-v<VERSION>.apk && git commit -m 'v<VERSION>: ...'
   git tag -f v<VERSION> HEAD
   git push && git push --tags
   ```

## Obtanium User Setup
- **Override Source**: HTML
- **URL**: `https://git.smesh.lol/cipherkeys`
- **App Name**: `CipherKeys`
- **Sort by Last Link Segment**: off
- **Reverse Sort**: on
