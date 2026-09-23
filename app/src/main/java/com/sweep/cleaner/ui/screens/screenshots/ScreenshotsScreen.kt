package com.sweep.cleaner.ui.screens.screenshots

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sweep.cleaner.ui.components.SafeDeleteConfirmDialog
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun ScreenshotsScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenshots by viewModel.screenshots.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedScreenshotIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isScreenshotsLoading.collectAsStateWithLifecycle()

    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (screenshots.isEmpty()) {
            viewModel.loadScreenshots()
        }
    }

    val selectedItems = screenshots.filter { selectedIds.contains(it.id) }
    val totalSelectedBytes = selectedItems.sumOf { it.sizeBytes }

    if (showConfirmDialog) {
        SafeDeleteConfirmDialog(
            selectedCount = selectedItems.size,
            totalSizeBytes = totalSelectedBytes,
            onConfirm = {
                showConfirmDialog = false
                viewModel.moveSelectedToTrash(selectedItems, "Screenshots") { moved, _ ->
                    Toast.makeText(context, "Moved $moved screenshots to Recently Deleted", Toast.LENGTH_SHORT).show()
                    viewModel.loadScreenshots()
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("screenshots_screen")
    ) {
        SweepTopBar(
            title = "Screenshots",
            subtitle = if (screenshots.isNotEmpty()) "${screenshots.size} screenshots found" else null,
            onBackClick = onBackClick,
            actions = {
                if (screenshots.isNotEmpty()) {
                    val allSelected = selectedIds.size == screenshots.size
                    TextButton(onClick = { viewModel.selectAllScreenshots(!allSelected) }) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandTealMint)
            }
        } else if (screenshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No screenshots found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Either you have no screenshots or scoped storage restricted album visibility.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(screenshots, key = { it.id }) { item ->
                    val isSelected = selectedIds.contains(item.id)

                    Box(
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) BrandTealMint else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.toggleScreenshotSelection(item.id) }
                    ) {
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Size pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = ByteFormatter.formatBytes(item.sizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        // Checkmark
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) BrandTealMint else Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = BrandTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom commit bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedItems.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ByteFormatter.formatBytes(totalSelectedBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = selectedItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("trash_screenshots_button")
                    ) {
                        Text("Move to Trash")
                    }
                }
            }
        }
    }
}
