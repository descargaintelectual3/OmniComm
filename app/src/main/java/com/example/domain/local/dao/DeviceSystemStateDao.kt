package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.local.entities.DeviceSystemStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceSystemStateDao {
    @Query("SELECT * FROM device_system_states ORDER BY lastStateSyncTimestamp DESC")
    fun getAllDeviceStates(): Flow<List<DeviceSystemStateEntity>>

    @Query("SELECT * FROM device_system_states WHERE deviceId = :deviceId LIMIT 1")
    fun getDeviceStateById(deviceId: String): Flow<DeviceSystemStateEntity?>

    @Query("SELECT * FROM device_system_states WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceStateDirect(deviceId: String): DeviceSystemStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: DeviceSystemStateEntity)

    @Query("UPDATE device_system_states SET batteryPercent = :battery, activeApp = :activeApp, availabilityStatus = :status, lastStateSyncTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateQuickTelemetry(deviceId: String, battery: Int, activeApp: String, status: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE device_system_states SET screenState = :screenState WHERE deviceId = :deviceId")
    suspend fun setScreenState(deviceId: String, screenState: String)

    @Query("DELETE FROM device_system_states WHERE deviceId = :deviceId")
    suspend fun deleteState(deviceId: String)
}
