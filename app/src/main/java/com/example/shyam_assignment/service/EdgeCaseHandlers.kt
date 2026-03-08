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

private const val TAG = "EdgeCaseHandlers"

// ════════════════════════════════════════════════════════════════════════
//  1.  Phone-call monitor  (multi-strategy: AudioManager mode polling
//      + TelephonyManager broadcast + AudioFocus listener)
// ════════════════════════════════════════════════════════════════════════

/**
 * Detects incoming/outgoing phone calls reliably.
 *
 * Three strategies run in parallel for maximum coverage:
 *  A) **AudioManager.mode polling** — every 500 ms, check if mode
 *     switches to MODE_IN_CALL / MODE_RINGTONE / MODE_IN_COMMUNICATION.
 *     Needs no permissions.
 *  B) **PHONE_STATE broadcast** — works on older APIs or when
 *     READ_PHONE_STATE has been granted.
 *  C) **TelephonyCallback** (API 31+) — registered inside try/catch
 *     so it's a bonus, not a requirement.
 *
 * Any strategy that fires first wins; duplicates are de-duplicated via
 * the [wasInCall] flag.
 */
class PhoneCallMonitor(private val context: Context) {

    interface Callback {
        fun onCallStarted()
        fun onCallEnded()
    }

    private var callback: Callback? = null

    /* shared flag — set/read on any thread, but simple boolean is fine */
    @Volatile private var wasInCall = false

    /* Strategy A — mode polling */
    private var pollingThread: Thread? = null
    @Volatile private var polling = false

    /* Strategy B — broadcast receiver */
    private var phoneStateReceiver: BroadcastReceiver? = null

    /* Strategy C — TelephonyCallback (API 31+) */
    private var telephonyCallback: Any? = null

    // ────────────────────────────────────────────────────────────────

    fun start(cb: Callback) {
        callback = cb
        wasInCall = false
        startModePolling(cb)
        startBroadcastReceiver(cb)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startTelephonyCallback(cb)
        }
    }

    fun stop() {
        callback = null
        stopModePolling()
        stopBroadcastReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopTelephonyCallback()
        }
    }

    // ── Strategy A: AudioManager.mode polling (no permissions) ─────

    private fun startModePolling(cb: Callback) {
        polling = true
        pollingThread = Thread {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            while (polling) {
                try {
                    val mode = am.mode
                    val inCall = mode == AudioManager.MODE_IN_CALL ||
                                 mode == AudioManager.MODE_RINGTONE ||
                                 mode == AudioManager.MODE_IN_COMMUNICATION
                    if (inCall && !wasInCall) {
                        wasInCall = true
                        Log.d(TAG, "PhoneCallMonitor: call detected via AudioManager.mode=$mode")
                        cb.onCallStarted()
                    } else if (!inCall && wasInCall) {
                        wasInCall = false
                        Log.d(TAG, "PhoneCallMonitor: call ended via AudioManager.mode=$mode")
                        cb.onCallEnded()
                    }
                    Thread.sleep(500)
                } catch (_: InterruptedException) { break }
                  catch (e: Exception) { Log.e(TAG, "Mode polling error", e) }
            }
        }.apply {
            name = "PhoneCallPoll"
            isDaemon = true
            start()
        }
    }

    private fun stopModePolling() {
        polling = false
        pollingThread?.interrupt()
        pollingThread = null
    }

    // ── Strategy B: PHONE_STATE broadcast ──────────────────────────

    @Suppress("DEPRECATION")
    private fun startBroadcastReceiver(cb: Callback) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING,
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (!wasInCall) {
                            wasInCall = true
                            Log.d(TAG, "PhoneCallMonitor: call detected via broadcast state=$state")
                            cb.onCallStarted()
                        }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (wasInCall) {
                            wasInCall = false
                            Log.d(TAG, "PhoneCallMonitor: call ended via broadcast")
                            cb.onCallEnded()
                        }
                    }
                }
            }
        }
        phoneStateReceiver = receiver
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register phone state receiver", e)
            phoneStateReceiver = null
        }
    }

    private fun stopBroadcastReceiver() {
        phoneStateReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        phoneStateReceiver = null
    }

    // ── Strategy C: TelephonyCallback (API 31+) ────────────────────

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startTelephonyCallback(cb: Callback) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val tcb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING,
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (!wasInCall) {
                            wasInCall = true
                            Log.d(TAG, "PhoneCallMonitor: call detected via TelephonyCallback")
                            cb.onCallStarted()
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (wasInCall) {
                            wasInCall = false
                            Log.d(TAG, "PhoneCallMonitor: call ended via TelephonyCallback")
                            cb.onCallEnded()
                        }
                    }
                }
            }
        }
        telephonyCallback = tcb
        try {
            tm.registerTelephonyCallback(context.mainExecutor, tcb)
        } catch (e: SecurityException) {
            Log.w(TAG, "TelephonyCallback registration failed (no permission)", e)
            telephonyCallback = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun stopTelephonyCallback() {
        val tcb = telephonyCallback as? TelephonyCallback ?: return
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            tm.unregisterTelephonyCallback(tcb)
        } catch (_: Exception) {}
        telephonyCallback = null
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
        private const val LOW_BATTERY_PERCENT = 3
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
//  5.  Silence detector — WARNING only (does NOT stop recording)
// ════════════════════════════════════════════════════════════════════════

/**
 * Tracks consecutive silence in a PCM-16 mono stream.
 *
 * After [SILENCE_TIMEOUT_MS] (10 s) of silence → fires [onSilenceDetected]
 * to show the warning banner.  When sound resumes → fires [onSoundDetected]
 * to clear the warning.  Recording continues regardless.
 *
 * Uses RMS amplitude for robustness instead of single-sample checks.
 */
class SilenceDetector {

    companion object {
        /**
         * RMS amplitude below which a buffer counts as "silent".
         * 16-bit PCM range is ±32 768.  150 catches near-zero mic
         * noise while still allowing very quiet speech through.
         */
        private const val RMS_SILENCE_THRESHOLD = 150

        /** Duration of continuous silence before we fire the warning. */
        private const val SILENCE_TIMEOUT_MS = 10_000L
    }

    interface Callback {
        fun onSilenceDetected()
        fun onSoundDetected()
    }

    private var callback: Callback? = null
    private var silenceStartMs: Long = 0L
    private var warningSent = false

    fun start(cb: Callback) {
        callback = cb
        silenceStartMs = 0L
        warningSent = false
    }

    fun stop() {
        callback = null
    }

    /**
     * Feed a raw PCM-16-LE buffer.  Call on every AudioRecord read.
     */
    fun feed(buffer: ByteArray, bytesRead: Int) {
        val cb = callback ?: return
        val now = System.currentTimeMillis()

        val rms = computeRms(buffer, bytesRead)

        if (rms > RMS_SILENCE_THRESHOLD) {
            // ── Sound detected ──────────────────────────────────────
            if (warningSent) {
                warningSent = false
                cb.onSoundDetected()
            }
            silenceStartMs = 0L
        } else {
            // ── Silence ─────────────────────────────────────────────
            if (silenceStartMs == 0L) {
                silenceStartMs = now
            }
            if (!warningSent && (now - silenceStartMs) >= SILENCE_TIMEOUT_MS) {
                warningSent = true
                Log.w(TAG, "10s silence detected — showing warning")
                cb.onSilenceDetected()
            }
        }
    }

    /**
     * Compute the RMS (root-mean-square) of the PCM-16 LE buffer.
     */
    private fun computeRms(buffer: ByteArray, bytesRead: Int): Double {
        if (bytesRead < 2) return 0.0
        var sumSquares = 0.0
        var sampleCount = 0
        var i = 0
        while (i + 1 < bytesRead) {
            val lo = buffer[i].toInt() and 0xFF
            val hi = buffer[i + 1].toInt()
            val sample = (hi shl 8) or lo
            val signed = sample.toShort().toInt()
            sumSquares += (signed.toLong() * signed.toLong()).toDouble()
            sampleCount++
            i += 2
        }
        return if (sampleCount > 0) kotlin.math.sqrt(sumSquares / sampleCount) else 0.0
    }
}



