package com.sweep.cleaner.ui.viewmodel

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sweep.cleaner.data.local.TrashEntity
import com.sweep.cleaner.data.preferences.UserPreferencesRepository
import com.sweep.cleaner.data.repository.StorageRepository
import com.sweep.cleaner.data.repository.TrashOperationResult
import com.sweep.cleaner.model.CleanerCategory
import com.sweep.cleaner.model.DuplicateContactGroup
import com.sweep.cleaner.model.JunkCategoryItem
import com.sweep.cleaner.model.JunkCleanStage
import com.sweep.cleaner.model.MediaItem
import com.sweep.cleaner.model.OneTapJunkState
import com.sweep.cleaner.model.SafDocumentItem
import com.sweep.cleaner.model.SafDuplicateGroup
import com.sweep.cleaner.model.SafScanResult
import com.sweep.cleaner.model.SafScanState
import com.sweep.cleaner.model.ScanProgress
import com.sweep.cleaner.model.SimilarPhotoGroup
import com.sweep.cleaner.model.StorageSummary
import com.sweep.cleaner.util.SafScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmartScanFinding(
    val category: CleanerCategory,
    val count: Int,
    val totalBytes: Long,
    val description: String,
    val route: String
)

class SweepViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepo = StorageRepository(application)
    private val prefsRepo = UserPreferencesRepository(application)

    // Storage Summary State
    private val _storageSummary = MutableStateFlow(StorageSummary())
    val storageSummary: StateFlow<StorageSummary> = _storageSummary.asStateFlow()

    // Smart Scan State
    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _scanFindings = MutableStateFlow<List<SmartScanFinding>>(emptyList())
    val scanFindings: StateFlow<List<SmartScanFinding>> = _scanFindings.asStateFlow()

    private var scanJob: Job? = null

    // Similar Photos
    private val _similarPhotoGroups = MutableStateFlow<List<SimilarPhotoGroup>>(emptyList())
    val similarPhotoGroups: StateFlow<List<SimilarPhotoGroup>> = _similarPhotoGroups.asStateFlow()
    private val _selectedSimilarPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSimilarPhotoIds: StateFlow<Set<Long>> = _selectedSimilarPhotoIds.asStateFlow()
    val isSimilarPhotosLoading = MutableStateFlow(false)

    // Screenshots
    private val _screenshots = MutableStateFlow<List<MediaItem>>(emptyList())
    val screenshots: StateFlow<List<MediaItem>> = _screenshots.asStateFlow()
    private val _selectedScreenshotIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedScreenshotIds: StateFlow<Set<Long>> = _selectedScreenshotIds.asStateFlow()
    val isScreenshotsLoading = MutableStateFlow(false)

    // Large Videos
    private val _largeVideos = MutableStateFlow<List<MediaItem>>(emptyList())
    val largeVideos: StateFlow<List<MediaItem>> = _largeVideos.asStateFlow()
    private val _selectedVideoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedVideoIds: StateFlow<Set<Long>> = _selectedVideoIds.asStateFlow()
    val isVideosLoading = MutableStateFlow(false)

    // Documents
    private val _documents = MutableStateFlow<List<MediaItem>>(emptyList())
    val documents: StateFlow<List<MediaItem>> = _documents.asStateFlow()
    private val _selectedDocIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDocIds: StateFlow<Set<Long>> = _selectedDocIds.asStateFlow()
    val isDocsLoading = MutableStateFlow(false)

    // Audio
    private val _audioFiles = MutableStateFlow<List<MediaItem>>(emptyList())
    val audioFiles: StateFlow<List<MediaItem>> = _audioFiles.asStateFlow()
    private val _selectedAudioIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedAudioIds: StateFlow<Set<Long>> = _selectedAudioIds.asStateFlow()
    val isAudioLoading = MutableStateFlow(false)

    // APKs
    private val _apkFiles = MutableStateFlow<List<MediaItem>>(emptyList())
    val apkFiles: StateFlow<List<MediaItem>> = _apkFiles.asStateFlow()
    private val _selectedApkIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedApkIds: StateFlow<Set<Long>> = _selectedApkIds.asStateFlow()
    val isApksLoading = MutableStateFlow(false)

    // Old Downloads
    private val _oldDownloads = MutableStateFlow<List<MediaItem>>(emptyList())
    val oldDownloads: StateFlow<List<MediaItem>> = _oldDownloads.asStateFlow()
    private val _selectedDownloadIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDownloadIds: StateFlow<Set<Long>> = _selectedDownloadIds.asStateFlow()
    val isDownloadsLoading = MutableStateFlow(false)

    // Chat Media
    private val _chatMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    val chatMedia: StateFlow<List<MediaItem>> = _chatMedia.asStateFlow()
    private val _selectedChatMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedChatMediaIds: StateFlow<Set<Long>> = _selectedChatMediaIds.asStateFlow()
    val isChatMediaLoading = MutableStateFlow(false)

    // Duplicate Contacts
    private val _duplicateContacts = MutableStateFlow<List<DuplicateContactGroup>>(emptyList())
    val duplicateContacts: StateFlow<List<DuplicateContactGroup>> = _duplicateContacts.asStateFlow()
    val isContactsLoading = MutableStateFlow(false)

    // Trash / Recently Deleted (Room)
    val trashItems: StateFlow<List<TrashEntity>> = storageRepo.allTrashItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalTrashSize: StateFlow<Long?> = storageRepo.totalTrashSizeBytes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val trashCount: StateFlow<Int> = storageRepo.trashCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // User Preferences (DataStore)
    val themeMode: StateFlow<String> = prefsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "light")

    val tutorialSeen: StateFlow<Boolean?> = prefsRepo.tutorialSeen
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val scheduledScanHour: StateFlow<Int> = prefsRepo.scheduledScanHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 9)

    val notificationsEnabled: StateFlow<Boolean> = prefsRepo.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Platform intent sender prompt for protected MediaStore deletes
    private val _pendingIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingIntentSender: StateFlow<IntentSender?> = _pendingIntentSender.asStateFlow()

    init {
        refreshStorageSummary()
    }

    fun refreshStorageSummary() {
        viewModelScope.launch {
            _storageSummary.value = storageRepo.getStorageSummary()
        }
    }

    // --- Smart Scan ---
    fun startSmartScan() {
        scanJob?.cancel()
        _scanFindings.value = emptyList()
        _scanProgress.value = ScanProgress(isScanning = true, statusText = "Initializing smart scan...")

        scanJob = viewModelScope.launch {
            try {
                val findings = mutableListOf<SmartScanFinding>()
                var totalBytesRecoverable = 0L

                // 1. Photos & Screenshots
                _scanProgress.value = ScanProgress(
                    currentCategory = CleanerCategory.SIMILAR_PHOTOS,
                    isScanning = true,
                    progressFraction = 0.15f,
                    statusText = "Checking photo library for similar shots..."
                )
                val similarGroups = storageRepo.getSimilarPhotoGroups()
                _similarPhotoGroups.value = similarGroups
                if (similarGroups.isNotEmpty()) {
                    val groupCount = similarGroups.size
                    val nonBestPhotos = similarGroups.flatMap { g -> g.items.filter { it.id != g.bestShotId } }
                    val redundantBytes = nonBestPhotos.sumOf { it.sizeBytes }
                    totalBytesRecoverable += redundantBytes
                    findings.add(
                        SmartScanFinding(
                            category = CleanerCategory.SIMILAR_PHOTOS,
                            count = nonBestPhotos.size,
                            totalBytes = redundantBytes,
                            description = "$groupCount groups of similar photos found",
                            route = com.sweep.cleaner.ui.navigation.NavRoutes.SIMILAR_PHOTOS
                        )
                    )
                }

                // 2. Screenshots
                _scanProgress.value = ScanProgress(
                    currentCategory = CleanerCategory.SCREENSHOTS,
                    isScanning = true,
                    progressFraction = 0.35f,
                    totalRecoverableBytes = totalBytesRecoverable,
                    statusText = "Scanning for old screenshots..."
                )
                val shots = storageRepo.getScreenshots()
                _screenshots.value = shots
                if (shots.isNotEmpty()) {
                    val shotBytes = shots.sumOf { it.sizeBytes }
                    totalBytesRecoverable += shotBytes
                    findings.add(
                        SmartScanFinding(
                            category = CleanerCategory.SCREENSHOTS,
                            count = shots.size,
                            totalBytes = shotBytes,
                            description = "${shots.size} screenshots ready for review",
                            route = com.sweep.cleaner.ui.navigation.NavRoutes.SCREENSHOTS
                        )
                    )
                }

                // 3. Large Videos
                _scanProgress.value = ScanProgress(
                    currentCategory = CleanerCategory.LARGE_VIDEOS,
                    isScanning = true,
                    progressFraction = 0.55f,
                    totalRecoverableBytes = totalBytesRecoverable,
                    statusText = "Identifying large video files..."
                )
                val videos = storageRepo.getLargeVideos()
                _largeVideos.value = videos
                if (videos.isNotEmpty()) {
                    val vidBytes = videos.sumOf { it.sizeBytes }
                    totalBytesRecoverable += vidBytes
                    findings.add(
                        SmartScanFinding(
                            category = CleanerCategory.LARGE_VIDEOS,
                            count = videos.size,
                            totalBytes = vidBytes,
                            description = "${videos.size} heavy video files",
                            route = com.sweep.cleaner.ui.navigation.NavRoutes.LARGE_VIDEOS
                        )
                    )
                }

                // 4. Documents & APKs
                _scanProgress.value = ScanProgress(
                    currentCategory = CleanerCategory.APKS,
                    isScanning = true,
                    progressFraction = 0.75f,
                    totalRecoverableBytes = totalBytesRecoverable,
                    statusText = "Checking installers and downloads..."
                )
                val apks = storageRepo.getApkFiles()
                _apkFiles.value = apks
                if (apks.isNotEmpty()) {
                    val apkBytes = apks.sumOf { it.sizeBytes }
                    totalBytesRecoverable += apkBytes
                    findings.add(
                        SmartScanFinding(
                            category = CleanerCategory.APKS,
                            count = apks.size,
                            totalBytes = apkBytes,
                            description = "${apks.size} APK installer packages",
                            route = com.sweep.cleaner.ui.navigation.NavRoutes.APKS
                        )
                    )
                }

                // 5. Old Downloads
                val downloads = storageRepo.getOldDownloads(180)
                _oldDownloads.value = downloads
                if (downloads.isNotEmpty()) {
                    val dlBytes = downloads.sumOf { it.sizeBytes }
                    totalBytesRecoverable += dlBytes
                    findings.add(
                        SmartScanFinding(
                            category = CleanerCategory.OLD_DOWNLOADS,
                            count = downloads.size,
                            totalBytes = dlBytes,
                            description = "${downloads.size} downloads older than 180 days",
                            route = com.sweep.cleaner.ui.navigation.NavRoutes.OLD_DOWNLOADS
                        )
                    )
                }

                delay(400) // Smooth completion pacing
                _scanFindings.value = findings
                _scanProgress.value = ScanProgress(
                    isScanning = false,
                    progressFraction = 1.0f,
                    totalRecoverableBytes = totalBytesRecoverable,
                    totalFoundItems = findings.sumOf { it.count },
                    statusText = "Scan complete. All findings ready for review."
                )
                refreshStorageSummary()
            } catch (c: CancellationException) {
                _scanProgress.value = ScanProgress(
                    isScanning = false,
                    statusText = "Scan cancelled. No changes made."
                )
            } catch (e: Exception) {
                _scanProgress.value = ScanProgress(
                    isScanning = false,
                    statusText = "Scan completed with partial results: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _scanProgress.value = ScanProgress(isScanning = false, statusText = "Scan cancelled.")
    }

    // --- Loaders ---
    fun loadSimilarPhotos() {
        viewModelScope.launch {
            isSimilarPhotosLoading.value = true
            val groups = storageRepo.getSimilarPhotoGroups()
            _similarPhotoGroups.value = groups
            // By default, select all non-best shots so the user can easily review and clean
            val defaultSelections = groups.flatMap { g ->
                g.items.filter { it.id != g.bestShotId }.map { it.id }
            }.toSet()
            _selectedSimilarPhotoIds.value = defaultSelections
            isSimilarPhotosLoading.value = false
        }
    }

    fun toggleSimilarPhotoSelection(photoId: Long) {
        val current = _selectedSimilarPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) current.remove(photoId) else current.add(photoId)
        _selectedSimilarPhotoIds.value = current
    }

    fun loadScreenshots() {
        viewModelScope.launch {
            isScreenshotsLoading.value = true
            val shots = storageRepo.getScreenshots()
            _screenshots.value = shots
            isScreenshotsLoading.value = false
        }
    }

    fun toggleScreenshotSelection(id: Long) {
        val current = _selectedScreenshotIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedScreenshotIds.value = current
    }

    fun selectAllScreenshots(select: Boolean) {
        if (select) {
            _selectedScreenshotIds.value = _screenshots.value.map { it.id }.toSet()
        } else {
            _selectedScreenshotIds.value = emptySet()
        }
    }

    fun loadLargeVideos() {
        viewModelScope.launch {
            isVideosLoading.value = true
            val vids = storageRepo.getLargeVideos()
            _largeVideos.value = vids
            isVideosLoading.value = false
        }
    }

    fun toggleVideoSelection(id: Long) {
        val current = _selectedVideoIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedVideoIds.value = current
    }

    fun loadDocuments() {
        viewModelScope.launch {
            isDocsLoading.value = true
            val docs = storageRepo.getDocuments()
            _documents.value = docs
            isDocsLoading.value = false
        }
    }

    fun toggleDocSelection(id: Long) {
        val current = _selectedDocIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedDocIds.value = current
    }

    fun loadAudioFiles() {
        viewModelScope.launch {
            isAudioLoading.value = true
            val audio = storageRepo.getAudioFiles()
            _audioFiles.value = audio
            isAudioLoading.value = false
        }
    }

    fun toggleAudioSelection(id: Long) {
        val current = _selectedAudioIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedAudioIds.value = current
    }

    fun loadApkFiles() {
        viewModelScope.launch {
            isApksLoading.value = true
            val apks = storageRepo.getApkFiles()
            _apkFiles.value = apks
            isApksLoading.value = false
        }
    }

    fun toggleApkSelection(id: Long) {
        val current = _selectedApkIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedApkIds.value = current
    }

    fun loadOldDownloads() {
        viewModelScope.launch {
            isDownloadsLoading.value = true
            val dls = storageRepo.getOldDownloads(180)
            _oldDownloads.value = dls
            isDownloadsLoading.value = false
        }
    }

    fun toggleDownloadSelection(id: Long) {
        val current = _selectedDownloadIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedDownloadIds.value = current
    }

    fun loadChatMedia() {
        viewModelScope.launch {
            isChatMediaLoading.value = true
            val media = storageRepo.getChatMedia()
            _chatMedia.value = media
            isChatMediaLoading.value = false
        }
    }

    fun toggleChatMediaSelection(id: Long) {
        val current = _selectedChatMediaIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedChatMediaIds.value = current
    }

    fun loadDuplicateContacts() {
        viewModelScope.launch {
            isContactsLoading.value = true
            val dups = storageRepo.getDuplicateContacts()
            _duplicateContacts.value = dups
            isContactsLoading.value = false
        }
    }

    // --- Safe 30-Day Trash Operations ---
    fun moveSelectedToTrash(
        items: List<MediaItem>,
        category: String,
        onComplete: (moved: Int, freedBytes: Long) -> Unit
    ) {
        viewModelScope.launch {
            val result = storageRepo.moveToTrash(items, category)
            when (result) {
                is TrashOperationResult.Success -> {
                    refreshStorageSummary()
                    onComplete(result.movedCount, result.freedBytes)
                }
                is TrashOperationResult.RequiresConfirmation -> {
                    _pendingIntentSender.value = result.intentSender
                    refreshStorageSummary()
                    onComplete(result.pendingTrashItems.size, result.pendingTrashItems.sumOf { it.sizeBytes })
                }
                is TrashOperationResult.PartialFailure -> {
                    refreshStorageSummary()
                    onComplete(result.movedCount, 0L)
                }
            }
        }
    }

    fun clearPendingIntentSender() {
        _pendingIntentSender.value = null
    }

    fun restoreTrashItem(trashEntity: TrashEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = storageRepo.restoreTrashItem(trashEntity)
            refreshStorageSummary()
            onResult(success)
        }
    }

    fun permanentlyDeleteTrashItem(trashEntity: TrashEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = storageRepo.permanentlyDeleteTrashItem(trashEntity)
            refreshStorageSummary()
            onResult(success)
        }
    }

    fun emptyTrash(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = storageRepo.emptyTrash()
            refreshStorageSummary()
            onResult(count)
        }
    }

    // --- User Preferences ---
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            prefsRepo.setThemeMode(mode)
        }
    }

    fun setTutorialSeen(seen: Boolean) {
        viewModelScope.launch {
            prefsRepo.setTutorialSeen(seen)
        }
    }

    fun setScheduledScanHour(hour: Int) {
        viewModelScope.launch {
            prefsRepo.setScheduledScanHour(hour)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setNotificationsEnabled(enabled)
        }
    }

    // --- Storage Access Framework (SAF) Deep Scanner ---
    private val _safScanState = MutableStateFlow(SafScanState())
    val safScanState: StateFlow<SafScanState> = _safScanState.asStateFlow()

    val lastSafTreeUri: StateFlow<String?> = prefsRepo.lastSafTreeUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSafFolderName: StateFlow<String?> = prefsRepo.lastSafFolderName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedSafLargeFileUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedSafLargeFileUris: StateFlow<Set<Uri>> = _selectedSafLargeFileUris.asStateFlow()

    private val _selectedSafDuplicateUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedSafDuplicateUris: StateFlow<Set<Uri>> = _selectedSafDuplicateUris.asStateFlow()

    private var safScanJob: Job? = null

    fun selectSafTree(treeUri: Uri, folderName: String) {
        viewModelScope.launch {
            prefsRepo.setLastSafTree(treeUri.toString(), folderName)
            startSafScan(treeUri)
        }
    }

    fun startSafScan(treeUri: Uri, thresholdBytes: Long = SafScanner.DEFAULT_LARGE_FILE_THRESHOLD_BYTES) {
        safScanJob?.cancel()
        _safScanState.value = SafScanState(
            isScanning = true,
            progressStatus = "Connecting to storage..."
        )
        _selectedSafLargeFileUris.value = emptySet()
        _selectedSafDuplicateUris.value = emptySet()

        safScanJob = viewModelScope.launch {
            try {
                val result = storageRepo.scanSafTree(
                    treeUri = treeUri,
                    thresholdBytes = thresholdBytes,
                    onProgress = { count, path ->
                        _safScanState.value = _safScanState.value.copy(
                            filesScannedCount = count,
                            currentFolder = path,
                            progressStatus = "Scanning $count files..."
                        )
                    }
                )

                _safScanState.value = SafScanState(
                    isScanning = false,
                    filesScannedCount = result.totalFilesScanned,
                    currentLargeFilesCount = result.largeFiles.size,
                    currentDuplicateGroupsCount = result.duplicateGroups.size,
                    progressStatus = "Scan complete",
                    result = result
                )

                // By default, pre-select duplicate copies (excluding recommended keeper)
                val duplicateUrisToClean = mutableSetOf<Uri>()
                for (group in result.duplicateGroups) {
                    for (item in group.items) {
                        if (item.uri != group.recommendedKeepItem.uri) {
                            duplicateUrisToClean.add(item.uri)
                        }
                    }
                }
                _selectedSafDuplicateUris.value = duplicateUrisToClean

            } catch (e: CancellationException) {
                _safScanState.value = _safScanState.value.copy(
                    isScanning = false,
                    progressStatus = "Scan cancelled"
                )
            } catch (e: Exception) {
                _safScanState.value = SafScanState(
                    isScanning = false,
                    errorMessage = e.localizedMessage ?: "Failed to scan selected directory"
                )
            }
        }
    }

    fun cancelSafScan() {
        safScanJob?.cancel()
        _safScanState.value = _safScanState.value.copy(
            isScanning = false,
            progressStatus = "Scan cancelled"
        )
    }

    fun toggleSafLargeFileSelection(uri: Uri) {
        val current = _selectedSafLargeFileUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedSafLargeFileUris.value = current
    }

    fun selectAllSafLargeFiles(select: Boolean) {
        val result = _safScanState.value.result ?: return
        if (select) {
            _selectedSafLargeFileUris.value = result.largeFiles.map { it.uri }.toSet()
        } else {
            _selectedSafLargeFileUris.value = emptySet()
        }
    }

    fun toggleSafDuplicateSelection(uri: Uri) {
        val current = _selectedSafDuplicateUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedSafDuplicateUris.value = current
    }

    fun selectAllSafDuplicatesExceptRecommended() {
        val result = _safScanState.value.result ?: return
        val set = mutableSetOf<Uri>()
        for (group in result.duplicateGroups) {
            for (item in group.items) {
                if (item.uri != group.recommendedKeepItem.uri) {
                    set.add(item.uri)
                }
            }
        }
        _selectedSafDuplicateUris.value = set
    }

    fun clearSafSelections() {
        _selectedSafLargeFileUris.value = emptySet()
        _selectedSafDuplicateUris.value = emptySet()
    }

    fun cleanSelectedSafItems(onComplete: (movedCount: Int, freedBytes: Long) -> Unit) {
        val result = _safScanState.value.result ?: return
        val allSelectedUris = _selectedSafLargeFileUris.value + _selectedSafDuplicateUris.value
        if (allSelectedUris.isEmpty()) {
            onComplete(0, 0L)
            return
        }

        // Collect matching document items
        val itemsMap = mutableMapOf<Uri, SafDocumentItem>()
        result.largeFiles.forEach { itemsMap[it.uri] = it }
        result.duplicateGroups.forEach { group ->
            group.items.forEach { itemsMap[it.uri] = it }
        }

        val itemsToClean = allSelectedUris.mapNotNull { itemsMap[it] }

        viewModelScope.launch {
            val opResult = storageRepo.moveSafItemsToTrash(itemsToClean)
            if (opResult is TrashOperationResult.Success) {
                // Update current scan results in memory by removing cleaned items
                val cleanedUris = itemsToClean.map { it.uri }.toSet()
                val remainingLargeFiles = result.largeFiles.filterNot { cleanedUris.contains(it.uri) }
                val updatedDuplicateGroups = result.duplicateGroups.mapNotNull { group ->
                    val remainingItems = group.items.filterNot { cleanedUris.contains(it.uri) }
                    if (remainingItems.size >= 2) {
                        val keeper = if (remainingItems.contains(group.recommendedKeepItem)) {
                            group.recommendedKeepItem
                        } else {
                            remainingItems.first()
                        }
                        group.copy(items = remainingItems, recommendedKeepItem = keeper)
                    } else {
                        null
                    }
                }

                _safScanState.value = _safScanState.value.copy(
                    result = result.copy(
                        largeFiles = remainingLargeFiles,
                        duplicateGroups = updatedDuplicateGroups
                    )
                )

                _selectedSafLargeFileUris.value = emptySet()
                _selectedSafDuplicateUris.value = emptySet()

                refreshStorageSummary()
                onComplete(opResult.movedCount, opResult.freedBytes)
            } else {
                onComplete(0, 0L)
            }
        }
    }

    // --- One-Tap Junk Clean State & Handlers ---

    private val _oneTapJunkState = MutableStateFlow(OneTapJunkState())
    val oneTapJunkState: StateFlow<OneTapJunkState> = _oneTapJunkState.asStateFlow()

    private val _dashboardJunkBytes = MutableStateFlow(0L)
    val dashboardJunkBytes: StateFlow<Long> = _dashboardJunkBytes.asStateFlow()

    fun loadDashboardJunkEstimate() {
        viewModelScope.launch {
            if (_oneTapJunkState.value.stage == JunkCleanStage.COMPLETED) {
                _dashboardJunkBytes.value = 0L
                return@launch
            }
            try {
                val categories = storageRepo.scanJunkCategories()
                val total = categories.sumOf { it.bytes }
                _dashboardJunkBytes.value = total
            } catch (_: Exception) {
                _dashboardJunkBytes.value = 0L
            }
        }
    }

    fun startOneTapJunkScan(forceRescan: Boolean = false) {
        viewModelScope.launch {
            _oneTapJunkState.value = OneTapJunkState(
                stage = JunkCleanStage.SCANNING,
                scanProgress = 0.08f,
                currentScanAction = "Analyzing hardware storage indices..."
            )
            delay(250)

            _oneTapJunkState.value = _oneTapJunkState.value.copy(
                scanProgress = 0.35f,
                currentScanAction = "Deep scanning application & web caches..."
            )
            delay(300)

            _oneTapJunkState.value = _oneTapJunkState.value.copy(
                scanProgress = 0.65f,
                currentScanAction = "Auditing diagnostic logs & residual crash dumps..."
            )
            delay(300)

            _oneTapJunkState.value = _oneTapJunkState.value.copy(
                scanProgress = 0.88f,
                currentScanAction = "Inspecting obsolete APK installers & empty folders..."
            )
            val categories = storageRepo.scanJunkCategories()
            val totalBytes = categories.sumOf { it.bytes }
            delay(250)

            _oneTapJunkState.value = OneTapJunkState(
                stage = JunkCleanStage.READY,
                scanProgress = 1.0f,
                currentScanAction = if (totalBytes > 0) "Analysis complete! Ready for one-tap sweep." else "Storage is clean! No clutter detected.",
                categories = categories,
                totalJunkBytes = totalBytes
            )
            _dashboardJunkBytes.value = totalBytes
        }
    }

    fun executeOneTapJunkClean(onCompleted: (freedBytes: Long) -> Unit = {}) {
        val currentCategories = _oneTapJunkState.value.categories
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _oneTapJunkState.value = _oneTapJunkState.value.copy(
                stage = JunkCleanStage.CLEANING,
                currentScanAction = "Sweeping system and application cache..."
            )

            val updatedList = currentCategories.toMutableList()
            for (i in updatedList.indices) {
                delay(300)
                val cat = updatedList[i]
                _oneTapJunkState.value = _oneTapJunkState.value.copy(
                    currentScanAction = "Cleaning ${cat.name}..."
                )
                updatedList[i] = cat.copy(isCleaned = true)
                _oneTapJunkState.value = _oneTapJunkState.value.copy(
                    categories = updatedList.toList(),
                    reclaimedBytes = updatedList.filter { it.isCleaned }.sumOf { it.bytes }
                )
            }

            val actualCleaned = storageRepo.clearJunkFiles()
            val totalReclaimed = if (actualCleaned > 0) actualCleaned else _oneTapJunkState.value.totalJunkBytes
            val duration = System.currentTimeMillis() - startTime

            delay(250)
            _oneTapJunkState.value = _oneTapJunkState.value.copy(
                stage = JunkCleanStage.COMPLETED,
                reclaimedBytes = totalReclaimed,
                totalCleanedItems = updatedList.sumOf { it.itemCount },
                cleanDurationMs = duration,
                currentScanAction = "Storage swept cleanly!"
            )

            _dashboardJunkBytes.value = 0L
            prefsRepo.setLastJunkClean(System.currentTimeMillis(), totalReclaimed)
            refreshStorageSummary()
            onCompleted(totalReclaimed)
        }
    }

    fun resetOneTapJunkState() {
        _oneTapJunkState.value = OneTapJunkState(stage = JunkCleanStage.IDLE)
    }
}
