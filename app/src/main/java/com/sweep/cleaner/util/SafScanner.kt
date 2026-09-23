package com.sweep.cleaner.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.sweep.cleaner.model.SafDocumentItem
import com.sweep.cleaner.model.SafDuplicateGroup
import com.sweep.cleaner.model.SafScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.util.ArrayDeque
import kotlin.coroutines.coroutineContext

/**
 * High-performance Storage Access Framework (SAF) scanner.
 * Traverses SAF directory trees iteratively, detects large files,
 * and identifies duplicate files via multi-stage fast hashing.
 */
class SafScanner(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        const val DEFAULT_LARGE_FILE_THRESHOLD_BYTES = 25L * 1024L * 1024L // 25 MB
        private const val PARTIAL_HASH_CHUNK_SIZE = 16 * 1024 // 16 KB

        private val DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
    }

    /**
     * Executes a full SAF tree scan for large files and duplicates.
     *
     * @param treeUri The root URI granted by the user via ACTION_OPEN_DOCUMENT_TREE.
     * @param largeFileThresholdBytes Minimum size in bytes to classify a file as large.
     * @param onProgress Callback invoked periodically with updated scan counts.
     */
    suspend fun scanTree(
        treeUri: Uri,
        largeFileThresholdBytes: Long = DEFAULT_LARGE_FILE_THRESHOLD_BYTES,
        onProgress: suspend (scannedFiles: Int, currentPath: String) -> Unit
    ): SafScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val allFiles = mutableListOf<SafDocumentItem>()
        var directoryCount = 0
        var totalBytes = 0L

        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootName = getDocumentDisplayName(treeUri, rootDocId) ?: "Storage Root"

        // Queue of (documentId, relativePath)
        val dirQueue = ArrayDeque<Pair<String, String>>()
        dirQueue.add(Pair(rootDocId, ""))

        var scanCounter = 0

        while (dirQueue.isNotEmpty() && coroutineContext.isActive) {
            val (currentDocId, currentRelativePath) = dirQueue.removeFirst()
            directoryCount++

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocId)

            try {
                contentResolver.query(
                    childrenUri,
                    DOCUMENT_PROJECTION,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    val modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext() && coroutineContext.isActive) {
                        val childDocId = if (idCol >= 0) cursor.getString(idCol) else continue
                        val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Unnamed" else "Unnamed"
                        val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                        val size = if (sizeCol >= 0 && !cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L
                        val lastModified = if (modifiedCol >= 0 && !cursor.isNull(modifiedCol)) cursor.getLong(modifiedCol) else 0L

                        val childPath = if (currentRelativePath.isEmpty()) name else "$currentRelativePath/$name"

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            dirQueue.add(Pair(childDocId, childPath))
                        } else {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                            val item = SafDocumentItem(
                                uri = docUri,
                                documentId = childDocId,
                                treeUri = treeUri,
                                displayName = name,
                                sizeBytes = size,
                                mimeType = mimeType,
                                lastModifiedMs = lastModified,
                                relativePath = childPath,
                                isDirectory = false
                            )
                            allFiles.add(item)
                            totalBytes += size

                            scanCounter++
                            if (scanCounter % 15 == 0) {
                                onProgress(allFiles.size, childPath)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently handle inaccessible subdirectories (e.g. system protected folders)
            }
        }

        onProgress(allFiles.size, "Analyzing duplicates & large files...")

        // 1. Identify Large Files
        val largeFiles = allFiles
            .filter { it.sizeBytes >= largeFileThresholdBytes }
            .sortedByDescending { it.sizeBytes }

        // 2. Identify Duplicate Groups using Multi-Tier Hashing
        val duplicateGroups = findDuplicates(allFiles)

        val duration = System.currentTimeMillis() - startTime

        SafScanResult(
            treeUri = treeUri,
            rootDisplayName = rootName,
            totalFilesScanned = allFiles.size,
            totalDirectoriesScanned = directoryCount,
            totalBytesScanned = totalBytes,
            scanDurationMs = duration,
            largeFiles = largeFiles,
            duplicateGroups = duplicateGroups
        )
    }

    /**
     * Multi-tier duplicate detection:
     * 1. Group by exact byte length (> 1KB)
     * 2. Quick partial hash comparison (first 16KB + last 16KB)
     * 3. Full cryptographic SHA-256 hash comparison
     */
    private suspend fun findDuplicates(files: List<SafDocumentItem>): List<SafDuplicateGroup> = withContext(Dispatchers.IO) {
        // Step 1: Size grouping. Files < 1KB are skipped for duplicate analysis to avoid cluttering with tiny configs
        val sizeBuckets = files
            .filter { it.sizeBytes >= 1024L }
            .groupBy { it.sizeBytes }
            .filter { it.value.size >= 2 }

        val duplicateGroups = mutableListOf<SafDuplicateGroup>()

        for ((size, candidateFiles) in sizeBuckets) {
            if (!coroutineContext.isActive) break

            // Step 2: Partial hash grouping
            val partialHashBuckets = mutableMapOf<String, MutableList<SafDocumentItem>>()
            for (file in candidateFiles) {
                if (!coroutineContext.isActive) break
                val partialHash = computePartialHash(file.uri, file.sizeBytes)
                if (partialHash != null) {
                    partialHashBuckets.getOrPut(partialHash) { mutableListOf() }.add(file)
                }
            }

            // Step 3: For partial hash collisions, compute full SHA-256
            for ((_, partialMatches) in partialHashBuckets) {
                if (partialMatches.size < 2) continue

                val fullHashBuckets = mutableMapOf<String, MutableList<SafDocumentItem>>()
                for (file in partialMatches) {
                    if (!coroutineContext.isActive) break
                    val fullHash = computeFullHash(file.uri)
                    if (fullHash != null) {
                        fullHashBuckets.getOrPut(fullHash) { mutableListOf() }.add(file.copy(hash = fullHash))
                    }
                }

                for ((hash, matchingFiles) in fullHashBuckets) {
                    if (matchingFiles.size >= 2) {
                        // Pick the recommended keeper (oldest modified file, or shortest path)
                        val keeper = matchingFiles.minWithOrNull(
                            compareBy<SafDocumentItem> { it.lastModifiedMs }
                                .thenBy { it.relativePath.length }
                        ) ?: matchingFiles.first()

                        duplicateGroups.add(
                            SafDuplicateGroup(
                                groupId = "${size}_${hash.take(8)}",
                                fileSize = size,
                                items = matchingFiles,
                                recommendedKeepItem = keeper
                            )
                        )
                    }
                }
            }
        }

        // Sort duplicate groups by total reclaimable bytes descending
        duplicateGroups.sortedByDescending { it.reclaimableBytes }
    }

    /**
     * Reads first and last chunk of file for lightning-fast pre-filtering.
     */
    private fun computePartialHash(uri: Uri, fileSize: Long): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(PARTIAL_HASH_CHUNK_SIZE)
                val readFirst = stream.read(buffer, 0, PARTIAL_HASH_CHUNK_SIZE)
                if (readFirst > 0) {
                    digest.update(buffer, 0, readFirst)
                }

                // If file is large enough, skip to the tail
                if (fileSize > PARTIAL_HASH_CHUNK_SIZE * 2) {
                    val bytesToSkip = fileSize - (PARTIAL_HASH_CHUNK_SIZE * 2)
                    var skippedTotal = 0L
                    while (skippedTotal < bytesToSkip) {
                        val skipped = stream.skip(bytesToSkip - skippedTotal)
                        if (skipped <= 0) break
                        skippedTotal += skipped
                    }

                    val readTail = stream.read(buffer, 0, PARTIAL_HASH_CHUNK_SIZE)
                    if (readTail > 0) {
                        digest.update(buffer, 0, readTail)
                    }
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Computes full SHA-256 hash by streaming file contents.
     */
    private fun computeFullHash(uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDocumentDisplayName(treeUri: Uri, documentId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return try {
            contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (col >= 0) cursor.getString(col) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
