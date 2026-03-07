package com.example.shyam_assignment.ui.screens.recording

import androidx.lifecycle.ViewModel
import com.example.shyam_assignment.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val repository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState())
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    fun startRecording() {
        // Will be implemented in Part 2
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            statusText = "Recording..."
        )
    }

    fun stopRecording() {
        // Will be implemented in Part 2
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            statusText = "Recording stopped"
        )
    }

    fun pauseRecording() {
        // Will be implemented in Part 2
        _uiState.value = _uiState.value.copy(
            isPaused = true,
            statusText = "Paused"
        )
    }

    fun resumeRecording() {
        // Will be implemented in Part 2
        _uiState.value = _uiState.value.copy(
            isPaused = false,
            statusText = "Recording..."
        )
    }
}

