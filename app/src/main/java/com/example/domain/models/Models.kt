package com.example.domain.models

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class CloudFile(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val fileUrl: String = "",
    val uploaderId: String = "",
    val sizeBytes: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceNode(
    val deviceId: String = "",
    val deviceName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false,
    val rssi: Int = -80
)

enum class ConnectionType(val label: String, val protocol: String) {
    BLUETOOTH_MESH("Bluetooth Mesh (BLE/RFCOMM)", "BLE-5.3"),
    WIFI_DIRECT("Wi-Fi Direct (P2P High Speed)", "802.11ax P2P"),
    DUAL_RADIO("Enlace Dual (BT + Wi-Fi)", "Multi-Radio"),
    CLOUD_RELAY("Nube / Relevo Mesh", "IP-Sync")
}

enum class NodeStatus(val label: String) {
    AUTHENTICATED_AND_TRUSTED("Autenticado & Confiable"),
    HANDSHAKE_IN_PROGRESS("Handshake en Proceso"),
    DISCOVERED("Detectado en Radiofrecuencia"),
    OFFLINE("Fuera de Rango")
}

data class ConnectionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventTitle: String,
    val latencyMs: Long,
    val throughputMbps: Float,
    val success: Boolean = true
)

data class NetworkNode(
    val id: String,
    val name: String,
    val avatarIcon: String = "🛡️",
    val model: String = "Android Tactical Node",
    val connectionType: ConnectionType = ConnectionType.DUAL_RADIO,
    val publicKeyFingerprint: String = "SHA256:7B:9A:C3:F1",
    val rawPublicKeyBase64: String = "",
    val isLocalMaster: Boolean = false,
    val status: NodeStatus = NodeStatus.AUTHENTICATED_AND_TRUSTED,
    val rssi: Int = -55,
    val latencyMs: Long = 12,
    val throughputMbps: Float = 48.5f,
    val batteryLevel: Int = 88,
    val isCharging: Boolean = false,
    val isPriority: Boolean = false,
    val accessCount: Int = 1,
    val capabilities: List<String> = listOf("Voz Intercom", "Bóveda SQLCipher", "Almacenamiento Compartido", "Enrutador Malla"),
    val sharedStorageUsedMb: Int = 124,
    val sharedStorageTotalMb: Int = 512,
    val lastHeartbeatTimestamp: Long = System.currentTimeMillis(),
    val hopCount: Int = 1,
    val connectionHistory: List<ConnectionHistoryItem> = listOf(
        ConnectionHistoryItem(eventTitle = "Handshake RSA-2048 Inicial", latencyMs = 14, throughputMbps = 45.0f),
        ConnectionHistoryItem(eventTitle = "Sincronización de Bóveda E2EE", latencyMs = 10, throughputMbps = 62.5f),
        ConnectionHistoryItem(eventTitle = "Latido Heartbeat Reciente", latencyMs = 12, throughputMbps = 50.2f)
    ),
    // Coordenadas normalizadas (-1.0f a 1.0f) para layout de topología
    val layoutX: Float = 0f,
    val layoutY: Float = 0f
)

enum class SystemEventType(val title: String, val badgeColorHex: Long) {
    DISCOVERY("Detección de Nodo", 0xFF00E5FF),
    HANDSHAKE_INIT("Handshake Iniciado", 0xFFFFD600),
    KEY_EXCHANGE("Intercambio de Claves RSA", 0xFF69F0AE),
    HEARTBEAT("Latido de Presencia (Heartbeat)", 0xFF80D8FF),
    TRUST_ESTABLISHED("Conexión de Confianza Establecida", 0xFF00E676),
    STORAGE_SYNC("Sincronización de Archivos", 0xFFB388FF),
    WARNING("Alerta de Red", 0xFFFF5252)
}

data class MeshSystemEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: SystemEventType,
    val title: String,
    val details: String,
    val sourceNodeId: String? = null,
    val sourceNodeName: String? = null,
    val medium: String = "Dual-Radio (BLE + Wi-Fi Direct)"
)

