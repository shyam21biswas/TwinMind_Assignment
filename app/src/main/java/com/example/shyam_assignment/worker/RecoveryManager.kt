package com.example.shyam_assignment.worker

import android.content.Context
import android.util.Log
import com.example.shyam_assignment.data.local.entity.SessionStatus
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.data.repository.SummaryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles recovery after app crash / kill / force-stop.
 * Called once from Application.onCreate().
 *
 * Responsibilities:
 * 1. Finalize interrupted recording sessions (RECORDING / PAUSED → STOPPED)
 * 2. Re-enqueue pending chunk transcriptions via WorkManager
 * 3. Re-enqueue pending summary generations via WorkManager
 */
@Singleton
class RecoveryManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingRepository: RecordingRepository,
    private val summaryRepository: SummaryRepository
) {

    companion object {
        private const val TAG = "RecoveryManager"
    }

    /**
     * Should be called from Application.onCreate() on a background thread.
     * Safe to call multiple times — idempotent.
     */
    suspend fun recover() {
        try {
            recoverInterruptedSessions()
            recoverPendingTranscriptions()
            recoverPendingSummaries()
            Log.i(TAG, "Recovery completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed", e)
        }
    }

    /**
     * Finds sessions stuck in RECORDING/PAUSED and marks them STOPPED.
     */
    private suspend fun recoverInterruptedSessions() {
        val interrupted = recordingRepository.getInterruptedSessions()
        if (interrupted.isEmpty()) return

        Log.i(TAG, "Found ${interrupted.size} interrupted session(s)")

        for (session in interrupted) {
            val now = System.currentTimeMillis()
            val durationMs = if (session.endedAt != null) {
                session.endedAt - session.startedAt
            } else {
                // Best estimate: use updatedAt as approximate end time
                (session.updatedAt - session.startedAt).coerceAtLeast(session.durationMs)
            }

            recordingRepository.updateSession(
                session.copy(
                    status = SessionStatus.STOPPED,
                    endedAt = session.endedAt ?: session.updatedAt,
                    durationMs = durationMs,
                    warningMessage = "Session recovered after interruption",
                    updatedAt = now
                )
            )
            Log.i(TAG, "Recovered session ${session.sessionId}: ${session.title}")
        }
    }

    /**
     * Re-enqueues chunk transcriptions that were PENDING or IN_PROGRESS.
     * ChunkTranscriptionWorker also has an early-exit if already COMPLETED.
     */
    private suspend fun recoverPendingTranscriptions() {
        val pendingChunks = recordingRepository.getPendingChunks()
        if (pendingChunks.isEmpty()) return

        Log.i(TAG, "Re-enqueuing ${pendingChunks.size} pending transcription(s)")

        for (chunk in pendingChunks) {
            // Double-check it's not completed
            val current = recordingRepository.getChunkById(chunk.chunkId)
            if (current?.transcriptionState == "COMPLETED") {
                Log.d(TAG, "Chunk ${chunk.chunkId} already transcribed — skipping")
                continue
            }
            ChunkTranscriptionWorker.enqueue(
                context = context,
                chunkId = chunk.chunkId,
                sessionId = chunk.sessionId
            )
            Log.d(TAG, "Re-enqueued transcription for chunk ${chunk.chunkId}")
        }
    }

    /**
     * Re-enqueues summary generation that was PENDING or GENERATING.
     * Skips if already COMPLETED (SummaryWorker also checks this).
     */
    private suspend fun recoverPendingSummaries() {
        val pendingSummaries = summaryRepository.getPendingSummaries()
        if (pendingSummaries.isEmpty()) return

        Log.i(TAG, "Re-enqueuing ${pendingSummaries.size} pending summary(ies)")

        for (summary in pendingSummaries) {
            // Double-check it's not completed (race condition guard)
            val current = summaryRepository.getSummaryBySessionOnce(summary.sessionId)
            if (current?.status == "COMPLETED") {
                Log.d(TAG, "Summary for ${summary.sessionId} already completed — skipping")
                continue
            }
            SummaryWorker.enqueue(
                context = context,
                sessionId = summary.sessionId
            )
            Log.d(TAG, "Re-enqueued summary for session ${summary.sessionId}")
        }
    }
}

