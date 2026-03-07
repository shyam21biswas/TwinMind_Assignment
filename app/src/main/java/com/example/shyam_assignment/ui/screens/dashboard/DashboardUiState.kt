package com.example.shyam_assignment.ui.screens.dashboard

import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity

data class DashboardUiState(
    val sessions: List<RecordingSessionEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = sessions.isEmpty() && !isLoading
}

