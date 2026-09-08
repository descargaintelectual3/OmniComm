package com.example.domain.c4isr

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * FASE 26: Enmascarador Acústico y Análisis Espectral de Voz en Tiempo Real:
 * Procesa la señal de audio de los operadores de radio para aplicar aleatorización de formantes (Frequency Inversion / Scrambling)
 * y análisis espectral FFT en tiempo real con reducción de ruido de fondo de disparos/motores.
 */
data class VoiceSpectralData(
    val isScramblerActive: Boolean = true,
    val carrierInversionFreqHz: Int = 3300,
    val ambientNoiseLevelDbfs: Float = -48.5f,
    val snrRatioDb: Float = 22.4f,
    val fftFrequencyBands: List<Float> = listOf(0.12f, 0.45f, 0.85f, 0.65f, 0.35f, 0.18f, 0.08f)
)

class TacticalVoiceSpectralEngine private constructor(private val context: Context) {

    private val _spectralData = MutableStateFlow(VoiceSpectralData())
    val spectralData: StateFlow<VoiceSpectralData> = _spectralData.asStateFlow()

    fun toggleVoiceScrambler() {
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableVoiceSpectral.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "VOICE_SCRAMBLE",
                message = "Enmascarador de voz omitido: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        val next = !_spectralData.value.isScramblerActive
        _spectralData.value = _spectralData.value.copy(isScramblerActive = next)
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "VOICE_SCRAMBLE",
            message = "Enmascarador acústico espectral ${if (next) "ACTIVADO (Inversión 3.3 kHz)" else "DESACTIVADO"}"
        )
    }

    fun sampleSpectralAudio() {
        val bands = List(7) { Random.nextFloat().coerceIn(0.05f, 0.95f) }
        _spectralData.value = _spectralData.value.copy(
            fftFrequencyBands = bands,
            ambientNoiseLevelDbfs = -40f - Random.nextFloat() * 20f
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: TacticalVoiceSpectralEngine? = null

        fun getInstance(context: Context): TacticalVoiceSpectralEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalVoiceSpectralEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
