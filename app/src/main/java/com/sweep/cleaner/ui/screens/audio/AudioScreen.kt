package com.sweep.cleaner.ui.screens.audio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.model.MediaItem
import com.sweep.cleaner.ui.components.SafeDeleteConfirmDialog
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.CategoryAudio
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter

@Composable
fun AudioScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedAudioIds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAudioLoading.collectAsStateWithLifecycle()

    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (audioFiles.isEmpty()) {
            viewModel.loadAudioFiles()
        }
    }

    val selectedItems = audioFiles.filter { selectedIds.contains(it.id) }
    val totalSelectedBytes = selectedItems.sumOf { it.sizeBytes }

    if (showConfirmDialog) {
        SafeDeleteConfirmDialog(
            selectedCount = selectedItems.size,
            totalSizeBytes = totalSelectedBytes,
            onConfirm = {
                showConfirmDialog = false
                viewModel.moveSelectedToTrash(selectedItems, "Audio") { moved, _ ->
                    Toast.makeText(context, "Moved $moved audio recordings to Recently Deleted", Toast.LENGTH_SHORT).show()
                    viewModel.loadAudioFiles()
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("audio_screen")
    ) {
        SweepTopBar(
            title = "Audio & Recordings",
            subtitle = if (audioFiles.isNotEmpty()) "${audioFiles.size} audio files found" else null,
            onBackClick = onBackClick
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandTealMint)
            }
        } else if (audioFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No audio files detected", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "No music or voice recordings found in standard shared storage folders.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Non-negotiable safety banner: never auto-select based on age
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BrandAmber, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Audio files are never auto-selected based on age. Music and cherished voice recordings are preserved safely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(audioFiles, key = { it.id }) { audio ->
                    val isSelected = selectedIds.contains(audio.id)
                    AudioItemCard(
                        audio = audio,
                        isSelected = isSelected,
                        onToggle = { viewModel.toggleAudioSelection(audio.id) }
                    )
                }
            }

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
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Move to Trash")
                    }
                }
            }
        }
    }
}

@Composable
fun AudioItemCard(
    audio: MediaItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
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
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CategoryAudio.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = CategoryAudio,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = audio.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${ByteFormatter.formatBytes(audio.sizeBytes)} • ${if (audio.durationMs > 0) ByteFormatter.formatDuration(audio.durationMs) else "Audio"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = BrandTealMint, checkmarkColor = BrandTeal)
            )
        }
    }
}
