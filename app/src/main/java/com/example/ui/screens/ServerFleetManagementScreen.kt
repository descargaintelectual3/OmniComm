package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.remote.*
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import kotlinx.coroutines.launch

enum class ServerFleetTab(val title: String, val icon: ImageVector) {
    SERVERS_LIST("Nodos & Salud", Icons.Default.Dns),
    TERMINAL_SHELL("Consola & Shell", Icons.Default.PlayArrow),
    SERVICES_PROCESSES("Servicios & Daemons", Icons.Default.List),
    BI_BRIDGE_ALERTS("Enlace & Alarmas", Icons.Default.Notifications),
    AGENT_INSTALL("Instalador Agente", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFleetManagementScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val engine = remember { ServerFleetManagementEngine.getInstance(context) }
    val serverControl = remember { TacticalExternalControlServer.getInstance(context) }

    val servers by engine.serverNodes.collectAsState()
    val executionHistory by engine.executionHistory.collectAsState()
    val incomingAlerts by engine.incomingServerAlerts.collectAsState()
    val localServerState by serverControl.serverState.collectAsState()

    var selectedTab by remember { mutableStateOf(ServerFleetTab.SERVERS_LIST) }
    var selectedServerId by remember { mutableStateOf(servers.firstOrNull()?.id ?: "") }
    var showAddServerDialog by remember { mutableStateOf(false) }

    val selectedServer = remember(servers, selectedServerId) {
        servers.firstOrNull { it.id == selectedServerId } ?: servers.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_servers")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = TacticalCyanPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "MANDO DE SERVIDORES LINUX & WINDOWS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TacticalEmeraldSecondary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                            ) {
                                Text(
                                    text = "${servers.size} NODOS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalEmeraldSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Control Total Bi-direccional • SSH / WinRM / REST Agent / PowerShell",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddServerDialog = true },
                        modifier = Modifier.testTag("btn_add_server")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Agregar Servidor",
                            tint = TacticalEmeraldSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F17))
            )
        },
        containerColor = Color(0xFF0B0F17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Ejecutivo con Métricas Globales de Infraestructura
            ServerFleetExecutiveHeader(
                totalServers = servers.size,
                linuxCount = servers.count { it.osType == ServerOsType.LINUX },
                windowsCount = servers.count { it.osType == ServerOsType.WINDOWS },
                alertsCount = incomingAlerts.size
            )

            // Selector de Pestañas
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF161B22),
                contentColor = TacticalCyanPrimary,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                ServerFleetTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (selectedTab == tab) TacticalCyanPrimary else Color.Gray
                                )
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) TacticalCyanPrimary else Color.LightGray
                                )
                            }
                        }
                    )
                }
            }

            // Contenido según pestaña
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    ServerFleetTab.SERVERS_LIST -> {
                        ServersListTabContent(
                            servers = servers,
                            onSelectServer = { srv ->
                                selectedServerId = srv.id
                                selectedTab = ServerFleetTab.TERMINAL_SHELL
                            },
                            onOpenServices = { srv ->
                                selectedServerId = srv.id
                                selectedTab = ServerFleetTab.SERVICES_PROCESSES
                            },
                            onDeleteServer = { srv ->
                                engine.removeServer(srv.id)
                                Toast.makeText(context, "Servidor ${srv.name} eliminado", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    ServerFleetTab.TERMINAL_SHELL -> {
                        TerminalShellTabContent(
                            servers = servers,
                            selectedServer = selectedServer,
                            onSelectServer = { selectedServerId = it.id },
                            history = executionHistory,
                            onExecuteCommand = { cmd ->
                                selectedServer?.let { srv ->
                                    scope.launch {
                                        engine.executeCommandOnServer(srv.id, cmd)
                                    }
                                }
                            }
                        )
                    }

                    ServerFleetTab.SERVICES_PROCESSES -> {
                        ServicesAndProcessesTabContent(
                            servers = servers,
                            selectedServer = selectedServer,
                            onSelectServer = { selectedServerId = it.id },
                            onServiceAction = { svcName, action ->
                                selectedServer?.let { srv ->
                                    scope.launch {
                                        val ok = engine.controlService(srv.id, svcName, action)
                                        Toast.makeText(context, if (ok) "Servicio $svcName: $action OK" else "Error en servicio", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onTerminateProcess = { pid ->
                                selectedServer?.let { srv ->
                                    scope.launch {
                                        val ok = engine.terminateProcess(srv.id, pid)
                                        Toast.makeText(context, if (ok) "Proceso PID $pid terminado" else "Error al terminar proceso", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    ServerFleetTab.BI_BRIDGE_ALERTS -> {
                        BiDirectionalBridgeTabContent(
                            alerts = incomingAlerts,
                            onSimulateCriticalAlert = {
                                engine.receiveServerAlert(
                                    serverId = selectedServer?.id ?: "SRV-DEMO",
                                    alertText = "CRITICAL: Consumo de CPU al 98% por proceso runaway en puerto 443",
                                    severity = "CRITICAL"
                                )
                                Toast.makeText(context, "Alerta Crítica recibida. Vibración táctica activada.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    ServerFleetTab.AGENT_INSTALL -> {
                        AgentInstallScriptsTabContent(
                            localIp = localServerState.localIp,
                            localPort = localServerState.port,
                            engine = engine
                        )
                    }
                }
            }
        }
    }

    if (showAddServerDialog) {
        AddServerDialog(
            onDismiss = { showAddServerDialog = false },
            onAdd = { node ->
                engine.registerServer(node)
                selectedServerId = node.id
                showAddServerDialog = false
                Toast.makeText(context, "Servidor ${node.name} añadido con éxito", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ServerFleetExecutiveHeader(
    totalServers: Int,
    linuxCount: Int,
    windowsCount: Int,
    alertsCount: Int
) {
    Surface(
        color = Color(0xFF0F141C),
        border = BorderStroke(1.dp, Color(0xFF21262D)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExecutiveStatBadge("FLOTA TOTAL", "$totalServers Nodos", TacticalCyanPrimary)
            ExecutiveStatBadge("LINUX (SSH/BASH)", "$linuxCount Activos", TacticalAmberTertiary)
            ExecutiveStatBadge("WINDOWS (WinRM)", "$windowsCount Activos", Color(0xFF80D8FF))
            ExecutiveStatBadge("ALARMAS ENLACE", "$alertsCount Registros", if (alertsCount > 0) Color(0xFFFF5252) else TacticalEmeraldSecondary)
        }
    }
}

@Composable
fun ExecutiveStatBadge(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            fontSize = 8.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ServersListTabContent(
    servers: List<RemoteServerNode>,
    onSelectServer: (RemoteServerNode) -> Unit,
    onOpenServices: (RemoteServerNode) -> Unit,
    onDeleteServer: (RemoteServerNode) -> Unit
) {
    if (servers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay servidores registrados. Presiona '+' para agregar uno.", color = Color.Gray, fontSize = 12.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(servers) { server ->
                ServerNodeCard(
                    server = server,
                    onOpenTerminal = { onSelectServer(server) },
                    onOpenServices = { onOpenServices(server) },
                    onDelete = { onDeleteServer(server) }
                )
            }
        }
    }
}

@Composable
fun ServerNodeCard(
    server: RemoteServerNode,
    onOpenTerminal: () -> Unit,
    onOpenServices: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, if (server.healthStatus == ServerHealthStatus.ALERT_TRIGGERED) Color(0xFFFF5252) else Color(0xFF30363D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Fila Superior: OS Icon, Nombre, IP:Port y Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (server.osType == ServerOsType.LINUX) TacticalAmberTertiary.copy(alpha = 0.2f) else Color(0xFF80D8FF).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (server.osType == ServerOsType.LINUX) TacticalAmberTertiary else Color(0xFF80D8FF))
                    ) {
                        Text(
                            text = server.osType.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (server.osType == ServerOsType.LINUX) TacticalAmberTertiary else Color(0xFF80D8FF),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Column {
                        Text(
                            text = server.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${server.username}@${server.hostnameOrIp}:${server.port} • ${server.authProtocol.name}",
                            fontSize = 9.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (server.healthStatus) {
                        ServerHealthStatus.ONLINE -> TacticalEmeraldSecondary.copy(alpha = 0.2f)
                        ServerHealthStatus.DEGRADED -> TacticalAmberTertiary.copy(alpha = 0.2f)
                        ServerHealthStatus.ALERT_TRIGGERED -> Color(0xFFFF5252).copy(alpha = 0.2f)
                        ServerHealthStatus.OFFLINE -> Color.Gray.copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = server.healthStatus.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (server.healthStatus) {
                            ServerHealthStatus.ONLINE -> TacticalEmeraldSecondary
                            ServerHealthStatus.DEGRADED -> TacticalAmberTertiary
                            ServerHealthStatus.ALERT_TRIGGERED -> Color(0xFFFF5252)
                            ServerHealthStatus.OFFLINE -> Color.Gray
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = server.osVersionDetails,
                fontSize = 9.sp,
                color = Color.LightGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Divider(color = Color(0xFF30363D))

            // Gauges de Rendimiento (CPU, RAM, Disco, Ping, Uptime)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ResourceGauge(label = "CPU", valuePercent = server.cpuUsagePercent, valueStr = "${server.cpuUsagePercent}%")
                ResourceGauge(label = "RAM", valuePercent = server.ramUsagePercent, valueStr = "${server.ramUsagePercent}%")
                ResourceGauge(label = "DISCO", valuePercent = server.diskUsagePercent, valueStr = "${server.diskUsagePercent}%")
                ResourceGauge(label = "PING", valuePercent = (server.pingLatencyMs / 2).toFloat().coerceIn(0f, 100f), valueStr = "${server.pingLatencyMs}ms")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Uptime: ${server.uptimeFormatted}",
                    fontSize = 9.sp,
                    color = TacticalCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Servicios Activos: ${server.activeServices.count { it.isRunning }} / ${server.activeServices.size}",
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            // Botones de Acción Rápida
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onOpenTerminal,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Consola Shell", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenServices,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalAmberTertiary),
                    border = BorderStroke(1.dp, TacticalAmberTertiary),
                    modifier = Modifier.weight(1f).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(12.dp), tint = TacticalAmberTertiary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Daemons & Serv", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ResourceGauge(label: String, valuePercent: Float, valueStr: String) {
    val barColor = when {
        valuePercent > 85f -> Color(0xFFFF5252)
        valuePercent > 65f -> TacticalAmberTertiary
        else -> TacticalEmeraldSecondary
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Text(text = label, fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(text = valueStr, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = barColor, fontFamily = FontFamily.Monospace)
        LinearProgressIndicator(
            progress = { (valuePercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = Color(0xFF21262D)
        )
    }
}

@Composable
fun TerminalShellTabContent(
    servers: List<RemoteServerNode>,
    selectedServer: RemoteServerNode?,
    onSelectServer: (RemoteServerNode) -> Unit,
    history: List<CommandExecutionResult>,
    onExecuteCommand: (String) -> Unit
) {
    var commandInput by remember { mutableStateOf("") }
    val isLinux = selectedServer?.osType == ServerOsType.LINUX

    val quickCommands = remember(isLinux) {
        if (isLinux) {
            listOf("uname -a", "top -b -n 1", "df -h", "free -m", "docker ps", "systemctl status nginx", "iptables -L -n", "uptime")
        } else {
            listOf("Get-ComputerInfo", "Get-Process", "Get-Service", "Get-EventLog -LogName System -Newest 5", "systeminfo")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Selector de Servidor Activo en la Consola
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            servers.forEach { srv ->
                val isSelected = srv.id == selectedServer?.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.2f) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF30363D)),
                    modifier = Modifier.clickable { onSelectServer(srv) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (srv.osType == ServerOsType.LINUX) TacticalAmberTertiary else Color(0xFF80D8FF),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Text(
                            text = "${srv.name} (${srv.osType.name})",
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TacticalCyanPrimary else Color.LightGray
                        )
                    }
                }
            }
        }

        // Chips de Comandos Rápidos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            quickCommands.forEach { qCmd ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF21262D),
                    modifier = Modifier.clickable {
                        commandInput = qCmd
                        onExecuteCommand(qCmd)
                    }
                ) {
                    Text(
                        text = qCmd,
                        fontSize = 9.sp,
                        color = TacticalEmeraldSecondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Output de la Terminal con Historial
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val serverHistory = history.filter { it.serverId == selectedServer?.id }
            if (serverHistory.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Terminal ${if (isLinux) "Bash SSH" else "PowerShell WinRM"} lista.\nSelecciona un comando rápido o escribe uno abajo.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(serverHistory) { item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$ ${item.command}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalCyanPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Code: ${item.exitCode} (${item.durationMs}ms)",
                                    fontSize = 8.sp,
                                    color = if (item.exitCode == 0) TacticalEmeraldSecondary else Color(0xFFFF5252),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (item.stdout.isNotEmpty()) {
                                Text(
                                    text = item.stdout,
                                    fontSize = 9.sp,
                                    color = Color(0xFFE6EDF3),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (item.stderr.isNotEmpty()) {
                                Text(
                                    text = "[STDERR]: ${item.stderr}",
                                    fontSize = 9.sp,
                                    color = Color(0xFFFF80AB),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Divider(color = Color(0xFF21262D), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        // Input de Línea de Comandos
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("input_server_cmd"),
                placeholder = {
                    Text(
                        if (isLinux) "Comando Bash (ej: top, systemctl restart nginx)..." else "Comando PowerShell (ej: Get-Process)...",
                        fontSize = 10.sp
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalCyanPrimary,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onExecuteCommand(commandInput)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                modifier = Modifier.height(48.dp).testTag("btn_execute_server_cmd")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Ejecutar", tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ServicesAndProcessesTabContent(
    servers: List<RemoteServerNode>,
    selectedServer: RemoteServerNode?,
    onSelectServer: (RemoteServerNode) -> Unit,
    onServiceAction: (String, String) -> Unit,
    onTerminateProcess: (Int) -> Unit
) {
    var subTab by remember { mutableStateOf(0) } // 0: Servicios, 1: Procesos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Selector de Servidor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            servers.forEach { srv ->
                val isSelected = srv.id == selectedServer?.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TacticalAmberTertiary.copy(alpha = 0.2f) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (isSelected) TacticalAmberTertiary else Color(0xFF30363D)),
                    modifier = Modifier.clickable { onSelectServer(srv) }
                ) {
                    Text(
                        text = srv.name,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TacticalAmberTertiary else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Subtabs: Servicios / Procesos
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color(0xFF161B22),
            contentColor = TacticalAmberTertiary
        ) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Servicios del Sistema (${selectedServer?.activeServices?.size ?: 0})", fontSize = 11.sp) })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Procesos en Ejecución (${selectedServer?.topProcesses?.size ?: 0})", fontSize = 11.sp) })
        }

        if (subTab == 0) {
            val services = selectedServer?.activeServices ?: emptyList()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(services) { svc ->
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, Color(0xFF30363D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (svc.isRunning) TacticalEmeraldSecondary else Color.Gray,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                    Text(text = svc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                                Text(text = svc.displayName, fontSize = 9.sp, color = Color.LightGray)
                                Text(text = "PID: ${if (svc.isRunning) svc.pid else "-"} • Memoria: ${svc.memoryUsageMb} MB", fontSize = 8.sp, color = Color.Gray)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onServiceAction(svc.name, "restart") },
                                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Reiniciar", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                if (svc.isRunning) {
                                    OutlinedButton(
                                        onClick = { onServiceAction(svc.name, "stop") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                                        border = BorderStroke(1.dp, Color(0xFFFF5252)),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Stop", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onServiceAction(svc.name, "start") },
                                        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Start", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val procs = selectedServer?.topProcesses ?: emptyList()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(procs) { p ->
                    Card(
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, Color(0xFF30363D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${p.name} (PID ${p.pid})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary, fontFamily = FontFamily.Monospace)
                                Text(text = "Usuario: ${p.user} • CPU: ${p.cpuPercent}% • RAM: ${p.memoryMb} MB", fontSize = 9.sp, color = Color.LightGray)
                            }

                            Button(
                                onClick = { onTerminateProcess(p.pid) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Kill", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BiDirectionalBridgeTabContent(
    alerts: List<String>,
    onSimulateCriticalAlert: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalEmeraldSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ENLACE BI-DIRECCIONAL SERVIDOR -> MÓVIL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TacticalEmeraldSecondary)
                Text(
                    text = "Permite a los servidores Linux y Windows enviar alertas instantáneas a tu teléfono. Si una alerta es crítica, activará automáticamente el motor de vibración háptica y el registro forense.",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )

                Button(
                    onClick = onSimulateCriticalAlert,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simular Alerta Crítica desde Servidor (Test Háptico)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Text("Registro de Alertas Recibidas (${alerts.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (alerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay alarmas pendientes recibidas de servidores.", color = Color.Gray, fontSize = 10.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(alerts) { alertText ->
                        Text(
                            text = alertText,
                            fontSize = 9.sp,
                            color = if (alertText.contains("CRITICAL")) Color(0xFFFF5252) else TacticalAmberTertiary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentInstallScriptsTabContent(
    localIp: String,
    localPort: Int,
    engine: ServerFleetManagementEngine
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var selectedOs by remember { mutableStateOf(ServerOsType.LINUX) }

    val script = remember(selectedOs, localIp, localPort) {
        if (selectedOs == ServerOsType.LINUX) {
            engine.generateLinuxAgentInstallScript(localIp, localPort)
        } else {
            engine.generateWindowsAgentInstallScript(localIp, localPort)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalCyanPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("INSTALADOR DE AGENTE EN 1-CLIC (ZERO CONFIG)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                Text(
                    text = "Copia este script y ejecútalo en la consola de tu servidor para enlazarlo con tu teléfono al instante mediante HTTP Heartbeat y control remoto.",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { selectedOs = ServerOsType.LINUX },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedOs == ServerOsType.LINUX) TacticalAmberTertiary else Color(0xFF21262D)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Linux (Bash / Curl)", fontSize = 10.sp, color = if (selectedOs == ServerOsType.LINUX) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { selectedOs = ServerOsType.WINDOWS },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selectedOs == ServerOsType.WINDOWS) Color(0xFF80D8FF) else Color(0xFF21262D)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Windows (PowerShell)", fontSize = 10.sp, color = if (selectedOs == ServerOsType.WINDOWS) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = script,
                fontSize = 9.sp,
                color = Color(0xFFE6EDF3),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(8.dp)
            )
        }

        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(script))
                Toast.makeText(context, "Script copiado al portapapeles.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copiar Script para Servidor Remoto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAdd: (RemoteServerNode) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var portStr by remember { mutableStateOf("22") }
    var osType by remember { mutableStateOf(ServerOsType.LINUX) }
    var username by remember { mutableStateOf("root") }
    var authProtocol by remember { mutableStateOf(ServerAuthProtocol.SSH_KEY) }
    var tokenOrKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Nuevo Servidor Remoto", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Servidor (ej: Web Core 01)", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            osType = ServerOsType.LINUX
                            portStr = "22"
                            username = "root"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (osType == ServerOsType.LINUX) TacticalAmberTertiary else Color(0xFF21262D)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Linux", fontSize = 10.sp, color = if (osType == ServerOsType.LINUX) Color.Black else Color.White)
                    }

                    Button(
                        onClick = {
                            osType = ServerOsType.WINDOWS
                            portStr = "5985"
                            username = "Administrator"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (osType == ServerOsType.WINDOWS) Color(0xFF80D8FF) else Color(0xFF21262D)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Windows", fontSize = 10.sp, color = if (osType == ServerOsType.WINDOWS) Color.Black else Color.White)
                    }
                }

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Dirección IP o Hostname", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Puerto", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tokenOrKey,
                    onValueChange = { tokenOrKey = it },
                    label = { Text("Token / Contraseña / Clave Privada", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (host.isNotBlank()) {
                        val node = RemoteServerNode(
                            name = name.ifBlank { "Servidor $host" },
                            hostnameOrIp = host,
                            port = portStr.toIntOrNull() ?: if (osType == ServerOsType.LINUX) 22 else 5985,
                            osType = osType,
                            username = username,
                            authProtocol = authProtocol,
                            authTokenOrKey = tokenOrKey
                        )
                        onAdd(node)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
            ) {
                Text("Guardar Servidor", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray, fontSize = 11.sp)
            }
        },
        containerColor = Color(0xFF161B22)
    )
}
