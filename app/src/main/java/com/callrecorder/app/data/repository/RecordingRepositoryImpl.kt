package com.callrecorder.app.data.repository

import com.callrecorder.app.data.local.dao.RecordingDao
import com.callrecorder.app.data.local.entity.RecordingEntity
import com.callrecorder.app.domain.model.Recording
import com.callrecorder.app.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val recordingDao: RecordingDao
) : RecordingRepository {

    override fun getAllRecordings(): Flow<List<Recording>> {
        return recordingDao.getAllRecordings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchRecordings(query: String): Flow<List<Recording>> {
        return recordingDao.searchRecordings(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRecordingById(id: Long): Recording? {
        return recordingDao.getRecordingById(id)?.toDomain()
    }

    override suspend fun getRecordingByFilePath(path: String): Recording? {
        return recordingDao.getRecordingByFilePath(path)?.toDomain()
    }

    override suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording.toEntity())
    }

    override suspend fun updateRecording(recording: Recording) {
        recordingDao.updateRecording(recording.toEntity())
    }

    override suspend fun deleteRecording(recording: Recording) {
        recordingDao.deleteRecording(recording.toEntity())
    }

    override suspend fun deleteRecordingById(id: Long) {
        recordingDao.deleteRecordingById(id)
    }

    // Mapper extension functions
    private fun RecordingEntity.toDomain(): Recording {
        return Recording(
            id = id,
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = callType,
            duration = duration,
            filePath = filePath,
            fileSize = fileSize,
            timestamp = timestamp,
            source = source,
            isStarred = isStarred,
            notes = notes
        )
    }

    private fun Recording.toEntity(): RecordingEntity {
        return RecordingEntity(
            id = id,
            phoneNumber = phoneNumber,
            contactName = contactName,
            callType = callType,
            duration = duration,
            filePath = filePath,
            fileSize = fileSize,
            timestamp = timestamp,
            source = source,
            isStarred = isStarred,
            notes = notes
        )
    }
}
