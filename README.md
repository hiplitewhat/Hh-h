uuhhh# FloatingIconOverlay

A draggable floating icon overlay app for Android. The icon stays on top of all other apps and can be dragged anywhere on screen.

## Features
- 🌟 Draggable golden star floating icon
- 📌 Stays on top of all other apps (overlay window)
- 🔔 Persistent foreground service notification
- 💡 Double-tap to return to the main app
- ✅ Works on Android 8.0+ (API 26+)

## Building

### Prerequisites
- Gradle wrapper JAR must be present. Run in the project root:
  ```
  gradle wrapper --gradle-version 8.4
  ```
  Or use Android Studio to open the project (it will set this up automatically).

### Build APK
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions
Push to `main` or `master` and the workflow in `.github/workflows/build.yml` will automatically build both debug and release APKs, available as artifacts.

## Permissions
- `SYSTEM_ALERT_WINDOW` — required to draw over other apps
- `FOREGROUND_SERVICE` — required to keep the service alive
