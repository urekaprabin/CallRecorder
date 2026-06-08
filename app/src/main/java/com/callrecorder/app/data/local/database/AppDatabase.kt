package com.callrecorder.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.callrecorder.app.data.local.dao.RecordingDao
import com.callrecorder.app.data.local.entity.RecordingEntity

@Database(entities = [RecordingEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
