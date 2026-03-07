package com.example.shyam_assignment.data.repository.impl

import com.example.shyam_assignment.data.local.dao.TranscriptSegmentDao
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import com.example.shyam_assignment.data.repository.TranscriptRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptDao: TranscriptSegmentDao
) : TranscriptRepository {

    override fun getTranscriptBySession(sessionId: String): Flow<List<TranscriptSegmentEntity>> =
        transcriptDao.getSegmentsBySessionFlow(sessionId)

    override suspend fun getTranscriptBySessionOnce(sessionId: String): List<TranscriptSegmentEntity> =
        transcriptDao.getSegmentsBySession(sessionId)

    override suspend fun insertSegment(segment: TranscriptSegmentEntity) =
        transcriptDao.insert(segment)

    override suspend fun insertSegments(segments: List<TranscriptSegmentEntity>) =
        transcriptDao.insertAll(segments)

    override suspend fun deleteTranscriptBySession(sessionId: String) =
        transcriptDao.deleteSegmentsBySession(sessionId)
}

