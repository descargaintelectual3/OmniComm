package com.example.domain.c2

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.p2p.PacketPriority
import com.example.domain.p2p.StoreAndForwardRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Tipos de eventos de entrada y control gestual virtual.
 */
enum class VirtualInputType {
    TOUCH_DOWN,
    TOUCH_MOVE,
    TOUCH_UP,
    TAP,
    LONG_PRESS,
    SCROLL,
    NAV_BACK,
    NAV_HOME,
    NAV_RECENTS,
    NAV_POWER_LOCK,
    KEY_TEXT,
    KEY_ENTER,
    KEY_BACKSPACE
}

data class VirtualInputEvent(
    val eventId: String = UUID.randomUUID().toString().take(8),
    val type: VirtualInputType,
    val normalizedX: Float = 0.5f,  // Coordenada relativa 0.0 - 1.0 (ancho de pantalla)
    val normalizedY: Float = 0.5f,  // Coordenada relativa 0.0 - 1.0 (alto de pantalla)
    val scrollDeltaY: Float = 0.0f,
    val textPayload: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class VirtualInputTunnelState(
    val isTunnelActive: Boolean = false,
    val targetNodeId: String = "NODE_BRAVO_02",
    val virtualCursorX: Float = 0.5f,
    val virtualCursorY: Float = 0.5f,
    val eventsSentCount: Int = 0,
    val eventsReceivedCount: Int = 0,
    val lastEventDispatched: VirtualInputEvent? = null,
    val lastReceivedEvent: VirtualInputEvent? = null,
    val latencyMs: Long = 12L
)

/**
 * Túnel de Entrada Remota Virtual (Teclado, Ratón y Control Táctil por Red).
 * Permite controlar la interfaz gráfica del dispositivo remoto en tiempo real transmitiendo
 * coordenadas normalizadas de toque, gestos de trackpad, botones de navegación del sistema y texto.
 */
class VirtualRemoteInputTunnel private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(VirtualInputTunnelState())
    val state: StateFlow<VirtualInputTunnelState> = _state.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: VirtualRemoteInputTunnel? = null

        fun getInstance(context: Context): VirtualRemoteInputTunnel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VirtualRemoteInputTunnel(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun setTargetNode(nodeId: String) {
        _state.value = _state.value.copy(targetNodeId = nodeId)
    }

    fun setTunnelActive(active: Boolean) {
        _state.value = _state.value.copy(isTunnelActive = active)
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "INPUT_TUNNEL",
            message = if (active) "Túnel de entrada remota activado hacia ${_state.value.targetNodeId}" else "Túnel de entrada remota cerrado"
        )
    }

    /**
     * Despacha un evento de toque o desplazamiento de cursor hacia el dispositivo remoto.
     */
    fun sendPointerMove(relDeltaX: Float, relDeltaY: Float) {
        val currentX = _state.value.virtualCursorX
        val currentY = _state.value.virtualCursorY
        val newX = (currentX + relDeltaX).coerceIn(0.02f, 0.98f)
        val newY = (currentY + relDeltaY).coerceIn(0.02f, 0.98f)

        val event = VirtualInputEvent(
            type = VirtualInputType.TOUCH_MOVE,
            normalizedX = newX,
            normalizedY = newY
        )

        _state.value = _state.value.copy(
            virtualCursorX = newX,
            virtualCursorY = newY,
            lastEventDispatched = event,
            eventsSentCount = _state.value.eventsSentCount + 1
        )

        dispatchInputPacket(event)
    }

    /**
     * Envía un clic/tap táctil en la posición actual del cursor virtual.
     */
    fun sendTapAtCurrentPosition() {
        val event = VirtualInputEvent(
            type = VirtualInputType.TAP,
            normalizedX = _state.value.virtualCursorX,
            normalizedY = _state.value.virtualCursorY
        )
        _state.value = _state.value.copy(
            lastEventDispatched = event,
            eventsSentCount = _state.value.eventsSentCount + 1
        )
        dispatchInputPacket(event)
    }

    /**
     * Envía una pulsación prolongada (Long Press).
     */
    fun sendLongPressAtCurrentPosition() {
        val event = VirtualInputEvent(
            type = VirtualInputType.LONG_PRESS,
            normalizedX = _state.value.virtualCursorX,
            normalizedY = _state.value.virtualCursorY
        )
        _state.value = _state.value.copy(
            lastEventDispatched = event,
            eventsSentCount = _state.value.eventsSentCount + 1
        )
        dispatchInputPacket(event)
    }

    /**
     * Envía un evento de desplazamiento vertical (Scroll Up/Down).
     */
    fun sendScroll(deltaY: Float) {
        val event = VirtualInputEvent(
            type = VirtualInputType.SCROLL,
            normalizedX = _state.value.virtualCursorX,
            normalizedY = _state.value.virtualCursorY,
            scrollDeltaY = deltaY
        )
        _state.value = _state.value.copy(
            lastEventDispatched = event,
            eventsSentCount = _state.value.eventsSentCount + 1
        )
        dispatchInputPacket(event)
    }

    /**
     * Envía un botón de navegación del sistema operativo (Atrás, Inicio, Recientes, Bloqueo).
     */
    fun sendNavigationKey(type: VirtualInputType) {
        val event = VirtualInputEvent(
            type = type,
            normalizedX = _state.value.virtualCursorX,
            normalizedY = _state.value.virtualCursorY
        )
        _state.value = _state.value.copy(
            lastEventDispatched = event,
            eventsSentCount = _state.value.eventsSentCount + 1
        )
        dispatchInputPacket(event)
    }

    /**
     * Transmite una cadena de caracteres o tecla enter para inyección directa en el campo de texto remoto.
     */
    fun sendTextInput(text: String) {
        val event = VirtualInputEvent(
            type = VirtualInputType.KEY_TEXT,
            textPayload = text
        )
        _state.value = _state.value.copy(
            lastEventDispatched = event,
            eventsSentCount = _state.value.eventsSentCount + 1
        )
        dispatchInputPacket(event)
    }

    /**
     * Procesa un evento de entrada recibido remotamente.
     */
    fun handleIncomingRemoteInput(eventJson: String) {
        try {
            val json = JSONObject(eventJson)
            val typeStr = json.optString("type", VirtualInputType.TAP.name)
            val type = VirtualInputType.valueOf(typeStr)
            val x = json.optDouble("x", 0.5).toFloat()
            val y = json.optDouble("y", 0.5).toFloat()
            val text = json.optString("text", null)

            val event = VirtualInputEvent(
                eventId = json.optString("id", UUID.randomUUID().toString()),
                type = type,
                normalizedX = x,
                normalizedY = y,
                textPayload = text
            )

            _state.value = _state.value.copy(
                lastReceivedEvent = event,
                virtualCursorX = x,
                virtualCursorY = y,
                eventsReceivedCount = _state.value.eventsReceivedCount + 1
            )

            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                tag = "INPUT_INJECTOR",
                message = "Inyectando evento remoto ${event.type} en ($x, $y) [Texto: $text]"
            )
        } catch (e: Exception) {
            // Ignorar errores de formato en paquetes corruptos
        }
    }

    private fun dispatchInputPacket(event: VirtualInputEvent) {
        scope.launch {
            val target = _state.value.targetNodeId
            val json = JSONObject().apply {
                put("id", event.eventId)
                put("type", event.type.name)
                put("x", event.normalizedX)
                put("y", event.normalizedY)
                put("deltaY", event.scrollDeltaY)
                if (event.textPayload != null) put("text", event.textPayload)
                put("ts", event.timestamp)
            }.toString()

            // Transmisión inmediata vía Malla Store-and-Forward / LAN
            StoreAndForwardRouter.getInstance().enqueueLocalPacket(
                sourceId = "LOCAL_DEVICE",
                destinationId = target,
                priority = PacketPriority.EMERGENCY_TELEMETRY,
                payloadType = "VIRTUAL_INPUT_EVENT",
                data = json,
                ttlHops = 4
            )
        }
    }
}
