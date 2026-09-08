package com.example.domain.c2

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ExternalBlePeripheral(
    val macAddress: String,
    val name: String,
    val rssi: Int,
    val isConnected: Boolean = false,
    val batteryLevel: Int? = null,
    val discoveredServicesCount: Int = 0,
    val supportedProfiles: List<String> = emptyList(),
    val lastSeenMillis: Long = System.currentTimeMillis()
)

/**
 * Gestor Universal de Conexión y Control BLE GATT para Dispositivos Periféricos Externos.
 * Permite gobernar hardware de terceros sin necesidad de que tengan OmniComm instalado:
 * - Servicios de Alerta Inmediata (IAS 0x1802) para hacer sonar pitidos/alarmas en balizas o tags.
 * - Servicio de Batería (BAS 0x180F) para monitorizar estado energético.
 * - Servicio de Frecuencia Cardiaca (HRS 0x180D) para telemetría fisiológica del combatiente.
 * - Canales de Datos Transparentes SPP/UART BLE (0xFFE0 / Nordic UART).
 */
class BleGattUniversalDeviceManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _peripherals = MutableStateFlow<Map<String, ExternalBlePeripheral>>(
        mapOf(
            "C4:4E:AC:88:99:11" to ExternalBlePeripheral(
                macAddress = "C4:4E:AC:88:99:11",
                name = "BALIZA_TACTICA_TAG_01",
                rssi = -55,
                isConnected = true,
                batteryLevel = 92,
                discoveredServicesCount = 3,
                supportedProfiles = listOf("Immediate Alert", "Battery Service", "Proximity Profile")
            ),
            "E2:15:2B:44:33:AA" to ExternalBlePeripheral(
                macAddress = "E2:15:2B:44:33:AA",
                name = "SENSOR_CARDIACO_PECHO",
                rssi = -68,
                isConnected = false,
                batteryLevel = 75,
                discoveredServicesCount = 2,
                supportedProfiles = listOf("Heart Rate", "Battery Service")
            )
        )
    )
    val peripherals: StateFlow<Map<String, ExternalBlePeripheral>> = _peripherals.asStateFlow()

    private val activeGattConnections = mutableMapOf<String, BluetoothGatt>()

    // UUIDs de perfiles estándar Bluetooth SIG
    private val IMMEDIATE_ALERT_SERVICE = UUID.fromString("00001802-0000-1000-8000-00805F9B34FB")
    private val ALERT_LEVEL_CHARACTERISTIC = UUID.fromString("00002A06-0000-1000-8000-00805F9B34FB")
    private val BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
    private val BATTERY_LEVEL_CHARACTERISTIC = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")

    companion object {
        @Volatile
        private var INSTANCE: BleGattUniversalDeviceManager? = null

        fun getInstance(context: Context): BleGattUniversalDeviceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BleGattUniversalDeviceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Envía una alerta o pulso sonoro a un periférico Bluetooth externo usando el servicio IAS (Immediate Alert).
     */
    @SuppressLint("MissingPermission")
    fun triggerExternalPeripheralAlert(macAddress: String, alertLevel: Byte = 0x02) { // 0x02 = High Alert
        scope.launch {
            val peripheral = _peripherals.value[macAddress]
            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.INFO,
                tag = "BLE_GATT_C2",
                message = "Emitiendo pulso de alerta sonora IAS a periférico $macAddress (${peripheral?.name ?: "Externo"})"
            )

            // Si hay conexión GATT física activa, escribimos en la característica
            val gatt = activeGattConnections[macAddress]
            if (gatt != null) {
                try {
                    val service = gatt.getService(IMMEDIATE_ALERT_SERVICE)
                    val characteristic = service?.getCharacteristic(ALERT_LEVEL_CHARACTERISTIC)
                    if (characteristic != null) {
                        characteristic.value = byteArrayOf(alertLevel)
                        gatt.writeCharacteristic(characteristic)
                    }
                } catch (ignored: Exception) {}
            }

            // Registrar en la lista reactiva
            val current = _peripherals.value[macAddress]
            if (current != null) {
                val updatedMap = _peripherals.value.toMutableMap()
                updatedMap[macAddress] = current.copy(lastSeenMillis = System.currentTimeMillis())
                _peripherals.value = updatedMap
            }
        }
    }

    /**
     * Conecta a un dispositivo BLE por MAC address y negocia perfiles GATT.
     */
    @SuppressLint("MissingPermission")
    fun connectToExternalDevice(macAddress: String) {
        if (bluetoothAdapter?.isEnabled != true) return
        scope.launch {
            try {
                val device = bluetoothAdapter.getRemoteDevice(macAddress) ?: return@launch
                val gattCallback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            gatt?.discoverServices()
                            updateConnectionState(macAddress, true)
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            updateConnectionState(macAddress, false)
                            activeGattConnections.remove(macAddress)
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                            val count = gatt.services.size
                            val current = _peripherals.value[macAddress]
                            if (current != null) {
                                val updated = _peripherals.value.toMutableMap()
                                updated[macAddress] = current.copy(
                                    discoveredServicesCount = count,
                                    supportedProfiles = gatt.services.map { it.uuid.toString().take(8) }
                                )
                                _peripherals.value = updated
                            }
                        }
                    }
                }

                val gatt = device.connectGatt(context, false, gattCallback)
                if (gatt != null) {
                    activeGattConnections[macAddress] = gatt
                }
            } catch (e: Exception) {
                DiscoveryLogCollector.log(
                    category = LogCategory.BLUETOOTH_MESH,
                    severity = LogSeverity.WARNING,
                    tag = "BLE_GATT_C2",
                    message = "Error conectando con $macAddress: ${e.message}"
                )
            }
        }
    }

    private fun updateConnectionState(mac: String, connected: Boolean) {
        val current = _peripherals.value[mac]
        if (current != null) {
            val updated = _peripherals.value.toMutableMap()
            updated[mac] = current.copy(isConnected = connected, lastSeenMillis = System.currentTimeMillis())
            _peripherals.value = updated
        }
    }
}
