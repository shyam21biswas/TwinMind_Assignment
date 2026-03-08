package com.example.shyam_assignment.ui.screens.recording

/**
 * UI state for the Recording screen.
 * Contains everything the screen needs to display the current recording status.
 */
data class RecordingUiState(
    val sessionId: String? = null,                // Current session ID
    val isRecording: Boolean = false,             // True when actively recording audio
    val isPaused: Boolean = false,                // True when recording is paused
    val elapsedTimeMs: Long = 0L,                 // Time elapsed since recording started (ms)
    val transcriptPreview: String = "",            // Quick preview of transcript text
    val statusText: String = "Ready to record",   // Status message shown on screen
    val warningMessage: String? = null,            // Warning (e.g., "No audio detected")
    val error: String? = null,                     // Error message if something went wrong
    val currentChunkIndex: Int = 0,                // Which chunk is currently being recorded
    val totalChunks: Int = 0,                      // Total number of chunks recorded so far
    val activeInputSource: String = "MICROPHONE",  // Current audio source (mic, bluetooth, etc.)
    val transcriptSegments: List<TranscriptSegment> = emptyList()  // Live transcript segments
) {
    /** Formats elapsed time as MM:SS for display */
    val formattedTime: String
        get() {
            val totalSeconds = elapsedTimeMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    /** Joins all transcript segments into a single string */
    val fullTranscript: String
        get() = transcriptSegments.joinToString("\n") { it.text }
}

/** Simple UI model for a transcript segment — just the chunk number and text */
data class TranscriptSegment(
    val chunkIndex: Int,   // Which chunk this text came from
    val text: String       // The transcribed text
)
