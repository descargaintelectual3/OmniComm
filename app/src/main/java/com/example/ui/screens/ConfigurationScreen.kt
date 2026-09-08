package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.config.*
import com.example.ui.components.*
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    onBack: () -> Unit,
    onOpenLiteMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val featureManager = remember { FeatureManager.getInstance(context) }

    val isLiteActive by featureManager.isLiteModeActive.collectAsStateWithLifecycle()
    val metrics = remember(isLiteActive) { featureManager.getEstimatedMetrics() }

    // Estados observados reactivamente desde FeatureManager
    val navLayout by featureManager.navigationLayout.collectAsStateWithLifecycle()
    val msgStyle by featureManager.messagingStyle.collectAsStateWithLifecycle()
    val themePalette by featureManager.themePalette.collectAsStateWithLifecycle()
    val radarEngine by featureManager.radarVisualEngine.collectAsStateWithLifecycle()

    val manDownEnabled by featureManager.enableManDownAlert.collectAsStateWithLifecycle()
    val storeForwardEnabled by featureManager.enableStoreAndForward.collectAsStateWithLifecycle()
    val opticalAiEnabled by featureManager.enableOpticalAiOcr.collectAsStateWithLifecycle()
    val gisMarkersEnabled by featureManager.enableGisMarkers.collectAsStateWithLifecycle()
    val adaptivePowerEnabled by featureManager.enableAdaptivePower.collectAsStateWithLifecycle()
    val duressEnabled by featureManager.enableDuressSecurity.collectAsStateWithLifecycle()

    val stegoEnabled by featureManager.enableSteganographyVault.collectAsStateWithLifecycle()
    val morseEnabled by featureManager.enableMorseBeaconEngine.collectAsStateWithLifecycle()
    val rfSpectrumEnabled by featureManager.enableRfSpectrumSimulator.collectAsStateWithLifecycle()
    val sitrepEnabled by featureManager.enableSitrepReporter.collectAsStateWithLifecycle()
    val thermalShaderEnabled by featureManager.enableThermalNightShader.collectAsStateWithLifecycle()
    val horizonHudEnabled by featureManager.enableHorizonHudLevel.collectAsStateWithLifecycle()
    val deadMansEnabled by featureManager.enableDeadMansSwitch.collectAsStateWithLifecycle()
    val topologyGraphEnabled by featureManager.enableMeshTopologyGraph.collectAsStateWithLifecycle()
    val hapticsEnabled by featureManager.enableTacticalHaptics.collectAsStateWithLifecycle()

    // Fases 1 a 5
    val lifiEnabled by featureManager.enableVisualLiFi.collectAsStateWithLifecycle()
    val shamirEnabled by featureManager.enableShamirVault.collectAsStateWithLifecycle()
    val acousticShotEnabled by featureManager.enableAcousticShotDetector.collectAsStateWithLifecycle()
    val physioBioEnabled by featureManager.enablePhysioBioTelemetry.collectAsStateWithLifecycle()
    val zeroizeEnabled by featureManager.enableEmergencyZeroize.collectAsStateWithLifecycle()

    // Bloque 2: Fases 6 a 10
    val pdrEnabled by featureManager.enablePdrNavigation.collectAsStateWithLifecycle()
    val fhssEnabled by featureManager.enableFrequencyHopping.collectAsStateWithLifecycle()
    val natoEnabled by featureManager.enableNatoSymbology.collectAsStateWithLifecycle()
    val agentEnabled by featureManager.enableTacticalMissionAgent.collectAsStateWithLifecycle()
    val compressorEnabled by featureManager.enableBinaryCompressor.collectAsStateWithLifecycle()

    // Bloque 3: Fases 11 a 15
    val barometerEnabled by featureManager.enableBarometerStorm.collectAsStateWithLifecycle()
    val ultrasonicEnabled by featureManager.enableUltrasonicLink.collectAsStateWithLifecycle()
    val safeBubbleEnabled by featureManager.enableSafeBubble3D.collectAsStateWithLifecycle()
    val ballisticsEnabled by featureManager.enableBallisticsCalc.collectAsStateWithLifecycle()
    val honeypotEnabled by featureManager.enableMeshHoneypot.collectAsStateWithLifecycle()

    // Bloque 4: Fases 16 a 20 (SIGINT & Ciberseguridad)
    val sybilEnabled by featureManager.enableSybilDetector.collectAsStateWithLifecycle()
    val pqcKyberEnabled by featureManager.enablePostQuantumKyber.collectAsStateWithLifecycle()
    val blockchainEnabled by featureManager.enableMissionBlockchain.collectAsStateWithLifecycle()
    val rfSignatureEnabled by featureManager.enableRfSignatureMeter.collectAsStateWithLifecycle()
    val emconAlphaEnabled by featureManager.enableEmconAlpha.collectAsStateWithLifecycle()

    // Bloque 5: Fases 21 a 25 (Reconocimiento & Mapeo)
    val photogrammetryEnabled by featureManager.enablePhotogrammetry.collectAsStateWithLifecycle()
    val mavlinkUavEnabled by featureManager.enableMavlinkUav.collectAsStateWithLifecycle()
    val medevac9LineEnabled by featureManager.enable9LineMedevac.collectAsStateWithLifecycle()
    val solarCompassEnabled by featureManager.enableSolarCompass.collectAsStateWithLifecycle()
    val satelliteGatewayEnabled by featureManager.enableSatelliteGateway.collectAsStateWithLifecycle()

    // Bloque 6: Fases 26 a 30 (Fusión C4ISR & Mando Maestro)
    val voiceSpectralEnabled by featureManager.enableVoiceSpectral.collectAsStateWithLifecycle()
    val aStarRouterEnabled by featureManager.enableAStarRouter.collectAsStateWithLifecycle()
    val saluteIntelEnabled by featureManager.enableSaluteIntel.collectAsStateWithLifecycle()
    val meshClockSyncEnabled by featureManager.enableMeshClockSync.collectAsStateWithLifecycle()
    val c4isrConsoleEnabled by featureManager.enableC4IsrConsole.collectAsStateWithLifecycle()

    // Modales de herramientas tácticas
    var activeToolDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Configuración de Módulos & Sistema",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            "Control Zero-Loss de Módulos y Personalización Táctica",
                            fontSize = 11.sp,
                            color = TacticalCyanPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_config")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1117))
            )
        },
        containerColor = Color(0xFF0B0F17)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // --- HERO CARD: VERSIÓN LITE & GESTOR MODULAR DE RENDIMIENTO ---
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.6f) else TacticalCyanPrimary.copy(alpha = 0.4f),
                            RoundedCornerShape(12.dp)
                        ),
                    color = if (isLiteActive) Color(0xFF0C1D16) else Color(0xFF101622)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.EnergySavingsLeaf,
                                    contentDescription = null,
                                    tint = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "VERSIÓN LITE & MODULAR",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = if (isLiteActive) TacticalEmeraldSecondary else Color.White
                                    )
                                    Text(
                                        if (isLiteActive) "Estado: ACTIVO (${metrics.second}MB RAM • +${metrics.third}% Batería)" 
                                        else "Estado: COMPLETO (31 Subsistemas)",
                                        fontSize = 11.sp,
                                        color = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary
                                    )
                                }
                            }

                            Switch(
                                checked = isLiteActive,
                                onCheckedChange = { active ->
                                    featureManager.setLiteModeActive(active)
                                },
                                modifier = Modifier.testTag("switch_config_lite_mode"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = TacticalEmeraldSecondary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Optimiza la carga en memoria y procesador activando o desactivando individualmente módulos, componentes, hilos y archivos de código fuente.",
                            fontSize = 11.sp,
                            color = Color(0xFF8B949E),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenLiteMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_open_lite_mode_screen"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.2f) else TacticalCyanPrimary.copy(alpha = 0.2f)
                            ),
                            border = BorderStroke(1.dp, if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ADMINISTRAR COMPONENTES & MÓDULOS LITE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary
                            )
                        }
                    }
                }
            }

            // --- SECCIÓN 1: ESTILO Y ESQUEMA DE NAVEGACIÓN ---
            item {
                SectionHeader("1. ESQUEMA DE NAVEGACIÓN", Icons.Default.ViewQuilt)
                Text(
                    "Selecciona cómo interactúas con la suite de mando. El código de los demás diseños se preserva íntegro.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavigationLayoutStyle.values().forEach { style ->
                        val isSelected = (navLayout == style)
                        SelectableCard(
                            title = style.title,
                            subtitle = style.subtitle,
                            isSelected = isSelected,
                            accentColor = TacticalCyanPrimary,
                            onClick = { featureManager.setNavigationLayout(style) }
                        )
                    }
                }
            }

            // --- SECCIÓN 2: ESTILO DE MENSAJERÍA TÁCTICA ---
            item {
                SectionHeader("2. FORMATO DE MENSAJERÍA Y CHAT", Icons.Default.Forum)
                Text("Alterna entre la terminal militar de alta densidad y burbujas modernas.", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MessagingStyle.values().forEach { style ->
                        val isSelected = (msgStyle == style)
                        SelectableCard(
                            title = style.title,
                            subtitle = style.subtitle,
                            isSelected = isSelected,
                            accentColor = TacticalEmeraldSecondary,
                            onClick = { featureManager.setMessagingStyle(style) }
                        )
                    }
                }
            }

            // --- SECCIÓN 3: MOTOR VISUAL DE RADAR ---
            item {
                SectionHeader("3. VISUALIZACIÓN DE RADAR & GIS", Icons.Default.Radar)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadarVisualEngine.values().forEach { engine ->
                        val isSelected = (radarEngine == engine)
                        SelectableCard(
                            title = engine.title,
                            subtitle = engine.description,
                            isSelected = isSelected,
                            accentColor = TacticalAmberTertiary,
                            onClick = { featureManager.setRadarVisualEngine(engine) }
                        )
                    }
                }
            }

            // --- SECCIÓN 4: PALETA DE COLOR TÁCTICA ---
            item {
                SectionHeader("4. PALETA DE COLOR Y CONTRASTE", Icons.Default.Palette)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePalette.values().forEach { palette ->
                        val isSelected = (themePalette == palette)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(palette.backgroundHex),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(palette.primaryHex) else Color.DarkGray),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { featureManager.setThemePalette(palette) }
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(palette.primaryHex)))
                                Text(palette.title.split(" ")[0], fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // --- SECCIÓN 5: INTERRUPTORES DE MÓDULOS DEL SISTEMA ---
            item {
                SectionHeader("5. MÓDULOS TÁCTICOS Y SENSORES", Icons.Default.ToggleOn)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToggleFeatureCard(
                        title = "Detección Man-Down & Impacto",
                        description = "Alerta cinemática automática con cuenta atrás de 10s al detectar inmovilidad prolongada o caídas.",
                        isEnabled = manDownEnabled,
                        onToggle = { featureManager.toggleManDownAlert(it) },
                        badgeText = "Hardware Acelerómetro"
                    )

                    ToggleFeatureCard(
                        title = "Enrutamiento Multi-Salto (Store & Forward)",
                        description = "Reenvío epidémico en malla con TTL de saltos y priorización QoS para paquetes críticos.",
                        isEnabled = storeForwardEnabled,
                        onToggle = { featureManager.toggleStoreAndForward(it) },
                        badgeText = "Malla P2P"
                    )

                    ToggleFeatureCard(
                        title = "Análisis Óptico y OCR Gemini AI",
                        description = "Extracción de matrículas, coordenadas e identificación de amenazas en fotos de evidencia.",
                        isEnabled = opticalAiEnabled,
                        onToggle = { featureManager.toggleOpticalAiOcr(it) },
                        badgeText = "Edge Vision AI"
                    )

                    ToggleFeatureCard(
                        title = "Marcadores GIS y Perímetros Tácticos",
                        description = "Transmisión y visualización de puntos de reunión (Rally Points) y zonas de peligro.",
                        isEnabled = gisMarkersEnabled,
                        onToggle = { featureManager.toggleGisMarkers(it) },
                        badgeText = "GIS Táctico"
                    )

                    ToggleFeatureCard(
                        title = "Gestión Energética Adaptativa",
                        description = "Ajuste dinámico de ciclos de trabajo en BLE/Wi-Fi según la carga de batería.",
                        isEnabled = adaptivePowerEnabled,
                        onToggle = { featureManager.toggleAdaptivePower(it) },
                        badgeText = "Batería / QoS"
                    )

                    ToggleFeatureCard(
                        title = "Seguridad Zero-Trust & Duress PIN",
                        description = "PIN de coacción para borrado señuelo y clave segura con auto-destrucción de mensajes.",
                        isEnabled = duressEnabled,
                        onToggle = { featureManager.toggleDuressSecurity(it) },
                        badgeText = "Criptografía"
                    )
                }
            }

            // --- SECCIÓN 6: 9 RECOMENDACIONES TÁCTICAS (HERRAMIENTAS INTERACTIVAS) ---
            item {
                SectionHeader("6. CAJA DE HERRAMIENTAS TÁCTICAS (9 MÓDULOS)", Icons.Default.Handyman)
                Text("Herramientas operativas avanzadas totalmente funcionales y listas para desplegar:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "1. Bóveda Esteganográfica LSB",
                        subtitle = "Incrustar o recuperar mensajes cifrados ocultos en imágenes",
                        icon = Icons.Default.EnhancedEncryption,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "stego" }
                    )

                    TacticalToolActionCard(
                        title = "2. Sintetizador de Baliza Morse",
                        subtitle = "Generador de tonos acústicos de 850 Hz para señales de supervivencia",
                        icon = Icons.Default.VolumeUp,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "morse" }
                    )

                    TacticalToolActionCard(
                        title = "3. Analizador de Espectro RF",
                        subtitle = "Monitor de SNR en 2.4 GHz y detección de bloqueadores (Jammers)",
                        icon = Icons.Default.GraphicEq,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "rf_spectrum" }
                    )

                    TacticalToolActionCard(
                        title = "4. Generador de SITREP Militar",
                        subtitle = "Informes de situación estandarizados con formato de 6 líneas OTAN",
                        icon = Icons.Default.Assignment,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "sitrep" }
                    )

                    TacticalToolActionCard(
                        title = "5. Horizonte Artificial & Brújula HUD",
                        subtitle = "Telemetría inercial, inclinómetro (pitch/roll) y rumbo magnético",
                        icon = Icons.Default.Navigation,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "horizon_hud" }
                    )

                    TacticalToolActionCard(
                        title = "6. Interruptor de Hombre Muerto",
                        subtitle = "Watchdog de inactividad con cuenta atrás y baliza SOS de emergencia",
                        icon = Icons.Default.HourglassBottom,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "dead_man" }
                    )

                    TacticalToolActionCard(
                        title = "7. Grafo de Topología Malla",
                        subtitle = "Mapa interactivo de nodos, calidad de enlace RSSI y saltos",
                        icon = Icons.Default.Hub,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "mesh_graph" }
                    )
                }
            }

            // --- SECCIÓN 7: BLOQUE 1 (FASES 1 A 5) — GUERRA ELECTRÓNICA & CRIPTO-RESILIENCIA ---
            item {
                SectionHeader("7. BLOQUE 1 (FASES 1 A 5): CRIPTO-RESILIENCIA & EW", Icons.Default.Security)
                Text("Herramientas operativas de supervivencia y resiliencia electromagnética:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 1: Transmisor Li-Fi Óptico (Flash OOB)",
                        subtitle = "Modulación de pulso lumínico para transmisión de datos fuera de banda",
                        icon = Icons.Default.FlashlightOn,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "lifi" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 2: Bóveda Shamir Secret Sharing (K-de-N)",
                        subtitle = "Fragmentación de llaves maestras con umbral de quórum distribuido",
                        icon = Icons.Default.Key,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "shamir" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 3: Detector & Clasificador de Disparos",
                        subtitle = "Análisis espectral y detección de onda de choque acústica",
                        icon = Icons.Default.Hearing,
                        accentColor = Color(0xFFFF5252),
                        onClick = { activeToolDialog = "acoustic_shot" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 4: Telemetría Biológica de Operador",
                        subtitle = "Monitor de pulso, estrés inercial y Combat Readiness Score",
                        icon = Icons.Default.Favorite,
                        accentColor = Color(0xFFFF4081),
                        onClick = { activeToolDialog = "physio_bio" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 5: Protocolo Cero-Huella ZEROIZE",
                        subtitle = "Purga de 3 pasadas (DoD) de claves, buffers en RAM y cachés de misión",
                        icon = Icons.Default.DeleteForever,
                        accentColor = Color(0xFFFF1744),
                        onClick = { activeToolDialog = "zeroize" }
                    )
                }
            }

            // --- SECCIÓN 8: BLOQUE 2 (FASES 6 A 10) — C2 TÁCTICO & AUTONOMÍA MESH ---
            item {
                SectionHeader("8. BLOQUE 2 (FASES 6 A 10): C2 TÁCTICO & AUTONOMÍA MESH", Icons.Default.AltRoute)
                Text("Herramientas de mando, control y navegación táctica en entornos hostiles:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 6: Navegación Inercial a Estima (PDR)",
                        subtitle = "Navegación táctica en interiores y GPS-Denied con cálculo inercial de pasos",
                        icon = Icons.Default.Navigation,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "pdr" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 7: Micro-Balizas Salto de Frecuencia (FHSS)",
                        subtitle = "Evasión de interferencias y anti-jamming determinista por semilla criptográfica",
                        icon = Icons.Default.SyncAlt,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "fhss" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 8: Tablero Simbología OTAN APP-6",
                        subtitle = "Estandarización militar MIL-STD-2525D de entidades amigas, hostiles y UAVs",
                        icon = Icons.Default.Shield,
                        accentColor = Color(0xFFFFD600),
                        onClick = { activeToolDialog = "nato" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 9: Agente Táctico Autónomo de Misión",
                        subtitle = "Evaluación de entorno táctico y recomendaciones de curso de acción (COA) offline",
                        icon = Icons.Default.Psychology,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "tactical_agent" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 10: Compresor Binario Ultradenso",
                        subtitle = "Compresión extrema de payloads para canales de baja tasa de datos (<1.2 kbps)",
                        icon = Icons.Default.Compress,
                        accentColor = Color(0xFF64FFDA),
                        onClick = { activeToolDialog = "binary_compressor" }
                    )
                }
            }

            // --- SECCIÓN 9: BLOQUE 3 (FASES 11 A 15) — SENSORES DE CAMPO & SUPERVIVENCIA ---
            item {
                SectionHeader("9. BLOQUE 3 (FASES 11 A 15): SENSORES DE CAMPO & SUPERVIVENCIA", Icons.Default.Sensors)
                Text("Sensores ambientales, acústica sub-audible y balística táctica:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 11: Barómetro Táctico & Alerta de Tormenta",
                        subtitle = "Presión barométrica, altitud hipsométrica QNH y alerta de caídas bruscas",
                        icon = Icons.Default.CloudQueue,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "barometer" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 12: Enlace Acústico Sub-audible (18-20 kHz)",
                        subtitle = "Transmisión de datos inaudible Data-Over-Sound en silencio de radio RF absoluto",
                        icon = Icons.Default.VolumeUp,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "ultrasonic" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 13: Perímetro Geofence 3D (Safe Bubble)",
                        subtitle = "Burbuja de protección tridimensional con radar y detección de brechas hostiles",
                        icon = Icons.Default.Security,
                        accentColor = Color(0xFFFF5252),
                        onClick = { activeToolDialog = "safe_bubble" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 14: Calculadora Balística Táctica",
                        subtitle = "Cálculo de elevación MRAD/MOA, deriva de viento y corrección de ángulo (Rifleman)",
                        icon = Icons.Default.TrackChanges,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "ballistics" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 15: Red de Señuelos RF Honeypot",
                        subtitle = "Nodos fantasma con identidades falsas para despistar sistemas SIGINT hostiles",
                        icon = Icons.Default.FilterVintage,
                        accentColor = Color(0xFFE040FB),
                        onClick = { activeToolDialog = "honeypot" }
                    )
                }
            }

            // --- SECCIÓN 10: BLOQUE 4 (FASES 16 A 20) — SIGINT & CIBERSEGURIDAD ZERO-TRUST ---
            item {
                SectionHeader("10. BLOQUE 4 (FASES 16 A 20): SIGINT & CIBERSEGURIDAD", Icons.Default.Security)
                Text("Detección de ataques Sybil, criptografía post-cuántica y control de emisiones EMCON:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 16: Detector Forense de Ataques Sybil",
                        subtitle = "Inspección PHY/MAC, detección de identidades duplicadas y aislamiento de clones",
                        icon = Icons.Default.GppBad,
                        accentColor = Color(0xFFFF5252),
                        onClick = { activeToolDialog = "sybil" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 17: Bóveda Post-Cuántica ML-KEM Kyber-768",
                        subtitle = "Criptografía híbrida post-cuántica y encapsulación de secretos segura a futuro",
                        icon = Icons.Default.Lock,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "pqc_kyber" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 18: Micro-Blockchain Táctica de Misión",
                        subtitle = "Libro mayor descentralizado Proof-of-Authority para órdenes y auditoría forense",
                        icon = Icons.Default.AccountTree,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "mission_blockchain" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 19: Medidor de Huella Electromagnética LPI/LPD",
                        subtitle = "Estimación de probabilidad de intercepción enemiga y perfil de sigilo RF",
                        icon = Icons.Default.Sensors,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "rf_signature" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 20: Control de Emisiones EMCON Alpha",
                        subtitle = "Silencio de radio estricto por software ante amenazas de guerra electrónica",
                        icon = Icons.Default.VolumeOff,
                        accentColor = Color(0xFFFF1744),
                        onClick = { activeToolDialog = "emcon_alpha" }
                    )
                }
            }

            // --- SECCIÓN 11: BLOQUE 5 (FASES 21 A 25) — RECONOCIMIENTO AÉREO & MAPEO ---
            item {
                SectionHeader("11. BLOQUE 5 (FASES 21 A 25): RECONOCIMIENTO & MAPEO", Icons.Default.FlightTakeoff)
                Text("Fotogrametría satelital/aérea, telemetría UAV MAVLink, MEDEVAC y brújula astronómica:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 21: Mapeador Fotogramétrico Aéreo 2D",
                        subtitle = "Costura offline de ortomosaicos y cálculo de resolución de terreno GSD",
                        icon = Icons.Default.CameraAlt,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "photogrammetry" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 22: Transceptor Telemetría MAVLink UAV ISR",
                        subtitle = "Recepción de estados de vuelo de drones, control de waypoints y RTL de emergencia",
                        icon = Icons.Default.FlightTakeoff,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "mavlink_uav" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 23: Triaje Táctico 9-Line MEDEVAC & TCCC",
                        subtitle = "Protocolo estandarizado de evacuación médica y registro de tiempos de torniquete",
                        icon = Icons.Default.LocalHospital,
                        accentColor = Color(0xFFFF5252),
                        onClick = { activeToolDialog = "medevac_9line" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 24: Brújula Solar & Efemérides Astronómicas",
                        subtitle = "Navegación astronómica y rumbo norte verdadero ante bloqueo GNSS y brújula",
                        icon = Icons.Default.WbSunny,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "solar_compass" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 25: Pasarela Satelital SATCOM DTN Gateway",
                        subtitle = "Detección de constelaciones satelitales LEO y vaciado de paquetes en tránsito",
                        icon = Icons.Default.SatelliteAlt,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "satellite_gateway" }
                    )
                }
            }

            // --- SECCIÓN 12: BLOQUE 6 (FASES 26 A 30) — FUSIÓN C4ISR & MANDO MAESTRO ---
            item {
                SectionHeader("12. BLOQUE 6 (FASES 26 A 30): FUSIÓN C4ISR & MANDO", Icons.Default.DashboardCustomize)
                Text("Enmascaramiento espectral de voz, enrutador A*, reportes SALUTE, reloj PPS y consola maestra:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 26: Enmascarador Acústico Espectral de Voz",
                        subtitle = "Inversión de frecuencia 3.3 kHz y análisis de espectro FFT contra escuchas",
                        icon = Icons.Default.GraphicEq,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "voice_spectral" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 27: Enrutador Táctico de Escape A* (E&E)",
                        subtitle = "Generación heurística de rutas seguras evitando áreas de peligro y hostiles",
                        icon = Icons.Default.Navigation,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "astar_router" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 28: Generador de Informes de Inteligencia SALUTE",
                        subtitle = "Reportes estandarizados militares de tamaño, actividad, ubicación y equipo",
                        icon = Icons.Default.Assignment,
                        accentColor = TacticalAmberTertiary,
                        onClick = { activeToolDialog = "salute_intel" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 29: Sincronizador de Reloj Atómico PPS en Malla",
                        subtitle = "Alineación temporal sub-microsegundo para saltos FHSS coordinados",
                        icon = Icons.Default.AccessTime,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "clock_sync" }
                    )

                    TacticalToolActionCard(
                        title = "Fase 30: Consola Maestra C4ISR Multidominio",
                        subtitle = "Centro unificado de mando y control con supervisión de los 31 subsistemas tácticos",
                        icon = Icons.Default.DashboardCustomize,
                        accentColor = TacticalEmeraldSecondary,
                        onClick = { activeToolDialog = "c4isr_console" }
                    )
                }
            }

            // --- SECCIÓN 13: BLOQUE 7 (FASE 31) — CONTROL EXTERNO, API REST & ESTACIÓN DE MANDO PC ---
            item {
                SectionHeader("13. BLOQUE 7 (FASE 31): CONTROL EXTERNO & API REST", Icons.Default.Lan)
                Text("Servidor HTTP REST embebido y consola web para control de la app, sensores y hardware desde PC vía Wi-Fi o USB:", fontSize = 11.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalToolActionCard(
                        title = "Fase 31: Servidor de APIs REST & Control Remoto Maestro por PC",
                        subtitle = "Puerto 9090 • Consola Web SPA • 16 endpoints REST • Comandos CLI • Control de Hardware",
                        icon = Icons.Default.Computer,
                        accentColor = TacticalCyanPrimary,
                        onClick = { activeToolDialog = "external_api" }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("OMNICOMM FULL TACTICAL SUITE (64 FASES ARQUITECTÓNICAS)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TacticalCyanPrimary, fontFamily = FontFamily.Monospace)
                        Text("Todos los 64 subsistemas tácticos y registros forenses sincronizados con la consola C4ISR y API Externa.", fontSize = 9.sp, color = TacticalEmeraldSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Diálogos de las herramientas
    when (activeToolDialog) {
        "stego" -> SteganographyToolDialog(onDismiss = { activeToolDialog = null })
        "morse" -> MorseBeaconToolDialog(onDismiss = { activeToolDialog = null })
        "rf_spectrum" -> RfSpectrumToolDialog(onDismiss = { activeToolDialog = null })
        "sitrep" -> SitrepToolDialog(onDismiss = { activeToolDialog = null })
        "horizon_hud" -> HorizonHudToolDialog(onDismiss = { activeToolDialog = null })
        "dead_man" -> DeadMansSwitchToolDialog(onDismiss = { activeToolDialog = null })
        "mesh_graph" -> MeshTopologyToolDialog(onDismiss = { activeToolDialog = null })
        "lifi" -> VisualOpticalLiFiDialog(onDismiss = { activeToolDialog = null })
        "shamir" -> ShamirSecretSharingDialog(onDismiss = { activeToolDialog = null })
        "acoustic_shot" -> AcousticShotClassifierDialog(onDismiss = { activeToolDialog = null })
        "physio_bio" -> PhysioBioTelemetryDialog(onDismiss = { activeToolDialog = null })
        "zeroize" -> EmergencyZeroizeDialog(onDismiss = { activeToolDialog = null })
        "pdr" -> PdrNavigationDialog(onDismiss = { activeToolDialog = null })
        "fhss" -> FrequencyHoppingDialog(onDismiss = { activeToolDialog = null })
        "nato" -> NatoSymbologyDialog(onDismiss = { activeToolDialog = null })
        "tactical_agent" -> TacticalMissionAgentDialog(onDismiss = { activeToolDialog = null })
        "binary_compressor" -> TacticalBinaryCompressorDialog(onDismiss = { activeToolDialog = null })
        "barometer" -> BarometerStormDialog(onDismiss = { activeToolDialog = null })
        "ultrasonic" -> UltrasonicLinkDialog(onDismiss = { activeToolDialog = null })
        "safe_bubble" -> SafeBubble3DDialog(onDismiss = { activeToolDialog = null })
        "ballistics" -> BallisticsCalculatorDialog(onDismiss = { activeToolDialog = null })
        "honeypot" -> MeshHoneypotDialog(onDismiss = { activeToolDialog = null })

        // Bloque 4
        "sybil" -> SybilDetectorDialog(onDismiss = { activeToolDialog = null })
        "pqc_kyber" -> PostQuantumKyberDialog(onDismiss = { activeToolDialog = null })
        "mission_blockchain" -> MissionBlockchainDialog(onDismiss = { activeToolDialog = null })
        "rf_signature" -> RfSignatureDialog(onDismiss = { activeToolDialog = null })
        "emcon_alpha" -> EmconAlphaDialog(onDismiss = { activeToolDialog = null })

        // Bloque 5
        "photogrammetry" -> PhotogrammetryDialog(onDismiss = { activeToolDialog = null })
        "mavlink_uav" -> MavlinkUavDialog(onDismiss = { activeToolDialog = null })
        "medevac_9line" -> NineLineMedevacDialog(onDismiss = { activeToolDialog = null })
        "solar_compass" -> SolarCompassDialog(onDismiss = { activeToolDialog = null })
        "satellite_gateway" -> SatelliteGatewayDialog(onDismiss = { activeToolDialog = null })

        // Bloque 6
        "voice_spectral" -> VoiceSpectralDialog(onDismiss = { activeToolDialog = null })
        "astar_router" -> AStarRouterDialog(onDismiss = { activeToolDialog = null })
        "salute_intel" -> SaluteIntelDialog(onDismiss = { activeToolDialog = null })
        "clock_sync" -> MeshClockSyncDialog(onDismiss = { activeToolDialog = null })
        "c4isr_console" -> C4IsrMasterConsoleDialog(onDismiss = { activeToolDialog = null })

        // Bloque 7: Control Externo & API REST
        "external_api" -> ExternalApiServerDialog(onDismiss = { activeToolDialog = null })
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(18.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SelectableCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF1E293B) else Color(0xFF161B22),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) accentColor else Color.DarkGray),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isSelected) accentColor else Color.White)
                Text(subtitle, fontSize = 10.sp, color = Color.LightGray)
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = accentColor)
            )
        }
    }
}

@Composable
private fun ToggleFeatureCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    badgeText: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161B22),
        border = BorderStroke(1.dp, if (isEnabled) TacticalCyanPrimary.copy(alpha = 0.5f) else Color.DarkGray),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF21262D)) {
                        Text(badgeText, fontSize = 8.sp, color = TacticalCyanPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontFamily = FontFamily.Monospace)
                    }
                }
                Text(description, fontSize = 10.sp, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = TacticalCyanPrimary
                )
            )
        }
    }
}

@Composable
private fun TacticalToolActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161B22),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    Text(subtitle, fontSize = 10.sp, color = Color.LightGray)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}
