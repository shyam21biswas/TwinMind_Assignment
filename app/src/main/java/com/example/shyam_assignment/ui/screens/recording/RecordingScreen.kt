package com.example.shyam_assignment.ui.screens.recording

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shyam_assignment.ui.theme.TwinCardBorder
import com.example.shyam_assignment.ui.theme.TwinElevatedCard
import com.example.shyam_assignment.ui.theme.TwinError
import com.example.shyam_assignment.ui.theme.TwinGlow
import com.example.shyam_assignment.ui.theme.TwinGradientEnd
import com.example.shyam_assignment.ui.theme.TwinGradientStart
import com.example.shyam_assignment.ui.theme.TwinPrimary
import com.example.shyam_assignment.ui.theme.TwinRecordingRed
import com.example.shyam_assignment.ui.theme.TwinSurface
import com.example.shyam_assignment.ui.theme.TwinTextSecondary
import com.example.shyam_assignment.ui.theme.TwinWarning

/**
 * Recording screen — the live audio recording interface.
 * Shows: status chip, timer, record/stop button, pause controls,
 * warning/error banners, chunk info card, and live transcript.
 * Requests microphone permission before first recording.
 * Navigates to summary screen when recording stops.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    onRecordingComplete: (String) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Track if user just stopped → navigate to summary
    var previouslyRecording by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isRecording, uiState.sessionId) {
        if (previouslyRecording && !uiState.isRecording && uiState.sessionId != null) {
            onRecordingComplete(uiState.sessionId!!)
        }
        previouslyRecording = uiState.isRecording
    }

    // Permission handling
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = results[Manifest.permission.POST_NOTIFICATIONS] == true
        }
        // If audio granted, start recording
        if (hasAudioPermission) {
            viewModel.startRecording(context)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recording",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Status Chip
            StatusChip(statusText = uiState.statusText, isRecording = uiState.isRecording)

            Spacer(modifier = Modifier.height(40.dp))

            // Timer Display
            Text(
                text = uiState.formattedTime,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtle gradient bar under timer
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (uiState.isRecording)
                                listOf(TwinRecordingRed, TwinRecordingRed.copy(alpha = 0.3f))
                            else
                                listOf(TwinGradientStart, TwinGradientEnd)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Large Record Button with glow
            RecordButton(
                isRecording = uiState.isRecording,
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording(context)
                    } else {
                        if (hasAudioPermission) {
                            viewModel.startRecording(context)
                        } else {
                            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pause / Stop controls
            if (uiState.isRecording) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause/Resume
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TwinSurface)
                            .border(1.dp, TwinCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (uiState.isPaused) viewModel.resumeRecording(context)
                                else viewModel.pauseRecording()
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = if (uiState.isPaused) "Resume" else "Pause",
                                tint = TwinTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Stop
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(TwinError.copy(alpha = 0.1f))
                            .border(1.dp, TwinError.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { viewModel.stopRecording(context) }) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop",
                                tint = TwinError,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Warning area
            if (!hasAudioPermission) {
                WarningCard(message = "Microphone permission is required to record audio.")
            } else if (uiState.warningMessage != null) {
                WarningCard(message = uiState.warningMessage!!)
            }

            // Error area
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                ErrorCard(message = uiState.error!!)
            }

            // Chunk info
            if (uiState.isRecording || uiState.totalChunks > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                ChunkInfoCard(
                    currentChunkIndex = uiState.currentChunkIndex,
                    totalChunks = uiState.totalChunks,
                    isRecording = uiState.isRecording,
                    activeInputSource = uiState.activeInputSource
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Transcript
            TranscriptCard(
                segments = uiState.transcriptSegments,
                isRecording = uiState.isRecording
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Record Button with animated glow ───────────────────────────────────
// Large circular button with pulsing teal glow. Starts recording on tap.

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isRecording) TwinRecordingRed else TwinPrimary

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Animated glow ring (only when recording)
        if (isRecording) {
            val infiniteTransition = rememberInfiniteTransition(label = "record_glow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_scale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glow_alpha"
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(glowScale)
                    .clip(CircleShape)
                    .background(TwinGlow.copy(alpha = glowAlpha * 0.15f))
                    .border(
                        width = 2.dp,
                        color = buttonColor.copy(alpha = glowAlpha),
                        shape = CircleShape
                    )
            )
        }

        // Outer ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(buttonColor.copy(alpha = 0.12f))
                .border(2.dp, buttonColor.copy(alpha = 0.35f), CircleShape)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            // Inner button
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

// ── Status Chip ────────────────────────────────────────────────────────
// Shows current recording status: "Recording...", "Paused", etc.

@Composable
private fun StatusChip(statusText: String, isRecording: Boolean) {
    val chipColor = if (isRecording) TwinPrimary else TwinTextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipColor.copy(alpha = 0.1f))
            .border(1.dp, chipColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isRecording) {
                val infiniteTransition = rememberInfiniteTransition(label = "status_dot")
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "status_dot_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TwinRecordingRed.copy(alpha = dotAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = chipColor
            )
        }
    }
}

// ── Warning Card ───────────────────────────────────────────────────────
// Orange-tinted card for warnings (e.g., "No audio detected")

@Composable
private fun WarningCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TwinWarning.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = TwinWarning.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⚠", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TwinWarning
            )
        }
    }
}

// ── Error Card ─────────────────────────────────────────────────────────
// Red-tinted card for errors (e.g., "Recording stopped — Low storage")

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TwinError.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = TwinError.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "❌", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TwinError
            )
        }
    }
}

// ── Chunk Info Card ────────────────────────────────────────────────────
// Shows active chunk number, total chunks, and audio source info

@Composable
private fun ChunkInfoCard(
    currentChunkIndex: Int,
    totalChunks: Int,
    isRecording: Boolean,
    activeInputSource: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TwinSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Audio Chunks",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = TwinCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ChunkStat(label = "Active Chunk", value = "#${currentChunkIndex + 1}")
                ChunkStat(label = "Total Chunks", value = "$totalChunks")
                ChunkStat(
                    label = "Status",
                    value = if (isRecording) "Writing" else "Idle",
                    valueColor = if (isRecording) TwinPrimary else TwinTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input source
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sourceLabel = when (activeInputSource) {
                    "BLUETOOTH" -> "🎧 Bluetooth"
                    "WIRED_HEADSET" -> "🎧 Wired Headset"
                    "USB_HEADSET" -> "🔌 USB Headset"
                    else -> "🎙 Built-in Mic"
                }
                Text(
                    text = "Source: $sourceLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = TwinTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ChunkStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TwinTextSecondary
        )
    }
}

// ── Transcript Card ────────────────────────────────────────────────────
// Shows the live transcript as chunks are transcribed by Gemini

@Composable
private fun TranscriptCard(
    segments: List<TranscriptSegment>,
    isRecording: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TwinSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient accent dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(TwinGradientStart, TwinGradientEnd)
                            )
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Live Transcript",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (segments.isEmpty()) {
                Text(
                    text = if (isRecording)
                        "Transcription will appear here as chunks are processed..."
                    else
                        "Start recording to see transcript.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TwinTextSecondary
                )
            } else {
                segments.forEach { segment ->
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

