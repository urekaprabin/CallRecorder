package com.callrecorder.app.domain.model

data class Recording(
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String?,
    val callType: String,
    val duration: Long,
    val filePath: String,
    val fileSize: Long,
    val timestamp: Long,
    val source: String,
    val isStarred: Boolean = false,
    val notes: String? = null
)
