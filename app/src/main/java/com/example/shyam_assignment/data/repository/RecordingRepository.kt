package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for recording sessions and audio chunks.
 * Acts as the single source of truth — ViewModels talk to this, not DAOs directly.
 */
interface RecordingRepository {

    /** Get all sessions ordered by newest first (live updates) */
    fun getAllSessions(): Flow<List<RecordingSessionEntity>>

    /** Get a specific session by ID (live updates) */
    fun getSessionById(sessionId: String): Flow<RecordingSessionEntity?>

    /** Get a specific session by ID (one-time read) */
    suspend fun getSessionByIdOnce(sessionId: String): RecordingSessionEntity?

    /** Get the currently active recording session */
    fun getActiveSession(): Flow<RecordingSessionEntity?>

    /** Save a new session to the database */
    suspend fun insertSession(session: RecordingSessionEntity)

    /** Update an existing session */
    suspend fun updateSession(session: RecordingSessionEntity)

    /** Delete a session and all its chunks */
    suspend fun deleteSession(sessionId: String)

    // ── Audio chunk operations ──

    /** Save a new audio chunk to the database */
    suspend fun insertChunk(chunk: AudioChunkEntity)

    /** Get a chunk by its ID */
    suspend fun getChunkById(chunkId: String): AudioChunkEntity?

    /** Get all chunks for a session (live updates) */
    fun getChunksBySession(sessionId: String): Flow<List<AudioChunkEntity>>

    /** Update a chunk's transcription status and text */
    suspend fun updateChunkTranscription(chunkId: String, state: String, text: String?)

    /** Increment a chunk's retry count after a failed transcription */
    suspend fun incrementChunkRetry(chunkId: String)

    // ── Recovery helpers ──

    /** Get sessions stuck in RECORDING/PAUSED (for crash recovery) */
    suspend fun getInterruptedSessions(): List<RecordingSessionEntity>

    /** Get chunks that are still waiting for transcription */
    suspend fun getPendingChunks(): List<AudioChunkEntity>
}
