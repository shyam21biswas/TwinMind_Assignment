package com.example.shyam_assignment

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.shyam_assignment.service.RecordingService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TwinMindApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

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
}

