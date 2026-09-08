package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Estado de Sistema y Disponibilidad de Nodos Remotos.
 * Persiste métricas en tiempo real en la base de datos cifrada (Room/SQLCipher).
 */
@Entity(tableName = "device_system_states")
data class DeviceSystemStateEntity(
    @PrimaryKey val deviceId: String,
    val nodeName: String,
    val activeApp: String = "OmniComm Hub",
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val connectivityType: String = "DUAL_RADIO_MESH",
    val cpuLoadPercent: Int = 18,
    val ramUsagePercent: Int = 42,
    val screenState: String = "ACTIVE_OK", // ACTIVE_OK, SCREEN_BROKEN, SCREEN_OFF
    val availabilityStatus: String = "ONLINE_HEALTHY", // ONLINE_HEALTHY, DEGRADED, OFFLINE
    val latencyMs: Int = 14,
    val lastStateSyncTimestamp: Long = System.currentTimeMillis()
)
