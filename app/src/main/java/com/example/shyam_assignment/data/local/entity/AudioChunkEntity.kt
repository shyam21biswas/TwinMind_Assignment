package com.example.shyam_assignment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a single audio chunk.
 * Recording is split into 30-second WAV chunks.
 * Each chunk is transcribed separately by Gemini.
 */
@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = RecordingSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE       // Delete chunks when session is deleted
        )
    ],
    indices = [Index("sessionId")]              // Index for fast lookup by session
)
data class AudioChunkEntity(
    @PrimaryKey
    val chunkId: String,                                // Unique ID for this chunk
    val sessionId: String,                              // Which session this chunk belongs to
    val chunkIndex: Int,                                // Order number (0, 1, 2, ...)
    val filePath: String,                               // Path to the WAV file on disk
    val startedAt: Long,                                // When this chunk started recording
    val endedAt: Long,                                  // When this chunk finished recording
    val durationMs: Long,                               // Duration of this chunk in ms
    val overlapMs: Long = 0L,                           // Overlap with previous chunk (~2s)
    val transcriptionState: String = TranscriptionState.PENDING,  // Current transcription status
    val transcriptText: String? = null,                 // Transcribed text (filled by Gemini)
    val retryCount: Int = 0,                            // How many times transcription was retried
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Possible states for chunk transcription.
 */
object TranscriptionState {
    const val PENDING = "PENDING"           // Waiting to be sent to Gemini
    const val IN_PROGRESS = "IN_PROGRESS"   // Currently being transcribed
    const val COMPLETED = "COMPLETED"       // Successfully transcribed
    const val FAILED = "FAILED"             // Transcription failed after retries
}
