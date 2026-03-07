package com.example.shyam_assignment.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val id: String,
    val sessionId: String,
    val chunkId: String,
    val chunkIndex: Int,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

