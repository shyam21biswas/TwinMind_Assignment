package com.example.shyam_assignment.data.repository

import com.example.shyam_assignment.data.local.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {

    fun getSummaryBySession(sessionId: String): Flow<SummaryEntity?>

    suspend fun getSummaryBySessionOnce(sessionId: String): SummaryEntity?

    suspend fun insertOrUpdateSummary(summary: SummaryEntity)

    suspend fun deleteSummary(sessionId: String)
}

