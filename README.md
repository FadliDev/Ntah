# Sallygram real Telegram chat-list patch

This patch adds the first live Telegram feature: loading the user's main chat list through TDLib after authorization, and showing real chat titles, last-message previews, timestamps, and unread counts in the existing Compose list.

## Files
- `app/src/main/java/com/sallygram/app/TdLibManager.kt` — replace the existing file.
- `MainActivity.patch` — apply this small change to `app/src/main/java/com/sallygram/app/MainActivity.kt`.

## Apply in Termux
Extract the ZIP into the repository root, then run:

```bash
cp app/src/main/java/com/sallygram/app/TdLibManager.kt ~/RecentTask/app/src/main/java/com/sallygram/app/TdLibManager.kt
cd ~/RecentTask
git apply /path/to/MainActivity.patch
git add app/src/main/java/com/sallygram/app/TdLibManager.kt app/src/main/java/com/sallygram/app/MainActivity.kt
git commit -m "Load real Telegram chats through TDLib"
git push origin main
```

If you extracted directly into `~/RecentTask`, the first `cp` is unnecessary and run `git apply MainActivity.patch`.

Notes:
- Requires a successful TDLib login and configured `TELEGRAM_API_ID` / `TELEGRAM_API_HASH`.
- This stage syncs the real chat list only. Opening a conversation and sending/receiving message history is the next feature.
- The patch is based on the public source files currently visible in `FadliDev/Ntah`. A full GitHub Actions build has not been run from this environment; check the next Actions run for API compatibility errors.
