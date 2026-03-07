package com.example.shyam_assignment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: AudioChunkEntity)

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    fun getChunksBySessionFlow(sessionId: String): Flow<List<AudioChunkEntity>>

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId ORDER BY chunkIndex ASC")
    suspend fun getChunksBySession(sessionId: String): List<AudioChunkEntity>

    @Query("UPDATE audio_chunks SET transcriptionState = :state, transcriptText = :text, updatedAt = :updatedAt WHERE chunkId = :chunkId")
    suspend fun updateTranscription(chunkId: String, state: String, text: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE audio_chunks SET retryCount = retryCount + 1, updatedAt = :updatedAt WHERE chunkId = :chunkId")
    suspend fun incrementRetryCount(chunkId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM audio_chunks WHERE sessionId = :sessionId")
    suspend fun deleteChunksBySession(sessionId: String)
}

