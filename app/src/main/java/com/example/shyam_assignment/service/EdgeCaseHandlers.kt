package com.example.shyam_assignment.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.StatFs
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.math.abs

private const val TAG = "EdgeCaseHandlers"

// ════════════════════════════════════════════════════════════════════════
//  1.  Phone-call monitor
// ════════════════════════════════════════════════════════════════════════

/**
 * Detects incoming/outgoing phone calls and invokes [onCallStarted] /
 * [onCallEnded] so the service can pause / resume recording.
 */
class PhoneCallMonitor(private val context: Context) {

    interface Callback {
        fun onCallStarted()
        fun onCallEnded()
    }

    private var callback: Callback? = null
    private var telephonyCallback: Any? = null  // TelephonyCallback for API 31+
    private var phoneStateReceiver: BroadcastReceiver? = null
    private var wasInCall = false

    fun start(cb: Callback) {
        callback = cb
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startApi31(cb)
        } else {
            startLegacy(cb)
        }
    }

    fun stop() {
        callback = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopApi31()
        } else {
            stopLegacy()
        }
    }

    // API 31+
    @RequiresApi(Build.VERSION_CODES.S)
    private fun startApi31(cb: Callback) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val tcb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleState(state, cb)
            }
        }
        telephonyCallback = tcb
        try {
            tm.registerTelephonyCallback(context.mainExecutor, tcb)
        } catch (e: SecurityException) {
            Log.w(TAG, "No READ_PHONE_STATE permission", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopApi31() {
        val tcb = telephonyCallback as? TelephonyCallback ?: return
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        tm.unregisterTelephonyCallback(tcb)
        telephonyCallback = null
    }

    // Pre-API 31
    @Suppress("DEPRECATION")
    private fun startLegacy(cb: Callback) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING,
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (!wasInCall) { wasInCall = true; cb.onCallStarted() }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (wasInCall) { wasInCall = false; cb.onCallEnded() }
                    }
                }
            }
        }
        phoneStateReceiver = receiver
        context.registerReceiver(
            receiver,
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        )
    }

    private fun stopLegacy() {
        phoneStateReceiver?.let { context.unregisterReceiver(it) }
        phoneStateReceiver = null
    }

    private fun handleState(state: Int, cb: Callback) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!wasInCall) { wasInCall = true; cb.onCallStarted() }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (wasInCall) { wasInCall = false; cb.onCallEnded() }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  2.  Battery monitor
// ════════════════════════════════════════════════════════════════════════

/**
 * Monitors battery level and fires [onBatteryLow] when it drops below
 * [LOW_BATTERY_PERCENT]. The recording should stop gracefully.
 */
class BatteryMonitor(private val context: Context) {

    companion object {
        private const val LOW_BATTERY_PERCENT = 6
    }

    interface Callback {
        fun onBatteryLow(level: Int)
    }

    private var receiver: BroadcastReceiver? = null
    private var hasFired = false

    fun start(cb: Callback) {
        hasFired = false
        receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (hasFired) return
                val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: return
                val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
                val percent = (level * 100) / scale
                if (percent in 0 until LOW_BATTERY_PERCENT) {
                    hasFired = true
                    cb.onBatteryLow(percent)
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    /**
     * One-shot check: returns true if battery is currently below threshold.
     */
    fun isBatteryLow(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return level in 0 until LOW_BATTERY_PERCENT
    }

    fun stop() {
        receiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        receiver = null
    }
}

// ════════════════════════════════════════════════════════════════════════
//  3.  Audio-source (headset / BT) monitor
// ════════════════════════════════════════════════════════════════════════

class AudioSourceMonitor(private val context: Context) {

    interface Callback {
        fun onSourceChanged(newSource: String)
    }

    private var callback: Callback? = null
    private var headsetReceiver: BroadcastReceiver? = null
    private var btReceiver: BroadcastReceiver? = null
    private var deviceCallback: AudioDeviceCallback? = null

    fun start(cb: Callback) {
        callback = cb

        // Wired headset
        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val plugged = intent?.getIntExtra("state", 0) == 1
                val source = if (plugged) "WIRED_HEADSET" else "MICROPHONE"
                cb.onSourceChanged(source)
            }
        }
        context.registerReceiver(
            headsetReceiver,
            IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        )

        // Bluetooth SCO
        btReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
                val source = if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) "BLUETOOTH" else "MICROPHONE"
                cb.onSourceChanged(source)
            }
        }
        context.registerReceiver(
            btReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )

        // Device callback for fine-grained detection
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                addedDevices?.forEach { dev ->
                    if (dev.isSource) {
                        val src = mapDeviceType(dev.type)
                        if (src != null) cb.onSourceChanged(src)
                    }
                }
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                cb.onSourceChanged("MICROPHONE") // fallback
            }
        }
        am.registerAudioDeviceCallback(deviceCallback, null)
    }

    fun stop() {
        headsetReceiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        btReceiver?.let { try { context.unregisterReceiver(it) } catch (_: Exception) {} }
        deviceCallback?.let {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.unregisterAudioDeviceCallback(it)
        }
        headsetReceiver = null
        btReceiver = null
        deviceCallback = null
        callback = null
    }

    private fun mapDeviceType(type: Int): String? = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "MICROPHONE"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        else -> null
    }
}

// ════════════════════════════════════════════════════════════════════════
//  4.  Low-storage monitor
// ════════════════════════════════════════════════════════════════════════

object StorageMonitor {

    /** Minimum free space to allow recording: 50 MB */
    private const val MIN_FREE_BYTES = 50L * 1024 * 1024

    /**
     * Returns true if there is enough storage to start / continue recording.
     */
    fun hasEnoughStorage(context: Context): Boolean {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            stat.availableBytes >= MIN_FREE_BYTES
        } catch (_: Exception) {
            true // assume OK on error
        }
    }
}

// ════════════════════════════════════════════════════════════════════════
//  5.  Silence detector
// ════════════════════════════════════════════════════════════════════════

/**
 * Tracks consecutive silence in a PCM-16 mono stream.
 * Silence = all samples below [SILENCE_THRESHOLD] for [SILENCE_TIMEOUT_MS].
 */
class SilenceDetector {

    companion object {
        /** Absolute sample value below which we consider "silence". */
        private const val SILENCE_THRESHOLD = 200   // out of 32 768
        /** Duration of continuous silence before we fire the warning. */
        private const val SILENCE_TIMEOUT_MS = 10_000L
    }

    interface Callback {
        fun onSilenceDetected()
        fun onSoundDetected()
    }

    private var callback: Callback? = null
    private var silenceStartMs: Long = 0L
    private var isSilent = false
    private var warningSent = false

    fun start(cb: Callback) {
        callback = cb
        silenceStartMs = 0L
        isSilent = false
        warningSent = false
    }

    fun stop() {
        callback = null
    }

    /**
     * Feed a raw PCM-16 LE buffer. Call this on every AudioRecord read.
     */
    fun feed(buffer: ByteArray, bytesRead: Int) {
        val cb = callback ?: return
        val now = System.currentTimeMillis()

        // Calculate RMS-ish: check if any sample exceeds threshold
        var loud = false
        var i = 0
        while (i + 1 < bytesRead) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val signed = if (sample > 32767) sample - 65536 else sample
            if (abs(signed) > SILENCE_THRESHOLD) {
                loud = true
                break
            }
            i += 2
        }

        if (loud) {
            if (isSilent || warningSent) {
                isSilent = false
                warningSent = false
                cb.onSoundDetected()
            }
            silenceStartMs = 0L
        } else {
            if (silenceStartMs == 0L) {
                silenceStartMs = now
            }
            if (!warningSent && (now - silenceStartMs) >= SILENCE_TIMEOUT_MS) {
                isSilent = true
                warningSent = true
                cb.onSilenceDetected()
            }
        }
    }
}



