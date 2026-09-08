package com.example.domain.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.domain.hardware.BluetoothMeshService
import com.example.domain.local.OmniDatabase
import java.util.concurrent.TimeUnit

/**
 * Worker en segundo plano impulsado por Android WorkManager.
 * Gestiona reintentos de la cola de mensajes cifrados en Room con eficiencia energética,
 * garantizando que sólo se ejecuten cuando el dispositivo tiene batería suficiente
 * (evita el drenado excesivo de energía durante transmisiones tácticas).
 */
class MessageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "⚡ Ejecutando MessageSyncWorker con restricción de batería optimizada.")
        val database = OmniDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()

        return try {
            val pendingMessages = chatDao.getPendingMessages()
            if (pendingMessages.isEmpty()) {
                Log.d(TAG, "No hay mensajes pendientes de transmisión en la bóveda SQLCipher.")
                return Result.success()
            }

            Log.d(TAG, "Procesando ${pendingMessages.size} mensajes encolados...")
            val meshService = BluetoothMeshService.getInstance(applicationContext)

            for (msg in pendingMessages) {
                val payload = "${msg.senderName}:${msg.text}"
                val sent = meshService.sendMessageOffline(payload)
                if (sent || meshService.isConnectedToAnyNode()) {
                    chatDao.updateMessage(msg.copy(status = "SENT"))
                    Log.d(TAG, "Mensaje ${msg.id} transmitido exitosamente por WorkManager.")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la sincronización de mensajes en segundo plano", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "MessageSyncWorker"
        private const val PERIODIC_WORK_NAME = "omnicomm_periodic_message_sync"
        private const val EXPEDITED_WORK_NAME = "omnicomm_expedited_message_sync"

        /**
         * Programa sincronizaciones periódicas asegurando que la batería no esté baja.
         */
        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val periodicRequest = PeriodicWorkRequestBuilder<MessageSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
                Log.d(TAG, "Sincronización periódica de WorkManager programada (BatteryNotLow = true).")
            } catch (e: Throwable) {
                Log.w(TAG, "WorkManager no disponible en este entorno de ejecución: ${e.message}")
            }
        }

        /**
         * Dispara una sincronización inmediata segura con comprobación de estado de batería.
         */
        fun triggerExpeditedSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val oneTimeRequest = OneTimeWorkRequestBuilder<MessageSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    EXPEDITED_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeRequest
                )
                Log.d(TAG, "Sincronización inmediata solicitada a WorkManager.")
            } catch (e: Throwable) {
                Log.w(TAG, "WorkManager no disponible en este entorno de ejecución: ${e.message}")
            }
        }
    }
}
