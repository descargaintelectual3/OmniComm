package com.example.domain.p2p

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Convierte el dispositivo Android en un Servidor Activo (Nodo P2P).
 * Escucha conexiones entrantes de otros dispositivos en la red.
 */
class MeshNodeServer {
    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private val port = 8080 // Puerto P2P por defecto

    suspend fun startServer() = withContext(Dispatchers.IO) {
        if (isRunning.get()) return@withContext
        
        try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            Log.d("MeshNode", "🟢 Nodo Servidor INICIADO. Escuchando en el puerto $port")

            while (isRunning.get()) {
                // El dispositivo ahora actúa como un servidor esperando que otros nodos se conecten
                val clientSocket: Socket = serverSocket!!.accept()
                Log.d("MeshNode", "Conexión P2P entrante desde: ${clientSocket.inetAddress.hostAddress}")
                
                // Aquí derivaríamos el socket a un Hilo/Corrutina de procesamiento bidireccional
                handleIncomingNode(clientSocket)
            }
        } catch (e: Exception) {
            Log.e("MeshNode", "Error en el Servidor P2P: ${e.message}")
        }
    }

    private fun handleIncomingNode(socket: Socket) {
        // En una implementación completa, aquí leemos y escribimos bytes directamente
        // sincronizando la base de datos Room y los archivos sin pasar por internet.
        socket.close()
    }

    fun stopServer() {
        isRunning.set(false)
        serverSocket?.close()
        Log.d("MeshNode", "⚪ Nodo Servidor DETENIDO.")
    }
}
