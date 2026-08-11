# CipherKeys Release Process

## Version Bump
1. Increment `projectVersionCode` and `projectVersionName` in `gradle.properties`
2. Build: `./gradlew assembleDebug`
3. Copy APK: `cp app/build/outputs/apk/debug/app-debug.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v<VERSION>.apk`

## README Update (cipherkeys repo)
1. Update the "Latest release" link at the top of `README.md` with new version:
   ```markdown
   **Latest release**: [cipherkeys v<VERSION>](https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v<VERSION>.apk)
   ```
2. Add new version row to releases table (text only, no links)
3. Commit and push: `git add README.md && git commit -m 'v<VERSION>: bump, update README' && git push`

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
- **URL**: `https://git.smesh.lol/cipherkeys/raw/README.md`
- **App Name**: `CipherKeys`
- **Sort by Last Link Segment**: on
