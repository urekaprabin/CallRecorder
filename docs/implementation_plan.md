# Android Call Recorder App — Implementation Plan (Xiaomi 15T, Android 15+)

This plan is tailored for building a **Hybrid Call Recorder app** for a non-rooted Xiaomi 15T (Android 15+, HyperOS, India region) to be sideloaded for personal use.

---

## User & Device Profile
- **Device:** Xiaomi 15T (HyperOS, Android 15+, India)
- **Root Status:** Non-Rooted (No plans to root)
- **Distribution:** Sideloaded APK for personal use
- **Goal:** Record both sides of incoming/outgoing calls silently (no audio warnings or announcements to either party) and save to a designated location (internal or user-specified folder).

---

## Technical Architecture: Hybrid Model

We will build both systems and provide a toggle in the settings screen to switch between them:

```
                  ┌──────────────────────────────────────────┐
                  │          Call Recorder App Settings      │
                  └────────────────────┬─────────────────────┘
                                       │
                     ┌─────────────────┴─────────────────┐
                     ▼                                   ▼
          [Mode 1: App Recorder]              [Mode 2: Native Sync]
          - Records via MIC/VOICE_COMM        - Watches Xiaomi folder
          - Requires Auto-Speakerphone        - Reads native call files
          - Saves directly to database        - Copies, logs, and manages
```

### Mode 1: App-Level Recorder
- **Recording Source:** Uses `AudioSource.VOICE_COMMUNICATION` (optimized for voice call streams) fallback to `AudioSource.MIC`.
- **Two-Way Capture:** Toggles the phone speakerphone on when recording starts (optional/configurable) so the microphone can capture the caller's voice clearly.
- **Audio Output:** Saved directly into a designated folder (e.g. `Downloads/MyCallRecordings/` or app-private storage).

### Mode 2: Xiaomi Native Call Sync (Recommended for Quality)
- **Mechanism:** Xiaomi's built-in system-level dialer records calls in perfect two-way quality.
- **Watcher Service:** Runs a background `FileObserver` watching the directory:
  `/storage/emulated/0/MIUI/sound_recorder/call_rec/` (or the corresponding HyperOS application directory).
- **Import Flow:** When a new recording is saved by the system, our app detects it, resolves the contact details using the timestamp, copies it to the user's custom location, and lists it in our UI.

---

## Recording Notification & Silence

### 1. Remote Caller (Other Party)
*   **Silence:** No audio warning, no beep, and no voice announcement will be played. The recording process will be completely silent to the other party.

### 2. Local Caller Notification (Visual & Haptic for App User)
*   **Status Notification:** A persistent, high-priority system notification showing a red recording dot and status (helps keep the background service from being killed by HyperOS).
*   **Floating Indicator (Overlay):** A small, semi-transparent recording dot overlay on top of the call screen showing call duration and recording status.
*   **Haptic feedback:** A distinct vibration when recording starts and stops so you are aware of the status without looking at the screen.

---

## Permissions & Android 15 Sideloading Workarounds

Since Android 15 enforces **Enhanced Confirmation Mode** (Restricted Settings) for sideloaded apps, the user must perform a one-time configuration step to grant the **Accessibility Service** and **System Overlay** permissions:
1. Go to **Settings** → **Apps** → **Manage Apps** → select **Call Recorder**.
2. Tap the **three dots** in the top-right corner.
3. Tap **Allow restricted settings**.
4. Now open the app and grant the required permissions normally.

### Permissions List
*   `RECORD_AUDIO`: Capture microphone audio.
*   `READ_PHONE_STATE`: Detect call state (ringing, off-hook, idle).
*   `READ_CALL_LOG` / `READ_CONTACTS`: Resolve numbers to names.
*   `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` + `FOREGROUND_SERVICE_PHONE_CALL`: Run recording services in the background.
*   `SYSTEM_ALERT_WINDOW`: Display the floating call overlay.
*   `MODIFY_AUDIO_SETTINGS`: Toggle speakerphone.
*   `POST_NOTIFICATIONS`: Show foreground notifications.
*   `RECEIVE_BOOT_COMPLETED`: Restart the call-watching service when the phone boots.

---

## Database & UI Specifications

### Room Database (Recording Table)
- `id` (Primary Key)
- `phoneNumber`, `contactName`
- `filePath` (Custom destination folder)
- `duration`
- `callType` (INCOMING / OUTGOING)
- `timestamp`
- `source` (APP_RECORDER / NATIVE_SYNC)

### UI Design (Jetpack Compose)
1. **Home/Recordings List:** Shows call logs grouped by date. Clicking plays the call instantly. Includes search and star filters.
2. **Settings Screen:**
   - **Service Switch:** Master on/off toggle.
   - **Recording Mode:** Toggle between `App Recorder (Mode 1)` and `Xiaomi Native Sync (Mode 2)`.
   - **Auto-Speakerphone:** On/off for Mode 1.
   - **Destination Folder:** Select custom folder via Storage Access Framework.
3. **Audio Player:** Beautiful floating bottom sheet player with speed control (1.0x, 1.25x, 1.5x, 2.0x) and audio waveform.

---

## Verification Plan

### Test Procedure
1. **Sideload APK** on Xiaomi 15T.
2. Enable **Restricted Settings** and grant all permissions (Overlay, Phone, Mic).
3. **Verify Mode 1 (App Recorder):**
   - Enable Mode 1 + Auto-Speakerphone.
   - Place a call → Verify speakerphone turns on, and both sides are recorded silently without audio alerts.
4. **Verify Mode 2 (Native Sync):**
   - Enable call recording in Xiaomi system settings.
   - Place a call → Verify our app detects the native recording file, copies it to the custom folder, and logs it in the recording list.
5. **Verify Storage:**
   - Check if audio files (.m4a / .mp3) are successfully saved to your selected directory.
