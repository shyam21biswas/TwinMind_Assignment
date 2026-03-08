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

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val summaryRepository: SummaryRepository,
    private val transcriptRepository: TranscriptRepository
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["meetingId"])

    val uiState = combine(
        recordingRepository.getSessionById(sessionId),
        summaryRepository.getSummaryBySession(sessionId),
        transcriptRepository.getTranscriptBySession(sessionId)
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

    /** Guards against multiple simultaneous generateSummary calls */
    private var generationTriggered = false

    /**
     * Triggers summary generation. Called automatically when entering SummaryScreen.
     * Will NOT re-call Gemini if summary is already COMPLETED or GENERATING.
     */
    fun generateSummary(context: Context) {
        if (generationTriggered) return
        generationTriggered = true

        viewModelScope.launch {
            try {
                // Check current summary status in Room
                val existing = summaryRepository.getSummaryBySessionOnce(sessionId)

                // Already done — nothing to do
                if (existing?.status == SummaryStatus.COMPLETED) return@launch

                // Already in progress — don't re-enqueue
                if (existing?.status == SummaryStatus.GENERATING) return@launch

                // Check if there's transcript to summarize
                val segments = transcriptRepository.getTranscriptBySessionOnce(sessionId)
                if (segments.isEmpty()) return@launch

                // Mark as generating in Room
                summaryRepository.insertOrUpdateSummary(
                    SummaryEntity(
                        sessionId = sessionId,
                        status = SummaryStatus.GENERATING,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                // Enqueue the worker (unique work — won't duplicate)
                SummaryWorker.enqueue(context, sessionId)
            } finally {
                // Allow retry button to work
                generationTriggered = false
            }
        }
    }

    /**
     * Retries summary generation after a failure.
     * Uses REPLACE policy to force a new attempt.
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
