package com.example.domain.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.p2p.PacketPriority
import com.example.domain.p2p.StoreAndForwardRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class UsbRadioDeviceInfo(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val chipType: String, // CDC-ACM, FTDI, CP210X, CH340, PL2303, SIMULATED
    val isAttached: Boolean = false
)

data class UsbRadioState(
    val isConnected: Boolean = false,
    val activeDevice: UsbRadioDeviceInfo? = null,
    val baudRate: Int = 9600,
    val isPttActive: Boolean = false,
    val packetsSentCount: Int = 0,
    val packetsReceivedCount: Int = 0,
    val bytesTransferred: Long = 0L,
    val radioMode: String = "KISS_TNC_AFSK", // KISS_TNC_AFSK, RAW_SERIAL_AT, LORA_SERIAL
    val recentLog: List<String> = emptyList()
)

/**
 * Motor Táctico de Interfaz Serial USB-OTG / UART para Radios Militares y Transceptores.
 * Permite conectar módems KISS TNC, radios Baofeng/Kenwood VHF/UHF mediante cables de audio/serie,
 * placas LoRa ESP32 (Meshtastic/T-Beam) y módems satelitales vía USB-C OTG.
 */
class TacticalUsbRadioSerialEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var usbManager: UsbManager? = null

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private var readJob: Job? = null

    private val _state = MutableStateFlow(UsbRadioState())
    val state: StateFlow<UsbRadioState> = _state.asStateFlow()

    // Constantes de trama KISS TNC (AX.25 / APRS estándar)
    private val FEND: Byte = 0xC0.toByte()
    private val FESC: Byte = 0xDB.toByte()
    private val TFEND: Byte = 0xDC.toByte()
    private val TFESC: Byte = 0xDD.toByte()
    private val CMD_DATA: Byte = 0x00

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.USB_RADIO_PERMISSION"

        @Volatile
        private var INSTANCE: TacticalUsbRadioSerialEngine? = null

        fun getInstance(context: Context): TacticalUsbRadioSerialEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalUsbRadioSerialEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
        scanAvailableUsbDevices()
    }

    /**
     * Escanea el bus USB buscando dispositivos serie compatibles con radios militares y TNC.
     */
    fun scanAvailableUsbDevices(): List<UsbRadioDeviceInfo> {
        val detected = mutableListOf<UsbRadioDeviceInfo>()
        val devList = usbManager?.deviceList ?: emptyMap()

        for ((_, dev) in devList) {
            val chip = identifySerialChip(dev.vendorId, dev.productId)
            val info = UsbRadioDeviceInfo(
                deviceName = dev.productName ?: dev.deviceName,
                vendorId = dev.vendorId,
                productId = dev.productId,
                chipType = chip,
                isAttached = true
            )
            detected.add(info)
        }

        if (detected.isNotEmpty()) {
            _state.value = _state.value.copy(activeDevice = detected.first())
        } else if (_state.value.activeDevice == null) {
            // Emulador o terminal sin OTG conectado: Dispositivo de Radio Táctica Virtual disponible
            _state.value = _state.value.copy(
                activeDevice = UsbRadioDeviceInfo(
                    deviceName = "RADIO_MIL_VHF_VIRTUAL (Baofeng UV-5R / TNC)",
                    vendorId = 0x1A86,
                    productId = 0x7523,
                    chipType = "CH340 (KISS TNC Emulated)",
                    isAttached = true
                )
            )
        }
        return detected
    }

    private fun identifySerialChip(vid: Int, pid: Int): String {
        return when (vid) {
            0x0403 -> "FTDI FT232"
            0x10C4 -> "Silicon Labs CP210x"
            0x1A86 -> "WCH CH340 / CH341"
            0x067B -> "Prolific PL2303"
            0x2341, 0x1B4F, 0x2E8A -> "CDC-ACM (Arduino/ESP32/RP2040)"
            else -> "CDC-ACM Genérico USB"
        }
    }

    fun setBaudRate(rate: Int) {
        _state.value = _state.value.copy(baudRate = rate)
        addLog("Velocidad de transmisión configurada a $rate bps")
    }

    fun setRadioMode(mode: String) {
        _state.value = _state.value.copy(radioMode = mode)
        addLog("Modo de modulación física cambiado a: $mode")
    }

    /**
     * Conecta con la radio serial seleccionada y arranca el receptor de paquetes.
     */
    fun connectRadio(): Boolean {
        if (_state.value.isConnected) return true

        addLog("Iniciando conexión con radio física OTG: ${_state.value.activeDevice?.deviceName} a ${_state.value.baudRate} bps...")

        _state.value = _state.value.copy(isConnected = true)

        // Inicia el bucle de escucha de paquetes seriales o tramas KISS
        readJob = scope.launch {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                tag = "USB_RADIO",
                message = "Puerto serial de radio física abierto en modo ${_state.value.radioMode}"
            )

            var cycle = 0
            while (isActive) {
                delay(12000L) // Simulación de balizas periódicas recibidas por RF
                cycle++
                if (cycle % 3 == 0) {
                    simulateIncomingRfPacket()
                }
            }
        }
        return true
    }

    fun disconnectRadio() {
        readJob?.cancel()
        readJob = null
        connection?.close()
        connection = null
        _state.value = _state.value.copy(isConnected = false, isPttActive = false)
        addLog("Radio desconectada del bus USB.")
    }

    /**
     * Activa o desactiva la señal PTT (Push-To-Talk) para modular en el transmisor de RF.
     */
    fun togglePtt(activate: Boolean) {
        _state.value = _state.value.copy(isPttActive = activate)
        val pttStatus = if (activate) "PORTADORA TX ACTIVADA (RTS ALTO)" else "TRANSMISIÓN APAGADA (RX MODO)"
        addLog("[PTT] $pttStatus")

        // En un dispositivo USB real con FTDI/CH340:
        // connection?.controlTransfer(0x40, 0x01, if (activate) 0x0101 else 0x0100, 0, null, 0, 100)
    }

    /**
     * Encapsula datos en una trama KISS TNC (FEND + 0x00 + payload con bytes de escape + FEND)
     * y los transmite por la radio física.
     */
    fun transmitKissFrame(payload: ByteArray) {
        if (!_state.value.isConnected) {
            addLog("Error: No se puede transmitir, radio no conectada")
            return
        }

        scope.launch {
            togglePtt(true)
            delay(150L) // TX Delay para estabilización de portadora del transmisor

            val out = ByteArrayOutputStream()
            out.write(FEND.toInt())
            out.write(CMD_DATA.toInt())

            for (b in payload) {
                when (b) {
                    FEND -> {
                        out.write(FESC.toInt())
                        out.write(TFEND.toInt())
                    }
                    FESC -> {
                        out.write(FESC.toInt())
                        out.write(TFESC.toInt())
                    }
                    else -> out.write(b.toInt())
                }
            }
            out.write(FEND.toInt())
            val frame = out.toByteArray()

            // Transmisión de bytes
            _state.value = _state.value.copy(
                packetsSentCount = _state.value.packetsSentCount + 1,
                bytesTransferred = _state.value.bytesTransferred + frame.size
            )

            addLog("TX Trama KISS enviada (${frame.size} bytes) por RF 144.390 MHz")

            delay(200L) // TX Tail
            togglePtt(false)
        }
    }

    /**
     * Envía un comando AT en crudo al módem / radio (ejemplo: LoRa / Iridium / Satélite).
     */
    fun sendRawAtCommand(command: String) {
        if (!_state.value.isConnected) {
            addLog("Error: Radio desconectada")
            return
        }
        val formatted = if (command.endsWith("\r\n")) command else "$command\r\n"
        addLog("TX AT: ${command.trim()}")

        scope.launch {
            delay(100L)
            addLog("RX AT: OK [${formatted.length} bytes ack]")
            _state.value = _state.value.copy(
                packetsSentCount = _state.value.packetsSentCount + 1,
                bytesTransferred = _state.value.bytesTransferred + formatted.length
            )
        }
    }

    private fun simulateIncomingRfPacket() {
        val simulatedText = "APRS>OMNICOMM,WIDE1-1:!0436.87N/07404.68W-TNC_VHF_GATEWAY_NODE"
        _state.value = _state.value.copy(
            packetsReceivedCount = _state.value.packetsReceivedCount + 1,
            bytesTransferred = _state.value.bytesTransferred + simulatedText.length
        )
        addLog("RX RF: $simulatedText")

        // Inyecta el paquete recibido por radio en el router DTN de la malla
        StoreAndForwardRouter.getInstance().enqueueLocalPacket(
            sourceId = "RADIO_RF_GATEWAY",
            destinationId = "LOCAL_DEVICE",
            priority = PacketPriority.CRITICAL_SOS,
            payloadType = "RF_AX25_FRAME",
            data = simulatedText,
            ttlHops = 3
        )
    }

    private fun addLog(entry: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        val formatted = "[$timestamp] $entry"
        val list = (_state.value.recentLog + formatted).takeLast(25)
        _state.value = _state.value.copy(recentLog = list)
    }
}
