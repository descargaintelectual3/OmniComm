package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_files")
data class CachedFileEntity(
    @PrimaryKey val fileId: String,
    val fileName: String,
    val localAbsolutePath: String,
    val sizeBytes: Long,
    val downloadedAt: Long,
    val isSyncedWithCloud: Boolean = true
)
