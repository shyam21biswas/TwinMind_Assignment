package com.example.shyam_assignment.ui.screens.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MeetingRepository
) : ViewModel() {

    private val meetingId: String = checkNotNull(savedStateHandle["meetingId"])

    val uiState = repository.getMeetingById(meetingId)
        .map { meeting ->
            SummaryUiState(
                meeting = meeting,
                isLoading = false,
                error = if (meeting == null) "Meeting not found" else null
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SummaryUiState(isLoading = true)
        )
}

