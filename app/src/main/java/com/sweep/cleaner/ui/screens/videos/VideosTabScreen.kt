package com.sweep.cleaner.ui.screens.videos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.QuickCleanCard
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.CategoryVideo
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun VideosTabScreen(
    viewModel: SweepViewModel,
    onNavigateToLargeVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.storageSummary.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("videos_tab_screen")
    ) {
        SweepTopBar(
            title = "Videos",
            subtitle = "${summary.videoCount} videos • ${ByteFormatter.formatBytes(summary.videoBytes)}"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Video Optimization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            QuickCleanCard(
                title = "Large Videos",
                subtitle = "Sorted largest-first with ~55% compress estimates",
                sizeBytes = summary.videoBytes,
                icon = Icons.Default.VideoLibrary,
                iconTint = CategoryVideo,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToLargeVideos
            )
        }
    }
}
