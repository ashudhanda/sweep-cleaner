package com.sweep.cleaner.ui.screens.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.QuickCleanCard
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandTealBright
import com.sweep.cleaner.ui.theme.CategoryPhoto
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun PhotosTabScreen(
    viewModel: SweepViewModel,
    onNavigateToSimilarPhotos: () -> Unit,
    onNavigateToScreenshots: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.storageSummary.collectAsStateWithLifecycle()
    val similarGroups by viewModel.similarPhotoGroups.collectAsStateWithLifecycle()
    val screenshots by viewModel.screenshots.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadScreenshots()
        if (similarGroups.isEmpty()) {
            viewModel.loadSimilarPhotos()
        }
    }

    val similarBytes = remember(similarGroups) {
        similarGroups.flatMap { g -> g.items.filter { it.id != g.bestShotId } }.sumOf { it.sizeBytes }
    }
    val screenshotsBytes = remember(screenshots) {
        screenshots.sumOf { it.sizeBytes }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("photos_tab_screen")
    ) {
        SweepTopBar(
            title = "Photos",
            subtitle = "${summary.photoCount} photos • ${ByteFormatter.formatBytes(summary.photoBytes)}"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Photo Cleaning",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            QuickCleanCard(
                title = "Similar Photos",
                subtitle = if (similarGroups.isNotEmpty()) "${similarGroups.size} duplicate groups found" else "Detect bursts and duplicate shots via visual hashing",
                sizeBytes = similarBytes,
                icon = Icons.Default.PhotoLibrary,
                iconTint = BrandTealBright,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToSimilarPhotos
            )

            QuickCleanCard(
                title = "Screenshots",
                subtitle = if (screenshots.isNotEmpty()) "${screenshots.size} screenshots found" else "Review and clean captured screens",
                sizeBytes = screenshotsBytes,
                icon = Icons.Default.Image,
                iconTint = CategoryPhoto,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToScreenshots
            )
        }
    }
}
