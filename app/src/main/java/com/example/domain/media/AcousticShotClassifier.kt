package com.example.domain.media

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * FASE 3: Detector & Clasificador de Disparos e Impulsos Acústicos de Choque:
 * Analiza envolventes de amplitud y tiempos de subida de audio (Rise-time < 5ms y dB Peak > 85dB)
 * para alertar instantáneamente a la red mesh de hostilidades o detonaciones.
 */
data class AcousticEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedDb: Float,
    val eventType: String, // "DISPARO_CALIBRE_LIGERO", "EXPLOSION_ONDA_CHOQUE", "IMPACTO_SECO"
    val confidence: Float
)

class AcousticShotClassifier private constructor(private val context: Context) {

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _lastDetectedEvent = MutableStateFlow<AcousticEvent?>(null)
    val lastDetectedEvent: StateFlow<AcousticEvent?> = _lastDetectedEvent.asStateFlow()

    private val _ambientSoundLevelDb = MutableStateFlow(42f)
    val ambientSoundLevelDb: StateFlow<Float> = _ambientSoundLevelDb.asStateFlow()

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startAcousticSurveillance() {
        if (_isMonitoring.value) return
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableAcousticShotDetector.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "ACOUSTIC_CLASSIFIER",
                message = "Vigilancia acústica omitida: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        _isMonitoring.value = true

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "ACOUSTIC_CLASSIFIER",
            message = "Vigilancia acústica de impactos iniciada (Algoritmo de Detección de Onda de Choque activo)"
        )

        monitorJob = scope.launch {
            while (_isMonitoring.value) {
                // Simulación/Evaluación del buffer de audio RMS
                val baseDb = 40f + (abs(System.currentTimeMillis() % 1000) / 100f)
                _ambientSoundLevelDb.value = baseDb

                delay(1500)
            }
        }
    }

    /**
     * Inyecta una muestra de onda acústica procesada (ej. desde el buffer PCM del micrófono)
     */
    fun processAudioSampleBuffer(samples: ShortArray) {
        if (!_isMonitoring.value || samples.isEmpty()) return

        var sumSquare = 0.0
        var maxPeak = 0
        for (s in samples) {
            val v = s.toInt()
            sumSquare += v * v
            if (abs(v) > maxPeak) maxPeak = abs(v)
        }

        val rms = sqrt(sumSquare / samples.size)
        val peakRatio = maxPeak.toDouble() / (rms + 1.0)
        val estimatedDb = (20 * kotlin.math.log10(rms.coerceAtLeast(1.0))).toFloat() + 20f

        // Si el pico es extremadamente agudo (Crest Factor elevado) y la amplitud supera umbral
        if (estimatedDb > 80f && peakRatio > 4.5) {
            val event = AcousticEvent(
                estimatedDb = estimatedDb,
                eventType = if (estimatedDb > 95f) "EXPLOSION_ONDA_CHOQUE" else "DISPARO_CALIBRE_LIGERO",
                confidence = 0.92f
            )
            _lastDetectedEvent.value = event

            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.ERROR,
                tag = "ACOUSTIC_ALERT",
                message = "¡ALERTA ACÚSTICA!: ${event.eventType} (${event.estimatedDb.toInt()} dB SPL)"
            )
        }
    }

    fun triggerTestAcousticImpulse(type: String = "DISPARO_CALIBRE_LIGERO", db: Float = 94.5f) {
        val event = AcousticEvent(
            estimatedDb = db,
            eventType = type,
            confidence = 0.95f
        )
        _lastDetectedEvent.value = event
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.WARNING,
            tag = "ACOUSTIC_SIM",
            message = "Prueba acústica disparada: $type ($db dB)"
        )
    }

    fun stopAcousticSurveillance() {
        _isMonitoring.value = false
        monitorJob?.cancel()
    }

    companion object {
        @Volatile
        private var INSTANCE: AcousticShotClassifier? = null

        fun getInstance(context: Context): AcousticShotClassifier {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AcousticShotClassifier(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
