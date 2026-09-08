package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.discovery.AutonomousMeshDiscoveryEngine
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.DiscoveryLogEntry
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.presence.PresenceManager
import com.example.domain.security.FirebaseAuthManager
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerDiscoveryLogScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val rawLogs by DiscoveryLogCollector.logs.collectAsStateWithLifecycle()
    val metrics by DiscoveryLogCollector.metrics.collectAsStateWithLifecycle()

    val discoveryEngine = remember(context) { AutonomousMeshDiscoveryEngine.getInstance(context) }
    val authManager = remember(context) { FirebaseAuthManager(context) }
    val presenceManager = remember(context) { PresenceManager(context) }
    val currentUserProfile by authManager.currentUserProfile.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf(LogCategory.ALL) }
    var selectedSeverity by remember { mutableStateOf<LogSeverity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var autoScrollEnabled by remember { mutableStateOf(true) }
    var isActionInProgress by remember { mutableStateOf(false) }

    // Filtrar logs según búsqueda y categorías
    val filteredLogs by remember(rawLogs, selectedCategory, selectedSeverity, searchQuery) {
        derivedStateOf {
            rawLogs.filter { entry ->
                val matchesCategory = (selectedCategory == LogCategory.ALL || entry.category == selectedCategory)
                val matchesSeverity = (selectedSeverity == null || entry.severity == selectedSeverity)
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                            entry.tag.contains(searchQuery, ignoreCase = true) ||
                            (entry.rawPayload?.contains(searchQuery, ignoreCase = true) == true)
                }
                matchesCategory && matchesSeverity && matchesSearch
            }.reversed() // Mostrar más recientes arriba
        }
    }

    // Auto-scroll al primer elemento cuando se agrega un nuevo log si está activo
    LaunchedEffect(rawLogs.size) {
        if (autoScrollEnabled && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("peer_discovery_debug_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = TacticalCyanPrimary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                "Monitor de Descubrimiento",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            "Handshake • Presencia Firestore • Webhooks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TacticalCyanPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = if (isSearchExpanded) TacticalCyanPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("OmniComm Discovery Logs", DiscoveryLogCollector.getLogsAsFormattedText())
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_logs_button")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copiar logs",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = {
                            DiscoveryLogCollector.clearLogs()
                            Toast.makeText(context, "Registro de logs limpiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Limpiar logs",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Barra de Búsqueda Desplegable
            AnimatedVisibility(
                visible = isSearchExpanded,
                enter = expandVertically(tween(250)) + fadeIn(),
                exit = shrinkVertically(tween(200)) + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filtrar por UID, huella, payload, tag...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = TacticalCyanPrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("log_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyanPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // 2. Panel de KPIs y Métricas Rápidas
            TacticalMetricsDashboard(
                metrics = metrics,
                currentUser = currentUserProfile?.displayName ?: "Offline"
            )

            // 3. Barra de Acciones de Prueba en Vivo (Trigger live test actions)
            TacticalLiveActionTriggers(
                isActionInProgress = isActionInProgress,
                onTriggerAuthHandshake = {
                    scope.launch {
                        isActionInProgress = true
                        DiscoveryLogCollector.logAuthHandshake(
                            step = "MANUAL_AUTH_TRIGGER",
                            message = "⚡ Disparando re-sincronización manual de identidad criptográfica con Firestore...",
                            severity = LogSeverity.PROTOCOL
                        )
                        val user = authManager.currentFirebaseUser
                        if (user != null) {
                            authManager.syncUserProfileToFirestore(user)
                        } else {
                            DiscoveryLogCollector.logAuthHandshake(
                                step = "AUTH_TRIGGER_NOTICE",
                                message = "Modo autónomo/local: verificando par de claves RSA locales...",
                                severity = LogSeverity.INFO
                            )
                        }
                        isActionInProgress = false
                    }
                },
                onTriggerWebhookSignal = {
                    scope.launch {
                        isActionInProgress = true
                        discoveryEngine.broadcastMeshSignal(
                            type = "TACTICAL_WEBHOOK_PING",
                            text = "⚡ Ping de verificación táctica emitido desde el Monitor de Logs."
                        )
                        isActionInProgress = false
                    }
                },
                onTriggerPresencePing = {
                    scope.launch {
                        isActionInProgress = true
                        val user = authManager.currentFirebaseUser
                        presenceManager.startTrackingMyPresence(
                            myUid = user?.uid ?: "local-operator",
                            myDisplayName = user?.displayName ?: "Operador Táctico",
                            myEmail = user?.email ?: "local@tactical.omni",
                            batteryPercent = 98
                        )
                        DiscoveryLogCollector.logRtdbPresence(
                            action = "MANUAL_PRESENCE_PING",
                            message = "🟢 Ping de presencia manual forzado en Realtime Database y Firestore.",
                            severity = LogSeverity.SUCCESS
                        )
                        isActionInProgress = false
                    }
                }
            )

            // 4. Filtros por Categoría (Chips Horizontales)
            CategoryFilterChipBar(
                selectedCategory = selectedCategory,
                onSelectCategory = { selectedCategory = it }
            )

            // 5. Encabezado de la Consola con contador y botón de auto-scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REGISTRO DE TELEMETRÍA (${filteredLogs.size} de ${rawLogs.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = autoScrollEnabled,
                        onClick = { autoScrollEnabled = !autoScrollEnabled },
                        label = { Text(if (autoScrollEnabled) "Auto-Scroll ON" else "Pausado", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                if (autoScrollEnabled) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            // 6. Lista de Logs Estilo Terminal Táctica
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TacticalCyanPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = TacticalCyanPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            "Sin registros para este filtro",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Prueba ejecutando una acción de prueba arriba o cambiando la categoría seleccionada.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { logEntry ->
                        TacticalLogItemCard(entry = logEntry)
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de cada entrada de log con inspector de payload JSON expandible
 */
@Composable
private fun TacticalLogItemCard(entry: DiscoveryLogEntry) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val severityColor = when (entry.severity) {
        LogSeverity.SUCCESS -> TacticalEmeraldSecondary
        LogSeverity.ERROR -> Color(0xFFFF5252)
        LogSeverity.WARNING -> TacticalAmberTertiary
        LogSeverity.PROTOCOL -> TacticalCyanPrimary
        LogSeverity.INFO -> Color(0xFF80D8FF)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("log_card_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Fila Superior: Timestamp + Severity Badge + Category Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge de Severidad
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = severityColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.8.dp, severityColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = entry.severity.label,
                            color = severityColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Categoría
                    Text(
                        text = "${entry.category.icon} ${entry.category.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Timestamp y duración
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.durationMs != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TacticalCyanPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "${entry.durationMs}ms",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TacticalCyanPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = entry.formattedTime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tag y Mensaje principal
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "[${entry.tag}]",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Text(
                    text = entry.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }

            // Raw Payload expandible si existe
            if (!entry.rawPayload.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070B0E))
                            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PAYLOAD RAW JSON",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TacticalCyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Log Payload", entry.rawPayload)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Payload copiado", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar",
                                    tint = TacticalCyanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.rawPayload,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF80D8FF),
                            lineHeight = 14.sp
                        )
                    }
                }
                if (!isExpanded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.UnfoldMore,
                            contentDescription = null,
                            tint = TacticalCyanPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "Toca para inspeccionar datos raw...",
                            fontSize = 10.sp,
                            color = TacticalCyanPrimary.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dashboard de KPIs métricos del estado de descubrimiento
 */
@Composable
private fun TacticalMetricsDashboard(
    metrics: com.example.domain.logging.DiscoveryMetrics,
    currentUser: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ESTADO DE CONEXIONES Y CANALES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = TacticalCyanPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (metrics.isRtdbConnected) TacticalEmeraldSecondary.copy(alpha = 0.2f) else TacticalAmberTertiary.copy(alpha = 0.2f)
                ) {
                    Text(
                        if (metrics.isRtdbConnected) "RTDB: ONLINE" else "RTDB: CONNECTING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (metrics.isRtdbConnected) TacticalEmeraldSecondary else TacticalAmberTertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Auth Handshakes",
                    value = "${metrics.authHandshakeCount}",
                    status = metrics.lastAuthStatus,
                    icon = Icons.Default.VpnKey,
                    color = TacticalEmeraldSecondary
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Eventos Presencia",
                    value = "${metrics.firestoreEventsCount + metrics.rtdbEventsCount}",
                    status = "RTDB + Cloud",
                    icon = Icons.Default.Sensors,
                    color = TacticalCyanPrimary
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = "Señales Webhook",
                    value = "${metrics.webhookSignalsCount}",
                    status = if (metrics.lastWebhookLatencyMs > 0) "${metrics.lastWebhookLatencyMs}ms" else "Activo",
                    icon = Icons.Default.RssFeed,
                    color = TacticalAmberTertiary
                )
            }
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    status: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(status, fontSize = 9.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Barra de acciones de prueba para disparar eventos en tiempo real
 */
@Composable
private fun TacticalLiveActionTriggers(
    isActionInProgress: Boolean,
    onTriggerAuthHandshake: () -> Unit,
    onTriggerWebhookSignal: () -> Unit,
    onTriggerPresencePing: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onTriggerAuthHandshake,
            enabled = !isActionInProgress,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp).testTag("trigger_auth_test_button")
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sync Auth Keys", fontSize = 11.sp)
        }

        FilledTonalButton(
            onClick = onTriggerWebhookSignal,
            enabled = !isActionInProgress,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp).testTag("trigger_webhook_test_button")
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Emitir Webhook", fontSize = 11.sp)
        }

        FilledTonalButton(
            onClick = onTriggerPresencePing,
            enabled = !isActionInProgress,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp).testTag("trigger_presence_ping_button")
        ) {
            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ping Presencia", fontSize = 11.sp)
        }
    }
}

/**
 * Barra de chips de categorías
 */
@Composable
private fun CategoryFilterChipBar(
    selectedCategory: LogCategory,
    onSelectCategory: (LogCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LogCategory.values().forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelectCategory(category) },
                label = { Text("${category.icon} ${category.label}", fontSize = 11.sp) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TacticalCyanPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = TacticalCyanPrimary
                ),
                border = BorderStroke(
                    1.dp,
                    if (selectedCategory == category) TacticalCyanPrimary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.height(32.dp).testTag("filter_chip_${category.name}")
            )
        }
    }
}
