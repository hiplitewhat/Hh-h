# GoToGemini — AI Automation Hub

## 🚀 Complete Android APK Project

### Features
- **Gemini AI Integration** — Auto-submit preset prompts, custom queries via WebView
- **YouTube Automation** — Search, play, playlist mode with multi-frame screenshot capture
- **App Control** — Launch WhatsApp, Chrome, Maps, Gmail, Camera, Calendar
- **System Toggles** — Wi-Fi, Bluetooth, Flashlight on/off
- **Screenshot Service** — Foreground service with MediaProjection for screen capture
- **Accessibility Service** — Auto-type, auto-click, gesture automation across apps
- **Activity Logging** — Real-time color-coded log of all actions

### How to Build
1. Extract this ZIP
2. Open in **Android Studio** (Hedgehog or newer)
3. Let Gradle sync (may take a few minutes)
4. Connect Android device or start emulator
5. Click **Run ▶️**

### Permissions Required
- Internet, Camera, Phone, Bluetooth, Wi-Fi, Storage
- Accessibility Service (enable in Settings > Accessibility)
- Media Projection (for screenshots — prompted at runtime)

### Project Structure
\`\`\`
app/
├── src/main/
│   ├── java/com/example/gotogemini/
│   │   ├── MainActivity.kt              — Main hub with Gemini prompts
│   │   ├── YouTubeAutomationActivity.kt  — YouTube search/playlist/capture
│   │   ├── AppControlActivity.kt         — App launcher & system toggles
│   │   ├── ScreenshotService.kt          — Foreground screenshot service
│   │   ├── AutomationAccessibilityService.kt — UI automation
│   │   ├── LogEntry.kt                   — Log data class
│   │   └── LogAdapter.kt                — RecyclerView adapter for logs
│   ├── res/
│   │   ├── layout/                       — All XML layouts
│   │   ├── values/                       — Colors, strings, themes
│   │   └── xml/                          — Accessibility config
│   └── AndroidManifest.xml
├── build.gradle                          — App-level Gradle
build.gradle                              — Project-level Gradle
settings.gradle
gradle.properties
\`\`\`

### Min SDK: 26 (Android 8.0)
### Target SDK: 34 (Android 14)
