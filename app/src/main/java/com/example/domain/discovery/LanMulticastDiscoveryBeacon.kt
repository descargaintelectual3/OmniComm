package com.example.domain.discovery

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

data class LanPeerInfo(
    val nodeId: String,
    val name: String,
    val avatar: String,
    val model: String,
    val ipAddress: String,
    val tcpPort: Int = 8889,
    val publicKeyFingerprint: String = "",
    val rawPublicKeyBase64: String = "",
    val batteryPercent: Int = 100,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/**
 * Baliza y Receptor de Auto-Descubrimiento Local por UDP Multicast & Broadcast (Puerto 8888).
 * Permite que dos o más dispositivos en la misma red Wi-Fi o Hotspot se detecten
 * instantáneamente sin requerir conexión a internet ni configuración manual.
 */
class LanMulticastDiscoveryBeacon(
    private val context: Context,
    private val localNodeId: String,
    private val localNodeName: String,
    private val localNodeAvatar: String,
    private val localFingerprint: String,
    private val localPublicKeyBase64: String,
    private val onPeerDiscovered: (LanPeerInfo) -> Unit
) {
    private val TAG = "LanMulticastBeacon"
    private val DISCOVERY_PORT = 8888
    private val MULTICAST_GROUP = "239.255.255.250"
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var multicastLock: WifiManager.MulticastLock? = null
    private var isRunning = false
    
    private val _discoveredLanPeers = MutableStateFlow<Map<String, LanPeerInfo>>(emptyMap())
    val discoveredLanPeers: StateFlow<Map<String, LanPeerInfo>> = _discoveredLanPeers.asStateFlow()

    fun start() {
        if (isRunning) return
        isRunning = true

        acquireMulticastLock()
        startListeningSocket()
        startPeriodicBroadcaster()
        Log.d(TAG, "🟢 Baliza LAN UDP iniciada en puerto $DISCOVERY_PORT")
        DiscoveryLogCollector.logLanBeacon(
            action = "BEACON_START",
            message = "Socket de escucha UDP iniciado en puerto :$DISCOVERY_PORT (Multicast $MULTICAST_GROUP)",
            severity = LogSeverity.SUCCESS
        )
    }

    fun stop() {
        isRunning = false
        scope.cancel()
        releaseMulticastLock()
        Log.d(TAG, "🔴 Baliza LAN UDP detenida")
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("OmniCommLanDiscoveryLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo adquirir MulticastLock: ${e.message}")
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error liberando MulticastLock: ${e.message}")
        }
    }

    /**
     * Bucle de escucha UDP para paquetes entrantes en 0.0.0.0:8888
     */
    private fun startListeningSocket() {
        scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket(DISCOVERY_PORT).apply {
                    broadcast = true
                    reuseAddress = true
                }
                val buffer = ByteArray(2048)

                while (isRunning && isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val data = String(packet.data, 0, packet.length)
                        val senderIp = packet.address.hostAddress ?: ""

                        handleIncomingBeacon(data, senderIp)
                    } catch (e: Exception) {
                        if (!isRunning) break
                        delay(200)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error iniciando socket UDP receptor: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    /**
     * Emisión periódica de la baliza de presencia cada 2.5 segundos
     */
    private fun startPeriodicBroadcaster() {
        scope.launch {
            while (isRunning && isActive) {
                broadcastDiscoveryPacket()
                delay(2500)
            }
        }
    }

    /**
     * Envía inmediatamente una baliza de descubrimiento a la subred
     */
    fun broadcastDiscoveryPacket() {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("protocol", "OMNI_LAN_MESH_V1")
                    put("type", "BEACON_ANNOUNCE")
                    put("nodeId", localNodeId)
                    put("name", localNodeName)
                    put("avatar", localNodeAvatar)
                    put("model", Build.MODEL)
                    put("tcpPort", 8889)
                    put("fingerprint", localFingerprint)
                    put("publicKeyBase64", localPublicKeyBase64)
                    put("battery", 95)
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                val data = payload.toByteArray()
                val broadcastSocket = DatagramSocket().apply {
                    broadcast = true
                }

                // 1. Broadcast a 255.255.255.255
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val broadcastPacket = DatagramPacket(data, data.size, broadcastAddress, DISCOVERY_PORT)
                broadcastSocket.send(broadcastPacket)

                // 2. Multicast a 239.255.255.250
                try {
                    val multicastGroup = InetAddress.getByName(MULTICAST_GROUP)
                    val multicastPacket = DatagramPacket(data, data.size, multicastGroup, DISCOVERY_PORT)
                    broadcastSocket.send(multicastPacket)
                } catch (e: Exception) {
                    // Multicast opcional
                }

                broadcastSocket.close()
            } catch (e: Exception) {
                // Ignore network errors when Wi-Fi is disabled
            }
        }
    }

    private fun handleIncomingBeacon(rawJson: String, senderIp: String) {
        try {
            if (!rawJson.startsWith("{") || !rawJson.contains("OMNI_LAN_MESH_V1")) return

            val json = JSONObject(rawJson)
            val remoteNodeId = json.optString("nodeId", "")
            if (remoteNodeId.isBlank() || remoteNodeId == localNodeId) return

            val remoteName = json.optString("name", "Nodo-${remoteNodeId.takeLast(4)}")
            val remoteAvatar = json.optString("avatar", "🛡️")
            val remoteModel = json.optString("model", "Android Device")
            val tcpPort = json.optInt("tcpPort", 8889)
            val fingerprint = json.optString("fingerprint", "")
            val publicKeyBase64 = json.optString("publicKeyBase64", "")
            val battery = json.optInt("battery", 90)

            val peerInfo = LanPeerInfo(
                nodeId = remoteNodeId,
                name = remoteName,
                avatar = remoteAvatar,
                model = remoteModel,
                ipAddress = senderIp,
                tcpPort = tcpPort,
                publicKeyFingerprint = fingerprint,
                rawPublicKeyBase64 = publicKeyBase64,
                batteryPercent = battery,
                lastSeenTimestamp = System.currentTimeMillis()
            )

            val currentMap = _discoveredLanPeers.value.toMutableMap()
            val isNew = !currentMap.containsKey(remoteNodeId)
            currentMap[remoteNodeId] = peerInfo
            _discoveredLanPeers.value = currentMap

            if (isNew) {
                Log.d(TAG, "⚡ ¡Nuevo nodo detectado por LAN Wi-Fi! $remoteName [$senderIp]")
                DiscoveryLogCollector.logLanBeacon(
                    action = "PEER_DISCOVERED",
                    message = "¡Nuevo par descubierto vía LAN UDP! $remoteName ($remoteNodeId) IP: $senderIp:$tcpPort",
                    payload = rawJson,
                    severity = LogSeverity.SUCCESS
                )
                onPeerDiscovered(peerInfo)
                // Responder inmediatamente con una baliza directa unicast para acelerar el enlace bidireccional
                sendDirectUnicastReply(senderIp)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parseando baliza LAN: ${e.message}")
        }
    }

    private fun sendDirectUnicastReply(targetIp: String) {
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("protocol", "OMNI_LAN_MESH_V1")
                    put("type", "BEACON_ACK")
                    put("nodeId", localNodeId)
                    put("name", localNodeName)
                    put("avatar", localNodeAvatar)
                    put("model", Build.MODEL)
                    put("tcpPort", 8889)
                    put("fingerprint", localFingerprint)
                    put("publicKeyBase64", localPublicKeyBase64)
                    put("battery", 95)
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                val data = payload.toByteArray()
                val targetAddress = InetAddress.getByName(targetIp)
                val socket = DatagramSocket()
                val packet = DatagramPacket(data, data.size, targetAddress, DISCOVERY_PORT)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                // Ignore transient I/O
            }
        }
    }
}
