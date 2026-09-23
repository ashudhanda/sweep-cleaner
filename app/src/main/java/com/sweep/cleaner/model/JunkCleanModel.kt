package com.sweep.cleaner.model

enum class JunkCleanStage {
    IDLE,
    SCANNING,
    READY,
    CLEANING,
    COMPLETED
}

data class JunkCategoryItem(
    val id: String,
    val name: String,
    val description: String,
    val bytes: Long,
    val itemCount: Int,
    val iconType: String,
    val isCleaned: Boolean = false
)

data class OneTapJunkState(
    val stage: JunkCleanStage = JunkCleanStage.IDLE,
    val scanProgress: Float = 0f,
    val currentScanAction: String = "",
    val categories: List<JunkCategoryItem> = emptyList(),
    val totalJunkBytes: Long = 0L,
    val reclaimedBytes: Long = 0L,
    val totalCleanedItems: Int = 0,
    val cleanDurationMs: Long = 0L
)
