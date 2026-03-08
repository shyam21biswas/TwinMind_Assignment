package com.example.shyam_assignment

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.shyam_assignment.service.RecordingService
import com.example.shyam_assignment.worker.RecoveryManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class — the entry point of the app.
 * Sets up Hilt dependency injection, creates notification channels,
 * and runs recovery for any sessions interrupted by a previous crash.
 */
@HiltAndroidApp
class TwinMindApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory    // Hilt-aware factory for WorkManager workers

    @Inject
    lateinit var recoveryManager: RecoveryManager    // Handles crash recovery on startup

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)  // Background scope for startup tasks

    /** Tells WorkManager to use Hilt for creating workers */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()   // Set up notification channel for recording
        runStartupRecovery()           // Recover any sessions left in a bad state
    }

    /** Creates the notification channel for the recording foreground service */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                RecordingService.CHANNEL_ID,
                "Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing recording status"
                setShowBadge(false)
            }

            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    /** Runs recovery in the background — marks interrupted sessions as stopped,
     *  re-enqueues pending transcriptions and summaries */
    private fun runStartupRecovery() {
        appScope.launch {
            try {
                recoveryManager.recover()
            } catch (e: Exception) {
                Log.e("TwinMindApp", "Startup recovery failed", e)
            }
        }
    }
}
