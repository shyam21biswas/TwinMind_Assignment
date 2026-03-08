package com.example.shyam_assignment.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shyam_assignment.data.local.entity.RecordingSessionEntity
import com.example.shyam_assignment.data.local.entity.SummaryEntity
import com.example.shyam_assignment.data.local.entity.SummaryStatus
import com.example.shyam_assignment.data.local.entity.TranscriptSegmentEntity
import com.example.shyam_assignment.ui.theme.TwinCardBorder
import com.example.shyam_assignment.ui.theme.TwinElevatedCard
import com.example.shyam_assignment.ui.theme.TwinError
import com.example.shyam_assignment.ui.theme.TwinGradientEnd
import com.example.shyam_assignment.ui.theme.TwinGradientStart
import com.example.shyam_assignment.ui.theme.TwinPrimary
import com.example.shyam_assignment.ui.theme.TwinSecondary
import com.example.shyam_assignment.ui.theme.TwinTextSecondary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Auto-trigger summary generation when entering screen
    LaunchedEffect(Unit) {
        viewModel.generateSummary(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Meeting Summary",
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
        when {
            uiState.isLoading -> {
                LoadingPlaceholder(
                    message = "Loading session...",
                    modifier = Modifier.padding(innerPadding)
                )
            }

            uiState.session == null -> {
                ErrorPlaceholder(
                    error = uiState.error ?: "Session not found",
                    onRetry = null,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            else -> {
                SummaryContent(
                    session = uiState.session!!,
                    summary = uiState.summary,
                    transcript = uiState.transcript,
                    isSummaryGenerating = uiState.isSummaryGenerating,
                    summaryStatus = uiState.summaryStatus,
                    summaryError = if (uiState.summaryStatus == SummaryStatus.FAILED) uiState.error else null,
                    onRetrySummary = { viewModel.retrySummary(context) },
                    onGenerateSummary = { viewModel.generateSummary(context) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun SummaryContent(
    session: RecordingSessionEntity,
    summary: SummaryEntity?,
    transcript: List<TranscriptSegmentEntity>,
    isSummaryGenerating: Boolean,
    summaryStatus: String?,
    summaryError: String?,
    onRetrySummary: () -> Unit,
    onGenerateSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionItems = remember(summary?.actionItemsJson) {
        parseJsonList(summary?.actionItemsJson)
    }
    val keyPoints = remember(summary?.keyPointsJson) {
        parseJsonList(summary?.keyPointsJson)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title Card
        SummaryCard(
            icon = Icons.AutoMirrored.Outlined.Article,
            title = "Meeting",
            iconTint = TwinPrimary
        ) {
            Text(
                text = if (summaryStatus == SummaryStatus.COMPLETED && !summary?.title.isNullOrBlank())
                    summary!!.title
                else
                    session.title ?: "Untitled Recording",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TwinPrimary.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Duration: ${formatDuration(session.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TwinTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary generation status
        if (isSummaryGenerating) {
            SummaryGeneratingCard()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Summary error + retry
        if (summaryError != null) {
            SummaryErrorCard(error = summaryError, onRetry = onRetrySummary)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Generate button only if no summary exists, not generating, not completed, and has transcript
        if (summaryStatus == null && !isSummaryGenerating && transcript.isNotEmpty()) {
            GenerateSummaryButton(onClick = onGenerateSummary)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Summary Card
        SummaryCard(
            icon = Icons.AutoMirrored.Outlined.Notes,
            title = "Summary",
            iconTint = TwinPrimary
        ) {
            val summaryText = summary?.summary.orEmpty()
            val isCompleted = summaryStatus == SummaryStatus.COMPLETED
            Text(
                text = when {
                    summaryText.isNotBlank() -> summaryText
                    isCompleted -> "No summary available."
                    isSummaryGenerating -> "Generating summary with Gemini AI..."
                    transcript.isEmpty() -> "No transcript available. Record a meeting first."
                    else -> "Summary will be generated by Gemini AI..."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (summaryText.isBlank()) TwinTextSecondary
                else MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Items Card — hide if completed with no items
        val isCompleted = summaryStatus == SummaryStatus.COMPLETED
        if (actionItems.isNotEmpty() || !isCompleted) {
            SummaryCard(
                icon = Icons.Outlined.CheckCircle,
                title = "Action Items",
                iconTint = TwinSecondary
            ) {
                if (actionItems.isEmpty()) {
                    Text(
                        text = if (isSummaryGenerating) "Extracting action items..."
                        else if (isCompleted) "No action items identified."
                        else "Action items will be extracted by Gemini AI...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TwinTextSecondary
                    )
                } else {
                    actionItems.forEachIndexed { index, item ->
                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(TwinSecondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TwinSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Key Points Card — hide if completed with no points
        if (keyPoints.isNotEmpty() || !isCompleted) {
            SummaryCard(
                icon = Icons.Outlined.Lightbulb,
                title = "Key Points",
                iconTint = TwinPrimary
            ) {
                if (keyPoints.isEmpty()) {
                    Text(
                        text = if (isSummaryGenerating) "Identifying key points..."
                        else if (isCompleted) "No key points identified."
                        else "Key points will be identified by Gemini AI...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TwinTextSecondary
                    )
                } else {
                    keyPoints.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(TwinGradientStart, TwinGradientEnd)
                                        )
                                    )
                                    .padding(top = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Transcript Card
        if (transcript.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SummaryCard(
                icon = Icons.AutoMirrored.Outlined.Notes,
                title = "Transcript",
                iconTint = TwinTextSecondary
            ) {
                transcript.forEachIndexed { index, segment ->
                    Text(
                        text = segment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    if (index < transcript.lastIndex) {
                        HorizontalDivider(
                            color = TwinCardBorder.copy(alpha = 0.3f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Composable helpers ─────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TwinElevatedCard)
    ) {
        Column {
            // Top accent gradient line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                iconTint.copy(alpha = 0.6f),
                                iconTint.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.1f))
                            .border(1.dp, iconTint.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
private fun SummaryGeneratingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TwinElevatedCard)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TwinPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = TwinPrimary,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Generating Summary...",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Gemini AI is analyzing your transcript",
                    style = MaterialTheme.typography.bodySmall,
                    color = TwinTextSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryErrorCard(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TwinError.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, TwinError.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                text = "Summary Generation Failed",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TwinError
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = TwinError.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = TwinPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
private fun GenerateSummaryButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = TwinPrimary),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = "Generate Summary with Gemini AI",
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun LoadingPlaceholder(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TwinElevatedCard),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = TwinPrimary,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.5.dp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TwinTextSecondary
            )
        }
    }
}

@Composable
private fun ErrorPlaceholder(
    error: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TwinError.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TwinError
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = TwinError
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = TwinPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

// ── Utility functions ──────────────────────────────────────────────────

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

private fun parseJsonList(json: String?): List<String> {
    if (json.isNullOrBlank() || json == "[]") return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson(json, type)
    } catch (_: Exception) {
        emptyList()
    }
}
