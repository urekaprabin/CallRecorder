package com.callrecorder.app.domain.repository

import com.callrecorder.app.domain.model.Recording
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<Recording>>
    fun searchRecordings(query: String): Flow<List<Recording>>
    suspend fun getRecordingById(id: Long): Recording?
    suspend fun getRecordingByFilePath(path: String): Recording?
    suspend fun insertRecording(recording: Recording): Long
    suspend fun updateRecording(recording: Recording)
    suspend fun deleteRecording(recording: Recording)
    suspend fun deleteRecordingById(id: Long)
}
