package com.example.shyam_assignment.ui.screens.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shyam_assignment.data.repository.RecordingRepository
import com.example.shyam_assignment.data.repository.SummaryRepository
import com.example.shyam_assignment.data.repository.TranscriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recordingRepository: RecordingRepository,
    summaryRepository: SummaryRepository,
    transcriptRepository: TranscriptRepository
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["meetingId"])

    val uiState = combine(
        recordingRepository.getSessionById(sessionId),
        summaryRepository.getSummaryBySession(sessionId),
        transcriptRepository.getTranscriptBySession(sessionId)
    ) { session, summary, transcript ->
        SummaryUiState(
            session = session,
            summary = summary,
            transcript = transcript,
            isLoading = false,
            error = if (session == null) "Session not found" else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState(isLoading = true)
    )
}

