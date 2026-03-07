package com.example.shyam_assignment.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.shyam_assignment.MainActivity
import com.example.shyam_assignment.R
import com.example.shyam_assignment.data.local.entity.AudioChunkEntity
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SessionStatus
import com.example.shyam_assignment.data.repository.RecordingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    companion object {
        const val TAG = "RecordingService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "twinmind_recording_channel"

        const val ACTION_START = "com.example.shyam_assignment.action.START_RECORDING"
        const val ACTION_STOP = "com.example.shyam_assignment.action.STOP_RECORDING"

        private const val SAMPLE_RATE = 16_000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /** Bytes per second of raw PCM: 16000 Hz × 1 ch × 2 bytes = 32 000 B/s */
        private const val BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)

        /** Main chunk duration */
        private const val CHUNK_DURATION_SEC = 30
        private const val CHUNK_SIZE_BYTES = BYTES_PER_SECOND * CHUNK_DURATION_SEC  // 960 000

        /** Overlap duration */
        private const val OVERLAP_DURATION_SEC = 2
        private const val OVERLAP_SIZE_BYTES = BYTES_PER_SECOND * OVERLAP_DURATION_SEC  // 64 000
        private const val OVERLAP_DURATION_MS = (OVERLAP_DURATION_SEC * 1000).toLong()

        fun startRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    @Inject lateinit var recordingRepository: RecordingRepository
    @Inject lateinit var serviceState: RecordingServiceState

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null
    private var recordingJob: Job? = null

    private var audioRecord: AudioRecord? = null
    private var sessionId: String? = null
    private var recordingStartTimeMs: Long = 0L
    private var accumulatedTimeMs: Long = 0L
    private var recordingsDir: File? = null

    // ── Chunk state ────────────────────────────────────────────────────
    private var currentChunkIndex: Int = 0
    private var currentWavWriter: WavWriter? = null
    private var currentChunkStartTime: Long = 0L
    private var currentChunkBytesWritten: Long = 0L
    private var currentChunkFile: File? = null

    /** Ring buffer that holds the last ~2 seconds of PCM for overlap */
    private val overlapBuffer = ByteArrayOutputStream(OVERLAP_SIZE_BYTES + 4096)

    // ── Lifecycle ──────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handleStop()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Start recording ────────────────────────────────────────────────

    private fun handleStart() {
        if (serviceState.isRecording.value) return

        val id = UUID.randomUUID().toString()
        sessionId = id
        val now = System.currentTimeMillis()

        // Create output directory for this session
        val dir = File(filesDir, "recordings/$id").also { it.mkdirs() }
        recordingsDir = dir

        // Persist session to Room
        serviceScope.launch {
            recordingRepository.insertSession(
                RecordingSessionEntity(
                    sessionId = id,
                    title = "Recording ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(now))}",
                    startedAt = now,
                    status = SessionStatus.RECORDING,
                    activeInputSource = "MICROPHONE",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Start foreground
        startForeground(NOTIFICATION_ID, buildNotification("Recording..."))

        // Reset chunk state
        currentChunkIndex = 0
        overlapBuffer.reset()

        // Update shared state
        serviceState.updateSessionId(id)
        serviceState.updateRecording(true)
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
        serviceState.updateError(null)
        serviceState.updateWarning(null)
        serviceState.updateElapsedTime(0L)
        serviceState.updateCurrentChunkIndex(0)
        serviceState.updateTotalChunks(0)
        accumulatedTimeMs = 0L
        recordingStartTimeMs = System.currentTimeMillis()

        // Start audio capture with chunking
        startChunkedCapture()

        // Start timer
        startTimer()
    }

    // ── Stop recording ─────────────────────────────────────────────────

    private fun handleStop() {
        timerJob?.cancel()
        timerJob = null

        // Signal the capture loop to exit
        val wasRecording = serviceState.isRecording.value
        serviceState.updateRecording(false)

        // Cancel the recording coroutine — this will NOT finalize the chunk,
        // so we finalize it explicitly here.
        recordingJob?.cancel()
        recordingJob = null

        // Stop AudioRecord
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        // Finalize last chunk if it has data
        if (wasRecording) {
            finalizeCurrentChunk()
        }

        val now = System.currentTimeMillis()
        val elapsed = if (recordingStartTimeMs > 0)
            accumulatedTimeMs + (now - recordingStartTimeMs)
        else 0L

        // Update Room session
        val sid = sessionId
        if (sid != null) {
            serviceScope.launch {
                val existing = recordingRepository.getSessionByIdOnce(sid)
                if (existing != null) {
                    recordingRepository.updateSession(
                        existing.copy(
                            endedAt = now,
                            durationMs = elapsed,
                            status = SessionStatus.STOPPED,
                            updatedAt = now
                        )
                    )
                }
            }
        }

        // Update shared state
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording stopped")
        serviceState.updateElapsedTime(elapsed)

        sessionId = null
        recordingStartTimeMs = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Chunked audio capture ──────────────────────────────────────────

    private fun startChunkedCapture() {
        recordingJob = serviceScope.launch {
            try {
                val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                val bufferSize = maxOf(minBuf, 4096)

                @Suppress("MissingPermission")
                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    serviceState.updateError("Failed to initialize AudioRecord")
                    serviceState.updateStatus("Error")
                    Log.e(TAG, "AudioRecord not initialized")
                    return@launch
                }

                audioRecord = recorder
                recorder.startRecording()

                // Open the first chunk
                openNewChunk()

                val readBuffer = ByteArray(bufferSize)

                while (isActive && serviceState.isRecording.value) {
                    if (serviceState.isPaused.value) {
                        delay(100)
                        continue
                    }

                    val bytesRead = recorder.read(readBuffer, 0, readBuffer.size)
                    if (bytesRead <= 0) continue

                    // Write to current chunk
                    currentWavWriter?.write(readBuffer, 0, bytesRead)
                    currentChunkBytesWritten += bytesRead

                    // Maintain overlap ring buffer: keep only the last OVERLAP_SIZE_BYTES
                    feedOverlapBuffer(readBuffer, bytesRead)

                    // Check if chunk is full (30 seconds of data)
                    if (currentChunkBytesWritten >= CHUNK_SIZE_BYTES) {
                        rolloverChunk()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Chunked recording error", e)
                serviceState.updateError("Recording error: ${e.message}")
                serviceState.updateStatus("Error")
            }
        }
    }

    /**
     * Opens a new WAV file for the current chunk index and writes the
     * overlap buffer from the previous chunk as a prefix.
     */
    private fun openNewChunk() {
        val sid = sessionId ?: return
        val dir = recordingsDir ?: return
        val now = System.currentTimeMillis()

        val chunkFile = File(dir, "chunk_${currentChunkIndex}.wav")
        currentChunkFile = chunkFile
        currentChunkStartTime = now

        val writer = WavWriter(chunkFile, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)
        writer.start()

        // If there's overlap data from the previous chunk, prefix it
        val overlapData = overlapBuffer.toByteArray()
        if (overlapData.isNotEmpty() && currentChunkIndex > 0) {
            writer.write(overlapData)
            currentChunkBytesWritten = overlapData.size.toLong()
        } else {
            currentChunkBytesWritten = 0L
        }

        currentWavWriter = writer

        // Update UI state
        serviceState.updateCurrentChunkIndex(currentChunkIndex)
        serviceState.updateTotalChunks(currentChunkIndex + 1)
    }

    /**
     * Finalizes the current chunk and opens a new one.
     * The overlap buffer already holds the last ~2 seconds.
     */
    private fun rolloverChunk() {
        finalizeCurrentChunk()
        currentChunkIndex++
        openNewChunk()
    }

    /**
     * Closes the current WAV writer and saves chunk metadata to Room.
     */
    private fun finalizeCurrentChunk() {
        val writer = currentWavWriter ?: return
        val file = currentChunkFile ?: return
        val sid = sessionId ?: return

        val now = System.currentTimeMillis()
        val chunkStartTime = currentChunkStartTime
        val dataBytesWritten = writer.dataBytesWritten

        writer.finish()
        currentWavWriter = null

        // Only persist if we actually recorded audio
        if (dataBytesWritten <= 0) return

        val durationMs = (dataBytesWritten * 1000L) / BYTES_PER_SECOND
        val overlapMs = if (currentChunkIndex > 0) OVERLAP_DURATION_MS else 0L
        val chunkIdx = currentChunkIndex

        serviceScope.launch {
            recordingRepository.insertChunk(
                AudioChunkEntity(
                    chunkId = UUID.randomUUID().toString(),
                    sessionId = sid,
                    chunkIndex = chunkIdx,
                    filePath = file.absolutePath,
                    startedAt = chunkStartTime,
                    endedAt = now,
                    durationMs = durationMs,
                    overlapMs = overlapMs,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        // Update total count in UI
        serviceState.updateTotalChunks(chunkIdx + 1)
    }

    /**
     * Feeds raw PCM bytes into the overlap ring buffer.
     * Keeps only the most recent [OVERLAP_SIZE_BYTES] bytes.
     */
    private fun feedOverlapBuffer(data: ByteArray, length: Int) {
        overlapBuffer.write(data, 0, length)

        // Trim to keep only the tail
        if (overlapBuffer.size() > OVERLAP_SIZE_BYTES) {
            val full = overlapBuffer.toByteArray()
            val keep = full.copyOfRange(full.size - OVERLAP_SIZE_BYTES, full.size)
            overlapBuffer.reset()
            overlapBuffer.write(keep)
        }
    }

    // ── Timer ──────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1_000)
                if (!serviceState.isPaused.value && serviceState.isRecording.value) {
                    val elapsed = accumulatedTimeMs + (System.currentTimeMillis() - recordingStartTimeMs)
                    serviceState.updateElapsedTime(elapsed)

                    val formatted = formatTime(elapsed)
                    val chunkInfo = "Chunk ${serviceState.currentChunkIndex.value + 1}"
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification("$formatted — $chunkInfo"))
                }
            }
        }
    }

    // ── Notification ───────────────────────────────────────────────────

    private fun buildNotification(contentText: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind Recording")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setOngoing(true)
            .setContentIntent(tapPending)
            .addAction(R.drawable.ic_stop_notification, "Stop", stopPending)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }
}


