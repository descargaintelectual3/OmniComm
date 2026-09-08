package com.example.domain.security

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * FASE 15 (Plan Maestro): Red de Señuelos RF & Nodos Fantasma (Mesh Honeypot Generator):
 * Genera micro-balizas con identidades y firmas criptográficas simuladas (Ghost Nodes) para confundir
 * sistemas de vigilancia hostil (SIGINT) y registrar intentos de escaneo y penetración enemiga.
 */
data class GhostDecoyNode(
    val id: String,
    val fakeCallsign: String,
    val simulatedRssi: Int,
    val simulatedHopCount: Int,
    val isTrappingHostileScan: Boolean = false,
    val probeCount: Int = 0
)

class MeshHoneypotGenerator private constructor(context: Context) {

    private val _decoys = MutableStateFlow<List<GhostDecoyNode>>(
        listOf(
            GhostDecoyNode("DEC-01", "GHOST-RELAY-ALPHA", -62, 1),
            GhostDecoyNode("DEC-02", "DECOY-CP-BRAVO", -78, 2),
            GhostDecoyNode("DEC-03", "SHADOW-NODE-CHARLIE", -85, 3)
        )
    )
    val decoys: StateFlow<List<GhostDecoyNode>> = _decoys.asStateFlow()

    private val _isHoneypotActive = MutableStateFlow(false)
    val isHoneypotActive: StateFlow<Boolean> = _isHoneypotActive.asStateFlow()

    private var decoyJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startHoneypotNetwork() {
        if (_isHoneypotActive.value) return
        _isHoneypotActive.value = true

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "HONEYPOT_GEN",
            message = "Red de Señuelos RF Honeypot iniciada (${_decoys.value.size} nodos fantasma emitiendo firmas señuelo)"
        )

        decoyJob = scope.launch {
            while (_isHoneypotActive.value) {
                delay(8000)
                // Simular rotación de firmas de señal
                _decoys.value = _decoys.value.map { node ->
                    val jitter = ((-3..3).random())
                    node.copy(simulatedRssi = (node.simulatedRssi + jitter).coerceIn(-95, -50))
                }
            }
        }
    }

    fun stopHoneypotNetwork() {
        _isHoneypotActive.value = false
        decoyJob?.cancel()
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "HONEYPOT_GEN",
            message = "Red de Señuelos RF detenida. Nodos fantasma desactivados."
        )
    }

    fun simulateHostileProbe() {
        val list = _decoys.value.toMutableList()
        if (list.isNotEmpty()) {
            val targetIdx = (list.indices).random()
            val target = list[targetIdx]
            list[targetIdx] = target.copy(
                isTrappingHostileScan = true,
                probeCount = target.probeCount + 1
            )
            _decoys.value = list

            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "HONEYPOT_TRAP",
                message = "¡SONDEO HOSTIL ATRAPADO! Escaneo no autorizado dirigido al nodo fantasma '${target.fakeCallsign}'"
            )
        }
    }

    fun addDecoyNode(customCallsign: String) {
        val newDecoy = GhostDecoyNode(
            id = "DEC-${UUID.randomUUID().toString().take(4).uppercase()}",
            fakeCallsign = customCallsign.ifBlank { "GHOST-NODE-${(10..99).random()}" },
            simulatedRssi = (-80..-60).random(),
            simulatedHopCount = (1..3).random()
        )
        _decoys.value = _decoys.value + newDecoy
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "HONEYPOT_GEN",
            message = "Nuevo nodo señuelo desplegado: ${newDecoy.fakeCallsign}"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MeshHoneypotGenerator? = null

        fun getInstance(context: Context): MeshHoneypotGenerator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshHoneypotGenerator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
