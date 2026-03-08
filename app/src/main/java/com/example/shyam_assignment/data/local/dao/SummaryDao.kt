package com.example.shyam_assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: SummaryEntity)

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun getSummaryBySessionFlow(sessionId: String): Flow<SummaryEntity?>

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    suspend fun getSummaryBySession(sessionId: String): SummaryEntity?

    @Query("DELETE FROM summaries WHERE sessionId = :sessionId")
    suspend fun deleteSummary(sessionId: String)

    @Query("SELECT * FROM summaries WHERE status IN (:statuses)")
    suspend fun getSummariesByStatuses(statuses: List<String>): List<SummaryEntity>
}

