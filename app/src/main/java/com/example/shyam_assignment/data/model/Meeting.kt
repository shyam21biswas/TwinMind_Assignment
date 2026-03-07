package com.example.shyam_assignment.data.model

data class Meeting(
    val id: String,
    val title: String,
    val date: Long,
    val duration: Long = 0L,
    val transcript: String = "",
    val summary: String = "",
    val actionItems: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val status: MeetingStatus = MeetingStatus.RECORDED
)

enum class MeetingStatus {
    RECORDING,
    RECORDED,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    ERROR
}

