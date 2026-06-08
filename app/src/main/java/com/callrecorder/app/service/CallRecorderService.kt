package com.callrecorder.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.NotificationCompat
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.domain.model.Recording
import com.callrecorder.app.domain.repository.RecordingRepository
import com.callrecorder.app.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CallRecorderService : Service() {

    @Inject
    lateinit var audioRecorderManager: AudioRecorderManager

    @Inject
    lateinit var recordingRepository: RecordingRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var audioManager: AudioManager? = null
    private var wasSpeakerphoneOn = false
    private var isSpeakerphoneModified = false
    private var startTimestamp: Long = 0
    private var phoneNumber: String = "Unknown"
    private var contactName: String? = null
    private var isRecordingStarted = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        phoneNumber = intent?.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        contactName = resolveContactName(phoneNumber)

        startTimestamp = System.currentTimeMillis()

        // Read user configurations
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        val recordingMode = prefs.getString(KEY_RECORDING_MODE, "HYBRID") // APP, NATIVE, HYBRID
        val autoSpeaker = prefs.getBoolean(KEY_AUTO_SPEAKER, false)

        Log.d(TAG, "Config - Enabled: $serviceEnabled, Mode: $recordingMode, AutoSpeaker: $autoSpeaker")

        if (!serviceEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Show foreground notification
        showForegroundNotification()

        // Mode 1 (APP) or Hybrid: Handle audio recording inside our app
        if (recordingMode == "APP" || recordingMode == "HYBRID") {
            // Check if we need to modify speakerphone settings
            if (autoSpeaker) {
                try {
                    audioManager?.let { am ->
                        wasSpeakerphoneOn = am.isSpeakerphoneOn
                        if (!wasSpeakerphoneOn) {
                            am.isSpeakerphoneOn = true
                            isSpeakerphoneModified = true
                            Log.d(TAG, "Auto-Speakerphone toggled ON")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to toggle speakerphone", e)
                }
            }

            // Prepare recording file destination
            val fileName = "Call_${phoneNumber}_${startTimestamp}.m4a"
            val dir = getRecordingsDirectory()
            val outputFile = File(dir, fileName)

            // Start recording using VOICE_COMMUNICATION fallback to MIC
            // Select source based on API or config (defaulting to VOICE_COMMUNICATION)
            val audioSource = android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION
            
            isRecordingStarted = audioRecorderManager.startRecording(outputFile, audioSource)
            if (!isRecordingStarted) {
                Log.e(TAG, "Audio recording failed to start. Stopping service.")
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        serviceJob.cancel()

        // Stop app-level recording if started
        if (isRecordingStarted) {
            val recordedFile = audioRecorderManager.stopRecording()
            val duration = System.currentTimeMillis() - startTimestamp
            
            if (recordedFile != null && recordedFile.exists() && duration > 1000) {
                // Read custom folder uri
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val customFolderUri = prefs.getString("custom_folder_uri", null)
                
                serviceScope.launch {
                    var finalPath = recordedFile.absolutePath
                    
                    if (!customFolderUri.isNullOrEmpty()) {
                        val safPath = com.callrecorder.app.util.FileManager.copyFileToSafFolder(
                            this@CallRecorderService, recordedFile, customFolderUri
                        )
                        if (safPath != null) {
                            finalPath = safPath
                            // Delete cache file since it's copied
                            recordedFile.delete()
                            Log.d(TAG, "Cache file deleted, playing from SAF path: $safPath")
                        }
                    }

                    val recording = Recording(
                        phoneNumber = phoneNumber,
                        contactName = contactName,
                        callType = detectCallType(),
                        duration = duration,
                        filePath = finalPath,
                        fileSize = if (finalPath == recordedFile.absolutePath) recordedFile.length() else 0L,
                        timestamp = startTimestamp,
                        source = "APP_RECORDER"
                    )
                    recordingRepository.insertRecording(recording)
                    Log.i(TAG, "Recording saved to DB: $finalPath")
                }
            } else {
                Log.w(TAG, "Recording file was null, does not exist, or call was too short")
            }
        }

        // Restore speakerphone state if modified
        if (isSpeakerphoneModified) {
            try {
                audioManager?.isSpeakerphoneOn = wasSpeakerphoneOn
                Log.d(TAG, "Speakerphone state restored to: $wasSpeakerphoneOn")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore speakerphone state", e)
            }
        }

        super.onDestroy()
    }

    private fun showForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val displayName = contactName ?: phoneNumber
        val notification: Notification = NotificationCompat.Builder(this, CallRecorderApp.CHANNEL_ID_RECORDING)
            .setContentTitle("Call Recording Active")
            .setContentText("Recording call with $displayName")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Standard mic icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        // Start Foreground with appropriate types for Android 14+ / Android 15+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getRecordingsDirectory(): File {
        val dir = File(filesDir, "recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun detectCallType(): String {
        // Broadcasts don't guarantee incoming/outgoing context in Service directly.
        // We look up if outgoing number matches current.
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastOutgoing = prefs.getString("last_outgoing_number", "")
        return if (phoneNumber == lastOutgoing) "OUTGOING" else "INCOMING"
    }

    private fun resolveContactName(number: String): String? {
        if (number == "Unknown") return null
        var contactName: String? = null
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (columnIndex != -1) {
                    contactName = cursor.getString(columnIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving contact name", e)
        } finally {
            cursor?.close()
        }
        return contactName
    }

    companion object {
        private const val TAG = "CallRecorderService"
        private const val NOTIFICATION_ID = 1001
        
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        
        private const val PREFS_NAME = "call_recorder_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_RECORDING_MODE = "recording_mode"
        private const val KEY_AUTO_SPEAKER = "auto_speakerphone"
    }
}
