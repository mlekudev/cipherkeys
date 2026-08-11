# CipherKeys Release Process

## Version Bump
1. Increment `projectVersionCode` and `projectVersionName` in `gradle.properties`
2. Build: `./gradlew assembleDebug`
3. Copy APK: `cp app/build/outputs/apk/debug/app-debug.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v<VERSION>.apk`

## Obtanium Release (cipherkeys-releases repo)
1. Copy APK: `cp app/build/outputs/apk/debug/app-debug.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v<VERSION>.apk`
2. Update `releases.html` with new version link at top of list:
   ```html
   <li><a href="https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v<VERSION>.apk">cipherkeys v<VERSION></a></li>
   ```
3. Commit, tag, push:
   ```
   git add cipherkeys-v<VERSION>.apk releases.html && git commit -m 'v<VERSION>: ...'
   git tag -f v<VERSION> HEAD
   git push && git push --tags
   ```

## README Update (cipherkeys repo)
1. Add new version row to releases table in `README.md`
2. Commit and push: `git add README.md && git commit -m 'v<VERSION>: bump, update README' && git push`

## Obtanium User Setup
- **Override Source**: HTML
- **URL**: `https://git.smesh.lol/cipherkeys-releases/raw/releases.html`
- **Sort by Last Link Segment**: on
