# Code Walkthrough — Hybrid Call Recorder App

We have successfully created the complete Android Studio project structure and codebase for your **Hybrid Call Recorder app** configured for a non-rooted **Xiaomi 15T on Android 15 (HyperOS)**.

Here is a comprehensive overview of the architecture and directories, along with explanations of the features and setup instructions.

---

## 1. Project Architecture & Codebase Layout

All code is written in **Kotlin** using standard **Jetpack Compose + Hilt + Room + MVVM + Clean Architecture** guidelines.

### Root Configs
*   [`settings.gradle.kts`](file:///d:/Software/CallRecorder/settings.gradle.kts): Module definition and repository configurations.
*   [`build.gradle.kts`](file:///d:/Software/CallRecorder/build.gradle.kts): Root-level plugins orchestration.
*   [`gradle.properties`](file:///d:/Software/CallRecorder/gradle.properties): AndroidX and JVM options.
*   [`gradle/libs.versions.toml`](file:///d:/Software/CallRecorder/gradle/libs.versions.toml): Version catalog managing dependencies.

### App Module
*   [`AndroidManifest.xml`](file:///d:/Software/CallRecorder/app/src/main/AndroidManifest.xml): Complete manifest declaring permissions, receivers, and foreground service requirements for Android 15.
*   [`CallRecorderApp.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/CallRecorderApp.kt): Sets up the Hilt framework and registers Android Oreo+ notification channels.

### Core System Services & Receivers
*   [`CallReceiver.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/receiver/CallReceiver.kt): Listens to `PHONE_STATE` broadcasts and triggers the recording service.
*   [`BootReceiver.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/receiver/BootReceiver.kt): Auto-starts the background monitoring and schedules periodic syncs.
*   [`CallScreeningServiceImpl.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/service/CallScreeningServiceImpl.kt): Resolves incoming phone numbers on Android 12+ (since system broadcasts block numbers).
*   [`CallRecorderService.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/service/CallRecorderService.kt): Foreground Service that toggles speakerphone (Mode 1), starts/stops recorder, and writes logs to DB.
*   [`AudioRecorderManager.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/service/AudioRecorderManager.kt): Manages low-level `MediaRecorder` setup (M4A/AAC format, VOICE_COMMUNICATION stream).
*   [`NativeFolderSyncWorker.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/service/NativeFolderSyncWorker.kt): Periodically watches the HyperOS directory `/MIUI/sound_recorder/call_rec/` using SAF tree traversal, extracts contact metadata, copies recordings to custom folders, and updates DB.

### Data Layer (Room)
*   [`RecordingEntity.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/data/local/entity/RecordingEntity.kt): SQLite table model for logs.
*   [`RecordingDao.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/data/local/dao/RecordingDao.kt): Query interface with Flow and search support.
*   [`AppDatabase.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/data/local/database/AppDatabase.kt): Room base database configuration.
*   [`RecordingRepositoryImpl.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/data/repository/RecordingRepositoryImpl.kt): Repository implementation mapping entities ↔ domain models.

### Presentation Layer (Jetpack Compose)
*   [`MainActivity.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/presentation/MainActivity.kt): Manages runtime permission requests and initiates Compose rendering.
*   [`AppNavigation.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/presentation/navigation/AppNavigation.kt): Routing configuration.
*   [`PermissionsOnboardingScreen.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/presentation/screens/onboarding/PermissionsOnboardingScreen.kt): Guide screen to assist in bypassing Android 15 Restricted Settings.
*   [`HomeScreen.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/presentation/screens/home/HomeScreen.kt): recordings list dashboard with search, star filters, manual Xiaomi sync button, note editor popup, and a custom sliding bottom player panel.
*   [`SettingsScreen.kt`](file:///d:/Software/CallRecorder/app/src/main/java/com/callrecorder/app/presentation/screens/settings/SettingsScreen.kt): Enables/disables recording service, toggles modes (App Recorder / Native Sync / Hybrid), toggles auto-speakerphone, and prompts Storage Access Framework folder selection.

---

## 2. Setting Up on Your Xiaomi 15T

Since you don't have Android Studio installed, we have pre-compiled the app directly from the codebase. You can find the installable package at:
*   [CallRecorder.apk](file:///d:/Software/CallRecorder/CallRecorder.apk) (located at the root of your project folder).

Follow these instructions to transfer and set it up on your **Xiaomi 15T (Android 15 / HyperOS)**:

1.  **Transfer the APK:**
    *   Connect your Xiaomi 15T to your PC via a USB cable.
    *   Select **File Transfer** mode on your phone.
    *   Copy the [CallRecorder.apk](file:///d:/Software/CallRecorder/CallRecorder.apk) file to your phone's internal storage (e.g., in the `Downloads` folder).
2.  **Install the APK:**
    *   On your phone, open the **File Manager** (or Files app) and locate the copied `CallRecorder.apk`.
    *   Tap the APK to install it. If prompted to allow installations from unknown sources, toggle it on.
3.  **Allow Restricted Settings (Critical for Android 15):**
    *   Once installed, the app might request overlay, notification, or special permissions and get blocked with a "Restricted setting" dialog.
    *   Minimize the app. Go to your phone's **Settings** → **Apps** → **Manage Apps** → **Call Recorder**.
    *   Scroll down, tap **Allow restricted settings** (you will be prompted to authenticate with your fingerprint/PIN).
4.  **Grant Permissions & Configure:**
    *   Reopen the **Call Recorder** app.
    *   Click **Grant Permissions** on the onboarding screen to authorize the required permissions (Call Logs, Contacts, Phone, Notification, and Accessibility Service if using app-level recording).
    *   Go to **Settings** in the app:
        *   Enable the Call Recorder Service.
        *   Choose your preferred recording mode (**Hybrid Mode** is recommended).
        *   Set **Auto-Speakerphone** if you intend to use App-level recording.
        *   Select the custom folder path to sync Xiaomi's native call records (see instructions below).

---

## 3. How to Test the Recording

### Test Mode 1 (App-Level Recorder)
1.  In the app Settings, set recording mode to **App Recorder Only** or **Hybrid Mode**.
2.  Enable **Auto-Speakerphone** (ensures earpiece sound is picked up by your microphone).
3.  Call a friend silently → Your phone will toggle speakerphone on automatically, record, and save it.
4.  Open the app and play it back from the dashboard list.

### Test Mode 2 (Xiaomi Native Call Sync — Recommended)
1.  Open your Xiaomi native Phone app settings and enable **Auto Record Calls**.
2.  In our Call Recorder Settings, set recording mode to **Xiaomi Native Sync Only** or **Hybrid Mode**.
3.  Click **Select Custom Folder** and choose the directory where Xiaomi saves its call recordings:
    `Internal Storage > MIUI > sound_recorder > call_rec`
4.  Place a standard cellular call.
5.  Open our app and tap the **Refresh icon** in the top-right → The native high-quality recording will be imported instantly, linked to contact name, and accessible in your recordings list dashboard!
