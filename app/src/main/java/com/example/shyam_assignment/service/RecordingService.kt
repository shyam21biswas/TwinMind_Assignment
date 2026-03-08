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
import kotlinx.coroutines.runBlocking
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
        const val ACTION_RESUME = "com.example.shyam_assignment.action.RESUME_RECORDING"

        private const val SAMPLE_RATE = 16_000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        private const val BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8)

        private const val CHUNK_DURATION_SEC = 30
        private const val CHUNK_SIZE_BYTES = BYTES_PER_SECOND * CHUNK_DURATION_SEC

        private const val OVERLAP_DURATION_SEC = 2
        private const val OVERLAP_SIZE_BYTES = BYTES_PER_SECOND * OVERLAP_DURATION_SEC
        private const val OVERLAP_DURATION_MS = (OVERLAP_DURATION_SEC * 1000).toLong()

        private const val STORAGE_CHECK_INTERVAL_SEC = 30

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
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var recordingRepository: RecordingRepository
    @Inject lateinit var serviceState: RecordingServiceState

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var timerJob: Job? = null
    private var recordingJob: Job? = null
    private var storageCheckJob: Job? = null

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
    private val overlapBuffer = ByteArrayOutputStream(OVERLAP_SIZE_BYTES + 4096)

    // ── Edge-case handlers ─────────────────────────────────────────────
    private var phoneCallMonitor: PhoneCallMonitor? = null
    private var batteryMonitor: BatteryMonitor? = null
    private var audioSourceMonitor: AudioSourceMonitor? = null
    private val silenceDetector = SilenceDetector()

    /** Tracks the reason the recording was auto-paused so we auto-resume correctly. */
    private var autoPauseReason: String? = null

    // ── Lifecycle ──────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> handleStart()
            ACTION_STOP   -> handleStop()
            ACTION_RESUME -> handleResume()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tearDownEdgeCaseHandlers()
        handleStop()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Start recording ────────────────────────────────────────────────

    private fun handleStart() {
        // MUST call startForeground() immediately to avoid crash.
        // Android enforces this within ~5s of startForegroundService().
        startForeground(NOTIFICATION_ID, buildNotification("Starting...", paused = false))

        if (serviceState.isRecording.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Pre-check storage
        if (!StorageMonitor.hasEnoughStorage(this)) {
            serviceState.updateError("Cannot start — Low storage")
            serviceState.updateStatus("Error")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // Pre-check battery
        val batteryCheck = BatteryMonitor(this)
        if (batteryCheck.isBatteryLow()) {
            serviceState.updateError("Cannot start — Battery below 6%")
            serviceState.updateStatus("Error")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val id = UUID.randomUUID().toString()
        sessionId = id
        val now = System.currentTimeMillis()

        val dir = File(filesDir, "recordings/$id").also { it.mkdirs() }
        recordingsDir = dir

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

        // Update notification to show recording state
        notifyNotification("Recording...", paused = false)

        currentChunkIndex = 0
        overlapBuffer.reset()
        autoPauseReason = null

        serviceState.updateSessionId(id)
        serviceState.updateRecording(true)
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
        serviceState.updateError(null)
        serviceState.updateWarning(null)
        serviceState.updateElapsedTime(0L)
        serviceState.updateCurrentChunkIndex(0)
        serviceState.updateTotalChunks(0)
        serviceState.updateActiveInputSource("MICROPHONE")
        accumulatedTimeMs = 0L
        recordingStartTimeMs = System.currentTimeMillis()

        setUpEdgeCaseHandlers()
        startChunkedCapture()
        startTimer()
        startStorageMonitor()
    }

    // ── Resume recording (from auto-pause) ─────────────────────────────

    private fun handleResume() {
        if (!serviceState.isRecording.value) return
        if (!serviceState.isPaused.value) return

        autoPauseReason = null
        recordingStartTimeMs = System.currentTimeMillis()

        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
        serviceState.updateWarning(null)

        updateSessionStatus(SessionStatus.RECORDING)
        notifyNotification("Recording...", paused = false)
    }

    // ── Stop recording ─────────────────────────────────────────────────

    private fun handleStop(errorMessage: String? = null) {
        tearDownEdgeCaseHandlers()

        timerJob?.cancel(); timerJob = null
        storageCheckJob?.cancel(); storageCheckJob = null

        val wasRecording = serviceState.isRecording.value
        serviceState.updateRecording(false)

        recordingJob?.cancel(); recordingJob = null

        try { audioRecord?.stop(); audioRecord?.release() }
        catch (e: Exception) { Log.e(TAG, "Error stopping AudioRecord", e) }
        audioRecord = null

        // Finalize the last chunk and persist everything SYNCHRONOUSLY
        // so the service doesn't die before Room writes + WorkManager enqueue complete.
        val sid = sessionId
        val now = System.currentTimeMillis()
        val elapsed = if (recordingStartTimeMs > 0)
            accumulatedTimeMs + (now - recordingStartTimeMs) else 0L

        runBlocking {
            // 1. Finalize last chunk → insert into Room → enqueue transcription
            if (wasRecording) {
                finalizeCurrentChunkSync(sid, now)
            }

            // 2. Update session in Room
            if (sid != null) {
                val finalStatus = if (errorMessage != null) SessionStatus.ERROR else SessionStatus.STOPPED
                try {
                    val existing = recordingRepository.getSessionByIdOnce(sid)
                    if (existing != null) {
                        recordingRepository.updateSession(
                            existing.copy(
                                endedAt = now,
                                durationMs = elapsed,
                                status = finalStatus,
                                errorMessage = errorMessage,
                                updatedAt = now
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update session on stop", e)
                }
            }
        }

        serviceState.updatePaused(false)
        if (errorMessage != null) {
            serviceState.updateError(errorMessage)
            serviceState.updateStatus("Error")
        } else {
            serviceState.updateStatus("Recording stopped")
        }
        serviceState.updateElapsedTime(elapsed)

        sessionId = null
        recordingStartTimeMs = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Edge-case handler setup / teardown ─────────────────────────────

    private fun setUpEdgeCaseHandlers() {
        // 1. Phone calls
        phoneCallMonitor = PhoneCallMonitor(this).also {
            it.start(object : PhoneCallMonitor.Callback {
                override fun onCallStarted() = autoPause("Phone call")
                override fun onCallEnded()   = autoResume("Phone call")
            })
        }

        // 2. Battery monitor — stop recording if battery drops below 6%
        batteryMonitor = BatteryMonitor(this).also {
            it.start(object : BatteryMonitor.Callback {
                override fun onBatteryLow(level: Int) {
                    Log.w(TAG, "Battery low ($level%) — stopping recording")
                    handleStop(errorMessage = "Recording stopped — Battery low ($level%)")
                }
            })
        }

        // 3. Audio source changes
        audioSourceMonitor = AudioSourceMonitor(this).also {
            it.start(object : AudioSourceMonitor.Callback {
                override fun onSourceChanged(newSource: String) {
                    serviceState.updateActiveInputSource(newSource)
                    serviceState.updateWarning("Audio source changed to $newSource")
                    updateSessionInputSource(newSource)
                }
            })
        }

        // 5. Silence detector — warn after 10s, do NOT stop recording
        silenceDetector.start(object : SilenceDetector.Callback {
            override fun onSilenceDetected() {
                serviceState.updateWarning("No audio detected — Check microphone")
            }
            override fun onSoundDetected() {
                if (serviceState.warningMessage.value == "No audio detected — Check microphone") {
                    serviceState.updateWarning(null)
                }
            }
        })
    }

    private fun tearDownEdgeCaseHandlers() {
        phoneCallMonitor?.stop(); phoneCallMonitor = null
        batteryMonitor?.stop(); batteryMonitor = null
        audioSourceMonitor?.stop(); audioSourceMonitor = null
        silenceDetector.stop()
    }

    // ── Auto-pause / auto-resume ───────────────────────────────────────

    private fun autoPause(reason: String) {
        if (!serviceState.isRecording.value || serviceState.isPaused.value) return
        autoPauseReason = reason

        // Accumulate elapsed time before pausing
        if (recordingStartTimeMs > 0) {
            accumulatedTimeMs += System.currentTimeMillis() - recordingStartTimeMs
            recordingStartTimeMs = 0L
        }

        serviceState.updatePaused(true)
        serviceState.updateStatus("Paused — $reason")
        updateSessionStatus(SessionStatus.PAUSED)
        notifyNotification("Paused — $reason", paused = true)
    }

    private fun autoResume(matchReason: String) {
        if (!serviceState.isRecording.value || !serviceState.isPaused.value) return
        if (autoPauseReason != matchReason) return

        autoPauseReason = null
        recordingStartTimeMs = System.currentTimeMillis()

        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
        serviceState.updateWarning(null)
        updateSessionStatus(SessionStatus.RECORDING)
        notifyNotification("Recording...", paused = false)
    }

    // ── Storage monitor ────────────────────────────────────────────────

    private fun startStorageMonitor() {
        storageCheckJob = serviceScope.launch {
            while (isActive && serviceState.isRecording.value) {
                delay(STORAGE_CHECK_INTERVAL_SEC * 1000L)
                if (!StorageMonitor.hasEnoughStorage(this@RecordingService)) {
                    Log.w(TAG, "Low storage detected — stopping recording")
                    launch(Dispatchers.Main) {
                        handleStop(errorMessage = "Recording stopped — Low storage")
                    }
                    break
                }
            }
        }
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

                    // Overlap ring buffer
                    feedOverlapBuffer(readBuffer, bytesRead)

                    // Silence detection
                    silenceDetector.feed(readBuffer, bytesRead)

                    // Chunk rollover
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

    private fun openNewChunk() {
        val dir = recordingsDir ?: return
        val now = System.currentTimeMillis()

        val chunkFile = File(dir, "chunk_${currentChunkIndex}.wav")
        currentChunkFile = chunkFile
        currentChunkStartTime = now

        val writer = WavWriter(chunkFile, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)
        writer.start()

        val overlapData = overlapBuffer.toByteArray()
        if (overlapData.isNotEmpty() && currentChunkIndex > 0) {
            writer.write(overlapData)
            currentChunkBytesWritten = overlapData.size.toLong()
        } else {
            currentChunkBytesWritten = 0L
        }

        currentWavWriter = writer
        serviceState.updateCurrentChunkIndex(currentChunkIndex)
        serviceState.updateTotalChunks(currentChunkIndex + 1)
    }

    private fun rolloverChunk() {
        finalizeCurrentChunk()
        currentChunkIndex++
        openNewChunk()
    }

    /**
     * Finalizes the current chunk during active recording (rollover).
     * Uses serviceScope.launch — safe because service is still alive.
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

        if (dataBytesWritten <= 0) return

        val durationMs = (dataBytesWritten * 1000L) / BYTES_PER_SECOND
        val overlapMs = if (currentChunkIndex > 0) OVERLAP_DURATION_MS else 0L
        val chunkIdx = currentChunkIndex
        val chunkId = UUID.randomUUID().toString()

        serviceScope.launch {
            recordingRepository.insertChunk(
                AudioChunkEntity(
                    chunkId = chunkId,
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

            // Enqueue transcription via WorkManager
            com.example.shyam_assignment.worker.ChunkTranscriptionWorker.enqueue(
                context = this@RecordingService,
                chunkId = chunkId,
                sessionId = sid
            )
        }
        serviceState.updateTotalChunks(chunkIdx + 1)
    }

    /**
     * Finalizes the current chunk at stop time — SYNCHRONOUS.
     * Called from runBlocking so Room write + WorkManager enqueue
     * complete before the service dies.
     */
    private suspend fun finalizeCurrentChunkSync(sid: String?, now: Long) {
        val writer = currentWavWriter ?: return
        val file = currentChunkFile ?: return
        val sessionId = sid ?: return

        val chunkStartTime = currentChunkStartTime
        val dataBytesWritten = writer.dataBytesWritten

        writer.finish()
        currentWavWriter = null

        if (dataBytesWritten <= 0) return

        val durationMs = (dataBytesWritten * 1000L) / BYTES_PER_SECOND
        val overlapMs = if (currentChunkIndex > 0) OVERLAP_DURATION_MS else 0L
        val chunkIdx = currentChunkIndex
        val chunkId = UUID.randomUUID().toString()

        try {
            recordingRepository.insertChunk(
                AudioChunkEntity(
                    chunkId = chunkId,
                    sessionId = sessionId,
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
            Log.d(TAG, "Final chunk $chunkIdx saved to Room (chunkId=$chunkId)")

            // Enqueue transcription — this is synchronous (just puts work in queue)
            com.example.shyam_assignment.worker.ChunkTranscriptionWorker.enqueue(
                context = this@RecordingService,
                chunkId = chunkId,
                sessionId = sessionId
            )
            Log.d(TAG, "Final chunk transcription enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save/enqueue final chunk", e)
        }

        serviceState.updateTotalChunks(chunkIdx + 1)
    }

    private fun feedOverlapBuffer(data: ByteArray, length: Int) {
        overlapBuffer.write(data, 0, length)
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
                if (serviceState.isRecording.value) {
                    val elapsed = if (serviceState.isPaused.value) {
                        accumulatedTimeMs
                    } else {
                        accumulatedTimeMs + (System.currentTimeMillis() - recordingStartTimeMs)
                    }
                    serviceState.updateElapsedTime(elapsed)

                    if (!serviceState.isPaused.value) {
                        val formatted = formatTime(elapsed)
                        val chunkInfo = "Chunk ${serviceState.currentChunkIndex.value + 1}"
                        notifyNotification("$formatted — $chunkInfo", paused = false)
                    }
                }
            }
        }
    }

    // ── Room helpers ───────────────────────────────────────────────────

    private fun updateSessionStatus(status: String) {
        val sid = sessionId ?: return
        serviceScope.launch {
            val existing = recordingRepository.getSessionByIdOnce(sid) ?: return@launch
            recordingRepository.updateSession(
                existing.copy(status = status, updatedAt = System.currentTimeMillis())
            )
        }
    }

    private fun updateSessionInputSource(source: String) {
        val sid = sessionId ?: return
        serviceScope.launch {
            val existing = recordingRepository.getSessionByIdOnce(sid) ?: return@launch
            recordingRepository.updateSession(
                existing.copy(activeInputSource = source, updatedAt = System.currentTimeMillis())
            )
        }
    }

    // ── Notification ───────────────────────────────────────────────────

    private fun notifyNotification(contentText: String, paused: Boolean) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(contentText, paused))
    }

    private fun buildNotification(contentText: String, paused: Boolean): Notification {
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

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TwinMind Recording")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_mic_notification)
            .setOngoing(true)
            .setContentIntent(tapPending)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (paused) {
            val resumeIntent = Intent(this, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(
                this, 2, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_mic_notification, "Resume", resumePending)
            builder.addAction(R.drawable.ic_stop_notification, "Stop", stopPending)
        } else {
            builder.addAction(R.drawable.ic_stop_notification, "Stop", stopPending)
        }

        return builder.build()
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }
}
