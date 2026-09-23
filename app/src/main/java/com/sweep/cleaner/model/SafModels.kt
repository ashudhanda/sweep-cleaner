package com.sweep.cleaner.model

import android.net.Uri

/**
 * Represents an individual file or directory discovered via Storage Access Framework (SAF).
 */
data class SafDocumentItem(
    val uri: Uri,
    val documentId: String,
    val treeUri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val lastModifiedMs: Long,
    val relativePath: String,
    val isDirectory: Boolean = false,
    val hash: String? = null
) {
    val isMedia: Boolean
        get() = mimeType.startsWith("image/") || mimeType.startsWith("video/") || mimeType.startsWith("audio/")

    val isApk: Boolean
        get() = mimeType == "application/vnd.android.package-archive" || displayName.endsWith(".apk", ignoreCase = true)

    val isArchive: Boolean
        get() = mimeType == "application/zip" || mimeType == "application/x-tar" ||
                mimeType == "application/x-rar-compressed" || displayName.endsWith(".zip", ignoreCase = true) ||
                displayName.endsWith(".rar", ignoreCase = true) || displayName.endsWith(".7z", ignoreCase = true)
}

/**
 * A group of duplicate files discovered under an SAF tree.
 * All files in this group have identical size and matching cryptographic content hash.
 */
data class SafDuplicateGroup(
    val groupId: String,
    val fileSize: Long,
    val items: List<SafDocumentItem>,
    val recommendedKeepItem: SafDocumentItem
) {
    val totalDuplicateCount: Int get() = items.size
    // Total potential bytes saved by keeping only 1 copy
    val reclaimableBytes: Long get() = (items.size - 1).coerceAtLeast(0) * fileSize
}

/**
 * Comprehensive results from an SAF scan session.
 */
data class SafScanResult(
    val treeUri: Uri,
    val rootDisplayName: String,
    val totalFilesScanned: Int,
    val totalDirectoriesScanned: Int,
    val totalBytesScanned: Long,
    val scanDurationMs: Long,
    val largeFiles: List<SafDocumentItem>,
    val duplicateGroups: List<SafDuplicateGroup>
) {
    val totalLargeFilesBytes: Long
        get() = largeFiles.sumOf { it.sizeBytes }

    val totalReclaimableDuplicateBytes: Long
        get() = duplicateGroups.sumOf { it.reclaimableBytes }

    val totalPotentialCleanBytes: Long
        get() = totalLargeFilesBytes + totalReclaimableDuplicateBytes
}

/**
 * Live state of an active or completed SAF scan.
 */
data class SafScanState(
    val isScanning: Boolean = false,
    val currentFolder: String = "",
    val filesScannedCount: Int = 0,
    val currentLargeFilesCount: Int = 0,
    val currentDuplicateGroupsCount: Int = 0,
    val progressStatus: String = "Idle",
    val result: SafScanResult? = null,
    val errorMessage: String? = null
)
