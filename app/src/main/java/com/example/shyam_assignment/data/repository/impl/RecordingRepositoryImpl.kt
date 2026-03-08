package com.example.shyam_assignment.data.repository.impl

import com.example.shyam_assignment.data.local.dao.AudioChunkDao
import com.example.shyam_assignment.data.local.dao.RecordingSessionDao
import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SessionStatus
import com.example.shyam_assignment.data.local.entity.TranscriptionState
import com.example.shyam_assignment.data.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RecordingRepository.
 * Simply delegates all calls to the Room DAOs.
 * This thin wrapper follows the repository pattern for testability.
 */
@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val sessionDao: RecordingSessionDao,   // DAO for recording sessions
    private val chunkDao: AudioChunkDao             // DAO for audio chunks
) : RecordingRepository {

    override fun getAllSessions(): Flow<List<RecordingSessionEntity>> =
        sessionDao.getAllSessionsFlow()

    override fun getSessionById(sessionId: String): Flow<RecordingSessionEntity?> =
        sessionDao.getSessionByIdFlow(sessionId)

    override suspend fun getSessionByIdOnce(sessionId: String): RecordingSessionEntity? =
        sessionDao.getSessionById(sessionId)

    override fun getActiveSession(): Flow<RecordingSessionEntity?> =
        sessionDao.getActiveSessionFlow()

    override suspend fun insertSession(session: RecordingSessionEntity) =
        sessionDao.insert(session)

    override suspend fun updateSession(session: RecordingSessionEntity) =
        sessionDao.update(session)

    override suspend fun deleteSession(sessionId: String) =
        sessionDao.deleteSession(sessionId)

    override suspend fun insertChunk(chunk: AudioChunkEntity) =
        chunkDao.insert(chunk)

    override suspend fun getChunkById(chunkId: String): AudioChunkEntity? =
        chunkDao.getChunkById(chunkId)

    override fun getChunksBySession(sessionId: String): Flow<List<AudioChunkEntity>> =
        chunkDao.getChunksBySessionFlow(sessionId)

    override suspend fun updateChunkTranscription(chunkId: String, state: String, text: String?) =
        chunkDao.updateTranscription(chunkId, state, text)

    override suspend fun incrementChunkRetry(chunkId: String) =
        chunkDao.incrementRetryCount(chunkId)

    /** Gets sessions stuck in RECORDING/PAUSED — for crash recovery */
    override suspend fun getInterruptedSessions(): List<RecordingSessionEntity> =
        sessionDao.getSessionsByStatuses(listOf(SessionStatus.RECORDING, SessionStatus.PAUSED))

    /** Gets chunks still waiting for transcription — for crash recovery */
    override suspend fun getPendingChunks(): List<AudioChunkEntity> =
        chunkDao.getChunksByTranscriptionStates(
            listOf(TranscriptionState.PENDING, TranscriptionState.IN_PROGRESS)
        )
}
