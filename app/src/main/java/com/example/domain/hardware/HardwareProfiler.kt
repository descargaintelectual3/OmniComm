package com.example.domain.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

/**
 * Exprime el hardware del dispositivo.
 * Monitorea CPU, RAM y Batería para decidir si el dispositivo puede actuar como 
 * un "Super Nodo" (Servidor de enrutamiento principal) o un "Nodo Ligero" para ahorrar energía.
 */
class HardwareProfiler(private val context: Context) {
    
    fun getDeviceCapabilityScore(): Int {
        var score = 100
        
        // 1. Analizar Batería
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()
        
        val isCharging: Boolean = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING

        // Si la batería es baja y no está cargando, reducimos su capacidad como servidor
        if (batteryPct < 20 && !isCharging) {
            score -= 50
            Log.w("HardwareProfiler", "Batería crítica. Cambiando a Nodo Ligero.")
        } else if (isCharging) {
            score += 20 // Conectado a la corriente: Potencia máxima.
        }

        // 2. Analizar RAM Disponible
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableRamMB = memoryInfo.availMem / (1024 * 1024)
        if (availableRamMB < 1024) {
            score -= 30 // Poca RAM: Reducir carga P2P
        }

        Log.d("HardwareProfiler", "Score de Hardware del Nodo: $score/100 (RAM Libre: $availableRamMB MB)")
        return score.coerceIn(0, 100) // 100 = Super Nodo, < 50 = Nodo Ligero
    }
}
