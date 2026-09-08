package com.example.domain.local.dao

import androidx.room.*
import com.example.domain.local.entities.DtnBundleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DtnBundleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundle(bundle: DtnBundleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundles(bundles: List<DtnBundleEntity>)

    @Query("""
        SELECT * FROM dtn_bundles 
        WHERE isDelivered = 0 AND expiresAt > :now 
        ORDER BY 
            CASE priority 
                WHEN 'CRITICAL_SOS' THEN 1 
                WHEN 'EMERGENCY_TELEMETRY' THEN 2 
                WHEN 'VOICE_PTT' THEN 3 
                WHEN 'HIGH_PRIORITY_TEXT' THEN 4 
                WHEN 'IMAGE_EVIDENCE' THEN 5 
                ELSE 6 
            END ASC, 
            timestamp ASC
    """)
    fun getActiveBundlesFlow(now: Long = System.currentTimeMillis()): Flow<List<DtnBundleEntity>>

    @Query("SELECT * FROM dtn_bundles WHERE isDelivered = 0 AND expiresAt > :now ORDER BY timestamp ASC")
    suspend fun getActiveBundlesSync(now: Long = System.currentTimeMillis()): List<DtnBundleEntity>

    @Query("SELECT bundleId FROM dtn_bundles WHERE expiresAt > :now")
    suspend fun getAllBundleIds(now: Long = System.currentTimeMillis()): List<String>

    @Query("SELECT * FROM dtn_bundles WHERE bundleId = :bundleId")
    suspend fun getBundleById(bundleId: String): DtnBundleEntity?

    @Query("UPDATE dtn_bundles SET isDelivered = 1, ackReceived = 1 WHERE bundleId = :bundleId")
    suspend fun markDelivered(bundleId: String)

    @Query("UPDATE dtn_bundles SET currentHops = currentHops + 1, visitedNodesCsv = :visitedCsv WHERE bundleId = :bundleId")
    suspend fun incrementHop(bundleId: String, visitedCsv: String)

    @Query("DELETE FROM dtn_bundles WHERE expiresAt <= :now OR isDelivered = 1")
    suspend fun purgeExpiredOrDelivered(now: Long = System.currentTimeMillis()): Int

    @Query("SELECT COUNT(*) FROM dtn_bundles WHERE isDelivered = 0 AND expiresAt > :now")
    fun getActiveBundleCountFlow(now: Long = System.currentTimeMillis()): Flow<Int>

    @Query("DELETE FROM dtn_bundles")
    suspend fun clearAllBundles()
}
