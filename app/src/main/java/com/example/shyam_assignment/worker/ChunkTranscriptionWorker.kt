package com.example.shyam_assignment.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.shyam_assignment.data.api.GeminiApiService
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import com.example.shyam_assignment.data.local.entity.TranscriptionState
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.data.repository.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that transcribes a single audio chunk using Gemini 2.5 Flash.
 *
 * Flow:
 * 1. Receives chunkId and sessionId as input
 * 2. Reads the audio chunk file from disk
 * 3. Sends it to Gemini API for transcription
 * 4. Saves the transcript text into Room
 * 5. Retries up to 3 times on failure with exponential backoff
 *
 * Enqueued automatically by RecordingService when a chunk is finalized.
 */
@HiltWorker
class ChunkTranscriptionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val geminiApiService: GeminiApiService,        // Calls Gemini API
    private val recordingRepository: RecordingRepository,  // Access to chunk data in Room
    private val transcriptRepository: TranscriptRepository // Saves transcript segments
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "ChunkTranscriptionWorker"
        const val KEY_CHUNK_ID = "chunk_id"
        const val KEY_SESSION_ID = "session_id"
        private const val MAX_RETRIES = 3

        /**
         * Enqueues a transcription job for the given chunk.
         * Uses KEEP policy — won't duplicate if a job for this chunk already exists.
         * Requires network. Retries with exponential backoff (15s, 30s, 60s).
         */
        fun enqueue(context: Context, chunkId: String, sessionId: String) {
            val workRequest = OneTimeWorkRequestBuilder<ChunkTranscriptionWorker>()
                .setInputData(
                    workDataOf(
                        KEY_CHUNK_ID to chunkId,
                        KEY_SESSION_ID to sessionId
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .addTag("transcription")
                .addTag("session_$sessionId")
                .build()

            // KEEP = if work for this chunk already exists, skip
            WorkManager.getInstance(context).enqueueUniqueWork(
                "transcribe_$chunkId",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Enqueued transcription for chunk=$chunkId session=$sessionId")
        }
    }

    /**
     * Main work method — called by WorkManager.
     * Steps: load chunk → check if already done → read audio file → call Gemini → save result
     */
    override suspend fun doWork(): Result {
        val chunkId = inputData.getString(KEY_CHUNK_ID)
            ?: return Result.failure()
        val sessionId = inputData.getString(KEY_SESSION_ID)
            ?: return Result.failure()

        Log.d(TAG, "Starting transcription for chunk=$chunkId (attempt ${runAttemptCount + 1})")

        // 1. Fetch chunk from Room
        val chunk = recordingRepository.getChunkById(chunkId)
        if (chunk == null) {
            Log.e(TAG, "Chunk $chunkId not found in database")
            return Result.failure()
        }

        // 1b. Skip if already transcribed (prevents duplicate API calls)
        if (chunk.transcriptionState == TranscriptionState.COMPLETED) {
            Log.d(TAG, "Chunk $chunkId already transcribed — skipping")
            return Result.success()
        }

        // 2. Check audio file exists
        val audioFile = File(chunk.filePath)
        if (!audioFile.exists() || audioFile.length() == 0L) {
            Log.e(TAG, "Audio file missing or empty: ${chunk.filePath}")
            recordingRepository.updateChunkTranscription(
                chunkId, TranscriptionState.FAILED, null
            )
            return Result.failure()
        }

        // 3. Mark as in-progress
        recordingRepository.updateChunkTranscription(
            chunkId, TranscriptionState.IN_PROGRESS, null
        )

        // 4. Call Gemini API
        val result = geminiApiService.transcribeAudio(audioFile)

        return result.fold(
            onSuccess = { transcript ->
                Log.d(TAG, "Transcription succeeded for chunk=$chunkId (${transcript.length} chars)")

                // 5a. Save transcript text on the chunk
                recordingRepository.updateChunkTranscription(
                    chunkId, TranscriptionState.COMPLETED, transcript
                )

                // 5b. Save as TranscriptSegment for ordered display
                transcriptRepository.insertSegment(
                    TranscriptSegmentEntity(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        chunkId = chunkId,
                        chunkIndex = chunk.chunkIndex,
                        text = transcript,
                        createdAt = System.currentTimeMillis()
                    )
                )

                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "Transcription failed for chunk=$chunkId", error)

                recordingRepository.incrementChunkRetry(chunkId)

                if (runAttemptCount < MAX_RETRIES) {
                    // Retry with backoff
                    Log.d(TAG, "Will retry (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                    recordingRepository.updateChunkTranscription(
                        chunkId, TranscriptionState.PENDING, null
                    )
                    Result.retry()
                } else {
                    // Give up
                    Log.e(TAG, "Max retries reached for chunk=$chunkId")
                    recordingRepository.updateChunkTranscription(
                        chunkId, TranscriptionState.FAILED, null
                    )
                    Result.failure()
                }
            }
        )
    }
}
