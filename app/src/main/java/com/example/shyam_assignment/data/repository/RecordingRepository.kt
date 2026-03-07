package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {

    fun getAllSessions(): Flow<List<RecordingSessionEntity>>

    fun getSessionById(sessionId: String): Flow<RecordingSessionEntity?>

    suspend fun getSessionByIdOnce(sessionId: String): RecordingSessionEntity?

    fun getActiveSession(): Flow<RecordingSessionEntity?>

    suspend fun insertSession(session: RecordingSessionEntity)

    suspend fun updateSession(session: RecordingSessionEntity)

    suspend fun deleteSession(sessionId: String)

    // Audio chunk operations
    suspend fun insertChunk(chunk: AudioChunkEntity)

    suspend fun getChunkById(chunkId: String): AudioChunkEntity?

    fun getChunksBySession(sessionId: String): Flow<List<AudioChunkEntity>>

    suspend fun updateChunkTranscription(chunkId: String, state: String, text: String?)

    suspend fun incrementChunkRetry(chunkId: String)
}


