package com.example.shyam_assignment.ui.screens.recording

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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shyam_assignment.ui.theme.TwinElevatedCard
import com.example.shyam_assignment.ui.theme.TwinError
import com.example.shyam_assignment.ui.theme.TwinPrimary
import com.example.shyam_assignment.ui.theme.TwinTextSecondary
import com.example.shyam_assignment.ui.theme.TwinWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    onNavigateBack: () -> Unit,
    onRecordingComplete: (String) -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            Spacer(modifier = Modifier.height(32.dp))

            // Status Chip
            StatusChipPlaceholder(statusText = uiState.statusText, isRecording = uiState.isRecording)

            Spacer(modifier = Modifier.height(40.dp))

            // Timer Display
            Text(
                text = uiState.formattedTime,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 56.sp,
                    letterSpacing = 4.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Large Record Button
            RecordButton(
                isRecording = uiState.isRecording,
                onClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pause / Stop controls
            if (uiState.isRecording) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (uiState.isPaused) viewModel.resumeRecording()
                            else viewModel.pauseRecording()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause",
                            tint = TwinTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    IconButton(onClick = { viewModel.stopRecording() }) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint = TwinError,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Warning area placeholder
            WarningPlaceholder()

            Spacer(modifier = Modifier.height(24.dp))

            // Transcript preview placeholder
            TranscriptPreviewPlaceholder()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isRecording) TwinError else TwinPrimary

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(buttonColor.copy(alpha = 0.15f))
            .border(2.dp, buttonColor.copy(alpha = 0.4f), CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(buttonColor)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun StatusChipPlaceholder(statusText: String, isRecording: Boolean) {
    val chipColor = if (isRecording) TwinPrimary else TwinTextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(TwinPrimary)
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

@Composable
private fun WarningPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TwinWarning.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Warning area — permission or microphone alerts will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = TwinWarning
            )
        }
    }
}

@Composable
private fun TranscriptPreviewPlaceholder() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TwinElevatedCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Live Transcript",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Transcript will appear here in real-time during recording...",
                style = MaterialTheme.typography.bodyMedium,
                color = TwinTextSecondary
            )
        }
    }
}

