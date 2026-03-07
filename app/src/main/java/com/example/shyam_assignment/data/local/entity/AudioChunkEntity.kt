package com.example.shyam_assignment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class AudioChunkEntity(
    @PrimaryKey
    val chunkId: String,
    val sessionId: String,
    val chunkIndex: Int,
    val filePath: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val overlapMs: Long = 0L,
    val transcriptionState: String = TranscriptionState.PENDING,
    val transcriptText: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object TranscriptionState {
    const val PENDING = "PENDING"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}

