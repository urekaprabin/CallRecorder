package com.callrecorder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callrecorder.app.service.NativeFolderSyncWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted. Registering services and workers...")
            
            // Check if Native Sync Mode is enabled in preferences
            val prefs = context.getSharedPreferences("call_recorder_prefs", Context.MODE_PRIVATE)
            val isServiceEnabled = prefs.getBoolean("service_enabled", true)
            val recordMode = prefs.getString("recording_mode", "HYBRID") // APP, NATIVE, HYBRID

            if (isServiceEnabled && (recordMode == "NATIVE" || recordMode == "HYBRID")) {
                scheduleFolderSync(context)
            }
        }
    }

    private fun scheduleFolderSync(context: Context) {
        Log.d(TAG, "Scheduling periodic folder sync worker...")
        val syncRequest = PeriodicWorkRequestBuilder<NativeFolderSyncWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NativeFolderSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
