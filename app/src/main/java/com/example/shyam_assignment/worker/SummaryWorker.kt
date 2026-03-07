package com.example.shyam_assignment.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.shyam_assignment.data.api.GeminiApiService
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.SummaryStatus
import com.example.shyam_assignment.data.repository.SummaryRepository
import com.example.shyam_assignment.data.repository.TranscriptRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val geminiApiService: GeminiApiService,
    private val transcriptRepository: TranscriptRepository,
    private val summaryRepository: SummaryRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "SummaryWorker"
        const val KEY_SESSION_ID = "session_id"
        private const val MAX_RETRIES = 3

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

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Enqueued summary generation for session=$sessionId")
        }
    }

    private val gson = Gson()

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
            ?: return Result.failure()

        Log.d(TAG, "Starting summary generation for session=$sessionId (attempt ${runAttemptCount + 1})")

        // 1. Mark as generating
        val existing = summaryRepository.getSummaryBySessionOnce(sessionId)
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
}

