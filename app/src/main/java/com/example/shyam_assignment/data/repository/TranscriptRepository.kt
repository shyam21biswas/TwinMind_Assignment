package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for transcript segments.
 * Provides access to transcribed text from audio chunks.
 */
interface TranscriptRepository {

    /** Get all transcript segments for a session, ordered by chunk index (live updates) */
    fun getTranscriptBySession(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    /** Get all transcript segments for a session (one-time read) */
    suspend fun getTranscriptBySessionOnce(sessionId: String): List<TranscriptSegmentEntity>

    /** Save a single transcript segment */
    suspend fun insertSegment(segment: TranscriptSegmentEntity)

    /** Save multiple transcript segments at once */
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)

    /** Delete all transcript segments for a session */
    suspend fun deleteTranscriptBySession(sessionId: String)
}
