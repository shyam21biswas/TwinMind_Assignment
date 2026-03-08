package com.example.shyam_assignment.data.repository.impl

import com.example.shyam_assignment.data.local.dao.SummaryDao
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.SummaryStatus
import com.example.shyam_assignment.data.repository.SummaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SummaryRepository.
 * Delegates all calls to the SummaryDao.
 */
@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao // DAO for summaries
) : SummaryRepository {

    override fun getSummaryBySession(sessionId: String): Flow<SummaryEntity?> =
        summaryDao.getSummaryBySessionFlow(sessionId)

    override suspend fun getSummaryBySessionOnce(sessionId: String): SummaryEntity? =
        summaryDao.getSummaryBySession(sessionId)

    override suspend fun insertOrUpdateSummary(summary: SummaryEntity) =
        summaryDao.insertOrUpdate(summary)

    override suspend fun deleteSummary(sessionId: String) =
        summaryDao.deleteSummary(sessionId)

    /** Gets summaries still PENDING or GENERATING — used for crash recovery */
    override suspend fun getPendingSummaries(): List<SummaryEntity> =
        summaryDao.getSummariesByStatuses(
            listOf(SummaryStatus.PENDING, SummaryStatus.GENERATING)
        )
}
