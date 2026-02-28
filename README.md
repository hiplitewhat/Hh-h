ooo# AI Assistant — Android APK Template

A standalone Android AI chat app powered by Cloudflare Workers AI (Llama 3 8B).
No Roblox. No third-party AI SDK keys required. Just drop it into Android Studio and build.

---

## 📁 Project Structure

```
AIAssistantApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/aiassistant/
│   │   │   └── MainActivity.java        ← All AI logic lives here
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml ← Chat UI
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       ├── colors.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🚀 How to Build the APK

### Step 1 — Open in Android Studio
1. Open Android Studio → **File → Open** → select the `AIAssistantApp/` folder.
2. Wait for Gradle sync to finish (it downloads OkHttp automatically).

### Step 2 — Connect a device or start an emulator
- **Physical device**: Enable Developer Options → USB Debugging → plug in via USB.
- **Emulator**: AVD Manager → create a device → API 24+.

### Step 3 — Run or build
- **Run directly**: Press ▶ (Run button).
- **Build APK**: Build → Build Bundle(s) / APK(s) → Build APK(s).
  - Output: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: Build → Generate Signed Bundle / APK → follow the signing wizard.

---

## ✨ Features

| Feature | How to trigger |
|---|---|
| Chat with AI | Type any message |
| Find & open installed app | "find YouTube" / "open Maps" |
| YouTube search | "find funny cats" (falls back if app not found) |
| Open a URL | AI replies `OPEN:https://...` |
| Wordle hint | Tap 🟩 Wordle button |
| Quick explore | Tap ✨ Explore button |
| Conversation memory | Last 10 turns kept in session |

---

## 🔧 AI Endpoints (Fallback Chain)

The app tries these 3 Cloudflare Workers AI endpoints in order, moving to the
next if one times out or returns empty:

1. `https://ai-chat.pastefyuser1231.workers.dev/api/chat`
2. `https://steep-union-c19f.eee199425.workers.dev/api/chat`
3. `https://holy-glitter-7345.foals-option9u.workers.dev/api/chat`

Model: `@cf/meta/llama-3-8b-instruct`

To swap in your own endpoint, edit `API_URLS[]` at the top of `MainActivity.java`.

---

## 🤖 AI Command Protocol

The system prompt instructs the AI to reply using structured commands:

| Command | Effect |
|---|---|
| `FIND:<query>` | Search installed apps, then YouTube |
| `OPEN:<url>` | Open URL in browser |
| `CHAT:<message>` | Display text in chat |
| `WORDLE:<hint>` | Show Wordle word suggestion |

---

## 📋 Permissions Required

| Permission | Why |
|---|---|
| `INTERNET` | AI API calls |
| `QUERY_ALL_PACKAGES` | Scan installed apps for FIND: command |

---

## 🔮 Optional Enhancements (not included, easy to add)

- **Voice input** — use `SpeechRecognizer` API, feed result to `submitMessage()`
- **Persistent chat history** — save `conversationHistory` to `SharedPreferences`
- **Background service** — `JobScheduler` + `Service` for autonomous AI actions
- **Notification replies** — `NotificationCompat` with inline reply action
- **Custom wake word** — `SpeechRecognizer` running in background service
