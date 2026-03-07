package com.example.shyam_assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RecordingSessionEntity)

    @Update
    suspend fun update(session: RecordingSessionEntity)

    @Query("SELECT * FROM recording_sessions ORDER BY startedAt DESC")
    fun getAllSessionsFlow(): Flow<List<RecordingSessionEntity>>

    @Query("SELECT * FROM recording_sessions WHERE sessionId = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<RecordingSessionEntity?>

    @Query("SELECT * FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): RecordingSessionEntity?

    @Query("SELECT * FROM recording_sessions WHERE status IN ('RECORDING', 'PAUSED') LIMIT 1")
    fun getActiveSessionFlow(): Flow<RecordingSessionEntity?>

    @Query("DELETE FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}

