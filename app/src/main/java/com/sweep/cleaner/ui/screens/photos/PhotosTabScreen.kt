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
import androidx.compose.runtime.getValue
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
                subtitle = "Detect bursts and duplicate shots via visual hashing",
                sizeBytes = summary.photoBytes / 4,
                icon = Icons.Default.PhotoLibrary,
                iconTint = BrandTealBright,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToSimilarPhotos
            )

            QuickCleanCard(
                title = "Screenshots",
                subtitle = "Review and clean captured screens",
                sizeBytes = 0L,
                icon = Icons.Default.Image,
                iconTint = CategoryPhoto,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToScreenshots
            )
        }
    }
}
