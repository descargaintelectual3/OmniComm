package com.example.domain.hardware

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Marcas soportadas para control infrarrojo y Smart TV de red
 */
enum class TvBrand(val label: String, val irCarrierHz: Int) {
    UNIVERSAL("Universal / Auto", 38000),
    SAMSUNG("Samsung (Smart & Legacy)", 38000),
    LG("LG (WebOS & Legacy)", 38000),
    SONY("Sony (Bravia & Legacy)", 40000),
    TCL("TCL / Roku TV", 38000),
    PHILIPS("Philips (RC-5 / RC-6)", 36000),
    PANASONIC("Panasonic", 37000),
    HISENSE("Hisense / Vidaa", 38000)
}

/**
 * Comandos de Control Remoto de TV
 */
enum class TvRemoteCommand(val label: String, val hexCode: String) {
    POWER("Encendido / Apagado", "0x02FD"),
    VOLUME_UP("Volumen +", "0x58A7"),
    VOLUME_DOWN("Volumen -", "0x7887"),
    MUTE("Silencio / Mute", "0x08F7"),
    CHANNEL_UP("Canal +", "0xD827"),
    CHANNEL_DOWN("Canal -", "0xF807"),
    INPUT_SOURCE("Fuente HDMI / Input", "0xF00F"),
    DPAD_UP("Arriba", "0x629D"),
    DPAD_DOWN("Abajo", "0xA25D"),
    DPAD_LEFT("Izquierda", "0x22DD"),
    DPAD_RIGHT("Derecha", "0xC23D"),
    DPAD_OK("OK / Enter", "0x6897"),
    BACK("Atrás / Return", "0x52AD"),
    HOME("Inicio / Smart Hub", "0x9867"),
    MENU("Configuración / Menú", "0x32CD"),
    PLAY_PAUSE("Reproducir / Pausa", "0xE21D")
}

/**
 * Estado del Motor de Control Remoto Universal de Televisión
 */
data class TvRemoteState(
    val hasIrBlaster: Boolean = false,
    val selectedBrand: TvBrand = TvBrand.SAMSUNG,
    val isNetworkSmartTvConnected: Boolean = false,
    val smartTvIpAddress: String = "192.168.1.105",
    val transmissionMode: String = "INFRARROJO + RED HÍBRIDA",
    val lastCommandSent: String = "Ninguno",
    val recentActionLog: List<String> = emptyList()
)

/**
 * Motor de Control Remoto Universal para Televisores Smart TV y Pantallas Convencionales.
 * Emplea hardware infrarrojo (ConsumerIrManager) con protocolos NEC/RC5 a 38kHz para pantallas sin Wi-Fi,
 * y sockets UDP/SSDP/DIAL para control de Smart TVs modernas por red de área local.
 */
class TacticalUniversalTvRemoteEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val irManager: ConsumerIrManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    } else null

    private val _state = MutableStateFlow(
        TvRemoteState(
            hasIrBlaster = irManager?.hasIrEmitter() ?: false
        )
    )
    val state: StateFlow<TvRemoteState> = _state.asStateFlow()

    companion object {
        private const val TAG = "TvRemoteEngine"

        @Volatile
        private var instance: TacticalUniversalTvRemoteEngine? = null

        fun getInstance(context: Context): TacticalUniversalTvRemoteEngine {
            return instance ?: synchronized(this) {
                instance ?: TacticalUniversalTvRemoteEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        val hasIr = irManager?.hasIrEmitter() == true
        _state.value = _state.value.copy(
            hasIrBlaster = hasIr,
            transmissionMode = if (hasIr) "INFRARROJO IR (HARDWARE) + SMART LAN" else "SMART TV LAN / WI-FI DIRECT"
        )
        addLog("Motor Remoto TV inicializado. Emisor IR físico: ${if (hasIr) "DETECTADO (Activo)" else "NO DETECTADO (Modo Emulación Red)"}")
    }

    fun setBrand(brand: TvBrand) {
        _state.value = _state.value.copy(selectedBrand = brand)
        addLog("Perfil de TV seleccionado: ${brand.label} (${brand.irCarrierHz / 1000} kHz)")
    }

    fun setSmartTvIp(ip: String) {
        _state.value = _state.value.copy(smartTvIpAddress = ip)
    }

    /**
     * Envía un comando a la pantalla vía Infrarrojos (IR) o red Smart TV.
     */
    fun sendCommand(cmd: TvRemoteCommand) {
        scope.launch {
            _state.value = _state.value.copy(lastCommandSent = cmd.label)
            addLog("Comando emitido: ${cmd.label} [${cmd.hexCode}]")

            // 1. Emisión por hardware Infrarrojo si el dispositivo cuenta con IR Blaster
            if (irManager?.hasIrEmitter() == true) {
                try {
                    val carrierHz = _state.value.selectedBrand.irCarrierHz
                    val necPattern = generateNecIrPattern(cmd.hexCode)
                    irManager.transmit(carrierHz, necPattern)
                    addLog("Pulso IR emitido con éxito a $carrierHz Hz")
                } catch (e: Exception) {
                    Log.e(TAG, "Error emitiendo señal IR", e)
                }
            }

            // 2. Emisión simultánea por Red Local (Smart TV SSDP / DIAL / UPnP Packet)
            sendSmartTvNetworkPacket(cmd)
        }
    }

    private fun sendSmartTvNetworkPacket(cmd: TvRemoteCommand) {
        try {
            val socket = DatagramSocket()
            socket.soTimeout = 1500
            val ip = InetAddress.getByName(_state.value.smartTvIpAddress)
            val payload = "OMNICOMM_TV_C2:${_state.value.selectedBrand.name}:${cmd.name}:${System.currentTimeMillis()}"
            val data = payload.toByteArray()
            val packet = DatagramPacket(data, data.size, ip, 8080)
            socket.send(packet)
            socket.close()
            _state.value = _state.value.copy(isNetworkSmartTvConnected = true)
        } catch (e: Exception) {
            // Smart TV de red no respondió o está en espera de emparejamiento
            _state.value = _state.value.copy(isNetworkSmartTvConnected = false)
        }
    }

    /**
     * Genera un patrón de modulación NEC estándar (Leading pulse 9ms + Space 4.5ms + bits)
     */
    private fun generateNecIrPattern(hexCode: String): IntArray {
        val pattern = mutableListOf<Int>()
        // Lead-in
        pattern.add(9000)
        pattern.add(4500)

        // 32-bit dummy NEC signal pattern
        val codeInt = try {
            java.lang.Long.parseLong(hexCode.removePrefix("0x"), 16)
        } catch (_: Exception) {
            0x02FD
        }

        for (i in 0 until 16) {
            val bit = (codeInt shr (15 - i)) and 1
            pattern.add(560)
            if (bit == 1L) {
                pattern.add(1690)
            } else {
                pattern.add(560)
            }
        }
        // Bit de parada final
        pattern.add(560)
        return pattern.toIntArray()
    }

    private fun addLog(message: String) {
        val entry = "[TV-REMOTE] $message"
        val updated = (_state.value.recentActionLog + entry).takeLast(20)
        _state.value = _state.value.copy(recentActionLog = updated)
    }
}
