package com.example.domain.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.UnifiedSessionStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Gestor de Sesión Unificada Multi-Dispositivo (Cross-Device Handoff).
 * Sincroniza y reanuda instantáneamente el estado de navegación, chats, borradores
 * y archivos abiertos entre nodos físicos de la malla a través de la cola persistente de Room.
 */
class UnifiedSessionStateManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database = OmniDatabase.getDatabase(context)
    private val sessionStateDao = database.sessionStateDao()

    private val localDeviceId = "OMNI-NODE-" + Build.MODEL.replace(" ", "").take(5).uppercase()

    private val _activeHandoffAvailable = MutableStateFlow<UnifiedSessionStateEntity?>(null)
    val activeHandoffAvailable: StateFlow<UnifiedSessionStateEntity?> = _activeHandoffAvailable.asStateFlow()

    init {
        observeRemoteHandoffs()
    }

    private fun observeRemoteHandoffs() {
        scope.launch {
            sessionStateDao.getLatestRemoteSession(localDeviceId).collect { session ->
                // Solo ofrecer handoff si la sesión es reciente (< 5 minutos)
                if (session != null && (System.currentTimeMillis() - session.lastActivityTimestamp) < 300000L) {
                    _activeHandoffAvailable.value = session
                } else {
                    _activeHandoffAvailable.value = null
                }
            }
        }
    }

    /**
     * Publica el estado de sesión local para que otros terminales de la malla puedan reanudarla.
     */
    fun saveLocalSessionState(
        activeRoute: String,
        routeTitle: String,
        draftMessage: String = "",
        activeChatPeerId: String = "",
        activeFileViewerName: String = "",
        activeVideoRoomId: String = ""
    ) {
        scope.launch {
            val session = UnifiedSessionStateEntity(
                sessionId = "SESSION-" + localDeviceId.takeLast(6),
                originDeviceId = localDeviceId,
                originDeviceName = Build.MODEL,
                activeRoute = activeRoute,
                routeTitle = routeTitle,
                draftMessage = draftMessage,
                activeChatPeerId = activeChatPeerId,
                activeFileViewerName = activeFileViewerName,
                activeVideoRoomId = activeVideoRoomId,
                lastActivityTimestamp = System.currentTimeMillis(),
                isSyncedToMesh = true
            )
            sessionStateDao.upsertSession(session)
            Log.d("UnifiedSessionState", "Sesión unificada guardada y transmitida: $activeRoute ($routeTitle)")
        }
    }

    /**
     * Descarta la sugerencia de Handoff actual.
     */
    fun dismissCurrentHandoff() {
        _activeHandoffAvailable.value = null
    }

    /**
     * Obtiene el flujo de todas las sesiones activas en la malla.
     */
    fun getAllMeshSessions(): Flow<List<UnifiedSessionStateEntity>> {
        return sessionStateDao.getAllSessions()
    }

    /**
     * Limpia sesiones viejas expiradas.
     */
    fun cleanupExpiredSessions() {
        scope.launch {
            val expireBefore = System.currentTimeMillis() - (1000 * 60 * 60) // 1 hora
            sessionStateDao.purgeExpiredSessions(expireBefore)
        }
    }
}
