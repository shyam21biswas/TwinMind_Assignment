package com.example.shyam_assignment.ui.screens.recording

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.service.RecordingService
import com.example.shyam_assignment.service.RecordingServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val serviceState: RecordingServiceState
) : ViewModel() {

    val uiState = combine(
        serviceState.isRecording,
        serviceState.isPaused,
        serviceState.elapsedTimeMs,
        serviceState.sessionId,
        serviceState.statusText
    ) { isRecording, isPaused, elapsed, sessionId, status ->
        // First pass: combine the 5 main fields
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordingUiState()
    )

    fun startRecording(context: Context) {
        RecordingService.startRecording(context)
    }

    fun stopRecording(context: Context) {
        RecordingService.stopRecording(context)
    }

    fun pauseRecording() {
        serviceState.updatePaused(true)
        serviceState.updateStatus("Paused")
    }

    fun resumeRecording() {
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
    }
}

