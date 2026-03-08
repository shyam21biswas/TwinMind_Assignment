package com.example.shyam_assignment.ui.screens.summary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.SummaryStatus
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.data.repository.SummaryRepository
import com.example.shyam_assignment.data.repository.TranscriptRepository
import com.example.shyam_assignment.worker.SummaryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Summary/Meeting Details screen.
 * Loads session data, transcript, and summary from Room.
 * Auto-triggers summary generation via Gemini when needed.
 */
@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,                    // Contains the meetingId from navigation
    private val recordingRepository: RecordingRepository,  // Access to session data
    private val summaryRepository: SummaryRepository,      // Access to summary data
    private val transcriptRepository: TranscriptRepository // Access to transcript data
) : ViewModel() {

    /** The session ID passed from the navigation route */
    val sessionId: String = checkNotNull(savedStateHandle["meetingId"])

    /**
     * The UI state — combines session, summary, and transcript data.
     * Updates automatically when any of them change in Room.
     */
    val uiState = combine(
        recordingRepository.getSessionById(sessionId),       // Session metadata
        summaryRepository.getSummaryBySession(sessionId),    // AI summary (may be null)
        transcriptRepository.getTranscriptBySession(sessionId) // Transcript segments
    ) { session, summary, transcript ->
        val isGenerating = summary?.status == SummaryStatus.GENERATING
        val summaryError = if (summary?.status == SummaryStatus.FAILED)
            summary.errorMessage ?: "Summary generation failed"
        else null

        SummaryUiState(
            session = session,
            summary = summary,
            transcript = transcript,
            isLoading = session == null && summaryError == null,
            error = if (session == null) "Session not found" else summaryError,
            isSummaryGenerating = isGenerating,
            summaryStatus = summary?.status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState(isLoading = true)
    )

    /** Prevents calling generateSummary multiple times simultaneously */
    private var generationTriggered = false

    /**
     * Triggers summary generation using Gemini AI.
     * Called automatically when the summary screen opens.
     * Skips if summary is already COMPLETED or currently GENERATING.
     * Skips if there's no transcript to summarize.
     */
    fun generateSummary(context: Context) {
        if (generationTriggered) return
        generationTriggered = true

        viewModelScope.launch {
            try {
                val existing = summaryRepository.getSummaryBySessionOnce(sessionId)

                // Already done or in progress — skip
                if (existing?.status == SummaryStatus.COMPLETED) return@launch
                if (existing?.status == SummaryStatus.GENERATING) return@launch

                // No transcript available — nothing to summarize
                val segments = transcriptRepository.getTranscriptBySessionOnce(sessionId)
                if (segments.isEmpty()) return@launch

                // Mark as "generating" in Room so UI shows loading state
                summaryRepository.insertOrUpdateSummary(
                    SummaryEntity(
                        sessionId = sessionId,
                        status = SummaryStatus.GENERATING,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Enqueue WorkManager job to call Gemini API
                SummaryWorker.enqueue(context, sessionId)
            } finally {
                generationTriggered = false  // Allow retry button to work
            }
        }
    }

    /**
     * Retries summary generation after a failure.
     * Resets the status to GENERATING and enqueues a fresh worker.
     */
    fun retrySummary(context: Context) {
        viewModelScope.launch {
            // Reset status to allow re-generation
            summaryRepository.insertOrUpdateSummary(
                SummaryEntity(
                    sessionId = sessionId,
                    status = SummaryStatus.GENERATING,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
            SummaryWorker.enqueueReplace(context, sessionId)
        }
    }
}
