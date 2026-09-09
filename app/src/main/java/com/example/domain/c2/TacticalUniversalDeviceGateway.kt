package com.example.domain.c2

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.example.domain.hardware.TacticalDeXDisplayEngine
import com.example.domain.hardware.TacticalUniversalTvRemoteEngine
import com.example.domain.hardware.TvRemoteCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Estado Operativo de la Pasarela Universal Multi-Dispositivo
 */
data class UniversalGatewayState(
    val isRunning: Boolean = false,
    val port: Int = 8888,
    val localIpAddress: String = "127.0.0.1",
    val accessUrl: String = "http://127.0.0.1:8888",
    val connectedClientsCount: Int = 0,
    val totalRequestsHandled: Int = 0,
    val clientLogs: List<String> = emptyList()
)

/**
 * Pasarela Táctica Universal Multi-Dispositivo (Android, iPhone, Laptops, Windows, Linux, macOS).
 * Levanta un micro-servidor HTTP local con panel web militar táctico integrado en el puerto 8888.
 * Permite que cualquier navegador (Safari en iPhone, Chrome/Firefox en PC o Mac) controle el centro de mando,
 * maneje la TV por infrarrojos/red, transmita entradas al escritorio Omni-DeX y envíe órdenes C2 a la malla.
 */
class TacticalUniversalDeviceGateway private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _state = MutableStateFlow(UniversalGatewayState())
    val state: StateFlow<UniversalGatewayState> = _state.asStateFlow()

    companion object {
        private const val TAG = "DeviceGateway"
        private const val DEFAULT_PORT = 8888

        @Volatile
        private var instance: TacticalUniversalDeviceGateway? = null

        fun getInstance(context: Context): TacticalUniversalDeviceGateway {
            return instance ?: synchronized(this) {
                instance ?: TacticalUniversalDeviceGateway(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        val ip = resolveLocalIpAddress()
        _state.value = _state.value.copy(
            localIpAddress = ip,
            accessUrl = "http://$ip:$DEFAULT_PORT"
        )
        startGateway()
    }

    fun startGateway(port: Int = DEFAULT_PORT) {
        if (serverJob?.isActive == true) return

        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val ip = resolveLocalIpAddress()
                _state.value = _state.value.copy(
                    isRunning = true,
                    port = port,
                    localIpAddress = ip,
                    accessUrl = "http://$ip:$port"
                )
                addLog("Pasarela Multi-Dispositivo activa en: http://$ip:$port")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClientConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en servidor gateway", e)
                _state.value = _state.value.copy(isRunning = false)
            }
        }
    }

    fun stopGateway() {
        try {
            serverSocket?.close()
            serverJob?.cancel()
            _state.value = _state.value.copy(isRunning = false)
            addLog("Pasarela Multi-Dispositivo detenida.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando pasarela", e)
        }
    }

    private fun handleClientConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Leer cabeceras
            var line: String?
            var contentLength = 0
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            var body = ""
            if (contentLength > 0) {
                val charArray = CharArray(contentLength)
                reader.read(charArray, 0, contentLength)
                body = String(charArray)
            }

            _state.value = _state.value.copy(
                totalRequestsHandled = _state.value.totalRequestsHandled + 1
            )

            when {
                path == "/" || path.startsWith("/?") -> {
                    serveWebDashboard(writer)
                }
                path.startsWith("/api/tv/") -> {
                    val cmdStr = path.removePrefix("/api/tv/").uppercase()
                    handleTvApi(cmdStr, writer)
                }
                path.startsWith("/api/dex/click") -> {
                    TacticalDeXDisplayEngine.getInstance(context).triggerPointerClick()
                    writeJsonResponse(writer, """{"status":"OK","action":"DEX_CLICK"}""")
                }
                path.startsWith("/api/dex/move") -> {
                    TacticalDeXDisplayEngine.getInstance(context).moveTrackpadPointer(0.02f, 0.02f)
                    writeJsonResponse(writer, """{"status":"OK","action":"DEX_MOVE"}""")
                }
                path.startsWith("/api/status") -> {
                    val dexState = TacticalDeXDisplayEngine.getInstance(context).state.value
                    val json = """{"system":"OmniComm C4ISR","defcon":${dexState.defconStatus},"zulu":"${dexState.zuluTime}","clients":${_state.value.totalRequestsHandled}}"""
                    writeJsonResponse(writer, json)
                }
                else -> {
                    writer.println("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n")
                }
            }
            socket.close()
        } catch (e: Exception) {
            Log.w(TAG, "Excepción atendiendo cliente web", e)
        }
    }

    private fun handleTvApi(cmdStr: String, writer: PrintWriter) {
        val tvEngine = TacticalUniversalTvRemoteEngine.getInstance(context)
        val matchedCmd = TvRemoteCommand.values().find { it.name.equals(cmdStr, ignoreCase = true) }
        if (matchedCmd != null) {
            tvEngine.sendCommand(matchedCmd)
            addLog("Comando TV disparado desde cliente remoto: ${matchedCmd.label}")
            writeJsonResponse(writer, """{"status":"OK","command":"${matchedCmd.name}"}""")
        } else {
            writeJsonResponse(writer, """{"status":"ERROR","message":"Comando TV desconocido"}""")
        }
    }

    private fun writeJsonResponse(writer: PrintWriter, json: String) {
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: application/json; charset=utf-8")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println("Content-Length: ${json.toByteArray().size}")
        writer.println()
        writer.println(json)
    }

    private fun serveWebDashboard(writer: PrintWriter) {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>OMNICOMM C4ISR • CENTRO DE CONTROL UNIVERSAL</title>
                <style>
                    body { background: #0b0f19; color: #38bdf8; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace; margin: 0; padding: 16px; }
                    .header { border-bottom: 2px solid #38bdf8; padding-bottom: 10px; margin-bottom: 16px; }
                    h1 { margin: 0; font-size: 1.2rem; letter-spacing: 1px; }
                    .badge { background: #0284c7; color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; }
                    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 10px; margin-bottom: 20px; }
                    .btn { background: #161b22; border: 1px solid #30363d; color: #f1f5f9; padding: 14px 10px; border-radius: 8px; font-size: 0.85rem; font-weight: bold; cursor: pointer; text-align: center; }
                    .btn:active { background: #0284c7; }
                    .btn-danger { border-color: #ef4444; color: #ef4444; }
                    .btn-accent { border-color: #38bdf8; color: #38bdf8; }
                    .section-title { font-size: 0.9rem; color: #94a3b8; margin: 12px 0 8px 0; text-transform: uppercase; font-weight: bold; }
                    .terminal { background: #030712; border: 1px solid #30363d; padding: 10px; border-radius: 6px; font-size: 0.75rem; color: #34d399; font-family: monospace; height: 100px; overflow-y: auto; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>OMNICOMM C4ISR <span class="badge">DISPOSITIVO CONECTADO</span></h1>
                    <p style="font-size:0.8rem; color:#94a3b8; margin:4px 0 0 0;">Control Remoto Universal de TV, Audio, Sensores & Escritorio Omni-DeX</p>
                </div>

                <div class="section-title">CONTROL UNIVERSAL DE TELEVISIÓN (IR & SMART LAN)</div>
                <div class="grid">
                    <button class="btn btn-danger" onclick="sendTv('POWER')">⚡ POWER</button>
                    <button class="btn btn-accent" onclick="sendTv('MUTE')">🔇 MUTE</button>
                    <button class="btn" onclick="sendTv('VOLUME_UP')">VOL +</button>
                    <button class="btn" onclick="sendTv('VOLUME_DOWN')">VOL -</button>
                    <button class="btn" onclick="sendTv('CHANNEL_UP')">CANAL +</button>
                    <button class="btn" onclick="sendTv('CHANNEL_DOWN')">CANAL -</button>
                    <button class="btn" onclick="sendTv('INPUT_SOURCE')">HDMI / SOURCE</button>
                    <button class="btn" onclick="sendTv('HOME')">SMART HUB</button>
                </div>

                <div class="section-title">CONTROL DE RATÓN & ESCRITORIO OMNI-DEX (TV HDMI)</div>
                <div class="grid">
                    <button class="btn btn-accent" onclick="sendDex('click')">🎯 CLICK RATÓN</button>
                    <button class="btn" onclick="sendDex('move')">↗️ DESPLAZAR CURSOR</button>
                </div>

                <div class="section-title">TELEMETRÍA DE ENLACE EN TIEMPO REAL</div>
                <div class="terminal" id="term">Enlace establecido con nodo móvil principal...</div>

                <script>
                    function log(msg) {
                        const t = document.getElementById('term');
                        t.innerHTML += '<br>' + new Date().toLocaleTimeString() + ' ' + msg;
                        t.scrollTop = t.scrollHeight;
                    }
                    function sendTv(cmd) {
                        fetch('/api/tv/' + cmd).then(r => r.json()).then(d => log('Comando TV emitido: ' + cmd));
                    }
                    function sendDex(action) {
                        fetch('/api/dex/' + action).then(r => r.json()).then(d => log('Acción Omni-DeX: ' + action));
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=utf-8")
        writer.println("Access-Control-Allow-Origin: *")
        writer.println("Content-Length: ${html.toByteArray().size}")
        writer.println()
        writer.println(html)
    }

    private fun resolveLocalIpAddress(): String {
        try {
            val interfaces = java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = java.util.Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr.address.size == 4) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }

    private fun addLog(message: String) {
        val updated = (_state.value.clientLogs + message).takeLast(20)
        _state.value = _state.value.copy(clientLogs = updated)
    }
}
