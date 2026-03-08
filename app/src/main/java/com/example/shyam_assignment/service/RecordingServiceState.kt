package com.example.shyam_assignment.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared observable state between RecordingService and ViewModels.
 *
 * How it works:
 * - The RecordingService WRITES to these StateFlows (updates recording status, timer, etc.)
 * - The ViewModels READ from these StateFlows (observe changes and update the UI)
 *
 * This is a @Singleton so the same instance is shared across the app.
 */
@Singleton
class RecordingServiceState @Inject constructor() {

    // ── Observable state fields ──

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()       // Is audio being captured?

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()             // Is recording paused?

    private val _elapsedTimeMs = MutableStateFlow(0L)
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()      // Timer value in ms

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()           // Current session ID

    private val _statusText = MutableStateFlow("Ready to record")
    val statusText: StateFlow<String> = _statusText.asStateFlow()          // Status label for UI

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow() // Warning text (e.g., silence)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()     // Error text

    private val _currentChunkIndex = MutableStateFlow(0)
    val currentChunkIndex: StateFlow<Int> = _currentChunkIndex.asStateFlow() // Active chunk number

    private val _totalChunks = MutableStateFlow(0)
    val totalChunks: StateFlow<Int> = _totalChunks.asStateFlow()           // Total chunks recorded

    private val _activeInputSource = MutableStateFlow("MICROPHONE")
    val activeInputSource: StateFlow<String> = _activeInputSource.asStateFlow() // Current mic source

    // ── Update functions (called by RecordingService) ──

    fun updateRecording(recording: Boolean) { _isRecording.value = recording }
    fun updatePaused(paused: Boolean) { _isPaused.value = paused }
    fun updateElapsedTime(ms: Long) { _elapsedTimeMs.value = ms }
    fun updateSessionId(id: String?) { _sessionId.value = id }
    fun updateStatus(text: String) { _statusText.value = text }
    fun updateWarning(msg: String?) { _warningMessage.value = msg }
    fun updateError(msg: String?) { _errorMessage.value = msg }
    fun updateCurrentChunkIndex(index: Int) { _currentChunkIndex.value = index }
    fun updateTotalChunks(count: Int) { _totalChunks.value = count }
    fun updateActiveInputSource(source: String) { _activeInputSource.value = source }

    /** Resets all state back to defaults (called when recording fully stops) */
    fun reset() {
        _isRecording.value = false
        _isPaused.value = false
        _elapsedTimeMs.value = 0L
        _sessionId.value = null
        _statusText.value = "Ready to record"
        _warningMessage.value = null
        _errorMessage.value = null
        _currentChunkIndex.value = 0
        _totalChunks.value = 0
        _activeInputSource.value = "MICROPHONE"
    }
}
