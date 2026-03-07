package com.example.shyam_assignment.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val title: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationMs: Long = 0L,
    val status: String = SessionStatus.IDLE,
    val warningMessage: String? = null,
    val errorMessage: String? = null,
    val activeInputSource: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object SessionStatus {
    const val IDLE = "IDLE"
    const val RECORDING = "RECORDING"
    const val PAUSED = "PAUSED"
    const val STOPPED = "STOPPED"
    const val TRANSCRIBING = "TRANSCRIBING"
    const val SUMMARIZING = "SUMMARIZING"
    const val COMPLETED = "COMPLETED"
    const val ERROR = "ERROR"
}

