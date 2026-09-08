package com.example.domain.remote

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.wifi.WifiManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.Formatter
import android.util.Log
import com.example.domain.c4isr.C4IsrMasterDashboardEngine
import com.example.domain.c4isr.MeshTimeSynchronizer
import com.example.domain.c4isr.SaluteIntelReportEngine
import com.example.domain.c4isr.TacticalAStarRouter
import com.example.domain.c4isr.TacticalVoiceSpectralEngine
import com.example.domain.discovery.AutonomousMeshDiscoveryEngine
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.media.AcousticShotClassifier
import com.example.domain.media.VisualOpticalLiFiTransceiver
import com.example.domain.p2p.FrequencyHoppingMeshEngine
import com.example.domain.p2p.TacticalBinaryCompressor
import com.example.domain.security.EmconAlphaManager
import com.example.domain.security.EmconLevel
import com.example.domain.security.EmergencyZeroizeManager
import com.example.domain.security.MeshHoneypotGenerator
import com.example.domain.security.PostQuantumKyberVault
import com.example.domain.security.RfEmissionSignatureMeter
import com.example.domain.security.ShamirSecretSharingVault
import com.example.domain.security.SybilMeshDetector
import com.example.domain.security.TacticalMissionBlockchain
import com.example.domain.sensors.BarometerStormAlertEngine
import com.example.domain.sensors.MavlinkTelemetryTransceiver
import com.example.domain.sensors.PedestrianDeadReckoningEngine
import com.example.domain.sensors.PhysioBioTelemetryManager
import com.example.domain.sensors.SafeBubble3DProximityRadar
import com.example.domain.sensors.SatelliteMeshGateway
import com.example.domain.sensors.SolarEphemerisCompass
import com.example.domain.sensors.TacticalBallisticsCalculator
import com.example.domain.sensors.TacticalMedevacEngine
import com.example.domain.sensors.TacticalPhotogrammetryEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Servidor Maestro de API REST, Consola Web y Control Externo para OmniComm Hub.
 * Permite controlar el 100% de la aplicación, los 31 subsistemas tácticos y el hardware
 * del dispositivo móvil desde una computadora externa (mediante Wi-Fi LAN, USB ADB Forwarding o Hotspot).
 */
class TacticalExternalControlServer private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null
    private val isRunningFlag = AtomicBoolean(false)
    private val requestCounter = AtomicInteger(0)
    private var serverJob: Job? = null

    private val _serverState = MutableStateFlow(
        ExternalServerState(
            isRunning = false,
            port = DEFAULT_PORT,
            localIp = "127.0.0.1",
            totalRequests = 0,
            lastEndpointCalled = "-",
            lastCallerIp = "-"
        )
    )
    val serverState: StateFlow<ExternalServerState> = _serverState.asStateFlow()

    companion object {
        const val DEFAULT_PORT = 9090
        private const val TAG = "TacticalControlApi"

        @Volatile
        private var INSTANCE: TacticalExternalControlServer? = null

        fun getInstance(context: Context): TacticalExternalControlServer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalExternalControlServer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    data class ExternalServerState(
        val isRunning: Boolean,
        val port: Int,
        val localIp: String,
        val totalRequests: Int,
        val lastEndpointCalled: String,
        val lastCallerIp: String,
        val activeClientsCount: Int = 0
    )

    fun startServer(port: Int = DEFAULT_PORT) {
        if (isRunningFlag.get()) {
            Log.d(TAG, "El servidor API externo ya está en ejecución.")
            return
        }

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunningFlag.set(true)
                val ip = getLocalIpAddress()

                _serverState.value = _serverState.value.copy(
                    isRunning = true,
                    port = port,
                    localIp = ip
                )

                DiscoveryLogCollector.log(
                    category = LogCategory.SYSTEM,
                    severity = LogSeverity.SUCCESS,
                    tag = "EXT_API_SERVER",
                    message = "Servidor de Control Externo API REST & Consola Web iniciado en http://$ip:$port (USB ADB: 'adb forward tcp:$port tcp:$port')",
                    metadata = mapOf("port" to port.toString(), "ip" to ip)
                )

                while (isRunningFlag.get() && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        scope.launch {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isRunningFlag.get()) break
                        Log.e(TAG, "Error aceptando conexión de cliente: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar servidor en puerto $port: ${e.message}", e)
                isRunningFlag.set(false)
                _serverState.value = _serverState.value.copy(isRunning = false)
                DiscoveryLogCollector.log(
                    category = LogCategory.SYSTEM,
                    severity = LogSeverity.ERROR,
                    tag = "EXT_API_SERVER",
                    message = "Fallo al iniciar servidor de control externo: ${e.message}"
                )
            }
        }
    }

    fun stopServer() {
        isRunningFlag.set(false)
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar socket del servidor: ${e.message}")
        }
        serverSocket = null
        serverJob?.cancel()
        _serverState.value = _serverState.value.copy(isRunning = false)

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "EXT_API_SERVER",
            message = "Servidor de Control Externo API detenido por el operador."
        )
    }

    private suspend fun handleClientConnection(socket: Socket) {
        withContext(Dispatchers.IO) {
            val clientIp = socket.inetAddress?.hostAddress ?: "unknown"
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val output = socket.getOutputStream()

                val requestLine = reader.readLine() ?: return@withContext
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@withContext

                val method = tokens[0].uppercase()
                val fullPath = tokens[1]
                val path = fullPath.substringBefore("?")
                val queryString = fullPath.substringAfter("?", "")

                val queryParams = mutableMapOf<String, String>()
                if (queryString.isNotEmpty()) {
                    queryString.split("&").forEach { pair ->
                        val kv = pair.split("=")
                        if (kv.size == 2) queryParams[kv[0]] = kv[1]
                    }
                }

                // Headers & Content-Length
                var contentLength = 0
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.isEmpty()) break
                    val headerLower = line!!.lowercase()
                    if (headerLower.startsWith("content-length:")) {
                        contentLength = headerLower.substringAfter("content-length:").trim().toIntOrNull() ?: 0
                    }
                }

                // Body
                val body = if (contentLength > 0) {
                    val charBuffer = CharArray(contentLength)
                    var read = 0
                    while (read < contentLength) {
                        val r = reader.read(charBuffer, read, contentLength - read)
                        if (r == -1) break
                        read += r
                    }
                    String(charBuffer, 0, read)
                } else ""

                val reqCount = requestCounter.incrementAndGet()
                _serverState.value = _serverState.value.copy(
                    totalRequests = reqCount,
                    lastEndpointCalled = "$method $path",
                    lastCallerIp = clientIp
                )

                routeHttpRequest(
                    method = method,
                    path = path,
                    query = queryParams,
                    body = body,
                    out = output,
                    clientIp = clientIp
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando petición de $clientIp: ${e.message}")
            } finally {
                try {
                    socket.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun routeHttpRequest(
        method: String,
        path: String,
        query: Map<String, String>,
        body: String,
        out: OutputStream,
        clientIp: String
    ) {
        // Manejo de CORS Preflight
        if (method == "OPTIONS") {
            sendResponse(out, 204, "No Content", "text/plain", "")
            return
        }

        when {
            // CONSOLA WEB Y BLUEPRINT
            (path == "/" || path == "/console") && method == "GET" -> {
                val html = generateWebCommandConsoleHtml()
                sendResponse(out, 200, "OK", "text/html; charset=UTF-8", html)
            }

            path == "/api/v1/ping" && method == "GET" -> {
                val json = JSONObject().apply {
                    put("status", "ok")
                    put("app", "OmniComm Hub")
                    put("version", "3.9-EXT-API")
                    put("serverTime", System.currentTimeMillis())
                    put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("androidVersion", Build.VERSION.RELEASE)
                }
                sendJsonResponse(out, 200, json)
            }

            // TELEMETRÍA GLOBAL Y ESTADO DE SALUD
            path == "/api/v1/status" && method == "GET" -> {
                val c4isrSummary = C4IsrMasterDashboardEngine.getInstance(context).summary.value
                val emconState = EmconAlphaManager.getInstance(context).state.value

                val json = JSONObject().apply {
                    put("timestamp", System.currentTimeMillis())
                    put("missionReadinessScore", c4isrSummary.operationalReadinessPercent)
                    put("emconLevel", emconState.currentLevel.name)
                    put("rfSilenceActive", emconState.passiveRxOnly)
                    put("meshHealthStatus", c4isrSummary.meshHealthStatus)
                    put("threatLevel", c4isrSummary.threatLevel)
                    put("activeSubsystemsCount", c4isrSummary.totalActiveSubsystems)
                    put("cryptoState", c4isrSummary.postQuantumSecurityStatus)
                }
                sendJsonResponse(out, 200, json)
            }

            // CONTROL DIRECTO DE HARDWARE: LINTERNA LED / MORSE / LI-FI
            path == "/api/v1/hardware/flash" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val state = bodyJson.optString("state", "toggle").lowercase()
                val pulses = bodyJson.optInt("pulses", 1)
                val durationMs = bodyJson.optLong("durationMs", 200L)
                val success = triggerFlashlightHardware(state, pulses, durationMs)

                val resp = JSONObject().apply {
                    put("success", success)
                    put("action", "flashlight_$state")
                    put("pulses", pulses)
                }
                sendJsonResponse(out, 200, resp)
            }

            // CONTROL DIRECTO DE HARDWARE: VIBRACIÓN HÁPTICA TÁCTICA
            path == "/api/v1/hardware/vibrate" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val patternType = bodyJson.optString("pattern", "sos").lowercase()
                val durationMs = bodyJson.optLong("durationMs", 500L)
                triggerTacticalVibration(patternType, durationMs)

                val resp = JSONObject().apply {
                    put("success", true)
                    put("pattern", patternType)
                }
                sendJsonResponse(out, 200, resp)
            }

            // CONTROL DIRECTO DE HARDWARE: SINTETIZADOR DE TONOS ACÚSTICOS DSP
            path == "/api/v1/hardware/audio" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val freqHz = bodyJson.optInt("frequencyHz", 1000)
                val durationMs = bodyJson.optInt("durationMs", 400)
                playToneSound(freqHz, durationMs)

                val resp = JSONObject().apply {
                    put("success", true)
                    put("frequencyHz", freqHz)
                    put("durationMs", durationMs)
                }
                sendJsonResponse(out, 200, resp)
            }

            // SENSORES EN TIEMPO REAL (ACELERÓMETRO, BARÓMETRO, PDR, BRÚJULA SOLAR, BIO)
            path == "/api/v1/sensors/live" && method == "GET" -> {
                val pdr = PedestrianDeadReckoningEngine.getInstance(context).pdrState.value
                val barometer = BarometerStormAlertEngine.getInstance(context).atmosphereState.value
                val solar = SolarEphemerisCompass.getInstance(context).solarData.value
                val bio = PhysioBioTelemetryManager.getInstance(context).bioMetrics.value

                val json = JSONObject().apply {
                    put("timestamp", System.currentTimeMillis())
                    put("deadReckoning", JSONObject().apply {
                        put("stepCount", pdr.stepCount)
                        put("distanceMeters", pdr.totalDistanceMeters)
                        put("headingDegrees", pdr.currentHeadingDegrees)
                        put("relativeX", pdr.relativeX)
                        put("relativeY", pdr.relativeY)
                        put("isGpsDeniedActive", pdr.isGpsDeniedActive)
                    })
                    put("barometer", JSONObject().apply {
                        put("pressureHpa", barometer.pressureHpa)
                        put("altitudeQnhMeters", barometer.estimatedAltitudeMeters)
                        put("stormAlertActive", barometer.stormAlertActive)
                        put("weatherSummary", barometer.weatherConditionSummary)
                    })
                    put("solarCompass", JSONObject().apply {
                        put("sunAzimuthDegrees", solar.sunAzimuthDegrees)
                        put("sunElevationDegrees", solar.sunElevationDegrees)
                        put("trueNorthHeading", solar.trueNorthCalculatedHeading)
                    })
                    put("biotelemetry", JSONObject().apply {
                        put("heartRateBpm", bio.estimatedHeartRateBpm)
                        put("combatReadinessScore", bio.combatReadinessScore)
                        put("stressIndex", bio.stressIndex)
                        put("fatigueStatus", bio.fatigueStatus)
                    })
                }
                sendJsonResponse(out, 200, json)
            }

            // ENRUTADOR DE ESCAPE A*
            path == "/api/v1/c4isr/a_star_route" && method == "POST" -> {
                val router = TacticalAStarRouter.getInstance(context)
                router.recalculateEscapeRoute(avoidHostileZone = true)
                val plan = router.currentPlan.value

                val resp = JSONObject().apply {
                    put("success", true)
                    put("routeId", plan.routeId)
                    put("origin", plan.origin)
                    put("extractionPoint", plan.extractionPoint)
                    put("distanceKm", plan.totalEstimatedDistanceKm)
                    put("riskScore", plan.routeRiskScore)
                    put("recommendedEgressMinutes", plan.recommendedEgressTimeMinutes)
                }
                sendJsonResponse(out, 200, resp)
            }

            // REPORTE DE INTELIGENCIA MILITAR SALUTE
            path == "/api/v1/c4isr/salute" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val size = bodyJson.optString("size", "1x Patrulla de Infantería (4 Pax)")
                val activity = bodyJson.optString("activity", "Desplazamiento táctico norte")
                val location = bodyJson.optString("location", "Sector Alpha-4 (Grid 34T)")
                val unit = bodyJson.optString("unit", "Fuerzas hostiles no identificadas")
                val time = bodyJson.optString("time", SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
                val equipment = bodyJson.optString("equipment", "Armamento ligero y comunicaciones")

                val report = com.example.domain.c4isr.SaluteReport(
                    size = size,
                    activity = activity,
                    location = location,
                    uniformUnit = unit,
                    timeObserved = time,
                    equipment = equipment
                )
                SaluteIntelReportEngine.getInstance(context).submitSaluteReport(report)

                val resp = JSONObject().apply {
                    put("success", true)
                    put("reportId", report.reportId)
                    put("location", report.location)
                }
                sendJsonResponse(out, 200, resp)
            }

            // TRIAJE MÉDICO 9-LINE MEDEVAC
            path == "/api/v1/c4isr/medevac" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val location = bodyJson.optString("location", "LZ-CHARLIE Grid 45E")
                val frequency = bodyJson.optString("freq", "38.500 MHz TACTICAL-1")
                val urgentPatients = bodyJson.optInt("urgentPatients", 1)
                val priorityPatients = bodyJson.optInt("priorityPatients", 0)

                val req = com.example.domain.sensors.NineLineMedevacRequest(
                    line1LocationGrid = location,
                    line2RadioFrequencyCallsign = frequency,
                    line3PatientsByPrecedence = "$urgentPatients x URGENTE, $priorityPatients x PRIORITARIO",
                    line4SpecialEquipmentRequired = "VENTILADOR + CABLE EXTRACCIÓN",
                    line5PatientsByType = "CAMILLA",
                    line6SecurityAtPickupSite = "SEGURO",
                    line7MethodOfMarking = "HUMO VERDE",
                    line8PatientNationalityStatus = "OPERADOR AMIGO",
                    line9NbcTerrainObstacles = "NINGUNO"
                )
                TacticalMedevacEngine.getInstance(context).submitMedevacRequest(req)

                val resp = JSONObject().apply {
                    put("success", true)
                    put("medevacId", req.reportId)
                    put("grid", req.line1LocationGrid)
                }
                sendJsonResponse(out, 200, resp)
            }

            // COMANDOS UAV MAVLINK
            path == "/api/v1/uav/command" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val command = bodyJson.optString("command", "RTL").uppercase()
                val transceiver = MavlinkTelemetryTransceiver.getInstance(context)

                when (command) {
                    "RTL", "RETURN_TO_LAUNCH" -> transceiver.triggerRtl()
                    else -> transceiver.updateWaypoint(19.4350, -99.1350, 150.0f)
                }

                val resp = JSONObject().apply {
                    put("success", true)
                    put("commandSent", command)
                    put("uavState", transceiver.state.value.activeUav.flightMode)
                }
                sendJsonResponse(out, 200, resp)
            }

            // PROTOCOLO DE AUTODESTRUCCIÓN ZEROIZE (DoD 5220.22-M)
            path == "/api/v1/security/zeroize" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val authKey = bodyJson.optString("authKey", "")

                if (authKey != "ZEROIZE-CONFIRM-994" && authKey != "TACTICAL-ALPHA") {
                    val errorResp = JSONObject().apply {
                        put("error", "Clave de autorización Zeroize inválida")
                        put("requiredKey", "ZEROIZE-CONFIRM-994")
                    }
                    sendJsonResponse(out, 403, errorResp)
                    return
                }

                scope.launch {
                    EmergencyZeroizeManager.getInstance(context).executeEmergencyZeroize()
                }

                val resp = JSONObject().apply {
                    put("success", true)
                    put("status", "ZEROIZE_EXECUTED")
                    put("message", "Bases de datos sobreescritas con DoD 5220.22-M y memoria purgada.")
                }
                sendJsonResponse(out, 200, resp)
            }

            // LOGS FORENSES EN TIEMPO REAL
            path == "/api/v1/logs" && method == "GET" -> {
                val limit = query["limit"]?.toIntOrNull() ?: 50
                val logs = DiscoveryLogCollector.logs.value.takeLast(limit)

                val arr = JSONArray()
                logs.forEach { logEntry ->
                    arr.put(JSONObject().apply {
                        put("id", logEntry.id)
                        put("time", logEntry.formattedTime)
                        put("category", logEntry.category.name)
                        put("severity", logEntry.severity.name)
                        put("tag", logEntry.tag)
                        put("message", logEntry.message)
                    })
                }

                val resp = JSONObject().apply {
                    put("totalCount", logs.size)
                    put("logs", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            // LÍNEA DE COMANDOS TÁCTICA (CLI API)
            path == "/api/v1/cli" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val cmdLine = bodyJson.optString("command", "help").trim()
                val cliOutput = executeTacticalCliCommand(cmdLine)

                val resp = JSONObject().apply {
                    put("command", cmdLine)
                    put("output", cliOutput)
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonResponse(out, 200, resp)
            }

            // GESTIÓN DE FLOTA DE SERVIDORES (LINUX & WINDOWS C2 API)
            path == "/api/v1/servers" && method == "GET" -> {
                val engine = ServerFleetManagementEngine.getInstance(context)
                val servers = engine.serverNodes.value
                val arr = JSONArray()
                servers.forEach { srv ->
                    arr.put(JSONObject().apply {
                        put("id", srv.id)
                        put("name", srv.name)
                        put("ip", srv.hostnameOrIp)
                        put("port", srv.port)
                        put("os", srv.osType.name)
                        put("status", srv.healthStatus.name)
                        put("cpu", srv.cpuUsagePercent)
                        put("ram", srv.ramUsagePercent)
                        put("disk", srv.diskUsagePercent)
                        put("pingMs", srv.pingLatencyMs)
                        put("uptime", srv.uptimeFormatted)
                        put("osVersion", srv.osVersionDetails)
                        put("servicesCount", srv.activeServices.size)
                        put("lastSeen", srv.lastSeenTimestamp)
                    })
                }
                val resp = JSONObject().apply {
                    put("count", servers.size)
                    put("servers", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            // REGISTRO DE SERVIDORES REMOTOS
            path == "/api/v1/servers/register" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val name = bodyJson.optString("name", "Remote Node")
                val ip = bodyJson.optString("ip", clientIp)
                val port = bodyJson.optInt("port", 22)
                val osStr = bodyJson.optString("os", "LINUX").uppercase()
                val osType = if (osStr.contains("WIN")) ServerOsType.WINDOWS else ServerOsType.LINUX

                val node = RemoteServerNode(
                    name = name,
                    hostnameOrIp = ip,
                    port = port,
                    osType = osType,
                    authProtocol = ServerAuthProtocol.REST_AGENT_TOKEN
                )
                ServerFleetManagementEngine.getInstance(context).registerServer(node)

                val resp = JSONObject().apply {
                    put("success", true)
                    put("serverId", node.id)
                    put("message", "Servidor ${node.name} registrado en la flota.")
                }
                sendJsonResponse(out, 200, resp)
            }

            // HEARTBEAT DESDE SERVIDORES LINUX / WINDOWS
            path == "/api/v1/servers/heartbeat" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val serverId = bodyJson.optString("serverId", clientIp)
                val cpu = bodyJson.optDouble("cpu", 10.0).toFloat()
                val ram = bodyJson.optDouble("ram", 40.0).toFloat()
                val disk = bodyJson.optDouble("disk", 50.0).toFloat()
                val uptime = bodyJson.optString("uptime", "up 1d")
                val srvCount = bodyJson.optInt("services", 5)

                ServerFleetManagementEngine.getInstance(context).updateServerHeartbeatFromRemote(
                    serverId = serverId,
                    cpu = cpu,
                    ram = ram,
                    disk = disk,
                    uptime = uptime,
                    activeServicesCount = srvCount
                )

                val resp = JSONObject().apply {
                    put("success", true)
                    put("acknowledged", true)
                    put("timestamp", System.currentTimeMillis())
                }
                sendJsonResponse(out, 200, resp)
            }

            // EJECUCIÓN REMOTA DE COMANDOS EN SERVIDORES (BASH / POWERSHELL)
            path == "/api/v1/servers/exec" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val serverId = bodyJson.optString("serverId", "")
                val command = bodyJson.optString("command", "uname -a")

                scope.launch {
                    val result = ServerFleetManagementEngine.getInstance(context).executeCommandOnServer(serverId, command)
                    val resp = JSONObject().apply {
                        put("executionId", result.executionId)
                        put("serverId", result.serverId)
                        put("command", result.command)
                        put("exitCode", result.exitCode)
                        put("stdout", result.stdout)
                        put("stderr", result.stderr)
                        put("durationMs", result.durationMs)
                    }
                    // Retornar en canal si fuera síncrono o despachado
                }

                // Ejecución síncrona inmediata para API REST
                val result = kotlinx.coroutines.runBlocking {
                    ServerFleetManagementEngine.getInstance(context).executeCommandOnServer(serverId, command)
                }

                val resp = JSONObject().apply {
                    put("executionId", result.executionId)
                    put("serverId", result.serverId)
                    put("command", result.command)
                    put("exitCode", result.exitCode)
                    put("stdout", result.stdout)
                    put("stderr", result.stderr)
                    put("durationMs", result.durationMs)
                }
                sendJsonResponse(out, 200, resp)
            }

            // ALERTA DESDE SERVIDOR EXTERNO HACIA ESTE DISPOSITIVO (BI-DIRECTIONAL TRIGGER)
            path == "/api/v1/servers/alert" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val serverId = bodyJson.optString("serverId", clientIp)
                val alertText = bodyJson.optString("alert", "Anomalía de sistema detectada.")
                val severity = bodyJson.optString("severity", "WARNING").uppercase()

                ServerFleetManagementEngine.getInstance(context).receiveServerAlert(
                    serverId = serverId,
                    alertText = alertText,
                    severity = severity
                )

                val resp = JSONObject().apply {
                    put("success", true)
                    put("status", "ALERT_PROCESSED_BY_MOBILE")
                    put("hapticTriggered", severity == "CRITICAL")
                }
                sendJsonResponse(out, 200, resp)
            }

            // SISTEMA DE CI/CD INTERNO Y AUTOMATIZACIÓN DEVSECOPS
            path == "/api/v1/cicd/pipelines" && method == "GET" -> {
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)
                val history = cicd.executionsHistory.value
                val running = cicd.currentRunningExecution.value

                val arr = JSONArray()
                history.forEach { exec ->
                    arr.put(JSONObject().apply {
                        put("executionId", exec.executionId)
                        put("pipelineName", exec.pipelineName)
                        put("status", exec.status.name)
                        put("triggerSource", exec.triggerSource.name)
                        put("branch", exec.branchOrTag)
                        put("commit", exec.commitHash)
                        put("durationMs", exec.totalDurationMs)
                        put("stagesCount", exec.stages.size)
                        put("artifactsCount", exec.generatedArtifacts.size)
                    })
                }

                val resp = JSONObject().apply {
                    put("isRunning", running != null)
                    put("activeExecutionId", running?.executionId)
                    put("totalPipelines", history.size)
                    put("pipelines", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            path == "/api/v1/cicd/trigger" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val name = bodyJson.optString("pipelineName", "Webhook DevSecOps Pipeline")
                val branch = bodyJson.optString("branch", "main")
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)

                val exec = cicd.triggerPipeline(
                    pipelineName = name,
                    source = com.example.domain.cicd.PipelineTriggerSource.WEBHOOK_EVENT,
                    branchOrTag = branch,
                    targetEnv = com.example.domain.cicd.DeploymentEnvironment.P2P_MESH_FLEET
                )

                val resp = JSONObject().apply {
                    put("success", true)
                    put("executionId", exec.executionId)
                    put("pipelineName", exec.pipelineName)
                    put("status", exec.status.name)
                    put("branch", exec.branchOrTag)
                }
                sendJsonResponse(out, 200, resp)
            }

            path == "/api/v1/cicd/artifacts" && method == "GET" -> {
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)
                val artifacts = cicd.artifactsRegistry.value

                val arr = JSONArray()
                artifacts.forEach { art ->
                    arr.put(JSONObject().apply {
                        put("id", art.id)
                        put("name", art.name)
                        put("type", art.type.name)
                        put("size", art.formattedSize)
                        put("sha256", art.sha256Checksum)
                        put("version", art.versionTag)
                    })
                }

                val resp = JSONObject().apply {
                    put("totalArtifacts", artifacts.size)
                    put("artifacts", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            path == "/api/v1/cicd/rollback" && method == "POST" -> {
                val bodyJson = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                val depId = bodyJson.optString("deploymentId", "")
                val reason = bodyJson.optString("reason", "Invocación vía API REST")
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)

                val ok = cicd.performRollback(depId, reason)
                val resp = JSONObject().apply {
                    put("success", ok)
                    put("deploymentId", depId)
                    put("action", "ROLLBACK_EXECUTED")
                }
                sendJsonResponse(out, if (ok) 200 else 400, resp)
            }

            // TELEMETRÍA VISUAL Y CAPTURAS DE PANTALLA INTERNAS
            path == "/api/v1/screenshots" && method == "GET" -> {
                val service = com.example.domain.media.TacticalScreenshotCaptureService.getInstance(context)
                val list = service.screenshotsList.value
                val arr = JSONArray()
                list.forEach { shot ->
                    arr.put(JSONObject().apply {
                        put("id", shot.id)
                        put("title", shot.title)
                        put("timestamp", shot.timestamp)
                        put("resolution", shot.resolutionLabel)
                        put("fileSizeBytes", shot.fileSizeBytes)
                        put("filePath", shot.filePath)
                    })
                }
                val resp = JSONObject().apply {
                    put("count", list.size)
                    put("screenshots", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            path == "/api/v1/screenshots/latest" && method == "GET" -> {
                val service = com.example.domain.media.TacticalScreenshotCaptureService.getInstance(context)
                val latest = service.latestScreenshot.value
                if (latest != null) {
                    val resp = JSONObject().apply {
                        put("id", latest.id)
                        put("title", latest.title)
                        put("timestamp", latest.timestamp)
                        put("resolution", latest.resolutionLabel)
                        put("fileSizeBytes", latest.fileSizeBytes)
                        put("base64Data", latest.base64Data)
                    }
                    sendJsonResponse(out, 200, resp)
                } else {
                    sendJsonResponse(out, 404, JSONObject().apply { put("error", "No hay capturas disponibles aún") })
                }
            }

            // REGISTRO DE ARQUITECTURA MAESTRA Y CAPACIDADES (64 FASES)
            path == "/api/v1/architecture/records" && method == "GET" -> {
                val records = com.example.domain.models.SystemArchitectureRegistry.masterRecords
                val queryCategory = query["category"]
                val filtered = if (!queryCategory.isNullOrBlank()) {
                    records.filter { it.category.contains(queryCategory, ignoreCase = true) }
                } else records

                val arr = JSONArray()
                filtered.forEach { rec ->
                    arr.put(JSONObject().apply {
                        put("phase", rec.phaseNumber)
                        put("block", rec.blockNumber)
                        put("title", rec.title)
                        put("category", rec.category)
                        put("scope", rec.operationalScope)
                        put("module", rec.domainModule)
                        put("layers", JSONArray(rec.architectureLayers))
                        put("files", JSONArray(rec.sourceFiles))
                        put("apis", JSONArray(rec.exposedApis))
                        put("standards", rec.researchAndStandards)
                        put("hardware", JSONArray(rec.hardwareAndSensors))
                    })
                }

                val resp = JSONObject().apply {
                    put("totalPhases", records.size)
                    put("filteredCount", filtered.size)
                    put("records", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            path == "/api/v1/architecture/capabilities" && method == "GET" -> {
                val caps = com.example.domain.models.SystemArchitectureRegistry.tacticalCapabilities
                val arr = JSONArray()
                caps.forEach { cap ->
                    arr.put(JSONObject().apply {
                        put("id", cap.id)
                        put("title", cap.title)
                        put("category", cap.category)
                        put("operationalScope", cap.operationalScope)
                        put("activePhase", cap.activePhase)
                        put("primaryModule", cap.primaryModule)
                        put("filePath", cap.filePath)
                        put("isRealTime", cap.isRealTime)
                        put("isHardwareAccelerated", cap.isHardwareAccelerated)
                        put("isZeroTrust", cap.isZeroTrust)
                    })
                }

                val resp = JSONObject().apply {
                    put("totalCapabilities", caps.size)
                    put("capabilities", arr)
                }
                sendJsonResponse(out, 200, resp)
            }

            else -> {
                val notFound = JSONObject().apply {
                    put("error", "Endpoint no encontrado: $method $path")
                    put("availableEndpoints", listOf(
                        "GET /console",
                        "GET /api/v1/ping",
                        "GET /api/v1/status",
                        "GET /api/v1/architecture/records",
                        "GET /api/v1/architecture/capabilities",
                        "POST /api/v1/hardware/flash",
                        "POST /api/v1/hardware/vibrate",
                        "POST /api/v1/hardware/audio",
                        "GET /api/v1/sensors/live",
                        "POST /api/v1/c4isr/a_star_route",
                        "POST /api/v1/c4isr/salute",
                        "POST /api/v1/c4isr/medevac",
                        "POST /api/v1/uav/command",
                        "POST /api/v1/security/zeroize",
                        "GET /api/v1/logs",
                        "POST /api/v1/cli",
                        "GET /api/v1/screenshots",
                        "GET /api/v1/screenshots/latest"
                    ))
                }
                sendJsonResponse(out, 404, notFound)
            }
        }
    }

    fun executeTacticalCliCommand(cmd: String): String {
        val parts = cmd.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "Comando vacío. Escribe 'help' para ver la lista de comandos disponibles."

        val root = parts[0].lowercase()
        val arg1 = parts.getOrNull(1)?.lowercase() ?: ""

        return when (root) {
            "help", "?" -> """
                === CONSOLA DE MANDO CLI TÁCTICA OMNICOMM HUB ===
                Comandos de Hardware & Sensores:
                  flash on|off|toggle|pulse [n]   - Controla la linterna LED
                  vibrate [sos|pulse|alert]       - Dispara patrón háptico
                  tone [freq_hz] [duration_ms]    - Emite tono audible/subaudible
                  pdr                             - Lee odometría PDR inercial
                  sensors                         - Muestra telemetría en tiempo real

                Comandos de Mando & C4ISR:
                  status                          - Estado general de la misión y 31 subsistemas
                  a_star                          - Calcula ruta de escape táctica A*
                  salute [size] [activity] [loc]  - Emite informe de inteligencia SALUTE
                  medevac [lz_grid]               - Transmite reporte 9-Line MEDEVAC
                  uav [rtl|waypoint]              - Envía comando MAVLink al dron ISR
                  voice_spectral                  - Alterna inversor espectral de voz 3.3 kHz
                  time_sync                       - Dispara pulso de sincronización temporal PPS

                Comandos de Ciberdefensa & Sigilo:
                  emcon [alpha|bravo|normal]      - Ajusta silencio de radio por software
                  kyber                           - Genera par de claves post-cuánticas ML-KEM
                  blockchain                      - Muestra el estado del bloque inmutable
                  honeypot                        - Despliega nodos señuelo fantasma
                  zeroize [confirm]               - Autodestrucción DoD 5220.22-M
                  logs [limit]                    - Muestra registros forenses recientes

                Comandos de Arquitectura & Fases (64 Fases):
                  architecture [categoria]       - Lista fases de la matriz arquitectónica
                  capabilities [dominio]          - Lista capacidades tácticas activas
            """.trimIndent()

            "status" -> {
                val summary = C4IsrMasterDashboardEngine.getInstance(context).summary.value
                val emcon = EmconAlphaManager.getInstance(context).state.value
                "ESTADO DEL SISTEMA: Score Operativo ${summary.operationalReadinessPercent}% | Subsistemas: ${summary.totalActiveSubsystems}/31 | EMCON: ${emcon.currentLevel.name} | Silencio RF: ${emcon.passiveRxOnly}"
            }

            "flash" -> {
                when (arg1) {
                    "on" -> { triggerFlashlightHardware("on", 1, 0); "Linterna encendida." }
                    "off" -> { triggerFlashlightHardware("off", 1, 0); "Linterna apagada." }
                    "pulse" -> {
                        val count = parts.getOrNull(2)?.toIntOrNull() ?: 3
                        triggerFlashlightHardware("pulse", count, 150); "Pulsos ópticos disparados: $count."
                    }
                    else -> { triggerFlashlightHardware("toggle", 1, 0); "Linterna alternada." }
                }
            }

            "vibrate" -> {
                triggerTacticalVibration(arg1.ifEmpty { "alert" }, 400)
                "Patrón de vibración '$arg1' emitido en el dispositivo."
            }

            "tone" -> {
                val freq = parts.getOrNull(1)?.toIntOrNull() ?: 1200
                val dur = parts.getOrNull(2)?.toIntOrNull() ?: 300
                playToneSound(freq, dur)
                "Tono emitido: $freq Hz durante $dur ms."
            }

            "emcon" -> {
                val lvl = when (arg1) {
                    "alpha" -> EmconLevel.EMCON_ALPHA
                    "bravo" -> EmconLevel.EMCON_BRAVO
                    else -> EmconLevel.NORMAL_TRANSMIT
                }
                EmconAlphaManager.getInstance(context).setEmconLevel(lvl)
                "Nivel EMCON establecido a: $lvl."
            }

            "pdr" -> {
                val pdr = PedestrianDeadReckoningEngine.getInstance(context).pdrState.value
                "PDR: Pasos=${pdr.stepCount}, Distancia=${pdr.totalDistanceMeters.toInt()}m, Rumbo=${pdr.currentHeadingDegrees.toInt()}°"
            }

            "a_star" -> {
                val router = TacticalAStarRouter.getInstance(context)
                router.recalculateEscapeRoute(avoidHostileZone = true)
                val plan = router.currentPlan.value
                "Ruta de escape A* calculada: ${plan.routeId} | Distancia: ${plan.totalEstimatedDistanceKm} km | Riesgo: ${plan.routeRiskScore}"
            }

            "uav" -> {
                val transceiver = MavlinkTelemetryTransceiver.getInstance(context)
                if (arg1 == "rtl") {
                    transceiver.triggerRtl()
                    "Comando RTL (Return to Launch) enviado a dron UAV."
                } else {
                    transceiver.updateWaypoint(19.4350, -99.1350, 120.0f)
                    "Waypoint UAV actualizado."
                }
            }

            "salute" -> {
                val rep = com.example.domain.c4isr.SaluteReport(
                    size = parts.getOrNull(1) ?: "1x Escuadrón",
                    activity = parts.getOrNull(2) ?: "Reconocimiento",
                    location = parts.getOrNull(3) ?: "Sector Grid 44",
                    uniformUnit = "Fuerzas hostiles",
                    timeObserved = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                    equipment = "Armas ligeras"
                )
                SaluteIntelReportEngine.getInstance(context).submitSaluteReport(rep)
                "Reporte SALUTE transmitido: ID ${rep.reportId} en ${rep.location}"
            }

            "medevac" -> {
                val lz = parts.getOrNull(1) ?: "LZ-EAGLE"
                val req = com.example.domain.sensors.NineLineMedevacRequest(
                    line1LocationGrid = lz,
                    line2RadioFrequencyCallsign = "38.500 MHz",
                    line3PatientsByPrecedence = "1x URGENTE",
                    line4SpecialEquipmentRequired = "GRUA",
                    line5PatientsByType = "CAMILLA",
                    line6SecurityAtPickupSite = "SEGURO",
                    line7MethodOfMarking = "HUMO VERDE",
                    line8PatientNationalityStatus = "AMIGO",
                    line9NbcTerrainObstacles = "NINGUNO"
                )
                TacticalMedevacEngine.getInstance(context).submitMedevacRequest(req)
                "9-Line MEDEVAC generado y emitido: ID ${req.reportId} en $lz"
            }

            "kyber" -> {
                val key = PostQuantumKyberVault.generateHybridKeyPair()
                "Llave híbrida ML-KEM Kyber-768 generada. ID: ${key.keyId} (${key.securityLevelBits} bits)"
            }

            "voice_spectral" -> {
                val eng = TacticalVoiceSpectralEngine.getInstance(context)
                eng.toggleVoiceScrambler()
                "Enmascarador espectral de voz alternado."
            }

            "sensors" -> {
                val pdr = PedestrianDeadReckoningEngine.getInstance(context).pdrState.value
                val baro = BarometerStormAlertEngine.getInstance(context).atmosphereState.value
                val bio = PhysioBioTelemetryManager.getInstance(context).bioMetrics.value
                "SENSORES: PDR Steps: ${pdr.stepCount}, Rumbo: ${pdr.currentHeadingDegrees.toInt()}° | Presión: ${baro.pressureHpa} hPa | FC: ${bio.estimatedHeartRateBpm} BPM, Readiness: ${bio.combatReadinessScore}%"
            }

            "zeroize" -> {
                val code = arg1.uppercase()
                if (code == "CONFIRM") {
                    scope.launch {
                        EmergencyZeroizeManager.getInstance(context).executeEmergencyZeroize()
                    }
                    "ZEROIZE EJECUTADO. Purgando datos de forma destructiva."
                } else {
                    "ADVERTENCIA: Para ejecutar la autodestrucción escribe: zeroize confirm"
                }
            }

            "logs" -> {
                val limit = arg1.toIntOrNull() ?: 5
                val logs = DiscoveryLogCollector.logs.value.takeLast(limit)
                logs.joinToString("\n") { "[${it.formattedTime}] [${it.category.name}] ${it.tag}: ${it.message}" }
            }

            "servers" -> {
                val engine = ServerFleetManagementEngine.getInstance(context)
                val srvList = engine.serverNodes.value
                val sb = StringBuilder("=== FLOTA DE SERVIDORES LINUX & WINDOWS (${srvList.size} NODOS) ===\n")
                srvList.forEach { s ->
                    sb.append("[${s.id}] ${s.name} (${s.osType}) - IP: ${s.hostnameOrIp}:${s.port} | CPU: ${s.cpuUsagePercent}% | RAM: ${s.ramUsagePercent}% | Status: ${s.healthStatus.name} | Uptime: ${s.uptimeFormatted}\n")
                }
                sb.toString()
            }

            "server_exec" -> {
                val srvId = parts.getOrNull(1) ?: ""
                val cmdToRun = parts.drop(2).joinToString(" ")
                if (srvId.isEmpty() || cmdToRun.isEmpty()) {
                    "Uso: server_exec <server_id> <comando bash/powershell>"
                } else {
                    val result = kotlinx.coroutines.runBlocking {
                        ServerFleetManagementEngine.getInstance(context).executeCommandOnServer(srvId, cmdToRun)
                    }
                    "=== RESULTADO EJECUCIÓN (Exit Code: ${result.exitCode}, ${result.durationMs}ms) ===\n${result.stdout}${if (result.stderr.isNotEmpty()) "\n[STDERR]: " + result.stderr else ""}"
                }
            }

            "server_restart" -> {
                val srvId = parts.getOrNull(1) ?: ""
                val svcName = parts.getOrNull(2) ?: ""
                if (srvId.isEmpty() || svcName.isEmpty()) {
                    "Uso: server_restart <server_id> <nombre_servicio>"
                } else {
                    val ok = kotlinx.coroutines.runBlocking {
                        ServerFleetManagementEngine.getInstance(context).controlService(srvId, svcName, "restart")
                    }
                    if (ok) "Servicio '$svcName' reiniciado con éxito en servidor $srvId."
                    else "Error reiniciando servicio '$svcName' en servidor $srvId."
                }
            }

            "server_script" -> {
                val osType = arg1.lowercase()
                val localIp = _serverState.value.localIp
                val port = _serverState.value.port
                val engine = ServerFleetManagementEngine.getInstance(context)
                if (osType.contains("win")) {
                    engine.generateWindowsAgentInstallScript(localIp, port)
                } else {
                    engine.generateLinuxAgentInstallScript(localIp, port)
                }
            }

            "cicd" -> {
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)
                val list = cicd.executionsHistory.value
                val running = cicd.currentRunningExecution.value
                val sb = StringBuilder("=== SISTEMA CI/CD INTERNO DEVSECOPS ===\n")
                sb.append("Estado Motor: ${if (running != null) "EN EJECUCIÓN (${running.executionId})" else "LISTO"}\n")
                sb.append("Total Pipelines: ${list.size} | Exitosas: ${list.count { it.status == com.example.domain.cicd.PipelineStatus.SUCCESS }}\n")
                list.takeLast(5).reversed().forEach { exec ->
                    sb.append(" • [${exec.executionId}] ${exec.pipelineName} (${exec.status}) - Rama: ${exec.branchOrTag} - Duración: ${exec.totalDurationMs}ms\n")
                }
                sb.toString()
            }

            "cicd_run" -> {
                val pipeName = if (parts.size > 1) parts.drop(1).joinToString(" ") else "CLI Automated Pipeline Run"
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)
                val exec = cicd.triggerPipeline(
                    pipelineName = pipeName,
                    source = com.example.domain.cicd.PipelineTriggerSource.REST_API_EXTERNAL,
                    branchOrTag = "main",
                    targetEnv = com.example.domain.cicd.DeploymentEnvironment.P2P_MESH_FLEET
                )
                "Pipeline despachada con éxito: ID ${exec.executionId} ('${exec.pipelineName}')"
            }

            "cicd_artifacts" -> {
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)
                val arts = cicd.artifactsRegistry.value
                val sb = StringBuilder("=== BÓVEDA DE ARTEFACTOS Y SBOM (${arts.size} PAQUETES) ===\n")
                arts.forEach { art ->
                    sb.append(" • [${art.type.name}] ${art.name} (${art.formattedSize}) - SHA256: ${art.sha256Checksum.take(16)}... - Ver: ${art.versionTag}\n")
                }
                sb.toString()
            }

            "cicd_rollback" -> {
                val depId = parts.getOrNull(1) ?: ""
                val cicd = com.example.domain.cicd.InternalCiCdEngine.getInstance(context)
                if (depId.isEmpty()) {
                    val deps = cicd.deploymentRecords.value
                    val sb = StringBuilder("Uso: cicd_rollback <deployment_id>\nDespliegues disponibles:\n")
                    deps.forEach { d ->
                        sb.append(" • [${d.id}] ${d.environment.title} - Ver: ${d.version} (Status: ${d.status})\n")
                    }
                    sb.toString()
                } else {
                    val ok = cicd.performRollback(depId, "Solicitud de rollback vía CLI")
                    if (ok) "Rollback ejecutado satisfactoriamente para despliegue $depId."
                    else "Error o despliegue $depId no elegible para rollback."
                }
            }

            "screenshots", "screenshot_list" -> {
                val service = com.example.domain.media.TacticalScreenshotCaptureService.getInstance(context)
                val list = service.screenshotsList.value
                val sb = StringBuilder("=== REGISTRO DE TELEMETRÍA VISUAL Y CAPTURAS (${list.size}) ===\n")
                if (list.isEmpty()) {
                    sb.append("No hay capturas internas almacenadas aún. Pulsa el botón de captura en la interfaz táctica o ejecuta un test.")
                } else {
                    list.forEach { shot ->
                        sb.append(" • [${shot.id}] ${shot.title} (${shot.resolutionLabel}) - ${shot.fileSizeBytes / 1024} KB - Archivo: ${shot.filePath}\n")
                    }
                }
                sb.toString()
            }

            "architecture", "phases", "roadmap" -> {
                val records = com.example.domain.models.SystemArchitectureRegistry.masterRecords
                val queryCategory = arg1
                val filtered = if (queryCategory.isNotEmpty()) {
                    records.filter { it.category.contains(queryCategory, ignoreCase = true) }
                } else records

                val sb = StringBuilder("=== MATRIZ DE ARQUITECTURA TÁCTICA (${filtered.size}/${records.size} FASES) ===\n")
                filtered.take(15).forEach { r ->
                    sb.append(" • [Fase ${r.phaseNumber} | Bloque ${r.blockNumber}] ${r.title} [${r.category}]\n")
                    sb.append("   Módulo: ${r.domainModule} | APIs: ${r.exposedApis.joinToString(", ")}\n")
                }
                if (filtered.size > 15) {
                    sb.append(" ... y ${filtered.size - 15} fases adicionales registradas en la bóveda forense.\n")
                }
                sb.toString()
            }

            "capabilities", "caps" -> {
                val caps = com.example.domain.models.SystemArchitectureRegistry.tacticalCapabilities
                val queryDomain = arg1
                val filtered = if (queryDomain.isNotEmpty()) {
                    caps.filter { it.category.contains(queryDomain, ignoreCase = true) }
                } else caps

                val sb = StringBuilder("=== MATRIZ DE CAPACIDADES TÁCTICAS (${filtered.size}/${caps.size} ACTIVAS) ===\n")
                filtered.take(15).forEach { c ->
                    sb.append(" • [${c.id}] Fase ${c.activePhase}: ${c.title} (${c.category})\n")
                    sb.append("   ${c.operationalScope}\n")
                }
                if (filtered.size > 15) {
                    sb.append(" ... y ${filtered.size - 15} capacidades adicionales activas.\n")
                }
                sb.toString()
            }

            else -> "Comando desconocido '$root'. Escribe 'help' para consultar los comandos admitidos."
        }
    }

    private fun triggerFlashlightHardware(state: String, pulses: Int, durationMs: Long): Boolean {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false

            scope.launch {
                when (state) {
                    "on" -> cameraManager.setTorchMode(cameraId, true)
                    "off" -> cameraManager.setTorchMode(cameraId, false)
                    "pulse" -> {
                        for (i in 0 until pulses) {
                            cameraManager.setTorchMode(cameraId, true)
                            kotlinx.coroutines.delay(durationMs)
                            cameraManager.setTorchMode(cameraId, false)
                            kotlinx.coroutines.delay(durationMs)
                        }
                    }
                    else -> {
                        cameraManager.setTorchMode(cameraId, true)
                        kotlinx.coroutines.delay(300)
                        cameraManager.setTorchMode(cameraId, false)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error controlando flash: ${e.message}")
            false
        }
    }

    private fun triggerTacticalVibration(pattern: String, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                when (pattern) {
                    "sos" -> {
                        val timings = longArrayOf(0, 100, 100, 100, 100, 100, 200, 300, 100, 300, 100, 300, 200, 100, 100, 100, 100, 100)
                        val effect = VibrationEffect.createWaveform(timings, -1)
                        vibrator?.vibrate(effect)
                    }
                    else -> {
                        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator?.vibrate(effect)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error emitiendo vibración: ${e.message}")
        }
    }

    private fun playToneSound(freqHz: Int, durationMs: Int) {
        scope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val generatedSnd = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / freqHz.toDouble())
                    generatedSnd[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.7).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, numSamples)
                audioTrack.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 100)
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error reproduciendo tono acústico: ${e.message}")
            }
        }
    }

    private fun sendJsonResponse(out: OutputStream, statusCode: Int, json: JSONObject) {
        val bodyBytes = json.toString(2).toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
    }

    private fun sendResponse(out: OutputStream, statusCode: Int, statusText: String, contentType: String, content: String) {
        val bodyBytes = content.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
    }

    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiIp = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIp != 0) {
                @Suppress("DEPRECATION")
                return Formatter.formatIpAddress(wifiIp)
            }

            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun generateWebCommandConsoleHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OmniComm Hub - Consola de Mando Externa Táctica</title>
                <style>
                    :root {
                        --bg-dark: #0B0F17;
                        --card-bg: #161B22;
                        --border-color: #30363D;
                        --cyan-primary: #00E5FF;
                        --emerald-sec: #00E676;
                        --amber-tertiary: #FFD600;
                        --red-alert: #FF1744;
                        --text-main: #F0F6FC;
                    }
                    body {
                        background-color: var(--bg-dark);
                        color: var(--text-main);
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        margin: 0;
                        padding: 20px;
                    }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 2px solid var(--cyan-primary);
                        padding-bottom: 15px;
                        margin-bottom: 20px;
                    }
                    .title { font-size: 22px; font-weight: bold; color: var(--cyan-primary); font-family: monospace; }
                    .badge { background: #00E67633; color: var(--emerald-sec); border: 1px solid var(--emerald-sec); padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; font-family: monospace; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 15px; margin-bottom: 20px; }
                    .card { background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 8px; padding: 15px; }
                    .card-header { font-weight: bold; color: var(--amber-tertiary); font-size: 13px; margin-bottom: 10px; font-family: monospace; }
                    .btn-group { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
                    button {
                        background: #21262D; color: var(--text-main); border: 1px solid var(--cyan-primary);
                        padding: 8px 12px; border-radius: 4px; cursor: pointer; font-family: monospace; font-size: 11px;
                        transition: all 0.2s;
                    }
                    button:hover { background: var(--cyan-primary); color: #000; }
                    button.danger { border-color: var(--red-alert); color: var(--red-alert); }
                    button.danger:hover { background: var(--red-alert); color: #fff; }
                    .terminal {
                        background: #000; border: 1px solid var(--border-color); border-radius: 6px;
                        padding: 12px; font-family: 'Courier New', Courier, monospace; font-size: 12px;
                        height: 220px; overflow-y: auto; color: var(--emerald-sec); margin-top: 15px;
                    }
                    .input-bar { display: flex; gap: 10px; margin-top: 10px; }
                    input[type="text"] {
                        flex: 1; background: #0D1117; border: 1px solid var(--border-color);
                        color: #fff; padding: 8px; border-radius: 4px; font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <div class="title">OMNICOMM TACTICAL SUITE - EXTERNAL C2 API</div>
                        <div style="font-size: 11px; color: #8B949E;">Control Remoto de Dispositivo y 31 Subsistemas Tácticos</div>
                    </div>
                    <div class="badge">HTTP REST v3.9 ACTIVE</div>
                </div>

                <div class="grid">
                    <div class="card">
                        <div class="card-header">⚡ CONTROL DE HARDWARE HAL</div>
                        <div>Accionamiento directo de actuadores y transceptores físicos:</div>
                        <div class="btn-group">
                            <button onclick="postHardware('flash', {state:'pulse', pulses:3})">Flash LED (3 Pulsos)</button>
                            <button onclick="postHardware('vibrate', {pattern:'sos'})">Vibración SOS</button>
                            <button onclick="postHardware('audio', {frequencyHz:1200, durationMs:400})">Tono 1.2 kHz</button>
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-header">📡 MANDO C4ISR & MISIÓN</div>
                        <div>Comandos operacionales y enlaces tácticos:</div>
                        <div class="btn-group">
                            <button onclick="postEndpoint('/api/v1/c4isr/salute', {})">Emitir SALUTE</button>
                            <button onclick="postEndpoint('/api/v1/c4isr/a_star_route', {})">Ruta A* Escape</button>
                            <button onclick="postEndpoint('/api/v1/uav/command', {command:'RTL'})">UAV RTL</button>
                            <button onclick="postEndpoint('/api/v1/c4isr/medevac', {})">9-Line MEDEVAC</button>
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-header">🛡️ SEGURIDAD ZERO-TRUST & EMCON</div>
                        <div>Políticas de sigilo y mitigación de amenazas:</div>
                        <div class="btn-group">
                            <button onclick="sendCli('emcon alpha')">EMCON Alpha (Silencio RF)</button>
                            <button onclick="sendCli('kyber')">Generar Kyber-768</button>
                            <button class="danger" onclick="executeZeroize()">ZEROIZE (DoD 5220.22-M)</button>
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-header">🖥️ MANDO DE SERVIDORES LINUX & WINDOWS (BI-DIRECTIONAL C2)</div>
                        <div>Control de infraestructura remota desde el navegador / móvil:</div>
                        <div class="btn-group">
                            <button onclick="sendCli('servers')">Listar Flota de Servidores</button>
                            <button onclick="sendCli('server_exec SRV-LNX-01 uname -a')">Linux: uname -a</button>
                            <button onclick="sendCli('server_exec SRV-LNX-01 top -b -n 1')">Linux: top</button>
                            <button onclick="sendCli('server_exec SRV-WIN-01 Get-ComputerInfo')">Windows: Get-ComputerInfo</button>
                            <button onclick="sendCli('server_exec SRV-WIN-01 Get-Process')">Windows: Get-Process</button>
                            <button onclick="sendCli('server_script linux')">Script Agente Linux</button>
                            <button onclick="sendCli('server_script windows')">Script Agente Windows</button>
                        </div>
                    </div>

                    <div class="card">
                        <div class="card-header">🚀 CI/CD & DEVSECOPS INTERNO</div>
                        <div>Automatización de compilación, análisis SAST y despliegue OTA:</div>
                        <div class="btn-group">
                            <button onclick="sendCli('cicd')">Estado CI/CD</button>
                            <button onclick="sendCli('cicd_run Webhook Run')">Lanzar Pipeline</button>
                            <button onclick="sendCli('cicd_artifacts')">Bóveda Artefactos</button>
                            <button onclick="sendCli('cicd_rollback DEP-001')">Rollback DEP-001</button>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">💻 TERMINAL CLI TÁCTICA EXTERNA</div>
                    <div class="terminal" id="termLog">> Consola Táctica conectada al servidor OmniComm en puerto 9090.\n> Escribe 'help' o presiona los botones de control para interactuar.</div>
                    <div class="input-bar">
                        <input type="text" id="cliCommand" placeholder="Escribe un comando CLI (ej: status, sensors, flash on, uav rtl)..." onkeydown="if(event.key==='Enter') sendCliInput()">
                        <button onclick="sendCliInput()">Enviar Orden</button>
                    </div>
                </div>

                <script>
                    function log(msg) {
                        const term = document.getElementById('termLog');
                        term.innerText += '\n> ' + msg;
                        term.scrollTop = term.scrollHeight;
                    }
                    async function postHardware(endpoint, payload) {
                        log('Enviando hardware POST: ' + endpoint);
                        try {
                            const res = await fetch('/api/v1/hardware/' + endpoint, {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify(payload)
                            });
                            const data = await res.json();
                            log(JSON.stringify(data));
                        } catch(e) { log('Error: ' + e.message); }
                    }
                    async function postEndpoint(endpoint, payload) {
                        log('POST ' + endpoint);
                        try {
                            const res = await fetch(endpoint, {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify(payload)
                            });
                            const data = await res.json();
                            log(JSON.stringify(data));
                        } catch(e) { log('Error: ' + e.message); }
                    }
                    async function sendCli(cmd) {
                        log('CLI CMD: ' + cmd);
                        try {
                            const res = await fetch('/api/v1/cli', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({command: cmd})
                            });
                            const data = await res.json();
                            log(data.output);
                        } catch(e) { log('Error: ' + e.message); }
                    }
                    function sendCliInput() {
                        const input = document.getElementById('cliCommand');
                        if (input.value.trim() !== '') {
                            sendCli(input.value.trim());
                            input.value = '';
                        }
                    }
                    function executeZeroize() {
                        if (confirm('¡ADVERTENCIA! ¿Deseas ejecutar el protocolo de autodestrucción DoD 5220.22-M ZEROIZE?')) {
                            postEndpoint('/api/v1/security/zeroize', {authKey: 'ZEROIZE-CONFIRM-994'});
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }
}
