package com.example.domain.local

import android.content.Context
import android.util.Log
import com.example.domain.hardware.BluetoothMeshService
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.sync.MessageSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sistema de Cola de Mensajes (Message Queue System).
 * Actúa como un daemon offline que revisa constantemente la base de datos Room/SQLCipher
 * en busca de mensajes "PENDING" e intenta despacharlos cuando los nodos
 * Bluetooth regresan al rango de alcance.
 */
class OfflineMessageQueue(
    private val context: Context,
    private val meshService: BluetoothMeshService = BluetoothMeshService.getInstance(context)
) {
    
    private val database = OmniDatabase.getDatabase(context)
    private val chatDao = database.chatDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isQueueRunning = false

    fun startQueueWorker() {
        if (isQueueRunning) return
        isQueueRunning = true
        
        // Programa sincronización periódica de bajo consumo con WorkManager
        MessageSyncWorker.schedulePeriodicSync(context)

        scope.launch {
            Log.d("MessageQueue", "Iniciando worker de retención y retransmisión de mensajes (Offline Queue)")
            while (isActive && isQueueRunning) {
                try {
                    val pendingMessages = chatDao.getPendingMessages()
                    if (pendingMessages.isNotEmpty()) {
                        Log.d("MessageQueue", "Se encontraron ${pendingMessages.size} mensajes PENDING. Intentando transmisión...")
                        
                        for (msg in pendingMessages) {
                            val payload = "${msg.senderName}:${msg.text}"
                            val success = meshService.sendMessageOffline(payload)
                            
                            if (success || meshService.isConnectedToAnyNode()) {
                                chatDao.updateMessage(msg.copy(status = "SENT"))
                                Log.d("MessageQueue", "Mensaje ${msg.id} retransmitido y marcado como SENT.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MessageQueue", "Error procesando cola de mensajes", e)
                }
                
                delay(5000)
            }
        }
    }

    fun stopQueueWorker() {
        isQueueRunning = false
        Log.d("MessageQueue", "Worker de retransmisión detenido.")
    }

    /**
     * Encola un nuevo mensaje. Lo guarda en Room como PENDING y dispara la verificación de envío.
     */
    suspend fun enqueueMessage(message: ChatMessageEntity) {
        val pendingMsg = message.copy(status = "PENDING")
        chatDao.insertMessage(pendingMsg)
        Log.d("MessageQueue", "Mensaje encolado en BD Local con status PENDING: ${pendingMsg.id}")

        // Intento directo inmediato si hay conexión
        if (meshService.isConnectedToAnyNode()) {
            val payload = "${pendingMsg.senderName}:${pendingMsg.text}"
            if (meshService.sendMessageOffline(payload)) {
                chatDao.updateMessage(pendingMsg.copy(status = "SENT"))
            }
        } else {
            // Disparar sincronización de WorkManager respetando la batería
            MessageSyncWorker.triggerExpeditedSync(context)
        }
    }
}

