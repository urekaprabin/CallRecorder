# Hybrid Call Recorder

A modern, offline-first, private Android call recording and sync utility designed specifically for non-rooted **Xiaomi devices (MIUI / HyperOS)** running **Android 10 to 15+**.

This application solves the call recording restrictions introduced in modern Android versions using a **Hybrid Strategy**, combining a real-time local microphone recorder with an automated folder-synchronization worker that imports high-quality native recordings from Xiaomi's system dialer.

---

## 🌟 Key Features

*   🔄 **Xiaomi Native Sync (Recommended Mode):** Automatically monitors, copies, and organizes high-quality native recordings from your Xiaomi dialer directory (`/MIUI/sound_recorder/call_rec/`) using the **Storage Access Framework (SAF)**.
*   🎙️ **App-Level Recorder:** Fallback real-time call audio recorder utilizing the `VOICE_COMMUNICATION` stream and microphone, with an optional **Auto-Speakerphone** toggle to capture both sides of the conversation.
*   🚀 **Instant Sync triggers:** Intercepts call terminations instantly using WorkManager to pull native call logs without background service bottlenecks.
*   🛡️ **System Roles Integration:** Utilizes Android's `RoleManager` API to act as the default **Call Screening** and **Call Redirection** app, securing system-level exemptions for background tasks and caller details.
*   📊 **Local Dashboard (Compose UI):**
    *   Search logs by caller names or phone numbers.
    *   Star/favorite important calls.
    *   Add custom text notes to calls.
    *   Custom sliding media player bar with variable playback speeds ($1.0x$, $1.25x$, $1.5x$, $2.0x$).
*   🔒 **Privacy-First:** Fully offline. No analytics, tracking, or network permissions declared. All databases (Room DB) and audio recordings are saved locally on your device.

---

## 🏗️ Project Architecture & Tech Stack

The application is built using modern Android architecture guidelines:
*   **Language:** Kotlin (1.9.0)
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Dependency Injection:** Hilt
*   **Local Database:** Room Database
*   **Background Processing:** WorkManager
*   **Design Pattern:** MVVM (Model-View-ViewModel) + Clean Architecture (Data/Domain/Presentation separation)

---

## 🛠️ Build Environment

If you need to compile the application yourself without Android Studio:
*   **JDK Version:** Java 17
*   **Gradle Version:** Gradle 8.4
*   **Target SDK:** Android 15 (API 35)

### Build Commands:
Run the following command in PowerShell:
```powershell
$env:JAVA_HOME = "C:\Path\To\JDK17"; .\.gradle-dist\gradle-8.4\bin\gradle.bat assembleDebug
```
The compiled output is generated at:
`[Project Root]/CallRecorder.apk`

---

## 📲 Installation & Onboarding (Xiaomi 15T / Android 15)

Because this is a sideloaded personal app, Android 15's **Enhanced Confirmation Mode** will restrict background overlays and accessibility behaviors. Follow these steps to configure the app:

### Step 1: Install & Allow Restricted Settings
1. Transfer the compiled `CallRecorder.apk` to your phone and install it.
2. Minimize the app. Go to your phone's **Settings** → **Apps** → **Manage Apps** → **Call Recorder**.
3. Tap the **three dots** in the top-right corner and select **Allow restricted settings** (authenticate with your PIN/fingerprint).

### Step 2: Grant Core Permissions
1. Open the app.
2. Tap **Grant Permissions** on the onboarding screen.
3. Allow permissions for **Phone**, **Call Logs**, **Contacts**, **Microphone**, and **Notifications**.

### Step 3: Accept Default App Roles (Crucial)
When prompted by the system dialogs:
1. Set Call Recorder as your default **Caller ID & Spam app** (Call Screening). This allows the app to detect incoming caller details.
2. Set Call Recorder as your default **Call Redirection app**. This allows the app to intercept outgoing caller details.

### Step 4: Configure Xiaomi Native Sync Folder
1. Go to **Settings** in the Call Recorder app.
2. Choose **Xiaomi Native Sync Only** or **Hybrid Mode**.
3. Tap **Select Custom Folder** (or *Setup Required* warning banner on Home screen).
4. Navigate to `Internal Storage > MIUI > sound_recorder > call_rec` and tap **Use this folder**.

---

## 📖 Codebase Reference

For detailed layout descriptions, code paths, and architectural diagrams, refer to the following documents inside the `docs/` folder:
*   [docs/implementation_plan.md](file:///d:/Software/CallRecorder/docs/implementation_plan.md) — Architectural goals and system integration details.
*   [docs/task.md](file:///d:/Software/CallRecorder/docs/task.md) — Features checklist and phase progression logs.
*   [docs/walkthrough.md](file:///d:/Software/CallRecorder/docs/walkthrough.md) — Code outline and step-by-step testing instructions.
