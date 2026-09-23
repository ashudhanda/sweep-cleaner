package com.sweep.cleaner.ui.screens.tools

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.CategoryAudio
import com.sweep.cleaner.ui.theme.CategoryDoc
import com.sweep.cleaner.ui.theme.CategoryPhoto
import com.sweep.cleaner.ui.theme.CategoryVideo
import com.sweep.cleaner.ui.viewmodel.SweepViewModel

@Composable
fun ToolsScreen(
    viewModel: SweepViewModel,
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("tools_screen")
    ) {
        SweepTopBar(
            title = "Clean & Manage",
            subtitle = "All utilities"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SAF Deep Storage Scanner Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.SAF_SCAN) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandTealMint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "SAF Deep Storage Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Scan any directory or SD card for large files and SHA-256 duplicate items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Review Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    ToolItemRow(
                        title = "SAF Storage & Duplicates",
                        subtitle = "Targeted folder scanning & exact duplicate analysis",
                        icon = Icons.Default.FolderOpen,
                        iconTint = BrandTeal,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.SAF_SCAN) }
                    )
                    ToolItemRow(
                        title = "Similar Photos",
                        subtitle = "Find near-duplicate camera shots",
                        icon = Icons.Default.Image,
                        iconTint = CategoryPhoto,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.SIMILAR_PHOTOS) }
                    )
                    ToolItemRow(
                        title = "Screenshots",
                        subtitle = "Review and clean screen captures",
                        icon = Icons.Default.Image,
                        iconTint = CategoryPhoto,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.SCREENSHOTS) }
                    )
                    ToolItemRow(
                        title = "Large Videos",
                        subtitle = "Sorted by file size with compress estimates",
                        icon = Icons.Default.VideoLibrary,
                        iconTint = CategoryVideo,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.LARGE_VIDEOS) }
                    )
                    ToolItemRow(
                        title = "Chat Media",
                        subtitle = "WhatsApp, Telegram, and Signal files",
                        icon = Icons.Default.Chat,
                        iconTint = BrandTealMint,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.CHAT_MEDIA) }
                    )
                    ToolItemRow(
                        title = "APK Installers",
                        subtitle = "Packages left in Downloads",
                        icon = Icons.Default.Android,
                        iconTint = BrandTeal,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.APKS) }
                    )
                    ToolItemRow(
                        title = "Old Downloads",
                        subtitle = "Files older than 180 days",
                        icon = Icons.Default.Download,
                        iconTint = BrandTeal,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.OLD_DOWNLOADS) }
                    )
                    ToolItemRow(
                        title = "Duplicate Contacts",
                        subtitle = "Safe merge previews with zero trash loss",
                        icon = Icons.Default.Contacts,
                        iconTint = BrandAmber,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.DUPLICATE_CONTACTS) }
                    )
                }
            }

            Text("System & Storage Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    ToolItemRow(
                        title = "Recently Deleted",
                        subtitle = "30-day reversible safety trash ($trashCount items)",
                        icon = Icons.Default.Delete,
                        iconTint = BrandAmber,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.RECENTLY_DELETED) }
                    )
                    ToolItemRow(
                        title = "Storage Breakdown",
                        subtitle = "Exact byte metrics by media category",
                        icon = Icons.Default.PieChart,
                        iconTint = CategoryDoc,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.STORAGE_BREAKDOWN) }
                    )
                    ToolItemRow(
                        title = "Storage Health & Tips",
                        subtitle = "Honest hygiene guidance without fake scores",
                        icon = Icons.Default.HealthAndSafety,
                        iconTint = BrandTealMint,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.STORAGE_HEALTH) }
                    )
                    ToolItemRow(
                        title = "App Leftovers",
                        subtitle = "Public folder analysis with scoped storage truth",
                        icon = Icons.Default.FolderZip,
                        iconTint = CategoryAudio,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.APP_LEFTOVERS) }
                    )
                    ToolItemRow(
                        title = "App Manager",
                        subtitle = "Native Android system settings",
                        icon = Icons.Default.Apps,
                        iconTint = BrandTeal,
                        onClick = { onNavigateToRoute(com.sweep.cleaner.ui.navigation.NavRoutes.UNUSED_APPS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ToolItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp)
        )
    }
}
