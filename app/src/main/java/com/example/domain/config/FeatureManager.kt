package com.example.domain.config

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavigationLayoutStyle(
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    GRID_DASHBOARD("Hub de Módulos (Rejilla)", "Diseño táctico central con tarjetas dinámicas", "GridView"),
    BOTTOM_NAV_BAR("Barra de Navegación Inferior", "Acceso directo ergonómico de 5 pestañas", "BottomBar"),
    TACTICAL_FLOATING_DOCK("Dock Flotante Táctico", "Botones translúcidos superpuestos en pantalla", "Dock"),
    SIDE_NAVIGATION_RAIL("Rail Lateral Táctico", "Barra lateral vertical para uso a dos manos o tablet", "NavRail")
}

enum class MessagingStyle(
    val title: String,
    val subtitle: String
) {
    TACTICAL_TERMINAL("Terminal Táctico de Mando", "Monospace de alta densidad con timestamps milimétricos y prefijos de canal"),
    MATERIAL_BUBBLES("Burbujas Modernas M3", "Burbujas redondeadas fluidas con elevación y sombras suaves"),
    CYBERPUNK_MATRIX("Matriz Neón Cyberpunk", "Estilo fósforo verde con bordes luminosos y escáner de ruido"),
    COMPACT_STREAM("Flujo Compacto de Campo", "Líneas ultra-densas para máxima visualización de texto en pantalla")
}

enum class ThemePalette(
    val title: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long
) {
    CYAN_TACTICAL("Cyan Táctico / Stealth", 0xFF00E5FF, 0xFF69F0AE, 0xFF0B0F17),
    OLED_EMERALD("Verde Militar / OLED Night", 0xFF00E676, 0xFFFFD600, 0xFF000000),
    AMBER_NIGHT_OPS("Ámbar Operaciones Nocturnas", 0xFFFF9100, 0xFFFF5252, 0xFF120E0A),
    CYBER_PURPLE("Neon Violeta / Cyberpunk", 0xFFD500F9, 0xFF00E5FF, 0xFF0A0714)
}

enum class RadarVisualEngine(
    val title: String,
    val description: String
) {
    SONAR_CIRCULAR("Barrido Sonar 360°", "Línea de barrido radial continua con detección por eco"),
    HUD_GRID_TOPOGRAPHIC("Cuadrícula Militar HUD", "Retícula ortogonal con coordenadas militares MGRS"),
    NODE_CONSTELLATION("Grafo de Constelación", "Topología de nodos enlazados por vectores de calidad RSSI")
}

/**
 * Contenedor de Estado Centralizado (FeatureManager) para orquestar la activación,
 * alternancia y personalización de todos los módulos del sistema sin eliminar código previo.
 */
class FeatureManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("omnicomm_feature_config", Context.MODE_PRIVATE)

    // --- Estilos de Navegación e Interfaz (Mutuamente Exclusivos) ---
    private val _navigationLayout = MutableStateFlow(
        NavigationLayoutStyle.valueOf(prefs.getString(KEY_NAV_LAYOUT, NavigationLayoutStyle.BOTTOM_NAV_BAR.name) ?: NavigationLayoutStyle.BOTTOM_NAV_BAR.name)
    )
    val navigationLayout: StateFlow<NavigationLayoutStyle> = _navigationLayout.asStateFlow()

    private val _messagingStyle = MutableStateFlow(
        MessagingStyle.valueOf(prefs.getString(KEY_MSG_STYLE, MessagingStyle.TACTICAL_TERMINAL.name) ?: MessagingStyle.TACTICAL_TERMINAL.name)
    )
    val messagingStyle: StateFlow<MessagingStyle> = _messagingStyle.asStateFlow()

    private val _themePalette = MutableStateFlow(
        ThemePalette.valueOf(prefs.getString(KEY_THEME_PALETTE, ThemePalette.CYAN_TACTICAL.name) ?: ThemePalette.CYAN_TACTICAL.name)
    )
    val themePalette: StateFlow<ThemePalette> = _themePalette.asStateFlow()

    private val _radarVisualEngine = MutableStateFlow(
        RadarVisualEngine.valueOf(prefs.getString(KEY_RADAR_ENGINE, RadarVisualEngine.SONAR_CIRCULAR.name) ?: RadarVisualEngine.SONAR_CIRCULAR.name)
    )
    val radarVisualEngine: StateFlow<RadarVisualEngine> = _radarVisualEngine.asStateFlow()

    // --- Módulos y Herramientas Funcionales (Interruptores Booleanos Independientes) ---
    private val _enableManDownAlert = MutableStateFlow(prefs.getBoolean(KEY_MAN_DOWN, true))
    val enableManDownAlert: StateFlow<Boolean> = _enableManDownAlert.asStateFlow()

    private val _enableStoreAndForward = MutableStateFlow(prefs.getBoolean(KEY_STORE_FORWARD, true))
    val enableStoreAndForward: StateFlow<Boolean> = _enableStoreAndForward.asStateFlow()

    private val _enableOpticalAiOcr = MutableStateFlow(prefs.getBoolean(KEY_AI_OCR, true))
    val enableOpticalAiOcr: StateFlow<Boolean> = _enableOpticalAiOcr.asStateFlow()

    private val _enableGisMarkers = MutableStateFlow(prefs.getBoolean(KEY_GIS_MARKERS, true))
    val enableGisMarkers: StateFlow<Boolean> = _enableGisMarkers.asStateFlow()

    private val _enableAdaptivePower = MutableStateFlow(prefs.getBoolean(KEY_ADAPTIVE_POWER, true))
    val enableAdaptivePower: StateFlow<Boolean> = _enableAdaptivePower.asStateFlow()

    private val _enableDuressSecurity = MutableStateFlow(prefs.getBoolean(KEY_DURESS_SEC, true))
    val enableDuressSecurity: StateFlow<Boolean> = _enableDuressSecurity.asStateFlow()

    // --- 9 NUEVAS RECOMENDACIONES DE MÓDULOS TÁCTICOS ---
    private val _enableSteganographyVault = MutableStateFlow(prefs.getBoolean(KEY_STEGANOGRAPHY, true))
    val enableSteganographyVault: StateFlow<Boolean> = _enableSteganographyVault.asStateFlow()

    private val _enableMorseBeaconEngine = MutableStateFlow(prefs.getBoolean(KEY_MORSE_BEACON, true))
    val enableMorseBeaconEngine: StateFlow<Boolean> = _enableMorseBeaconEngine.asStateFlow()

    private val _enableRfSpectrumSimulator = MutableStateFlow(prefs.getBoolean(KEY_RF_SPECTRUM, true))
    val enableRfSpectrumSimulator: StateFlow<Boolean> = _enableRfSpectrumSimulator.asStateFlow()

    private val _enableSitrepReporter = MutableStateFlow(prefs.getBoolean(KEY_SITREP_REPORTER, true))
    val enableSitrepReporter: StateFlow<Boolean> = _enableSitrepReporter.asStateFlow()

    private val _enableThermalNightShader = MutableStateFlow(prefs.getBoolean(KEY_THERMAL_SHADER, true))
    val enableThermalNightShader: StateFlow<Boolean> = _enableThermalNightShader.asStateFlow()

    private val _enableHorizonHudLevel = MutableStateFlow(prefs.getBoolean(KEY_HORIZON_HUD, true))
    val enableHorizonHudLevel: StateFlow<Boolean> = _enableHorizonHudLevel.asStateFlow()

    private val _enableDeadMansSwitch = MutableStateFlow(prefs.getBoolean(KEY_DEAD_MANS_SWITCH, false))
    val enableDeadMansSwitch: StateFlow<Boolean> = _enableDeadMansSwitch.asStateFlow()

    private val _enableMeshTopologyGraph = MutableStateFlow(prefs.getBoolean(KEY_TOPOLOGY_GRAPH, true))
    val enableMeshTopologyGraph: StateFlow<Boolean> = _enableMeshTopologyGraph.asStateFlow()

    private val _enableTacticalHaptics = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS, true))
    val enableTacticalHaptics: StateFlow<Boolean> = _enableTacticalHaptics.asStateFlow()

    // --- FASES 1 A 5: GUERRA ELECTRÓNICA, CRIPTOGRAFÍA & RESILIENCIA RF ---
    private val _enableVisualLiFi = MutableStateFlow(prefs.getBoolean(KEY_VISUAL_LIFI, true))
    val enableVisualLiFi: StateFlow<Boolean> = _enableVisualLiFi.asStateFlow()

    private val _enableShamirVault = MutableStateFlow(prefs.getBoolean(KEY_SHAMIR_VAULT, true))
    val enableShamirVault: StateFlow<Boolean> = _enableShamirVault.asStateFlow()

    private val _enableAcousticShotDetector = MutableStateFlow(prefs.getBoolean(KEY_ACOUSTIC_SHOT, true))
    val enableAcousticShotDetector: StateFlow<Boolean> = _enableAcousticShotDetector.asStateFlow()

    private val _enablePhysioBioTelemetry = MutableStateFlow(prefs.getBoolean(KEY_PHYSIO_BIO, true))
    val enablePhysioBioTelemetry: StateFlow<Boolean> = _enablePhysioBioTelemetry.asStateFlow()

    private val _enableEmergencyZeroize = MutableStateFlow(prefs.getBoolean(KEY_EMERGENCY_ZEROIZE, true))
    val enableEmergencyZeroize: StateFlow<Boolean> = _enableEmergencyZeroize.asStateFlow()

    // --- BLOQUE 2 (FASES 6 A 10): C2 TÁCTICO & AUTONOMÍA MESH ---
    private val _enablePdrNavigation = MutableStateFlow(prefs.getBoolean(KEY_PDR_NAVIGATION, true))
    val enablePdrNavigation: StateFlow<Boolean> = _enablePdrNavigation.asStateFlow()

    private val _enableFrequencyHopping = MutableStateFlow(prefs.getBoolean(KEY_FHSS_HOPPING, true))
    val enableFrequencyHopping: StateFlow<Boolean> = _enableFrequencyHopping.asStateFlow()

    private val _enableNatoSymbology = MutableStateFlow(prefs.getBoolean(KEY_NATO_SYMBOLOGY, true))
    val enableNatoSymbology: StateFlow<Boolean> = _enableNatoSymbology.asStateFlow()

    private val _enableTacticalMissionAgent = MutableStateFlow(prefs.getBoolean(KEY_TACTICAL_AGENT, true))
    val enableTacticalMissionAgent: StateFlow<Boolean> = _enableTacticalMissionAgent.asStateFlow()

    private val _enableBinaryCompressor = MutableStateFlow(prefs.getBoolean(KEY_BINARY_COMPRESSOR, true))
    val enableBinaryCompressor: StateFlow<Boolean> = _enableBinaryCompressor.asStateFlow()

    // --- BLOQUE 3 (FASES 11 A 15): SENSORES DE CAMPO & SUPERVIVENCIA ---
    private val _enableBarometerStorm = MutableStateFlow(prefs.getBoolean(KEY_BAROMETER_STORM, true))
    val enableBarometerStorm: StateFlow<Boolean> = _enableBarometerStorm.asStateFlow()

    private val _enableUltrasonicLink = MutableStateFlow(prefs.getBoolean(KEY_ULTRASONIC_LINK, true))
    val enableUltrasonicLink: StateFlow<Boolean> = _enableUltrasonicLink.asStateFlow()

    private val _enableSafeBubble3D = MutableStateFlow(prefs.getBoolean(KEY_SAFE_BUBBLE_3D, true))
    val enableSafeBubble3D: StateFlow<Boolean> = _enableSafeBubble3D.asStateFlow()

    private val _enableBallisticsCalc = MutableStateFlow(prefs.getBoolean(KEY_BALLISTICS_CALC, true))
    val enableBallisticsCalc: StateFlow<Boolean> = _enableBallisticsCalc.asStateFlow()

    private val _enableMeshHoneypot = MutableStateFlow(prefs.getBoolean(KEY_MESH_HONEYPOT, true))
    val enableMeshHoneypot: StateFlow<Boolean> = _enableMeshHoneypot.asStateFlow()

    // --- BLOQUE 4 (FASES 16 A 20): SIGINT & CIBERSEGURIDAD ZERO-TRUST ---
    private val _enableSybilDetector = MutableStateFlow(prefs.getBoolean(KEY_SYBIL_DETECTOR, true))
    val enableSybilDetector: StateFlow<Boolean> = _enableSybilDetector.asStateFlow()

    private val _enablePostQuantumKyber = MutableStateFlow(prefs.getBoolean(KEY_PQC_KYBER, true))
    val enablePostQuantumKyber: StateFlow<Boolean> = _enablePostQuantumKyber.asStateFlow()

    private val _enableMissionBlockchain = MutableStateFlow(prefs.getBoolean(KEY_MISSION_BLOCKCHAIN, true))
    val enableMissionBlockchain: StateFlow<Boolean> = _enableMissionBlockchain.asStateFlow()

    private val _enableRfSignatureMeter = MutableStateFlow(prefs.getBoolean(KEY_RF_SIGNATURE, true))
    val enableRfSignatureMeter: StateFlow<Boolean> = _enableRfSignatureMeter.asStateFlow()

    private val _enableEmconAlpha = MutableStateFlow(prefs.getBoolean(KEY_EMCON_ALPHA, true))
    val enableEmconAlpha: StateFlow<Boolean> = _enableEmconAlpha.asStateFlow()

    // --- BLOQUE 5 (FASES 21 A 25): RECONOCIMIENTO AÉREO & MAPEO TÁCTICO ---
    private val _enablePhotogrammetry = MutableStateFlow(prefs.getBoolean(KEY_PHOTOGRAMMETRY, true))
    val enablePhotogrammetry: StateFlow<Boolean> = _enablePhotogrammetry.asStateFlow()

    private val _enableMavlinkUav = MutableStateFlow(prefs.getBoolean(KEY_MAVLINK_UAV, true))
    val enableMavlinkUav: StateFlow<Boolean> = _enableMavlinkUav.asStateFlow()

    private val _enable9LineMedevac = MutableStateFlow(prefs.getBoolean(KEY_9LINE_MEDEVAC, true))
    val enable9LineMedevac: StateFlow<Boolean> = _enable9LineMedevac.asStateFlow()

    private val _enableSolarCompass = MutableStateFlow(prefs.getBoolean(KEY_SOLAR_COMPASS, true))
    val enableSolarCompass: StateFlow<Boolean> = _enableSolarCompass.asStateFlow()

    private val _enableSatelliteGateway = MutableStateFlow(prefs.getBoolean(KEY_SATELLITE_GATEWAY, true))
    val enableSatelliteGateway: StateFlow<Boolean> = _enableSatelliteGateway.asStateFlow()

    // --- BLOQUE 6 (FASES 26 A 30): FUSIÓN C4ISR & MANDO MAESTRO ---
    private val _enableVoiceSpectral = MutableStateFlow(prefs.getBoolean(KEY_VOICE_SPECTRAL, true))
    val enableVoiceSpectral: StateFlow<Boolean> = _enableVoiceSpectral.asStateFlow()

    private val _enableAStarRouter = MutableStateFlow(prefs.getBoolean(KEY_ASTAR_ROUTER, true))
    val enableAStarRouter: StateFlow<Boolean> = _enableAStarRouter.asStateFlow()

    private val _enableSaluteIntel = MutableStateFlow(prefs.getBoolean(KEY_SALUTE_INTEL, true))
    val enableSaluteIntel: StateFlow<Boolean> = _enableSaluteIntel.asStateFlow()

    private val _enableMeshClockSync = MutableStateFlow(prefs.getBoolean(KEY_CLOCK_SYNC, true))
    val enableMeshClockSync: StateFlow<Boolean> = _enableMeshClockSync.asStateFlow()

    private val _enableC4IsrConsole = MutableStateFlow(prefs.getBoolean(KEY_C4ISR_CONSOLE, true))
    val enableC4IsrConsole: StateFlow<Boolean> = _enableC4IsrConsole.asStateFlow()

    // --- MODO LITE (VERSIÓN LIGERA TÁCTICA) ---
    private val _isLiteModeActive = MutableStateFlow(prefs.getBoolean(KEY_LITE_MODE_ACTIVE, false))
    val isLiteModeActive: StateFlow<Boolean> = _isLiteModeActive.asStateFlow()

    private val _currentLiteProfile = MutableStateFlow(
        try {
            LiteProfile.valueOf(prefs.getString(KEY_LITE_PROFILE, LiteProfile.BALANCED_LITE.name) ?: LiteProfile.BALANCED_LITE.name)
        } catch (e: Exception) {
            LiteProfile.BALANCED_LITE
        }
    )
    val currentLiteProfile: StateFlow<LiteProfile> = _currentLiteProfile.asStateFlow()

    companion object {
        private const val KEY_LITE_MODE_ACTIVE = "cfg_lite_mode_active"
        private const val KEY_LITE_PROFILE = "cfg_lite_profile"

        private const val KEY_NAV_LAYOUT = "cfg_nav_layout"
        private const val KEY_MSG_STYLE = "cfg_msg_style"
        private const val KEY_THEME_PALETTE = "cfg_theme_palette"
        private const val KEY_RADAR_ENGINE = "cfg_radar_engine"

        private const val KEY_MAN_DOWN = "cfg_man_down"
        private const val KEY_STORE_FORWARD = "cfg_store_forward"
        private const val KEY_AI_OCR = "cfg_ai_ocr"
        private const val KEY_GIS_MARKERS = "cfg_gis_markers"
        private const val KEY_ADAPTIVE_POWER = "cfg_adaptive_power"
        private const val KEY_DURESS_SEC = "cfg_duress_sec"

        private const val KEY_STEGANOGRAPHY = "cfg_steganography"
        private const val KEY_MORSE_BEACON = "cfg_morse_beacon"
        private const val KEY_RF_SPECTRUM = "cfg_rf_spectrum"
        private const val KEY_SITREP_REPORTER = "cfg_sitrep_reporter"
        private const val KEY_THERMAL_SHADER = "cfg_thermal_shader"
        private const val KEY_HORIZON_HUD = "cfg_horizon_hud"
        private const val KEY_DEAD_MANS_SWITCH = "cfg_dead_mans_switch"
        private const val KEY_TOPOLOGY_GRAPH = "cfg_topology_graph"
        private const val KEY_HAPTICS = "cfg_haptics"

        private const val KEY_VISUAL_LIFI = "cfg_visual_lifi"
        private const val KEY_SHAMIR_VAULT = "cfg_shamir_vault"
        private const val KEY_ACOUSTIC_SHOT = "cfg_acoustic_shot"
        private const val KEY_PHYSIO_BIO = "cfg_physio_bio"
        private const val KEY_EMERGENCY_ZEROIZE = "cfg_emergency_zeroize"

        private const val KEY_PDR_NAVIGATION = "cfg_pdr_navigation"
        private const val KEY_FHSS_HOPPING = "cfg_fhss_hopping"
        private const val KEY_NATO_SYMBOLOGY = "cfg_nato_symbology"
        private const val KEY_TACTICAL_AGENT = "cfg_tactical_agent"
        private const val KEY_BINARY_COMPRESSOR = "cfg_binary_compressor"

        private const val KEY_BAROMETER_STORM = "cfg_barometer_storm"
        private const val KEY_ULTRASONIC_LINK = "cfg_ultrasonic_link"
        private const val KEY_SAFE_BUBBLE_3D = "cfg_safe_bubble_3d"
        private const val KEY_BALLISTICS_CALC = "cfg_ballistics_calc"
        private const val KEY_MESH_HONEYPOT = "cfg_mesh_honeypot"

        // Bloque 4
        private const val KEY_SYBIL_DETECTOR = "cfg_sybil_detector"
        private const val KEY_PQC_KYBER = "cfg_pqc_kyber"
        private const val KEY_MISSION_BLOCKCHAIN = "cfg_mission_blockchain"
        private const val KEY_RF_SIGNATURE = "cfg_rf_signature"
        private const val KEY_EMCON_ALPHA = "cfg_emcon_alpha"

        // Bloque 5
        private const val KEY_PHOTOGRAMMETRY = "cfg_photogrammetry"
        private const val KEY_MAVLINK_UAV = "cfg_mavlink_uav"
        private const val KEY_9LINE_MEDEVAC = "cfg_9line_medevac"
        private const val KEY_SOLAR_COMPASS = "cfg_solar_compass"
        private const val KEY_SATELLITE_GATEWAY = "cfg_satellite_gateway"

        // Bloque 6
        private const val KEY_VOICE_SPECTRAL = "cfg_voice_spectral"
        private const val KEY_ASTAR_ROUTER = "cfg_astar_router"
        private const val KEY_SALUTE_INTEL = "cfg_salute_intel"
        private const val KEY_CLOCK_SYNC = "cfg_clock_sync"
        private const val KEY_C4ISR_CONSOLE = "cfg_c4isr_console"

        @Volatile
        private var INSTANCE: FeatureManager? = null

        fun getInstance(context: Context): FeatureManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeatureManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Setters con persistencia automática y log de auditoría
    fun setNavigationLayout(style: NavigationLayoutStyle) {
        _navigationLayout.value = style
        prefs.edit().putString(KEY_NAV_LAYOUT, style.name).apply()
        logConfigChange("Estilo de Navegación", style.title)
    }

    fun setMessagingStyle(style: MessagingStyle) {
        _messagingStyle.value = style
        prefs.edit().putString(KEY_MSG_STYLE, style.name).apply()
        logConfigChange("Estilo de Mensajería", style.title)
    }

    fun setThemePalette(palette: ThemePalette) {
        _themePalette.value = palette
        prefs.edit().putString(KEY_THEME_PALETTE, palette.name).apply()
        logConfigChange("Paleta de Color", palette.title)
    }

    fun setRadarVisualEngine(engine: RadarVisualEngine) {
        _radarVisualEngine.value = engine
        prefs.edit().putString(KEY_RADAR_ENGINE, engine.name).apply()
        logConfigChange("Motor Visual de Radar", engine.title)
    }

    fun toggleManDownAlert(enable: Boolean) {
        _enableManDownAlert.value = enable
        prefs.edit().putBoolean(KEY_MAN_DOWN, enable).apply()
        logConfigChange("Alerta Man-Down", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleStoreAndForward(enable: Boolean) {
        _enableStoreAndForward.value = enable
        prefs.edit().putBoolean(KEY_STORE_FORWARD, enable).apply()
        logConfigChange("Enrutamiento Multi-Salto", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleOpticalAiOcr(enable: Boolean) {
        _enableOpticalAiOcr.value = enable
        prefs.edit().putBoolean(KEY_AI_OCR, enable).apply()
        logConfigChange("Análisis Óptico AI", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleGisMarkers(enable: Boolean) {
        _enableGisMarkers.value = enable
        prefs.edit().putBoolean(KEY_GIS_MARKERS, enable).apply()
        logConfigChange("Marcadores GIS", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleAdaptivePower(enable: Boolean) {
        _enableAdaptivePower.value = enable
        prefs.edit().putBoolean(KEY_ADAPTIVE_POWER, enable).apply()
        logConfigChange("Control de Potencia Adaptativa", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleDuressSecurity(enable: Boolean) {
        _enableDuressSecurity.value = enable
        prefs.edit().putBoolean(KEY_DURESS_SEC, enable).apply()
        logConfigChange("Seguridad de Coacción (Duress PIN)", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleSteganographyVault(enable: Boolean) {
        _enableSteganographyVault.value = enable
        prefs.edit().putBoolean(KEY_STEGANOGRAPHY, enable).apply()
        logConfigChange("Bóveda Esteganográfica", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleMorseBeaconEngine(enable: Boolean) {
        _enableMorseBeaconEngine.value = enable
        prefs.edit().putBoolean(KEY_MORSE_BEACON, enable).apply()
        logConfigChange("Sintetizador Baliza Morse", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleRfSpectrumSimulator(enable: Boolean) {
        _enableRfSpectrumSimulator.value = enable
        prefs.edit().putBoolean(KEY_RF_SPECTRUM, enable).apply()
        logConfigChange("Analizador de Espectro RF", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleSitrepReporter(enable: Boolean) {
        _enableSitrepReporter.value = enable
        prefs.edit().putBoolean(KEY_SITREP_REPORTER, enable).apply()
        logConfigChange("Generador SITREP Militar", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleThermalNightShader(enable: Boolean) {
        _enableThermalNightShader.value = enable
        prefs.edit().putBoolean(KEY_THERMAL_SHADER, enable).apply()
        logConfigChange("Filtro Visión Térmica / Nocturna", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleHorizonHudLevel(enable: Boolean) {
        _enableHorizonHudLevel.value = enable
        prefs.edit().putBoolean(KEY_HORIZON_HUD, enable).apply()
        logConfigChange("Horizonte Artificial & Brújula HUD", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleDeadMansSwitch(enable: Boolean) {
        _enableDeadMansSwitch.value = enable
        prefs.edit().putBoolean(KEY_DEAD_MANS_SWITCH, enable).apply()
        logConfigChange("Interruptor de Hombre Muerto", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleMeshTopologyGraph(enable: Boolean) {
        _enableMeshTopologyGraph.value = enable
        prefs.edit().putBoolean(KEY_TOPOLOGY_GRAPH, enable).apply()
        logConfigChange("Grafo de Topología Malla", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleTacticalHaptics(enable: Boolean) {
        _enableTacticalHaptics.value = enable
        prefs.edit().putBoolean(KEY_HAPTICS, enable).apply()
        logConfigChange("Háptica Táctica", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleVisualLiFi(enable: Boolean) {
        _enableVisualLiFi.value = enable
        prefs.edit().putBoolean(KEY_VISUAL_LIFI, enable).apply()
        logConfigChange("Transmisor Li-Fi Óptico", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleShamirVault(enable: Boolean) {
        _enableShamirVault.value = enable
        prefs.edit().putBoolean(KEY_SHAMIR_VAULT, enable).apply()
        logConfigChange("Bóveda Shamir Secret Sharing", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleAcousticShotDetector(enable: Boolean) {
        _enableAcousticShotDetector.value = enable
        prefs.edit().putBoolean(KEY_ACOUSTIC_SHOT, enable).apply()
        logConfigChange("Clasificador Acústico de Disparos", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun togglePhysioBioTelemetry(enable: Boolean) {
        _enablePhysioBioTelemetry.value = enable
        prefs.edit().putBoolean(KEY_PHYSIO_BIO, enable).apply()
        logConfigChange("Telemetría Biológica de Operador", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleEmergencyZeroize(enable: Boolean) {
        _enableEmergencyZeroize.value = enable
        prefs.edit().putBoolean(KEY_EMERGENCY_ZEROIZE, enable).apply()
        logConfigChange("Protocolo Destrucción Zeroize", if (enable) "Habilitado" else "Deshabilitado")
    }

    // --- TOGGLES BLOQUE 2 (FASES 6 A 10) ---
    fun togglePdrNavigation(enable: Boolean) {
        _enablePdrNavigation.value = enable
        prefs.edit().putBoolean(KEY_PDR_NAVIGATION, enable).apply()
        logConfigChange("Navegación Inercial PDR", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleFrequencyHopping(enable: Boolean) {
        _enableFrequencyHopping.value = enable
        prefs.edit().putBoolean(KEY_FHSS_HOPPING, enable).apply()
        logConfigChange("Salto de Frecuencia FHSS", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleNatoSymbology(enable: Boolean) {
        _enableNatoSymbology.value = enable
        prefs.edit().putBoolean(KEY_NATO_SYMBOLOGY, enable).apply()
        logConfigChange("Simbología OTAN APP-6", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleTacticalMissionAgent(enable: Boolean) {
        _enableTacticalMissionAgent.value = enable
        prefs.edit().putBoolean(KEY_TACTICAL_AGENT, enable).apply()
        logConfigChange("Agente Táctico de Misión", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleBinaryCompressor(enable: Boolean) {
        _enableBinaryCompressor.value = enable
        prefs.edit().putBoolean(KEY_BINARY_COMPRESSOR, enable).apply()
        logConfigChange("Compresor Binario Ultradenso", if (enable) "Habilitado" else "Deshabilitado")
    }

    // --- TOGGLES BLOQUE 3 (FASES 11 A 15) ---
    fun toggleBarometerStorm(enable: Boolean) {
        _enableBarometerStorm.value = enable
        prefs.edit().putBoolean(KEY_BAROMETER_STORM, enable).apply()
        logConfigChange("Barómetro y Alerta de Tormenta", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleUltrasonicLink(enable: Boolean) {
        _enableUltrasonicLink.value = enable
        prefs.edit().putBoolean(KEY_ULTRASONIC_LINK, enable).apply()
        logConfigChange("Enlace Acústico Sub-audible", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleSafeBubble3D(enable: Boolean) {
        _enableSafeBubble3D.value = enable
        prefs.edit().putBoolean(KEY_SAFE_BUBBLE_3D, enable).apply()
        logConfigChange("Burbuja de Seguridad 3D", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleBallisticsCalc(enable: Boolean) {
        _enableBallisticsCalc.value = enable
        prefs.edit().putBoolean(KEY_BALLISTICS_CALC, enable).apply()
        logConfigChange("Calculadora Balística Táctica", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleMeshHoneypot(enable: Boolean) {
        _enableMeshHoneypot.value = enable
        prefs.edit().putBoolean(KEY_MESH_HONEYPOT, enable).apply()
        logConfigChange("Red Señuelos RF Honeypot", if (enable) "Habilitado" else "Deshabilitado")
    }

    // --- TOGGLES BLOQUE 4 (FASES 16 A 20) ---
    fun toggleSybilDetector(enable: Boolean) {
        _enableSybilDetector.value = enable
        prefs.edit().putBoolean(KEY_SYBIL_DETECTOR, enable).apply()
        logConfigChange("Detector de Ataques Sybil", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun togglePostQuantumKyber(enable: Boolean) {
        _enablePostQuantumKyber.value = enable
        prefs.edit().putBoolean(KEY_PQC_KYBER, enable).apply()
        logConfigChange("Criptografía Post-Cuántica Kyber", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleMissionBlockchain(enable: Boolean) {
        _enableMissionBlockchain.value = enable
        prefs.edit().putBoolean(KEY_MISSION_BLOCKCHAIN, enable).apply()
        logConfigChange("Blockchain Táctica de Misión", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleRfSignatureMeter(enable: Boolean) {
        _enableRfSignatureMeter.value = enable
        prefs.edit().putBoolean(KEY_RF_SIGNATURE, enable).apply()
        logConfigChange("Medidor de Huella RF", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleEmconAlpha(enable: Boolean) {
        _enableEmconAlpha.value = enable
        prefs.edit().putBoolean(KEY_EMCON_ALPHA, enable).apply()
        logConfigChange("Control de Emisiones EMCON Alpha", if (enable) "Habilitado" else "Deshabilitado")
    }

    // --- TOGGLES BLOQUE 5 (FASES 21 A 25) ---
    fun togglePhotogrammetry(enable: Boolean) {
        _enablePhotogrammetry.value = enable
        prefs.edit().putBoolean(KEY_PHOTOGRAMMETRY, enable).apply()
        logConfigChange("Mapeador Fotogramétrico Aéreo", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleMavlinkUav(enable: Boolean) {
        _enableMavlinkUav.value = enable
        prefs.edit().putBoolean(KEY_MAVLINK_UAV, enable).apply()
        logConfigChange("Telemetría MAVLink Drones", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggle9LineMedevac(enable: Boolean) {
        _enable9LineMedevac.value = enable
        prefs.edit().putBoolean(KEY_9LINE_MEDEVAC, enable).apply()
        logConfigChange("Triaje 9-Line MEDEVAC TCCC", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleSolarCompass(enable: Boolean) {
        _enableSolarCompass.value = enable
        prefs.edit().putBoolean(KEY_SOLAR_COMPASS, enable).apply()
        logConfigChange("Brújula Solar y Efemérides", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleSatelliteGateway(enable: Boolean) {
        _enableSatelliteGateway.value = enable
        prefs.edit().putBoolean(KEY_SATELLITE_GATEWAY, enable).apply()
        logConfigChange("Enlace Satelital SATCOM Gateway", if (enable) "Habilitado" else "Deshabilitado")
    }

    // --- TOGGLES BLOQUE 6 (FASES 26 A 30) ---
    fun toggleVoiceSpectral(enable: Boolean) {
        _enableVoiceSpectral.value = enable
        prefs.edit().putBoolean(KEY_VOICE_SPECTRAL, enable).apply()
        logConfigChange("Enmascarador Acústico Espectral", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleAStarRouter(enable: Boolean) {
        _enableAStarRouter.value = enable
        prefs.edit().putBoolean(KEY_ASTAR_ROUTER, enable).apply()
        logConfigChange("Enrutador Táctico de Escape A*", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleSaluteIntel(enable: Boolean) {
        _enableSaluteIntel.value = enable
        prefs.edit().putBoolean(KEY_SALUTE_INTEL, enable).apply()
        logConfigChange("Informes de Inteligencia SALUTE", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleMeshClockSync(enable: Boolean) {
        _enableMeshClockSync.value = enable
        prefs.edit().putBoolean(KEY_CLOCK_SYNC, enable).apply()
        logConfigChange("Sincronización de Reloj Atómico PPS", if (enable) "Habilitado" else "Deshabilitado")
    }

    fun toggleC4IsrConsole(enable: Boolean) {
        _enableC4IsrConsole.value = enable
        prefs.edit().putBoolean(KEY_C4ISR_CONSOLE, enable).apply()
        logConfigChange("Consola Maestra C4ISR", if (enable) "Habilitado" else "Deshabilitado")
    }

    // --- MÉTODOS DE CONTROL DEL MODO LITE (GESTOR MODULAR) ---
    fun setLiteModeActive(active: Boolean) {
        _isLiteModeActive.value = active
        prefs.edit().putBoolean(KEY_LITE_MODE_ACTIVE, active).apply()
        logConfigChange("Modo Lite Master", if (active) "ACTIVADO (Versión Ligera)" else "DESACTIVADO (Modo Completo)")
        if (active) {
            applyLiteProfile(_currentLiteProfile.value)
        }
    }

    fun setLiteProfile(profile: LiteProfile) {
        _currentLiteProfile.value = profile
        prefs.edit().putString(KEY_LITE_PROFILE, profile.name).apply()
        logConfigChange("Perfil Lite", profile.title)
        if (_isLiteModeActive.value) {
            applyLiteProfile(profile)
        }
    }

    fun applyLiteProfile(profile: LiteProfile) {
        when (profile) {
            LiteProfile.FULL_TACTICAL -> {
                // Habilitar todos los módulos
                toggleThermalNightShader(true)
                toggleAcousticShotDetector(true)
                toggleUltrasonicLink(true)
                togglePdrNavigation(true)
                toggleBarometerStorm(true)
                toggleFrequencyHopping(true)
                toggleBinaryCompressor(true)
                toggleSafeBubble3D(true)
                toggleBallisticsCalc(true)
                toggleMeshHoneypot(true)
                toggleSybilDetector(true)
                togglePostQuantumKyber(true)
                toggleMissionBlockchain(true)
                toggleRfSignatureMeter(true)
                togglePhotogrammetry(true)
                toggleMavlinkUav(true)
                toggle9LineMedevac(true)
                toggleSolarCompass(true)
                toggleSatelliteGateway(true)
                toggleVoiceSpectral(true)
                toggleAStarRouter(true)
                toggleSaluteIntel(true)
                toggleMeshClockSync(true)
                toggleC4IsrConsole(true)
                toggleVisualLiFi(true)
                toggleShamirVault(true)
                togglePhysioBioTelemetry(true)
                toggleEmergencyZeroize(true)
                toggleOpticalAiOcr(true)
                toggleGisMarkers(true)
                toggleSteganographyVault(true)
                toggleMorseBeaconEngine(true)
                toggleRfSpectrumSimulator(true)
                toggleSitrepReporter(true)
                toggleHorizonHudLevel(true)
                toggleMeshTopologyGraph(true)
                toggleTacticalHaptics(true)
            }
            LiteProfile.BALANCED_LITE -> {
                // Apagar shaders de cámara intensivos, audio DSP continuo, transceptores ultrasonido y honeypot
                toggleThermalNightShader(false)
                toggleAcousticShotDetector(false)
                toggleUltrasonicLink(false)
                toggleVoiceSpectral(false)
                toggleMeshHoneypot(false)
                togglePhotogrammetry(false)
                toggleRfSpectrumSimulator(false)
                toggleOpticalAiOcr(false)
                
                // Mantener activo lo esencial para comunicación y radar
                toggleBinaryCompressor(true)
                togglePostQuantumKyber(true)
                togglePdrNavigation(true)
                toggleBarometerStorm(true)
                toggleShamirVault(true)
                toggleEmergencyZeroize(true)
                toggleGisMarkers(true)
                toggleMeshTopologyGraph(true)
                toggleTacticalHaptics(true)
            }
            LiteProfile.EXTREME_SURVIVAL -> {
                // Apagar todos los sensores periódicos, shaders y algoritmos pesados
                toggleThermalNightShader(false)
                toggleAcousticShotDetector(false)
                toggleUltrasonicLink(false)
                toggleVoiceSpectral(false)
                toggleMeshHoneypot(false)
                togglePhotogrammetry(false)
                toggleRfSpectrumSimulator(false)
                toggleOpticalAiOcr(false)
                togglePdrNavigation(false)
                toggleBarometerStorm(false)
                toggleSafeBubble3D(false)
                toggleBallisticsCalc(false)
                toggleSybilDetector(false)
                toggleMissionBlockchain(false)
                toggleRfSignatureMeter(false)
                toggleMavlinkUav(false)
                toggleSolarCompass(false)
                toggleSatelliteGateway(false)
                toggleAStarRouter(false)
                toggleMeshClockSync(false)
                toggleVisualLiFi(false)
                togglePhysioBioTelemetry(false)
                toggleSteganographyVault(false)
                toggleMorseBeaconEngine(false)
                toggleHorizonHudLevel(false)
                toggleMeshTopologyGraph(false)
                toggleTacticalHaptics(false)

                // Mantener estrictamente el núcleo de mensajería y cifrado
                toggleBinaryCompressor(true)
                togglePostQuantumKyber(true)
                toggleEmergencyZeroize(true)
            }
        }
    }

    /**
     * Catálogo exhaustivo de todos los módulos, componentes y archivos de código
     * que componen la arquitectura de OmniComm para el visor de la Versión Lite.
     */
    fun getAllModuleComponents(): List<TacticalModuleComponent> {
        return listOf(
            // Capa 1: UI & Visual
            TacticalModuleComponent(
                id = "mod_thermal_shader",
                name = "Shaders Térmicos OpenGL",
                layerCategory = "Capa 1: Presentación & UI",
                sourceFilePath = "app/.../ui/screens/CameraScreen.kt",
                description = "Filtro de amplificación de luz residual y gradiente térmico sobre cámara.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 18500,
                affectsBackgroundThreads = false,
                isEnabled = _enableThermalNightShader.value
            ),
            TacticalModuleComponent(
                id = "mod_horizon_hud",
                name = "Horizonte Artificial & HUD Canvas",
                layerCategory = "Capa 1: Presentación & UI",
                sourceFilePath = "app/.../ui/components/TacticalComponents.kt",
                description = "Renderizado de vector inercial en Canvas sobre la vista de radar.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 4200,
                affectsBackgroundThreads = false,
                isEnabled = _enableHorizonHudLevel.value
            ),
            TacticalModuleComponent(
                id = "mod_haptics",
                name = "Motor Háptico Táctico",
                layerCategory = "Capa 1: Presentación & UI",
                sourceFilePath = "app/.../domain/hardware/TacticalHapticsEngine.kt",
                description = "Patrones de vibración de confirmación silenciosa al pulsar botones.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 450,
                affectsBackgroundThreads = false,
                isEnabled = _enableTacticalHaptics.value
            ),

            // Capa 3: Dominio & Criptografía
            TacticalModuleComponent(
                id = "mod_kyber_pqc",
                name = "Criptografía Post-Cuántica ML-KEM Kyber",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/security/PostQuantumKyberVault.kt",
                description = "Encapsulado seguro de claves inmune a computación cuántica (NIST FIPS 203).",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 6400,
                affectsBackgroundThreads = false,
                isEnabled = _enablePostQuantumKyber.value
            ),
            TacticalModuleComponent(
                id = "mod_shamir_vault",
                name = "Bóveda Shamir Secret Sharing (K-de-N)",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/security/ShamirSecretSharingVault.kt",
                description = "Fragmentación matemática de contraseñas y llaves sobre el cuerpo GF(256).",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1200,
                affectsBackgroundThreads = false,
                isEnabled = _enableShamirVault.value
            ),
            TacticalModuleComponent(
                id = "mod_emergency_zeroize",
                name = "Protocolo ZEROIZE (DoD 5220.22-M)",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/security/EmergencyZeroizeManager.kt",
                description = "Autodestrucción y sobreescritura de 3 pasadas de memoria y bases de datos.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 800,
                affectsBackgroundThreads = false,
                isEnabled = _enableEmergencyZeroize.value
            ),
            TacticalModuleComponent(
                id = "mod_mission_blockchain",
                name = "Micro-Blockchain Proof-of-Authority",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/security/TacticalMissionBlockchain.kt",
                description = "Auditoría inmutable encadenada por SHA-256 de órdenes y reportes.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 5100,
                affectsBackgroundThreads = false,
                isEnabled = _enableMissionBlockchain.value
            ),
            TacticalModuleComponent(
                id = "mod_steganography",
                name = "Bóveda Esteganográfica LSB",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/security/SteganographyVault.kt",
                description = "Ocultación de mensajes y coordenadas dentro de los píxeles de fotos JPEG.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 9500,
                affectsBackgroundThreads = false,
                isEnabled = _enableSteganographyVault.value
            ),

            // Capa 4: Malla P2P & DTN
            TacticalModuleComponent(
                id = "mod_store_forward",
                name = "Enrutador DTN Store & Forward",
                layerCategory = "Capa 4: Red, Malla P2P & DTN",
                sourceFilePath = "app/.../domain/p2p/StoreAndForwardRouter.kt",
                description = "Retransmisión oportunista y almacenamiento temporal de paquetes en malla.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 4800,
                affectsBackgroundThreads = true,
                isEnabled = _enableStoreAndForward.value
            ),
            TacticalModuleComponent(
                id = "mod_binary_compressor",
                name = "Compresor Binario Huffman",
                layerCategory = "Capa 4: Red, Malla P2P & DTN",
                sourceFilePath = "app/.../domain/p2p/TacticalBinaryCompressor.kt",
                description = "Reducción de tamaño de datagramas UDP en un 40% para evadir intercepción.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1600,
                affectsBackgroundThreads = false,
                isEnabled = _enableBinaryCompressor.value
            ),
            TacticalModuleComponent(
                id = "mod_frequency_hopping",
                name = "Salto de Frecuencia Virtual FHSS",
                layerCategory = "Capa 4: Red, Malla P2P & DTN",
                sourceFilePath = "app/.../domain/p2p/FrequencyHoppingMeshEngine.kt",
                description = "Rotación pseudo-aleatoria de canales de comunicación para evadir inhibidores.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 3200,
                affectsBackgroundThreads = true,
                isEnabled = _enableFrequencyHopping.value
            ),
            TacticalModuleComponent(
                id = "mod_topology_graph",
                name = "Gestor del Grafo de Topología Malla",
                layerCategory = "Capa 4: Red, Malla P2P & DTN",
                sourceFilePath = "app/.../domain/p2p/MeshTopologyGraphManager.kt",
                description = "Cálculo en vivo de distancias euclidianas, RSSI y caminos óptimos de enlace.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 2400,
                affectsBackgroundThreads = false,
                isEnabled = _enableMeshTopologyGraph.value
            ),

            // Capa 5: Ciberseguridad & Sigilo
            TacticalModuleComponent(
                id = "mod_sybil_detector",
                name = "Detector Forense de Ataques Sybil",
                layerCategory = "Capa 5: Ciberseguridad & Sigilo",
                sourceFilePath = "app/.../domain/security/SybilMeshDetector.kt",
                description = "Inspección de timestamps y anomalías de huella MAC para bloquear clones.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 3900,
                affectsBackgroundThreads = true,
                isEnabled = _enableSybilDetector.value
            ),
            TacticalModuleComponent(
                id = "mod_rf_signature",
                name = "Medidor de Huella RF (LPI/LPD)",
                layerCategory = "Capa 5: Ciberseguridad & Sigilo",
                sourceFilePath = "app/.../domain/security/RfEmissionSignatureMeter.kt",
                description = "Cálculo del riesgo de intercepción enemiga según la potencia de transmisión.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1100,
                affectsBackgroundThreads = false,
                isEnabled = _enableRfSignatureMeter.value
            ),
            TacticalModuleComponent(
                id = "mod_mesh_honeypot",
                name = "Inyector de Nodos Fantasma (Honeypot)",
                layerCategory = "Capa 5: Ciberseguridad & Sigilo",
                sourceFilePath = "app/.../domain/security/MeshHoneypotGenerator.kt",
                description = "Generación de telemetría sintética para despistar a radiogoniómetros hostiles.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 5600,
                affectsBackgroundThreads = true,
                isEnabled = _enableMeshHoneypot.value
            ),

            // Capa 7: Hardware & Sensores HAL
            TacticalModuleComponent(
                id = "mod_pdr_nav",
                name = "Navegación Inercial sin GPS (PDR 100Hz)",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/sensors/PedestrianDeadReckoningEngine.kt",
                description = "Muestreo continuo de acelerómetro y giróscopo para navegación sin satélites.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 12400,
                affectsBackgroundThreads = true,
                isEnabled = _enablePdrNavigation.value
            ),
            TacticalModuleComponent(
                id = "mod_acoustic_shot",
                name = "Clasificador Acústico de Disparos",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/media/AcousticShotClassifier.kt",
                description = "Análisis continuo del buffer de micrófono para detectar detonaciones.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 15800,
                affectsBackgroundThreads = true,
                isEnabled = _enableAcousticShotDetector.value
            ),
            TacticalModuleComponent(
                id = "mod_ultrasonic_link",
                name = "Transceptor Ultrasonido (18-20 kHz)",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/audio/UltrasonicDataLinkTransceiver.kt",
                description = "Modulación acústica inaudible Data-Over-Sound mediante AudioTrack.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 14200,
                affectsBackgroundThreads = true,
                isEnabled = _enableUltrasonicLink.value
            ),
            TacticalModuleComponent(
                id = "mod_voice_spectral",
                name = "Enmascarador Espectral de Voz (FFT)",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/c4isr/TacticalVoiceSpectralEngine.kt",
                description = "Inversión espectral a 3.3 kHz y análisis FFT de 512 puntos en tiempo real.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 16700,
                affectsBackgroundThreads = true,
                isEnabled = _enableVoiceSpectral.value
            ),
            TacticalModuleComponent(
                id = "mod_barometer_storm",
                name = "Barómetro & Alerta de Tormentas",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/sensors/BarometerStormAlertEngine.kt",
                description = "Monitoreo continuo de presión hPa y cálculo de altitud barométrica.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 2100,
                affectsBackgroundThreads = true,
                isEnabled = _enableBarometerStorm.value
            ),
            TacticalModuleComponent(
                id = "mod_visual_lifi",
                name = "Transmisor Óptico Li-Fi (Flash LED)",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/media/VisualOpticalLiFiTransceiver.kt",
                description = "Modulación óptica de alta velocidad mediante pulsos del flash Camera2.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 6800,
                affectsBackgroundThreads = true,
                isEnabled = _enableVisualLiFi.value
            ),
            TacticalModuleComponent(
                id = "mod_man_down",
                name = "Detector de Hombre Caído (Man-Down)",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/sensors/TacticalManDownDetector.kt",
                description = "Vigilancia de inmovilidad y caída libre con disparo de baliza SOS.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 2300,
                affectsBackgroundThreads = true,
                isEnabled = _enableManDownAlert.value
            ),

            // Capa 8: Control Externo & Mando C2
            TacticalModuleComponent(
                id = "mod_c4isr_console",
                name = "Consola Maestra C4ISR & API REST 9090",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/remote/TacticalExternalControlServer.kt",
                description = "Servidor web HTTP embebido y endpoints REST para control por PC/USB.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 19200,
                affectsBackgroundThreads = true,
                isEnabled = _enableC4IsrConsole.value
            ),
            TacticalModuleComponent(
                id = "mod_astar_router",
                name = "Enrutador Táctico de Escape A*",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/c4isr/TacticalAStarRouter.kt",
                description = "Cálculo de rutas de evasión evitando campos minados y zonas hostiles.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 7800,
                affectsBackgroundThreads = false,
                isEnabled = _enableAStarRouter.value
            ),
            TacticalModuleComponent(
                id = "mod_ballistics",
                name = "Calculadora Balística Táctica",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/sensors/TacticalBallisticsCalculator.kt",
                description = "Solución balística para tiradores con corrección de viento y ángulo.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1900,
                affectsBackgroundThreads = false,
                isEnabled = _enableBallisticsCalc.value
            ),
            TacticalModuleComponent(
                id = "mod_salute_intel",
                name = "Generador de Informes SALUTE",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/c4isr/SaluteIntelReportEngine.kt",
                description = "Formularios de inteligencia militar normalizados listos para transmisión.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1400,
                affectsBackgroundThreads = false,
                isEnabled = _enableSaluteIntel.value
            ),
            TacticalModuleComponent(
                id = "mod_solar_compass",
                name = "Brújula Solar y Efemérides",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/sensors/SolarEphemerisCompass.kt",
                description = "Determinación del Norte Verdadero sin brújula magnética ni satélites GPS.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1300,
                affectsBackgroundThreads = false,
                isEnabled = _enableSolarCompass.value
            ),
            TacticalModuleComponent(
                id = "mod_adaptive_power",
                name = "Gestor de Energía Adaptativo",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/hardware/AdaptivePowerProfileManager.kt",
                description = "Monitoreo de batería y transición automática a perfiles de bajo consumo.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1500,
                affectsBackgroundThreads = true,
                isEnabled = _enableAdaptivePower.value
            ),
            TacticalModuleComponent(
                id = "mod_duress_sec",
                name = "Seguridad de Coacción (PIN Bajo Presión)",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/security/DuressAndSecurityManager.kt",
                description = "Desbloqueo silencioso de interfaz señuelo al ingresar PIN de coacción.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1100,
                affectsBackgroundThreads = false,
                isEnabled = _enableDuressSecurity.value
            ),
            TacticalModuleComponent(
                id = "mod_morse_beacon",
                name = "Sintetizador Baliza Código Morse",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/audio/MorseBeaconSynthesizer.kt",
                description = "Transmisión auditiva de balizas SOS de emergencia en modulación Morse.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1800,
                affectsBackgroundThreads = false,
                isEnabled = _enableMorseBeaconEngine.value
            ),
            TacticalModuleComponent(
                id = "mod_rf_spectrum",
                name = "Analizador de Espectro RF y Señales",
                layerCategory = "Capa 5: Ciberseguridad & Sigilo",
                sourceFilePath = "app/.../domain/hardware/RfSpectrumAnalyzer.kt",
                description = "Monitoreo del espectro electromagnético y detección de portadoras sospechosas.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 6200,
                affectsBackgroundThreads = true,
                isEnabled = _enableRfSpectrumSimulator.value
            ),
            TacticalModuleComponent(
                id = "mod_sitrep_reporter",
                name = "Generador de Informes SITREP Militar",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/models/SitrepReportManager.kt",
                description = "Plantillas tácticas estandarizadas de situación militar y reporte de bajas.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1200,
                affectsBackgroundThreads = false,
                isEnabled = _enableSitrepReporter.value
            ),
            TacticalModuleComponent(
                id = "mod_dead_mans_switch",
                name = "Interruptor de Hombre Muerto (Watchdog)",
                layerCategory = "Capa 3: Criptografía & Dominio",
                sourceFilePath = "app/.../domain/hardware/DeadMansSwitchWatchdog.kt",
                description = "Temporizador de confirmación con purga automática de datos si el operador es capturado.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 950,
                affectsBackgroundThreads = true,
                isEnabled = _enableDeadMansSwitch.value
            ),
            TacticalModuleComponent(
                id = "mod_physio_bio",
                name = "Telemetría Biológica del Operador",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/sensors/PhysioBioTelemetryManager.kt",
                description = "Procesamiento de signos vitales (SpO2, FC, índice de fatiga en combate).",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 5400,
                affectsBackgroundThreads = true,
                isEnabled = _enablePhysioBioTelemetry.value
            ),
            TacticalModuleComponent(
                id = "mod_optical_ai_ocr",
                name = "Analizador de Evidencia & Reconocimiento OCR",
                layerCategory = "Capa 2: Inteligencia Artificial Táctica",
                sourceFilePath = "app/.../domain/ai/TacticalEvidenceAnalyzer.kt",
                description = "Extracción instantánea de texto y matrículas en fotos con modelo en el dispositivo.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 22000,
                affectsBackgroundThreads = false,
                isEnabled = _enableOpticalAiOcr.value
            ),
            TacticalModuleComponent(
                id = "mod_gis_markers",
                name = "Marcadores Vectoriales Tácticos GIS",
                layerCategory = "Capa 1: Presentación & UI",
                sourceFilePath = "app/.../domain/p2p/TacticalGISManager.kt",
                description = "Capas de mapas offline, waypoints y zonas de peligro compartidas en malla.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 8200,
                affectsBackgroundThreads = false,
                isEnabled = _enableGisMarkers.value
            ),
            TacticalModuleComponent(
                id = "mod_nato_symbology",
                name = "Simbología Militar NATO APP-6D",
                layerCategory = "Capa 1: Presentación & UI",
                sourceFilePath = "app/.../domain/models/NatoSymbologyOverlay.kt",
                description = "Renderizado de iconos estándar OTAN (infantería, blindados, puntos de reunión).",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 2800,
                affectsBackgroundThreads = false,
                isEnabled = _enableNatoSymbology.value
            ),
            TacticalModuleComponent(
                id = "mod_tactical_agent",
                name = "Agente IA Táctico de Misión",
                layerCategory = "Capa 2: Inteligencia Artificial Táctica",
                sourceFilePath = "app/.../domain/ai/TacticalMissionAgent.kt",
                description = "Asesor autónomo táctico para análisis de amenazas y planificación de contingencias.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 24500,
                affectsBackgroundThreads = false,
                isEnabled = _enableTacticalMissionAgent.value
            ),
            TacticalModuleComponent(
                id = "mod_safe_bubble_3d",
                name = "Radar de Proximidad Burbuja 3D",
                layerCategory = "Capa 7: Sensores & Hardware HAL",
                sourceFilePath = "app/.../domain/sensors/SafeBubble3DProximityRadar.kt",
                description = "Alerta de aproximación perimétrica omnidireccional basada en RSSI tridimensional.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 4900,
                affectsBackgroundThreads = true,
                isEnabled = _enableSafeBubble3D.value
            ),
            TacticalModuleComponent(
                id = "mod_emcon_alpha",
                name = "Gestor de Silencio de Radio EMCON Alpha",
                layerCategory = "Capa 5: Ciberseguridad & Sigilo",
                sourceFilePath = "app/.../domain/security/EmconAlphaManager.kt",
                description = "Bloqueo estricto a nivel de kernel de transmisiones no esenciales de radiofrecuencia.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1400,
                affectsBackgroundThreads = false,
                isEnabled = _enableEmconAlpha.value
            ),
            TacticalModuleComponent(
                id = "mod_photogrammetry",
                name = "Motor de Fotogrametría y Modelado 3D",
                layerCategory = "Capa 2: Inteligencia Artificial Táctica",
                sourceFilePath = "app/.../domain/sensors/TacticalPhotogrammetryEngine.kt",
                description = "Reconstrucción tridimensional de terrenos a partir de secuencias fotográficas.",
                resourceWeight = ResourceWeight.HEAVY,
                estimatedRamKb = 28000,
                affectsBackgroundThreads = false,
                isEnabled = _enablePhotogrammetry.value
            ),
            TacticalModuleComponent(
                id = "mod_mavlink_uav",
                name = "Transceptor de Telemetría UAV MAVLink",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/sensors/MavlinkTelemetryTransceiver.kt",
                description = "Intercambio de telemetría y órdenes de vuelo con drones mediante protocolo MAVLink.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 6700,
                affectsBackgroundThreads = true,
                isEnabled = _enableMavlinkUav.value
            ),
            TacticalModuleComponent(
                id = "mod_9line_medevac",
                name = "Generador 9-Line MEDEVAC Táctico",
                layerCategory = "Capa 8: Control Externo & Mando",
                sourceFilePath = "app/.../domain/sensors/TacticalMedevacEngine.kt",
                description = "Protocolo militar estandarizado de evacuación médica de heridos en combate.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1600,
                affectsBackgroundThreads = false,
                isEnabled = _enable9LineMedevac.value
            ),
            TacticalModuleComponent(
                id = "mod_satellite_gateway",
                name = "Pasarela Satelital Iridium / Inmarsat",
                layerCategory = "Capa 4: Red, Malla P2P & DTN",
                sourceFilePath = "app/.../domain/sensors/SatelliteMeshGateway.kt",
                description = "Enlace satelital de emergencia SBD cuando la malla terrestre queda aislada.",
                resourceWeight = ResourceWeight.MEDIUM,
                estimatedRamKb = 4500,
                affectsBackgroundThreads = true,
                isEnabled = _enableSatelliteGateway.value
            ),
            TacticalModuleComponent(
                id = "mod_mesh_clock_sync",
                name = "Sincronizador de Reloj Mesh PTP",
                layerCategory = "Capa 4: Red, Malla P2P & DTN",
                sourceFilePath = "app/.../domain/c4isr/MeshTimeSynchronizer.kt",
                description = "Sincronización horaria de precisión milisegundo por algoritmo Berkeley en malla.",
                resourceWeight = ResourceWeight.LIGHT,
                estimatedRamKb = 1800,
                affectsBackgroundThreads = true,
                isEnabled = _enableMeshClockSync.value
            )
        )
    }

    /**
     * Alterna cualquier módulo por su ID único
     */
    fun toggleModuleById(moduleId: String, enabled: Boolean) {
        when (moduleId) {
            "mod_thermal_shader" -> toggleThermalNightShader(enabled)
            "mod_horizon_hud" -> toggleHorizonHudLevel(enabled)
            "mod_haptics" -> toggleTacticalHaptics(enabled)
            "mod_kyber_pqc" -> togglePostQuantumKyber(enabled)
            "mod_shamir_vault" -> toggleShamirVault(enabled)
            "mod_emergency_zeroize" -> toggleEmergencyZeroize(enabled)
            "mod_mission_blockchain" -> toggleMissionBlockchain(enabled)
            "mod_steganography" -> toggleSteganographyVault(enabled)
            "mod_store_forward" -> toggleStoreAndForward(enabled)
            "mod_binary_compressor" -> toggleBinaryCompressor(enabled)
            "mod_frequency_hopping" -> toggleFrequencyHopping(enabled)
            "mod_topology_graph" -> toggleMeshTopologyGraph(enabled)
            "mod_sybil_detector" -> toggleSybilDetector(enabled)
            "mod_rf_signature" -> toggleRfSignatureMeter(enabled)
            "mod_mesh_honeypot" -> toggleMeshHoneypot(enabled)
            "mod_pdr_nav" -> togglePdrNavigation(enabled)
            "mod_acoustic_shot" -> toggleAcousticShotDetector(enabled)
            "mod_ultrasonic_link" -> toggleUltrasonicLink(enabled)
            "mod_voice_spectral" -> toggleVoiceSpectral(enabled)
            "mod_barometer_storm" -> toggleBarometerStorm(enabled)
            "mod_visual_lifi" -> toggleVisualLiFi(enabled)
            "mod_man_down" -> toggleManDownAlert(enabled)
            "mod_c4isr_console" -> toggleC4IsrConsole(enabled)
            "mod_astar_router" -> toggleAStarRouter(enabled)
            "mod_ballistics" -> toggleBallisticsCalc(enabled)
            "mod_salute_intel" -> toggleSaluteIntel(enabled)
            "mod_solar_compass" -> toggleSolarCompass(enabled)
            "mod_adaptive_power" -> toggleAdaptivePower(enabled)
            "mod_duress_sec" -> toggleDuressSecurity(enabled)
            "mod_morse_beacon" -> toggleMorseBeaconEngine(enabled)
            "mod_rf_spectrum" -> toggleRfSpectrumSimulator(enabled)
            "mod_sitrep_reporter" -> toggleSitrepReporter(enabled)
            "mod_dead_mans_switch" -> toggleDeadMansSwitch(enabled)
            "mod_physio_bio" -> togglePhysioBioTelemetry(enabled)
            "mod_optical_ai_ocr" -> toggleOpticalAiOcr(enabled)
            "mod_gis_markers" -> toggleGisMarkers(enabled)
            "mod_nato_symbology" -> toggleNatoSymbology(enabled)
            "mod_tactical_agent" -> toggleTacticalMissionAgent(enabled)
            "mod_safe_bubble_3d" -> toggleSafeBubble3D(enabled)
            "mod_emcon_alpha" -> toggleEmconAlpha(enabled)
            "mod_photogrammetry" -> togglePhotogrammetry(enabled)
            "mod_mavlink_uav" -> toggleMavlinkUav(enabled)
            "mod_9line_medevac" -> toggle9LineMedevac(enabled)
            "mod_satellite_gateway" -> toggleSatelliteGateway(enabled)
            "mod_mesh_clock_sync" -> toggleMeshClockSync(enabled)
        }
    }

    /**
     * Retorna una tupla con (Módulos activos, RAM estimada en MB, Ahorro de batería estimado en %)
     */
    fun getEstimatedMetrics(): Triple<Int, Int, Int> {
        val modules = getAllModuleComponents()
        val activeModules = modules.filter { it.isEnabled }
        val activeCount = activeModules.size
        val totalCount = modules.size

        // Base mínima del runtime Android / Compose: 24 MB
        val totalRamKb = 24000 + activeModules.sumOf { it.estimatedRamKb }
        val ramMb = totalRamKb / 1024

        // Ahorro de batería estimado comparado contra el modo full
        val inactiveHeavyOrThreads = modules.count { !it.isEnabled && (it.resourceWeight == ResourceWeight.HEAVY || it.affectsBackgroundThreads) }
        val totalHeavyOrThreads = modules.count { it.resourceWeight == ResourceWeight.HEAVY || it.affectsBackgroundThreads }
        val savingsPercent = if (totalHeavyOrThreads > 0) {
            ((inactiveHeavyOrThreads.toFloat() / totalHeavyOrThreads.toFloat()) * 78f).toInt()
        } else 0

        return Triple(activeCount, ramMb, savingsPercent.coerceIn(0, 85))
    }

    private fun logConfigChange(moduleName: String, newState: String) {
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "CONFIG_CHANGE",
            message = "Módulo [$moduleName] actualizado a: $newState"
        )
    }
}
