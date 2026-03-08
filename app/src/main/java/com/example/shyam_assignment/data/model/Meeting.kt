package com.example.shyam_assignment.data.model

/**
 * Simple UI model representing a meeting/recording session.
 * Used as a lightweight data holder before Room entities were introduced.
 */
data class Meeting(
    val id: String,                                    // Unique meeting ID
    val title: String,                                 // Title of the meeting
    val date: Long,                                    // Timestamp when the meeting started
    val duration: Long = 0L,                           // Recording duration in milliseconds
    val transcript: String = "",                       // Full transcript text
    val summary: String = "",                          // AI-generated summary text
    val actionItems: List<String> = emptyList(),       // List of action items from the meeting
    val keyPoints: List<String> = emptyList(),         // Key discussion highlights
    val status: MeetingStatus = MeetingStatus.RECORDED // Current status of the meeting
)

/**
 * Possible states a meeting can be in during its lifecycle.
 */
enum class MeetingStatus {
    RECORDING,     // Audio is currently being captured
    RECORDED,      // Recording finished, not yet transcribed
    TRANSCRIBING,  // Gemini is converting audio to text
    SUMMARIZING,   // Gemini is generating the summary
    COMPLETED,     // Everything done — transcript and summary ready
    ERROR          // Something went wrong
}
