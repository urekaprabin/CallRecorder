package com.callrecorder.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CallRecorderApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for Foreground Call Recording Service
            val recordingChannel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                "Call Recording Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows a status notification during an active call recording."
            }

            // Channel for app notifications (e.g. sync notifications, errors)
            val appChannel = NotificationChannel(
                CHANNEL_ID_INFO,
                "App Alerts & Information",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows notifications when new calls are logged or synced."
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(recordingChannel)
            manager.createNotificationChannel(appChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_RECORDING = "call_recording_channel"
        const val CHANNEL_ID_INFO = "call_recorder_info_channel"
    }
}
