# Checklist for Android Call Recorder App

## Phase 1: Core Foundation ✦
- [x] Set up Android Gradle project with Kotlin, Compose, Hilt, Room
- [x] Create `AndroidManifest.xml` with all required permissions
- [x] Implement `CallReceiver` (BroadcastReceiver for call detection)
- [x] Implement `CallRecorderService` (Foreground Service)
- [x] Implement `AudioRecorderManager` (MediaRecorder wrapper)
- [x] Implement `CallScreeningServiceImpl` (Caller ID resolution on Android 12+)
- [x] Implement `NativeFolderSyncWorker` (Watch and sync Xiaomi native recordings)
- [x] Create Room database + `RecordingDao` + `RecordingEntity`
- [x] Create basic `RecordingRepository`
- [x] Implement runtime permission check and request flow

## Phase 2: UI & Playback ✦
- [x] Set up navigation and main dashboard
- [x] Design Home Screen (recording list with search and filters)
- [x] Build recording list item card and swipe-to-delete
- [x] Build custom Player Screen with audio playback & controls
- [x] Implement waveform visualizer for player
- [x] Build Settings Screen (toggles for recording modes, speakerphone, storage folder)

## Phase 3: Settings & Polish ✦
- [x] Add notes and star features to recording entities
- [x] Implement Storage Access Framework (SAF) folder picker and file moving logic
- [x] Set up boot-start receiver (`BootReceiver`) and service auto-restart
- [x] Refine notification channel and foreground service handling for Android 15
- [x] Create permission onboarding flow for Enhanced Confirmation Mode bypass

## Phase 4: Advanced Features ✦
- [ ] Add per-contact recording rules (always/never record specific contacts)
- [ ] Implement auto-delete old recordings based on retention settings
- [ ] Add dark/light theme options and haptic feedback toggles
- [x] Final end-to-end local testing and validation (APK compilation & verification)
