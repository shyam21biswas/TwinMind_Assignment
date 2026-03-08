package com.example.shyam_assignment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for one transcript segment.
 * Each audio chunk produces one segment after Gemini transcription.
 * Segments are ordered by chunkIndex to form the full transcript.
 */
@Entity(
    tableName = "transcript_segments",
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
data class TranscriptSegmentEntity(
    @PrimaryKey
    val id: String,                 // Unique segment ID
    val sessionId: String,          // Which session this belongs to
    val chunkId: String,            // Which audio chunk produced this text
    val chunkIndex: Int,            // Order number — used to sort transcript
    val text: String,               // The transcribed text from Gemini
    val createdAt: Long = System.currentTimeMillis()
)
