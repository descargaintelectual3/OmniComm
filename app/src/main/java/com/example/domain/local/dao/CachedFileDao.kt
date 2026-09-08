package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.domain.local.entities.CachedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedFileDao {
    @Query("SELECT * FROM cached_files ORDER BY downloadedAt DESC")
    fun getAllCachedFiles(): Flow<List<CachedFileEntity>>

    @Query("SELECT * FROM cached_files ORDER BY downloadedAt DESC")
    suspend fun getAllCachedFilesSync(): List<CachedFileEntity>

    @Query("SELECT * FROM cached_files WHERE fileId = :id LIMIT 1")
    fun getFileById(id: String): Flow<CachedFileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CachedFileEntity)

    @Update
    suspend fun updateFile(file: CachedFileEntity)

    @Delete
    suspend fun deleteFile(file: CachedFileEntity)

    @Query("UPDATE cached_files SET isSyncedWithCloud = :synced WHERE fileId = :id")
    suspend fun updateSyncStatus(id: String, synced: Boolean)

    @Query("DELETE FROM cached_files WHERE fileId = :id")
    suspend fun deleteFileById(id: String)
}
