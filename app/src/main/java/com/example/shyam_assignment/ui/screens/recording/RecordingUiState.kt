package com.example.shyam_assignment.ui.screens.recording

data class RecordingUiState(
    val sessionId: String? = null,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTimeMs: Long = 0L,
    val transcriptPreview: String = "",
    val statusText: String = "Ready to record",
    val warningMessage: String? = null,
    val error: String? = null,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val activeInputSource: String = "MICROPHONE",
    val transcriptSegments: List<TranscriptSegment> = emptyList()
) {
    val formattedTime: String
        get() {
            val totalSeconds = elapsedTimeMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    /** Full transcript from all segments joined by newlines */
    val fullTranscript: String
        get() = transcriptSegments.joinToString("\n") { it.text }
}

/** UI model for a transcript segment */
data class TranscriptSegment(
    val chunkIndex: Int,
    val text: String
)

