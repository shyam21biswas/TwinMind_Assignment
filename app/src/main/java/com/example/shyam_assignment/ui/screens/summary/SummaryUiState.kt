package com.example.shyam_assignment.ui.screens.summary

import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity

/**
 * UI state for the Summary/Meeting Details screen.
 * Holds session info, summary data, transcript, and loading/error states.
 */
data class SummaryUiState(
    val session: RecordingSessionEntity? = null,               // The recording session details
    val summary: SummaryEntity? = null,                        // AI-generated summary (may be null)
    val transcript: List<TranscriptSegmentEntity> = emptyList(), // Ordered transcript segments
    val isLoading: Boolean = false,                             // True while loading session data
    val error: String? = null,                                  // Error message
    val isSummaryGenerating: Boolean = false,                   // True while Gemini is generating
    val summaryStatus: String? = null                           // Current summary status string
)
