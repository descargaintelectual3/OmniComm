package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domain.local.entities.ThreadCustomizationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadCustomizationDao {
    @Query("SELECT * FROM thread_customizations WHERE sessionId = :sessionId LIMIT 1")
    fun getCustomizationFlow(sessionId: String): Flow<ThreadCustomizationEntity?>

    @Query("SELECT * FROM thread_customizations WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getCustomizationDirect(sessionId: String): ThreadCustomizationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomization(entity: ThreadCustomizationEntity)

    @Query("DELETE FROM thread_customizations WHERE sessionId = :sessionId")
    suspend fun deleteCustomization(sessionId: String)
}
