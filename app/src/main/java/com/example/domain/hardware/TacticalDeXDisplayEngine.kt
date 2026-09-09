package com.example.domain.hardware

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modos de Proyección Externa
 */
enum class DeXOperatingMode(val label: String, val description: String) {
    TACTICAL_DEX_DESKTOP("Omni-DeX C4ISR Desktop", "Escritorio táctico militar multi-ventana en pantalla externa HDMI/TV"),
    SCREEN_MIRROR_LOW_LATENCY("Espejo Táctico de Pantalla (Mirror)", "Retransmisión íntegra de la pantalla del móvil a TV en tiempo real"),
    DISCONNECTED_STANDBY("En Espera / Desconectado", "Sin pantalla secundaria detectada (esperando conexión OTG HDMI o Cast)")
}

/**
 * Ventana Táctica Activa dentro del Sistema de Escritorio Omni-DeX
 */
data class TacticalDesktopWindow(
    val id: String,
    val title: String,
    val iconName: String,
    val isVisible: Boolean = true,
    val isMinimized: Boolean = false,
    val xPercent: Float, // 0.0f a 1.0f relativo a la resolución externa
    val yPercent: Float,
    val widthPercent: Float,
    val heightPercent: Float,
    val zIndex: Int = 1
)

/**
 * Estado Operativo del Sistema Omni-DeX Táctico
 */
data class DeXDisplayState(
    val isExternalDisplayConnected: Boolean = false,
    val externalDisplayName: String = "Ninguna",
    val externalResolution: String = "1920x1080 @ 60Hz",
    val displayDpi: Int = 320,
    val operatingMode: DeXOperatingMode = DeXOperatingMode.TACTICAL_DEX_DESKTOP,
    val pointerXPercent: Float = 0.5f,
    val pointerYPercent: Float = 0.5f,
    val isPointerClicking: Boolean = false,
    val defconStatus: Int = 2,
    val emconStatus: String = "EMCON-BRAVO",
    val zuluTime: String = "12:00:00Z",
    val windows: List<TacticalDesktopWindow> = emptyList(),
    val logHistory: List<String> = emptyList()
)

/**
 * Motor de Escritorio Industrial y Militar Omni-DeX (Samsung DeX Alternativo de Grado Táctico).
 * Detecta automáticamente adaptadores USB-C / OTG a HDMI, monitores DisplayPort y receptores Miracast/Cast.
 * Transforma cualquier televisor o pantalla externa en una estación de trabajo militar C4ISR completa
 * mientras el teléfono móvil actúa como panel táctil (trackpad), teclado remoto y controlador C2.
 */
class TacticalDeXDisplayEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private val _state = MutableStateFlow(DeXDisplayState())
    val state: StateFlow<DeXDisplayState> = _state.asStateFlow()

    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateZuluTime()
            timeHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val TAG = "DeXDisplayEngine"

        @Volatile
        private var instance: TacticalDeXDisplayEngine? = null

        fun getInstance(context: Context): TacticalDeXDisplayEngine {
            return instance ?: synchronized(this) {
                instance ?: TacticalDeXDisplayEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        initDefaultWindows()
        registerDisplayListener()
        checkExistingDisplays()
        timeHandler.post(timeRunnable)
    }

    private fun initDefaultWindows() {
        val initialWindows = listOf(
            TacticalDesktopWindow(
                id = "WIN_RADAR",
                title = "RADAR TÁCTICO & WAYPOINTS UAV",
                iconName = "Radar",
                xPercent = 0.05f,
                yPercent = 0.10f,
                widthPercent = 0.42f,
                heightPercent = 0.40f,
                zIndex = 1
            ),
            TacticalDesktopWindow(
                id = "WIN_SPECTRUM",
                title = "ANALIZADOR ESPECTRAL RF & CASCADA",
                iconName = "GraphicEq",
                xPercent = 0.50f,
                yPercent = 0.10f,
                widthPercent = 0.45f,
                heightPercent = 0.38f,
                zIndex = 2
            ),
            TacticalDesktopWindow(
                id = "WIN_NATO_MAP",
                title = "MAPA SITUACIONAL OTAN APP-6D",
                iconName = "Map",
                xPercent = 0.05f,
                yPercent = 0.53f,
                widthPercent = 0.48f,
                heightPercent = 0.38f,
                zIndex = 3
            ),
            TacticalDesktopWindow(
                id = "WIN_TERMINAL",
                title = "CONSOLA C2 & INPUT REMOTO SERIAL",
                iconName = "Terminal",
                xPercent = 0.55f,
                yPercent = 0.51f,
                widthPercent = 0.40f,
                heightPercent = 0.40f,
                zIndex = 4
            )
        )
        _state.value = _state.value.copy(windows = initialWindows)
    }

    private fun registerDisplayListener() {
        displayManager?.registerDisplayListener(object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {
                addLog("Nueva pantalla externa detectada (Display ID: $displayId) vía HDMI/Cast")
                inspectDisplay(displayId)
            }

            override fun onDisplayRemoved(displayId: Int) {
                addLog("Pantalla externa desconectada (Display ID: $displayId)")
                _state.value = _state.value.copy(
                    isExternalDisplayConnected = false,
                    externalDisplayName = "Ninguna",
                    operatingMode = DeXOperatingMode.DISCONNECTED_STANDBY
                )
            }

            override fun onDisplayChanged(displayId: Int) {
                inspectDisplay(displayId)
            }
        }, null)
    }

    private fun checkExistingDisplays() {
        val displays = displayManager?.displays ?: emptyArray()
        for (d in displays) {
            if (d.displayId != Display.DEFAULT_DISPLAY) {
                inspectDisplay(d.displayId)
                return
            }
        }
        // Si no hay física, mantenemos listo el simulador DeX para que el usuario pueda visualizar
        // el escritorio táctico en vivo desde la aplicación o lanzar a Smart TV
        addLog("Motor Omni-DeX listo. Modo Táctico Desktop armado para proyección inmediata.")
    }

    private fun inspectDisplay(displayId: Int) {
        val display = displayManager?.getDisplay(displayId) ?: return
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)

        val resString = "${metrics.widthPixels}x${metrics.heightPixels} @ ${display.refreshRate.toInt()}Hz"
        _state.value = _state.value.copy(
            isExternalDisplayConnected = true,
            externalDisplayName = display.name ?: "Pantalla HDMI Externa",
            externalResolution = resString,
            displayDpi = metrics.densityDpi,
            operatingMode = DeXOperatingMode.TACTICAL_DEX_DESKTOP
        )
        addLog("Pantalla Externa activada: ${display.name} [$resString]")
    }

    /**
     * Mueve el puntero del cursor virtual de Omni-DeX usando el trackpad del móvil
     */
    fun moveTrackpadPointer(deltaXPercent: Float, deltaYPercent: Float) {
        val current = _state.value
        val newX = (current.pointerXPercent + deltaXPercent).coerceIn(0.01f, 0.99f)
        val newY = (current.pointerYPercent + deltaYPercent).coerceIn(0.01f, 0.99f)
        _state.value = current.copy(pointerXPercent = newX, pointerYPercent = newY)
    }

    fun triggerPointerClick() {
        _state.value = _state.value.copy(isPointerClicking = true)
        timeHandler.postDelayed({
            _state.value = _state.value.copy(isPointerClicking = false)
        }, 150)
        addLog("Click de ratón enviado a pantalla externa en (${(_state.value.pointerXPercent * 100).toInt()}%, ${(_state.value.pointerYPercent * 100).toInt()}%)")
    }

    fun setOperatingMode(mode: DeXOperatingMode) {
        _state.value = _state.value.copy(operatingMode = mode)
        addLog("Modo de Pantalla cambiado a: ${mode.label}")
    }

    fun toggleWindowVisibility(windowId: String) {
        val updated = _state.value.windows.map { win ->
            if (win.id == windowId) win.copy(isVisible = !win.isVisible) else win
        }
        _state.value = _state.value.copy(windows = updated)
    }

    private fun updateZuluTime() {
        val sdf = SimpleDateFormat("HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        _state.value = _state.value.copy(zuluTime = sdf.format(Date()))
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = "[$time] $message"
        val updated = (_state.value.logHistory + entry).takeLast(25)
        _state.value = _state.value.copy(logHistory = updated)
    }
}
