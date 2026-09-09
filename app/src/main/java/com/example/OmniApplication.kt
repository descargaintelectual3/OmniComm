package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class OmniApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Guardián Global contra Cierres Inesperados (Zero-Crash Guardian)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("OmniApplication", "CRASH_PREVENTED: Excepción interceptada en [${thread.name}]: ${throwable.message}", throwable)
            try {
                com.example.domain.logging.DiscoveryLogCollector.log(
                    category = com.example.domain.logging.LogCategory.SYSTEM,
                    severity = com.example.domain.logging.LogSeverity.ERROR,
                    tag = "CRASH_GUARDIAN",
                    message = "Fallo interceptado de forma segura en hilo [${thread.name}]: ${throwable.localizedMessage ?: throwable.javaClass.simpleName}"
                )
            } catch (_: Throwable) {
                // Fallback silencioso para garantizar que la app permanezca viva
            }
        }

        // 2. Inicialización segura de Firebase (con fallback offline inmediato)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(packageName)
                    .setProjectId("omnicomm-tactical")
                    .setApiKey("AIzaSyOmniCommDefaultTacticalKey2026")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("OmniApplication", "FirebaseApp inicializado con fallback táctico offline-safe.")
            }
        } catch (t: Throwable) {
            Log.w("OmniApplication", "No se pudo inicializar FirebaseApp: ${t.message}")
        }

        // 3. Inicialización DTN Store & Forward y Radios Tácticas
        try {
            com.example.domain.p2p.StoreAndForwardRouter.getInstance().initialize(this)
            Log.d("OmniApplication", "StoreAndForwardRouter DTN inicializado exitosamente.")
        } catch (t: Throwable) {
            Log.w("OmniApplication", "Error al inicializar DTN Router: ${t.message}")
        }

        try {
            com.example.domain.hardware.TacticalUsbRadioSerialEngine.getInstance(this)
            com.example.domain.media.AfskBell202ModemEngine.initialize(this)
            com.example.domain.hardware.TacticalUniversalTvRemoteEngine.getInstance(this)
            com.example.domain.hardware.TacticalDeXDisplayEngine.getInstance(this)
            com.example.domain.c2.TacticalUniversalDeviceGateway.getInstance(this)
            Log.d("OmniApplication", "Motores C4ISR: Radio USB, Módem AFSK, Remoto TV, Omni-DeX HDMI y Pasarela Multi-Dispositivo inicializados.")
        } catch (t: Throwable) {
            Log.w("OmniApplication", "Error al inicializar suite táctica extendida: ${t.message}")
        }
    }
}

