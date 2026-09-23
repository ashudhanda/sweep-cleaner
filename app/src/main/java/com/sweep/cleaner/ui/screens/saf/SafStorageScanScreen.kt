package com.sweep.cleaner.ui.screens.saf

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sweep.cleaner.model.SafDocumentItem
import com.sweep.cleaner.model.SafDuplicateGroup
import com.sweep.cleaner.ui.components.PermanentDeleteConfirmDialog
import com.sweep.cleaner.ui.theme.BrandAmber
import com.sweep.cleaner.ui.theme.BrandAmberLight
import com.sweep.cleaner.ui.theme.BrandTeal
import com.sweep.cleaner.ui.theme.BrandTealBright
import com.sweep.cleaner.ui.theme.BrandTealMint
import com.sweep.cleaner.ui.theme.CategoryDoc
import com.sweep.cleaner.ui.theme.CategoryPhoto
import com.sweep.cleaner.ui.theme.CategoryVideo
import com.sweep.cleaner.ui.theme.DangerRed
import com.sweep.cleaner.ui.viewmodel.SweepViewModel
import com.sweep.cleaner.util.ByteFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafStorageScanScreen(
    viewModel: SweepViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val safState by viewModel.safScanState.collectAsStateWithLifecycle()
    val lastSafUriString by viewModel.lastSafTreeUri.collectAsStateWithLifecycle()
    val lastSafFolderName by viewModel.lastSafFolderName.collectAsStateWithLifecycle()

    val selectedLargeUris by viewModel.selectedSafLargeFileUris.collectAsStateWithLifecycle()
    val selectedDuplicateUris by viewModel.selectedSafDuplicateUris.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showConfirmCleanDialog by remember { mutableStateOf(false) }

    // Launcher for OpenDocumentTree (SAF)
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            } catch (_: Exception) {}

            val folderName = treeUri.lastPathSegment?.substringAfterLast(":") ?: "Selected Folder"
            viewModel.selectSafTree(treeUri, folderName)
        }
    }

    val totalSelectedCount = selectedLargeUris.size + selectedDuplicateUris.size
    val totalSelectedBytes = remember(selectedLargeUris, selectedDuplicateUris, safState.result) {
        val result = safState.result ?: return@remember 0L
        var bytes = 0L
        result.largeFiles.forEach {
            if (selectedLargeUris.contains(it.uri)) bytes += it.sizeBytes
        }
        result.duplicateGroups.forEach { group ->
            group.items.forEach {
                if (selectedDuplicateUris.contains(it.uri)) bytes += it.sizeBytes
            }
        }
        bytes
    }

    if (showConfirmCleanDialog) {
        PermanentDeleteConfirmDialog(
            itemName = "$totalSelectedCount items (${ByteFormatter.formatBytes(totalSelectedBytes)})",
            onConfirm = {
                showConfirmCleanDialog = false
                viewModel.cleanSelectedSafItems { count, freedBytes ->
                    if (count > 0) {
                        Toast.makeText(
                            context,
                            "Moved $count items to 30-day Trash (${ByteFormatter.formatBytes(freedBytes)} freed)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(context, "No items cleaned", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showConfirmCleanDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SAF Deep Storage Scan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (safState.isScanning) safState.progressStatus
                            else safState.result?.rootDisplayName ?: (lastSafFolderName ?: "Select storage folder"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { openDocumentTreeLauncher.launch(null) }) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Pick folder")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = totalSelectedCount > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$totalSelectedCount items selected",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${ByteFormatter.formatBytes(totalSelectedBytes)} to 30-day trash",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandTeal
                            )
                        }

                        Button(
                            onClick = { showConfirmCleanDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clean Selected")
                        }
                    }
                }
            }
        },
        modifier = modifier.testTag("saf_storage_scan_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Picker Banner
            SafTreePickerCard(
                folderName = safState.result?.rootDisplayName ?: lastSafFolderName,
                isScanning = safState.isScanning,
                onPickFolder = { openDocumentTreeLauncher.launch(null) },
                onRescan = {
                    lastSafUriString?.let { uriStr ->
                        viewModel.startSafScan(Uri.parse(uriStr))
                    } ?: openDocumentTreeLauncher.launch(null)
                }
            )

            // Live Scanning Indicator
            if (safState.isScanning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BrandTeal,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(44.dp)
                        )

                        Text(
                            text = safState.progressStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (safState.currentFolder.isNotEmpty()) {
                            Text(
                                text = safState.currentFolder,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.cancelSafScan() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel Scan")
                        }
                    }
                }
            }

            // Error display
            safState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Results Section
            val scanResult = safState.result
            if (scanResult != null && !safState.isScanning) {
                // Tab Selection: Large Files vs Duplicate Groups
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text("Large Files (${scanResult.largeFiles.size})")
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text("Duplicates (${scanResult.duplicateGroups.size})")
                        }
                    )
                }

                if (selectedTab == 0) {
                    SafLargeFilesList(
                        largeFiles = scanResult.largeFiles,
                        selectedUris = selectedLargeUris,
                        onToggleSelect = { viewModel.toggleSafLargeFileSelection(it) },
                        onSelectAll = { viewModel.selectAllSafLargeFiles(it) }
                    )
                } else {
                    SafDuplicatesList(
                        groups = scanResult.duplicateGroups,
                        selectedUris = selectedDuplicateUris,
                        onToggleSelect = { viewModel.toggleSafDuplicateSelection(it) },
                        onSelectDuplicatesOnly = { viewModel.selectAllSafDuplicatesExceptRecommended() }
                    )
                }
            } else if (!safState.isScanning && safState.result == null) {
                // Empty state when no scan has been run yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BrandTealMint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Storage Access Framework (SAF)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Tap 'Select Storage Folder' to grant access to your Downloads, Documents, or external SD card. Sweep will scan for duplicate files (exact SHA-256 verification) and large files without uploading anything.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Button(
                            onClick = { openDocumentTreeLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Storage Folder")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SafTreePickerCard(
    folderName: String?,
    isScanning: Boolean,
    onPickFolder: () -> Unit,
    onRescan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BrandTealMint.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = folderName ?: "No Folder Selected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (folderName != null) "Storage tree access granted" else "Choose a directory to scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (folderName != null && !isScanning) {
                    IconButton(onClick = onRescan) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rescan", tint = BrandTeal)
                    }
                }
                OutlinedButton(
                    onClick = onPickFolder,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (folderName != null) "Change" else "Browse", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun SafLargeFilesList(
    largeFiles: List<SafDocumentItem>,
    selectedUris: Set<Uri>,
    onToggleSelect: (Uri) -> Unit,
    onSelectAll: (Boolean) -> Unit
) {
    if (largeFiles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No large files found (threshold: 25 MB)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val allSelected = largeFiles.isNotEmpty() && largeFiles.all { selectedUris.contains(it.uri) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${largeFiles.size} large files (${ByteFormatter.formatBytes(largeFiles.sumOf { it.sizeBytes })})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onSelectAll(!allSelected) }) {
                    Text(if (allSelected) "Deselect All" else "Select All")
                }
            }
        }

        items(largeFiles, key = { it.uri.toString() }) { item ->
            val isSelected = selectedUris.contains(item.uri)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSelect(item.uri) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) BrandTealMint.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(getMimeTypeColor(item.mimeType).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getMimeTypeIcon(item.mimeType),
                                contentDescription = null,
                                tint = getMimeTypeColor(item.mimeType),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = item.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = ByteFormatter.formatBytes(item.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTeal
                                )
                                Text(
                                    text = "• ${item.relativePath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect(item.uri) },
                        colors = CheckboxDefaults.colors(checkedColor = BrandTeal)
                    )
                }
            }
        }
    }
}

@Composable
fun SafDuplicatesList(
    groups: List<SafDuplicateGroup>,
    selectedUris: Set<Uri>,
    onToggleSelect: (Uri) -> Unit,
    onSelectDuplicatesOnly: () -> Unit
) {
    if (groups.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No duplicate files found under selected directory",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val totalReclaimable = groups.sumOf { it.reclaimableBytes }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${groups.size} duplicate groups",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ByteFormatter.formatBytes(totalReclaimable)} reclaimable • SHA-256 verified",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onSelectDuplicatesOnly) {
                    Text("Select Duplicates (Keep 1)")
                }
            }
        }

        items(groups, key = { it.groupId }) { group ->
            SafDuplicateGroupCard(
                group = group,
                selectedUris = selectedUris,
                onToggleSelect = onToggleSelect
            )
        }
    }
}

@Composable
fun SafDuplicateGroupCard(
    group: SafDuplicateGroup,
    selectedUris: Set<Uri>,
    onToggleSelect: (Uri) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = BrandAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${group.totalDuplicateCount} exact copies",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandAmberLight
                ) {
                    Text(
                        text = "${ByteFormatter.formatBytes(group.fileSize)} each",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandAmber,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Items in group
            group.items.forEach { item ->
                val isKeeper = item.uri == group.recommendedKeepItem.uri
                val isSelected = selectedUris.contains(item.uri)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) DangerRed.copy(alpha = 0.08f)
                            else if (isKeeper) BrandTealMint.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onToggleSelect(item.uri) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isKeeper) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = BrandTealMint.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = "Keep (Original)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = BrandTeal,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = item.relativePath,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect(item.uri) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = DangerRed
                        )
                    )
                }
            }
        }
    }
}

private fun getMimeTypeIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("image/") -> Icons.Default.Image
        mimeType.startsWith("video/") -> Icons.Default.Movie
        mimeType.startsWith("audio/") -> Icons.Default.MusicNote
        mimeType == "application/zip" || mimeType.contains("tar") || mimeType.contains("rar") -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }
}

private fun getMimeTypeColor(mimeType: String): Color {
    return when {
        mimeType.startsWith("image/") -> CategoryPhoto
        mimeType.startsWith("video/") -> CategoryVideo
        mimeType.startsWith("audio/") -> BrandTealMint
        else -> CategoryDoc
    }
}
