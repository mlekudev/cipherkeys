# CipherKeys Release Process

## Versioning
Zero-padded monotonic counter. Increment `projectVersionCode` and set `projectVersionName` to 5-digit zero-padded version (e.g., `00019`). Lexicographically sortable.

## Version Bump
1. Increment `projectVersionCode` (e.g., 18 → 19) and `projectVersionName` (e.g., `00018` → `00019`) in `gradle.properties`
2. Build: `./gradlew assembleRelease`
3. Copy APK: `cp app/build/outputs/apk/release/app-release.apk /home/mleku/s/cipherkeys-releases/cipherkeys-v00019.apk`

## README Update (cipherkeys repo)
1. Update the "Latest release" link at the top of `README.md` with new version
2. Add new version row to releases table
3. Commit and push: `git add README.md && git commit -m 'v<VERSION>: bump' && git push`

## Obtanium Release (cipherkeys-releases repo)
1. Copy APK: already done in version bump step
2. Commit, tag, push to git.smesh.lol:
   ```
   git add cipherkeys-v<VERSION>.apk && git commit -m 'v<VERSION>: ...'
   git tag -f v<VERSION> HEAD
   git push && git push --tags
   ```
3. Create GitHub release: `gh release create v<VERSION> cipherkeys-v<VERSION>.apk -R mlekudev/cipherkeys-releases`

## Obtanium User Setup
- Add `https://github.com/mlekudev/cipherkeys-releases` in Obtanium.
