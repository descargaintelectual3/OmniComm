package com.example.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OmniPushService : FirebaseMessagingService() {

    companion object {
        const val TAG = "OmniPushService"
        const val CHANNEL_CHAT_ID = "channel_omni_chat_messages"
        const val CHANNEL_CONTACTS_ID = "channel_omni_contact_requests"
        const val CHANNEL_ALERTS_ID = "channel_omni_tactical_alerts"

        /**
         * Inicializa los canales de notificación en Android 8.0+
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val chatChannel = NotificationChannel(
                    CHANNEL_CHAT_ID,
                    "Mensajes de Chat E2EE",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas en tiempo real para mensajes cifrados entrantes"
                    enableVibration(true)
                    setShowBadge(true)
                }

                val contactsChannel = NotificationChannel(
                    CHANNEL_CONTACTS_ID,
                    "Solicitudes de Contacto y Nodos",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alertas para nuevas solicitudes de enlace y nodos agregados"
                    enableVibration(true)
                    setShowBadge(true)
                }

                val alertsChannel = NotificationChannel(
                    CHANNEL_ALERTS_ID,
                    "Alertas Tácticas y Red",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas críticas de estado de red y batería"
                    enableVibration(true)
                    setShowBadge(true)
                }

                notificationManager.createNotificationChannel(chatChannel)
                notificationManager.createNotificationChannel(contactsChannel)
                notificationManager.createNotificationChannel(alertsChannel)
            }
        }

        /**
         * Registra y sincroniza el token FCM actual en Firebase
         */
        fun registerCurrentToken(userUid: String? = null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Error obteniendo token FCM", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d(TAG, "FCM Token obtenido: $token")
                syncTokenToCloud(token, userUid)
            }
        }

        /**
         * Sincroniza el token FCM en Firestore y Realtime Database para el usuario autenticado
         */
        fun syncTokenToCloud(token: String, explicitUid: String? = null) {
            val uid = explicitUid ?: FirebaseAuth.getInstance().currentUser?.uid ?: return
            if (token.isBlank() || uid.isBlank()) return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Guardar en Firestore
                    FirebaseFirestore.getInstance().collection("users")
                        .document(uid)
                        .set(
                            mapOf(
                                "fcmToken" to token,
                                "lastTokenUpdated" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )

                    // 2. Guardar en Realtime Database
                    try {
                        FirebaseDatabase.getInstance().getReference("status/$uid/fcmToken")
                            .setValue(token)
                    } catch (e: Exception) {
                        Log.w(TAG, "No se pudo actualizar token en RTDB: ${e.message}")
                    }

                    Log.d(TAG, "Token FCM sincronizado con éxito para UID: $uid")
                } catch (e: Exception) {
                    Log.w(TAG, "Fallo al sincronizar token FCM: ${e.message}")
                }
            }
        }

        /**
         * Muestra una notificación local inmediata (útil cuando se detecta un mensaje o solicitud en segundo plano)
         */
        fun showLocalNotification(
            context: Context,
            title: String,
            body: String,
            channelId: String = CHANNEL_CHAT_ID,
            notificationId: Int = (System.currentTimeMillis() % 10000).toInt(),
            sessionId: String? = null,
            targetScreen: String? = null
        ) {
            createNotificationChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (!sessionId.isNullOrBlank()) {
                    putExtra("open_session_id", sessionId)
                }
                if (!targetScreen.isNullOrBlank()) {
                    putExtra("open_screen", targetScreen)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo FCM Token recibido: $token")
        syncTokenToCloud(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Mensaje FCM recibido de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: "CHAT_MESSAGE"
        val title = notification?.title ?: data["title"] ?: "OmniComm Táctico"
        val body = notification?.body ?: data["body"] ?: "Nuevo mensaje recibido"
        val sessionId = data["sessionId"]
        val targetScreen = data["targetScreen"]

        val channelId = when (type) {
            "CONTACT_REQUEST" -> CHANNEL_CONTACTS_ID
            "TACTICAL_ALERT" -> CHANNEL_ALERTS_ID
            else -> CHANNEL_CHAT_ID
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        showLocalNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = channelId,
            notificationId = notificationId,
            sessionId = sessionId,
            targetScreen = targetScreen
        )
    }
}

