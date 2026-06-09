package com.callrecorder.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.callrecorder.app.service.CallRecorderService
import com.callrecorder.app.service.NativeFolderSyncWorker

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive action: $action")

        if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            Log.d(TAG, "Outgoing call detected to: $phoneNumber")
            saveLastPhoneNumber(context, phoneNumber)
        } else if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            
            Log.d(TAG, "Phone state changed: $stateStr, number: $incomingNumber")

            if (incomingNumber != null) {
                saveLastPhoneNumber(context, incomingNumber)
            }

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Call is ringing (incoming)
                    Log.d(TAG, "State: RINGING")
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call is active (outgoing started or incoming answered)
                    Log.d(TAG, "State: OFFHOOK - Starting recording service")
                    val number = getLastPhoneNumber(context)
                    startRecordingService(context, number)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended
                    Log.d(TAG, "State: IDLE - Stopping recording service and triggering folder sync")
                    stopRecordingService(context)
                    triggerFolderSync(context)
                }
            }
        }
    }

    private fun triggerFolderSync(context: Context) {
        try {
            val syncRequest = OneTimeWorkRequestBuilder<NativeFolderSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "One-time native folder sync worker enqueued successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger folder sync worker", e)
        }
    }

    private fun startRecordingService(context: Context, phoneNumber: String?) {
        val intent = Intent(context, CallRecorderService::class.java).apply {
            putExtra(CallRecorderService.EXTRA_PHONE_NUMBER, phoneNumber ?: "Unknown")
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording service", e)
        }
    }

    private fun stopRecordingService(context: Context) {
        val intent = Intent(context, CallRecorderService::class.java)
        context.stopService(intent)
    }

    private fun saveLastPhoneNumber(context: Context, number: String?) {
        if (number.isNullOrEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_NUMBER, number).apply()
    }

    private fun getLastPhoneNumber(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_NUMBER, null)
    }

    companion object {
        private const val TAG = "CallReceiver"
        private const val PREFS_NAME = "call_recorder_prefs"
        private const val KEY_LAST_NUMBER = "last_phone_number"
    }
}
