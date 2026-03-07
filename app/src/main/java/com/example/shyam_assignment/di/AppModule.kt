package com.example.shyam_assignment.di

import android.content.Context
import androidx.room.Room
import com.example.shyam_assignment.data.local.AppDatabase
import com.example.shyam_assignment.data.local.dao.AudioChunkDao
import com.example.shyam_assignment.data.local.dao.RecordingSessionDao
import com.example.shyam_assignment.data.local.dao.SummaryDao
import com.example.shyam_assignment.data.local.dao.TranscriptSegmentDao
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.data.repository.SummaryRepository
import com.example.shyam_assignment.data.repository.TranscriptRepository
import com.example.shyam_assignment.data.repository.impl.RecordingRepositoryImpl
import com.example.shyam_assignment.data.repository.impl.SummaryRepositoryImpl
import com.example.shyam_assignment.data.repository.impl.TranscriptRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "twinmind.db"
        ).build()
    }

    @Provides
    fun provideRecordingSessionDao(db: AppDatabase): RecordingSessionDao =
        db.recordingSessionDao()

    @Provides
    fun provideAudioChunkDao(db: AppDatabase): AudioChunkDao =
        db.audioChunkDao()

    @Provides
    fun provideTranscriptSegmentDao(db: AppDatabase): TranscriptSegmentDao =
        db.transcriptSegmentDao()

    @Provides
    fun provideSummaryDao(db: AppDatabase): SummaryDao =
        db.summaryDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptRepository(impl: TranscriptRepositoryImpl): TranscriptRepository

    @Binds
    @Singleton
    abstract fun bindSummaryRepository(impl: SummaryRepositoryImpl): SummaryRepository
}

