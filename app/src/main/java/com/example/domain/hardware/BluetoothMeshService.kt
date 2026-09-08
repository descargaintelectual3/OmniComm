package com.example.domain.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.ChatMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

data class MeshConnectionState(
    val isConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val connectedDeviceAddress: String? = null,
    val activePeersCount: Int = 0,
    val isScanning: Boolean = false,
    val statusLabel: String = "Desconectado de la Malla"
)

data class DiscoveredPeer(
    val address: String,
    val name: String,
    val isConnected: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/**
 * Servicio de Malla Bluetooth P2P y Enrutamiento Automático de Nodos.
 * Incorpora un protocolo de saludo automático (Handshake de Bienvenida) en cuanto
 * un dispositivo se une a la red, transmitiendo su identificación a todos los pares cercanos.
 */
@SuppressLint("MissingPermission")
class BluetoothMeshService private constructor(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val OMNI_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val APP_NAME = "OmniCommMesh"

    private var serverSocket: BluetoothServerSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow(MeshConnectionState())
    val connectionState: StateFlow<MeshConnectionState> = _connectionState.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val localDeviceName: String
        get() = bluetoothAdapter?.name ?: "Nodo-${Build.MODEL}"

    private val localDeviceId: String = UUID.randomUUID().toString().take(8)

    init {
        registerBluetoothDiscoveryReceiver()
        startMeshNodeAutoDaemon()
    }

    /**
     * Inicia el ciclo táctico de red: servidor pasivo + descubrimiento activo continuo.
     */
    fun startMeshNodeAutoDaemon() {
        startListeningForPeers()
        startPeriodicPeerDiscovery()
    }

    private fun registerBluetoothDiscoveryReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }
                            device?.let { handleDiscoveredDevice(it) }
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                            _connectionState.value = _connectionState.value.copy(
                                isScanning = true,
                                statusLabel = "Escaneando Nodos Malla P2P..."
                            )
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            _connectionState.value = _connectionState.value.copy(
                                isScanning = false,
                                statusLabel = if (_connectionState.value.isConnected) "Malla Conectada" else "En Espera / Modo Cola"
                            )
                        }
                    }
                }
            }, filter)
        } catch (e: Exception) {
            Log.e("BluetoothMesh", "Error registrando broadcast receiver de Bluetooth", e)
        }
    }

    private fun handleDiscoveredDevice(device: BluetoothDevice) {
        val name = try { device.name ?: "Nodo Cercano (${device.address.takeLast(5)})" } catch (e: Exception) { "Nodo ${device.address}" }
        val peer = DiscoveredPeer(
            address = device.address,
            name = name,
            isConnected = connectedSocket?.remoteDevice?.address == device.address
        )

        val current = _discoveredPeers.value.toMutableList()
        val index = current.indexOfFirst { it.address == device.address }
        if (index >= 0) {
            current[index] = peer
        } else {
            current.add(peer)
            Log.d("BluetoothMesh", "📡 Nuevo nodo detectado en el espacio radioeléctrico: $name [${device.address}]")
            
            // Intento de conexión y saludo automático
            if (connectedSocket == null || connectedSocket?.isConnected != true) {
                connectToPeer(device)
            }
        }
        _discoveredPeers.value = current
        updateConnectionState()
    }

    fun startPeriodicPeerDiscovery() {
        scope.launch {
            while (true) {
                try {
                    if (bluetoothAdapter?.isEnabled == true && bluetoothAdapter.isDiscovering == false) {
                        bluetoothAdapter.startDiscovery()
                    }
                } catch (e: Exception) {
                    Log.w("BluetoothMesh", "No se pudo iniciar descubrimiento: ${e.message}")
                }
                delay(30000) // Escaneo de sondeo cada 30 segundos
            }
        }
    }

    fun startListeningForPeers() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = _connectionState.value.copy(statusLabel = "Bluetooth inactivo")
            return
        }

        scope.launch {
            try {
                serverSocket?.close()
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, OMNI_UUID)
                Log.d("BluetoothMesh", "🔵 Servidor RFCOMM escuchando conexiones entrantes...")

                while (true) {
                    val socket: BluetoothSocket? = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        null
                    }

                    socket?.also {
                        Log.d("BluetoothMesh", "✅ Conexión entrante aceptada de ${it.remoteDevice.address}")
                        manageConnectedSocket(it)
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothMesh", "Error en servidor RFCOMM", e)
            }
        }
    }

    fun connectToPeer(device: BluetoothDevice) {
        scope.launch {
            try {
                Log.d("BluetoothMesh", "Intentando enlazar con nodo: ${device.address}")
                val socket = device.createRfcommSocketToServiceRecord(OMNI_UUID)
                try { bluetoothAdapter?.cancelDiscovery() } catch (e: Exception) {}
                socket.connect()
                Log.d("BluetoothMesh", "🔵 Enlace P2P completado con éxito: ${device.name ?: device.address}")
                manageConnectedSocket(socket)
            } catch (e: IOException) {
                Log.w("BluetoothMesh", "No se pudo conectar al nodo ${device.address} (fuera de rango o ocupado)")
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        connectedSocket = socket
        updateConnectionState()

        // Protocolo de Saludo Automático al unirse a la red
        sendWelcomeHandshake()

        scope.launch {
            val inputStream = socket.inputStream
            val buffer = ByteArray(4096)
            var bytes: Int

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    if (bytes <= 0) break
                    val incomingString = String(buffer, 0, bytes)
                    handleIncomingPacket(incomingString, socket.remoteDevice)
                } catch (e: IOException) {
                    Log.w("BluetoothMesh", "Túnel Bluetooth interrumpido con ${socket.remoteDevice.address}")
                    break
                }
            }
            if (connectedSocket == socket) {
                connectedSocket = null
                updateConnectionState()
            }
        }
    }

    /**
     * Envía el saludo inicial ("Handshake de Bienvenida") a la red.
     */
    fun sendWelcomeHandshake() {
        scope.launch {
            try {
                val handshakeJson = JSONObject().apply {
                    put("type", "OMNI_WELCOME_HELLO")
                    put("nodeId", localDeviceId)
                    put("nodeName", localDeviceName)
                    put("timestamp", System.currentTimeMillis())
                    put("text", "👋 ¡Hola a la Malla! $localDeviceName se ha unido y está listo para comunicarse.")
                }
                sendMessageOffline(handshakeJson.toString())
                Log.d("BluetoothMesh", "🤝 Handshake de Bienvenida emitido: $handshakeJson")
            } catch (e: Exception) {
                Log.e("BluetoothMesh", "Error emitiendo saludo", e)
            }
        }
    }

    private fun handleIncomingPacket(rawMessage: String, remoteDevice: BluetoothDevice) {
        Log.d("BluetoothMesh", "📩 Paquete crudo recibido: $rawMessage")
        scope.launch {
            try {
                if (rawMessage.startsWith("{") && rawMessage.contains("\"type\"")) {
                    val json = JSONObject(rawMessage)
                    val type = json.optString("type")
                    val senderName = json.optString("nodeName", remoteDevice.name ?: "Nodo Remoto")
                    val text = json.optString("text", "")

                    if (type == "OMNI_WELCOME_HELLO") {
                        // Respondemos con ACK de bienvenida
                        val ackJson = JSONObject().apply {
                            put("type", "OMNI_WELCOME_ACK")
                            put("nodeId", localDeviceId)
                            put("nodeName", localDeviceName)
                            put("text", "🤝 Saludos $senderName, conexión bidireccional confirmada.")
                        }
                        sendMessageOffline(ackJson.toString())

                        // Insertamos mensaje en la base de datos cifrada SQLCipher
                        val db = OmniDatabase.getDatabase(context)
                        db.chatDao().insertMessage(
                            ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                senderId = json.optString("nodeId", remoteDevice.address),
                                senderName = senderName,
                                text = text.ifEmpty { "👋 $senderName se ha unido a la red." },
                                timestamp = System.currentTimeMillis(),
                                status = "SENT"
                            )
                        )
                        return@launch
                    } else if (type == "OMNI_WELCOME_ACK") {
                        val db = OmniDatabase.getDatabase(context)
                        db.chatDao().insertMessage(
                            ChatMessageEntity(
                                id = UUID.randomUUID().toString(),
                                senderId = json.optString("nodeId", remoteDevice.address),
                                senderName = senderName,
                                text = text,
                                timestamp = System.currentTimeMillis(),
                                status = "SENT"
                            )
                        )
                        return@launch
                    }
                }

                // Mensaje regular en formato 'Sender:Text' o texto plano
                val parts = rawMessage.split(":", limit = 2)
                val sender = if (parts.size == 2) parts[0] else (remoteDevice.name ?: "Nodo Malla")
                val content = if (parts.size == 2) parts[1] else rawMessage

                val db = OmniDatabase.getDatabase(context)
                db.chatDao().insertMessage(
                    ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = remoteDevice.address,
                        senderName = sender,
                        text = content,
                        timestamp = System.currentTimeMillis(),
                        status = "SENT"
                    )
                )
            } catch (e: Exception) {
                Log.e("BluetoothMesh", "Error procesando paquete entrante", e)
            }
        }
    }

    private fun updateConnectionState() {
        val socket = connectedSocket
        val isConn = socket?.isConnected == true
        val devName = if (isConn) {
            try { socket?.remoteDevice?.name ?: socket?.remoteDevice?.address } catch (e: Exception) { "Nodo Conectado" }
        } else null

        val peers = _discoveredPeers.value
        _connectionState.value = MeshConnectionState(
            isConnected = isConn,
            connectedDeviceName = devName,
            connectedDeviceAddress = socket?.remoteDevice?.address,
            activePeersCount = if (isConn) 1 + peers.count { it.isConnected } else peers.size,
            isScanning = bluetoothAdapter?.isDiscovering == true,
            statusLabel = if (isConn) "🟢 Malla P2P Activa: $devName" else "🟡 Modo Offline (Encolando en Room)"
        )
    }

    fun isConnectedToAnyNode(): Boolean {
        return connectedSocket?.isConnected == true
    }

    fun sendMessageOffline(message: String): Boolean {
        return try {
            if (connectedSocket?.isConnected == true) {
                connectedSocket?.outputStream?.write(message.toByteArray())
                Log.d("BluetoothMesh", "📤 Paquete transmitido a través del canal RFCOMM: $message")
                true
            } else {
                Log.d("BluetoothMesh", "⚠️ No hay conexión física activa. Mensaje permanece en cola.")
                false
            }
        } catch (e: IOException) {
            Log.e("BluetoothMesh", "Fallo de I/O al transmitir paquete", e)
            false
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BluetoothMeshService? = null

        fun getInstance(context: Context): BluetoothMeshService {
            return INSTANCE ?: synchronized(this) {
                val instance = BluetoothMeshService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
