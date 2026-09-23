package com.sweep.cleaner.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Size
import com.sweep.cleaner.data.local.SweepDatabase
import com.sweep.cleaner.data.local.TrashEntity
import com.sweep.cleaner.model.ContactItem
import com.sweep.cleaner.model.DuplicateContactGroup
import com.sweep.cleaner.model.MediaItem
import com.sweep.cleaner.model.SafDocumentItem
import com.sweep.cleaner.model.SafScanResult
import com.sweep.cleaner.model.SimilarPhotoGroup
import com.sweep.cleaner.model.StorageSummary
import com.sweep.cleaner.util.ContactUtils
import com.sweep.cleaner.util.ImageHasher
import com.sweep.cleaner.util.SafScanner
import com.sweep.cleaner.util.SimilarPhotoDetector
import com.sweep.cleaner.util.TrashExpiryHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class TrashOperationResult {
    data class Success(val movedCount: Int, val freedBytes: Long) : TrashOperationResult()
    data class RequiresConfirmation(val intentSender: IntentSender, val pendingTrashItems: List<TrashEntity>) : TrashOperationResult()
    data class PartialFailure(val movedCount: Int, val failedCount: Int, val errorMessage: String) : TrashOperationResult()
}

class StorageRepository(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val database = SweepDatabase.getInstance(context)
    private val trashDao = database.trashDao()

    private val trashDir: File by lazy {
        File(context.filesDir, "trash").apply {
            if (!exists()) mkdirs()
        }
    }

    val allTrashItems: Flow<List<TrashEntity>> = trashDao.getAllTrashItems()
    val totalTrashSizeBytes: Flow<Long?> = trashDao.getTotalTrashSizeBytes()
    val trashCount: Flow<Int> = trashDao.getTrashItemCount()

    suspend fun getStorageSummary(): StorageSummary = withContext(Dispatchers.IO) {
        var totalBytes = 0L
        var freeBytes = 0L
        var isStorageStatsAvailable = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                if (storageStatsManager != null) {
                    val appUuid = StorageManager.UUID_DEFAULT
                    totalBytes = storageStatsManager.getTotalBytes(appUuid)
                    freeBytes = storageStatsManager.getFreeBytes(appUuid)
                    isStorageStatsAvailable = true
                }
            }
        } catch (_: Exception) {
            // Fallback to StatFs
        }

        if (totalBytes <= 0L) {
            try {
                val dataDir = Environment.getDataDirectory()
                val statFs = StatFs(dataDir.path)
                val blockSize = statFs.blockSizeLong
                totalBytes = statFs.blockCountLong * blockSize
                freeBytes = statFs.availableBlocksLong * blockSize
                isStorageStatsAvailable = true
            } catch (_: Exception) {
                totalBytes = 64_000_000_000L
                freeBytes = 16_000_000_000L
            }
        }

        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        // Query MediaStore categories
        val (photoBytes, photoCount) = queryMediaCategoryTotals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val (videoBytes, videoCount) = queryMediaCategoryTotals(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val (audioBytes, audioCount) = queryMediaCategoryTotals(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val (docBytes, docCount) = queryDocumentsTotals()

        val accountedMedia = photoBytes + videoBytes + audioBytes + docBytes
        val otherBytes = (usedBytes - accountedMedia).coerceAtLeast(0L)

        StorageSummary(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            photoBytes = photoBytes,
            videoBytes = videoBytes,
            audioBytes = audioBytes,
            docBytes = docBytes,
            otherBytes = otherBytes,
            photoCount = photoCount,
            videoCount = videoCount,
            audioCount = audioCount,
            docCount = docCount,
            isStorageStatsAvailable = isStorageStatsAvailable
        )
    }

    private fun queryMediaCategoryTotals(uri: Uri): Pair<Long, Int> {
        var totalBytes = 0L
        var count = 0
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    val size = cursor.getLong(sizeCol)
                    if (size > 0) {
                        totalBytes += size
                        count++
                    }
                }
            }
        } catch (_: Exception) {
            // Permission or system limitation handled gracefully
        }
        return Pair(totalBytes, count)
    }

    private fun queryDocumentsTotals(): Pair<Long, Int> {
        var totalBytes = 0L
        var count = 0
        val projection = arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DISPLAY_NAME)
        val selection = "${MediaStore.MediaColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.MediaColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.MediaColumns.MIME_TYPE} = ? OR " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf(
            "%pdf%",
            "%document%",
            "text/plain",
            "%.zip",
            "%.apk"
        )
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }
            contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    val size = cursor.getLong(sizeCol)
                    if (size > 0) {
                        totalBytes += size
                        count++
                    }
                }
            }
        } catch (_: Exception) {
            // Silently fallback to zero on scoped storage restrictions
        }
        return Pair(totalBytes, count)
    }

    suspend fun getPhotos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                items.addAll(parseImagesCursor(cursor))
            }
        } catch (_: Exception) {}
        items
    }

    suspend fun getScreenshots(): List<MediaItem> = withContext(Dispatchers.IO) {
        val allPhotos = getPhotos()
        allPhotos.filter { item ->
            item.bucketName.contains("screenshot", ignoreCase = true) ||
                    item.displayName.contains("screenshot", ignoreCase = true) ||
                    item.displayName.startsWith("Screenshot_", ignoreCase = true) ||
                    item.displayName.startsWith("Screen_Shot", ignoreCase = true)
        }.map { it.copy(isScreenshot = true) }
    }

    suspend fun getSimilarPhotoGroups(): List<SimilarPhotoGroup> = withContext(Dispatchers.IO) {
        val photos = getPhotos().take(100) // Process latest 100 photos for rapid responsiveness
        val hashedPhotos = mutableListOf<Pair<MediaItem, Long>>()

        for (photo in photos) {
            try {
                val bitmap = loadThumbnail(photo.uri, 128, 128)
                if (bitmap != null) {
                    val hash = ImageHasher.computeDHash(bitmap)
                    hashedPhotos.add(Pair(photo, hash))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // One unreadable item never cancels scan - skip it and continue
            }
        }

        SimilarPhotoDetector.clusterSimilarPhotos(hashedPhotos)
    }

    suspend fun getLargeVideos(minSizeBytes: Long = 20_000_000L): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.SIZE} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    if (size >= minSizeBytes || items.size < 30) {
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        val name = cursor.getString(nameCol) ?: "Video_$id"
                        val date = cursor.getLong(dateCol) * 1000
                        val mime = cursor.getString(mimeCol) ?: "video/*"
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""
                        val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                        val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                        val duration = if (durCol != -1) cursor.getLong(durCol) else 0L

                        items.add(
                            MediaItem(
                                id = id,
                                uri = uri,
                                displayName = name,
                                sizeBytes = size,
                                dateModifiedMs = date,
                                mimeType = mime,
                                bucketName = bucket,
                                width = width,
                                height = height,
                                durationMs = duration
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        items.sortedByDescending { it.sizeBytes }
    }

    suspend fun getDocuments(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (?, ?, ?, ?, ?, ?) OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "%.pdf", "%.doc%", "%.xls%", "%.zip"
        )

        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }
            contentResolver.query(uri, projection, selection, args, "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    val name = cursor.getString(nameCol) ?: "Document_$id"
                    val date = cursor.getLong(dateCol) * 1000
                    val mime = cursor.getString(mimeCol) ?: "application/octet-stream"

                    items.add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(uri, id),
                            displayName = name,
                            sizeBytes = size,
                            dateModifiedMs = date,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        items
    }

    suspend fun getAudioFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION
        )
        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    val name = cursor.getString(nameCol) ?: "Audio_$id"
                    val date = cursor.getLong(dateCol) * 1000
                    val mime = cursor.getString(mimeCol) ?: "audio/*"
                    val duration = if (durCol != -1) cursor.getLong(durCol) else 0L

                    items.add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            displayName = name,
                            sizeBytes = size,
                            dateModifiedMs = date,
                            mimeType = mime,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        items
    }

    suspend fun getApkFiles(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val args = arrayOf("%.apk", "application/vnd.android.package-archive")

        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri("external")
            }
            contentResolver.query(uri, projection, selection, args, "${MediaStore.Files.FileColumns.SIZE} DESC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    val name = cursor.getString(nameCol) ?: "Installer_$id.apk"
                    val date = cursor.getLong(dateCol) * 1000
                    val mime = cursor.getString(mimeCol) ?: "application/vnd.android.package-archive"

                    items.add(
                        MediaItem(
                            id = id,
                            uri = ContentUris.withAppendedId(uri, id),
                            displayName = name,
                            sizeBytes = size,
                            dateModifiedMs = date,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        items
    }

    suspend fun getOldDownloads(daysThreshold: Int = 180): List<MediaItem> = withContext(Dispatchers.IO) {
        val cutoffMs = System.currentTimeMillis() - (daysThreshold.toLong() * 24 * 60 * 60 * 1000)
        val items = mutableListOf<MediaItem>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED,
                MediaStore.Downloads.MIME_TYPE
            )
            try {
                contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Downloads.DATE_MODIFIED} <= ?",
                    arrayOf((cutoffMs / 1000).toString()),
                    "${MediaStore.Downloads.DATE_MODIFIED} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val size = cursor.getLong(sizeCol)
                        val name = cursor.getString(nameCol) ?: "Download_$id"
                        val date = cursor.getLong(dateCol) * 1000
                        val mime = cursor.getString(mimeCol) ?: "application/octet-stream"

                        items.add(
                            MediaItem(
                                id = id,
                                uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id),
                                displayName = name,
                                sizeBytes = size,
                                dateModifiedMs = date,
                                mimeType = mime
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        items
    }

    suspend fun getChatMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val chatBuckets = listOf("WhatsApp", "Telegram", "Signal", "WhatsApp Images", "WhatsApp Video", "Telegram Images", "Telegram Video")
        val allPhotos = getPhotos()
        val allVideos = getLargeVideos(0L)

        (allPhotos + allVideos).filter { item ->
            chatBuckets.any { item.bucketName.contains(it, ignoreCase = true) || item.relativePath.contains(it, ignoreCase = true) }
        }
    }

    suspend fun getDuplicateContacts(): List<DuplicateContactGroup> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactItem>()
        try {
            val cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val lookupCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
                val nameCol = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val lookup = c.getString(lookupCol) ?: id.toString()
                    val name = c.getString(nameCol) ?: "Unnamed"

                    // Query phones for this contact
                    val phones = mutableListOf<String>()
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id.toString()),
                        null
                    )?.use { pc ->
                        val numCol = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (pc.moveToNext()) {
                            if (numCol != -1) pc.getString(numCol)?.let { phones.add(it) }
                        }
                    }

                    // Query emails
                    val emails = mutableListOf<String>()
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                        arrayOf(id.toString()),
                        null
                    )?.use { ec ->
                        val emailCol = ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        while (ec.moveToNext()) {
                            if (emailCol != -1) ec.getString(emailCol)?.let { emails.add(it) }
                        }
                    }

                    contacts.add(
                        ContactItem(
                            id = id,
                            lookupKey = lookup,
                            displayName = name,
                            phoneNumbers = phones,
                            emails = emails
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        ContactUtils.findDuplicateContacts(contacts)
    }

    /**
     * Safely moves items to private 30-day trash:
     * 1. Copies bytes to filesDir/trash/<uuid>
     * 2. Writes Room row with metadata + 30-day expiry
     * 3. Deletes source via ContentResolver
     */
    suspend fun moveToTrash(items: List<MediaItem>, category: String): TrashOperationResult = withContext(Dispatchers.IO) {
        var movedCount = 0
        var freedBytes = 0L
        val pendingEntities = mutableListOf<TrashEntity>()
        val urisToDelete = mutableListOf<Uri>()

        for (item in items) {
            try {
                val trashId = UUID.randomUUID().toString()
                val targetFile = File(trashDir, trashId)

                // Step 1: Copy bytes to safety location first
                val copied = copyUriToFile(item.uri, targetFile)
                if (!copied) continue

                val now = System.currentTimeMillis()
                val expiry = TrashExpiryHelper.calculateExpiryTimestamp(now)
                val entity = TrashEntity(
                    id = trashId,
                    originalUri = item.uri.toString(),
                    displayName = item.displayName,
                    sizeBytes = item.sizeBytes,
                    mimeType = item.mimeType,
                    localFilePath = targetFile.absolutePath,
                    deletedTimestamp = now,
                    expiryTimestamp = expiry,
                    category = category
                )

                // Step 2: Write Room row
                trashDao.insertTrashItem(entity)
                pendingEntities.add(entity)
                urisToDelete.add(item.uri)

                // Step 3: Delete from contentResolver
                try {
                    val rowsDeleted = contentResolver.delete(item.uri, null, null)
                    if (rowsDeleted > 0) {
                        movedCount++
                        freedBytes += item.sizeBytes
                    }
                } catch (secEx: SecurityException) {
                    // Check for RecoverableSecurityException on Android 10+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && secEx is RecoverableSecurityException) {
                        return@withContext TrashOperationResult.RequiresConfirmation(
                            intentSender = secEx.userAction.actionIntent.intentSender,
                            pendingTrashItems = pendingEntities
                        )
                    }
                }
            } catch (e: Exception) {
                // Defensive: one failed item doesn't stop the whole batch
            }
        }

        // On Android 11+, if items were protected and need MediaStore confirmation:
        if (movedCount == 0 && urisToDelete.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, urisToDelete)
                return@withContext TrashOperationResult.RequiresConfirmation(
                    intentSender = pendingIntent.intentSender,
                    pendingTrashItems = pendingEntities
                )
            } catch (_: Exception) {}
        }

        TrashOperationResult.Success(movedCount = movedCount, freedBytes = freedBytes)
    }

    /**
     * Executes a deep scan on a user-granted Storage Access Framework (SAF) folder tree.
     */
    suspend fun scanSafTree(
        treeUri: Uri,
        thresholdBytes: Long = SafScanner.DEFAULT_LARGE_FILE_THRESHOLD_BYTES,
        onProgress: suspend (Int, String) -> Unit
    ): SafScanResult {
        val scanner = SafScanner(context)
        return scanner.scanTree(treeUri, thresholdBytes, onProgress)
    }

    /**
     * Safely moves items selected via SAF into Sweep's 30-day private quarantine trash,
     * then deletes the document via DocumentsContract.deleteDocument.
     */
    suspend fun moveSafItemsToTrash(items: List<SafDocumentItem>): TrashOperationResult = withContext(Dispatchers.IO) {
        var movedCount = 0
        var freedBytes = 0L

        for (item in items) {
            try {
                val trashId = UUID.randomUUID().toString()
                val targetFile = File(trashDir, trashId)

                // Step 1: Copy bytes to safety quarantine location first
                val copied = copyUriToFile(item.uri, targetFile)
                if (!copied) continue

                val now = System.currentTimeMillis()
                val expiry = TrashExpiryHelper.calculateExpiryTimestamp(now)
                val entity = TrashEntity(
                    id = trashId,
                    originalUri = item.uri.toString(),
                    displayName = item.displayName,
                    sizeBytes = item.sizeBytes,
                    mimeType = item.mimeType,
                    localFilePath = targetFile.absolutePath,
                    deletedTimestamp = now,
                    expiryTimestamp = expiry,
                    category = "SAF_STORAGE"
                )

                // Step 2: Record in Room database
                trashDao.insertTrashItem(entity)

                // Step 3: Delete from SAF tree
                val deleted = try {
                    DocumentsContract.deleteDocument(contentResolver, item.uri)
                } catch (e: Exception) {
                    false
                }

                if (deleted) {
                    movedCount++
                    freedBytes += item.sizeBytes
                }
            } catch (_: Exception) {
                // Ignore individual document errors and continue batch
            }
        }

        TrashOperationResult.Success(movedCount = movedCount, freedBytes = freedBytes)
    }

    /**
     * Restores media from private trash back into MediaStore under Pictures/SweepRestored,
     * Music/SweepRestored, or Download/SweepRestored, then deletes local copy and Room row.
     */
    suspend fun restoreTrashItem(trashEntity: TrashEntity): Boolean = withContext(Dispatchers.IO) {
        val localFile = File(trashEntity.localFilePath)
        if (!localFile.exists()) {
            trashDao.deleteTrashItemById(trashEntity.id)
            return@withContext false
        }

        try {
            val isVideo = trashEntity.mimeType.startsWith("video/")
            val isAudio = trashEntity.mimeType.startsWith("audio/")
            val isImage = trashEntity.mimeType.startsWith("image/")

            val targetCollection = when {
                isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                else -> MediaStore.Files.getContentUri("external")
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, trashEntity.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, trashEntity.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val folder = when {
                        isVideo -> "Movies/SweepRestored"
                        isAudio -> "Music/SweepRestored"
                        isImage -> "Pictures/SweepRestored"
                        else -> "Download/SweepRestored"
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val restoredUri = contentResolver.insert(targetCollection, values)
            if (restoredUri != null) {
                contentResolver.openOutputStream(restoredUri)?.use { out ->
                    localFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(restoredUri, values, null, null)
                }

                // Delete local copy and Room record
                localFile.delete()
                trashDao.deleteTrashItemById(trashEntity.id)
                return@withContext true
            }
        } catch (_: Exception) {}

        false
    }

    /**
     * Permanently deletes private trash file and Room row.
     */
    suspend fun permanentlyDeleteTrashItem(trashEntity: TrashEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val localFile = File(trashEntity.localFilePath)
            if (localFile.exists()) {
                localFile.delete()
            }
            trashDao.deleteTrashItemById(trashEntity.id)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun emptyTrash(): Int = withContext(Dispatchers.IO) {
        var count = 0
        try {
            val all = trashDir.listFiles() ?: emptyArray()
            for (f in all) {
                if (f.delete()) count++
            }
            trashDao.deleteAll()
        } catch (_: Exception) {}
        count
    }

    suspend fun purgeExpiredTrashItems(): Int = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expired = trashDao.getExpiredTrashItems(now)
        var purgedCount = 0
        for (item in expired) {
            File(item.localFilePath).delete()
            trashDao.deleteTrashItemById(item.id)
            purgedCount++
        }
        purgedCount
    }

    private fun copyUriToFile(uri: Uri, dest: File): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            dest.exists() && dest.length() > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun loadThumbnail(uri: Uri, width: Int, height: Int): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(uri, Size(width, height), null)
            } else {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 8
                    }
                    BitmapFactory.decodeStream(stream, null, options)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseImagesCursor(cursor: Cursor): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
        val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val widthCol = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val name = cursor.getString(nameCol) ?: "Image_$id"
            val size = cursor.getLong(sizeCol)
            val date = cursor.getLong(dateCol) * 1000
            val mime = cursor.getString(mimeCol) ?: "image/*"
            val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""
            val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
            val height = if (heightCol != -1) cursor.getInt(heightCol) else 0

            items.add(
                MediaItem(
                    id = id,
                    uri = uri,
                    displayName = name,
                    sizeBytes = size,
                    dateModifiedMs = date,
                    mimeType = mime,
                    bucketName = bucket,
                    width = width,
                    height = height
                )
            )
        }
        return items
    }
}
