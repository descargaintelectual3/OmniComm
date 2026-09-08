package com.example.domain.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PowerProfile(
    val title: String,
    val description: String,
    val scanDurationMs: Long,
    val sleepDurationMs: Long,
    val estimatedMilliwatts: Int,
    val dutyCyclePercent: Int
) {
    STEALTH_LOW_POWER(
        title = "Sigilo / Ahorro Extremo",
        description = "Escaneo intermitente BLE (3s cada 15s) para máxima duración de batería en campo",
        scanDurationMs = 3000L,
        sleepDurationMs = 15000L,
        estimatedMilliwatts = 18,
        dutyCyclePercent = 16
    ),
    PATROL_STANDARD(
        title = "Patrulla Estándar",
        description = "Balance óptimo entre detección de nodos y consumo energético (5s escaneo / 5s reposo)",
        scanDurationMs = 5000L,
        sleepDurationMs = 5000L,
        estimatedMilliwatts = 55,
        dutyCyclePercent = 50
    ),
    CONTINUOUS_TACTICAL(
        title = "Operación Continua (HQ)",
        description = "Escaneo ininterrumpido a máxima potencia para baja latencia de enlace",
        scanDurationMs = 10000L,
        sleepDurationMs = 0L,
        estimatedMilliwatts = 120,
        dutyCyclePercent = 100
    )
}

/**
 * Gestor de Perfiles de Energía Adaptativos para Radiofrecuencia (BLE / Wi-Fi Mesh).
 * Ajusta el ciclo de trabajo (Duty Cycle) según el nivel de batería y el perfil seleccionado.
 */
class AdaptivePowerProfileManager private constructor(context: Context) {

    private val _currentProfile = MutableStateFlow(PowerProfile.PATROL_STANDARD)
    val currentProfile: StateFlow<PowerProfile> = _currentProfile.asStateFlow()

    private val _batteryLevel = MutableStateFlow(85)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _isEcoModeForced = MutableStateFlow(false)
    val isEcoModeForced: StateFlow<Boolean> = _isEcoModeForced.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AdaptivePowerProfileManager? = null

        fun getInstance(context: Context): AdaptivePowerProfileManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdaptivePowerProfileManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // Registrar receptor de estado de batería
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    val pct = (level * 100 / scale.toFloat()).toInt()
                    _batteryLevel.value = pct
                    checkBatteryThreshold(pct)
                }
            }
        }, filter)
    }

    private fun checkBatteryThreshold(batteryPct: Int) {
        if (batteryPct <= 20 && _currentProfile.value != PowerProfile.STEALTH_LOW_POWER) {
            _isEcoModeForced.value = true
            setProfile(PowerProfile.STEALTH_LOW_POWER)
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "BATTERY_GUARD",
                message = "Batería baja ($batteryPct%). Modo Sigilo / Ahorro activado automáticamente."
            )
        } else if (batteryPct > 25 && _isEcoModeForced.value) {
            _isEcoModeForced.value = false
        }
    }

    fun setProfile(profile: PowerProfile) {
        _currentProfile.value = profile
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "POWER_PROFILE",
            message = "Perfil de energía cambiado a: ${profile.title} (Duty Cycle: ${profile.dutyCyclePercent}%)"
        )
    }
}
