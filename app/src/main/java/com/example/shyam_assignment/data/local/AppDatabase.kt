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
    abstract fun recordingSessionDao(): RecordingSessionDao
    abstract fun audioChunkDao(): AudioChunkDao
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao
    abstract fun summaryDao(): SummaryDao
}

