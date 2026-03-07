package com.example.shyam_assignment.data.repository.impl

import com.example.shyam_assignment.data.local.dao.SummaryDao
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.repository.SummaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao
) : SummaryRepository {

    override fun getSummaryBySession(sessionId: String): Flow<SummaryEntity?> =
        summaryDao.getSummaryBySessionFlow(sessionId)

    override suspend fun getSummaryBySessionOnce(sessionId: String): SummaryEntity? =
        summaryDao.getSummaryBySession(sessionId)

    override suspend fun insertOrUpdateSummary(summary: SummaryEntity) =
        summaryDao.insertOrUpdate(summary)

    override suspend fun deleteSummary(sessionId: String) =
        summaryDao.deleteSummary(sessionId)
}

