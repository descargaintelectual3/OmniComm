package com.example.domain.hardware

import android.content.Context
import android.util.Log
import com.example.domain.discovery.PeerDiscoveryService
import com.example.domain.models.ConnectionType

/**
 * Gestor de Conectividad de Proximidad de Alto Rendimiento.
 * Integra Bluetooth Low Energy (BLE) y Wi-Fi Direct (Wi-Fi P2P / Aware)
 * a través de PeerDiscoveryService para sincronización táctica total.
 */
class ProximityMeshManager(private val context: Context) {

    private val peerDiscoveryService = PeerDiscoveryService.getInstance(context)

    fun startProximityDiscovery() {
        Log.d("ProximityMesh", "Iniciando escaneo multi-radio BLE + Wi-Fi Direct...")
        peerDiscoveryService.forceMultiRadioScan()
    }

    fun broadcastNodePresence(nodeId: String) {
        Log.d("ProximityMesh", "Transmitiendo pulso de presencia y latido de red...")
        peerDiscoveryService.broadcastHeartbeatPulse()
    }

    fun establishDirectConnection(targetAddress: String, targetName: String = "Nodo Táctico", medium: ConnectionType = ConnectionType.WIFI_DIRECT) {
        Log.d("ProximityMesh", "Negociando túnel P2P cifrado con $targetAddress ($medium)...")
        peerDiscoveryService.initiateAutomatedHandshake(
            remoteId = targetAddress,
            remoteName = targetName,
            medium = medium,
            rssi = -50
        )
    }
}

