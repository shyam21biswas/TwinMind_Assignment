package com.example.shyam_assignment.ui.screens.summary

import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity

data class SummaryUiState(
    val session: RecordingSessionEntity? = null,
    val summary: SummaryEntity? = null,
    val transcript: List<TranscriptSegmentEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSummaryGenerating: Boolean = false,
    val summaryStatus: String? = null
)
