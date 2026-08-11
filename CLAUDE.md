# CipherKeys Release Process

## Version Bump
1. Increment `projectVersionCode` and `projectVersionName` in `gradle.properties`
2. Build: `./gradlew assembleDebug`
3. Copy APK: `cp app/build/outputs/apk/debug/app-debug.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v<VERSION>.apk`

## README Update (cipherkeys repo)
1. Update the "Latest release" link at the top of `README.md` with new version
2. Update `index.html` with new version link:
   ```html
   <li><a href="https://git.smesh.lol/cipherkeys-releases/raw/cipherkeys-v<VERSION>.apk">CipherKeys v<VERSION></a></li>
   ```
3. Add new version row to releases table (text only, no links)
4. Commit and push: `git add README.md index.html && git commit -m 'v<VERSION>: bump' && git push`

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
- **Sort by Last Link Segment**: on
