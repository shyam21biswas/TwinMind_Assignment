package com.example.shyam_assignment.ui.screens.summary

import com.example.shyam_assignment.data.model.Meeting

data class SummaryUiState(
    val meeting: Meeting? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

