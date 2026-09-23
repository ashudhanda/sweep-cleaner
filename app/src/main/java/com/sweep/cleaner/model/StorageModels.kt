package com.sweep.cleaner.model

import android.net.Uri

data class StorageSummary(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val photoBytes: Long = 0L,
    val videoBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val docBytes: Long = 0L,
    val otherBytes: Long = 0L,
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val audioCount: Int = 0,
    val docCount: Int = 0,
    val isStorageStatsAvailable: Boolean = true
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0L) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateModifiedMs: Long,
    val mimeType: String,
    val bucketName: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val relativePath: String = "",
    val isScreenshot: Boolean = false
) {
    val pixelCount: Long get() = width.toLong() * height.toLong()
}

data class SimilarPhotoGroup(
    val groupId: String,
    val bestShotId: Long,
    val items: List<MediaItem>
)

data class ContactItem(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList()
)

data class DuplicateContactGroup(
    val groupId: String,
    val primaryContact: ContactItem,
    val duplicates: List<ContactItem>,
    val survivingPhoneNumbers: List<String>,
    val survivingEmails: List<String>
)

enum class CleanerCategory(val displayName: String) {
    SIMILAR_PHOTOS("Similar Photos"),
    SCREENSHOTS("Screenshots"),
    LARGE_VIDEOS("Large Videos"),
    DOCUMENTS("Documents"),
    AUDIO("Audio"),
    APKS("APK Installers"),
    OLD_DOWNLOADS("Old Downloads"),
    CHAT_MEDIA("Chat Media"),
    APP_LEFTOVERS("App Leftovers"),
    DUPLICATE_CONTACTS("Duplicate Contacts")
}

data class CleanCandidate(
    val id: String,
    val category: CleanerCategory,
    val title: String,
    val subtitle: String,
    val sizeBytes: Long,
    val mediaItem: MediaItem? = null,
    val isSelected: Boolean = false,
    val tag: String? = null,
    val warning: String? = null
)

data class ScanProgress(
    val currentCategory: CleanerCategory = CleanerCategory.SIMILAR_PHOTOS,
    val itemsScanned: Int = 0,
    val totalFoundItems: Int = 0,
    val totalRecoverableBytes: Long = 0L,
    val isScanning: Boolean = false,
    val progressFraction: Float = 0f,
    val statusText: String = ""
)
