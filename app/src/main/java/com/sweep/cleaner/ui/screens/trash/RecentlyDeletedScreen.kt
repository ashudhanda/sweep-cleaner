package com.sweep.cleaner.ui.screens.trash

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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sweep.cleaner.data.local.TrashEntity
import com.sweep.cleaner.ui.components.PermanentDeleteConfirmDialog
import com.sweep.cleaner.ui.components.SweepTopBar
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.DangerRed
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter
import com.sweep.cleaner.util.TrashExpiryHelper
import java.io.File

@Composable
fun RecentlyDeletedScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val totalTrashSize by viewModel.totalTrashSize.collectAsStateWithLifecycle()

    var itemToPermanentDelete by remember { mutableStateOf<TrashEntity?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    itemToPermanentDelete?.let { item ->
        PermanentDeleteConfirmDialog(
            itemName = item.displayName,
            onConfirm = {
                itemToPermanentDelete = null
                viewModel.permanentlyDeleteTrashItem(item) { success ->
                    if (success) {
                        Toast.makeText(context, "Item permanently deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { itemToPermanentDelete = null }
        )
    }

    if (showEmptyTrashDialog) {
        PermanentDeleteConfirmDialog(
            itemName = "all ${trashItems.size} items in trash",
            onConfirm = {
                showEmptyTrashDialog = false
                viewModel.emptyTrash { count ->
                    Toast.makeText(context, "Permanently removed $count items", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showEmptyTrashDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("recently_deleted_screen")
    ) {
        SweepTopBar(
            title = "Recently Deleted",
            subtitle = "${trashItems.size} items • ${ByteFormatter.formatBytes(totalTrashSize ?: 0L)}",
            onBackClick = onBackClick,
            actions = {
                if (trashItems.isNotEmpty()) {
                    TextButton(onClick = { showEmptyTrashDialog = true }) {
                        Text("Empty Trash", color = DangerRed)
                    }
                }
            }
        )

        if (trashItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recently Deleted is Empty",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Files moved to trash stay safely stored in private storage for 30 days before automatic purge.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Info banner
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
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Items are safely recoverable for 30 days. Tapping restore will place the file back in your public Pictures folder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(trashItems, key = { it.id }) { item ->
                    TrashItemCard(
                        item = item,
                        onRestore = {
                            viewModel.restoreTrashItem(item) { success ->
                                if (success) {
                                    Toast.makeText(context, "Restored \"${item.displayName}\"", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to restore", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeletePermanent = { itemToPermanentDelete = item }
                    )
                }
            }
        }
    }
}

@Composable
fun TrashItemCard(
    item: TrashEntity,
    onRestore: () -> Unit,
    onDeletePermanent: () -> Unit
) {
    val daysRemaining = TrashExpiryHelper.calculateDaysRemaining(item.deletedTimestamp)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                val file = File(item.localFilePath)
                if (file.exists() && item.mimeType.startsWith("image/")) {
                    AsyncImage(
                        model = file,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = ByteFormatter.formatBytes(item.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandAmberLight
                        ) {
                            Text(
                                text = "$daysRemaining days left",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandAmber,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        tint = BrandTeal
                    )
                }
                IconButton(onClick = onDeletePermanent) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete Permanently",
                        tint = DangerRed
                    )
                }
            }
        }
    }
}
