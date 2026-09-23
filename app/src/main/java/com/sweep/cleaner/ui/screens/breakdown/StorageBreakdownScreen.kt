package com.sweep.cleaner.ui.screens.breakdown

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.CategoryRowItem
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.CategoryAudio
import com.sweep.cleaner.ui.theme.CategoryDoc
import com.sweep.cleaner.ui.theme.CategoryOther
import com.sweep.cleaner.ui.theme.CategoryPhoto
import com.sweep.cleaner.ui.theme.CategoryVideo
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun StorageBreakdownScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    onNavigateToPhotos: () -> Unit,
    onNavigateToVideos: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToDocs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.storageSummary.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("storage_breakdown_screen")
    ) {
        SweepTopBar(
            title = "Storage Breakdown",
            subtitle = "${ByteFormatter.formatBytes(summary.usedBytes)} of ${ByteFormatter.formatBytes(summary.totalBytes)} used",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Segmented Storage Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Storage Allocation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Multi-color segmented row
                    val total = summary.totalBytes.coerceAtLeast(1L).toFloat()
                    val pPhoto = (summary.photoBytes / total).coerceIn(0.01f, 1f)
                    val pVideo = (summary.videoBytes / total).coerceIn(0.01f, 1f)
                    val pAudio = (summary.audioBytes / total).coerceIn(0.01f, 1f)
                    val pDoc = (summary.docBytes / total).coerceIn(0.01f, 1f)
                    val pOther = (summary.otherBytes / total).coerceIn(0.01f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Box(modifier = Modifier.weight(pPhoto).fillMaxSize().background(CategoryPhoto))
                        Box(modifier = Modifier.weight(pVideo).fillMaxSize().background(CategoryVideo))
                        Box(modifier = Modifier.weight(pAudio).fillMaxSize().background(CategoryAudio))
                        Box(modifier = Modifier.weight(pDoc).fillMaxSize().background(CategoryDoc))
                        Box(modifier = Modifier.weight(pOther).fillMaxSize().background(CategoryOther))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "${ByteFormatter.formatBytes(summary.freeBytes)} available for new photos, apps, and system updates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Categories Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CategoryRowItem(
                        title = "Photos & Images",
                        count = summary.photoCount,
                        sizeBytes = summary.photoBytes,
                        dotColor = CategoryPhoto,
                        onClick = onNavigateToPhotos
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    CategoryRowItem(
                        title = "Videos & Movies",
                        count = summary.videoCount,
                        sizeBytes = summary.videoBytes,
                        dotColor = CategoryVideo,
                        onClick = onNavigateToVideos
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    CategoryRowItem(
                        title = "Audio & Music",
                        count = summary.audioCount,
                        sizeBytes = summary.audioBytes,
                        dotColor = CategoryAudio,
                        onClick = onNavigateToAudio
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    CategoryRowItem(
                        title = "Documents & Files",
                        count = summary.docCount,
                        sizeBytes = summary.docBytes,
                        dotColor = CategoryDoc,
                        onClick = onNavigateToDocs
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    CategoryRowItem(
                        title = "Other & System",
                        count = 0,
                        sizeBytes = summary.otherBytes,
                        dotColor = CategoryOther,
                        onClick = {},
                        isInspectable = false
                    )
                }
            }
        }
    }
}
