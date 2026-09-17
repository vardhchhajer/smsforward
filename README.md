# SMS Forwarder Pro

SMS Forwarder Pro is an Android application for processing and forwarding SMS and MMS messages using configurable rules. The app includes a foreground service, background workers, and an Android UI.

## Build

Open the project in Android Studio with the required Android SDK, or run `gradlew.bat assembleDebug` on Windows. The Gradle wrapper is included. Install the resulting APK on a device where you can grant the SMS permissions requested by the application.

## Code layout

- `app/src/main/`: application, UI, service, and worker code.
- `app/src/test/`: unit tests, including rule evaluation tests.
- `app/src/androidTest/`: device UI tests.
- `gradle/`: Gradle wrapper configuration.

See `ARCHITECTURE.md` and `CONTRIBUTING.md` for additional project notes. `PROJECT_SUMMARY.md` is an older generated summary and does not accurately describe the Android implementation.
