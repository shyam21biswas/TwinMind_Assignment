package com.example.shyam_assignment.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared observable state between RecordingService and RecordingViewModel.
 * The service writes to it; the ViewModel reads from it.
 */
@Singleton
class RecordingServiceState @Inject constructor() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _statusText = MutableStateFlow("Ready to record")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateRecording(recording: Boolean) { _isRecording.value = recording }
    fun updatePaused(paused: Boolean) { _isPaused.value = paused }
    fun updateElapsedTime(ms: Long) { _elapsedTimeMs.value = ms }
    fun updateSessionId(id: String?) { _sessionId.value = id }
    fun updateStatus(text: String) { _statusText.value = text }
    fun updateWarning(msg: String?) { _warningMessage.value = msg }
    fun updateError(msg: String?) { _errorMessage.value = msg }

    fun reset() {
        _isRecording.value = false
        _isPaused.value = false
        _elapsedTimeMs.value = 0L
        _sessionId.value = null
        _statusText.value = "Ready to record"
        _warningMessage.value = null
        _errorMessage.value = null
    }
}

