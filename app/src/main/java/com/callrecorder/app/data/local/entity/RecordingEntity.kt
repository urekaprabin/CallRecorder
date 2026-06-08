package com.callrecorder.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val callType: String,              // "INCOMING" or "OUTGOING"
    val duration: Long,                // in milliseconds
    val filePath: String,              // local file URI/path
    val fileSize: Long,                // in bytes
    val timestamp: Long,               // Epoch millis
    val source: String,                // "APP_RECORDER" or "NATIVE_SYNC"
    val isStarred: Boolean = false,
    val notes: String? = null
)
