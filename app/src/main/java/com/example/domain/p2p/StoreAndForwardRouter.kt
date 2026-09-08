package com.example.domain.p2p

import android.content.Context
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.dao.DtnBundleDao
import com.example.domain.local.entities.DtnBundleEntity
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Nivel de Prioridad y Calidad de Servicio (QoS) para Enrutamiento Táctico DTN
 */
enum class PacketPriority(val value: Int, val label: String, val badgeColorHex: Long) {
    CRITICAL_SOS(1, "CRITICAL SOS", 0xFFFF1744),
    EMERGENCY_TELEMETRY(2, "TELEMETRÍA DE ALERTA", 0xFFFF9100),
    VOICE_PTT(3, "AUDIO PTT EN VIVO", 0xFF00E5FF),
    HIGH_PRIORITY_TEXT(4, "MENSAJE TEXTO E2EE", 0xFF69F0AE),
    IMAGE_EVIDENCE(5, "EVIDENCIA OPTICA", 0xFFB388FF),
    MESH_HEARTBEAT(6, "LATIDO DE MALLA", 0xFF78909C)
}

/**
 * Paquete de Datos Multi-Salto para Enrutamiento Epidémico / Store-and-Forward
 */
data class MeshRelayPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val sourceNodeId: String,
    val destinationNodeId: String, // ID específico o "BROADCAST_ALL"
    val priority: PacketPriority = PacketPriority.HIGH_PRIORITY_TEXT,
    val payloadType: String, // "TEXT", "SOS", "PTT_AUDIO", "GIS_MARKER", "OPTICAL", "EACK"
    val payloadData: String,
    val ttlHops: Int = 7, // Saltos máximos permitidos
    val currentHops: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L, // 24 horas
    val visitedNodes: List<String> = listOf(sourceNodeId)
) {
    fun toEntity(): DtnBundleEntity = DtnBundleEntity(
        bundleId = packetId,
        sourceNodeId = sourceNodeId,
        destinationNodeId = destinationNodeId,
        priority = priority.name,
        payloadType = payloadType,
        payloadData = payloadData,
        ttlHops = ttlHops,
        currentHops = currentHops,
        timestamp = timestamp,
        expiresAt = expiresAt,
        visitedNodesCsv = visitedNodes.joinToString(","),
        custodyAccepted = true
    )

    companion object {
        fun fromEntity(entity: DtnBundleEntity): MeshRelayPacket {
            val prio = try {
                PacketPriority.valueOf(entity.priority)
            } catch (e: Exception) {
                PacketPriority.HIGH_PRIORITY_TEXT
            }
            return MeshRelayPacket(
                packetId = entity.bundleId,
                sourceNodeId = entity.sourceNodeId,
                destinationNodeId = entity.destinationNodeId,
                priority = prio,
                payloadType = entity.payloadType,
                payloadData = entity.payloadData,
                ttlHops = entity.ttlHops,
                currentHops = entity.currentHops,
                timestamp = entity.timestamp,
                expiresAt = entity.expiresAt,
                visitedNodes = entity.getVisitedList()
            )
        }
    }
}

/**
 * Vector de Resumen Anti-Entropía intercambiado entre nodos para sincronización eficiente.
 */
data class AntiEntropySummaryVector(
    val senderNodeId: String,
    val bundleIds: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Enrutador Multi-Salto Store-and-Forward (DTN Epidemic Routing, Custody Transfer & Congestion Control).
 * Almacena en la base de datos local Room/SQLCipher para persistencia ante reinicios y caídas de energía.
 */
class StoreAndForwardRouter private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dtnDao: DtnBundleDao? = null

    // Caché en memoria para acceso ultrarrápido durante ráfagas de radiofrecuencia
    private val memoryPacketQueue = ConcurrentHashMap<String, MeshRelayPacket>()
    private val processedPacketHashes = ConcurrentHashMap.newKeySet<String>()

    private val _queuedPacketsCount = MutableStateFlow(0)
    val queuedPacketsCount: StateFlow<Int> = _queuedPacketsCount.asStateFlow()

    private val _relayedPacketsCount = MutableStateFlow(0)
    val relayedPacketsCount: StateFlow<Int> = _relayedPacketsCount.asStateFlow()

    private val _qosDropCount = MutableStateFlow(0)
    val qosDropCount: StateFlow<Int> = _qosDropCount.asStateFlow()

    private val _custodyTransfersCount = MutableStateFlow(0)
    val custodyTransfersCount: StateFlow<Int> = _custodyTransfersCount.asStateFlow()

    private val _antiEntropySyncCount = MutableStateFlow(0)
    val antiEntropySyncCount: StateFlow<Int> = _antiEntropySyncCount.asStateFlow()

    private val _recentRelayedPackets = MutableStateFlow<List<MeshRelayPacket>>(emptyList())
    val recentRelayedPackets: StateFlow<List<MeshRelayPacket>> = _recentRelayedPackets.asStateFlow()

    companion object {
        const val MAX_BUFFER_CAPACITY = 300

        @Volatile
        private var INSTANCE: StoreAndForwardRouter? = null

        fun getInstance(): StoreAndForwardRouter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoreAndForwardRouter().also { INSTANCE = it }
            }
        }
    }

    /**
     * Inicializa la persistencia con la base de datos SQLCipher
     */
    fun initialize(context: Context) {
        if (dtnDao != null) return
        val db = OmniDatabase.getDatabase(context)
        dtnDao = db.dtnBundleDao()

        // Restaurar paquetes activos desde Room
        scope.launch {
            try {
                dtnDao?.purgeExpiredOrDelivered()
                val persisted = dtnDao?.getActiveBundlesSync() ?: emptyList()
                for (entity in persisted) {
                    val packet = MeshRelayPacket.fromEntity(entity)
                    memoryPacketQueue[packet.packetId] = packet
                    processedPacketHashes.add(packet.packetId)
                }
                updateState()
                Log.d("StoreAndForwardRouter", "DTN Storage restauró ${persisted.size} paquetes en custodia.")
            } catch (e: Exception) {
                Log.e("StoreAndForwardRouter", "Error al restaurar DTN bundles: ${e.message}")
            }
        }
    }

    /**
     * Encola un nuevo paquete originado localmente con su prioridad QoS y lo persiste
     */
    fun enqueueLocalPacket(
        sourceId: String,
        destinationId: String,
        priority: PacketPriority,
        payloadType: String,
        data: String,
        ttlHops: Int = 7
    ): MeshRelayPacket {
        enforceBufferCongestionControl()

        val packet = MeshRelayPacket(
            sourceNodeId = sourceId,
            destinationNodeId = destinationId,
            priority = priority,
            payloadType = payloadType,
            payloadData = data,
            ttlHops = ttlHops,
            currentHops = 0,
            visitedNodes = listOf(sourceId)
        )

        memoryPacketQueue[packet.packetId] = packet
        processedPacketHashes.add(packet.packetId)
        updateState()

        scope.launch {
            try {
                dtnDao?.insertBundle(packet.toEntity())
            } catch (e: Exception) {
                Log.w("StoreAndForwardRouter", "Fallo al persistir DTN bundle en Room: ${e.message}")
            }
        }

        DiscoveryLogCollector.log(
            category = LogCategory.BLUETOOTH_MESH,
            severity = if (priority == PacketPriority.CRITICAL_SOS) LogSeverity.WARNING else LogSeverity.INFO,
            tag = "STORE_FORWARD",
            message = "Paquete local [${packet.payloadType}] en custodia DTN (QoS: ${priority.label}, Saltos: 0/$ttlHops)",
            metadata = mapOf("destinationId" to destinationId, "packetId" to packet.packetId)
        )

        return packet
    }

    /**
     * Procesa un paquete recibido de otro nodo de la malla o túnel Wi-Fi Direct.
     * Retorna true si el paquete debe consumirse localmente por ser destinatario o broadcast.
     */
    fun onPacketReceivedFromMesh(packet: MeshRelayPacket, localNodeId: String): Boolean {
        // Manejar reconocimientos End-to-End (EACK)
        if (packet.payloadType == "EACK") {
            handleDeliveryAck(packet.payloadData)
            return true
        }

        // 1. Descartar si ya fue procesado o ha expirado
        if (processedPacketHashes.contains(packet.packetId)) {
            return false
        }
        if (System.currentTimeMillis() > packet.expiresAt) {
            _qosDropCount.value += 1
            return false
        }
        if (packet.currentHops >= packet.ttlHops) {
            _qosDropCount.value += 1
            return false
        }

        processedPacketHashes.add(packet.packetId)

        // 2. Comprobar si este nodo es el destinatario o es mensaje grupal broadcast
        val isForMe = packet.destinationNodeId == localNodeId || packet.destinationNodeId == "BROADCAST_ALL"

        // Si es para mí, generar acuse de entrega (EACK) hacia la malla si no era broadcast
        if (isForMe && packet.destinationNodeId != "BROADCAST_ALL") {
            enqueueDeliveryAck(sourceNodeId = localNodeId, targetNodeId = packet.sourceNodeId, deliveredPacketId = packet.packetId)
        }

        // 3. Custody Transfer: Si aún tiene saltos disponibles, almacenar y retransmitir (Store-and-Forward)
        val updatedPacket = packet.copy(
            currentHops = packet.currentHops + 1,
            visitedNodes = packet.visitedNodes + localNodeId
        )

        if (updatedPacket.currentHops < updatedPacket.ttlHops) {
            enforceBufferCongestionControl()
            memoryPacketQueue[updatedPacket.packetId] = updatedPacket
            _relayedPacketsCount.value += 1
            _custodyTransfersCount.value += 1
            updateState()

            scope.launch {
                try {
                    dtnDao?.insertBundle(updatedPacket.toEntity())
                } catch (e: Exception) {
                    Log.w("StoreAndForwardRouter", "Error al actualizar bundle en Room: ${e.message}")
                }
            }

            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.INFO,
                tag = "MULTI_HOP_RELAY",
                message = "Custodia DTN aceptada [${packet.payloadType}] Salto ${updatedPacket.currentHops}/${updatedPacket.ttlHops}",
                metadata = mapOf("sourceNodeId" to packet.sourceNodeId, "packetId" to packet.packetId)
            )
        }

        return isForMe
    }

    /**
     * Genera el Vector de Resumen de Anti-Entropía con los paquetes que posee este nodo
     */
    fun createAntiEntropySummary(localNodeId: String): AntiEntropySummaryVector {
        val now = System.currentTimeMillis()
        val activeIds = memoryPacketQueue.values
            .filter { it.expiresAt > now }
            .map { it.packetId }
        return AntiEntropySummaryVector(senderNodeId = localNodeId, bundleIds = activeIds)
    }

    /**
     * Compara el Vector de Resumen recibido de un par con nuestro inventario
     * y retorna los paquetes que el par necesita recibir.
     */
    fun getBundlesToSyncWithPeer(peerSummary: AntiEntropySummaryVector): List<MeshRelayPacket> {
        val peerKnownSet = peerSummary.bundleIds.toSet()
        val now = System.currentTimeMillis()

        val missingForPeer = memoryPacketQueue.values
            .filter { packet ->
                packet.expiresAt > now &&
                !peerKnownSet.contains(packet.packetId) &&
                !packet.visitedNodes.contains(peerSummary.senderNodeId)
            }
            .sortedWith(
                compareBy<MeshRelayPacket> { it.priority.value }
                    .thenBy { it.timestamp }
            )

        _antiEntropySyncCount.value += 1
        return missingForPeer
    }

    /**
     * Encola un acuse de recibo de entrega (EACK) con prioridad máxima (CRITICAL_SOS)
     */
    private fun enqueueDeliveryAck(sourceNodeId: String, targetNodeId: String, deliveredPacketId: String) {
        val ackPacket = MeshRelayPacket(
            sourceNodeId = sourceNodeId,
            destinationNodeId = targetNodeId,
            priority = PacketPriority.CRITICAL_SOS,
            payloadType = "EACK",
            payloadData = deliveredPacketId,
            ttlHops = 7
        )
        memoryPacketQueue[ackPacket.packetId] = ackPacket
        processedPacketHashes.add(ackPacket.packetId)
        updateState()
    }

    /**
     * Procesa un acuse de recibo de entrega (EACK), eliminando el paquete de la custodia de Room
     */
    fun handleDeliveryAck(packetId: String) {
        memoryPacketQueue.remove(packetId)
        updateState()
        scope.launch {
            try {
                dtnDao?.markDelivered(packetId)
            } catch (e: Exception) {
                Log.w("StoreAndForwardRouter", "Error al marcar bundle como entregado: ${e.message}")
            }
        }
    }

    /**
     * Control de congestión adaptativo: Si la memoria o disco supera el límite de capacidad,
     * purga los paquetes de menor prioridad QoS y mayor tiempo en cola (Drop Least Priority FIFO).
     */
    private fun enforceBufferCongestionControl() {
        if (memoryPacketQueue.size >= MAX_BUFFER_CAPACITY) {
            val candidateToDrop = memoryPacketQueue.values
                .filter { it.priority != PacketPriority.CRITICAL_SOS }
                .maxByOrNull { it.priority.value * 1000000000L - it.timestamp }

            if (candidateToDrop != null) {
                memoryPacketQueue.remove(candidateToDrop.packetId)
                _qosDropCount.value += 1
                scope.launch {
                    try {
                        dtnDao?.markDelivered(candidateToDrop.packetId)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                Log.w("StoreAndForwardRouter", "Congestión DTN: Paquete descartado por QoS: ${candidateToDrop.packetId}")
            }
        }
    }

    /**
     * Devuelve los paquetes pendientes ordenados por prioridad de QoS para transmitir a un nodo recién conectado
     */
    fun getPacketsToRelayToPeer(targetPeerId: String): List<MeshRelayPacket> {
        val now = System.currentTimeMillis()
        memoryPacketQueue.entries.removeIf { it.value.expiresAt < now }

        val eligible = memoryPacketQueue.values
            .filter { packet ->
                !packet.visitedNodes.contains(targetPeerId)
            }
            .sortedWith(
                compareBy<MeshRelayPacket> { it.priority.value }
                    .thenBy { it.timestamp }
            )

        return eligible
    }

    /**
     * Limpia completamente la cola DTN en memoria y base de datos (usado en Zeroize / Emergencia)
     */
    fun purgeAllBundles() {
        memoryPacketQueue.clear()
        processedPacketHashes.clear()
        updateState()
        scope.launch {
            try {
                dtnDao?.clearAllBundles()
            } catch (e: Exception) {
                Log.e("StoreAndForwardRouter", "Error al purgar paquetes en Room: ${e.message}")
            }
        }
    }

    private fun updateState() {
        _queuedPacketsCount.value = memoryPacketQueue.size
        _recentRelayedPackets.value = memoryPacketQueue.values.take(15).toList()
    }
}
