package com.example.shyam_assignment.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.data.local.DatabaseSeeder
import com.example.shyam_assignment.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val seeder: DatabaseSeeder
) : ViewModel() {

    init {
        viewModelScope.launch {
            seeder.seedIfEmpty()
        }
    }

    val uiState = recordingRepository.getAllSessions()
        .map { sessions ->
            DashboardUiState(sessions = sessions)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true)
        )
}

