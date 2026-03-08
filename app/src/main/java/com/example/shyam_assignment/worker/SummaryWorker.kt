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
import com.example.shyam_assignment.data.local.entity.SessionStatus
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.SummaryStatus
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.data.repository.SummaryRepository
import com.example.shyam_assignment.data.repository.TranscriptRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that generates a meeting summary using Gemini 2.5 Flash.
 *
 * Flow:
 * 1. Receives sessionId as input
 * 2. Fetches the full ordered transcript from Room
 * 3. Sends it to Gemini API with a structured JSON prompt
 * 4. Saves the result (title, summary, action items, key points) into Room
 * 5. Marks the session as COMPLETED
 * 6. Retries up to 3 times on failure with exponential backoff
 *
 * Enqueued by SummaryViewModel or RecoveryManager.
 */
@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val geminiApiService: GeminiApiService,        // Calls Gemini API
    private val transcriptRepository: TranscriptRepository, // Reads transcript from Room
    private val summaryRepository: SummaryRepository,       // Saves summary to Room
    private val recordingRepository: RecordingRepository    // Updates session status
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SummaryWorker"
        const val KEY_SESSION_ID = "session_id"
        private const val MAX_RETRIES = 3

        /**
         * Enqueues summary generation for a session.
         * Uses KEEP policy — won't duplicate if already queued.
         */
        fun enqueue(context: Context, sessionId: String) {
            val workRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    20,
                    TimeUnit.SECONDS
                )
                .addTag("summary")
                .addTag("session_$sessionId")
                .build()

            // KEEP = if work for this session already exists, skip
            WorkManager.getInstance(context).enqueueUniqueWork(
                "summary_$sessionId",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Enqueued summary generation for session=$sessionId")
        }

        /**
         * Force-enqueues by replacing any existing work — used for Retry button.
         */
        fun enqueueReplace(context: Context, sessionId: String) {
            val workRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf(KEY_SESSION_ID to sessionId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    20,
                    TimeUnit.SECONDS
                )
                .addTag("summary")
                .addTag("session_$sessionId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "summary_$sessionId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.d(TAG, "Enqueued (REPLACE) summary generation for session=$sessionId")
        }
    }

    private val gson = Gson()

    /**
     * Main work method — called by WorkManager.
     * Steps: check if done → fetch transcript → call Gemini → save summary → mark completed
     */
    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
            ?: return Result.failure()

        Log.d(TAG, "Starting summary generation for session=$sessionId (attempt ${runAttemptCount + 1})")

        // 0. Early-exit if summary is already completed (prevents duplicate API calls)
        val existing = summaryRepository.getSummaryBySessionOnce(sessionId)
        if (existing?.status == SummaryStatus.COMPLETED) {
            Log.d(TAG, "Summary already completed for session=$sessionId — skipping")
            // Also ensure session is marked COMPLETED
            markSessionCompleted(sessionId)
            return Result.success()
        }

        // 1. Mark as generating
        val summaryEntity = existing ?: SummaryEntity(sessionId = sessionId)
        summaryRepository.insertOrUpdateSummary(
            summaryEntity.copy(
                status = SummaryStatus.GENERATING,
                errorMessage = null,
                updatedAt = System.currentTimeMillis()
            )
        )

        // 2. Fetch ordered transcript from Room
        val segments = transcriptRepository.getTranscriptBySessionOnce(sessionId)
        if (segments.isEmpty()) {
            Log.w(TAG, "No transcript segments found for session=$sessionId")
            summaryRepository.insertOrUpdateSummary(
                summaryEntity.copy(
                    status = SummaryStatus.FAILED,
                    errorMessage = "No transcript available to summarize",
                    updatedAt = System.currentTimeMillis()
                )
            )
            return Result.failure()
        }

        val fullTranscript = segments.joinToString("\n") { it.text }
        Log.d(TAG, "Transcript length: ${fullTranscript.length} chars from ${segments.size} segments")

        // 3. Call Gemini API
        val result = geminiApiService.generateSummary(fullTranscript)

        return result.fold(
            onSuccess = { summaryResponse ->
                Log.d(TAG, "Summary generated successfully: title='${summaryResponse.title}'")

                summaryRepository.insertOrUpdateSummary(
                    SummaryEntity(
                        sessionId = sessionId,
                        title = summaryResponse.title,
                        summary = summaryResponse.summary,
                        actionItemsJson = gson.toJson(summaryResponse.actionItems),
                        keyPointsJson = gson.toJson(summaryResponse.keyPoints),
                        status = SummaryStatus.COMPLETED,
                        errorMessage = null,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // ── Mark session as COMPLETED ──
                markSessionCompleted(sessionId)

                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "Summary generation failed for session=$sessionId", error)

                if (runAttemptCount < MAX_RETRIES) {
                    Log.d(TAG, "Will retry (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
                    summaryRepository.insertOrUpdateSummary(
                        summaryEntity.copy(
                            status = SummaryStatus.GENERATING,
                            errorMessage = "Retrying... (${error.message})",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    Result.retry()
                } else {
                    Log.e(TAG, "Max retries reached for session=$sessionId")
                    summaryRepository.insertOrUpdateSummary(
                        summaryEntity.copy(
                            status = SummaryStatus.FAILED,
                            errorMessage = error.message ?: "Summary generation failed",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    Result.failure()
                }
            }
        )
    }

    /**
     * Marks the recording session as COMPLETED in Room so the dashboard
     * shows the correct status chip.
     */
    private suspend fun markSessionCompleted(sessionId: String) {
        try {
            val session = recordingRepository.getSessionByIdOnce(sessionId)
            if (session != null && session.status != SessionStatus.COMPLETED) {
                recordingRepository.updateSession(
                    session.copy(
                        status = SessionStatus.COMPLETED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Log.d(TAG, "Session $sessionId marked as COMPLETED")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark session completed", e)
        }
    }
}
