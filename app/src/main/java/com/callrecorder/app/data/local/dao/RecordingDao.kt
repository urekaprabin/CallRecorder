package com.callrecorder.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callrecorder.app.data.local.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE filePath = :path LIMIT 1")
    suspend fun getRecordingByFilePath(path: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE phoneNumber LIKE '%' || :query || '%' OR contactName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchRecordings(query: String): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Delete
    suspend fun deleteRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecordingById(id: Long)
}
