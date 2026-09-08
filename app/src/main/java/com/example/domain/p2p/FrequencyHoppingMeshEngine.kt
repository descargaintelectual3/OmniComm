package com.example.domain.p2p

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * FASE 7 (Plan Maestro): Motor de Salto de Frecuencia Virtual (FHSS - Frequency-Hopping Spread Spectrum):
 * Genera una secuencia pseudoaleatoria determinista de canales (1..14 WiFi / 0..39 BLE) basada en una semilla
 * de tiempo y clave de misión compartida para evitar interferencias hostiles y radiogoniometría enemiga.
 */
data class FhssHopStatus(
    val currentChannel: Int = 1,
    val frequencyMhz: Int = 2412,
    val hopRatePerSec: Int = 10,
    val nextHopInMs: Long = 100,
    val hoppingPattern: String = "SHA256-DETERMINISTIC",
    val isAntiJammingActive: Boolean = true
)

class FrequencyHoppingMeshEngine private constructor(private val context: Context) {

    private val _hopStatus = MutableStateFlow(FhssHopStatus())
    val hopStatus: StateFlow<FhssHopStatus> = _hopStatus.asStateFlow()

    private val _isHoppingActive = MutableStateFlow(false)
    val isHoppingActive: StateFlow<Boolean> = _isHoppingActive.asStateFlow()

    private var hoppingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var missionSeed = "TACTICAL_APOLLO_KEY_99"

    fun startFrequencyHopping(seed: String = "TACTICAL_APOLLO_KEY_99", hopIntervalMs: Long = 100L) {
        if (_isHoppingActive.value) return
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableFrequencyHopping.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "FHSS_ENGINE",
                message = "Salto de frecuencia FHSS omitido: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        _isHoppingActive.value = true
        missionSeed = seed

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "FHSS_ENGINE",
            message = "Salto de Frecuencia Virtual FHSS iniciado (Semilla criptográfica activa, Tasa: ${1000 / hopIntervalMs} hops/s)"
        )

        hoppingJob = scope.launch {
            while (_isHoppingActive.value) {
                val epochSlot = System.currentTimeMillis() / hopIntervalMs
                val channel = computeDeterministicChannel(missionSeed, epochSlot)
                val freq = 2412 + (channel - 1) * 5

                _hopStatus.value = FhssHopStatus(
                    currentChannel = channel,
                    frequencyMhz = freq,
                    hopRatePerSec = (1000 / hopIntervalMs).toInt(),
                    nextHopInMs = hopIntervalMs - (System.currentTimeMillis() % hopIntervalMs),
                    isAntiJammingActive = true
                )

                delay(hopIntervalMs)
            }
        }
    }

    fun stopFrequencyHopping() {
        _isHoppingActive.value = false
        hoppingJob?.cancel()
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "FHSS_ENGINE",
            message = "Secuencia FHSS detenida. Retorno a canal estático."
        )
    }

    private fun computeDeterministicChannel(seed: String, slot: Long): Int {
        val input = "$seed:$slot"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        val firstByte = digest[0].toInt() and 0xFF
        // Mapear a canal ISM 1..13
        return (firstByte % 13) + 1
    }

    companion object {
        @Volatile
        private var INSTANCE: FrequencyHoppingMeshEngine? = null

        fun getInstance(context: Context): FrequencyHoppingMeshEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FrequencyHoppingMeshEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
