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
import java.io.File
import java.io.FileOutputStream
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
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

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
    private var outputFile: File? = null
    private var outputStream: FileOutputStream? = null

    private var sessionId: String? = null
    private var recordingStartTimeMs: Long = 0L
    private var accumulatedTimeMs: Long = 0L

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
        if (serviceState.isRecording.value) return // already recording

        val id = UUID.randomUUID().toString()
        sessionId = id
        val now = System.currentTimeMillis()

        // Create output directory
        val dir = File(filesDir, "recordings").also { it.mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
        val file = File(dir, "rec_${timestamp}.pcm")
        outputFile = file

        // Persist session to Room
        serviceScope.launch {
            val session = RecordingSessionEntity(
                sessionId = id,
                title = "Recording ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(now))}",
                startedAt = now,
                status = SessionStatus.RECORDING,
                activeInputSource = "MICROPHONE",
                createdAt = now,
                updatedAt = now
            )
            recordingRepository.insertSession(session)
        }

        // Start foreground immediately
        startForeground(NOTIFICATION_ID, buildNotification("Recording..."))

        // Update shared state
        serviceState.updateSessionId(id)
        serviceState.updateRecording(true)
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording...")
        serviceState.updateError(null)
        serviceState.updateWarning(null)
        serviceState.updateElapsedTime(0L)
        accumulatedTimeMs = 0L
        recordingStartTimeMs = System.currentTimeMillis()

        // Start audio capture
        startAudioCapture(file)

        // Start timer
        startTimer()
    }

    // ── Stop recording ─────────────────────────────────────────────────

    private fun handleStop() {
        timerJob?.cancel()
        timerJob = null

        stopAudioCapture()

        val now = System.currentTimeMillis()
        val elapsed = if (recordingStartTimeMs > 0)
            accumulatedTimeMs + (now - recordingStartTimeMs)
        else 0L

        // Update Room
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
        serviceState.updateRecording(false)
        serviceState.updatePaused(false)
        serviceState.updateStatus("Recording stopped")
        serviceState.updateElapsedTime(elapsed)

        sessionId = null
        recordingStartTimeMs = 0L
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Audio capture ──────────────────────────────────────────────────

    private fun startAudioCapture(file: File) {
        recordingJob = serviceScope.launch {
            try {
                val bufferSize = maxOf(
                    AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
                    4096
                )

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
                outputStream = FileOutputStream(file)
                recorder.startRecording()

                val buffer = ByteArray(bufferSize)
                while (isActive && serviceState.isRecording.value) {
                    if (!serviceState.isPaused.value) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            outputStream?.write(buffer, 0, read)
                        }
                    } else {
                        delay(100) // idle while paused
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording error", e)
                serviceState.updateError("Recording error: ${e.message}")
                serviceState.updateStatus("Error")
            }
        }
    }

    private fun stopAudioCapture() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null
        try {
            outputStream?.flush()
            outputStream?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing output stream", e)
        }
        outputStream = null
    }

    // ── Timer ──────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1_000)
                if (!serviceState.isPaused.value && serviceState.isRecording.value) {
                    val elapsed = accumulatedTimeMs + (System.currentTimeMillis() - recordingStartTimeMs)
                    serviceState.updateElapsedTime(elapsed)

                    // Update notification periodically
                    val formatted = formatTime(elapsed)
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification("Recording — $formatted"))
                }
            }
        }
    }

    // ── Notification ───────────────────────────────────────────────────

    private fun buildNotification(contentText: String): Notification {
        // Tap notification → open app
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPending = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
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


