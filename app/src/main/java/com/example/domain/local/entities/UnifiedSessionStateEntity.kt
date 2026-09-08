package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Sesión Unificada (Cross-Device Handoff).
 * Permite reanudar el flujo de navegación, borradores de texto, salas de reunión y archivos abiertos
 * en cualquier terminal de la malla local.
 */
@Entity(tableName = "unified_session_states")
data class UnifiedSessionStateEntity(
    @PrimaryKey val sessionId: String,
    val originDeviceId: String,
    val originDeviceName: String,
    val activeRoute: String = "hub", // hub, team_chat, cloud_drive, video_rooms, location, camera
    val routeTitle: String = "OmniComm Hub",
    val draftMessage: String = "",
    val activeChatPeerId: String = "",
    val activeFileViewerName: String = "",
    val scrollPositionOffset: Int = 0,
    val activeVideoRoomId: String = "",
    val lastActivityTimestamp: Long = System.currentTimeMillis(),
    val isSyncedToMesh: Boolean = true
)
