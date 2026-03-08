package com.example.shyam_assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for transcript segments.
 * Each segment holds the text from one transcribed audio chunk.
 */
@Dao
interface TranscriptSegmentDao {

    /** Insert or replace a segment */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: TranscriptSegmentEntity)

    /** Insert or replace multiple segments at once */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<TranscriptSegmentEntity>)

    /** Get all segments for a session, ordered by chunk number (live Flow) */
    @Query("SELECT * FROM transcript_segments WHERE sessionId = :sessionId ORDER BY chunkIndex ASC, createdAt ASC")
    fun getSegmentsBySessionFlow(sessionId: String): Flow<List<TranscriptSegmentEntity>>

    /** Get all segments for a session, ordered by chunk number (one-time read) */
    @Query("SELECT * FROM transcript_segments WHERE sessionId = :sessionId ORDER BY chunkIndex ASC, createdAt ASC")
    suspend fun getSegmentsBySession(sessionId: String): List<TranscriptSegmentEntity>

    /** Delete all segments for a session */
    @Query("DELETE FROM transcript_segments WHERE sessionId = :sessionId")
    suspend fun deleteSegmentsBySession(sessionId: String)
}
