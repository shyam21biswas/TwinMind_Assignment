package com.example.shyam_assignment.ui.screens.dashboard

import com.example.shyam_assignment.data.model.Meeting

data class DashboardUiState(
    val meetings: List<Meeting> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = meetings.isEmpty() && !isLoading
}

