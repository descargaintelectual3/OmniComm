package com.example.domain.models

import java.util.UUID

/**
 * Representa una tarea de sincronización activa de la malla.
 */
data class ActiveSyncTask(
    val taskId: String = UUID.randomUUID().toString().take(8),
    val taskName: String,
    val targetNodeId: String,
    val targetNodeName: String,
    val progressPercent: Int,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val syncType: SyncTaskType,
    val isEncrypted: Boolean = true
)

enum class SyncTaskType(val label: String, val iconEmoji: String) {
    SQLCIPHER_VAULT("Bóveda E2EE", "🔐"),
    SHARED_FILES("Archivos Mesh", "📁"),
    IDENTITY_TOKENS("Tokens de Seguridad", "🛡️"),
    TELEMETRY_LOGS("Telemetría Radar", "📡"),
    REMOTE_CONTROL_STATE("Estado Espejo / Control", "🎮")
}

/**
 * Comandos de Control Remoto Total Universal para manejar cualquier nodo
 * incluso con pantalla rota o física dañada.
 */
data class RemoteControlCommand(
    val commandId: String = UUID.randomUUID().toString(),
    val targetNodeId: String,
    val actionType: RemoteActionType,
    val payload: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class RemoteActionType(val label: String, val description: String) {
    AUDIO_ALARM_BEACON("Emitir Alarma Sonora", "Reproduce sonido baliza a máximo volumen para ubicar el terminal"),
    TRIGGER_TORCH("Linterna Táctica SOS", "Enciende flash LED intermitente en modo rescate"),
    CAPTURE_REMOTE_PHOTO("Capturar Foto Remota", "Toma foto con sensor trasero/frontal y la envía por Wi-Fi Direct"),
    TEXT_TO_SPEECH_ALERT("Voz Sintetizada", "Lee mensaje de audio de alta prioridad en el dispositivo"),
    DUMP_VAULT_BACKUP("Rescatar Bóveda de Datos", "Extrae todos los archivos y chats hacia este terminal"),
    REBOOT_RECOVERY("Reinicio Seguro de Red", "Reinicia subsistema de radiofrecuencia y reconecta"),
    TOGGLE_SCREEN_CAST("Modo Espejo Pantalla", "Transmite flujo de pantalla táctico en tiempo real")
}

/**
 * Notificación táctica no intrusiva de la aplicación.
 */
data class TacticalToastNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val avatarIcon: String = "🛡️",
    val type: ToastType = ToastType.WELCOME_HANDSHAKE,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ToastType {
    WELCOME_HANDSHAKE,
    PRIORITY_DEVICE_LINKED,
    SELF_HEALING_ACTIVATED,
    SYNC_ALL_COMPLETE,
    REMOTE_CONTROL_EXECUTED
}
