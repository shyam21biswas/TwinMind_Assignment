package com.example.shyam_assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for recording sessions.
 * Handles CRUD operations and queries for session data in Room.
 */
@Dao
interface RecordingSessionDao {

    /** Insert or replace a session */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RecordingSessionEntity)

    /** Update an existing session */
    @Update
    suspend fun update(session: RecordingSessionEntity)

    /** Get all sessions, newest first (live updates via Flow) */
    @Query("SELECT * FROM recording_sessions ORDER BY startedAt DESC")
    fun getAllSessionsFlow(): Flow<List<RecordingSessionEntity>>

    /** Get a specific session by ID (live updates via Flow) */
    @Query("SELECT * FROM recording_sessions WHERE sessionId = :sessionId")
    fun getSessionByIdFlow(sessionId: String): Flow<RecordingSessionEntity?>

    /** Get a specific session by ID (one-time read) */
    @Query("SELECT * FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): RecordingSessionEntity?

    /** Get the currently active recording session, if any */
    @Query("SELECT * FROM recording_sessions WHERE status IN ('RECORDING', 'PAUSED') LIMIT 1")
    fun getActiveSessionFlow(): Flow<RecordingSessionEntity?>

    /** Get sessions by status list — used for recovery */
    @Query("SELECT * FROM recording_sessions WHERE status IN (:statuses)")
    suspend fun getSessionsByStatuses(statuses: List<String>): List<RecordingSessionEntity>

    /** Delete a session by ID */
    @Query("DELETE FROM recording_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
