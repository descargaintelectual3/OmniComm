package com.example.domain.discovery

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.domain.chat.FirestoreEncryptedChatService
import com.example.domain.hardware.BluetoothMeshService
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.ContactEntity
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogSeverity
import com.example.domain.models.ConnectionType
import com.example.domain.models.TacticalNotificationPayload
import com.example.domain.security.CryptoManager
import com.example.services.OmniPushService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class MeshNodeBroadcast(
    val nodeId: String = "",
    val displayName: String = "",
    val avatar: String = "🛡️",
    val model: String = "",
    val publicKeyFingerprint: String = "",
    val rawPublicKeyBase64: String = "",
    val batteryPercent: Int = 100,
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = 0L,
    val preferredMedium: String = "CLOUD_RELAY + LAN_P2P",
    val fcmToken: String = ""
)

/**
 * Motor Maestro de Auto-Descubrimiento y Enlace Autónomo Instantáneo.
 * Orquesta en paralelo:
 * 1. Protocolo de Señalización Global Cloud (Firestore /tactical_nodes y /mesh_signals).
 * 2. Protocolo de Balizas UDP Multicast & Broadcast Local (Wi-Fi / Hotspot).
 * 3. Enlace y Escaneo de Malla Bluetooth RFCOMM.
 * 
 * En cuanto se instala o abre la aplicación en cualquier dispositivo,
 * se anuncia y vincula automáticamente con todos los demás teléfonos
 * sin requerir inicio de sesión manual ni configuración previa.
 */
class AutonomousMeshDiscoveryEngine private constructor(private val context: Context) {

    private val TAG = "AutoMeshDiscovery"
    private val LOG_TAG_DISCOVERY = "PeerDiscovery:Mesh"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (t: Throwable) {
            Log.w(TAG, "FirebaseFirestore no inicializado o sin configuración: ${t.message}")
            null
        }
    }
    private val database = OmniDatabase.getDatabase(context)
    private val contactDao = database.contactDao()
    private val cryptoManager = CryptoManager()
    private val encryptedChatService = FirestoreEncryptedChatService(context)
    private val bluetoothMeshService = BluetoothMeshService.getInstance(context)

    private val prefs = context.getSharedPreferences("omni_tactical_identity_prefs", Context.MODE_PRIVATE)

    // Identificador persistente y único del nodo
    val localNodeId: String = prefs.getString("local_node_id", null) ?: run {
        val newId = "OP-" + (Build.MODEL.replace(" ", "").take(6).uppercase()) + "-" + UUID.randomUUID().toString().take(4).uppercase()
        prefs.edit().putString("local_node_id", newId).apply()
        newId
    }

    private val localDisplayName: String
        get() = prefs.getString("user_custom_name", "Operador ${Build.MODEL}") ?: "Operador ${Build.MODEL}"

    private val localAvatar: String
        get() = prefs.getString("user_custom_avatar", "🛡️") ?: "🛡️"

    private val localFingerprint = cryptoManager.getPublicKeyFingerprint()
    private val localPublicKeyBase64 = cryptoManager.getPublicEncryptionKeyBase64()

    private var lanBeacon: LanMulticastDiscoveryBeacon? = null
    private var globalNodesListener: ListenerRegistration? = null
    private var signalsListener: ListenerRegistration? = null

    private val _discoveredNodesMap = MutableStateFlow<Map<String, MeshNodeBroadcast>>(emptyMap())
    val discoveredNodesMap: StateFlow<Map<String, MeshNodeBroadcast>> = _discoveredNodesMap.asStateFlow()

    private val _discoveryStatus = MutableStateFlow("🟢 Auto-Descubrimiento Activo: Cloud + LAN + Bluetooth")
    val discoveryStatus: StateFlow<String> = _discoveryStatus.asStateFlow()

    private val _activeNodesCount = MutableStateFlow(0)
    val activeNodesCount: StateFlow<Int> = _activeNodesCount.asStateFlow()

    private var isStarted = false

    fun startAutoDiscovery() {
        if (isStarted) return
        isStarted = true

        Log.d(TAG, "🚀 Iniciando Motor de Auto-Descubrimiento Autónomo para Nodo: $localNodeId")

        // 1. Iniciar Baliza LAN UDP Multicast (Cero Internet / Misma Red Wi-Fi o Hotspot)
        startLanDiscovery()

        // 2. Iniciar Registro y Escucha en el Pool Global Cloud de Firestore
        startCloudDiscovery()

        // 3. Iniciar Demonio de Bluetooth Mesh
        startBluetoothMesh()

        // 4. Iniciar Bucle Periódico de Presencia (Heartbeat cada 12 segundos)
        startPeriodicPresenceHeartbeat()
    }

    private fun getBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 90
        } catch (e: Exception) {
            90
        }
    }

    /**
     * 1. Protocolo LAN UDP Multicast & Broadcast
     */
    private fun startLanDiscovery() {
        lanBeacon = LanMulticastDiscoveryBeacon(
            context = context,
            localNodeId = localNodeId,
            localNodeName = localDisplayName,
            localNodeAvatar = localAvatar,
            localFingerprint = localFingerprint,
            localPublicKeyBase64 = localPublicKeyBase64,
            onPeerDiscovered = { peer ->
                handleDiscoveredPeer(
                    nodeId = peer.nodeId,
                    name = peer.name,
                    avatar = peer.avatar,
                    model = peer.model,
                    fingerprint = peer.publicKeyFingerprint,
                    publicKeyBase64 = peer.rawPublicKeyBase64,
                    battery = peer.batteryPercent,
                    medium = ConnectionType.WIFI_DIRECT,
                    sourceLabel = "LAN Wi-Fi Direct (${peer.ipAddress})"
                )
            }
        )
        lanBeacon?.start()
    }

    /**
     * 2. Protocolo Cloud de Firestore y Señales Globales
     */
    private fun startCloudDiscovery() {
        val fs = firestore ?: run {
            Log.w(TAG, "Cloud discovery omitido: Firebase no configurado. Operando en modo LAN + Bluetooth Mesh.")
            return
        }

        // A. Publicar presencia inmediata del nodo en Firestore /tactical_nodes
        publishLocalPresence()

        // B. Escuchar activamente todos los nodos tácticos registrados en la nube
        globalNodesListener = fs.collection("tactical_nodes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando /tactical_nodes: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val currentNodes = mutableMapOf<String, MeshNodeBroadcast>()
                        for (doc in snapshot.documents) {
                            val node = doc.toObject(MeshNodeBroadcast::class.java)
                            if (node != null && node.nodeId != localNodeId && node.nodeId.isNotBlank()) {
                                currentNodes[node.nodeId] = node

                                // Si el nodo ha tenido actividad reciente (últimos 15 minutos), auto-vincular
                                val isRecentlyActive = (System.currentTimeMillis() - node.lastSeenTimestamp) < 900000L || node.isOnline
                                if (isRecentlyActive) {
                                    handleDiscoveredPeer(
                                        nodeId = node.nodeId,
                                        name = node.displayName.ifBlank { "Operador ${node.model}" },
                                        avatar = node.avatar.ifBlank { "🛡️" },
                                        model = node.model.ifBlank { "Terminal Táctica" },
                                        fingerprint = node.publicKeyFingerprint,
                                        publicKeyBase64 = node.rawPublicKeyBase64,
                                        battery = node.batteryPercent,
                                        medium = ConnectionType.CLOUD_RELAY,
                                        sourceLabel = "Cloud Mesh Relay"
                                    )
                                }
                            }
                        }
                        _discoveredNodesMap.value = currentNodes
                        _activeNodesCount.value = currentNodes.size
                    }
                }
            }

        // C. Emitir señal inmediata de bienvenida / aviso a toda la red
        broadcastMeshSignal(type = "NODE_HELLO_BROADCAST", text = "👋 $localDisplayName se ha conectado a la red.")

        // D. Escuchar señales de bienvenida de otros nodos
        signalsListener = fs.collection("mesh_signals")
            .whereGreaterThanOrEqualTo("timestamp", System.currentTimeMillis() - 60000L)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (change in snapshot.documentChanges) {
                        val senderId = change.document.getString("senderId") ?: ""
                        val senderName = change.document.getString("senderName") ?: ""
                        val type = change.document.getString("type") ?: ""

                        if (senderId.isNotBlank() && senderId != localNodeId) {
                            DiscoveryLogCollector.logWebhookSignal(
                                signalType = type,
                                status = "Señal entrante recibida de $senderName ($senderId)",
                                payload = "{ \"type\": \"$type\", \"sender\": \"$senderName\", \"senderId\": \"$senderId\" }",
                                severity = LogSeverity.INFO
                            )
                            if (type == "NODE_HELLO_BROADCAST") {
                                Log.d(TAG, "📡 Señal de saludo recibida de $senderName. Respondiendo con ACK...")
                                broadcastMeshSignal(
                                    type = "NODE_HELLO_ACK",
                                    targetNodeId = senderId,
                                    text = "🤝 Conexión bidireccional confirmada con $localDisplayName."
                                )
                            }
                        }
                    }
                }
            }
    }

    private fun startBluetoothMesh() {
        try {
            bluetoothMeshService.startMeshNodeAutoDaemon()
        } catch (e: Exception) {
            Log.w(TAG, "Error iniciando Bluetooth mesh: ${e.message}")
        }
    }

    private fun startPeriodicPresenceHeartbeat() {
        scope.launch {
            while (isStarted && isActive) {
                delay(12000)
                publishLocalPresence()
                lanBeacon?.broadcastDiscoveryPacket()
            }
        }
    }

    /**
     * Publica o actualiza el estado de presencia de este nodo en Firestore
     */
    fun publishLocalPresence() {
        val fs = firestore ?: return
        scope.launch {
            try {
                val nodePayload = MeshNodeBroadcast(
                    nodeId = localNodeId,
                    displayName = localDisplayName,
                    avatar = localAvatar,
                    model = Build.MODEL,
                    publicKeyFingerprint = localFingerprint,
                    rawPublicKeyBase64 = localPublicKeyBase64,
                    batteryPercent = getBatteryLevel(),
                    isOnline = true,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    preferredMedium = "DUAL_RADIO_CLOUD"
                )

                fs.collection("tactical_nodes")
                    .document(localNodeId)
                    .set(nodePayload, SetOptions.merge())

                // También actualizar en /users para sincronización de Contactos
                fs.collection("users")
                    .document(localNodeId)
                    .set(
                        mapOf(
                            "uid" to localNodeId,
                            "displayName" to localDisplayName,
                            "email" to "$localNodeId@tactical.omni",
                            "photoUrl" to "",
                            "batteryPercent" to getBatteryLevel(),
                            "isOnline" to true,
                            "lastSeenTimestamp" to System.currentTimeMillis(),
                            "publicKeyFingerprint" to localFingerprint,
                            "rawPublicKeyBase64" to localPublicKeyBase64
                        ),
                        SetOptions.merge()
                    )
            } catch (e: Exception) {
                Log.w(TAG, "Error publicando presencia local: ${e.message}")
            }
        }
    }

    fun broadcastMeshSignal(type: String, targetNodeId: String = "", text: String = "") {
        val fs = firestore ?: return
        scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val signalId = UUID.randomUUID().toString()
                val payload = hashMapOf(
                    "id" to signalId,
                    "type" to type,
                    "senderId" to localNodeId,
                    "senderName" to localDisplayName,
                    "senderAvatar" to localAvatar,
                    "targetNodeId" to targetNodeId,
                    "text" to text,
                    "timestamp" to System.currentTimeMillis()
                )
                fs.collection("mesh_signals").document(signalId).set(payload).await()
                val duration = System.currentTimeMillis() - startTime
                DiscoveryLogCollector.logWebhookSignal(
                    signalType = type,
                    status = "Señal Webhook/Malla emitida hacia /mesh_signals/$signalId ($duration ms)",
                    payload = "{ \"type\": \"$type\", \"sender\": \"$localDisplayName\", \"target\": \"${targetNodeId.ifBlank { "GLOBAL_BROADCAST" }}\", \"text\": \"$text\" }",
                    severity = LogSeverity.SUCCESS,
                    durationMs = duration
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error emitiendo señal: ${e.message}")
                DiscoveryLogCollector.logWebhookSignal(
                    signalType = type,
                    status = "Error emitiendo señal: ${e.message}",
                    severity = LogSeverity.ERROR
                )
            }
        }
    }

    /**
     * Procesador unificado para cualquier nodo descubierto por Cloud, LAN o Bluetooth
     */
    private fun handleDiscoveredPeer(
        nodeId: String,
        name: String,
        avatar: String,
        model: String,
        fingerprint: String,
        publicKeyBase64: String,
        battery: Int,
        medium: ConnectionType,
        sourceLabel: String
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🤝 Iniciando procesamiento de enlace con nodo: $name ($nodeId) vía $sourceLabel")
                val finalFingerprint = fingerprint.ifBlank { "SHA256:" + nodeId.hashCode().toString(16).take(8).uppercase() }

                // 1. Guardar o actualizar en Room DB (Contactos)
                val existing = contactDao.getContactDirect(nodeId)
                val contactEntity = ContactEntity(
                    deviceId = nodeId,
                    alias = name.ifBlank { "Operador $model" },
                    email = if (existing != null && existing.email.isNotBlank()) existing.email else "$nodeId@tactical.omni",
                    avatarIcon = avatar.ifBlank { "🛡️" },
                    lastSeenTimestamp = System.currentTimeMillis(),
                    isOnline = true,
                    isTrustedNode = true,
                    isPriority = existing?.isPriority ?: false,
                    batteryPercent = battery,
                    publicKeyFingerprint = finalFingerprint,
                    rawPublicKeyBase64 = publicKeyBase64,
                    preferredMedium = medium.label
                )
                contactDao.insertContact(contactEntity)
                Log.d(TAG, "[$LOG_TAG_DISCOVERY] 💾 Contacto guardado/actualizado en Room DB: ${contactEntity.deviceId} (${contactEntity.alias})")

                // 2. Disparar Handshake en PeerDiscoveryService (Topología Radar)
                PeerDiscoveryService.getInstance(context).initiateAutomatedHandshake(
                    remoteId = nodeId,
                    remoteName = name,
                    remoteAvatar = avatar,
                    medium = medium,
                    rssi = if (medium == ConnectionType.WIFI_DIRECT) -38 else -45
                )

                // 3. Crear o recuperar la Sesión de Chat Cifrado E2EE automáticamente
                encryptedChatService.createOrGetDirectSession(
                    myUid = localNodeId,
                    myName = localDisplayName,
                    partnerUid = nodeId,
                    partnerName = name,
                    partnerPublicKeyFingerprint = finalFingerprint
                )

                Log.d(TAG, "[$LOG_TAG_DISCOVERY] ✅ Enlace automático y sesión E2EE listos con: $name ($nodeId) vía $sourceLabel")
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando nodo descubierto: ${e.message}", e)
            }
        }
    }

    fun stop() {
        isStarted = false
        lanBeacon?.stop()
        globalNodesListener?.remove()
        signalsListener?.remove()
        // Marcar como offline en Firestore
        val fs = firestore
        if (fs != null) {
            scope.launch {
                try {
                    fs.collection("tactical_nodes")
                        .document(localNodeId)
                        .update("isOnline", false, "lastSeenTimestamp", System.currentTimeMillis())
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AutonomousMeshDiscoveryEngine? = null

        fun getInstance(context: Context): AutonomousMeshDiscoveryEngine {
            return INSTANCE ?: synchronized(this) {
                val instance = AutonomousMeshDiscoveryEngine(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
