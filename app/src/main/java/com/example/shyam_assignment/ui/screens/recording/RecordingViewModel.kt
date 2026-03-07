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
        RecordingUiState(
            sessionId = sessionId,
            isRecording = isRecording,
            isPaused = isPaused,
            elapsedTimeMs = elapsed,
            statusText = status,
            warningMessage = serviceState.warningMessage.value,
            error = serviceState.errorMessage.value
        )
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
        // Pause support will be refined in a later part
        serviceState.updatePaused(true)
        serviceState.updateStatus("Paused")
    }

    fun resumeRecording() {
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
    }
}

