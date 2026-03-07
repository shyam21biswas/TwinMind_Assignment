package com.example.shyam_assignment.data.repository.impl

import com.example.shyam_assignment.data.local.dao.AudioChunkDao
import com.example.shyam_assignment.data.local.dao.RecordingSessionDao
import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val sessionDao: RecordingSessionDao,
    private val chunkDao: AudioChunkDao
) : RecordingRepository {

    override fun getAllSessions(): Flow<List<RecordingSessionEntity>> =
        sessionDao.getAllSessionsFlow()

    override fun getSessionById(sessionId: String): Flow<RecordingSessionEntity?> =
        sessionDao.getSessionByIdFlow(sessionId)

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

    override fun getChunksBySession(sessionId: String): Flow<List<AudioChunkEntity>> =
        chunkDao.getChunksBySessionFlow(sessionId)

    override suspend fun updateChunkTranscription(chunkId: String, state: String, text: String?) =
        chunkDao.updateTranscription(chunkId, state, text)

    override suspend fun incrementChunkRetry(chunkId: String) =
        chunkDao.incrementRetryCount(chunkId)
}

