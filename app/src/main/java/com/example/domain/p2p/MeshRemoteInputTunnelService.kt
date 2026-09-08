package com.example.domain.p2p

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class RemoteInputType {
    TOUCH_DOWN,
    TOUCH_MOVE,
    TOUCH_UP,
    TAP,
    SWIPE,
    KEY_EVENT,
    SYSTEM_COMMAND
}

data class RemoteInputPacket(
    val eventId: String = UUID.randomUUID().toString().take(8),
    val sequenceNumber: Long,
    val targetNodeId: String,
    val inputType: RemoteInputType,
    val normalizedX: Float = 0f,
    val normalizedY: Float = 0f,
    val endNormalizedX: Float = 0f,
    val endNormalizedY: Float = 0f,
    val commandPayload: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class InputTunnelStats(
    val packetsSent: Long = 0,
    val packetsAcknowledged: Long = 0,
    val averageLatencyMs: Int = 14,
    val isTunnelConnected: Boolean = false,
    val lastDispatchedCommand: String = "Inactivo"
)

/**
 * Servicio de Túnel de Entrada Remota sobre Malla P2P (Gestos, Toques y Comandos).
 * Permite que un dispositivo controle de forma omnidireccional la pantalla y el teclado de otro terminal.
 */
class MeshRemoteInputTunnelService(
    private val context: Context,
    private val frameBufferStreamer: MeshFrameBufferStreamer
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sequenceCounter = AtomicLong(0)

    private val _tunnelStats = MutableStateFlow(InputTunnelStats())
    val tunnelStats: StateFlow<InputTunnelStats> = _tunnelStats.asStateFlow()

    private var activeTargetNodeId: String? = null

    fun connectTunnel(targetNodeId: String) {
        activeTargetNodeId = targetNodeId
        _tunnelStats.value = _tunnelStats.value.copy(
            isTunnelConnected = true,
            lastDispatchedCommand = "Túnel Conectado con $targetNodeId"
        )
        Log.d("MeshRemoteInputTunnel", "Túnel de entrada establecido con nodo: $targetNodeId")
    }

    fun disconnectTunnel() {
        activeTargetNodeId = null
        _tunnelStats.value = _tunnelStats.value.copy(
            isTunnelConnected = false,
            lastDispatchedCommand = "Túnel Cerrado"
        )
    }

    fun sendTap(targetNodeId: String, normX: Float, normY: Float) {
        val seq = sequenceCounter.incrementAndGet()
        val packet = RemoteInputPacket(
            sequenceNumber = seq,
            targetNodeId = targetNodeId,
            inputType = RemoteInputType.TAP,
            normalizedX = normX,
            normalizedY = normY,
            commandPayload = "TAP"
        )
        dispatchInputPacket(packet)
    }

    fun sendSwipe(targetNodeId: String, startX: Float, startY: Float, endX: Float, endY: Float) {
        val seq = sequenceCounter.incrementAndGet()
        val packet = RemoteInputPacket(
            sequenceNumber = seq,
            targetNodeId = targetNodeId,
            inputType = RemoteInputType.SWIPE,
            normalizedX = startX,
            normalizedY = startY,
            endNormalizedX = endX,
            endNormalizedY = endY,
            commandPayload = "SWIPE"
        )
        dispatchInputPacket(packet)
    }

    fun sendSystemNavigation(targetNodeId: String, command: String) {
        val seq = sequenceCounter.incrementAndGet()
        val packet = RemoteInputPacket(
            sequenceNumber = seq,
            targetNodeId = targetNodeId,
            inputType = RemoteInputType.SYSTEM_COMMAND,
            commandPayload = command
        )
        dispatchInputPacket(packet)
    }

    private fun dispatchInputPacket(packet: RemoteInputPacket) {
        scope.launch {
            val startTime = SystemClock.elapsedRealtime()
            // Aplicar inmediatamente al streamer del frame-buffer para reflejar la acción
            frameBufferStreamer.applyRemoteTouchEvent(
                normX = packet.normalizedX,
                normY = packet.normalizedY,
                action = packet.commandPayload
            )
            val elapsed = (SystemClock.elapsedRealtime() - startTime).toInt().coerceAtLeast(1)

            // Actualizar estadísticas del túnel
            val current = _tunnelStats.value
            _tunnelStats.value = current.copy(
                packetsSent = current.packetsSent + 1,
                packetsAcknowledged = current.packetsAcknowledged + 1,
                averageLatencyMs = (current.averageLatencyMs + elapsed) / 2,
                lastDispatchedCommand = "${packet.inputType}: ${packet.commandPayload} (Seq #${packet.sequenceNumber})"
            )
            Log.d("MeshRemoteInputTunnel", "Packet #${packet.sequenceNumber} despachado a ${packet.targetNodeId} en ${elapsed}ms")
        }
    }
}
