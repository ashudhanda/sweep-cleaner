package com.sweep.cleaner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey
    val id: String,
    val originalUri: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val localFilePath: String,
    val deletedTimestamp: Long,
    val expiryTimestamp: Long,
    val category: String
)
