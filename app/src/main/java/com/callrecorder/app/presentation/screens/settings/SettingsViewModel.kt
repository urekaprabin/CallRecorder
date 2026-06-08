package com.callrecorder.app.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callrecorder.app.service.NativeFolderSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _serviceEnabled = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ENABLED, true))
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    private val _recordingMode = MutableStateFlow(prefs.getString(KEY_RECORDING_MODE, "HYBRID") ?: "HYBRID")
    val recordingMode: StateFlow<String> = _recordingMode.asStateFlow()

    private val _autoSpeakerphone = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SPEAKER, false))
    val autoSpeakerphone: StateFlow<Boolean> = _autoSpeakerphone.asStateFlow()

    private val _customFolderUri = MutableStateFlow(prefs.getString(KEY_CUSTOM_FOLDER_URI, "") ?: "")
    val customFolderUri: StateFlow<String> = _customFolderUri.asStateFlow()

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _serviceEnabled.value = enabled
        updateWorkManagerSchedule()
    }

    fun setRecordingMode(mode: String) {
        prefs.edit().putString(KEY_RECORDING_MODE, mode).apply()
        _recordingMode.value = mode
        updateWorkManagerSchedule()
    }

    fun setAutoSpeakerphone(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAKER, enabled).apply()
        _autoSpeakerphone.value = enabled
    }

    fun setCustomFolderUri(uriStr: String) {
        prefs.edit().putString(KEY_CUSTOM_FOLDER_URI, uriStr).apply()
        _customFolderUri.value = uriStr
        
        // Take persistable permission
        try {
            val contentResolver = context.contentResolver
            val uri = android.net.Uri.parse(uriStr)
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            Log.e("SettingsViewModel", "Failed to take persistable URI permission", e)
        }
    }

    private fun updateWorkManagerSchedule() {
        val workManager = WorkManager.getInstance(context)
        val isEnabled = _serviceEnabled.value
        val mode = _recordingMode.value

        if (isEnabled && (mode == "NATIVE" || mode == "HYBRID")) {
            // Schedule periodic folder sync worker
            Log.d("SettingsViewModel", "Scheduling sync worker...")
            val syncRequest = PeriodicWorkRequestBuilder<NativeFolderSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()

            workManager.enqueueUniquePeriodicWork(
                NativeFolderSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        } else {
            // Cancel background folder sync worker
            Log.d("SettingsViewModel", "Cancelling sync worker...")
            workManager.cancelUniqueWork(NativeFolderSyncWorker.WORK_NAME)
        }
    }

    companion object {
        private const val PREFS_NAME = "call_recorder_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_RECORDING_MODE = "recording_mode"
        private const val KEY_AUTO_SPEAKER = "auto_speakerphone"
        private const val KEY_CUSTOM_FOLDER_URI = "custom_folder_uri"
    }
}

