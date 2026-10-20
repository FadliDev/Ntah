# Sallygram performance + persistent signing patch

Contains:
- `.github/workflows/build.yml`: copies the persistent keystore to `$HOME/.android/debug.keystore` before `assembleDebug`.
- `signing/sallygram-debug.keystore`: stable debug signing identity for this private repository.
- `app/src/main/java/com/sallygram/app/TdLibManager.kt`: initially loads 30 chats rather than 100 to reduce initial loading work.

## Apply
Extract the ZIP into the repository root, replacing the listed files. Keep this repository private and do not share the keystore or this ZIP publicly.

Then run:
```bash
git add .github/workflows/build.yml signing/sallygram-debug.keystore app/src/main/java/com/sallygram/app/TdLibManager.kt
git commit -m "Speed up initial chat loading and persist APK signing key"
git push origin main
```

## Important signing note
The keystore uses alias `androiddebugkey` and password `android` for Android's debug signing configuration. This is intended for personal sideloaded debug APKs. Do not use this debug key for Google Play production releases.

Your already-installed APK may have been signed with a different ephemeral runner key. The first install using this new key may require uninstalling the old APK first (which deletes local app data); after that, future builds using this same key should install as updates as long as `applicationId` remains unchanged.

The keystore is included because the repository is private as requested, but anyone who obtains this file can sign builds as this app. Keep repository access restricted and retain a secure backup.
