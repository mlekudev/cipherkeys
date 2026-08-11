# CipherKeys Release Process

## Version Bump
1. Increment `projectVersionCode` and `projectVersionName` in `gradle.properties`
2. Build: `./gradlew assembleDebug`
3. Copy APK: `cp app/build/outputs/apk/debug/app-debug.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v<VERSION>.apk`

## Obtanium Release (cipherkeys-releases repo)
1. Update `latest.json` with new version and APK URL using array format:
   ```json
   [{"version":"<VERSION>","url":"https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v<VERSION>.apk"}]
   ```
2. Commit: `git add cipherkeys-v<VERSION>.apk latest.json && git commit -m 'v<VERSION>: ...'`
3. Tag: `git tag -f v<VERSION> HEAD`
4. Push: `git push && git push --tags`

## README Update (cipherkeys repo)
1. Add new version row to releases table in `README.md`
2. Commit and push: `git add README.md && git commit -m 'v<VERSION>: bump, update README' && git push`

## Obtanium User Setup
- Source type: JSON
- URL: `https://git.smesh.lol/cipherkeys-releases/raw/latest.json`
