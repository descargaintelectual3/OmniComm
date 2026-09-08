package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.models.*
import com.example.ui.components.RealtimeMeshHealthOverlay
import com.example.ui.components.ScreenMirrorViewerDialog
import com.example.ui.components.SessionHandoffBanner
import com.example.ui.components.TacticalToastHost
import com.example.ui.components.UnifiedRemoteControlDialog
import com.example.ui.theme.*
import com.example.ui.viewmodels.RadarViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.domain.c4isr.CursorOnTargetTranscoder
import com.example.domain.c4isr.CotEvent
import com.example.domain.models.TacticalMBTilesManager
import com.example.domain.models.TacticalMBTilesPackage
import com.example.domain.models.MapLayerType
import com.example.domain.p2p.BleTacticalProximityTransport
import com.example.domain.c2.UniversalMeshC2Protocol
import com.example.domain.c2.C2HardwareAction
import com.example.domain.c2.BleGattUniversalDeviceManager
import com.example.domain.c2.LineOfSightElevationEngine
import com.example.domain.sensors.MavlinkDroneTelemetryEngine
import com.example.domain.media.DataOverSoundAcousticModem
import com.example.ui.components.TacticalAdvancedC2SuiteDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    onBack: () -> Unit,
    viewModel: RadarViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val networkNodes by viewModel.networkNodes.collectAsStateWithLifecycle()
    val systemEvents by viewModel.systemEvents.collectAsStateWithLifecycle()
    val activeTasks by viewModel.activeSyncTasks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isSyncAllRunning by viewModel.isSyncAllRunning.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()
    val eventFilter by viewModel.selectedEventFilter.collectAsStateWithLifecycle()
    val networkHealth by viewModel.networkHealthScore.collectAsStateWithLifecycle()
    val localUserName by viewModel.localUserName.collectAsStateWithLifecycle()
    val localUserAvatar by viewModel.localUserAvatar.collectAsStateWithLifecycle()
    val heartbeatMode by viewModel.heartbeatModeLabel.collectAsStateWithLifecycle()

    val currentFrame by viewModel.currentScreenFrame.collectAsStateWithLifecycle()
    val tunnelStats by viewModel.tunnelStats.collectAsStateWithLifecycle()
    val activeHandoffSession by viewModel.activeHandoffSession.collectAsStateWithLifecycle()

    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showIdentityDialog by remember { mutableStateOf(false) }
    var showHealthDashboardOverlay by remember { mutableStateOf(false) }
    var nodeForRemoteControl by remember { mutableStateOf<NetworkNode?>(null) }
    var nodeForScreenMirror by remember { mutableStateOf<NetworkNode?>(null) }

    var currentToast by remember { mutableStateOf<TacticalToastNotification?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { toast ->
            currentToast = toast
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Topología Malla & Auto-Conexión", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                modifier = Modifier.clickable { showHealthDashboardOverlay = true }
                            ) {
                                Text(
                                    text = "$networkHealth% Salud 📊",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$localUserAvatar $localUserName • ${networkNodes.size} Nodos",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("radar_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Botón de Dashboard de Salud en Vivo
                    IconButton(
                        onClick = { showHealthDashboardOverlay = true },
                        modifier = Modifier.testTag("open_health_dashboard_button")
                    ) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = "Salud de Malla", tint = Color(0xFF00E5FF))
                    }
                    // Botón de Sync-All Criptográfico
                    IconButton(
                        onClick = { viewModel.triggerSyncAll() },
                        modifier = Modifier.testTag("toolbar_sync_all_button")
                    ) {
                        Icon(
                            Icons.Default.SyncLock,
                            contentDescription = "Sync All",
                            tint = if (isSyncAllRunning) Color(0xFFFFD600) else MaterialTheme.colorScheme.primary
                        )
                    }
                    // Botón para personalizar Identidad Táctica (Nombre y Avatar)
                    IconButton(
                        onClick = { showIdentityDialog = true },
                        modifier = Modifier.testTag("customize_identity_button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(localUserAvatar, fontSize = 15.sp)
                            }
                        }
                    }
                    // Botón de Latido Adaptativo
                    IconButton(
                        onClick = { viewModel.triggerHeartbeat() },
                        modifier = Modifier.testTag("send_heartbeat_button")
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = "Heartbeat", tint = Color(0xFFFF5252))
                    }
                    // Botón de Escaneo Prioritario
                    IconButton(
                        onClick = { viewModel.triggerMultiRadioScan() },
                        modifier = Modifier.testTag("force_scan_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Scan",
                            tint = if (isScanning) Color(0xFF00E5FF) else LocalContentColor.current
                        )
                    }
                    // Botón para registrar nodo táctico manual
                    IconButton(
                        onClick = { showAddNodeDialog = true },
                        modifier = Modifier.testTag("add_tactical_node_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Vincular Nodo Físico", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Barra de Estado de Heartbeat Adaptativo y Salud
                Surface(
                    color = Color(0xFF0B132B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (heartbeatMode.contains("Ráfaga")) Color(0xFFFFD600) else Color(0xFF00E676),
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = heartbeatMode,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Auto-Sanación: Activa",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showHealthDashboardOverlay = true }
                            )
                        }
                    }
                }

                // Tareas Activas de Sincronización en curso
                if (activeTasks.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF1E1B4B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔄", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${activeTasks.first().taskName} con ${activeTasks.first().targetNodeName} (${activeTasks.first().progressPercent}%)",
                                    color = Color(0xFFDDD6FE),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            CircularProgressIndicator(
                                progress = { activeTasks.first().progressPercent / 100f },
                                modifier = Modifier.size(14.dp),
                                color = Color(0xFFA78BFA),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                // Banner de Handoff Multi-Dispositivo (Reanudar sesión remota)
                SessionHandoffBanner(
                    session = activeHandoffSession,
                    onResume = { session ->
                        viewModel.dismissHandoff()
                    },
                    onDismiss = { viewModel.dismissHandoff() }
                )

                // Pestañas de Navegación Visual
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Topología Malla", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_topology_graph")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Radar Táctico", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_radar_view")
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Eventos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                if (systemEvents.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${systemEvents.size}")
                                    }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_events_feed")
                    )
                    Tab(
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 },
                        text = { Text("Marcadores GIS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_gis_markers")
                    )
                    Tab(
                        selected = selectedTabIndex == 4,
                        onClick = { selectedTabIndex = 4 },
                        text = { Text("Capas (16)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_tactical_layers")
                    )
                }

                // Contenido Principal según la pestaña
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> MeshTopologyGraphView(
                            nodes = networkNodes,
                            selectedNode = selectedNode,
                            onNodeClick = { viewModel.selectNode(it) },
                            onSendHeartbeat = { viewModel.triggerHeartbeat() },
                            onPingNode = { viewModel.pingDevice(it) }
                        )
                        1 -> RadarRadialScannerView(
                            nodes = networkNodes,
                            selectedNode = selectedNode,
                            onNodeClick = { viewModel.selectNode(it) },
                            onPingNode = { viewModel.pingDevice(it) }
                        )
                        2 -> SystemEventsFeedView(
                            events = systemEvents,
                            currentFilter = eventFilter,
                            onSelectFilter = { viewModel.setEventFilter(it) },
                            onRegisterNode = { showAddNodeDialog = true }
                        )
                        3 -> TacticalGISMarkersView()
                        4 -> TacticalRadarLayersView()
                    }

                    // Modal flotante de información detallada del nodo seleccionado
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedNode != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        selectedNode?.let { node ->
                            NodeDetailCard(
                                node = node,
                                onClose = { viewModel.selectNode(null) },
                                onPing = { viewModel.pingDevice(node.id) },
                                onTogglePriority = { viewModel.togglePriority(node.id) },
                                onOpenRemoteControl = {
                                    nodeForRemoteControl = node
                                    viewModel.selectNode(null)
                                }
                            )
                        }
                    }
                }

                // Barra inferior rápida con estado del daemon de Auto-Conexión y Auto-Aviso
                MeshDaemonStatusBar(
                    nodesCount = networkNodes.size,
                    isHeartbeatActive = true,
                    onBroadcastHello = {
                        viewModel.triggerMultiRadioScan()
                    }
                )
            }

            // Notificación Táctica Flotante (Toast System)
            TacticalToastHost(
                currentToast = currentToast,
                onDismiss = { currentToast = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showHealthDashboardOverlay) {
        RealtimeMeshHealthOverlay(
            nodes = networkNodes,
            activeTasks = activeTasks,
            isSyncAllRunning = isSyncAllRunning,
            onDismiss = { showHealthDashboardOverlay = false },
            onTriggerSyncAll = { viewModel.triggerSyncAll() },
            onNodeSelect = { node ->
                viewModel.selectNode(node)
                showHealthDashboardOverlay = false
            },
            onOpenRemoteControl = { node ->
                nodeForRemoteControl = node
                showHealthDashboardOverlay = false
            }
        )
    }

    if (nodeForRemoteControl != null) {
        UnifiedRemoteControlDialog(
            node = nodeForRemoteControl!!,
            onDismiss = { nodeForRemoteControl = null },
            onExecuteAction = { action, payload ->
                viewModel.executeRemoteControl(nodeForRemoteControl!!.id, action, payload)
            },
            onOpenScreenMirror = { node ->
                nodeForScreenMirror = node
                viewModel.startScreenMirror(node.id, isBrokenScreen = true)
            }
        )
    }

    if (nodeForScreenMirror != null) {
        ScreenMirrorViewerDialog(
            node = nodeForScreenMirror!!,
            currentFrame = currentFrame,
            tunnelStats = tunnelStats,
            onDismiss = {
                viewModel.stopScreenMirror()
                nodeForScreenMirror = null
            },
            onSendTap = { x, y ->
                viewModel.sendRemoteTap(x, y)
            },
            onSendSwipe = { x1, y1, x2, y2 ->
                viewModel.sendRemoteSwipe(x1, y1, x2, y2)
            },
            onSendSystemNav = { command ->
                viewModel.sendRemoteSystemNav(command)
            },
            onExecuteAction = { action, payload ->
                viewModel.executeRemoteControl(nodeForScreenMirror!!.id, action, payload)
            }
        )
    }

    if (showAddNodeDialog) {
        RegisterTacticalNodeDialog(
            onDismiss = { showAddNodeDialog = false },
            onConfirm = { nodeId, name, model, medium, avatar ->
                viewModel.registerTacticalNode(nodeId, name, model, medium, avatar)
                showAddNodeDialog = false
            }
        )
    }

    if (showIdentityDialog) {
        CustomizeIdentityDialog(
            currentName = localUserName,
            currentAvatar = localUserAvatar,
            onDismiss = { showIdentityDialog = false },
            onSave = { name, avatar ->
                viewModel.updateIdentity(name, avatar)
                showIdentityDialog = false
            }
        )
    }
}

/**
 * 🌐 Visualizador Gráfico Interactivo de Topología de Red (Estilo D3.js Force Graph).
 */
@Composable
fun MeshTopologyGraphView(
    nodes: List<NetworkNode>,
    selectedNode: NetworkNode?,
    onNodeClick: (NetworkNode) -> Unit,
    onSendHeartbeat: () -> Unit,
    onPingNode: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "topology_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_phase"
    )

    val masterNode = nodes.find { it.isLocalMaster } ?: nodes.firstOrNull()
    val peerNodes = nodes.filterNot { it.isLocalMaster }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF0F172A),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Topología Multi-Salto P2P", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ancho de banda: 138 Mbps", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text("Cifrado: RSA+AES-256", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF020617))
                    )
                )
                .pointerInput(nodes) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = (size.width.coerceAtMost(size.height) / 2f) - 52.dp.toPx()

                        val masterOffset = Offset(centerX, centerY)
                        if ((tapOffset - masterOffset).getDistance() <= 38.dp.toPx()) {
                            masterNode?.let { onNodeClick(it) }
                            return@detectTapGestures
                        }

                        peerNodes.forEach { peer ->
                            val px = centerX + (peer.layoutX * radius)
                            val py = centerY + (peer.layoutY * radius)
                            val peerOffset = Offset(px, py)
                            if ((tapOffset - peerOffset).getDistance() <= 34.dp.toPx()) {
                                onNodeClick(peer)
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.width.coerceAtMost(size.height) / 2f) - 52.dp.toPx()

                // Círculos guía
                drawCircle(
                    color = Color(0xFF1E293B),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
                drawCircle(
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    radius = radius * 0.5f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                )

                // Enlaces
                peerNodes.forEach { peer ->
                    val peerX = center.x + (peer.layoutX * radius)
                    val peerY = center.y + (peer.layoutY * radius)
                    val peerOffset = Offset(peerX, peerY)

                    val isSelected = selectedNode?.id == peer.id
                    val linkColor = when (peer.connectionType) {
                        ConnectionType.WIFI_DIRECT -> Color(0xFF00E5FF)
                        ConnectionType.BLUETOOTH_MESH -> Color(0xFF2979FF)
                        ConnectionType.DUAL_RADIO -> Color(0xFF00E676)
                        ConnectionType.CLOUD_RELAY -> Color(0xFFFF9100)
                    }

                    drawLine(
                        color = if (isSelected) Color.White else linkColor.copy(alpha = 0.6f),
                        start = center,
                        end = peerOffset,
                        strokeWidth = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), flowPhase),
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = linkColor.copy(alpha = 0.2f),
                        start = center,
                        end = peerOffset,
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Interconexiones secundarias
                for (i in peerNodes.indices) {
                    val nextIndex = (i + 1) % peerNodes.size
                    if (peerNodes.size > 1 && nextIndex != i) {
                        val n1 = peerNodes[i]
                        val n2 = peerNodes[nextIndex]
                        val p1 = Offset(center.x + (n1.layoutX * radius), center.y + (n1.layoutY * radius))
                        val p2 = Offset(center.x + (n2.layoutX * radius), center.y + (n2.layoutY * radius))
                        drawLine(
                            color = Color(0xFF334155).copy(alpha = 0.4f),
                            start = p1,
                            end = p2,
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }
                }

                // Nodos pares
                peerNodes.forEach { peer ->
                    val peerX = center.x + (peer.layoutX * radius)
                    val peerY = center.y + (peer.layoutY * radius)
                    val peerOffset = Offset(peerX, peerY)
                    val isSelected = selectedNode?.id == peer.id

                    val nodeColor = when (peer.connectionType) {
                        ConnectionType.WIFI_DIRECT -> Color(0xFF00E5FF)
                        ConnectionType.BLUETOOTH_MESH -> Color(0xFF2979FF)
                        ConnectionType.DUAL_RADIO -> Color(0xFF00E676)
                        ConnectionType.CLOUD_RELAY -> Color(0xFFFF9100)
                    }

                    drawCircle(
                        color = nodeColor.copy(alpha = 0.25f),
                        radius = (24.dp.toPx() * (if (isSelected) pulseScale * 1.1f else 1.0f)),
                        center = peerOffset
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(nodeColor, nodeColor.copy(alpha = 0.8f)),
                            center = peerOffset,
                            radius = 18.dp.toPx()
                        ),
                        radius = if (isSelected) 22.dp.toPx() else 18.dp.toPx(),
                        center = peerOffset
                    )

                    if (peer.isPriority) {
                        drawCircle(
                            color = Color(0xFFFFD600),
                            radius = 24.dp.toPx(),
                            center = peerOffset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    } else if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 24.dp.toPx(),
                            center = peerOffset,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    val avatarPaint = android.graphics.Paint().apply {
                        textSize = 34f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        peer.avatarIcon,
                        peerX,
                        peerY + 12f,
                        avatarPaint
                    )

                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 26f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val subPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.LTGRAY
                        textSize = 20f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    val priorityPrefix = if (peer.isPriority) "⭐ " else ""
                    drawContext.canvas.nativeCanvas.drawText(
                        priorityPrefix + peer.name.take(12),
                        peerX,
                        peerY + 36.dp.toPx(),
                        textPaint
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "🔋 ${peer.batteryLevel}% • ${peer.latencyMs}ms",
                        peerX,
                        peerY + 48.dp.toPx(),
                        subPaint
                    )
                }

                // Maestro Central
                masterNode?.let { master ->
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.2f),
                        radius = 44.dp.toPx() * pulseScale,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF00E676).copy(alpha = 0.1f),
                        radius = 62.dp.toPx() * pulseScale,
                        center = center
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E676), Color(0xFF00897B)),
                            center = center,
                            radius = 30.dp.toPx()
                        ),
                        radius = 30.dp.toPx(),
                        center = center
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 30.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    val masterAvatarPaint = android.graphics.Paint().apply {
                        textSize = 46f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        master.avatarIcon,
                        center.x,
                        center.y + 16f,
                        masterAvatarPaint
                    )

                    val masterTextPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val masterSubPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 22f
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    drawContext.canvas.nativeCanvas.drawText(master.name.take(16), center.x, center.y + 46.dp.toPx(), masterTextPaint)
                    drawContext.canvas.nativeCanvas.drawText("🔋 ${master.batteryLevel}% • Maestro", center.x, center.y + 58.dp.toPx(), masterSubPaint)
                }
            }
        }
    }
}

/**
 * 🎯 Vista del Radar Táctico Radial.
 */
@Composable
fun RadarRadialScannerView(
    nodes: List<NetworkNode>,
    selectedNode: NetworkNode?,
    onNodeClick: (NetworkNode) -> Unit,
    onPingNode: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF02101E)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = (size.width.coerceAtMost(size.height) / 2) - 32.dp.toPx()

                for (i in 1..4) {
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                        radius = maxRadius * (i / 4f),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )

                val sweepBrush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0f),
                        Color(0xFF00E5FF).copy(alpha = 0.6f)
                    ),
                    center = center
                )

                rotate(angle, center) {
                    drawArc(
                        brush = sweepBrush,
                        startAngle = -90f,
                        sweepAngle = 90f,
                        useCenter = true,
                        topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                        size = Size(maxRadius * 2, maxRadius * 2)
                    )
                }

                nodes.forEach { node ->
                    val distanceRatio = (1f - ((node.rssi + 100) / 70f)).coerceIn(0.1f, 0.95f)
                    val distance = maxRadius * distanceRatio
                    val angleRad = (Math.abs(node.id.hashCode()) % 360) * (Math.PI / 180f)

                    val nodeX = center.x + distance * kotlin.math.cos(angleRad).toFloat()
                    val nodeY = center.y + distance * kotlin.math.sin(angleRad).toFloat()
                    val nodeOffset = Offset(nodeX, nodeY)

                    val color = if (node.isLocalMaster) Color(0xFF00E676) else Color(0xFF00E5FF)

                    drawCircle(color = color.copy(alpha = 0.35f), radius = 18.dp.toPx(), center = nodeOffset)
                    drawCircle(color = color, radius = 8.dp.toPx(), center = nodeOffset)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nodes) { node ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNodeClick(node) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(node.avatarIcon, fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(node.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (node.isPriority) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("⭐", fontSize = 12.sp)
                                        }
                                    }
                                    Text("🔋 ${node.batteryLevel}% • ${node.connectionType.label} • ${node.rssi} dBm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onPingNode(node.id) }) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Ping", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 📜 Registro en Vivo de Eventos del Sistema.
 */
@Composable
fun SystemEventsFeedView(
    events: List<MeshSystemEvent>,
    currentFilter: SystemEventType?,
    onSelectFilter: (SystemEventType?) -> Unit,
    onRegisterNode: () -> Unit
) {
    val filteredEvents = remember(events, currentFilter) {
        if (currentFilter == null) events else events.filter { it.type == currentFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = currentFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("Todos (${events.size})", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == SystemEventType.TRUST_ESTABLISHED,
                    onClick = { onSelectFilter(if (currentFilter == SystemEventType.TRUST_ESTABLISHED) null else SystemEventType.TRUST_ESTABLISHED) },
                    label = { Text("👋 Bienvenida & Confianza", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == SystemEventType.KEY_EXCHANGE,
                    onClick = { onSelectFilter(if (currentFilter == SystemEventType.KEY_EXCHANGE) null else SystemEventType.KEY_EXCHANGE) },
                    label = { Text("🔐 Intercambio de Claves", fontSize = 11.sp) }
                )
            }
            item {
                FilterChip(
                    selected = currentFilter == SystemEventType.HEARTBEAT,
                    onClick = { onSelectFilter(if (currentFilter == SystemEventType.HEARTBEAT) null else SystemEventType.HEARTBEAT) },
                    label = { Text("💓 Heartbeats", fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hay eventos registrados para este filtro.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    SystemEventTimelineCard(event)
                }
            }
        }
    }
}

@Composable
fun SystemEventTimelineCard(event: MeshSystemEvent) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(event.timestamp))
    val badgeColor = Color(event.type.badgeColorHex)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (event.type) {
                        SystemEventType.DISCOVERY -> Icons.Default.Radar
                        SystemEventType.HANDSHAKE_INIT -> Icons.Default.WavingHand
                        SystemEventType.KEY_EXCHANGE -> Icons.Default.Key
                        SystemEventType.HEARTBEAT -> Icons.Default.Favorite
                        SystemEventType.TRUST_ESTABLISHED -> Icons.Default.CheckCircle
                        SystemEventType.STORAGE_SYNC -> Icons.Default.FolderShared
                        SystemEventType.WARNING -> Icons.Default.Info
                    }
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.details,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = event.medium,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (event.sourceNodeName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${event.sourceNodeName}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 📇 Tarjeta Flotante Interactiva de Detalles del Nodo Seleccionado.
 */
@Composable
fun NodeDetailCard(
    node: NetworkNode,
    onClose: () -> Unit,
    onPing: () -> Unit,
    onTogglePriority: () -> Unit,
    onOpenRemoteControl: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val lastSyncTime = timeFormat.format(Date(node.lastHeartbeatTimestamp))

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(node.avatarIcon, fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(node.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (node.isPriority) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFD600).copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        "⭐ Prioritario",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFB300),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text("${node.model} • ${node.connectionType.label}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (node.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                            contentDescription = null,
                            tint = if (node.batteryLevel > 30) Color(0xFF00E676) else Color(0xFFFF5252),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Batería: ${node.batteryLevel}% ${if (node.isCharging) "(Cargando)" else ""}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Última Sinc: $lastSyncTime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip("Señal RSSI", "${node.rssi} dBm")
                MetricChip("Latencia", "${node.latencyMs} ms")
                MetricChip("Throughput", "${node.throughputMbps} Mbps")
                MetricChip("Accesos", "${node.accessCount}")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Historial de Conexiones
            Text("Historial de Conexiones & Handshakes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                node.connectionHistory.take(2).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item.eventTitle, fontSize = 11.sp, maxLines = 1)
                        }
                        Text("${item.latencyMs}ms • ${item.throughputMbps} Mbps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Acciones del Nodo (Control Total, Prioridad en Room + Ping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!node.isLocalMaster) {
                    Button(
                        onClick = onOpenRemoteControl,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f).height(38.dp)
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Control Total", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onTogglePriority,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (node.isPriority) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (node.isPriority) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (node.isPriority) "Prioritario" else "Priorizar", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = onPing,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(0.9f).height(38.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ping", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Barra inferior persistente de estado del daemon de la malla.
 */
@Composable
fun MeshDaemonStatusBar(
    nodesCount: Int,
    isHeartbeatActive: Boolean,
    onBroadcastHello: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF00E676),
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daemon Activo: Auto-Conexión, Control & Auto-Sanación",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            FilledTonalButton(
                onClick = onBroadcastHello,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("scan_mesh_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Escanear Malla", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Diálogo para personalizar el Nombre del Dispositivo y el Avatar Táctico.
 */
@Composable
fun CustomizeIdentityDialog(
    currentName: String,
    currentAvatar: String,
    onDismiss: () -> Unit,
    onSave: (name: String, avatar: String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var selectedAvatar by remember { mutableStateOf(currentAvatar) }

    val avatars = listOf("🦅", "🐺", "⚡", "🛡️", "🛰️", "⚓", "🛸", "🤖", "🎯", "🚀")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalizar Identidad Táctica", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Esta identidad (Nombre y Avatar) se comparte de forma autónoma durante el protocolo de Handshake para identificarte en la topología de la malla.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre o Distintivo de Llamada") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Seleccionar Avatar Táctico:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatars) { avatar ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedAvatar == avatar) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedAvatar == avatar) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { selectedAvatar = avatar }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(avatar, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, selectedAvatar) },
                modifier = Modifier.testTag("save_identity_button")
            ) {
                Text("Guardar y Anunciar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo para vincular y registrar un nodo táctico físico o baliza de hardware.
 */
@Composable
fun RegisterTacticalNodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (nodeId: String, name: String, model: String, medium: ConnectionType, avatar: String) -> Unit
) {
    var nodeId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("Terminal Táctico") }
    var deviceModel by remember { mutableStateOf("Radio Mesh Unit") }
    var selectedMedium by remember { mutableStateOf(ConnectionType.DUAL_RADIO) }
    var selectedAvatar by remember { mutableStateOf("🛡️") }

    val avatars = listOf("🛡️", "⚡", "🐺", "🦅", "🛰️", "🚀")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vincular Nodo Físico / Baliza") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Registra manualmente un terminal o repetidor radioeléctrico. El servicio ejecutará el handshake autenticado, validará su clave y lo asociará a la malla.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nodeId,
                    onValueChange = { nodeId = it },
                    label = { Text("ID de Nodo / Dirección MAC (Opcional)") },
                    placeholder = { Text("Ej: AA:BB:CC:11:22:33 o NODE-9021") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("node_id_input")
                )
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Nombre / Alias del Dispositivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("node_name_input")
                )
                OutlinedTextField(
                    value = deviceModel,
                    onValueChange = { deviceModel = it },
                    label = { Text("Modelo de Hardware") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("node_model_input")
                )

                Text("Avatar Táctico:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    avatars.forEach { avatar ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedAvatar == avatar) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedAvatar == avatar) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { selectedAvatar = avatar }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(avatar, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Text("Medio de Enlace:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedMedium == ConnectionType.WIFI_DIRECT,
                        onClick = { selectedMedium = ConnectionType.WIFI_DIRECT },
                        label = { Text("Wi-Fi Direct", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedMedium == ConnectionType.BLUETOOTH_MESH,
                        onClick = { selectedMedium = ConnectionType.BLUETOOTH_MESH },
                        label = { Text("BLE Mesh", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedMedium == ConnectionType.DUAL_RADIO,
                        onClick = { selectedMedium = ConnectionType.DUAL_RADIO },
                        label = { Text("Dual Radio", fontSize = 11.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nodeId, deviceName, deviceModel, selectedMedium, selectedAvatar) },
                modifier = Modifier.testTag("confirm_register_tactical_node")
            ) {
                Text("Vincular & Autenticar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TacticalGISMarkersView() {
    val gisManager = remember { com.example.domain.p2p.TacticalGISManager.getInstance() }
    val markers by gisManager.markers.collectAsStateWithLifecycle()
    val selectedMarker by gisManager.selectedMarker.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Marcadores de Zona & Perímetros", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text("${markers.size} puntos tácticos activos en la malla", fontSize = 11.sp, color = Color.LightGray)
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Añadir Punto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(markers) { marker ->
                val isSelected = (selectedMarker?.id == marker.id)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color(marker.type.colorHex).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { gisManager.selectMarker(if (isSelected) null else marker) }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(marker.type.iconSymbol, fontSize = 18.sp)
                                Column {
                                    Text(marker.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(marker.type.title, fontSize = 10.sp, color = Color(marker.type.colorHex), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            IconButton(onClick = { gisManager.removeMarker(marker.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Surface(shape = RoundedCornerShape(6.dp), color = Color.Black, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "LAT: ${String.format("%.4f", marker.latitude)}° | LON: ${String.format("%.4f", marker.longitude)}°",
                                    color = Color.LightGray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Radio: ${marker.radiusMeters.toInt()}m",
                                    color = Color(marker.type.colorHex),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (marker.notes.isNotBlank()) {
                            Text("Nota: ${marker.notes}", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGISMarkerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, type, lat, lon, radius, notes ->
                gisManager.addMarker(title, type, lat, lon, radius, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddGISMarkerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, com.example.domain.models.GISMarkerType, Double, Double, Float, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(com.example.domain.models.GISMarkerType.RALLY_POINT) }
    var latText by remember { mutableStateOf("4.6105") }
    var lonText by remember { mutableStateOf("-74.0820") }
    var radiusText by remember { mutableStateOf("50") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = Color(0xFF00E5FF))
                Text("Nuevo Marcador Táctico", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del Punto / Zona", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Tipo de Marcador:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    com.example.domain.models.GISMarkerType.values().forEach { type ->
                        val isSelected = (selectedType == type)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(type.colorHex).copy(alpha = 0.2f) else Color(0xFF161B22),
                            border = BorderStroke(1.dp, if (isSelected) Color(type.colorHex) else Color.DarkGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = type
                                    radiusText = type.defaultRadiusMeters.toInt().toString()
                                }
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(type.iconSymbol)
                                Text(type.title, fontSize = 11.sp, color = if (isSelected) Color(type.colorHex) else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitud", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitud", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it },
                    label = { Text("Radio Perímetro (Metros)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Instrucciones / Notas Tácticas", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: 4.6105
                    val lon = lonText.toDoubleOrNull() ?: -74.0820
                    val radius = radiusText.toFloatOrNull() ?: 50f
                    val validTitle = if (title.isBlank()) selectedType.title else title
                    onConfirm(validTitle, selectedType, lat, lon, radius, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("Transmitir a la Malla", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

data class TacticalMapLayerItem(
    val id: Int,
    val name: String,
    val category: String,
    val protocol: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val defaultActive: Boolean = true,
    val elementCount: String = "0 elem."
)

@Composable
fun TacticalRadarLayersView() {
    val context = LocalContext.current
    val cotTranscoder = remember { CursorOnTargetTranscoder.getInstance(context) }
    val cotStatus by cotTranscoder.status.collectAsStateWithLifecycle()
    val cotEvents by cotTranscoder.receivedCotEvents.collectAsStateWithLifecycle()

    val mbTilesManager = remember { TacticalMBTilesManager.getInstance(context) }
    val mbTilesPackages by mbTilesManager.installedPackages.collectAsStateWithLifecycle()
    val activeMbTilesLayer by mbTilesManager.selectedLayer.collectAsStateWithLifecycle()
    val activeZoom by mbTilesManager.activeZoomLevel.collectAsStateWithLifecycle()

    val bleTransport = remember { BleTacticalProximityTransport.getInstance(context) }
    val bleStatus by bleTransport.status.collectAsStateWithLifecycle()
    val blePeers by bleTransport.peers.collectAsStateWithLifecycle()

    val c2Protocol = remember { UniversalMeshC2Protocol.getInstance(context) }
    val c2Devices by c2Protocol.managedDevices.collectAsStateWithLifecycle()
    val c2LastResult by c2Protocol.lastResult.collectAsStateWithLifecycle()

    val bleGattManager = remember { BleGattUniversalDeviceManager.getInstance(context) }
    val extPeripherals by bleGattManager.peripherals.collectAsStateWithLifecycle()

    val droneEngine = remember { MavlinkDroneTelemetryEngine.getInstance(context) }
    val uavTelemetry by droneEngine.activeDrone.collectAsStateWithLifecycle()
    val isUavStreaming by droneEngine.isStreamingActive.collectAsStateWithLifecycle()

    val losEngine = remember { LineOfSightElevationEngine.getInstance(context) }
    var losResult by remember {
        mutableStateOf(losEngine.calculateLos(4.6145, -74.0780, 2.0, 4.6180, -74.0720, 2.0))
    }

    val acousticModem = remember { DataOverSoundAcousticModem.getInstance(context) }
    val acousticState by acousticModem.state.collectAsStateWithLifecycle()

    var selectedC2TargetId by remember { mutableStateOf("LOCAL_DEVICE") }

    var showAdvancedC2SuiteDialog by remember { mutableStateOf(false) }
    var c2SuiteInitialTab by remember { mutableIntStateOf(0) }

    var activeSubSection by remember { mutableIntStateOf(0) } // 0: 16 Capas, 1: MBTiles, 2: CoT/ATAK, 3: BLE, 4: C2 Malla, 5: Drones & LOS, 6: Suite C2 Avanzada

    val predefinedLayers = remember {
        listOf(
            TacticalMapLayerItem(1, "Simbología Militar OTAN APP-6D", "C4ISR & Fuerzas", "MIL-STD-2525D", "Marcadores normalizados de infantería, vehículos blindados, puestos de mando y unidades amigas/hostiles.", Icons.Default.Shield, Color(0xFF00E5FF), true, "12 unidades"),
            TacticalMapLayerItem(2, "Burbuja Geofence Esférica 3D", "Perímetro Aéreo", "3D Euclidean Space", "Volumen cilíndrico/esférico de protección táctica con alertas de altitud y proximidad.", Icons.Default.Radar, Color(0xFF00E676), true, "Radio 50m / 25m"),
            TacticalMapLayerItem(3, "Eventos Cursor-on-Target (CoT)", "Interoperabilidad ATAK", "CoT XML / UDP 4242", "Balizas situacionales interoperables con dispositivos ATAK, CivTAK y WinTAK en tiempo real.", Icons.Default.ShareLocation, Color(0xFFFF9100), true, "${cotEvents.size + 18} eventos"),
            TacticalMapLayerItem(4, "Topología de Malla P2P & Nodos", "Red & Enlace", "Mesh Multi-Hop", "Enlaces activos entre terminales móviles, saltos de red, métricas RSSI y calidad de enlace.", Icons.Default.Hub, Color(0xFF00E5FF), true, "6 nodos"),
            TacticalMapLayerItem(5, "Balizas RF Pasivas & Guerra Electrónica", "SIGINT & RF", "Spectral Scan", "Detección de emisiones de radio no autorizadas, torres celulares sospechosas y alertas de inhibidor.", Icons.Default.WifiTethering, Color(0xFFFF5252), false, "3 portadoras"),
            TacticalMapLayerItem(6, "Salto de Frecuencia FHSS (Anti-Jamming)", "Sigilo RF & LPI", "Time-Slotted PRNG", "Canales de conmutación ágil de frecuencia para transmisión inmune a contramedidas electrónicas.", Icons.Default.Security, Color(0xFFE040FB), true, "CH-44 Activo"),
            TacticalMapLayerItem(7, "Rutas de Escape Tácticas Heurísticas A*", "Navegación & Escape", "Cost-Grid A* Path", "Vectores de evacuación calculados en tiempo real esquivando zonas calientes y áreas de fuego.", Icons.Default.AltRoute, Color(0xFFFFEA00), true, "2 rutas"),
            TacticalMapLayerItem(8, "Odometría Inercial Pedestre (PDR)", "GPS-Denied Navigation", "Kalman IMU / ZUPT", "Rastro de zancadas y migas de pan relativas para orientación en búnkeres o subterráneos sin GPS.", Icons.Default.DirectionsWalk, Color(0xFF00E676), true, "324 pasos"),
            TacticalMapLayerItem(9, "Telemetría de Drones & Vectores Aéreos", "Robótica & ISR", "MAVLink v2.0", "Posición de micro-drones de reconocimiento en vuelo, cono de cámara y cuadrículas de patrulla.", Icons.Default.Flight, Color(0xFF40C4FF), false, "UAV-Alpha (48%)"),
            TacticalMapLayerItem(10, "Ráfagas Satelitales BLOS (Beyond Line-of-Sight)", "Comms Estratégicas", "Iridium SBD / 100B", "Pasarela de ráfagas comprimidas hacia satélites orbitales para enlace fuera de alcance de malla.", Icons.Default.SatelliteAlt, Color(0xFF7C4DFF), false, "Sat-Link Standby"),
            TacticalMapLayerItem(11, "Líneas de Fase FLOT & Zonas Hostiles", "Áreas Operativas", "GeoJSON Polygon", "Perímetros de exclusión de fuego amigo, líneas de avance seguro y cuadrículas de peligro (No-Go).", Icons.Default.WarningAmber, Color(0xFFFF1744), true, "4 perímetros"),
            TacticalMapLayerItem(12, "Enlace Acústico Data-Over-Sound", "Silencio de Radio", "18.5/19.5 kHz FSK", "Capa de transmisión inaudible para sincronización de claves en búnkeres herméticos bajo EMCON Alpha.", Icons.Default.GraphicEq, Color(0xFF69F0AE), false, "Escucha Activa"),
            TacticalMapLayerItem(13, "Radares de Proximidad IoT de Trinchera", "Sensores Perimetrales", "BLE Micropower", "Micro-sensores acústicos y sísmicos desplegados en el terreno para alerta perimétrica temprana.", Icons.Default.Sensors, Color(0xFFFFD740), true, "8 balizas"),
            TacticalMapLayerItem(14, "Trazabilidad Inmutable Micro-Blockchain", "Auditoría Forense", "SHA-256 Merkle Ledger", "Sellado criptográfico de posiciones y órdenes para evitar suplantación de identidad o spoofing.", Icons.Default.Lock, Color(0xFF00E5FF), true, "Bloque #142"),
            TacticalMapLayerItem(15, "Frentes Atmosféricos Barométricos", "Meteorología Táctica", "Baro Sensor / NOAA", "Gradiente barométrico local con proyección de turbonadas y tormentas en los próximos 60 min.", Icons.Default.Air, Color(0xFF80D8FF), true, "1013.2 hPa"),
            TacticalMapLayerItem(16, "Balística Exterior & Sectores de Fuego", "Computación de Tiro", "Pejsa G1/G7 Drag", "Arco balístico, corrección de viento lateral en MILs y elevación requerida para blancos asignados.", Icons.Default.GpsFixed, Color(0xFFFF6D00), false, "Solución 450m")
        )
    }

    var activeLayerIds by remember {
        mutableStateOf(predefinedLayers.filter { it.defaultActive }.map { it.id }.toSet())
    }
    var layerOpacity by remember { mutableFloatStateOf(0.85f) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }

    val categories = remember {
        listOf("Todas", "C4ISR & Fuerzas", "Red & Enlace", "Sigilo RF & LPI", "Navegación & Escape", "Sensores Perimetrales")
    }

    val filteredLayers = remember(predefinedLayers, searchQuery, selectedCategory) {
        predefinedLayers.filter { layer ->
            val matchesCategory = (selectedCategory == "Todas" || layer.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                layer.name.contains(searchQuery, ignoreCase = true) ||
                layer.protocol.contains(searchQuery, ignoreCase = true) ||
                layer.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Selector de Subsección Táctica
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val subSections = listOf(
                Triple(0, "Capas (16)", Icons.Default.Layers),
                Triple(1, "MBTiles Offline", Icons.Default.Map),
                Triple(2, "Enlace ATAK CoT", Icons.Default.ShareLocation),
                Triple(3, "Transporte BLE", Icons.Default.Bluetooth),
                Triple(4, "Mando C2 Malla", Icons.Default.SettingsRemote),
                Triple(5, "Drones & Enlaces", Icons.Default.AirplanemodeActive),
                Triple(6, "Suite C2 Avanzada", Icons.Default.DeveloperBoard)
            )
            subSections.forEach { (idx, label, icon) ->
                val isSelected = activeSubSection == idx
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.25f) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF30363D)),
                    modifier = Modifier.clickable { activeSubSection = idx }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = if (isSelected) TacticalCyanPrimary else Color.LightGray, modifier = Modifier.size(14.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TacticalCyanPrimary else Color.LightGray
                        )
                    }
                }
            }
        }

        when (activeSubSection) {
            0 -> {
                // Vista 16 Capas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CATÁLOGO DE CAPAS RADAR (16)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TacticalCyanPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${activeLayerIds.size} de ${predefinedLayers.size} activas en renderizado",
                            fontSize = 9.sp,
                            color = Color.LightGray
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF21262D),
                        modifier = Modifier.clickable {
                            activeLayerIds = if (activeLayerIds.size == predefinedLayers.size) emptySet() else predefinedLayers.map { it.id }.toSet()
                        }
                    ) {
                        Text(
                            text = if (activeLayerIds.size == predefinedLayers.size) "Apagar Todas" else "Activar Todas",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                // Opacidad
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color(0xFF30363D))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Opacidad:", fontSize = 9.sp, color = Color.White)
                        Slider(
                            value = layerOpacity,
                            onValueChange = { layerOpacity = it },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = TacticalCyanPrimary, activeTrackColor = TacticalCyanPrimary, inactiveTrackColor = Color.DarkGray)
                        )
                        Text("${(layerOpacity * 100).toInt()}%", fontSize = 9.sp, color = TacticalCyanPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                // Lista de capas
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLayers, key = { it.id }) { layer ->
                        val isActive = activeLayerIds.contains(layer.id)
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFF161B22) else Color(0xFF0D1117)),
                            border = BorderStroke(1.dp, if (isActive) layer.color.copy(alpha = 0.6f) else Color(0xFF30363D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier.size(26.dp).background(if (isActive) layer.color.copy(alpha = 0.2f) else Color(0xFF21262D), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(layer.icon, contentDescription = null, tint = if (isActive) layer.color else Color.Gray, modifier = Modifier.size(15.dp))
                                        }
                                        Column {
                                            Text("${layer.id}. ${layer.name}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isActive) Color.White else Color.Gray)
                                            Text("${layer.category} • ${layer.protocol}", fontSize = 8.sp, color = if (isActive) layer.color else Color.DarkGray)
                                        }
                                    }
                                    Switch(
                                        checked = isActive,
                                        onCheckedChange = { checked ->
                                            activeLayerIds = if (checked) activeLayerIds + layer.id else activeLayerIds - layer.id
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = layer.color)
                                    )
                                }
                                Text(layer.description, fontSize = 9.sp, color = if (isActive) Color.LightGray else Color.Gray)
                            }
                        }
                    }
                }
            }
            1 -> {
                // Vista MBTiles Offline
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PAQUETES CARTOGRÁFICOS OFFLINE (MBTILES)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TacticalCyanPrimary
                    )
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Capa Activa en Visor: ${activeMbTilesLayer.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalEmeraldSecondary)
                            Text("Formato: ${activeMbTilesLayer.format.uppercase()} | Bounds: ${activeMbTilesLayer.bounds}", fontSize = 9.sp, color = Color.LightGray)
                            Text("Zoom Configurado: Lvl $activeZoom (Rango: ${activeMbTilesLayer.minZoom}-${activeMbTilesLayer.maxZoom})", fontSize = 9.sp, color = TacticalCyanPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { mbTilesManager.setZoomLevel(activeZoom - 1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-", fontSize = 14.sp) }
                                Text("Zoom $activeZoom", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Button(
                                    onClick = { mbTilesManager.setZoomLevel(activeZoom + 1) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+", fontSize = 14.sp) }
                            }
                        }
                    }

                    Text("Paquetes Instalados en Terminal (${mbTilesPackages.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                        items(mbTilesPackages, key = { it.id }) { pkg ->
                            val isSelected = pkg.id == activeMbTilesLayer.id
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1F242C) else Color(0xFF0D1117)),
                                border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF30363D)),
                                modifier = Modifier.fillMaxWidth().clickable { mbTilesManager.selectPrimaryPackage(pkg.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (isSelected) TacticalCyanPrimary else Color.White)
                                        Text(pkg.description, fontSize = 9.sp, color = Color.LightGray)
                                        Text("${pkg.layerType.label} • ${(pkg.sizeBytes / (1024 * 1024))} MB • ${pkg.tileCount} mosaicos", fontSize = 8.sp, color = Color.Gray)
                                    }
                                    if (isSelected) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = TacticalCyanPrimary.copy(alpha = 0.2f)) {
                                            Text("ACTIVO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary, modifier = Modifier.padding(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Vista Enlace ATAK CoT
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "INTEROPERABILIDAD CURSOR-ON-TARGET (ATAK / CIVTAK)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TacticalCyanPrimary
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, if (cotStatus.isBroadcastingActive) TacticalEmeraldSecondary else Color(0xFF30363D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Baliza Situacional CoT UDP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("Puerto: ${cotStatus.broadcastPort} | Destino: ${cotStatus.targetBroadcastIp}", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Switch(
                                    checked = cotStatus.isBroadcastingActive,
                                    onCheckedChange = { active ->
                                        if (active) cotTranscoder.startBroadcasting("APOLLO_HQ") else cotTranscoder.stopBroadcasting()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TacticalEmeraldSecondary)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Escucha Activa de Nodos ATAK", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("Captura paquetes XML emitidos por WinTAK / ATAK", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Switch(
                                    checked = cotStatus.isListeningActive,
                                    onCheckedChange = { active ->
                                        if (active) cotTranscoder.startListening() else cotTranscoder.stopListening()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TacticalCyanPrimary)
                                )
                            }

                            Divider(color = Color(0xFF30363D))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Paquetes Enviados: ${cotStatus.packetsSent}", fontSize = 9.sp, color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold)
                                Text("Paquetes Recibidos: ${cotStatus.packetsReceived}", fontSize = 9.sp, color = TacticalCyanPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Botón para despachar CoT manual de prueba
                    Button(
                        onClick = {
                            val testEvent = CotEvent(
                                uid = "MANUAL-ALERT-${System.currentTimeMillis()}",
                                type = "b-m-p-s-p-loc",
                                callsign = "PUNTO_REUNION_DELTA",
                                lat = 4.6120,
                                lon = -74.0810,
                                remarks = "Punto de encuentro ordenado por Comando Táctico"
                            )
                            cotTranscoder.dispatchCotEvent(testEvent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Emitir Evento CoT Inmediato a ATAK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Text("Eventos CoT Interceptados (${cotEvents.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (cotEvents.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0D1117),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text("Esperando balizas CoT en UDP 4242...", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                            items(cotEvents, key = { it.uid }) { evt ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF161B22),
                                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("${evt.callsign} [${evt.type}]", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                                        Text("Coords: ${evt.lat}, ${evt.lon} | Alt: ${evt.haeMeters}m", fontSize = 8.sp, color = Color.LightGray)
                                        if (evt.remarks.isNotBlank()) {
                                            Text("Notas: ${evt.remarks}", fontSize = 8.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Vista Transporte BLE Proximidad
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "TRANSPORTE FÍSICO BLE / PROXIMIDAD TÁCTICA OFFLINE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TacticalCyanPrimary
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        border = BorderStroke(1.dp, if (bleStatus.isAdvertising) TacticalEmeraldSecondary else Color(0xFF30363D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Baliza Táctica BLE (Advertising)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("Transmite presencia e ID sin emparejamiento", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Switch(
                                    checked = bleStatus.isAdvertising,
                                    onCheckedChange = { active ->
                                        if (active) bleTransport.startAdvertising("APOLLO_HQ") else bleTransport.stopAdvertising()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TacticalEmeraldSecondary)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Escáner de Proximidad BLE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("Detecta terminales tácticos en radio de ~50m", fontSize = 9.sp, color = Color.LightGray)
                                }
                                Switch(
                                    checked = bleStatus.isScanning,
                                    onCheckedChange = { active ->
                                        if (active) bleTransport.startScanning() else bleTransport.stopScanning()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = TacticalCyanPrimary)
                                )
                            }

                            Divider(color = Color(0xFF30363D))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bluetooth BLE: ${if (bleStatus.isBleSupported) "Soportado" else "No disp."}", fontSize = 9.sp, color = Color.LightGray)
                                Text("Pares Cercanos: ${blePeers.size}", fontSize = 9.sp, color = TacticalCyanPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("Pares Tácticos en Rango BLE (${blePeers.size}):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (blePeers.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0D1117),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (bleStatus.isScanning) "Buscando balizas BLE en la frecuencia 2.4 GHz..." else "Inicia el escáner para descubrir terminales cercanos.",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                            items(blePeers.values.toList(), key = { it.nodeId }) { peer ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF161B22),
                                    border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(peer.callsign, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TacticalCyanPrimary)
                                            Text("MAC / ID: ${peer.nodeId}", fontSize = 8.sp, color = Color.Gray)
                                            Text("Batería: ${peer.batteryPercent}%", fontSize = 8.sp, color = Color.LightGray)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("RSSI: ${peer.rssi} dBm", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalEmeraldSecondary)
                                            Text("~${peer.distanceEstimatedMeters} m", fontSize = 8.sp, color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subsección 4: Mando C2 Malla & Hardware Remoto Universal
            4 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.SettingsRemote, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(16.dp))
                                    Text("MANDO C2 DISTRIBUIDO & HARDWARE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE11D48).copy(alpha = 0.2f)) {
                                    Text("E2EE AUTH", fontSize = 8.sp, color = Color(0xFFFDA4AF), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "Control total nodo-a-nodo sobre hardware, sensores y periféricos vía LAN 9090, Malla DTN y BLE GATT.",
                                fontSize = 9.sp,
                                color = Color.LightGray
                            )

                            if (c2LastResult != null) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (c2LastResult!!.success) TacticalEmeraldSecondary.copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Último C2: ${c2LastResult!!.action.name} -> ${c2LastResult!!.message}",
                                        fontSize = 8.sp,
                                        color = if (c2LastResult!!.success) TacticalEmeraldSecondary else Color(0xFFFF5252),
                                        modifier = Modifier.padding(6.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Text("Seleccionar Nodo Objetivo (${c2Devices.size} detectados):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        c2Devices.values.forEach { dev ->
                            val isSel = selectedC2TargetId == dev.nodeId
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) TacticalCyanPrimary.copy(alpha = 0.25f) else Color(0xFF161B22),
                                border = BorderStroke(1.dp, if (isSel) TacticalCyanPrimary else Color(0xFF30363D)),
                                modifier = Modifier.clickable { selectedC2TargetId = dev.nodeId }
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(dev.callsign, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isSel) TacticalCyanPrimary else Color.White)
                                    Text("Tipo: ${dev.transportType}", fontSize = 7.sp, color = Color.Gray)
                                    Text("Bat: ${dev.batteryPercent}%", fontSize = 8.sp, color = TacticalEmeraldSecondary)
                                }
                            }
                        }
                    }

                    val currentTarget = c2Devices[selectedC2TargetId]
                    if (currentTarget != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0D1117),
                            border = BorderStroke(1.dp, Color(0xFF30363D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Actuadores de Hardware en ${currentTarget.callsign}:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalAmberTertiary)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            c2Protocol.dispatchCommand(
                                                currentTarget.nodeId,
                                                if (currentTarget.isFlashlightOn) C2HardwareAction.FLASHLIGHT_OFF else C2HardwareAction.FLASHLIGHT_ON
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (currentTarget.isFlashlightOn) Color(0xFFFFD740) else Color(0xFF21262D)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(if (currentTarget.isFlashlightOn) "Apagar Flash" else "Linterna ON", fontSize = 9.sp, color = if (currentTarget.isFlashlightOn) Color.Black else Color.White)
                                    }

                                    Button(
                                        onClick = { c2Protocol.dispatchCommand(currentTarget.nodeId, C2HardwareAction.FLASHLIGHT_STROBE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Estrobo SOS", fontSize = 9.sp, color = Color.White)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { c2Protocol.dispatchCommand(currentTarget.nodeId, C2HardwareAction.SIREN_ALARM_PULSE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Sirena Sónica", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { c2Protocol.dispatchCommand(currentTarget.nodeId, C2HardwareAction.HAPTIC_VIBRATION_PULSE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Vibración Háptica", fontSize = 9.sp, color = Color.White)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { c2Protocol.dispatchCommand(currentTarget.nodeId, C2HardwareAction.TELEMETRY_SNAPSHOT) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Solicitar Telemetría", fontSize = 9.sp, color = TacticalCyanPrimary)
                                    }

                                    Button(
                                        onClick = { c2Protocol.dispatchCommand(currentTarget.nodeId, C2HardwareAction.EMERGENCY_ZEROIZE) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                                        modifier = Modifier.weight(1f).height(36.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Borrado Zeroize", fontSize = 9.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Text("Periféricos BLE Externos sin App (${extPeripherals.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                        items(extPeripherals.values.toList(), key = { it.macAddress }) { p ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF161B22),
                                border = BorderStroke(1.dp, Color(0xFF30363D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(p.name, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = TacticalEmeraldSecondary)
                                        Text("MAC: ${p.macAddress} | RSSI: ${p.rssi} dBm", fontSize = 8.sp, color = Color.Gray)
                                        Text("Perfiles: ${p.supportedProfiles.joinToString(", ")}", fontSize = 7.sp, color = Color.LightGray)
                                    }
                                    Button(
                                        onClick = { bleGattManager.triggerExternalPeripheralAlert(p.macAddress) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Alertar IAS", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subsección 5: Drones MAVLink & Intervisibilidad LOS & Módem FSK
            5 -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // MAVLink Drone Telemetry
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, Color(0xFF40C4FF).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Flight, contentDescription = null, tint = Color(0xFF40C4FF), modifier = Modifier.size(16.dp))
                                    Text("MICRO-UAV MAVLINK v2.0", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        if (isUavStreaming) droneEngine.stopLiveUavStream() else droneEngine.startLiveUavStream()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isUavStreaming) Color(0xFFFF5252) else Color(0xFF40C4FF)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(if (isUavStreaming) "Detener" else "Iniciar Patrulla", fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Unidad: ${uavTelemetry.uavName}", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Modo: ${uavTelemetry.flightMode}", fontSize = 9.sp, color = TacticalCyanPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Alt AGL: ${"%.1f".format(uavTelemetry.altAglMeters)} m", fontSize = 8.sp, color = Color.LightGray)
                                Text("Rumbo: ${"%.0f".format(uavTelemetry.headingDeg)}°", fontSize = 8.sp, color = Color.LightGray)
                                Text("Batería: ${uavTelemetry.batteryPercent}%", fontSize = 8.sp, color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Cálculo de Intervisibilidad LOS / NLOS
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, if (losResult.isDirectLosClear) TacticalEmeraldSecondary else Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = if (losResult.isDirectLosClear) TacticalEmeraldSecondary else Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                    Text("LÍNEA DE VISIÓN & FRESNEL (LOS/NLOS)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (losResult.isDirectLosClear) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color(0xFFFF5252).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        if (losResult.isDirectLosClear) "LOS DESPEJADO" else "NLOS OBSTRUIDO",
                                        fontSize = 8.sp,
                                        color = if (losResult.isDirectLosClear) TacticalEmeraldSecondary else Color(0xFFFF5252),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                "Distancia: ${"%.0f".format(losResult.distanceTotalMeters)}m | Despeje Fresnel: ${"%.1f".format(losResult.fresnelZoneClearancePercent)}%",
                                fontSize = 8.sp,
                                color = Color.LightGray
                            )
                            Button(
                                onClick = {
                                    losResult = losEngine.calculateLos(4.6145, -74.0780, 2.5, 4.6180, -74.0720, 2.5)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                modifier = Modifier.fillMaxWidth().height(30.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text("Recalcular Relieve Orografía DEM", fontSize = 8.sp, color = Color.White)
                            }
                        }
                    }

                    // Data-Over-Sound Módem Acústico (EMCON Alpha)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, Color(0xFF69F0AE).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(16.dp))
                                    Text("DATA-OVER-SOUND (FSK 18.5-19.5 kHz)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF69F0AE).copy(alpha = 0.2f)) {
                                    Text("SILENCIO RF", fontSize = 8.sp, color = Color(0xFF69F0AE), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Transmisión de datos por audio inaudible bajo jamming de radiofrecuencia total.", fontSize = 8.sp, color = Color.LightGray)
                            Button(
                                onClick = {
                                    acousticModem.transmitAcousticData("SOS_LAT_4.6145_LON_-74.0780")
                                },
                                enabled = !acousticState.isTransmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF69F0AE)),
                                modifier = Modifier.fillMaxWidth().height(34.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text(
                                    if (acousticState.isTransmitting) "Emitiendo Ultrasonido..." else "Transmitir Ráfaga Acústica FSK",
                                    fontSize = 9.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            6 -> {
                // Vista Suite C2 Avanzada (Input Virtual, Radio USB, IA Visión, Room Audit)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.5.dp, TacticalCyanPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = TacticalCyanPrimary)
                                    Column {
                                        Text("SUITE C2 TÁCTICA MULTI-CANAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text("Control Remoto, Serial OTG, Reconocimiento Óptico y SQLCipher", fontSize = 8.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                                Button(
                                    onClick = {
                                        c2SuiteInitialTab = 0
                                        showAdvancedC2SuiteDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("Abrir Suite Completa", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 4 Tarjetas de acceso directo interactivo
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f).clickable {
                                        c2SuiteInitialTab = 0
                                        showAdvancedC2SuiteDialog = true
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Mouse, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                            Text("Input Virtual", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Text("Control táctil, trackpad y teclado remoto", fontSize = 7.sp, color = Color.LightGray)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f).clickable {
                                        c2SuiteInitialTab = 1
                                        showAdvancedC2SuiteDialog = true
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                                            Text("Radio USB-OTG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Text("TNC KISS, PTT VHF/UHF y puertos serie", fontSize = 7.sp, color = Color.LightGray)
                                    }
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f).clickable {
                                        c2SuiteInitialTab = 2
                                        showAdvancedC2SuiteDialog = true
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                            Text("IA Visión 24 FPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Text("Detección de blancos, distancia e infrarrojo", fontSize = 7.sp, color = Color.LightGray)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f).clickable {
                                        c2SuiteInitialTab = 3
                                        showAdvancedC2SuiteDialog = true
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                                            Text("Auditoría Room", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Text("Cifrado SQLCipher de órdenes y telemetría", fontSize = 7.sp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdvancedC2SuiteDialog) {
        TacticalAdvancedC2SuiteDialog(
            initialTab = c2SuiteInitialTab,
            onDismiss = { showAdvancedC2SuiteDialog = false }
        )
    }
}

