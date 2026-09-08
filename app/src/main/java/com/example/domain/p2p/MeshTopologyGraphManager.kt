package com.example.domain.p2p

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class MeshLink(
    val fromNodeId: String,
    val toNodeId: String,
    val linkQualityPercent: Int,
    val latencyMs: Int,
    val isDirect: Boolean
)

data class MeshTopologyNode(
    val nodeId: String,
    val callsign: String,
    val role: String,
    val batteryPercent: Int,
    val hopsFromLocal: Int,
    val isRelayActive: Boolean,
    val xPos: Float, // 0.0 a 1.0 para renderizado en canvas
    val yPos: Float
)

data class MeshTopologyState(
    val nodes: List<MeshTopologyNode> = emptyList(),
    val links: List<MeshLink> = emptyList(),
    val totalHopsDiameter: Int = 3,
    val networkHealthPercent: Int = 94
)

/**
 * Gestor de Topología de Malla Gráfica:
 * Rastrea la constelación de nodos distribuidos y sus interconexiones inalámbricas.
 */
object MeshTopologyGraphManager {

    private val _topologyState = MutableStateFlow(generateInitialTopology())
    val topologyState: StateFlow<MeshTopologyState> = _topologyState.asStateFlow()

    fun refreshTopology() {
        _topologyState.value = generateInitialTopology()
    }

    private fun generateInitialTopology(): MeshTopologyState {
        val nodes = listOf(
            MeshTopologyNode("LOCAL_HOST", "TU NODO (LOCAL)", "Gateway / Mando", 96, 0, true, 0.5f, 0.5f),
            MeshTopologyNode("NODE_ALPHA", "ALFA-RECON", "Explorador BLE", 88, 1, true, 0.25f, 0.3f),
            MeshTopologyNode("NODE_BRAVO", "BRAVO-SENTRY", "Repetidor Wi-Fi", 72, 1, true, 0.75f, 0.35f),
            MeshTopologyNode("NODE_CHARLIE", "CHARLIE-DRONE", "Vigilancia Aérea", 64, 2, false, 0.85f, 0.75f),
            MeshTopologyNode("NODE_DELTA", "DELTA-MEDIC", "Puesto Médico", 91, 1, false, 0.2f, 0.7f),
            MeshTopologyNode("NODE_ECHO", "ECHO-BASE", "HQ Remoto", 100, 2, true, 0.5f, 0.9f)
        )

        val links = listOf(
            MeshLink("LOCAL_HOST", "NODE_ALPHA", 95, 12, true),
            MeshLink("LOCAL_HOST", "NODE_BRAVO", 88, 18, true),
            MeshLink("LOCAL_HOST", "NODE_DELTA", 92, 15, true),
            MeshLink("NODE_BRAVO", "NODE_CHARLIE", 78, 42, false),
            MeshLink("NODE_DELTA", "NODE_ECHO", 84, 30, false),
            MeshLink("NODE_ALPHA", "NODE_DELTA", 65, 50, false),
            MeshLink("NODE_CHARLIE", "NODE_ECHO", 70, 48, false)
        )

        return MeshTopologyState(
            nodes = nodes,
            links = links,
            totalHopsDiameter = 2,
            networkHealthPercent = 92
        )
    }
}
