package com.example.shyam_assignment.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.data.local.DatabaseSeeder
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.service.RecordingService
import com.example.shyam_assignment.service.RecordingServiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * Combines data from Room (sessions) and RecordingServiceState (live recording status)
 * into a single DashboardUiState flow for the UI to observe.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,  // Access to saved sessions
    private val seeder: DatabaseSeeder,                     // Seeds sample data on first launch
    private val serviceState: RecordingServiceState          // Live recording state from service
) : ViewModel() {

    init {
        // Insert sample meetings if the database is empty (first launch)
        viewModelScope.launch {
            seeder.seedIfEmpty()
        }
    }

    /**
     * The UI state — combines sessions list + live recording status.
     * Updates automatically when Room data changes or recording state changes.
     */
    val uiState = combine(
        recordingRepository.getAllSessions(),   // All saved sessions from Room
        serviceState.isRecording,              // Is a recording currently active?
        serviceState.sessionId                 // ID of the active recording session
    ) { sessions, isRecording, sessionId ->
        DashboardUiState(
            sessions = sessions,
            isRecording = isRecording,
            activeSessionId = sessionId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    /** Stops the current recording by telling the foreground service to stop */
    fun stopRecording(context: Context) {
        RecordingService.stopRecording(context)
    }
}
