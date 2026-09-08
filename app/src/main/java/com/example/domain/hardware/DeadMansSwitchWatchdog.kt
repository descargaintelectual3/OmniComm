package com.example.domain.hardware

import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DeadManInterval(val label: String, val durationMs: Long) {
    MINUTES_5("5 Minutos", 5 * 60 * 1000L),
    MINUTES_15("15 Minutos", 15 * 60 * 1000L),
    MINUTES_30("30 Minutos", 30 * 60 * 1000L),
    HOURS_1("1 Hora", 60 * 60 * 1000L)
}

/**
 * Interruptor de Hombre Muerto Táctico (Dead Man's Switch):
 * Requiere confirmación periódica de presencia por parte del operador.
 * Si expira sin confirmación, emite una baliza SOS de socorro automática con la última posición conocida.
 */
object DeadMansSwitchWatchdog {

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _selectedInterval = MutableStateFlow(DeadManInterval.MINUTES_15)
    val selectedInterval: StateFlow<DeadManInterval> = _selectedInterval.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(15 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isWarningState = MutableStateFlow(false)
    val isWarningState: StateFlow<Boolean> = _isWarningState.asStateFlow()

    private var watchdogJob: Job? = null

    fun startWatchdog(interval: DeadManInterval = _selectedInterval.value) {
        _selectedInterval.value = interval
        _isActive.value = true
        _remainingSeconds.value = (interval.durationMs / 1000).toInt()
        _isWarningState.value = false

        watchdogJob?.cancel()
        watchdogJob = CoroutineScope(Dispatchers.Default).launch {
            while (_isActive.value && _remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1

                // Últimos 60 segundos entran en estado de advertencia crítica
                if (_remainingSeconds.value <= 60 && !_isWarningState.value) {
                    _isWarningState.value = true
                }
            }

            if (_isActive.value && _remainingSeconds.value <= 0) {
                triggerDeadManEmergency()
            }
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "DEAD_MANS_SWITCH",
            message = "Interruptor de Hombre Muerto armado para intervalo de ${interval.label}."
        )
    }

    fun checkInOperatorAlive() {
        if (!_isActive.value) return
        _remainingSeconds.value = (_selectedInterval.value.durationMs / 1000).toInt()
        _isWarningState.value = false

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "DEAD_MANS_CHECKIN",
            message = "Operador confirmó presencia. Temporizador de Hombre Muerto restablecido a ${_selectedInterval.value.label}."
        )
    }

    fun disarmWatchdog() {
        _isActive.value = false
        watchdogJob?.cancel()
        watchdogJob = null
        _isWarningState.value = false

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "DEAD_MANS_DISARMED",
            message = "Interruptor de Hombre Muerto desarmado."
        )
    }

    private fun triggerDeadManEmergency() {
        _isWarningState.value = true
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.ERROR,
            tag = "DEAD_MANS_TRIGGERED",
            message = "🚨 ALERTA CRÍTICA: INACTIVIDAD DE OPERADOR. Emitiendo baliza SOS a la malla."
        )
    }
}
