package com.example.domain.logging

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LogCategory(val label: String, val icon: String) {
    ALL("Todos", "🌐"),
    AUTH_HANDSHAKE("Auth Handshake", "🔐"),
    FIRESTORE_PRESENCE("Firestore Presence", "☁️"),
    RTDB_PRESENCE("RTDB Status", "⚡"),
    WEBHOOK_SIGNAL("Webhooks & Signals", "📡"),
    LAN_BEACON("LAN UDP Beacon", "📻"),
    BLUETOOTH_MESH("Bluetooth Mesh", "📶"),
    SYSTEM("Sistema", "⚙️")
}

enum class LogSeverity(val label: String) {
    INFO("INFO"),
    SUCCESS("SUCCESS"),
    WARNING("WARN"),
    ERROR("ERROR"),
    PROTOCOL("PROTO")
}

data class DiscoveryLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: LogCategory,
    val severity: LogSeverity,
    val tag: String,
    val message: String,
    val rawPayload: String? = null,
    val durationMs: Long? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

data class DiscoveryMetrics(
    val authHandshakeCount: Int = 0,
    val firestoreEventsCount: Int = 0,
    val rtdbEventsCount: Int = 0,
    val webhookSignalsCount: Int = 0,
    val lanBeaconsCount: Int = 0,
    val lastAuthStatus: String = "No iniciado",
    val isRtdbConnected: Boolean = false,
    val activePeersDiscovered: Int = 0,
    val lastWebhookLatencyMs: Long = 0L
)

/**
 * Colector centralizado de logs y telemetría en tiempo real para el flujo
 * de descubrimiento de pares, handshake de Firebase Auth, presencia y webhooks.
 */
object DiscoveryLogCollector {
    private const val TAG = "DiscoveryLogCollector"
    private const val MAX_LOGS_CAPACITY = 800

    private val _logs = MutableStateFlow<List<DiscoveryLogEntry>>(emptyList())
    val logs: StateFlow<List<DiscoveryLogEntry>> = _logs.asStateFlow()

    private val _metrics = MutableStateFlow(DiscoveryMetrics())
    val metrics: StateFlow<DiscoveryMetrics> = _metrics.asStateFlow()

    init {
        log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "CollectorInit",
            message = "🟢 Monitor de Auto-Descubrimiento y Handshake Táctico inicializado."
        )
    }

    fun log(
        category: LogCategory,
        severity: LogSeverity,
        tag: String,
        message: String,
        rawPayload: String? = null,
        durationMs: Long? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        val entry = DiscoveryLogEntry(
            category = category,
            severity = severity,
            tag = tag,
            message = message,
            rawPayload = rawPayload,
            durationMs = durationMs,
            metadata = metadata
        )

        // Logcat output
        val logLine = "[${entry.category.label}] [${entry.severity.label}] ${entry.tag}: $message"
        when (severity) {
            LogSeverity.ERROR -> Log.e(TAG, logLine)
            LogSeverity.WARNING -> Log.w(TAG, logLine)
            else -> Log.d(TAG, logLine)
        }

        synchronized(this) {
            val currentList = _logs.value.toMutableList()
            if (currentList.size >= MAX_LOGS_CAPACITY) {
                currentList.removeAt(0)
            }
            currentList.add(entry)
            _logs.value = currentList

            // Actualizar métricas
            val currentMetrics = _metrics.value
            _metrics.value = when (category) {
                LogCategory.AUTH_HANDSHAKE -> currentMetrics.copy(
                    authHandshakeCount = currentMetrics.authHandshakeCount + 1,
                    lastAuthStatus = if (severity == LogSeverity.SUCCESS || severity == LogSeverity.INFO) "Activo / Autenticado" else "Error"
                )
                LogCategory.FIRESTORE_PRESENCE -> currentMetrics.copy(
                    firestoreEventsCount = currentMetrics.firestoreEventsCount + 1
                )
                LogCategory.RTDB_PRESENCE -> currentMetrics.copy(
                    rtdbEventsCount = currentMetrics.rtdbEventsCount + 1,
                    isRtdbConnected = if (message.contains("online", ignoreCase = true) || message.contains("true", ignoreCase = true)) true else currentMetrics.isRtdbConnected
                )
                LogCategory.WEBHOOK_SIGNAL -> currentMetrics.copy(
                    webhookSignalsCount = currentMetrics.webhookSignalsCount + 1,
                    lastWebhookLatencyMs = durationMs ?: currentMetrics.lastWebhookLatencyMs
                )
                LogCategory.LAN_BEACON -> currentMetrics.copy(
                    lanBeaconsCount = currentMetrics.lanBeaconsCount + 1
                )
                else -> currentMetrics
            }
        }
    }

    fun logAuthHandshake(
        step: String,
        message: String,
        payload: String? = null,
        severity: LogSeverity = LogSeverity.INFO,
        durationMs: Long? = null
    ) {
        log(
            category = LogCategory.AUTH_HANDSHAKE,
            severity = severity,
            tag = step,
            message = message,
            rawPayload = payload,
            durationMs = durationMs
        )
    }

    fun logFirestorePresence(
        action: String,
        message: String,
        payload: String? = null,
        severity: LogSeverity = LogSeverity.INFO
    ) {
        log(
            category = LogCategory.FIRESTORE_PRESENCE,
            severity = severity,
            tag = action,
            message = message,
            rawPayload = payload
        )
    }

    fun logRtdbPresence(
        action: String,
        message: String,
        payload: String? = null,
        severity: LogSeverity = LogSeverity.INFO
    ) {
        log(
            category = LogCategory.RTDB_PRESENCE,
            severity = severity,
            tag = action,
            message = message,
            rawPayload = payload
        )
    }

    fun logWebhookSignal(
        signalType: String,
        status: String,
        payload: String? = null,
        severity: LogSeverity = LogSeverity.INFO,
        durationMs: Long? = null
    ) {
        log(
            category = LogCategory.WEBHOOK_SIGNAL,
            severity = severity,
            tag = signalType,
            message = status,
            rawPayload = payload,
            durationMs = durationMs
        )
    }

    fun logLanBeacon(
        action: String,
        message: String,
        payload: String? = null,
        severity: LogSeverity = LogSeverity.INFO
    ) {
        log(
            category = LogCategory.LAN_BEACON,
            severity = severity,
            tag = action,
            message = message,
            rawPayload = payload
        )
    }

    fun logBluetoothMesh(
        action: String,
        message: String,
        payload: String? = null,
        severity: LogSeverity = LogSeverity.INFO
    ) {
        log(
            category = LogCategory.BLUETOOTH_MESH,
            severity = severity,
            tag = action,
            message = message,
            rawPayload = payload
        )
    }

    fun updateDiscoveredPeersCount(count: Int) {
        _metrics.value = _metrics.value.copy(activePeersDiscovered = count)
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "ClearLogs",
            message = "🧹 Registro de logs reiniciado por el usuario."
        )
    }

    fun getLogsAsFormattedText(): String {
        val sb = StringBuilder()
        sb.append("=== OMNICOMM TACTICAL PEER DISCOVERY LOGS ===\n")
        sb.append("Export Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
        for (entry in _logs.value) {
            sb.append("[${entry.formattedTime}] [${entry.category.label}] [${entry.severity.label}] ${entry.tag}: ${entry.message}\n")
            if (!entry.rawPayload.isNullOrBlank()) {
                sb.append("  ↳ Payload: ${entry.rawPayload}\n")
            }
            if (entry.durationMs != null) {
                sb.append("  ↳ Latencia: ${entry.durationMs} ms\n")
            }
        }
        return sb.toString()
    }
}
