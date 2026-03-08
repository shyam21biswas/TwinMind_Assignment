package com.example.shyam_assignment.ui.screens.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.data.repository.TranscriptRepository
import com.example.shyam_assignment.service.RecordingService
import com.example.shyam_assignment.service.RecordingServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for the Recording screen.
 * Observes live recording state from the foreground service and
 * live transcript segments from Room, combining them into RecordingUiState.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val serviceState: RecordingServiceState,    // Live state from recording service
    private val transcriptRepository: TranscriptRepository  // Access to transcript data in Room
) : ViewModel() {

    /**
     * Watches the current sessionId — whenever it changes, switches to
     * observing transcript segments for that session from Room.
     */
    private val transcriptSegments = serviceState.sessionId
        .flatMapLatest { sid ->
            if (sid == null) flowOf(emptyList())
            else transcriptRepository.getTranscriptBySession(sid)
                .map { entities ->
                    entities.map { TranscriptSegment(it.chunkIndex, it.text) }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The UI state — combines all service state fields + transcript segments
     * into a single observable state for the Recording screen.
     */
    val uiState = combine(
        serviceState.isRecording,
        serviceState.isPaused,
        serviceState.elapsedTimeMs,
        serviceState.sessionId,
        serviceState.statusText
    ) { isRecording, isPaused, elapsed, sessionId, status ->
        RecordingUiState(
            sessionId = sessionId,
            isRecording = isRecording,
            isPaused = isPaused,
            elapsedTimeMs = elapsed,
            statusText = status
        )
    }.combine(serviceState.currentChunkIndex) { state, chunkIndex ->
        state.copy(currentChunkIndex = chunkIndex)
    }.combine(serviceState.totalChunks) { state, total ->
        state.copy(totalChunks = total)
    }.combine(serviceState.warningMessage) { state, warning ->
        state.copy(warningMessage = warning)
    }.combine(serviceState.errorMessage) { state, error ->
        state.copy(error = error)
    }.combine(serviceState.activeInputSource) { state, source ->
        state.copy(activeInputSource = source)
    }.combine(transcriptSegments) { state, segments ->
        state.copy(transcriptSegments = segments)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordingUiState()
    )

    /** Tells the foreground service to start recording */
    fun startRecording(context: Context) {
        RecordingService.startRecording(context)
    }

    /** Tells the foreground service to stop recording */
    fun stopRecording(context: Context) {
        RecordingService.stopRecording(context)
    }

    /** Pauses recording (updates shared state — service reads this) */
    fun pauseRecording() {
        serviceState.updatePaused(true)
        serviceState.updateStatus("Paused")
    }

    /** Tells the foreground service to resume recording after a pause */
    fun resumeRecording(context: Context) {
        RecordingService.resumeRecording(context)
    }
}
