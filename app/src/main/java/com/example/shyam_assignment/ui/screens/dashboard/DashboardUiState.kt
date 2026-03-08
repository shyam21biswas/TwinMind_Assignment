package com.example.shyam_assignment.ui.screens.dashboard

import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity

/**
 * UI state for the Dashboard screen.
 * Holds the list of meetings, loading/error state, and recording status.
 */
data class DashboardUiState(
    val sessions: List<RecordingSessionEntity> = emptyList(),  // All saved meetings
    val isLoading: Boolean = false,                             // True while loading from Room
    val error: String? = null,                                  // Error message if something failed
    val isRecording: Boolean = false,                           // True if a recording is in progress
    val activeSessionId: String? = null                         // ID of the active recording session
) {
    /** True when there are no meetings and we're not loading */
    val isEmpty: Boolean get() = sessions.isEmpty() && !isLoading
}
