package com.sweep.cleaner.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.QuickCleanCard
import com.sweep.cleaner.ui.components.SectionHeader
import com.sweep.cleaner.ui.components.StorageRing
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealBright
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.BrandTealSurface
import com.sweep.cleaner.ui.theme.CategoryAudio
import com.sweep.cleaner.ui.theme.CategoryDoc
import com.sweep.cleaner.ui.theme.CategoryOther
import com.sweep.cleaner.ui.theme.CategoryPhoto
import com.sweep.cleaner.ui.theme.CategoryVideo
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun DashboardScreen(
    viewModel: SweepViewModel,
    onNavigateToSmartScan: () -> Unit,
    onNavigateToSimilarPhotos: () -> Unit,
    onNavigateToScreenshots: () -> Unit,
    onNavigateToLargeVideos: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToBreakdown: () -> Unit,
    onNavigateToSafScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.storageSummary.collectAsStateWithLifecycle()
    val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshStorageSummary()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 32.dp)
            .testTag("dashboard_screen")
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sweep",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sweep the junk away.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick Trash badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable(onClick = onNavigateToTrash)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Recently Deleted Trash",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (trashCount > 0) {
                            Text(
                                text = "$trashCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("dashboard_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Storage Overview Card (24dp rounded corners, matching prototype)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("storage_overview_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Animated storage ring
                    StorageRing(
                        usedBytes = summary.usedBytes,
                        totalBytes = summary.totalBytes,
                        size = 130.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = ByteFormatter.formatBytes(summary.freeBytes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandAmberLight,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(BrandAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (summary.usedPercentage > 0.8f) "Cleanup recommended" else "Storage healthy",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandAmber
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Mini Category Dots Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniCategoryDot(CategoryPhoto, "Photos")
                    MiniCategoryDot(CategoryVideo, "Videos")
                    MiniCategoryDot(CategoryAudio, "Audio")
                    MiniCategoryDot(CategoryDoc, "Docs")
                    MiniCategoryDot(CategoryOther, "Other")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToBreakdown)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View full storage breakdown",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Smart Scan Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToSmartScan)
                .testTag("hero_smart_scan_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BrandTeal),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandTealSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Smart Scan",
                            tint = BrandTealMint,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Smart scan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Review before cleaning",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandTealMint
                ) {
                    Text(
                        text = "Start",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandTeal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Clean Section
        SectionHeader(title = "Quick clean")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickCleanCard(
                title = "Similar photos",
                subtitle = "${summary.photoCount} photos scanned",
                sizeBytes = summary.photoBytes / 4,
                icon = Icons.Default.PhotoLibrary,
                iconTint = BrandTealBright,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToSimilarPhotos
            )

            QuickCleanCard(
                title = "Screenshots",
                subtitle = "Review older screen captures",
                sizeBytes = 0L,
                icon = Icons.Default.Image,
                iconTint = CategoryPhoto,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToScreenshots
            )

            QuickCleanCard(
                title = "Large videos",
                subtitle = "${summary.videoCount} video clips found",
                sizeBytes = summary.videoBytes,
                icon = Icons.Default.VideoLibrary,
                iconTint = CategoryVideo,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToLargeVideos
            )

            QuickCleanCard(
                title = "Duplicate contacts",
                subtitle = "Safe merge preview",
                sizeBytes = 0L,
                icon = Icons.Default.Contacts,
                iconTint = BrandAmber,
                iconBackground = BrandAmberLight,
                onClick = onNavigateToContacts
            )

            QuickCleanCard(
                title = "SAF Deep Scanner",
                subtitle = "External folders, SD cards & duplicates",
                sizeBytes = 0L,
                icon = Icons.Default.FolderOpen,
                iconTint = BrandTeal,
                iconBackground = MaterialTheme.colorScheme.surfaceVariant,
                onClick = onNavigateToSafScan
            )
        }
    }
}

@Composable
fun MiniCategoryDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
