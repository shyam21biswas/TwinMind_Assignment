package com.example.shyam_assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for audio chunks.
 * Each recording session has multiple 30-second chunks.
 */
@Dao
interface AudioChunkDao {

    /** Insert or replace a chunk */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunkEntity)

    /** Get a chunk by its ID (one-time read) */
    @Query("SELECT * FROM audio_chunks WHERE chunkId = :chunkId")
    suspend fun getChunkById(chunkId: String): AudioChunkEntity?

    /** Get all chunks for a session, ordered by chunk number (live Flow) */
    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    fun getChunksBySessionFlow(sessionId: String): Flow<List<AudioChunkEntity>>

    /** Get all chunks for a session, ordered by chunk number (one-time read) */
    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    suspend fun getChunksBySession(sessionId: String): List<AudioChunkEntity>

    /** Update a chunk's transcription status and text */
    @Query("UPDATE audio_chunks SET transcriptionState = :state, transcriptText = :text, updatedAt = :updatedAt WHERE chunkId = :chunkId")
    suspend fun updateTranscription(chunkId: String, state: String, text: String?, updatedAt: Long = System.currentTimeMillis())

    /** Increment the retry count when transcription fails */
    @Query("UPDATE audio_chunks SET retryCount = retryCount + 1, updatedAt = :updatedAt WHERE chunkId = :chunkId")
    suspend fun incrementRetryCount(chunkId: String, updatedAt: Long = System.currentTimeMillis())

    /** Delete all chunks for a session */
    @Query("DELETE FROM audio_chunks WHERE sessionId = :sessionId")
    suspend fun deleteChunksBySession(sessionId: String)

    /** Get chunks that still need transcription — used for recovery */
    @Query("SELECT * FROM audio_chunks WHERE transcriptionState IN (:states)")
    suspend fun getChunksByTranscriptionStates(states: List<String>): List<AudioChunkEntity>
}
