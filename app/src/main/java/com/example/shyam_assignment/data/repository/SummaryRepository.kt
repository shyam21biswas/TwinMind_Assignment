package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.local.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for AI-generated summaries.
 * One summary per session — stores title, summary text, action items, key points.
 */
interface SummaryRepository {

    /** Get a session's summary (live updates — UI auto-refreshes) */
    fun getSummaryBySession(sessionId: String): Flow<SummaryEntity?>

    /** Get a session's summary (one-time read — used in workers) */
    suspend fun getSummaryBySessionOnce(sessionId: String): SummaryEntity?

    /** Insert a new summary or update an existing one */
    suspend fun insertOrUpdateSummary(summary: SummaryEntity)

    /** Delete a session's summary */
    suspend fun deleteSummary(sessionId: String)

    /** Get summaries still pending/generating — used for crash recovery */
    suspend fun getPendingSummaries(): List<SummaryEntity>
}
