package com.example.shyam_assignment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.shyam_assignment.data.local.dao.AudioChunkDao
import com.example.shyam_assignment.data.local.dao.RecordingSessionDao
import com.example.shyam_assignment.data.local.dao.SummaryDao
import com.example.shyam_assignment.data.local.dao.TranscriptSegmentDao
import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity

/**
 * Room database for the app.
 * Contains 4 tables: sessions, chunks, transcript segments, and summaries.
 */
@Database(
    entities = [
        RecordingSessionEntity::class,
        AudioChunkEntity::class,
        TranscriptSegmentEntity::class,
        SummaryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingSessionDao(): RecordingSessionDao       // Access recording sessions
    abstract fun audioChunkDao(): AudioChunkDao                   // Access audio chunks
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao     // Access transcript segments
    abstract fun summaryDao(): SummaryDao                         // Access summaries
}
