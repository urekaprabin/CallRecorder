package com.callrecorder.app.di

import com.callrecorder.app.data.repository.RecordingRepositoryImpl
import com.callrecorder.app.domain.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        recordingRepositoryImpl: RecordingRepositoryImpl
    ): RecordingRepository
}
