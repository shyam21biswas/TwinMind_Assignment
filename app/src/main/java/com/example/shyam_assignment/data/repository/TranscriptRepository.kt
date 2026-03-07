package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

interface TranscriptRepository {

    fun getTranscriptBySession(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    suspend fun getTranscriptBySessionOnce(sessionId: String): List<TranscriptSegmentEntity>

    suspend fun insertSegment(segment: TranscriptSegmentEntity)

    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    suspend fun deleteTranscriptBySession(sessionId: String)
}

