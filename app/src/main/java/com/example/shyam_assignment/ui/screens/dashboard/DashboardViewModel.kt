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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val seeder: DatabaseSeeder,
    private val serviceState: RecordingServiceState
) : ViewModel() {

    init {
        viewModelScope.launch {
            seeder.seedIfEmpty()
        }
    }

    val uiState = combine(
        recordingRepository.getAllSessions(),
        serviceState.isRecording,
        serviceState.sessionId
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

    fun stopRecording(context: Context) {
        RecordingService.stopRecording(context)
    }
}

