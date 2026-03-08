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

/**
 * Hilt module that provides singleton instances for the app.
 * Creates the Room database and exposes all DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Creates the Room database (single instance for the whole app) */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "twinmind.db"
        ).build()
    }

    /** Provides the DAO for recording sessions */
    @Provides
    fun provideRecordingSessionDao(db: AppDatabase): RecordingSessionDao =
        db.recordingSessionDao()

    /** Provides the DAO for audio chunks */
    @Provides
    fun provideAudioChunkDao(db: AppDatabase): AudioChunkDao =
        db.audioChunkDao()

    /** Provides the DAO for transcript segments */
    @Provides
    fun provideTranscriptSegmentDao(db: AppDatabase): TranscriptSegmentDao =
        db.transcriptSegmentDao()

    /** Provides the DAO for summaries */
    @Provides
    fun provideSummaryDao(db: AppDatabase): SummaryDao =
        db.summaryDao()
}

/**
 * Hilt module that binds repository interfaces to their implementations.
 * This lets us swap implementations easily (e.g., for testing).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /** Binds RecordingRepository interface → RecordingRepositoryImpl */
    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: RecordingRepositoryImpl): RecordingRepository

    /** Binds TranscriptRepository interface → TranscriptRepositoryImpl */
    @Binds
    @Singleton
    abstract fun bindTranscriptRepository(impl: TranscriptRepositoryImpl): TranscriptRepository

    /** Binds SummaryRepository interface → SummaryRepositoryImpl */
    @Binds
    @Singleton
    abstract fun bindSummaryRepository(impl: SummaryRepositoryImpl): SummaryRepository
}
