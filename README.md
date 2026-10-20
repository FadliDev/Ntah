# Sallygram — TDLib integration starter

Sallygram is an Android Telegram-client starter built with Kotlin, AndroidX, Jetpack Compose, Material Design 3, and the official TDLib Java/JNI interface.

## Current implementation

- Initial Compose/Material 3 UI and demo chat list.
- TDLib client initialization and authorization-state handling.
- Login flow UI for phone number, login code, and two-step verification password.
- GitHub Actions workflow builds TDLib for Android using the upstream TDLib Android scripts, stages generated Java bindings/native libraries, and then builds the debug APK.

**Important:** The visible chat list, contacts, and calls are still demo placeholders. Successful login does not yet populate real chats or send messages; those features need to be connected to TDLib chat/message updates in the next implementation stage. A TDLib build has not been executed in this environment, so verify the first GitHub Actions run before treating the APK as release-ready.

## Configure Telegram API credentials

1. Register an application at https://my.telegram.org and obtain `api_id` and `api_hash`.
2. In your GitHub repository, open **Settings → Secrets and variables → Actions**.
3. Add repository secrets named `TELEGRAM_API_ID` and `TELEGRAM_API_HASH`.
4. Run the **Build Sallygram APK** workflow again.

The workflow reads those secrets as environment variables. If they are absent, the app still builds but displays a configuration message and cannot start Telegram authorization. Do not commit your API hash or account credentials to source control.

For local builds, set `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` in the shell environment before running `./gradlew assembleDebug`. The `scripts/prepare-tdlib.sh` script must also be run first to generate/stage TDLib's Java bindings and native libraries.

## Build APK with GitHub Actions

1. Push this project to a GitHub repository.
2. Add the optional Telegram API secrets described above.
3. Open **Actions → Build Sallygram APK** and run the workflow, or push to `main`/`master`.
4. Download the **Sallygram-debug** artifact from the workflow run and extract the APK.

The workflow builds TDLib from the official upstream repository. This native build can take several minutes and may need maintenance if upstream build scripts or Android toolchain versions change.

## License and upstream

TDLib is maintained by Telegram. Review its current license and terms at https://github.com/tdlib/td before distributing a derivative application.
