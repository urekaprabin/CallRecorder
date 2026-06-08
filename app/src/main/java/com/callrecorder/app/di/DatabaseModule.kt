package com.callrecorder.app.di

import android.content.Context
import androidx.room.Room
import com.callrecorder.app.data.local.dao.RecordingDao
import com.callrecorder.app.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "call_recorder_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideRecordingDao(database: AppDatabase): RecordingDao {
        return database.recordingDao()
    }
}
