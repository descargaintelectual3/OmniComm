package com.example.domain.c4isr

import android.content.Context
import com.example.domain.config.FeatureManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.models.GISMarkerType
import com.example.domain.models.TacticalGISMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Evento Cursor-on-Target (CoT) estructurado compatible con especificación MITRE / ATAK CoT 2.0.
 */
data class CotEvent(
    val uid: String,
    val type: String, // ej. "a-f-G-U-C" (Friendly Ground Unit), "b-m-p-s-p-loc" (Waypoint)
    val callsign: String,
    val lat: Double,
    val lon: Double,
    val haeMeters: Double = 0.0, // Height Above Ellipsoid
    val ceMeters: Double = 10.0, // Circular Error
    val leMeters: Double = 15.0, // Linear Error
    val timeMillis: Long = System.currentTimeMillis(),
    val staleMillis: Long = System.currentTimeMillis() + 60_000L,
    val how: String = "m-g", // Machine - GPS
    val remarks: String = "",
    val speedMps: Double = 0.0,
    val courseDeg: Double = 0.0
)

data class CotTransceiverStatus(
    val isBroadcastingActive: Boolean = false,
    val isListeningActive: Boolean = false,
    val broadcastPort: Int = 4242,
    val targetBroadcastIp: String = "255.255.255.255",
    val multicastGroupIp: String = "239.2.3.1",
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val lastDispatchedXml: String = "",
    val lastReceivedCallsign: String = "Ninguno",
    val activeAtakPeersCount: Int = 0
)

/**
 * Transcodificador e Interoperador Cursor-on-Target (CoT / ATAK) bidireccional sobre UDP.
 * Capa 11: Interoperabilidad Conjunta C4ISR / CoT.
 */
class CursorOnTargetTranscoder private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var broadcastJob: Job? = null
    private var listeningJob: Job? = null

    private val _status = MutableStateFlow(CotTransceiverStatus())
    val status: StateFlow<CotTransceiverStatus> = _status.asStateFlow()

    private val _receivedCotEvents = MutableStateFlow<List<CotEvent>>(emptyList())
    val receivedCotEvents: StateFlow<List<CotEvent>> = _receivedCotEvents.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    companion object {
        @Volatile
        private var INSTANCE: CursorOnTargetTranscoder? = null

        fun getInstance(context: Context): CursorOnTargetTranscoder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CursorOnTargetTranscoder(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Mapea un marcador táctico GIS a su tipo CoT estándar militar OTAN.
     */
    fun getCotTypeForGisMarker(type: GISMarkerType): String {
        return when (type) {
            GISMarkerType.RALLY_POINT -> "b-m-p-s-p-loc"
            GISMarkerType.HAZARD_ZONE -> "b-m-p-s-p-w"
            GISMarkerType.EXTRACTION_ZONE -> "b-m-p-s-p-x"
            GISMarkerType.COMMAND_POST -> "a-f-G-U-C-H"
            GISMarkerType.SUPPLY_CACHE -> "b-m-p-c-s"
        }
    }

    /**
     * Serializa un objeto CotEvent a formato XML Cursor-on-Target estricto para ATAK/CivTAK.
     */
    fun serializeToCotXml(event: CotEvent): String {
        val timeIso = synchronized(dateFormat) { dateFormat.format(Date(event.timeMillis)) }
        val staleIso = synchronized(dateFormat) { dateFormat.format(Date(event.staleMillis)) }

        return buildString {
            append("""<?xml version="1.0" standalone="yes"?>""")
            append("""<event version="2.0" """)
            append("""uid="${escapeXml(event.uid)}" """)
            append("""type="${escapeXml(event.type)}" """)
            append("""time="$timeIso" """)
            append("""start="$timeIso" """)
            append("""stale="$staleIso" """)
            append("""how="${escapeXml(event.how)}">""")
            append("""<point lat="${event.lat}" lon="${event.lon}" hae="${event.haeMeters}" ce="${event.ceMeters}" le="${event.leMeters}"/>""")
            append("""<detail>""")
            append("""<contact callsign="${escapeXml(event.callsign)}" endpoint="*:-1:stcp"/>""")
            if (event.remarks.isNotBlank()) {
                append("""<remarks>${escapeXml(event.remarks)}</remarks>""")
            }
            if (event.speedMps > 0 || event.courseDeg > 0) {
                append("""<track speed="${event.speedMps}" course="${event.courseDeg}"/>""")
            }
            append("""<precisionlocation geopointsrc="GPS"/>""")
            append("""</detail>""")
            append("""</event>""")
        }
    }

    /**
     * Parsea un texto XML básico de CoT para extraer sus campos principales.
     */
    fun parseCotXml(xml: String): CotEvent? {
        return try {
            val uidRegex = """uid=["']([^"']+)["']""".toRegex()
            val typeRegex = """type=["']([^"']+)["']""".toRegex()
            val latRegex = """lat=["']([^"']+)["']""".toRegex()
            val lonRegex = """lon=["']([^"']+)["']""".toRegex()
            val callsignRegex = """callsign=["']([^"']+)["']""".toRegex()
            val remarksRegex = """<remarks>([^<]+)</remarks>""".toRegex()

            val uid = uidRegex.find(xml)?.groupValues?.get(1) ?: UUID.randomUUID().toString()
            val type = typeRegex.find(xml)?.groupValues?.get(1) ?: "a-f-G-U-C"
            val lat = latRegex.find(xml)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
            val lon = lonRegex.find(xml)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
            val callsign = callsignRegex.find(xml)?.groupValues?.get(1) ?: "ATAK_NODE"
            val remarks = remarksRegex.find(xml)?.groupValues?.get(1) ?: ""

            CotEvent(
                uid = uid,
                type = type,
                callsign = callsign,
                lat = lat,
                lon = lon,
                remarks = remarks
            )
        } catch (e: Exception) {
            DiscoveryLogCollector.log(
                category = LogCategory.LAN_BEACON,
                severity = LogSeverity.WARNING,
                tag = "COT_TRANSCODER",
                message = "Fallo parseando XML CoT recibido: ${e.message}"
            )
            null
        }
    }

    /**
     * Envía inmediatamente un evento CoT hacia la red ATAK UDP (broadcast y multicast opcional).
     */
    fun dispatchCotEvent(event: CotEvent) {
        scope.launch {
            try {
                val xml = serializeToCotXml(event)
                val payload = xml.toByteArray(Charsets.UTF_8)
                val currentStatus = _status.value

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val broadcastAddr = InetAddress.getByName(currentStatus.targetBroadcastIp)
                    val packet = DatagramPacket(payload, payload.size, broadcastAddr, currentStatus.broadcastPort)
                    socket.send(packet)
                }

                _status.value = _status.value.copy(
                    packetsSent = _status.value.packetsSent + 1,
                    lastDispatchedXml = xml
                )

                DiscoveryLogCollector.log(
                    category = LogCategory.LAN_BEACON,
                    severity = LogSeverity.INFO,
                    tag = "COT_UDP",
                    message = "Evento CoT despachado a ATAK (${event.callsign} [${event.type}]) lat=${event.lat}, lon=${event.lon}"
                )
            } catch (e: Exception) {
                DiscoveryLogCollector.log(
                    category = LogCategory.LAN_BEACON,
                    severity = LogSeverity.WARNING,
                    tag = "COT_UDP",
                    message = "Error despachando paquete UDP CoT: ${e.message}"
                )
            }
        }
    }

    /**
     * Convierte y despacha un TacticalGISMarker como evento CoT.
     */
    fun dispatchGisMarkerAsCot(marker: TacticalGISMarker, operatorCallsign: String = "APOLLO_HQ") {
        val cotType = getCotTypeForGisMarker(marker.type)
        val event = CotEvent(
            uid = "GIS-${marker.id}",
            type = cotType,
            callsign = marker.title,
            lat = marker.latitude,
            lon = marker.longitude,
            remarks = "Creado por $operatorCallsign. Radio: ${marker.radiusMeters}m. ${marker.notes}"
        )
        dispatchCotEvent(event)
    }

    /**
     * Inicia la emisión periódica de balizas SA (Situational Awareness) CoT hacia ATAK.
     */
    fun startBroadcasting(callsign: String = "APOLLO_NODE_1", intervalMs: Long = 5000L) {
        if (_status.value.isBroadcastingActive) return
        _status.value = _status.value.copy(isBroadcastingActive = true)

        broadcastJob = scope.launch {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                tag = "COT_BROADCAST",
                message = "Iniciado servicio de baliza periódica CoT hacia ATAK UDP 4242 ($callsign)"
            )

            var lat = 4.6105
            var lon = -74.0820
            var course = 45.0

            while (isActive) {
                // Simulación de desplazamiento táctico leve si no hay GPS satelital disponible
                lat += (Math.random() - 0.5) * 0.0002
                lon += (Math.random() - 0.5) * 0.0002
                course = (course + (Math.random() - 0.5) * 10).coerceIn(0.0, 360.0)

                val event = CotEvent(
                    uid = "TACTICAL-$callsign",
                    type = "a-f-G-U-C", // Amigo Terrestre Unidad de Combate
                    callsign = callsign,
                    lat = lat,
                    lon = lon,
                    courseDeg = course,
                    speedMps = 1.4, // Velocidad pedestre
                    remarks = "Nodo Malla Táctica Activo - Batería 92%"
                )
                dispatchCotEvent(event)

                delay(intervalMs)
            }
        }
    }

    /**
     * Detiene la emisión periódica CoT.
     */
    fun stopBroadcasting() {
        broadcastJob?.cancel()
        broadcastJob = null
        _status.value = _status.value.copy(isBroadcastingActive = false)
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "COT_BROADCAST",
            message = "Baliza periódica CoT detenida por el operador"
        )
    }

    /**
     * Inicia el receptor UDP para capturar paquetes CoT de otros dispositivos ATAK / CivTAK en la red local.
     */
    fun startListening() {
        if (_status.value.isListeningActive) return
        _status.value = _status.value.copy(isListeningActive = true)

        listeningJob = scope.launch {
            try {
                DatagramSocket(_status.value.broadcastPort).use { socket ->
                    socket.reuseAddress = true
                    val buffer = ByteArray(4096)

                    DiscoveryLogCollector.log(
                        category = LogCategory.LAN_BEACON,
                        severity = LogSeverity.INFO,
                        tag = "COT_LISTENER",
                        message = "Receptor CoT a la escucha en puerto UDP ${_status.value.broadcastPort}"
                    )

                    while (isActive) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val receivedString = String(packet.data, 0, packet.length, Charsets.UTF_8)

                        val parsedEvent = parseCotXml(receivedString)
                        if (parsedEvent != null) {
                            val currentList = _receivedCotEvents.value
                            val filtered = currentList.filter { it.uid != parsedEvent.uid }
                            _receivedCotEvents.value = (listOf(parsedEvent) + filtered).take(30)

                            _status.value = _status.value.copy(
                                packetsReceived = _status.value.packetsReceived + 1,
                                lastReceivedCallsign = parsedEvent.callsign,
                                activeAtakPeersCount = (_receivedCotEvents.value.size)
                            )

                            DiscoveryLogCollector.log(
                                category = LogCategory.LAN_BEACON,
                                severity = LogSeverity.INFO,
                                tag = "COT_RECV",
                                message = "Recibido CoT de ${parsedEvent.callsign} (${parsedEvent.type}) lat=${parsedEvent.lat}, lon=${parsedEvent.lon}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    DiscoveryLogCollector.log(
                        category = LogCategory.LAN_BEACON,
                        severity = LogSeverity.WARNING,
                        tag = "COT_LISTENER",
                        message = "Error en receptor UDP CoT: ${e.message}"
                    )
                }
            } finally {
                _status.value = _status.value.copy(isListeningActive = false)
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _status.value = _status.value.copy(isListeningActive = false)
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
