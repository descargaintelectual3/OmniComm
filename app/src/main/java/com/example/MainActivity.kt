package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.hardware.TacticalManDownDetector
import com.example.ui.components.EmergencyManDownOverlay
import com.example.ui.components.TacticalSecurityAndEnhancementsDialog
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.domain.config.FeatureManager
import com.example.domain.config.NavigationLayoutStyle
import com.example.ui.components.TacticalBottomNavigationBar
import com.example.ui.components.TacticalFloatingDock
import com.example.ui.components.TacticalSideNavigationRail
import com.example.ui.screens.ConfigurationScreen
import com.example.domain.presence.PresenceManager
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.ChatViewModel
import com.example.ui.viewmodels.ContextualSensorViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Iniciar inmediatamente de forma segura el motor de auto-descubrimiento
        try {
            com.example.domain.discovery.AutonomousMeshDiscoveryEngine.getInstance(applicationContext).startAutoDiscovery()
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Error iniciando descubrimiento autónomo: ${t.message}")
        }

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                val sharedChatViewModel: ChatViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()
                val currentUserProfile by authViewModel.currentUserProfile.collectAsStateWithLifecycle()

                val presenceManager = remember { PresenceManager(context) }
                val discoveryEngine = remember { com.example.domain.discovery.AutonomousMeshDiscoveryEngine.getInstance(context) }

                LaunchedEffect(Unit) {
                    discoveryEngine.startAutoDiscovery()
                }

                LaunchedEffect(currentUserProfile) {
                    val user = currentUserProfile
                    if (user != null) {
                        presenceManager.startTrackingMyPresence(
                            myUid = user.uid,
                            myDisplayName = user.displayName,
                            myEmail = user.email
                        )
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        presenceManager.stopTracking()
                    }
                }

                val manDownDetector = remember { TacticalManDownDetector.getInstance(this@MainActivity) }
                val featureManager = remember { FeatureManager.getInstance(context) }
                val navLayout by featureManager.navigationLayout.collectAsStateWithLifecycle()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "hub"

                // Contenedor Modular con Layout Adaptativo (Rejilla, Barra Inferior, Dock Flotante, o Rail Lateral)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (navLayout == NavigationLayoutStyle.SIDE_NAVIGATION_RAIL) {
                        TacticalSideNavigationRail(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("hub") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                NavHost(
                                    navController = navController, 
                                    startDestination = "hub",
                                    enterTransition = { slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn(tween(300)) },
                                    exitTransition = { slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut(tween(300)) },
                                    popEnterTransition = { slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn(tween(300)) },
                                    popExitTransition = { slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut(tween(300)) }
                                ) {
                                    composable("hub") {
                                        HubScreen(onNavigate = { route -> navController.navigate(route) })
                                    }
                                    composable("auth") {
                                        com.example.ui.screens.AuthScreen(
                                            onAuthSuccess = { navController.popBackStack() },
                                            onContinueOffline = { navController.popBackStack() }
                                        )
                                    }
                                    composable("contacts") {
                                        com.example.ui.screens.ContactsScreen(
                                            onBack = { navController.popBackStack() },
                                            onStartChat = { session ->
                                                sharedChatViewModel.selectSession(
                                                    sessionId = session.sessionId,
                                                    title = session.title,
                                                    partner = session.participantNames.values.firstOrNull() ?: "Operador"
                                                )
                                                navController.navigate("team_chat")
                                            },
                                            onStartVideo = {
                                                navController.navigate("video_rooms")
                                            },
                                            onOpenLogs = {
                                                navController.navigate("discovery_logs")
                                            }
                                        )
                                    }
                                    composable("team_chat") {
                                        com.example.ui.screens.ChatScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenContacts = { navController.navigate("contacts") },
                                            onOpenCryptoKeys = { navController.navigate("crypto_keys") },
                                            viewModel = sharedChatViewModel
                                        )
                                    }
                                    composable("chat") {
                                        com.example.ui.screens.ChatScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenContacts = { navController.navigate("contacts") },
                                            onOpenCryptoKeys = { navController.navigate("crypto_keys") },
                                            viewModel = sharedChatViewModel
                                        )
                                    }
                                    composable("video_rooms") { com.example.ui.screens.VideoScreen(onBack = { navController.popBackStack() }) }
                                    composable("cloud_drive") { com.example.ui.screens.CloudScreen(onBack = { navController.popBackStack() }) }
                                    composable("location") { com.example.ui.screens.RadarScreen(onBack = { navController.popBackStack() }) }
                                    composable("radar") { com.example.ui.screens.RadarScreen(onBack = { navController.popBackStack() }) }
                                    composable("camera") { com.example.ui.screens.CameraScreen(onBack = { navController.popBackStack() }) }
                                    composable("config") { 
                                        ConfigurationScreen(
                                            onBack = { navController.popBackStack() },
                                            onOpenLiteMode = { navController.navigate("lite_mode") }
                                        ) 
                                    }
                                    composable("lite_mode") { 
                                        com.example.ui.screens.LiteModeScreen(onBack = { navController.popBackStack() }) 
                                    }
                                    composable("roadmap") { com.example.ui.screens.RoadmapScreen(onBack = { navController.popBackStack() }) }
                                    composable("discovery_logs") { com.example.ui.screens.PeerDiscoveryLogScreen(onBack = { navController.popBackStack() }) }
                                    composable("server_fleet") { com.example.ui.screens.ServerFleetManagementScreen(onBack = { navController.popBackStack() }) }
                                    composable("servers") { com.example.ui.screens.ServerFleetManagementScreen(onBack = { navController.popBackStack() }) }
                                    composable("cicd") { com.example.ui.screens.InternalCiCdScreen(onBack = { navController.popBackStack() }) }
                                    composable("visual_telemetry") { com.example.ui.screens.VisualTelemetryScreen(onBack = { navController.popBackStack() }) }
                                    composable("crypto_keys") { com.example.ui.screens.KeyManagementScreen(onBack = { navController.popBackStack() }) }
                                }

                                // Dock flotante táctico en caso de estar activo
                                if (navLayout == NavigationLayoutStyle.TACTICAL_FLOATING_DOCK) {
                                    TacticalFloatingDock(
                                        currentRoute = currentRoute,
                                        onNavigate = { route ->
                                            if (currentRoute != route) {
                                                navController.navigate(route) {
                                                    popUpTo("hub") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.BottomCenter)
                                    )
                                }
                            }

                            // Barra inferior táctica
                            if (navLayout == NavigationLayoutStyle.BOTTOM_NAV_BAR) {
                                TacticalBottomNavigationBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { route ->
                                        if (currentRoute != route) {
                                            navController.navigate(route) {
                                                popUpTo("hub") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Overlay de Alerta Man Down / Caída Táctica
                        EmergencyManDownOverlay(detector = manDownDetector)

                        // Capa de Bloqueo y Autenticación Biométrica (Android BiometricPrompt API)
                        BiometricLockOverlay(
                            biometricVaultManager = sharedChatViewModel.biometricVaultManager
                        )
                    }
                }
            }
        }
    }
}

data class HubFeature(
    val name: String, 
    val icon: ImageVector, 
    val route: String, 
    val subtitle: String,
    val badge: String,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubScreen(
    onNavigate: (String) -> Unit,
    sensorViewModel: ContextualSensorViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val ambientLight by sensorViewModel.ambientLight.collectAsStateWithLifecycle()
    val isProximityNear by sensorViewModel.isProximityNear.collectAsStateWithLifecycle()
    val movementLevel by sensorViewModel.movementLevel.collectAsStateWithLifecycle()
    val currentUserProfile by authViewModel.currentUserProfile.collectAsStateWithLifecycle()
    
    var isServerMeshModeActive by remember { mutableStateOf(true) }
    var showTacticalEnhancementsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val featureManager = remember(context) { FeatureManager.getInstance(context) }
    val isLiteActive by featureManager.isLiteModeActive.collectAsStateWithLifecycle()

    val discoveryEngine = remember(context) { com.example.domain.discovery.AutonomousMeshDiscoveryEngine.getInstance(context) }
    val activeNodesCount by discoveryEngine.activeNodesCount.collectAsStateWithLifecycle()
    val discoveryStatus by discoveryEngine.discoveryStatus.collectAsStateWithLifecycle()

    val features = listOf(
        HubFeature("Versión Lite & Modular", Icons.Default.EnergySavingsLeaf, "lite_mode", "Control y Ahorro", if (isLiteActive) "LITE ON" else "Modular", TacticalEmeraldSecondary),
        HubFeature("Contactos & Nodos", Icons.Default.Contacts, "contacts", "Libreta en Tiempo Real", "Firestore Sync", TacticalEmeraldSecondary),
        HubFeature("Team Chat E2EE", Icons.Default.Group, "team_chat", "Mensajería Cifrada", "AES-256 GCM", TacticalCyanPrimary),
        HubFeature("Llaves E2EE (Room)", Icons.Default.Key, "crypto_keys", "Gestión de Pares & PQC", "Room Local", TacticalEmeraldSecondary),
        HubFeature("Identidad Táctica", Icons.Default.Security, "auth", "Firebase Auth & Google", "E2EE Key", TacticalAmberTertiary),
        HubFeature("Monitor Telemetría", Icons.Default.Dns, "discovery_logs", "Logs Handshake & Malla", "Live Trace", TacticalCyanPrimary),
        HubFeature("Video Rooms", Icons.Default.VideoCall, "video_rooms", "Salas WebRTC P2P", "E2EE HD", TacticalCyanPrimary),
        HubFeature("Bóveda Segura", Icons.Default.Cloud, "cloud_drive", "Almacenamiento Local", "SQLCipher", TacticalEmeraldSecondary),
        HubFeature("Topología Malla", Icons.Default.Hub, "location", "Radar & Auto-Enlace", "RF / GPS", TacticalAmberTertiary),
        HubFeature("Cámara Táctica", Icons.Default.CameraAlt, "camera", "Captura Evidencias", "CameraX & Storage", TacticalCyanPrimary),
        HubFeature("Configuración & Módulos", Icons.Default.Settings, "config", "FeatureManager & Labs", "Control Táctico", TacticalCyanPrimary),
        HubFeature("Mando Servidores", Icons.Default.Dns, "server_fleet", "Linux & Windows C2", "SSH & WinRM", TacticalCyanPrimary),
        HubFeature("CI/CD DevSecOps", Icons.Default.PlayArrow, "cicd", "Pipelines & OTA Deploy", "SAST / SBOM", TacticalEmeraldSecondary),
        HubFeature("Telemetría Visual", Icons.Default.CameraAlt, "visual_telemetry", "Capturas & Diagnóstico", "PixelCopy HD", TacticalCyanPrimary),
        HubFeature("Dev Roadmap", Icons.Default.History, "roadmap", "Registro Táctico", "v3.4 Live", TacticalEmeraldSecondary)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TacticalCyanPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp),
                            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TacticalCyanPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "OmniComm Tactical Hub",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = TacticalEmeraldSecondary,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Text(
                                    if (currentUserProfile != null) "OPERADOR: ${currentUserProfile?.displayName?.uppercase()}" else "ONLINE • ENLACE MALLA CIFRADO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalEmeraldSecondary,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigate("lite_mode") },
                        modifier = Modifier.testTag("btn_lite_mode_topbar")
                    ) {
                        Icon(
                            Icons.Default.EnergySavingsLeaf,
                            contentDescription = "Versión Lite & Gestor Modular",
                            tint = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary
                        )
                    }
                    IconButton(
                        onClick = { onNavigate("config") },
                        modifier = Modifier.testTag("btn_config_hub")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración de Módulos",
                            tint = TacticalCyanPrimary
                        )
                    }
                    IconButton(
                        onClick = { showTacticalEnhancementsDialog = true },
                        modifier = Modifier.testTag("btn_tactical_enhancements")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Protocolos Tácticos & Zero-Trust",
                            tint = TacticalAmberTertiary
                        )
                    }
                    IconButton(
                        onClick = { onNavigate("crypto_keys") },
                        modifier = Modifier.testTag("btn_keys_hub")
                    ) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Bóveda de Claves E2EE (Room)",
                            tint = TacticalEmeraldSecondary
                        )
                    }
                    IconButton(
                        onClick = { onNavigate("discovery_logs") },
                        modifier = Modifier.testTag("btn_logs_hub")
                    ) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = "Monitor de Telemetría",
                            tint = TacticalCyanPrimary
                        )
                    }
                    if (currentUserProfile != null) {
                        IconButton(
                            onClick = { authViewModel.signOut() },
                            modifier = Modifier.testTag("btn_logout_hub")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        IconButton(
                            onClick = { onNavigate("auth") },
                            modifier = Modifier.testTag("btn_login_hub")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Iniciar Sesión", tint = TacticalCyanPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                // Barra contextual de auto-enlace táctico
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = TacticalEmeraldSecondary,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = if (activeNodesCount > 0) "$activeNodesCount nodos enlazados (LAN/Cloud/BT)" else "Escaneando red táctica en tiempo real...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeNodesCount > 0) TacticalEmeraldSecondary else TacticalCyanPrimary
                            )
                        }

                        TextButton(
                            onClick = {
                                discoveryEngine.publishLocalPresence()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = TacticalAmberTertiary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Baliza", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalAmberTertiary)
                        }
                    }
                }

                // Barra contextual de fusión de sensores
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(14.dp), tint = TacticalCyanPrimary)
                            Text(
                                text = "Luz: ${ambientLight.toInt()} lux (${if(ambientLight < 10) "Oscuro" else "Luminoso"}) • $movementLevel", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isProximityNear) TacticalAmberTertiary.copy(alpha = 0.2f) else TacticalCyanPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if(isProximityNear) "🔒 Modo Discreto" else "🔊 Altavoz Libre", 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold,
                                color = if (isProximityNear) TacticalAmberTertiary else TacticalCyanPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Selector de modo Servidor / Mesh Node
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isServerMeshModeActive) TacticalEmeraldSecondary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Radio,
                                        contentDescription = null,
                                        tint = if (isServerMeshModeActive) TacticalEmeraldSecondary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "Nodo Servidor Mesh Autónomo", 
                                    fontWeight = FontWeight.SemiBold, 
                                    fontSize = 13.sp, 
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    if (isServerMeshModeActive) "Enrutando balizas RFCOMM & TCP P2P" else "Modo sólo escucha pasiva", 
                                    fontSize = 11.sp, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isServerMeshModeActive, 
                            onCheckedChange = { isServerMeshModeActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TacticalEmeraldSecondary,
                                checkedTrackColor = TacticalEmeraldSecondary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.testTag("mesh_server_switch")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("lite_mode") }
                        .testTag("banner_lite_mode_hub"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLiteActive) Color(0xFF0C1D16) else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.6f) else TacticalCyanPrimary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.2f) else TacticalCyanPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EnergySavingsLeaf,
                                        contentDescription = null,
                                        tint = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Versión Lite & Rendimiento",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isLiteActive) TacticalEmeraldSecondary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color.DarkGray
                                    ) {
                                        Text(
                                            text = if (isLiteActive) "ACTIVO" else "MODULAR",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLiteActive) TacticalEmeraldSecondary else Color.LightGray,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    if (isLiteActive) "Optimizando memoria y CPU • Desactiva o activa módulos"
                                    else "Configura qué archivos y componentes corren en la app",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            items(features) { feature ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .testTag("feature_${feature.route}")
                        .clickable { onNavigate(feature.route) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(
                        1.dp, 
                        feature.accentColor.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = feature.accentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        feature.icon, 
                                        contentDescription = feature.name, 
                                        modifier = Modifier.size(24.dp), 
                                        tint = feature.accentColor
                                    )
                                }
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = feature.accentColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = feature.badge,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = feature.accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        Column {
                            Text(
                                feature.name, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                feature.subtitle, 
                                fontSize = 11.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (showTacticalEnhancementsDialog) {
            TacticalSecurityAndEnhancementsDialog(
                onDismiss = { showTacticalEnhancementsDialog = false }
            )
        }
    }
}


