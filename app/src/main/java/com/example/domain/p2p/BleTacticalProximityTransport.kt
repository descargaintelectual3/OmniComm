package com.example.domain.p2p

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class BleDiscoveredPeer(
    val nodeId: String,
    val callsign: String,
    val rssi: Int,
    val batteryPercent: Int,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val distanceEstimatedMeters: Double = 1.0,
    val rawPayloadHex: String = ""
)

data class BleTransportStatus(
    val isBleSupported: Boolean = false,
    val isAdvertising: Boolean = false,
    val isScanning: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val localCallsign: String = "ALPHA-NODE",
    val packetsAdvertised: Long = 0L,
    val packetsScanned: Long = 0L,
    val activeNearbyPeersCount: Int = 0,
    val lastDiscoveredPeer: String = "Ninguno",
    val permissionGranted: Boolean = false
)

/**
 * Capa de Enlace Físico BLE / Proximidad Táctica Offline (Capa 4 & Capa 13).
 * Emite y recibe balizas tácticas compactas mediante Bluetooth Low Energy sin requerir Wi-Fi o datos celulares.
 */
class BleTacticalProximityTransport private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null

    private var advertiseCallback: AdvertiseCallback? = null
    private var scanCallback: ScanCallback? = null
    private var periodicBeaconJob: Job? = null

    private val TACTICAL_SERVICE_UUID = UUID.fromString("0000FEF0-0000-1000-8000-00805F9B34FB")

    private val _status = MutableStateFlow(
        BleTransportStatus(
            isBleSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
        )
    )
    val status: StateFlow<BleTransportStatus> = _status.asStateFlow()

    private val _peers = MutableStateFlow<Map<String, BleDiscoveredPeer>>(emptyMap())
    val peers: StateFlow<Map<String, BleDiscoveredPeer>> = _peers.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: BleTacticalProximityTransport? = null

        fun getInstance(context: Context): BleTacticalProximityTransport {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BleTacticalProximityTransport(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        _status.value = _status.value.copy(permissionGranted = hasPermissions)
        return hasPermissions
    }

    /**
     * Inicia la emisión periódica de balizas de presencia táctica BLE.
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising(localCallsign: String = "APOLLO-NODE", batteryPercent: Int = 88) {
        if (!checkPermissions() || bluetoothAdapter?.isEnabled != true) {
            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.WARNING,
                tag = "BLE_TRANSPORT",
                message = "No se puede iniciar BLE Advertising: Permisos o Bluetooth desactivado"
            )
            return
        }

        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(false)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        // Construir trama compacta: [0x54, 0x41] (TAG) + Battery (1B) + Callsign truncated (max 8B)
        val callsignBytes = localCallsign.take(8).toByteArray(Charsets.UTF_8)
        val payload = ByteBuffer.allocate(3 + callsignBytes.size)
            .put(0x54.toByte()) // 'T'
            .put(0x41.toByte()) // 'A'
            .put(batteryPercent.toByte())
            .put(callsignBytes)
            .array()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceData(ParcelUuid(TACTICAL_SERVICE_UUID), payload)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                _status.value = _status.value.copy(
                    isAdvertising = true,
                    localCallsign = localCallsign
                )
                DiscoveryLogCollector.log(
                    category = LogCategory.BLUETOOTH_MESH,
                    severity = LogSeverity.INFO,
                    tag = "BLE_TRANSPORT",
                    message = "Baliza táctica BLE activa ($localCallsign). Transmitiendo presencia local."
                )
            }

            override fun onStartFailure(errorCode: Int) {
                _status.value = _status.value.copy(isAdvertising = false)
                DiscoveryLogCollector.log(
                    category = LogCategory.BLUETOOTH_MESH,
                    severity = LogSeverity.WARNING,
                    tag = "BLE_TRANSPORT",
                    message = "Fallo al iniciar BLE Advertising (Código: $errorCode)"
                )
            }
        }

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {
            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.WARNING,
                tag = "BLE_TRANSPORT",
                message = "Excepción al iniciar advertising: ${e.message}"
            )
        }
    }

    /**
     * Detiene la emisión de balizas BLE.
     */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        try {
            if (advertiseCallback != null && advertiser != null) {
                advertiser?.stopAdvertising(advertiseCallback)
            }
        } catch (ignored: Exception) {}
        advertiseCallback = null
        _status.value = _status.value.copy(isAdvertising = false)
    }

    /**
     * Inicia el escáner BLE para interceptar balizas tácticas de otros terminales en rango.
     */
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (!checkPermissions() || bluetoothAdapter?.isEnabled != true) {
            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.WARNING,
                tag = "BLE_TRANSPORT",
                message = "Permisos de escaneo BLE insuficientes o Bluetooth inactivo"
            )
            return
        }

        scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) return

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceData(ParcelUuid(TACTICAL_SERVICE_UUID), byteArrayOf(0x54, 0x41), byteArrayOf(0xFF.toByte(), 0xFF.toByte()))
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                processScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { processScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                DiscoveryLogCollector.log(
                    category = LogCategory.BLUETOOTH_MESH,
                    severity = LogSeverity.WARNING,
                    tag = "BLE_TRANSPORT",
                    message = "Fallo en escaneo BLE (Código: $errorCode)"
                )
            }
        }

        try {
            scanner?.startScan(filters, settings, scanCallback)
            _status.value = _status.value.copy(isScanning = true)
            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.INFO,
                tag = "BLE_TRANSPORT",
                message = "Escáner táctico BLE iniciado. Escuchando balizas tácticas cercanas."
            )
        } catch (e: Exception) {
            DiscoveryLogCollector.log(
                category = LogCategory.BLUETOOTH_MESH,
                severity = LogSeverity.WARNING,
                tag = "BLE_TRANSPORT",
                message = "Excepción al iniciar escaneo BLE: ${e.message}"
            )
        }
    }

    private fun processScanResult(result: ScanResult) {
        val serviceData = result.scanRecord?.getServiceData(ParcelUuid(TACTICAL_SERVICE_UUID))
        val rssi = result.rssi
        val deviceAddress = result.device.address ?: "UNKNOWN_ADDR"

        var callsign = "NODO-$deviceAddress"
        var battery = 100

        if (serviceData != null && serviceData.size >= 3) {
            if (serviceData[0] == 0x54.toByte() && serviceData[1] == 0x41.toByte()) {
                battery = serviceData[2].toInt() and 0xFF
                if (serviceData.size > 3) {
                    val rawCallsign = String(serviceData, 3, serviceData.size - 3, Charsets.UTF_8).trim()
                    if (rawCallsign.isNotBlank()) callsign = rawCallsign
                }
            }
        }

        // Estimación simplificada de distancia por RSSI
        val estimatedDist = Math.pow(10.0, (-69.0 - rssi) / (10.0 * 2.5)).coerceIn(0.2, 80.0)

        val peer = BleDiscoveredPeer(
            nodeId = deviceAddress,
            callsign = callsign,
            rssi = rssi,
            batteryPercent = battery,
            lastSeenTimestamp = System.currentTimeMillis(),
            distanceEstimatedMeters = Math.round(estimatedDist * 10.0) / 10.0
        )

        val updatedMap = _peers.value.toMutableMap()
        updatedMap[deviceAddress] = peer
        _peers.value = updatedMap

        _status.value = _status.value.copy(
            packetsScanned = _status.value.packetsScanned + 1,
            activeNearbyPeersCount = updatedMap.size,
            lastDiscoveredPeer = callsign
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            if (scanCallback != null && scanner != null) {
                scanner?.stopScan(scanCallback)
            }
        } catch (ignored: Exception) {}
        scanCallback = null
        _status.value = _status.value.copy(isScanning = false)
    }
}
