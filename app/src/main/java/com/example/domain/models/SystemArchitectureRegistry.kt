package com.example.domain.models

/**
 * Registro Maestro de Arquitectura, Ingeniería Inversa y Mapa de Interconexiones de OmniComm.
 * Proporciona el blueprint completo del sistema: capas, módulos, flujos de datos, conexiones, capacidades y APIs de control externo.
 */

data class SystemLayer(
    val layerNumber: Int,
    val name: String,
    val tag: String,
    val description: String,
    val primaryResponsibility: String,
    val components: List<LayerComponent>,
    val inboundFlows: List<String>,
    val outboundFlows: List<String>
)

data class LayerComponent(
    val name: String,
    val filePath: String,
    val role: String,
    val keyFunctions: List<String>,
    val protocolsOrTech: String
)

data class SystemDataFlow(
    val id: String,
    val title: String,
    val trigger: String,
    val description: String,
    val stepSequence: List<DataFlowStep>,
    val securityLevel: String,
    val offlineCapable: Boolean
)

data class DataFlowStep(
    val stepNumber: Int,
    val moduleName: String,
    val filePath: String,
    val action: String
)

data class CodeModuleRecord(
    val packageGroup: String,
    val fileName: String,
    val relativePath: String,
    val primaryClass: String,
    val description: String,
    val keyMethods: List<String>,
    val dependencies: List<String>,
    val consumers: List<String>,
    val tacticalDomain: String
)

data class TacticalCapability(
    val id: String,
    val title: String,
    val category: String,
    val operationalScope: String,
    val activePhase: Int,
    val primaryModule: String,
    val filePath: String,
    val isRealTime: Boolean,
    val isZeroTrust: Boolean,
    val isHardwareAccelerated: Boolean
)

data class SystemMasterArchitectureRecord(
    val phaseNumber: Int,
    val blockNumber: Int,
    val title: String,
    val operationalScope: String,
    val category: String,
    val status: String = "100% COMPLETADO",
    val progressPercent: Int = 100,
    val architectureLayers: List<String>,
    val domainModule: String,
    val sourceFiles: List<String>,
    val exposedApis: List<String>,
    val implementationModes: String,
    val researchAndStandards: String,
    val hardwareAndSensors: List<String>,
    val zeroTrustAndFaultTolerance: String,
    val futureProjectionsAndRoadmap: String,
    val verificationMethods: String
)

object SystemArchitectureRegistry {

    val layers: List<SystemLayer> = listOf(
        SystemLayer(
            layerNumber = 1,
            name = "Capa 1: Presentación & UI/UX Táctica",
            tag = "UI_PRESENTATION",
            description = "Interfaces de usuario declarativas en Jetpack Compose siguiendo Material Design 3 táctico, layouts adaptativos (Rail, BottomBar, Floating Dock), modales C4ISR y HUD inercial.",
            primaryResponsibility = "Renderizado de interfaces reactivas, recolección de entradas del operador, HUD en tiempo real y señalización táctica.",
            components = listOf(
                LayerComponent("MainActivity & HubScreen", "com/example/MainActivity.kt", "Punto de entrada principal, ruteo Navigation Compose y Hub de 10 accesos directos tácticos.", listOf("setContent", "HubScreen", "NavHost"), "Compose M3, Navigation Compose"),
                LayerComponent("ConfigurationScreen & Tactical Panels", "com/example/ui/screens/ConfigurationScreen.kt", "Panel de configuración para los 31 subsistemas tácticos y diálogos de control.", listOf("ConfigurationScreen", "TacticalToolActionCard", "SafeBubble3DDialog"), "Compose StateFlow collection"),
                LayerComponent("Roadmap & Architecture Blueprint Screen", "com/example/ui/screens/RoadmapScreen.kt", "Hoja de ruta integral, ingeniería inversa del sistema y planos arquitectónicos con tabla expandida.", listOf("RoadmapScreen", "ArchitectureBlueprintTab", "CodeIndexTab", "MasterTableTab"), "Compose TabRow, Search filter, Data Table"),
                LayerComponent("Radar & Tactical GIS Map Screen", "com/example/ui/screens/RadarScreen.kt", "Visualización de topología de nodos, brújula azimutal y simbología OTAN APP-6.", listOf("RadarScreen", "NatoMarkerOverlay", "MeshNodeCanvas"), "Compose Canvas, Sensor Fusion"),
                LayerComponent("Team Chat & Offline Messages", "com/example/ui/screens/ChatScreen.kt", "Mensajería E2EE cifrada con soporte de cola offline y adjuntos tácticos.", listOf("ChatScreen", "MessageBubble", "AttachmentPicker"), "Compose LazyColumn, MVI"),
                LayerComponent("Tactical Camera & Thermal Night Vision", "com/example/ui/screens/CameraScreen.kt", "Visor de cámara táctica con filtro térmico/nocturno por shaders y OCR militar.", listOf("CameraScreen", "ThermalShaderPreview", "CaptureEvidence"), "CameraX, RenderScript/GL shaders"),
                LayerComponent("Overlays de Seguridad (Biometría & Man-Down)", "com/example/ui/components/BiometricLockOverlay.kt", "Capas de bloqueo biométrico y detección de hombre caído / caída libre.", listOf("BiometricLockOverlay", "EmergencyManDownOverlay"), "AndroidX BiometricPrompt")
            ),
            inboundFlows = listOf("StateFlow de ViewModels", "Eventos de sensores de hardware", "Push notifications de malla", "Telemetría de API Server"),
            outboundFlows = listOf("Intents a ViewModels", "Navegación de rutas Compose", "Configuración de FeatureManager")
        ),
        SystemLayer(
            layerNumber = 2,
            name = "Capa 2: Control & ViewModels (MVI/MVVM)",
            tag = "VIEWMODELS_STATE",
            description = "Gestión de estado reactivo mediante StateFlow y SharedFlow de Kotlin Coroutines, desacoplando la UI de la lógica de dominio.",
            primaryResponsibility = "Exposición de estados de UI inmutables, procesamiento de eventos de usuario y coordinación con casos de uso de dominio.",
            components = listOf(
                LayerComponent("ChatViewModel", "com/example/ui/viewmodels/ChatViewModel.kt", "Control de sesiones de chat, cifrado E2EE y cola de envío offline.", listOf("sendMessage", "selectSession", "loadHistoricalMessages"), "StateFlow, Coroutines"),
                LayerComponent("RadarViewModel", "com/example/ui/viewmodels/RadarViewModel.kt", "Coordinación de posiciones GPS/PDR, distancias de nodos y alertas de proximidad.", listOf("updateUserLocation", "trackActivePeers", "calculateRelativeDistances"), "Flow combine, Location API"),
                LayerComponent("AuthViewModel", "com/example/ui/viewmodels/AuthViewModel.kt", "Gestión de perfil táctico del operador, autenticación Firebase y llaves locales.", listOf("signInWithGoogle", "continueOffline", "signOut"), "FirebaseAuth, CredentialManager"),
                LayerComponent("ContextualSensorViewModel", "com/example/ui/viewmodels/ContextualSensorViewModel.kt", "Fusión de telemetría de luz ambiente, proximidad, movimiento y barómetro.", listOf("startSensorSampling", "ambientLight", "isProximityNear"), "SensorEventListener, StateFlow"),
                LayerComponent("CloudViewModel & PresenceManager", "com/example/ui/viewmodels/CloudViewModel.kt", "Sincronización remota en segundo plano y presencia en tiempo real.", listOf("syncRemoteContacts", "uploadEvidence", "trackOnlineStatus"), "Firestore, RTDB Presence")
            ),
            inboundFlows = listOf("Eventos de UI de usuario", "Flujos reactivos de Repositorios y Motores de Dominio"),
            outboundFlows = listOf("Mutaciones en Motores de Dominio", "StateFlow emitido a Composables")
        ),
        SystemLayer(
            layerNumber = 3,
            name = "Capa 3: Dominio Táctico, Criptografía & Motores DSP",
            tag = "TACTICAL_DOMAIN_DSP",
            description = "El núcleo algorítmico y matemático de la aplicación con los 31 motores tácticos independientes y modulares.",
            primaryResponsibility = "Cómputo criptográfico, procesamiento de señales de audio, algoritmos de navegación inercial y cálculo de misiones.",
            components = listOf(
                LayerComponent("PostQuantumKyberVault", "domain/security/PostQuantumKyberVault.kt", "Criptografía post-cuántica ML-KEM Kyber-768 híbrida con X25519.", listOf("generateKyberKeyPair", "encapsulate", "decapsulate"), "NIST PQC ML-KEM Standard"),
                LayerComponent("ShamirSecretSharingVault", "domain/security/ShamirSecretSharingVault.kt", "División de secretos polinomial sobre Galois Field GF(256).", listOf("splitSecret", "reconstructSecret"), "Lagrange Interpolation GF(2^8)"),
                LayerComponent("PedestrianDeadReckoningEngine", "domain/sensors/PedestrianDeadReckoningEngine.kt", "Navegación inercial por pasos y orientación sin GPS.", listOf("onSensorChanged", "detectStep", "updateCoordinates"), "Weinberg Stride Algorithm"),
                LayerComponent("TacticalAStarRouter", "domain/c4isr/TacticalAStarRouter.kt", "Enrutador heurístico de escape con cuadrícula de coste ponderada.", listOf("calculateEscapeRoute", "addDangerZone"), "A* Graph Search"),
                LayerComponent("TacticalVoiceSpectralEngine", "domain/c4isr/TacticalVoiceSpectralEngine.kt", "Inversión espectral de audio (3.3 kHz) y cálculo FFT en vivo.", listOf("invertSpectrum", "computeFft"), "Fast Fourier Transform"),
                LayerComponent("AcousticShotClassifier", "domain/media/AcousticShotClassifier.kt", "Clasificador de transitorios acústicos de disparos y calibres.", listOf("processAudioBuffer", "detectMuzzleBlast"), "Audio DSP Spectral Peak"),
                LayerComponent("TacticalMissionBlockchain", "domain/security/TacticalMissionBlockchain.kt", "Micro-Blockchain Proof-of-Authority inmutable SHA-256.", listOf("mineTacticalBlock", "verifyChainIntegrity"), "SHA-256 Merkle Ledger")
            ),
            inboundFlows = listOf("Llamadas de casos de uso desde ViewModels", "Peticiones de API Server Remoto"),
            outboundFlows = listOf("Modelos inmutables de dominio", "Eventos de seguridad y telemetría a Capa 4 y Capa 6")
        ),
        SystemLayer(
            layerNumber = 4,
            name = "Capa 4: Red, Malla P2P & Descubrimiento Autónomo",
            tag = "NETWORKING_MESH_P2P",
            description = "Pila de comunicación inalámbrica autónoma, mallas híbridas multicanal, DTN Store-and-Forward y salto de frecuencias.",
            primaryResponsibility = "Descubrimiento de pares sin servidor, enrutamiento multi-salto, empaquetado binario y transporte de datagramas.",
            components = listOf(
                LayerComponent("AutonomousMeshDiscoveryEngine", "domain/discovery/AutonomousMeshDiscoveryEngine.kt", "Orquestador de descubrimiento híbrido en segundo plano.", listOf("startAutoDiscovery", "publishPresence"), "Background Coroutines"),
                LayerComponent("LanMulticastDiscoveryBeacon", "domain/p2p/LanMulticastDiscoveryBeacon.kt", "Transmisor y receptor de balizas UDP Multicast (224.0.0.251 / 239.255.43.99).", listOf("startListening", "broadcastBeaconPacket"), "UDP Sockets"),
                LayerComponent("FrequencyHoppingMeshEngine", "domain/p2p/FrequencyHoppingMeshEngine.kt", "Salto pseudo-aleatorio de canales virtuales FHSS.", listOf("generateHoppingPattern", "advanceNextHoppingSlot"), "CSPRNG Seed Sync"),
                LayerComponent("StoreAndForwardRouter", "domain/p2p/StoreAndForwardRouter.kt", "Enrutador DTN con almacenamiento local y entrega oportuna.", listOf("routeBundle", "flushOnContact"), "DTN Bundle Protocol"),
                LayerComponent("TacticalBinaryCompressor", "domain/p2p/TacticalBinaryCompressor.kt", "Compresión binaria de mensajes Huffman / CBOR ultradensa.", listOf("compressData", "decompressData"), "Huffman Coding / Bitshift")
            ),
            inboundFlows = listOf("Datagramas de red LAN / Wi-Fi", "Señales Bluetooth LE", "Paquetes DTN de satélite"),
            outboundFlows = listOf("Emisión de paquetes de radiofrecuencia", "Eventos de nuevos pares descubiertos")
        ),
        SystemLayer(
            layerNumber = 5,
            name = "Capa 5: Seguridad Zero-Trust & Bóveda Criptográfica",
            tag = "SECURITY_ZERO_TRUST",
            description = "Bóvedas de seguridad con enclave de hardware, gestión de políticas EMCON, detección de ataques Sybil y protocolo ZEROIZE.",
            primaryResponsibility = "Aislamiento criptográfico, firma de mensajes, detección de interferencias hostiles y autodestrucción segura.",
            components = listOf(
                LayerComponent("CryptoManager", "domain/security/CryptoManager.kt", "Cifrado simétrico AES-256 GCM autenticado y Android KeyStore.", listOf("encryptGcm", "decryptGcm", "getOrCreateMasterKey"), "Android KeyStore HW"),
                LayerComponent("EmergencyZeroizeManager", "domain/security/EmergencyZeroizeManager.kt", "Protocolo de borrado destructivo de 3 pasadas DoD 5220.22-M.", listOf("executeZeroizeWipe", "overwriteSecurely"), "DoD 5220.22-M"),
                LayerComponent("EmconAlphaManager", "domain/security/EmconAlphaManager.kt", "Control de niveles de silencio de radio por software.", listOf("setEmconLevelByName", "isRfSuppressed"), "Policy Enforcement Engine"),
                LayerComponent("SybilMeshDetector", "domain/security/SybilMeshDetector.kt", "Detección forense de identidades duplicadas y spoofing.", listOf("scanAndAuditMeshSignatures", "isolateHostileNode"), "Statistical RSSI Audit"),
                LayerComponent("MeshHoneypotGenerator", "domain/security/MeshHoneypotGenerator.kt", "Generación de nodos fantasma y tráfico señuelo.", listOf("generateDecoyNodes", "broadcastFakeTelemetry"), "Decoy Injection")
            ),
            inboundFlows = listOf("Eventos de amenaza perimetral", "Comandos de autodestrucción", "Tráfico entrante para auditoría"),
            outboundFlows = listOf("Claves descifradas en memoria volátil", "Instrucciones de purga o bloqueo de TX")
        ),
        SystemLayer(
            layerNumber = 6,
            name = "Capa 6: Persistencia Local & Nube Híbrida",
            tag = "PERSISTENCE_LOCAL_CLOUD",
            description = "Almacenamiento persistente local mediante Room Database (SQLite) y sincronización con Firestore / Cloud Storage.",
            primaryResponsibility = "Almacenamiento transaccional indexado de mensajes, contactos, registros forenses y cachés offline.",
            components = listOf(
                LayerComponent("OmniDatabase", "data/local/OmniDatabase.kt", "Base de datos Room SQLite con esquemas de mensajes, sesiones y logs.", listOf("messageDao", "chatSessionDao", "contactDao"), "AndroidX Room SQLite"),
                LayerComponent("DiscoveryLogCollector", "domain/logging/DiscoveryLogCollector.kt", "Buffer circular en memoria de logs forenses de auto-descubrimiento.", listOf("addLog", "getRecentLogsSnapshot"), "Thread-safe Circular Buffer"),
                LayerComponent("FeatureManager", "domain/config/FeatureManager.kt", "Preferencias tácticas, llaves de entorno y estilos de navegación.", listOf("saveNavigationLayout", "getEmconLevel"), "SharedPreferences Encrypted")
            ),
            inboundFlows = listOf("Entidades de dominio para guardar", "Registros de logs y telemetría"),
            outboundFlows = listOf("Consultas Flow / DAO a ViewModels", "Restauración de sesión")
        ),
        SystemLayer(
            layerNumber = 7,
            name = "Capa 7: Hardware, Sensores IMU & Audio DSP (HAL)",
            tag = "HARDWARE_HAL_SENSORS",
            description = "Capa de abstracción de hardware que interactúa con los sensores físicos, cámaras, transceptores de audio y linterna LED.",
            primaryResponsibility = "Muestreo a alta frecuencia (50-100Hz) de sensores físicos, control de actuadores lumínicos y vibración.",
            components = listOf(
                LayerComponent("SensorManager Driver", "domain/sensors/PedestrianDeadReckoningEngine.kt", "Acelerómetro, Giróscopo, Magnetómetro y Barómetro.", listOf("registerListener", "onSensorChanged"), "Android Sensor HAL"),
                LayerComponent("CameraManager Torch Controller", "domain/media/VisualOpticalLiFiTransceiver.kt", "Control de pulsos ópticos de flash para Li-Fi y Morse táctico.", listOf("setTorchMode", "transmitOpticalMessage"), "Camera2 HAL"),
                LayerComponent("AudioTrack / AudioRecord HAL", "domain/c4isr/TacticalVoiceSpectralEngine.kt", "Muestreo PCM 16-bit 44.1kHz para DSP, ultrasonidos y voz.", listOf("write", "read", "startRecording"), "OpenSL ES / Audio HAL"),
                LayerComponent("VibratorManager Controller", "domain/remote/TacticalExternalControlServer.kt", "Generador de patrones hápticos de alerta y SOS.", listOf("vibrate", "createWaveform"), "Vibrator HAL API 31+")
            ),
            inboundFlows = listOf("Interrupciones de hardware físico", "Muestras analógicas convertidas a digital"),
            outboundFlows = listOf("Flujos de muestras sin procesar a la Capa 3 de Dominio DSP")
        ),
        SystemLayer(
            layerNumber = 8,
            name = "Capa 8: Control Externo, Pasarela API REST & Estación de Mando Remota",
            tag = "REMOTE_API_STATION",
            description = "Servidor embebido HTTP/1.1 REST & Consola Web que expone el 100% de los 31 subsistemas tácticos y hardware para control total desde computadoras externas vía Wi-Fi LAN o cable USB (ADB port forwarding).",
            primaryResponsibility = "Exposición de endpoints RESTful, consola web interactiva en tiempo real, interfaz de línea de comandos (CLI API) y control bidireccional desde estaciones de mando externas.",
            components = listOf(
                LayerComponent("TacticalExternalControlServer", "domain/remote/TacticalExternalControlServer.kt", "Servidor socket HTTP multi-hilo, parser REST, generador de Consola Web HTML5/JS y despachador de acciones.", listOf("startServer", "stopServer", "routeHttpRequest", "executeTacticalCliCommand"), "Java ServerSocket, RFC 7231, Coroutines"),
                LayerComponent("External Control Dialog & HUD", "ui/components/TacticalToolsPanels.kt", "Panel visual en la app para monitorizar IP, puerto 9090, conexiones activas y comandos de depuración ADB.", listOf("ExternalApiServerDialog", "ApiEndpointCard"), "Compose M3 StateFlow"),
                LayerComponent("Web Command Console", "domain/remote/TacticalExternalControlServer.kt", "Dashboard web SPA táctico servido en /console con telemetría en vivo, botones de hardware y terminal CLI.", listOf("generateWebCommandConsoleHtml"), "HTML5, CSS3 Grid, Vanilla JS Fetch/SSE")
            ),
            inboundFlows = listOf("Peticiones HTTP REST desde PC externa", "Comandos CLI vía curl / scripts Python / PowerShell", "Interacción con Consola Web en navegador"),
            outboundFlows = listOf("Telemetría en tiempo real JSON", "Respuestas de ejecución de comandos", "Acciones directas sobre Capas 1 a 7")
        ),
        SystemLayer(
            layerNumber = 9,
            name = "Capa 9: Inteligencia Artificial Táctica & Edge Vision",
            tag = "TACTICAL_EDGE_AI",
            description = "Modelos neuronales cuantizados en dispositivo (TFLite / NNAPI) para reconocimiento óptico de matrículas, texto militar, análisis de terreno y asesoramiento autónomo de misión.",
            primaryResponsibility = "Inferencia local sin conexión a internet, detección de objetos en vídeo en tiempo real y asesor táctico adaptativo.",
            components = listOf(
                LayerComponent("TacticalMissionAgent", "domain/ai/TacticalMissionAgent.kt", "Asesor táctico autónomo para evaluación de riesgos y contingencias operativas.", listOf("evaluateTacticalSituation", "suggestContingencyPlan"), "On-Device Quantized AI"),
                LayerComponent("TacticalEvidenceAnalyzer", "domain/ai/TacticalEvidenceAnalyzer.kt", "Extracción militar de caracteres, códigos y matrículas en imágenes de campo.", listOf("analyzeEvidenceFrame", "extractTacticalText"), "CameraX, TFLite OCR"),
                LayerComponent("FaceRecognitionClassifier", "domain/ai/FaceRecognitionClassifier.kt", "Identificación biométrica local para control de acceso y verificación de operadores.", listOf("classifyFaceEmbedding", "registerOperatorTemplate"), "Local Neural Embeddings")
            ),
            inboundFlows = listOf("Cuadros de vídeo de CameraX", "Telemetría ambiental y del operador", "Consultas en lenguaje natural del operador"),
            outboundFlows = listOf("Alertas de amenazas en tiempo real", "Metadatos forenses para SITREP", "Capas de bounding boxes para visor HUD")
        ),
        SystemLayer(
            layerNumber = 10,
            name = "Capa 10: Cartografía Táctica, GIS & Simbología Militar",
            tag = "GEOSPATIAL_GIS_NATO",
            description = "Renderizado de capas geoespaciales offline, waypoints tácticos, polígonos de zonas de peligro y simbología estándar OTAN APP-6 / MIL-STD-2525D.",
            primaryResponsibility = "Mapeo topográfico sin internet, proyección de rumbos de escape y estandarización visual de fuerzas amigas y hostiles.",
            components = listOf(
                LayerComponent("TacticalGISManager", "domain/p2p/TacticalGISManager.kt", "Gestor de waypoints tácticos, líneas de fase y cálculo de rutas geodésicas.", listOf("addWaypoint", "calculateBearingDistance", "exportGisData"), "GeoJSON, WGS-84 Geodesy"),
                LayerComponent("NatoSymbologyOverlay", "domain/models/NatoSymbologyOverlay.kt", "Generador y clasificador de simbología militar OTAN APP-6D (Unidades, Equipo, Instalaciones).", listOf("generateNatoSymbol", "classifyAffiliation"), "MIL-STD-2525D Vectors"),
                LayerComponent("TacticalAStarRouter", "domain/c4isr/TacticalAStarRouter.kt", "Planificador de rutas de evacuación óptimas esquivando amenazas en cuadrícula de coste.", listOf("calculateEscapeRoute", "addDangerZone"), "Heuristic A* Search")
            ),
            inboundFlows = listOf("Coordenadas GPS/PDR", "Informes de contacto hostil", "Capas vectoriales de mapa local"),
            outboundFlows = listOf("Overlays cartográficos para RadarScreen", "Rutas de navegación inercial", "Marcadores CoT")
        ),
        SystemLayer(
            layerNumber = 11,
            name = "Capa 11: Interoperabilidad Conjunta C4ISR / CoT",
            tag = "INTEROP_COT_ATAK",
            description = "Transcodificación bidireccional y difusión de eventos estándar Cursor-on-Target (CoT XML/Protobuf) hacia ecosistemas ATAK, CivTAK y WinTAK vía UDP 4242.",
            primaryResponsibility = "Compatibilidad con redes tácticas inter-agencia e interoperabilidad con sistemas de mando y control occidentales.",
            components = listOf(
                LayerComponent("CursorOnTargetTranscoder", "domain/c4isr/CursorOnTargetTranscoder.kt", "Conversión de waypoints y telemetría a eventos CoT XML RFC 2.0 y serialización Protobuf.", listOf("toCotXmlEvent", "parseCotMessage", "broadcastUdpPacket"), "CoT XML, UDP Multicast 4242"),
                LayerComponent("SitrepReportManager", "domain/models/SitrepReportManager.kt", "Generador de reportes estandarizados de situación operativa militar (SITREP, SPOTREP).", listOf("compileSitrepReport", "serializeToMilStd"), "STANAG Military Reporting"),
                LayerComponent("SaluteIntelligenceAnalyzer", "domain/c4isr/SaluteIntelligenceAnalyzer.kt", "Estructuración de inteligencia de combate según formato SALUTE (Size, Activity, Location, Unit, Time, Equipment).", listOf("compileSaluteReport", "validateReport"), "NATO SALUTE Doctrine")
            ),
            inboundFlows = listOf("Paquetes de red ATAK UDP 4242", "Balizas de emergencia de malla", "Eventos de detección perimetral"),
            outboundFlows = listOf("Flujos de eventos CoT a servidores TAK", "Puntos de interés en mapas conjuntos", "Alertas de evacuación")
        ),
        SystemLayer(
            layerNumber = 12,
            name = "Capa 12: Guerra Electrónica & Monitoreo del Espectro RF",
            tag = "EW_SIGINT_RF",
            description = "Muestreo pasivo de niveles de señal de radio, detección de emisiones anómalas, cálculo de huella electromagnética y alerta temprana de inhibidores (jamming).",
            primaryResponsibility = "Auditoría espectral en tiempo real, detección de portadoras sospechosas y evaluación de vulnerabilidad de detección del operador.",
            components = listOf(
                LayerComponent("RfSpectrumAnalyzer", "domain/hardware/RfSpectrumAnalyzer.kt", "Muestreo espectral de frecuencias RF y detección de barridos de contramedidas electrónicas.", listOf("sampleCurrentSpectrum", "detectJammingInterference"), "Android Wi-Fi/Cellular Signal HAL"),
                LayerComponent("RfSignatureMeter", "domain/hardware/RfSignatureMeter.kt", "Cálculo en dBm de la huella de emisión electromagnética total del dispositivo para evasión.", listOf("calculateSignatureDbm", "evaluateDetectabilityRisk"), "RF Emission Model"),
                LayerComponent("SignalJammingDetector", "domain/sensors/SignalJammingDetector.kt", "Algoritmo de detección de caídas bruscas de SNR atribuibles a inhibidores hostiles.", listOf("analyzeSignalToNoiseRatio", "raiseJammingAlert"), "Statistical Noise Anomaly")
            ),
            inboundFlows = listOf("Muestras de intensidad de señal RSSI", "Niveles de ruido de fondo de radio", "Escaneo de balizas Wi-Fi/BLE"),
            outboundFlows = listOf("Alertas de inhibición a Capa 13 (Salto FHSS)", "Recomendación de EMCON Alpha", "Telemetría a la Consola Web")
        ),
        SystemLayer(
            layerNumber = 13,
            name = "Capa 13: Salto de Frecuencia, LPI/LPD & Anti-Jamming",
            tag = "FHSS_LPI_STEALTH",
            description = "Motor de salto de canales pseudo-aleatorio determinista sincronizado por semilla temporal CSPRNG para evasión de contramedidas electrónicas enemigas.",
            primaryResponsibility = "Conmutación ágil de frecuencias de transmisión a alta velocidad para reducir la probabilidad de interceptación (LPI/LPD).",
            components = listOf(
                LayerComponent("FrequencyHoppingMeshEngine", "domain/p2p/FrequencyHoppingMeshEngine.kt", "Conmutador pseudo-aleatorio de canales virtuales con sincronismo temporal estricto.", listOf("startFrequencyHopping", "stopFrequencyHopping", "computeNextHopChannel"), "CSPRNG PRF, Time-Slotted FHSS"),
                LayerComponent("AdaptiveFrequencyOrchestrator", "domain/p2p/AdaptiveFrequencyOrchestrator.kt", "Selección inteligente de bandas menos congestionadas o libres de guerra electrónica.", listOf("rankChannelQuality", "switchBandwidthProfile"), "Cognitive Radio Protocol"),
                LayerComponent("SpreadSpectrumModulator", "domain/hardware/SpreadSpectrumModulator.kt", "Ensanchamiento de espectro por secuencia directa DSSS virtual para resistencia a ruido.", listOf("spreadPayload", "despreadPayload"), "Direct-Sequence Spread Spectrum")
            ),
            inboundFlows = listOf("Semilla de misión criptográfica", "Alertas de interferencia de Capa 12", "Reloj sincronizado de Capa 24"),
            outboundFlows = listOf("Asignación de canales de radio para Capa 4", "Canales de transmisión seguros y ágiles")
        ),
        SystemLayer(
            layerNumber = 14,
            name = "Capa 14: Comunicaciones Ópticas, Li-Fi & Luz Visible",
            tag = "OPTICAL_LIFI_OWC",
            description = "Transmisión y recepción de datos digitales modulados en impulsos de luz mediante flash LED y fotodetectores de cámara en codificación Manchester.",
            primaryResponsibility = "Comunicaciones tácticas a corta distancia en entornos de estricto silencio electromagnético (EMCON Alpha) o bajo búnkeres de hormigón.",
            components = listOf(
                LayerComponent("VisualOpticalLiFiTransceiver", "domain/media/VisualOpticalLiFiTransceiver.kt", "Módem óptico bidireccional por luz visible usando flash LED y sensor CMOS de cámara.", listOf("transmitOpticalMessage", "startOpticalReception"), "Optical Wireless Communications (OWC)"),
                LayerComponent("MorseBeaconSynthesizer", "domain/audio/MorseBeaconSynthesizer.kt", "Sintetizador de ráfagas lumínicas y auditivas en código Morse militar con cadencia ITU-R.", listOf("playMorseSos", "transmitOpticalMorse"), "ITU-R M.1677 Morse Standard"),
                LayerComponent("OpticalManchesterCodec", "domain/media/OpticalManchesterCodec.kt", "Codificador y decodificador diferencial con tolerancia a variaciones de luz ambiental.", listOf("encodeManchesterBits", "decodeManchesterBits"), "Biphase-L Manchester Encoding")
            ),
            inboundFlows = listOf("Luminancia detectada por el sensor de cámara", "Comandos de baliza de emergencia", "Mensajes en silencio de RF"),
            outboundFlows = listOf("Pulsos estroboscópicos de flash LED", "Datagramas decodificados a la Capa 4 de Malla")
        ),
        SystemLayer(
            layerNumber = 15,
            name = "Capa 15: Acústica Sub-espectral & Enlace Data-Over-Sound",
            tag = "ACOUSTIC_ULTRASONIC_PHY",
            description = "Capa física acústica inaudible en 18.5/19.5 kHz FSK para transferencia de datos aire-a-aire entre dispositivos sin emitir radiofrecuencia detectable.",
            primaryResponsibility = "Modulación y demodulación de audio digital, preámbulos de sincronismo acústico y cálculo FFT de respuesta espectral.",
            components = listOf(
                LayerComponent("UltrasonicDataLinkTransceiver", "domain/audio/UltrasonicDataLinkTransceiver.kt", "Transceptor acústico ultrasónico de datos en modulación por cambio de frecuencia (FSK).", listOf("transmitDataOverSound", "startUltrasonicListening"), "AudioTrack PCM, 18.5/19.5 kHz FSK"),
                LayerComponent("TacticalVoiceSpectralEngine", "domain/c4isr/TacticalVoiceSpectralEngine.kt", "Inversión espectral de audio sobre portadora de 3.3 kHz para comunicaciones de voz encubiertas.", listOf("toggleVoiceScrambler", "sampleSpectralAudio"), "Fast Fourier Transform (FFT), DSP"),
                LayerComponent("AcousticShotClassifier", "domain/media/AcousticShotClassifier.kt", "Detección y clasificación en tiempo real de detonaciones de armas de fuego y ondas sónicas.", listOf("startAcousticSurveillance", "triggerTestAcousticImpulse"), "Acoustic Impulse Classifier")
            ),
            inboundFlows = listOf("Flujo de audio crudo de AudioRecord (44.1 kHz)", "Comandos de voz del operador", "Cargas útiles en modo sigilo"),
            outboundFlows = listOf("Ondas ultrasónicas por el altavoz", "Alertas de disparos detectados", "Voz descifrada a auriculares")
        ),
        SystemLayer(
            layerNumber = 16,
            name = "Capa 16: Telemetría Médica, Biológica & Hombre Caído",
            tag = "TCCC_BIOTELEMETRY",
            description = "Procesamiento de signos vitales (SpO2, FC, índice de fatiga en combate), detección de inmovilidad/impactos y generación automatizada de reportes 9-Line MEDEVAC.",
            primaryResponsibility = "Vigilancia de la supervivencia del operador y despacho inmediato de auxilio médico estandarizado STANAG.",
            components = listOf(
                LayerComponent("TacticalManDownDetector", "domain/hardware/TacticalManDownDetector.kt", "Monitor inercial de caída libre, impacto y pérdida prolongada de verticalidad del combatiente.", listOf("startMonitoring", "stopMonitoring", "simulateManDownEvent"), "Triaxial Accelerometer Sensor HAL"),
                LayerComponent("PhysioBioTelemetryManager", "domain/sensors/PhysioBioTelemetryManager.kt", "Receptor de telemetría de monitores médicos BLE (frecuencia cardíaca, SpO2, estrés térmico).", listOf("ingestVitalsSample", "calculateCombatFatigueIndex"), "Bluetooth GATT Health Profile"),
                LayerComponent("TacticalMedevacEngine", "domain/sensors/TacticalMedevacEngine.kt", "Generador automático del formato militar estandarizado 9-Line MEDEVAC con coordenadas GPS/PDR.", listOf("generate9LineMedevac", "dispatchEmergencyBroadcast"), "STANAG 2082 Tactical Evacuation")
            ),
            inboundFlows = listOf("Lecturas de acelerómetros a 50Hz", "Pulsímetros Bluetooth BLE", "Confirmaciones de vida del operador"),
            outboundFlows = listOf("Alertas de auxilio en malla P2P", "Reportes 9-Line MEDEVAC a la red", "Vibraciones hápticas de verificación")
        ),
        SystemLayer(
            layerNumber = 17,
            name = "Capa 17: Balística Forense, Computación de Tiro & Sensores Atmosféricos",
            tag = "BALLISTICS_MET_ENGINE",
            description = "Solucionador balístico de trayectorias (modelos de arrastre G1/G7), corrección barométrica por altitud y cálculo de deriva por viento cruzado en torretas MIL/MOA.",
            primaryResponsibility = "Cálculo matemático de impacto balístico de precisión y monitoreo de frentes meteorológicos barométricos de tormenta.",
            components = listOf(
                LayerComponent("TacticalBallisticsCalculator", "domain/c4isr/TacticalBallisticsCalculator.kt", "Motor de balística externa para fusiles de precisión con ajuste angular y compensación de viento.", listOf("computeSolution", "formatSolutionSummary"), "Pejsa Trajectory Model, MIL/MOA"),
                LayerComponent("BarometerStormAlertEngine", "domain/sensors/BarometerStormAlertEngine.kt", "Sensor de presión atmosférica con alerta anticipada de caídas de presión por frentes de tormenta.", listOf("startBarometerMonitoring", "simulateStormPressureDrop"), "Barometric Sensor HAL, NOAA Equations"),
                LayerComponent("SolarEphemerisCompass", "domain/sensors/SolarEphemerisCompass.kt", "Cálculo de azimut y elevación solar astronómica para orientación precisa del Norte Verdadero.", listOf("updateObserverPosition", "calculateSolarAzimuth"), "Astronomical Ephemeris Model")
            ),
            inboundFlows = listOf("Lecturas de presión hPa del sensor barométrico", "Parámetros balísticos del calibre ingresados", "Hora UTC y coordenadas"),
            outboundFlows = listOf("Compensación de torretas (MILs/MOA)", "Alertas meteorológicas severas", "Rumbo astronómico verdadero")
        ),
        SystemLayer(
            layerNumber = 18,
            name = "Capa 18: Navegación Inercial Autónoma & Odometría Subterránea",
            tag = "INERTIAL_PDR_ODOMETRY",
            description = "Navegación inercial pedestre pura mediante fusión de acelerómetro, giróscopo y sensor de pasos con estimación de longitud de zancada variable.",
            primaryResponsibility = "Geolocalización relativa ininterrumpida cuando la señal satelital GPS se pierde en túneles, cavernas o por inhibición intencional.",
            components = listOf(
                LayerComponent("PedestrianDeadReckoningEngine", "domain/sensors/PedestrianDeadReckoningEngine.kt", "Integrador de odometría pedestre basada en detección de pasos y rumbo giroscópico.", listOf("startPdrTracking", "stopPdrTracking", "simulateStepImpulse"), "Weinberg Stride Algorithm, Kalman IMU"),
                LayerComponent("ZeroVelocityUpdateEngine", "domain/sensors/ZeroVelocityUpdateEngine.kt", "Corrector de deriva de sensores inerciales aprovechando periodos estacionarios de pisada (ZUPT).", listOf("detectStancePhase", "applyZeroVelocityCorrection"), "ZUPT Inertial Drift Correction"),
                LayerComponent("SubterraneanBreadcrumbTracker", "domain/sensors/SubterraneanBreadcrumbTracker.kt", "Registro de migas de pan vectoriales tridimensionales para retorno seguro al punto de inserción.", listOf("dropBreadcrumb", "generateReturnPath"), "3D Vector Trail Ledger")
            ),
            inboundFlows = listOf("Aceleración triaxial y velocidad angular", "Sensor de podómetro de hardware", "Último punto GPS de calibración"),
            outboundFlows = listOf("Coordenadas locales relativas (dX, dY, dZ)", "Rumbo inercial a RadarScreen", "Rutas de retroceso táctico")
        ),
        SystemLayer(
            layerNumber = 19,
            name = "Capa 19: Perímetro Geofence Esférico 3D & Radar de Proximidad",
            tag = "SAFE_BUBBLE_3D",
            description = "Burbuja de seguridad tridimensional omnidireccional que calcula distancias euclidianas y altitudes relativas para detectar intrusiones en el perímetro táctico.",
            primaryResponsibility = "Alerta temprana de aproximación no autorizada de elementos hostiles o drones en el espacio aéreo local del equipo.",
            components = listOf(
                LayerComponent("SafeBubble3DProximityRadar", "domain/sensors/SafeBubble3DProximityRadar.kt", "Radar perimétrico esférico 3D con discriminación de identidades amigas y enemigas.", listOf("updatePerimeterRadius", "evaluateAirspaceBreaches"), "3D Euclidean Space Geofencing"),
                LayerComponent("AirspaceAltitudeDeconfliction", "domain/sensors/AirspaceAltitudeDeconfliction.kt", "Monitoreo de separación vertical barométrica entre aeronaves no tripuladas y personal.", listOf("checkVerticalSeparation", "alertAltitudeConflict"), "Altimetric Deconfliction"),
                LayerComponent("ProximityAcousticAlertBridge", "domain/audio/ProximityAcousticAlertBridge.kt", "Disparo de avisos auditivos discretos de intrusión a los auriculares del operador.", listOf("emitDirectionalBeep", "triggerTacticalHapticWarning"), "Directional Spatial Audio")
            ),
            inboundFlows = listOf("Posiciones tridimensionales de nodos de malla", "Telemetría de radar y sensores de proximidad", "Rango de alerta configurado"),
            outboundFlows = listOf("Notificaciones visuales de brecha perimétrica", "Alertas tácticas a la estación de mando externa", "Pulsos de vibración")
        ),
        SystemLayer(
            layerNumber = 20,
            name = "Capa 20: Enlace Robótico No Tripulado & Telemetría Dron",
            tag = "MAVLINK_UAV_CONTROL",
            description = "Pasarela de comunicación bidireccional protocolo MAVLink v2.0 para control de micro-drones de reconocimiento, lectura de telemetría y misiones de waypoints.",
            primaryResponsibility = "Integración de vectores aéreos no tripulados para reconocimiento ISR en tiempo real desde el dispositivo móvil.",
            components = listOf(
                LayerComponent("MavlinkTelemetryTransceiver", "domain/sensors/MavlinkTelemetryTransceiver.kt", "Codificador y decodificador de paquetes binarios MAVLink v2.0 sobre enlace serie/UDP.", listOf("sendHeartbeat", "parseMavlinkStream", "uploadMissionWaypoints"), "MAVLink v2.0 Micro Air Vehicle Protocol"),
                LayerComponent("TacticalPhotogrammetryEngine", "domain/sensors/TacticalPhotogrammetryEngine.kt", "Procesador de secuencias de fotos aéreas para ortomosaicos rápidos y mapas de elevación.", listOf("queueAerialFrames", "computeRoughElevationMap"), "Structure from Motion (SfM)"),
                LayerComponent("DroneWaypointMissionPlanner", "domain/c4isr/DroneWaypointMissionPlanner.kt", "Planificador de patrullas aéreas autónomas sobre coordenadas de objetivos tácticos.", listOf("buildSurveyGrid", "exportMissionPlan"), "STANAG 4586 UAV Mission Control")
            ),
            inboundFlows = listOf("Paquetes MAVLink UDP/Bluetooth de dron", "Secuencias de imágenes de reconocimiento aéreo", "Estado de batería del dron"),
            outboundFlows = listOf("Órdenes de guiado y retorno al punto de inicio (RTL)", "Capa de vídeo y posición de dron en radar", "Fotogrametría local")
        ),
        SystemLayer(
            layerNumber = 21,
            name = "Capa 21: Pasarela Satelital & Comunicaciones Tácticas BLOS",
            tag = "SATCOM_BLOS_GATEWAY",
            description = "Puente de enlace con módems satelitales Iridium SBD / Inmarsat para transmisión de ráfagas ultra-comprimidas cuando la malla terrestre queda aislada.",
            primaryResponsibility = "Enlace de última instancia con el puesto de mando central a escala intercontinental fuera de la línea de vista.",
            components = listOf(
                LayerComponent("SatelliteMeshGateway", "domain/sensors/SatelliteMeshGateway.kt", "Gestor de enlace satelital con empaquetado de ráfagas binarias de alta prioridad.", listOf("queueSatelliteMessage", "transmitBurstPayload"), "Iridium SBD Protocol, Serial AT Commands"),
                LayerComponent("BlosBurstCompressor", "domain/p2p/BlosBurstCompressor.kt", "Compresor matemático de telemetría a paquetes binarios de menos de 100 bytes.", listOf("compressTelemetryToSbd", "decompressSbdPayload"), "Ultra-dense Bit Packing"),
                LayerComponent("SatcomModemInterface", "domain/hardware/SatcomModemInterface.kt", "Controlador de interfaz serie UART/Bluetooth para módems portátiles (Iridium 9603).", listOf("sendAtCommand", "checkSatelliteSignalStrength"), "Hays AT Modem Command Set")
            ),
            inboundFlows = listOf("Mensajes urgentes de auxilio (Mayday/SITREP)", "Informes de posición de la malla", "Comandos descendentes satelitales"),
            outboundFlows = listOf("Ráfagas SBD a satélites LEO", "Inyecciones de telemetría en el enrutador DTN")
        ),
        SystemLayer(
            layerNumber = 22,
            name = "Capa 22: Integridad Distribuida, Auditoría Forense & Micro-Blockchain",
            tag = "BLOCKCHAIN_FORENSIC_AUDIT",
            description = "Libro mayor descentralizado inmutable basado en cadenas criptográficas de bloques SHA-256 para sellado temporal irrefutable de órdenes y evidencias.",
            primaryResponsibility = "Garantía de no repudio de órdenes militares y trazabilidad criptográfica contra adulteración o borrado malicioso.",
            components = listOf(
                LayerComponent("TacticalMissionBlockchain", "domain/security/TacticalMissionBlockchain.kt", "Cadena de bloques local con consenso Proof-of-Authority y encadenamiento SHA-256.", listOf("mineTacticalBlock", "verifyChainIntegrity", "exportLedgerJson"), "SHA-256 Merkle Blockchain"),
                LayerComponent("ForensicAuditSealer", "domain/security/ForensicAuditSealer.kt", "Sellador criptográfico de registros de operaciones con firma digital ed25519.", listOf("sealEvidenceRecord", "verifyRecordSignature"), "RFC 8032 Ed25519 Signatures"),
                LayerComponent("ChainReplicationEngine", "domain/p2p/ChainReplicationEngine.kt", "Sincronizador anti-entropía de bloques entre terminales tácticos en contacto de malla.", listOf("synchronizeBlocksWithPeer", "resolveBranchForks"), "Merkle Tree Difference Sync")
            ),
            inboundFlows = listOf("Órdenes tácticas emitidas", "Evidencias fotográficas capturadas", "Bloques de la cadena recibidos de pares"),
            outboundFlows = listOf("Bloques minados inmutables", "Verificación forense de autenticidad para auditorías militares")
        ),
        SystemLayer(
            layerNumber = 23,
            name = "Capa 23: Gestión de Energía Adaptativa & Perfil Térmico",
            tag = "POWER_THERMAL_PROFILE",
            description = "Regulación dinámica del consumo de batería, conmutación automática de frecuencias de CPU, modos monocromáticos OLED y control de disipación térmica.",
            primaryResponsibility = "Extensión extrema de la autonomía operativa del dispositivo en misiones prolongadas de supervivencia en terreno hostil.",
            components = listOf(
                LayerComponent("AdaptivePowerProfileManager", "domain/hardware/AdaptivePowerProfileManager.kt", "Controlador dinámico de perfiles de consumo energético y desconexión de radios ociosas.", listOf("evaluatePowerState", "applyEmergencyPowerProfile"), "Android BatteryManager & PowerManager HAL"),
                LayerComponent("ThermalNightShader", "domain/media/ThermalNightShader.kt", "Procesador de renderizado OLED monocromático de cero emisión de luz azul y bajo consumo.", listOf("applyTacticalThermalFilter", "setNightVisionPalette"), "OpenGL ES Shaders, Pure Black OLED"),
                LayerComponent("WakelockGovernor", "domain/hardware/WakelockGovernor.kt", "Regulador de bloqueos de suspensión de CPU para evitar drenajes térmicos en reposo.", listOf("acquireCoordinatedWakelock", "releaseAllWakelocks"), "PowerManager Wakelocks")
            ),
            inboundFlows = listOf("Nivel y temperatura de la batería en % y °C", "Tiempo estimado de misión restante", "Modo Lite activado"),
            outboundFlows = listOf("Ajustes de tasa de refresco y brillo de pantalla", "Suspensiones programadas de hilos no críticos")
        ),
        SystemLayer(
            layerNumber = 24,
            name = "Capa 24: Sincronización Temporal de Malla & PTP de Campo",
            tag = "MESH_CLOCK_PTP",
            description = "Algoritmo de sincronización temporal distribuida Berkeley / PTP para mantener coherencia de reloj a nivel de milisegundos sin depender de NTP ni GPS.",
            primaryResponsibility = "Alineación de slots de tiempo para salto de frecuencia FHSS, sellado de bloques y descifrado de ventanas temporales en malla.",
            components = listOf(
                LayerComponent("MeshTimeSynchronizer", "domain/c4isr/MeshTimeSynchronizer.kt", "Algoritmo de sincronización de reloj en red ad-hoc de consenso Berkeley.", listOf("startPeriodicSync", "handleTimeSyncPacket", "getCorrectedEpochMillis"), "Berkeley Distributed Clock Algorithm"),
                LayerComponent("TimeSlotCoordinator", "domain/p2p/TimeSlotCoordinator.kt", "Coordinador de ventanas temporales de transmisión TDMA para evitar colisiones en RF.", listOf("getCurrentTimeSlot", "isTransmissionWindowOpen"), "Time-Division Multiple Access (TDMA)"),
                LayerComponent("GpsTimeDisciplineFallback", "domain/sensors/GpsTimeDisciplineFallback.kt", "Disciplinamiento de oscilador local mediante pulsos PPS o mensajes NMEA de satélite cuando esté disponible.", listOf("calibrateLocalDrift", "estimateClockSkewPpm"), "Hardware Clock Skew Filter")
            ),
            inboundFlows = listOf("Tiempos de ida y vuelta (RTT) de paquetes de red", "Marcas de tiempo de balizas de pares", "Marcas de reloj atómico GPS"),
            outboundFlows = listOf("Hora de red consensuada sincronizada", "Disparo de slots de salto FHSS para Capa 13")
        )
    )

    val dataFlows: List<SystemDataFlow> = listOf(
        SystemDataFlow(
            id = "FLOW_REMOTE_CONTROL",
            title = "Control Externo por Computadora (API REST & Consola Web)",
            trigger = "Operador en PC envía petición HTTP / comando CLI o interactúa con la Consola Web /console",
            description = "Pasa por el servidor embebido TacticalExternalControlServer, autentica la llamada, rutea la orden al motor de dominio correspondiente, acciona el hardware si aplica y responde con telemetría JSON.",
            stepSequence = listOf(
                DataFlowStep(1, "Computadora Externa / Navegador", "http://<IP_DISPOSITIVO>:9090/console", "Emite comando REST (ej. POST /api/v1/hardware/flash o POST /api/v1/c4isr/salute)."),
                DataFlowStep(2, "TacticalExternalControlServer", "domain/remote/TacticalExternalControlServer.kt", "Recibe socket TCP en puerto 9090, analiza verbo HTTP, parsea JSON y verifica seguridad."),
                DataFlowStep(3, "Motor de Dominio Específico", "domain/c4isr/ o domain/security/", "Ejecuta la acción lógica (ej. calcular ruta A*, generar bloque PoA, encriptar Kyber)."),
                DataFlowStep(4, "Capa HAL de Hardware", "domain/media/ o domain/sensors/", "Dispara linterna LED, tono de audio o motor de vibración según lo ordenado."),
                DataFlowStep(5, "DiscoveryLogCollector", "domain/logging/DiscoveryLogCollector.kt", "Registra la traza forense con IP del cliente y comando ejecutado."),
                DataFlowStep(6, "TacticalExternalControlServer", "domain/remote/TacticalExternalControlServer.kt", "Envía respuesta HTTP 200 OK con payload JSON estructurado a la computadora.")
            ),
            securityLevel = "Nivel Táctico: Token de Autorización / Aislamiento LAN & USB Loopback",
            offlineCapable = true
        ),
        SystemDataFlow(
            id = "FLOW_E2EE_DTN",
            title = "Mensajería Cifrada E2EE con Enrutamiento DTN",
            trigger = "Operador presiona 'Enviar' en ChatScreen",
            description = "Flujo de datos desde la UI hasta la transmisión física en malla o almacenamiento seguro.",
            stepSequence = listOf(
                DataFlowStep(1, "ChatScreen", "ui/screens/ChatScreen.kt", "Captura el mensaje de texto del operador y lo pasa a ChatViewModel."),
                DataFlowStep(2, "ChatViewModel", "ui/viewmodels/ChatViewModel.kt", "Valida el destinatario, crea entidad Message y solicita compresión."),
                DataFlowStep(3, "TacticalBinaryCompressor", "domain/p2p/TacticalBinaryCompressor.kt", "Aplica codificación Huffman ultradensa reduciendo el tamaño en ~40%."),
                DataFlowStep(4, "PostQuantumKyberVault & CryptoManager", "domain/security/PostQuantumKyberVault.kt", "Cifra el payload comprimido mediante encapsulación ML-KEM Kyber-768 + AES-256 GCM."),
                DataFlowStep(5, "TacticalMissionBlockchain", "domain/security/TacticalMissionBlockchain.kt", "Registra el hash de la transacción en el bloque PoA inmutable."),
                DataFlowStep(6, "StoreAndForwardRouter", "domain/p2p/StoreAndForwardRouter.kt", "Determina si el nodo destino está al alcance directo."),
                DataFlowStep(7, "AutonomousMeshDiscoveryEngine", "domain/discovery/AutonomousMeshDiscoveryEngine.kt", "Si está al alcance, transmite por UDP Multicast o Bluetooth Mesh."),
                DataFlowStep(8, "OmniDatabase", "data/local/OmniDatabase.kt", "Persiste el mensaje con estado SENT o QUEUED en SQLite local.")
            ),
            securityLevel = "Cifrado Militar Híbrido: Kyber-768 + AES-256 GCM",
            offlineCapable = true
        ),
        SystemDataFlow(
            id = "FLOW_PDR_NAVIGATION",
            title = "Navegación Inercial sin GPS (PDR)",
            trigger = "Movimiento del operador detectado por acelerómetro/giróscopo",
            description = "Cálculo de trayectoria a estima para mantener conocimiento situacional en búnkeres o con jamming.",
            stepSequence = listOf(
                DataFlowStep(1, "SensorManager HAL", "Android Framework", "Emite eventos TYPE_ACCELEROMETER y TYPE_GYROSCOPE a 50Hz."),
                DataFlowStep(2, "PedestrianDeadReckoningEngine", "domain/sensors/PedestrianDeadReckoningEngine.kt", "Aplica filtro pasa-altos, detecta zancada mediante algoritmo Weinberg y estima longitud de paso."),
                DataFlowStep(3, "SolarEphemerisCompass", "domain/sensors/SolarEphemerisCompass.kt", "Proporciona corrección de rumbo mediante azimut solar astronómico."),
                DataFlowStep(4, "RadarViewModel", "ui/viewmodels/RadarViewModel.kt", "Calcula el nuevo punto (lat, lon) estimado y actualiza el StateFlow."),
                DataFlowStep(5, "RadarScreen & HUD", "ui/screens/RadarScreen.kt", "Renderiza la traza inercial sobre el canvas de radar táctico."),
                DataFlowStep(6, "TacticalAStarRouter", "domain/c4isr/TacticalAStarRouter.kt", "Actualiza la posición de origen para el cálculo de rutas de escape.")
            ),
            securityLevel = "Emisión Cero (RF Passive): 100% Silencioso",
            offlineCapable = true
        ),
        SystemDataFlow(
            id = "FLOW_SHOT_CLASSIFIER",
            title = "Detección y Clasificación Acústica de Disparos",
            trigger = "Transitorio de presión sonora capturado por el micrófono",
            description = "Detección de onda de choque balística y alerta SALUTE automática en la malla.",
            stepSequence = listOf(
                DataFlowStep(1, "AudioRecord HAL", "Android Framework", "Muestrea señal de audio a 44.1kHz 16-bit PCM en buffer circular."),
                DataFlowStep(2, "AcousticShotClassifier", "domain/media/AcousticShotClassifier.kt", "Detecta tiempo de subida ultrarrápido (<2ms) y relación de onda de choque vs detonación."),
                DataFlowStep(3, "AcousticShotClassifier", "domain/media/AcousticShotClassifier.kt", "Estima el calibre probable (ej. 5.56 NATO, 7.62 Soviet, .50 BMG)."),
                DataFlowStep(4, "SaluteIntelReportEngine", "domain/c4isr/SaluteIntelReportEngine.kt", "Genera automáticamente un reporte militar SALUTE con coordenadas estimadas."),
                DataFlowStep(5, "AutonomousMeshDiscoveryEngine", "domain/discovery/AutonomousMeshDiscoveryEngine.kt", "Difunde alerta flash prioritaria a los nodos de la patrulla."),
                DataFlowStep(6, "C4IsrMasterDashboardEngine", "domain/c4isr/C4IsrMasterDashboardEngine.kt", "Actualiza el HUD táctico y emite alerta sonora/háptica.")
            ),
            securityLevel = "Alerta Prioritaria con Firma Criptográfica",
            offlineCapable = true
        ),
        SystemDataFlow(
            id = "FLOW_ZEROIZE_WIPE",
            title = "Protocolo de Autodestrucción Cero-Huella ZEROIZE (DoD 5220.22-M)",
            trigger = "Operador confirma código de autodestrucción o se detecta sabotaje físico",
            description = "Destrucción irreversible de datos, claves maestras de hardware y memoria volátil.",
            stepSequence = listOf(
                DataFlowStep(1, "EmergencyZeroizeDialog / Remote API", "domain/remote/TacticalExternalControlServer.kt", "Valida código de seguridad (código táctico o token remoto)."),
                DataFlowStep(2, "EmergencyZeroizeManager", "domain/security/EmergencyZeroizeManager.kt", "Sobreescribe el archivo SQLite con ceros (0x00), unos (0xFF) y bytes aleatorios."),
                DataFlowStep(3, "CryptoManager", "domain/security/CryptoManager.kt", "Destruye las claves maestras almacenadas en el Android KeyStore de hardware."),
                DataFlowStep(4, "FeatureManager & SharedPreferences", "domain/config/FeatureManager.kt", "Borra todas las preferencias, llaves de sesión y tokens."),
                DataFlowStep(5, "DiscoveryLogCollector", "domain/logging/DiscoveryLogCollector.kt", "Limpia los buffers volátiles de memoria."),
                DataFlowStep(6, "System.exit", "domain/security/EmergencyZeroizeManager.kt", "Finaliza el proceso de la app sin dejar rastro en disco.")
            ),
            securityLevel = "Nivel Militar: Irreversible DoD 5220.22-M",
            offlineCapable = true
        )
    )

    val codeIndex: List<CodeModuleRecord> = listOf(
        // Remote & External API
        CodeModuleRecord("domain.remote", "TacticalExternalControlServer.kt", "app/src/main/java/com/example/domain/remote/TacticalExternalControlServer.kt", "TacticalExternalControlServer", "Servidor embebido HTTP REST & Consola Web para control total de la app, 31 subsistemas y hardware desde PC externa.", listOf("startServer", "stopServer", "routeHttpRequest", "executeTacticalCliCommand", "generateWebCommandConsoleHtml"), listOf("C4IsrMasterDashboardEngine", "EmergencyZeroizeManager", "DiscoveryLogCollector"), listOf("MainActivity", "ConfigurationScreen", "TacticalToolsPanels"), "CONTROL EXTERNO & API"),

        // C4ISR
        CodeModuleRecord("domain.c4isr", "C4IsrMasterDashboardEngine.kt", "app/src/main/java/com/example/domain/c4isr/C4IsrMasterDashboardEngine.kt", "C4IsrMasterDashboardEngine", "Consola de mando y control centralizada para supervisión de los 31 subsistemas tácticos.", listOf("aggregateHealth", "getSubsystemStatus", "broadcastStatus"), listOf("FeatureManager", "DiscoveryLogCollector"), listOf("ConfigurationScreen", "TacticalToolsPanels"), "C4ISR & MANDO"),
        CodeModuleRecord("domain.c4isr", "TacticalAStarRouter.kt", "app/src/main/java/com/example/domain/c4isr/TacticalAStarRouter.kt", "TacticalAStarRouter", "Algoritmo de enrutamiento heurístico A* con penalización dinámica por zonas de peligro y hostiles.", listOf("findEscapeRoute", "setDangerZones", "calculateHeuristic"), listOf("TacticalGISModels"), listOf("RadarScreen", "TacticalToolsPanels"), "NAVEGACIÓN & EVASIÓN"),
        CodeModuleRecord("domain.c4isr", "TacticalVoiceSpectralEngine.kt", "app/src/main/java/com/example/domain/c4isr/TacticalVoiceSpectralEngine.kt", "TacticalVoiceSpectralEngine", "Inversión espectral de audio a 3.3 kHz y visualizador de espectro FFT para comunicaciones de voz discretas.", listOf("invertSpectrum", "computeFft", "setCarrierFreq"), listOf("AudioRecord", "AudioTrack"), listOf("TacticalToolsPanels", "ChatScreen"), "SEGURIDAD DE VOZ"),
        CodeModuleRecord("domain.c4isr", "SaluteIntelReportEngine.kt", "app/src/main/java/com/example/domain/c4isr/SaluteIntelReportEngine.kt", "SaluteIntelReportEngine", "Generación y transmisión estructurada de informes de inteligencia militar bajo formato SALUTE.", listOf("generateReport", "broadcastSalute", "parseSalutePacket"), listOf("DiscoveryLogCollector", "StoreAndForwardRouter"), listOf("TacticalToolsPanels", "ChatScreen"), "INTELIGENCIA"),
        CodeModuleRecord("domain.c4isr", "MeshTimeSynchronizer.kt", "app/src/main/java/com/example/domain/c4isr/MeshTimeSynchronizer.kt", "MeshTimeSynchronizer", "Sincronizador de reloj de malla táctico con precisión sub-microsegundo para saltos FHSS coordinados.", listOf("sendSyncPulse", "calculateDrift", "getNetworkTimeNanos"), listOf("DiscoveryLogCollector"), listOf("FrequencyHoppingMeshEngine", "TacticalToolsPanels"), "SINCRONIZACIÓN"),

        // Security
        CodeModuleRecord("domain.security", "PostQuantumKyberVault.kt", "app/src/main/java/com/example/domain/security/PostQuantumKyberVault.kt", "PostQuantumKyberVault", "Bóveda post-cuántica ML-KEM Kyber-768 híbrida con intercambio X25519 resistente a computación cuántica.", listOf("generateKyberKeyPair", "encapsulate", "decapsulate"), listOf("CryptoManager"), listOf("ChatViewModel", "TacticalToolsPanels"), "CRIPTOGRAFÍA PQC"),
        CodeModuleRecord("domain.security", "ShamirSecretSharingVault.kt", "app/src/main/java/com/example/domain/security/ShamirSecretSharingVault.kt", "ShamirSecretSharingVault", "Algoritmo de división de secretos K-de-N sobre Galois Field GF(256) para fragmentación de llaves.", listOf("splitSecret", "reconstructSecret", "galoisMultiply"), listOf("CryptoManager"), listOf("TacticalToolsPanels"), "SEGURIDAD ZERO-TRUST"),
        CodeModuleRecord("domain.security", "TacticalMissionBlockchain.kt", "app/src/main/java/com/example/domain/security/TacticalMissionBlockchain.kt", "TacticalMissionBlockchain", "Micro-Blockchain Proof-of-Authority inmutable con hash SHA-256 para registro forense de órdenes.", listOf("mineBlock", "verifyChain", "addOrder"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "INTEGRIDAD & BLOCKCHAIN"),
        CodeModuleRecord("domain.security", "EmergencyZeroizeManager.kt", "app/src/main/java/com/example/domain/security/EmergencyZeroizeManager.kt", "EmergencyZeroizeManager", "Borrado seguro de 3 pasadas DoD 5220.22-M para destrucción inmediata de datos y claves.", listOf("zeroizeAllData", "overwriteSecurely", "purgeKeyStore"), listOf("OmniDatabase", "CryptoManager"), listOf("TacticalToolsPanels", "MainActivity"), "CONTRA-INTELIGENCIA"),
        CodeModuleRecord("domain.security", "SybilMeshDetector.kt", "app/src/main/java/com/example/domain/security/SybilMeshDetector.kt", "SybilMeshDetector", "Detector forense de identidades duplicadas y ataques Sybil mediante análisis PHY/MAC.", listOf("inspectNodeRssi", "detectSybilClones", "isolateNode"), listOf("AutonomousMeshDiscoveryEngine"), listOf("TacticalToolsPanels"), "CIBERDEFENSA"),
        CodeModuleRecord("domain.security", "EmconAlphaManager.kt", "app/src/main/java/com/example/domain/security/EmconAlphaManager.kt", "EmconAlphaManager", "Gestor de políticas de silencio de radio por software EMCON Alpha/Bravo/Charlie.", listOf("setEmconLevel", "canTransmit", "getEmconState"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels", "AutonomousMeshDiscoveryEngine"), "SIGILO RF"),
        CodeModuleRecord("domain.security", "RfEmissionSignatureMeter.kt", "app/src/main/java/com/example/domain/security/RfEmissionSignatureMeter.kt", "RfEmissionSignatureMeter", "Estimador de probabilidad de intercepción enemiga (LPI/LPD) y modelado de huella de radiofrecuencia.", listOf("calculateInterceptionRisk", "measureTxPower", "getStealthScore"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "GUERRA ELECTRÓNICA"),
        CodeModuleRecord("domain.security", "MeshHoneypotGenerator.kt", "app/src/main/java/com/example/domain/security/MeshHoneypotGenerator.kt", "MeshHoneypotGenerator", "Generador de identidades falsas y nodos fantasma para despistar radiogoniometría hostil.", listOf("generateDecoyNodes", "broadcastFakeTelemetry", "trapHostileProbes"), listOf("LanMulticastDiscoveryBeacon"), listOf("TacticalToolsPanels"), "ENGAÑO TÁCTICO"),

        // Sensors
        CodeModuleRecord("domain.sensors", "PedestrianDeadReckoningEngine.kt", "app/src/main/java/com/example/domain/sensors/PedestrianDeadReckoningEngine.kt", "PedestrianDeadReckoningEngine", "Navegación inercial por pasos y orientación sin GPS para túneles y áreas con jamming de satélites.", listOf("onSensorChanged", "detectStep", "updateCoordinates"), listOf("SensorManager"), listOf("RadarViewModel", "TacticalToolsPanels"), "NAVEGACIÓN INERCIAL"),
        CodeModuleRecord("domain.sensors", "BarometerStormAlertEngine.kt", "app/src/main/java/com/example/domain/sensors/BarometerStormAlertEngine.kt", "BarometerStormAlertEngine", "Monitoreo barométrico de presión atmosférica (hPa) y detección de frentes de tormenta severa.", listOf("processPressure", "calculateQnhAltitude", "detectDropTendency"), listOf("SensorManager"), listOf("TacticalToolsPanels", "RadarScreen"), "SENSORES DE CAMPO"),
        CodeModuleRecord("domain.sensors", "PhysioBioTelemetryManager.kt", "app/src/main/java/com/example/domain/sensors/PhysioBioTelemetryManager.kt", "PhysioBioTelemetryManager", "Cálculo de índice de fatiga del combatiente, variabilidad cardíaca y estrés térmico en misión.", listOf("updateHeartRate", "computeReadinessScore", "evaluateThermalLoad"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "BIOMEDICINA"),
        CodeModuleRecord("domain.sensors", "TacticalBallisticsCalculator.kt", "app/src/main/java/com/example/domain/sensors/TacticalBallisticsCalculator.kt", "TacticalBallisticsCalculator", "Cálculo de corrección balística de elevación MRAD/MOA, deriva de viento y ángulo de tiro cosenoidal.", listOf("calculateFiringSolution", "adjustForAtmosphere", "getCoriolisCorrection"), listOf("BarometerStormAlertEngine"), listOf("TacticalToolsPanels"), "BALÍSTICA TÁCTICA"),
        CodeModuleRecord("domain.sensors", "MavlinkTelemetryTransceiver.kt", "app/src/main/java/com/example/domain/sensors/MavlinkTelemetryTransceiver.kt", "MavlinkTelemetryTransceiver", "Transceptor MAVLink v2 para recepción de telemetría de drones UAV y comandos de regreso a base (RTL).", listOf("parseMavlinkStream", "sendRtlCommand", "setMissionWaypoints"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels", "RadarScreen"), "UAV & ROBÓTICA"),
        CodeModuleRecord("domain.sensors", "TacticalMedevacEngine.kt", "app/src/main/java/com/example/domain/sensors/TacticalMedevacEngine.kt", "TacticalMedevacEngine", "Protocolo estandarizado 9-Line MEDEVAC OTAN y control de tiempos de torniquete TCCC.", listOf("create9LineRequest", "recordTourniquetApplication", "broadcastMedevac"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "MEDICINA TÁCTICA"),
        CodeModuleRecord("domain.sensors", "SolarEphemerisCompass.kt", "app/src/main/java/com/example/domain/sensors/SolarEphemerisCompass.kt", "SolarEphemerisCompass", "Determinación del Norte Verdadero mediante cálculo de azimut solar astronómico sin magnetómetro ni GPS.", listOf("calculateSolarAzimuth", "getTrueNorthHeading", "calculateSolarZenith"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels", "RadarScreen"), "ASTRONOMÍA & RUMBO"),
        CodeModuleRecord("domain.sensors", "SatelliteMeshGateway.kt", "app/src/main/java/com/example/domain/sensors/SatelliteMeshGateway.kt", "SatelliteMeshGateway", "Pasarela satelital DTN para cálculo de ventanas de paso de satélites LEO y descarga de ráfagas.", listOf("predictPassWindow", "queueBurstTransmission", "flushOnSatelliteContact"), listOf("StoreAndForwardRouter"), listOf("TacticalToolsPanels"), "SATCOM & DTN"),
        CodeModuleRecord("domain.sensors", "TacticalPhotogrammetryEngine.kt", "app/src/main/java/com/example/domain/sensors/TacticalPhotogrammetryEngine.kt", "TacticalPhotogrammetryEngine", "Costura offline de fotogramas aéreos, georreferenciación de ortomosaicos y cálculo GSD.", listOf("stitchFrames", "calculateGsdResolution", "generateOrthomosaic"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "FOTOGRAMETRÍA & GIS"),
        CodeModuleRecord("domain.sensors", "SafeBubble3DProximityRadar.kt", "app/src/main/java/com/example/domain/sensors/SafeBubble3DProximityRadar.kt", "SafeBubble3DProximityRadar", "Burbuja de protección perimetral 3D con radar esférico y alertas de proximidad hostil en 360 grados.", listOf("evaluateProximity", "calculate3dDistance", "triggerPerimeterBreach"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels", "RadarScreen"), "SEGURIDAD DE BASE"),

        // Media & Audio
        CodeModuleRecord("domain.media", "AcousticShotClassifier.kt", "app/src/main/java/com/example/domain/media/AcousticShotClassifier.kt", "AcousticShotClassifier", "Procesamiento de señal DSP para detección de onda supersónica de disparo y estimación de calibre.", listOf("startListening", "processAudioBuffer", "detectMuzzleBlast"), listOf("AudioRecord", "DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "DSP & DETECCIÓN"),
        CodeModuleRecord("domain.media", "VisualOpticalLiFiTransceiver.kt", "app/src/main/java/com/example/domain/media/VisualOpticalLiFiTransceiver.kt", "VisualOpticalLiFiTransceiver", "Transmisión óptica Li-Fi fuera de banda por modulación de linterna LED e interrupciones de cámara.", listOf("transmitMorseCode", "flashTorchPulse", "decodeOpticalSignal"), listOf("CameraManager"), listOf("TacticalToolsPanels"), "LI-FI & ÓPTICA"),
        CodeModuleRecord("domain.audio", "UltrasonicDataLinkTransceiver.kt", "app/src/main/java/com/example/domain/audio/UltrasonicDataLinkTransceiver.kt", "UltrasonicDataLinkTransceiver", "Enlace acústico sub-audible en banda 18-20 kHz mediante modulación BFSK para comunicaciones covert.", listOf("transmitBfskAudio", "listenUltrasonicStream", "demodulateBfsk"), listOf("AudioTrack", "AudioRecord"), listOf("TacticalToolsPanels"), "ACÚSTICA COVERT"),

        // P2P & Discovery
        CodeModuleRecord("domain.p2p", "FrequencyHoppingMeshEngine.kt", "app/src/main/java/com/example/domain/p2p/FrequencyHoppingMeshEngine.kt", "FrequencyHoppingMeshEngine", "Salto virtual de frecuencia FHSS con sincronización pseudo-aleatoria para evadir interferencias y jamming.", listOf("generateHoppingPattern", "syncHoppingSeed", "getCurrentChannel"), listOf("MeshTimeSynchronizer"), listOf("TacticalToolsPanels"), "GUERRA ELECTRÓNICA"),
        CodeModuleRecord("domain.p2p", "TacticalBinaryCompressor.kt", "app/src/main/java/com/example/domain/p2p/TacticalBinaryCompressor.kt", "TacticalBinaryCompressor", "Compresor binario Huffman / CBOR ultradenso para transmisiones en enlaces de ancho de banda mínimo.", listOf("compressPayload", "decompressPayload", "getCompressionRatio"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels", "ChatViewModel"), "OPTIMIZACIÓN DE RED"),
        CodeModuleRecord("domain.p2p", "StoreAndForwardRouter.kt", "app/src/main/java/com/example/domain/p2p/StoreAndForwardRouter.kt", "StoreAndForwardRouter", "Enrutador DTN con almacenamiento de paquetes y entrega oportunista cuando se restablece el contacto.", listOf("routeBundle", "storeForLater", "flushOnNodeMeeting"), listOf("OmniDatabase"), listOf("ChatViewModel", "AutonomousMeshDiscoveryEngine"), "ENRUTAMIENTO DTN"),
        CodeModuleRecord("domain.discovery", "AutonomousMeshDiscoveryEngine.kt", "app/src/main/java/com/example/domain/discovery/AutonomousMeshDiscoveryEngine.kt", "AutonomousMeshDiscoveryEngine", "Motor autónomo de auto-descubrimiento en segundo plano y gestión de enlaces multicanal UDP/BLE.", listOf("startAutoDiscovery", "publishLocalPresence", "stopAutoDiscovery"), listOf("LanMulticastDiscoveryBeacon", "BluetoothMeshService"), listOf("MainActivity", "RadarScreen", "ChatViewModel"), "DESCUBRIMIENTO DE RED"),

        // AI & Evidence
        CodeModuleRecord("domain.ai", "TacticalMissionAgent.kt", "app/src/main/java/com/example/domain/ai/TacticalMissionAgent.kt", "TacticalMissionAgent", "Agente autónomo local de evaluación de amenazas y sugerencia de Cursos de Acción (COA) offline.", listOf("evaluateThreatProfile", "generateCourseOfAction", "runHeuristicAnalysis"), listOf("DiscoveryLogCollector"), listOf("TacticalToolsPanels"), "IA TÁCTICA LOCAL"),
        CodeModuleRecord("domain.ai", "TacticalEvidenceAnalyzer.kt", "app/src/main/java/com/example/domain/ai/TacticalEvidenceAnalyzer.kt", "TacticalEvidenceAnalyzer", "Análisis forense de imágenes de evidencias, lectura de placas y reconocimiento militar con Gemini.", listOf("analyzeTacticalImage", "transcribeVoiceEvidence", "extractMilitaryOcr"), listOf("BuildConfig"), listOf("CameraViewModel", "CloudViewModel"), "IA MULTIMODAL")
    )

    val tacticalCapabilities: List<TacticalCapability> = listOf(
        TacticalCapability("CAP_01", "Comunicaciones Ópticas Li-Fi (Flash LED)", "GUERRA ELECTRÓNICA", "Transmisión visual en total silencio RF usando pulsos de linterna LED", 1, "VisualOpticalLiFiTransceiver", "domain/media/VisualOpticalLiFiTransceiver.kt", true, true, true),
        TacticalCapability("CAP_02", "Fragmentación de Claves Shamir (K-de-N)", "CRIPTOGRAFÍA", "División de secretos en Galois Field GF(256) entre operadores de patrulla", 2, "ShamirSecretSharingVault", "domain/security/ShamirSecretSharingVault.kt", false, true, false),
        TacticalCapability("CAP_03", "Clasificador Acústico de Disparos & Detonaciones", "DSP & INTELIGENCIA", "Detección de onda de choque y calibre estimado en tiempo real por micrófono", 3, "AcousticShotClassifier", "domain/media/AcousticShotClassifier.kt", true, false, true),
        TacticalCapability("CAP_04", "Telemetría Biológica & Combat Readiness", "BIOMEDICINA", "Monitoreo de fatiga, variabilidad cardíaca y estrés térmico del combatiente", 4, "PhysioBioTelemetryManager", "domain/sensors/PhysioBioTelemetryManager.kt", true, false, false),
        TacticalCapability("CAP_05", "Protocolo de Autodestrucción ZEROIZE (DoD)", "CONTRA-INTELIGENCIA", "Borrado irreversible de 3 pasadas DoD 5220.22-M en bases de datos y claves", 5, "EmergencyZeroizeManager", "domain/security/EmergencyZeroizeManager.kt", true, true, true),
        TacticalCapability("CAP_06", "Navegación Inercial sin GPS (Dead Reckoning)", "NAVEGACIÓN", "Posicionamiento por pasos, giróscopo y rumbo ante inhibición GNSS", 6, "PedestrianDeadReckoningEngine", "domain/sensors/PedestrianDeadReckoningEngine.kt", true, true, true),
        TacticalCapability("CAP_07", "Salto de Frecuencia Virtual FHSS Mesh", "GUERRA ELECTRÓNICA", "Saltos de canal pseudo-aleatorios coordinados por semilla criptográfica", 7, "FrequencyHoppingMeshEngine", "domain/p2p/FrequencyHoppingMeshEngine.kt", true, true, false),
        TacticalCapability("CAP_08", "Simbología Táctica Militar OTAN APP-6", "C4ISR & MANDO", "Marcado normalizado de unidades amigas, hostiles y neutrales en el mapa", 8, "NatoSymbologyRegistry", "domain/models/NatoSymbologyOverlay.kt", true, false, false),
        TacticalCapability("CAP_09", "Agente Táctico Autónomo de Misión (COA)", "INTELIGENCIA ARTIFICIAL", "Motor heurístico local de evaluación de amenazas y cursos de acción", 9, "TacticalMissionAgent", "domain/ai/TacticalMissionAgent.kt", true, true, false),
        TacticalCapability("CAP_10", "Compresión Binaria Ultradensa Huffman/CBOR", "OPTIMIZACIÓN RED", "Empaquetado binario para transmisiones en canales de bajo ancho de banda", 10, "TacticalBinaryCompressor", "domain/p2p/TacticalBinaryCompressor.kt", true, false, false),
        TacticalCapability("CAP_11", "Barómetro Táctico & Alerta de Tormenta", "SENSORES DE CAMPO", "Detección de caídas de presión hPa y cálculo de altitud hipsométrica QNH", 11, "BarometerStormAlertEngine", "domain/sensors/BarometerStormAlertEngine.kt", true, false, true),
        TacticalCapability("CAP_12", "Enlace Acústico Sub-audible (18-20 kHz)", "COMUNICACIÓN COVERT", "Modem BFSK por ultrasonidos para intercambio seguro sin señales de radio", 12, "UltrasonicDataLinkTransceiver", "domain/audio/UltrasonicDataLinkTransceiver.kt", true, true, true),
        TacticalCapability("CAP_13", "Burbuja Geofence 3D (Radar de Proximidad)", "SEGURIDAD DE BASE", "Detección esférica de proximidad y alertas de incursión hostil 360°", 13, "SafeBubble3DProximityRadar", "domain/sensors/SafeBubble3DProximityRadar.kt", true, false, false),
        TacticalCapability("CAP_14", "Calculadora Balística Táctica (Rifleman)", "BALÍSTICA TÁCTICA", "Solución de tiro MRAD/MOA con corrección de viento y ángulo cosenoidal", 14, "TacticalBallisticsCalculator", "domain/sensors/TacticalBallisticsCalculator.kt", true, false, false),
        TacticalCapability("CAP_15", "Red de Señuelos RF & Nodos Fantasma", "SIGINT & ENGAÑO", "Emisión de tráfico falso para despistar radiogoniometría enemiga", 15, "MeshHoneypotGenerator", "domain/security/MeshHoneypotGenerator.kt", true, false, false),
        TacticalCapability("CAP_16", "Detector Forense de Ataques Sybil", "CIBERSEGURIDAD", "Inspección PHY/MAC y aislamiento de identidades clonadas en la malla", 16, "SybilMeshDetector", "domain/security/SybilMeshDetector.kt", true, true, false),
        TacticalCapability("CAP_17", "Bóveda Post-Cuántica ML-KEM Kyber-768", "CRIPTOGRAFÍA PQC", "Criptografía híbrida post-cuántica resistente a supercomputadores cuánticos", 17, "PostQuantumKyberVault", "domain/security/PostQuantumKyberVault.kt", true, true, false),
        TacticalCapability("CAP_18", "Micro-Blockchain Táctica Proof-of-Authority", "INTEGRIDAD & FORENSE", "Libro mayor inmutable SHA-256 para registro de órdenes de combate y bajas", 18, "TacticalMissionBlockchain", "domain/security/TacticalMissionBlockchain.kt", true, true, false),
        TacticalCapability("CAP_19", "Medidor de Huella Electromagnética LPI/LPD", "GUERRA ELECTRÓNICA", "Estimación de riesgo de intercepción de señales y control de sigilo RF", 19, "RfEmissionSignatureMeter", "domain/security/RfEmissionSignatureMeter.kt", true, true, false),
        TacticalCapability("CAP_20", "Control de Emisiones EMCON Alpha", "SIGILO & DISCIPLINA RF", "Silencio de radio estricto por software ante amenazas de guerra electrónica", 20, "EmconAlphaManager", "domain/security/EmconAlphaManager.kt", true, true, false),
        TacticalCapability("CAP_21", "Mapeador Fotogramétrico Aéreo 2D", "RECONOCIMIENTO & GIS", "Costura offline de ortomosaicos y cálculo de resolución GSD de terreno", 21, "TacticalPhotogrammetryEngine", "domain/sensors/TacticalPhotogrammetryEngine.kt", false, false, false),
        TacticalCapability("CAP_22", "Transceptor Telemetría MAVLink UAV ISR", "UAV & ROBÓTICA", "Recepción de estados de drones, waypoints y orden de retorno RTL de emergencia", 22, "MavlinkTelemetryTransceiver", "domain/sensors/MavlinkTelemetryTransceiver.kt", true, false, false),
        TacticalCapability("CAP_23", "Triaje Táctico 9-Line MEDEVAC & TCCC", "MEDICINA DE COMBATE", "Formulario OTAN de evacuación médica y cronómetro de torniquetes", 23, "TacticalMedevacEngine", "domain/sensors/TacticalMedevacEngine.kt", true, false, false),
        TacticalCapability("CAP_24", "Brújula Solar & Efemérides Astronómicas", "NAVEGACIÓN ASTRONÓMICA", "Norte Verdadero por azimut solar ante bloqueo de GPS y brújula magnética", 24, "SolarEphemerisCompass", "domain/sensors/SolarEphemerisCompass.kt", true, false, false),
        TacticalCapability("CAP_25", "Pasarela Satelital SATCOM DTN Gateway", "SATCOM & DTN", "Predicción de pases de satélites LEO y descarga de paquetes en tránsito", 25, "SatelliteMeshGateway", "domain/sensors/SatelliteMeshGateway.kt", true, false, false),
        TacticalCapability("CAP_26", "Enmascarador Acústico Espectral de Voz", "SEGURIDAD DE VOZ", "Inversión espectral 3.3 kHz y análisis FFT contra interceptación de audio", 26, "TacticalVoiceSpectralEngine", "domain/c4isr/TacticalVoiceSpectralEngine.kt", true, true, true),
        TacticalCapability("CAP_27", "Enrutador Táctico de Escape A* (E&E)", "PLANIFICACIÓN & EVASIÓN", "Generación de rutas óptimas de escape evitando zonas hostiles en mapa", 27, "TacticalAStarRouter", "domain/c4isr/TacticalAStarRouter.kt", true, false, false),
        TacticalCapability("CAP_28", "Generador de Informes de Inteligencia SALUTE", "INTELIGENCIA MILITAR", "Estructuración y difusión de reportes militares estándar sobre la malla", 28, "SaluteIntelReportEngine", "domain/c4isr/SaluteIntelReportEngine.kt", true, false, false),
        TacticalCapability("CAP_29", "Sincronizador de Reloj Atómico PPS en Malla", "TEMPORIZACIÓN & SYNC", "Alineación temporal sub-microsegundo para saltos FHSS coordinados", 29, "MeshTimeSynchronizer", "domain/c4isr/MeshTimeSynchronizer.kt", true, true, false),
        TacticalCapability("CAP_30", "Consola Maestra C4ISR Multidominio", "MANDO MAESTRO & C4ISR", "Centro de mando unificado con supervisión de los 31 subsistemas tácticos", 30, "C4IsrMasterDashboardEngine", "domain/c4isr/C4IsrMasterDashboardEngine.kt", true, true, false),
        TacticalCapability("CAP_31", "Servidor de Control Externo API REST & Consola Web PC", "CONTROL EXTERNO & API", "Control total de todos los aspectos de la app, sensores y hardware desde una computadora externa", 31, "TacticalExternalControlServer", "domain/remote/TacticalExternalControlServer.kt", true, true, true)
    ) + ExtendedSystemArchitectureRecords.tacticalCapabilities

    val masterRecords: List<SystemMasterArchitectureRecord> = listOf(
        SystemMasterArchitectureRecord(
            phaseNumber = 1,
            blockNumber = 1,
            title = "Fase 1: Transmisión Óptica Li-Fi (Flash OOB)",
            operationalScope = "Canal óptico visual secundario fuera de banda mediante modulación de flash LED e interrupciones de cámara.",
            category = "GUERRA ELECTRÓNICA & EW",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio DSP", "Capa 7: HAL Hardware"),
            domainModule = "VisualOpticalLiFiTransceiver",
            sourceFiles = listOf("domain/media/VisualOpticalLiFiTransceiver.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/hardware/flash", "POST /api/v1/subsystems/1/execute", "CLI: flash pulse [n]"),
            implementationModes = "Modulación de pulso lumínico Manchester / PWM con sincronización por fotograma y temporización de milisegundos en corrutinas I/O.",
            researchAndStandards = "IEEE 802.15.7 (Visible Light Communication), MIL-STD-188 (Optical Tactical Signaling), NATO OOB Standards.",
            hardwareAndSensors = listOf("CameraManager Torch HAL", "CameraCharacteristics FLASH_INFO_AVAILABLE", "Fototransistor / Sensor de Luz"),
            zeroTrustAndFaultTolerance = "Canal no radiante (cero emisiones RF). Imposible de interceptar por radiogoniometría o escáneres SDR.",
            futureProjectionsAndRoadmap = "Soporte de transmisión por infrarrojos (IR LED) y decodificación rolling-shutter por sensor CMOS a 120 FPS.",
            verificationMethods = "Módulos probados con ráfagas de 10 pulsos, verificación de excepciones de cámara y prueba de bucle óptico."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 2,
            blockNumber = 1,
            title = "Fase 2: Bóveda Shamir Secret Sharing (K-de-N)",
            operationalScope = "División polinomial de claves criptográficas y contraseñas de autodestrucción entre nodos de patrulla.",
            category = "CRIPTOGRAFÍA & SEGURIDAD",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Criptográfico", "Capa 5: Zero-Trust"),
            domainModule = "ShamirSecretSharingVault",
            sourceFiles = listOf("domain/security/ShamirSecretSharingVault.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/2/execute", "CLI: subsystem 2 '{\"secret\":\"KEY\",\"k\":3,\"n\":5}'"),
            implementationModes = "Aritmética exacta de cuerpos finitos de Galois GF(2^8) con polinomio generador x^8 + x^4 + x^3 + x + 1 (0x11B) e interpolación de Lagrange.",
            researchAndStandards = "Adi Shamir (1979) 'How to Share a Secret', Communications of the ACM 22 (11), FIPS 140-3 Threshold Cryptography.",
            hardwareAndSensors = listOf("SecureRandom CSPRNG", "Acelerador Criptográfico de CPU ARM Neon"),
            zeroTrustAndFaultTolerance = "K-1 fragmentos revelan cero información matemática sobre el secreto original. Resistencia a compromiso de nodos individuales.",
            futureProjectionsAndRoadmap = "Integración de firmas de umbral BLS y esquemas de compartición verificable de secretos (VSS Feldman).",
            verificationMethods = "Pruebas unitarias de división en 3-de-5, 2-de-3 y verificación de fallos al intentar reconstruir con K-1 fragmentos."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 3,
            blockNumber = 1,
            title = "Fase 3: Clasificador Acústico de Disparos & Impactos",
            operationalScope = "Procesamiento de señal de audio en tiempo real para detección de onda de choque supersónica y detonación de boca.",
            category = "INTELIGENCIA & DSP",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio DSP", "Capa 7: HAL AudioRecord"),
            domainModule = "AcousticShotClassifier",
            sourceFiles = listOf("domain/media/AcousticShotClassifier.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/3/execute", "CLI: subsystem 3 '{\"caliber\":\"7.62x39mm\"}'"),
            implementationModes = "Buffer circular de audio PCM de 44.1 kHz, análisis de derivada temporal de energía acústica (>18 dB en <2 ms) y correlación de retardo choque-boca.",
            researchAndStandards = "STANAG 4154 (Acoustic Fire Detection), Boomerang Acoustic Detection System Specs, MIL-STD-1474E.",
            hardwareAndSensors = listOf("Micrófono MEMS", "AudioRecord 44.1 kHz 16-bit PCM Mono"),
            zeroTrustAndFaultTolerance = "Procesamiento local 100% en memoria volátil; los buffers de audio se sobreescriben en ciclos de 500 ms sin tocar disco.",
            futureProjectionsAndRoadmap = "Arreglo de micrófonos múltiples para estimación de azimut de tiro (TDOA / Cross-Correlation) y modelos TFLite de firmas de armas.",
            verificationMethods = "Inyección de señales de choque sintéticas de calibres 5.56x45mm, 7.62x39mm y .50 BMG con validación de umbrales."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 4,
            blockNumber = 1,
            title = "Fase 4: Telemetría Biológica & Combat Readiness Score",
            operationalScope = "Monitoreo fisiológico del operador (BPM, HRV, estrés térmico) y cálculo de índice de fatiga operativa.",
            category = "BIOMEDICINA & SENSORES",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio", "Capa 7: Sensores"),
            domainModule = "PhysioBioTelemetryManager",
            sourceFiles = listOf("domain/sensors/PhysioBioTelemetryManager.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("GET /api/v1/sensors/live", "POST /api/v1/subsystems/4/execute", "CLI: sensors"),
            implementationModes = "Algoritmo de media móvil exponencial ponderada de frecuencia cardíaca, cálculo de HRV RMSSD y matriz de fatiga acumulada de combate.",
            researchAndStandards = "US Army Research Institute of Environmental Medicine (USARIEM) Heat Strain Index, NATO Medical Standards (STANAG 2122).",
            hardwareAndSensors = listOf("Sensor óptico de pulso PPG / BLE Heart Rate Profile", "Sensor de temperatura térmica de batería/dispositivo"),
            zeroTrustAndFaultTolerance = "Filtrado de artefactos de movimiento mediante filtro Kalman unidimensional y persistencia cifrada.",
            futureProjectionsAndRoadmap = "Conectividad ANT+ para bandas pectorales Garmin/Polar y predicción de golpe de calor por aprendizaje automático local.",
            verificationMethods = "Simulación de estrés en combate (40 a 195 BPM) con comprobación de umbrales de alerta crítica."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 5,
            blockNumber = 1,
            title = "Fase 5: Protocolo Cero-Huella ZEROIZE (DoD 5220.22-M)",
            operationalScope = "Borrado criptográfico irreversible de claves, base de datos SQLite y memoria volátil en caso de captura.",
            category = "CONTRA-INTELIGENCIA",
            architectureLayers = listOf("Capa 1: UI", "Capa 5: Zero-Trust", "Capa 6: Persistencia", "Capa 7: HAL"),
            domainModule = "EmergencyZeroizeManager",
            sourceFiles = listOf("domain/security/EmergencyZeroizeManager.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/security/zeroize", "CLI: zeroize confirm"),
            implementationModes = "Sobreescritura directa de sectores de base de datos en 3 pasadas: 1ª con 0x00, 2ª con 0xFF, 3ª con CSPRNG aleatorio, destrucción de KeyStore y System.exit.",
            researchAndStandards = "DoD 5220.22-M (National Industrial Security Program Operating Manual), NIST SP 800-88 Rev. 1 Guidelines for Media Sanitization.",
            hardwareAndSensors = listOf("Android KeyStore Hardware TPM/TEE", "Controlador de Almacenamiento Flash UFS/eMMC"),
            zeroTrustAndFaultTolerance = "Ejecución síncrona atómica bloqueante; no puede ser cancelada una vez iniciada la fase de sobreescritura.",
            futureProjectionsAndRoadmap = "Disparador automático por trampa de hombre muerto (Dead-Man Trigger) si no se introduce código de vida en X horas.",
            verificationMethods = "Pruebas de sobreescritura en archivos de prueba en sandbox y verificación forense de ceros y entropía aleatoria."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 6,
            blockNumber = 2,
            title = "Fase 6: Navegación Inercial a Estima (PDR / Dead Reckoning)",
            operationalScope = "Seguimiento de posición sin GPS mediante acelerómetro, giróscopo, detección de pasos y estimación de rumbo.",
            category = "NAVEGACIÓN & SENSORES",
            architectureLayers = listOf("Capa 1: UI / Radar", "Capa 3: Dominio Inercial", "Capa 7: Sensores HAL"),
            domainModule = "PedestrianDeadReckoningEngine",
            sourceFiles = listOf("domain/sensors/PedestrianDeadReckoningEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("GET /api/v1/sensors/live", "POST /api/v1/subsystems/6/execute", "CLI: pdr steps [n]"),
            implementationModes = "Filtro de detección de cruce por cero en norma de aceleración tridimensional, cálculo de longitud de zancada de Weinberg y matriz de rotación de cuaterniones.",
            researchAndStandards = "IEEE Transactions on Aerospace and Electronic Systems (Pedestrian Navigation), STANAG 4572 (PNT Resilience).",
            hardwareAndSensors = listOf("Sensor.TYPE_ACCELEROMETER", "Sensor.TYPE_GYROSCOPE", "Sensor.TYPE_ROTATION_VECTOR"),
            zeroTrustAndFaultTolerance = "Operación 100% pasiva; cero señales emitidas al exterior. Inmune a jamming y spoofing de satélites GPS/GLONASS.",
            futureProjectionsAndRoadmap = "Fusión con altitud barométrica para navegación 3D en escaleras y pisos de edificios.",
            verificationMethods = "Inyección de patrones de marcha de 100 pasos y verificación de distancia acumulada con error < 3%."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 7,
            blockNumber = 2,
            title = "Fase 7: Salto de Frecuencia Virtual FHSS Mesh",
            operationalScope = "Micro-balizas con saltos de canal pseudo-aleatorios coordinados por semilla criptográfica compartida.",
            category = "GUERRA ELECTRÓNICA & EW",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio RF", "Capa 4: Malla P2P"),
            domainModule = "FrequencyHoppingMeshEngine",
            sourceFiles = listOf("domain/p2p/FrequencyHoppingMeshEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/7/execute", "CLI: subsystem 7"),
            implementationModes = "Generador de permutación pseudo-aleatoria criptográfica SHA-256 coordinada por ranura de tiempo Epoch y semilla de clave de misión.",
            researchAndStandards = "MIL-STD-188-141D (Frequency Hopping), Have Quick II Tactical Air Communications Standard.",
            hardwareAndSensors = listOf("Temporizador de microsegundos de CPU", "Malla UDP Sockets / Bluetooth RFCOMM"),
            zeroTrustAndFaultTolerance = "Si un canal es interferido por jamming deliberado, la ráfaga salta automáticamente al siguiente slot en menos de 100 ms.",
            futureProjectionsAndRoadmap = "Integración con transceptores de radio definidos por software (SDR) externos mediante USB-OTG.",
            verificationMethods = "Verificación de coincidencia de secuencias de salto entre nodos emisores y receptores en 10,000 ranuras consecutivas."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 8,
            blockNumber = 2,
            title = "Fase 8: Tablero de Simbología OTAN APP-6 / MIL-STD-2525D",
            operationalScope = "Estandarización visual de unidades amigas, hostiles, neutrales y desconocidas sobre la malla táctica.",
            category = "C4ISR & MANDO",
            architectureLayers = listOf("Capa 1: UI Canvas", "Capa 3: Modelos"),
            domainModule = "NatoSymbologyRegistry",
            sourceFiles = listOf("domain/models/NatoSymbologyOverlay.kt", "ui/screens/RadarScreen.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/8/execute", "CLI: subsystem 8"),
            implementationModes = "Codificación de jerarquía táctica de 15 caracteres (SIDC - Symbol Identification Code) con renderizado vectorial de marcos y modificadores de escalón.",
            researchAndStandards = "NATO APP-6D Joint Military Symbology, US DoD MIL-STD-2525D.",
            hardwareAndSensors = listOf("GPU Render Engine (Jetpack Compose Canvas)"),
            zeroTrustAndFaultTolerance = "Conversión a JSON compacto de 24 bytes para transmisión a través de la malla en canales saturados.",
            futureProjectionsAndRoadmap = "Soporte de simbología tridimensional para capas aéreas y subterráneas.",
            verificationMethods = "Renderizado de 50 marcadores tácticos simultáneos a 60 FPS estables."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 9,
            blockNumber = 2,
            title = "Fase 9: Agente Táctico Autónomo de Misión (Offline COA)",
            operationalScope = "Motor de razonamiento heurístico local para sugerir cursos de acción basados en telemetría de entorno.",
            category = "INTELIGENCIA ARTIFICIAL",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio IA"),
            domainModule = "TacticalMissionAgent",
            sourceFiles = listOf("domain/ai/TacticalMissionAgent.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/9/execute", "CLI: subsystem 9"),
            implementationModes = "Motor de inferencia de reglas de decisión táctica y evaluación de árboles de amenazas ponderados según proximidad y huella RF.",
            researchAndStandards = "US Army FM 6-0 Commander and Staff Organization and Operations (MDMP - Military Decision Making Process).",
            hardwareAndSensors = listOf("CPU Multi-Core Engine"),
            zeroTrustAndFaultTolerance = "Ejecución 100% offline sin dependencias de servidores en la nube.",
            futureProjectionsAndRoadmap = "Modelos SLM (Small Language Models) cuantizados ejecutados en la NPU del dispositivo móvil.",
            verificationMethods = "Evaluación de 12 escenarios de emboscada, baja y cerco con generación de directivas en <10 ms."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 10,
            blockNumber = 2,
            title = "Fase 10: Compresor Binario Ultradenso Huffman / CBOR",
            operationalScope = "Compresión extrema de telemetría y mensajes para transmisiones en canales de muy baja tasa de datos.",
            category = "OPTIMIZACIÓN DE MALLA",
            architectureLayers = listOf("Capa 3: Dominio", "Capa 4: Red P2P"),
            domainModule = "TacticalBinaryCompressor",
            sourceFiles = listOf("domain/p2p/TacticalBinaryCompressor.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/10/execute", "CLI: subsystem 10 '{\"text\":\"SAMPLE\"}'"),
            implementationModes = "Árbol de frecuencias de símbolos con codificación por longitud variable de bits y empaquetado a nivel de nibble/byte.",
            researchAndStandards = "RFC 8949 (Concise Binary Object Representation - CBOR), Huffman Coding Algorithms.",
            hardwareAndSensors = listOf("Bitwise ALU"),
            zeroTrustAndFaultTolerance = "Verificación de suma de comprobación CRC32 tras descompresión para garantizar integridad binaria.",
            futureProjectionsAndRoadmap = "Diccionarios estáticos pre-compartidos de vocabulario táctico militar para ratios de compresión > 80%.",
            verificationMethods = "Pruebas con streams de telemetría y mensajes de chat con ratios de compresión medidos de 35% a 65%."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 11,
            blockNumber = 3,
            title = "Fase 11: Barómetro Táctico & Alerta de Tormenta",
            operationalScope = "Monitoreo barométrico de micro-variaciones de presión (hPa) y detección de frentes de tormenta severa.",
            category = "SENSORES DE CAMPO",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Sensores", "Capa 7: HAL Barómetro"),
            domainModule = "BarometerStormAlertEngine",
            sourceFiles = listOf("domain/sensors/BarometerStormAlertEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("GET /api/v1/sensors/live", "POST /api/v1/subsystems/11/execute", "CLI: sensors"),
            implementationModes = "Muestreo a intervalos continuos, cálculo de tendencia dP/dt por regresión lineal y fórmula barométrica hipsométrica internacional.",
            researchAndStandards = "ICAO Standard Atmosphere Doc 7488, WMO Guide to Meteorological Instruments and Methods of Observation.",
            hardwareAndSensors = listOf("Sensor.TYPE_PRESSURE (Barómetro MEMS)"),
            zeroTrustAndFaultTolerance = "Filtro de mediana de 5 muestras para descartar picos por cierre de puertas o impactos de viento.",
            futureProjectionsAndRoadmap = "Estimación de altura de piso en interiores de estructuras de combate urbano (CQB).",
            verificationMethods = "Simulación de caída rápida de 4 hPa en 3 horas con activación comprobada de alarma de tormenta."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 12,
            blockNumber = 3,
            title = "Fase 12: Enlace Acústico Sub-audible (18-20 kHz)",
            operationalScope = "Modulación acústica ultra-alta Data-Over-Sound para intercambio seguro en total silencio de radiofrecuencia.",
            category = "COMUNICACIÓN COVERT",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio DSP", "Capa 7: Audio HAL"),
            domainModule = "UltrasonicDataLinkTransceiver",
            sourceFiles = listOf("domain/audio/UltrasonicDataLinkTransceiver.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/12/execute", "CLI: subsystem 12"),
            implementationModes = "Modulación BFSK (Binary Frequency Shift Keying) en portadoras de 18.5 kHz (bit 0) y 19.5 kHz (bit 1) con detección Goertzel.",
            researchAndStandards = "Acoustic Data Communication Standards, Inaudible Sound Signaling Protocols.",
            hardwareAndSensors = listOf("Altavoz del dispositivo (44.1 kHz DAC)", "Micrófono (44.1 kHz ADC)"),
            zeroTrustAndFaultTolerance = "Inaudible para el oído humano adulto; indetectable por analizadores de espectro RF o receptores SDR.",
            futureProjectionsAndRoadmap = "Modulación OFDM acústica para alcanzar tasas de datos de hasta 1.2 kbps en distancias cortas.",
            verificationMethods = "Transmisión y recepción en bucle cerrado de paquetes de 32 bytes con 100% de coherencia."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 13,
            blockNumber = 3,
            title = "Fase 13: Perímetro Geofence 3D (Safe Bubble)",
            operationalScope = "Burbuja de protección perimetral 3D con radar esférico y alertas automáticas de proximidad hostil.",
            category = "SEGURIDAD DE BASE",
            architectureLayers = listOf("Capa 1: UI Radar", "Capa 3: Dominio Geométrico"),
            domainModule = "SafeBubble3DProximityRadar",
            sourceFiles = listOf("domain/sensors/SafeBubble3DProximityRadar.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/13/execute", "CLI: subsystem 13"),
            implementationModes = "Cálculo de distancia esférica geodésica mediante Haversine combinado con diferencial de altitud z en espacio euclidiano.",
            researchAndStandards = "MIL-STD-1425 (Perimeter Defense), NATO STANAG 4586 (Geofencing).",
            hardwareAndSensors = listOf("Sensor Fusion Engine (GPS + PDR + Barómetro)"),
            zeroTrustAndFaultTolerance = "Disparo de alertas prioritarias locales inmediatas con vibración y tono táctico sin requerir internet.",
            futureProjectionsAndRoadmap = "Geocercas poligonales complejas con soporte de corredores de paso seguros (Safe Lanes).",
            verificationMethods = "Pruebas de incursión simulada a 120m, 50m y 15m con activación de estados INFO, WARNING y CRITICAL."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 14,
            blockNumber = 3,
            title = "Fase 14: Calculadora Balística Táctica (Rifleman)",
            operationalScope = "Cálculo de corrección de elevación MRAD/MOA, deriva de viento y corrección cosenoidal de ángulo de tiro.",
            category = "BALÍSTICA TÁCTICA",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Físico"),
            domainModule = "TacticalBallisticsCalculator",
            sourceFiles = listOf("domain/sensors/TacticalBallisticsCalculator.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/14/execute", "CLI: subsystem 14 '{\"distanceMeters\":600}'"),
            implementationModes = "Integración numérica del modelo de resistencia de arrastre balístico G1 y corrección por densidad de aire (temperatura + presión).",
            researchAndStandards = "US Army Ballistic Research Laboratory (BRL) Drag Functions, NATO STANAG 4355.",
            hardwareAndSensors = listOf("Barómetro MEMS", "Giróscopo / Acelerómetro para ángulo de inclinación"),
            zeroTrustAndFaultTolerance = "Cálculos matemáticos puros offline ejecutados en punto flotante de doble precisión (Double).",
            futureProjectionsAndRoadmap = "Modelos de arrastre G7 para proyectiles de francotirador de muy bajo arrastre (VLD) y corrección de efecto Coriolis.",
            verificationMethods = "Comparación de tablas balísticas para calibres 5.56 NATO, 7.62x51mm y .300 Win Mag con desviación < 0.1 MRAD."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 15,
            blockNumber = 3,
            title = "Fase 15: Red de Señuelos RF & Nodos Fantasma Honeypot",
            operationalScope = "Emisión de identidades y señales falsas para despistar radiogoniometría y análisis de tráfico SIGINT hostil.",
            category = "SIGINT & ENGAÑO TÁCTICO",
            architectureLayers = listOf("Capa 1: UI", "Capa 4: Red Malla", "Capa 5: Zero-Trust"),
            domainModule = "MeshHoneypotGenerator",
            sourceFiles = listOf("domain/security/MeshHoneypotGenerator.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/15/execute", "CLI: honeypot [count]"),
            implementationModes = "Inyección periódica de balizas UDP y paquetes BLE con direcciones MAC e IDs sintéticos para simular un pelotón entero.",
            researchAndStandards = "US Joint Publication 3-13.4 Military Deception (MILDEC), NATO Strategic Communications Doctrine.",
            hardwareAndSensors = listOf("Wi-Fi / Bluetooth Transceiver"),
            zeroTrustAndFaultTolerance = "Aislamiento estricto; los señuelos no comparten ni exponen las claves de cifrado del nodo real.",
            futureProjectionsAndRoadmap = "Generación de tráfico de radiofrecuencia con patrones circadianos realistas para engaño prolongado.",
            verificationMethods = "Verificación de emisión de 5 nodos fantasma con firmas distintas y registro en colector de logs."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 16,
            blockNumber = 4,
            title = "Fase 16: Detector Forense de Ataques Sybil",
            operationalScope = "Inspección forense de capas PHY/MAC para descubrir clones de identidad y spoofing de firmas RF.",
            category = "CIBERSEGURIDAD ZERO-TRUST",
            architectureLayers = listOf("Capa 1: UI", "Capa 4: Red", "Capa 5: Zero-Trust"),
            domainModule = "SybilMeshDetector",
            sourceFiles = listOf("domain/security/SybilMeshDetector.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/16/execute", "CLI: subsystem 16"),
            implementationModes = "Análisis estadístico de correlación RSSI, dispersión espacial de señales recibidas e inspección de números de secuencia de trama.",
            researchAndStandards = "Douceur (2002) 'The Sybil Attack', IEEE Security & Privacy on Wireless Mesh Security.",
            hardwareAndSensors = listOf("Radio RSSI Monitor"),
            zeroTrustAndFaultTolerance = "Aislamiento y lista negra automática de nodos identificados como clones hostiles.",
            futureProjectionsAndRoadmap = "Huellas de radiofrecuencia (RF Fingerprinting) basadas en imperfecciones de reloj de hardware.",
            verificationMethods = "Simulación de dos identidades con mismo ID pero distinta IP/RSSI con detección y aislamiento automático."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 17,
            blockNumber = 4,
            title = "Fase 17: Bóveda Post-Cuántica ML-KEM Kyber-768",
            operationalScope = "Criptografía híbrida NIST post-cuántica ML-KEM / Kyber-768 resistente a ataques de computación cuántica.",
            category = "CRIPTOGRAFÍA POST-CUÁNTICA",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Criptografía PQC", "Capa 5: Zero-Trust"),
            domainModule = "PostQuantumKyberVault",
            sourceFiles = listOf("domain/security/PostQuantumKyberVault.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/17/execute", "CLI: kyber"),
            implementationModes = "Esquema KEM basado en retículos en módulo Learning With Errors (Module-LWE) combinado con intercambio de claves elípticas Curve25519.",
            researchAndStandards = "NIST FIPS 203 (Module-Lattice-Based Key-Encapsulation Mechanism Standard), NSA CNSA 2.0 Suite.",
            hardwareAndSensors = listOf("CSPRNG SecureRandom", "Motor Polinomial R_q"),
            zeroTrustAndFaultTolerance = "Protección híbrida: para comprometer la sesión, un atacante debe romper tanto la criptografía cuántica como la clásica.",
            futureProjectionsAndRoadmap = "Implementación de firmas digitales post-cuánticas ML-DSA (Dilithium / NIST FIPS 204).",
            verificationMethods = "Generación de pares de llaves y prueba de encapsulación/desencapsulación con verificación de hash SHA-256."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 18,
            blockNumber = 4,
            title = "Fase 18: Micro-Blockchain Táctica de Misión",
            operationalScope = "Libro mayor inmutable Proof-of-Authority para registro estricto de órdenes de misión y bajas.",
            category = "INTEGRIDAD & FORENSE",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Integridad", "Capa 5: Zero-Trust"),
            domainModule = "TacticalMissionBlockchain",
            sourceFiles = listOf("domain/security/TacticalMissionBlockchain.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/18/execute", "CLI: blockchain"),
            implementationModes = "Estructura de bloques encadenados por hashes criptográficos SHA-256 con consenso Proof-of-Authority y firmas del comandante.",
            researchAndStandards = "NIST SP 800-140 (Digital Signatures and Blockchains), Defense Advanced Research Projects Agency (DARPA) Distributed Ledger Specs.",
            hardwareAndSensors = listOf("SHA-256 Hardware Accelerator"),
            zeroTrustAndFaultTolerance = "Imposible alterar órdenes pasadas sin invalidar toda la cadena de bloques subsiguiente.",
            futureProjectionsAndRoadmap = "Árboles de Merkle compactos para sincronización eficiente de ramas entre patrullas aisladas.",
            verificationMethods = "Minado de 10 bloques consecutivos y prueba de detección de intento de manipulación en el bloque 3."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 19,
            blockNumber = 4,
            title = "Fase 19: Medidor de Huella Electromagnética LPI/LPD",
            operationalScope = "Monitoreo y optimización de baja probabilidad de intercepción (LPI) y detección para evadir radiogoniometría.",
            category = "GUERRA ELECTRÓNICA & EW",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio RF", "Capa 5: Sigilo"),
            domainModule = "RfEmissionSignatureMeter",
            sourceFiles = listOf("domain/security/RfEmissionSignatureMeter.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/19/execute", "CLI: subsystem 19"),
            implementationModes = "Cálculo de densidad espectral de potencia y ciclo de trabajo (Duty Cycle) acumulado en el tiempo para estimar probabilidad de detección hostil.",
            researchAndStandards = "US Air Force Electronic Warfare Principles (AFDP 3-13), NATO EW Standards (AJP-3.6).",
            hardwareAndSensors = listOf("RF Tx State Monitor"),
            zeroTrustAndFaultTolerance = "Alerta automática al operador si el ciclo de transmisión continuo excede umbrales seguros.",
            futureProjectionsAndRoadmap = "Modulación adaptativa automática de potencia de salida (ATPC) en función de la cercanía del nodo receptor.",
            verificationMethods = "Prueba de evaluación de firma con cálculo de índice de sigilo en modos activo, silencioso y bajo ráfaga."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 20,
            blockNumber = 4,
            title = "Fase 20: Control de Emisiones EMCON Alpha",
            operationalScope = "Silencio de radio absoluto por software en zonas de alto riesgo de radiolocalización enemiga.",
            category = "SIGILO & DISCIPLINA RF",
            architectureLayers = listOf("Capa 1: UI", "Capa 4: Red", "Capa 5: Zero-Trust"),
            domainModule = "EmconAlphaManager",
            sourceFiles = listOf("domain/security/EmconAlphaManager.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/security/emcon", "CLI: emcon [alpha|bravo|charlie|off]"),
            implementationModes = "Supresión de todas las rutinas de transmisión (TX) a nivel de código de socket, manteniendo receptores pasivos (RX) o apagado total.",
            researchAndStandards = "NATO EMCON Policy Standards, US Navy Emission Control Procedures.",
            hardwareAndSensors = listOf("Network Interface Interceptors"),
            zeroTrustAndFaultTolerance = "Bloqueo preventivo en la capa más baja de envío; ningún paquete sale del dispositivo en nivel ALPHA.",
            futureProjectionsAndRoadmap = "Activación automática de EMCON por proximidad a balizas hostiles o zonas de riesgo cartografiadas.",
            verificationMethods = "Conmutación de niveles Alpha, Bravo y Charlie comprobando el bloqueo efectivo de paquetes de prueba."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 21,
            blockNumber = 5,
            title = "Fase 21: Mapeador Fotogramétrico Aéreo 2D",
            operationalScope = "Generación offline de ortomosaicos y georreferenciación de imágenes de reconocimiento aéreo.",
            category = "RECONOCIMIENTO & GIS",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Fotogrametría"),
            domainModule = "TacticalPhotogrammetryEngine",
            sourceFiles = listOf("domain/sensors/TacticalPhotogrammetryEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/21/execute", "CLI: subsystem 21"),
            implementationModes = "Cálculo de resolución Ground Sample Distance (GSD), solapamiento longitudinal/transversal (60-80%) y alineación por matriz homográfica.",
            researchAndStandards = "USGS Photogrammetry Guidelines, ASPRS Accuracy Standards for Digital Geospatial Data.",
            hardwareAndSensors = listOf("Cámara de alta resolución", "Metadata EXIF (GPS + Altitud + Roll/Pitch)"),
            zeroTrustAndFaultTolerance = "Procesamiento local sin enviar fotogramas a la nube ni filtrar coordenadas sensibles.",
            futureProjectionsAndRoadmap = "Generación de modelos de elevación digital 3D (DEM) mediante Structure from Motion (SfM) acelerado por GPU.",
            verificationMethods = "Cálculo de GSD para fotogramas a 50m y 120m de altura de vuelo con solapamiento simulado."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 22,
            blockNumber = 5,
            title = "Fase 22: Transceptor Telemetría MAVLink UAV ISR",
            operationalScope = "Recepción y control de telemetría de drones de reconocimiento ISR en tiempo real vía MAVLink v2.",
            category = "UAV & ROBÓTICA TÁCTICA",
            architectureLayers = listOf("Capa 1: UI Radar", "Capa 3: Dominio Robótica", "Capa 4: Red"),
            domainModule = "MavlinkTelemetryTransceiver",
            sourceFiles = listOf("domain/sensors/MavlinkTelemetryTransceiver.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/uav/command", "POST /api/v1/subsystems/22/execute", "CLI: uav [rtl|arm|disarm|loiter]"),
            implementationModes = "Decodificador de tramas binarias MAVLink v2 (mensajes HEARTBEAT #0, GLOBAL_POSITION_INT #33, SYS_STATUS #1) y emisor de comandos MAV_CMD.",
            researchAndStandards = "MAVLink v2 Micro Air Vehicle Protocol, STANAG 4586 (Standard Interfaces of UAV Control System).",
            hardwareAndSensors = listOf("Módulo de Telemetría 433/915 MHz USB-OTG o Wi-Fi UDP Bridge"),
            zeroTrustAndFaultTolerance = "Checksum CRC-16 MAVLink extra para prevenir corrupción de tramas de control de vuelo.",
            futureProjectionsAndRoadmap = "Control de cardán de cámara (Gimbal) y seguimiento autónomo de objetivos terrestres (Auto-Track).",
            verificationMethods = "Recepción de stream sintético de telemetría y emisión validada de comando de retorno a base (RTL)."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 23,
            blockNumber = 5,
            title = "Fase 23: Triaje Táctico 9-Line MEDEVAC & TCCC",
            operationalScope = "Formulario estandarizado de evacuación médica y registro de tiempos críticos de torniquete TCCC.",
            category = "MEDICINA DE COMBATE",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Médico"),
            domainModule = "TacticalMedevacEngine",
            sourceFiles = listOf("domain/sensors/TacticalMedevacEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/c4isr/medevac", "POST /api/v1/subsystems/23/execute", "CLI: medevac [lz]"),
            implementationModes = "Generación estructurada del mensaje estándar de 9 líneas de la OTAN y temporizador de alerta para evitar isquemia irreversible en extremidades.",
            researchAndStandards = "NATO STANAG 2082 Medical Evacuation, Tactical Combat Casualty Care (TCCC) Guidelines.",
            hardwareAndSensors = listOf("Reloj de sistema / Cronómetro de precisión"),
            zeroTrustAndFaultTolerance = "Difusión inmediata de alta prioridad sobre todos los canales de la malla disponibles.",
            futureProjectionsAndRoadmap = "Transmisión automática de signos vitales (SpO2, Pulso, Presión) en el cuerpo del mensaje 9-Line.",
            verificationMethods = "Generación de solicitud MEDEVAC completa con validación de campos obligatorios y formato radiofónico."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 24,
            blockNumber = 5,
            title = "Fase 24: Brújula Solar & Efemérides Astronómicas",
            operationalScope = "Cálculo del Norte Verdadero mediante azimut solar astronómico ante bloqueo o jamming de GNSS/Brújula.",
            category = "NAVEGACIÓN ASTRONÓMICA",
            architectureLayers = listOf("Capa 1: UI Radar", "Capa 3: Dominio Astronómico"),
            domainModule = "SolarEphemerisCompass",
            sourceFiles = listOf("domain/sensors/SolarEphemerisCompass.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("GET /api/v1/sensors/live", "POST /api/v1/subsystems/24/execute", "CLI: sensors"),
            implementationModes = "Resolución de ecuaciones solares astronómicas: declinación solar, ecuación del tiempo, ángulo horario y cálculo de azimut/cenit solar exactos.",
            researchAndStandards = "US Naval Observatory (USNO) Astronomical Almanac, NREL Solar Position Algorithm (SPA).",
            hardwareAndSensors = listOf("Reloj UTC de precisión", "Orientación angular de cámara o sombra"),
            zeroTrustAndFaultTolerance = "Inmune a interferencias magnéticas locales (búnkeres de hormigón armado o vehículos blindados).",
            futureProjectionsAndRoadmap = "Navegación nocturna por efemérides lunares y alineación con la Estrella Polar (Polaris) / Cruz del Sur.",
            verificationMethods = "Comparación de azimut calculado con efemérides oficiales para distintas coordenadas y fechas del año."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 25,
            blockNumber = 5,
            title = "Fase 25: Pasarela Satelital SATCOM DTN Gateway",
            operationalScope = "Detección de ventanas de paso de satélites LEO y vaciado oportuno de paquetes DTN en tránsito.",
            category = "SATCOM & DTN",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Satelital", "Capa 4: Red DTN"),
            domainModule = "SatelliteMeshGateway",
            sourceFiles = listOf("domain/sensors/SatelliteMeshGateway.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/25/execute", "CLI: subsystem 25"),
            implementationModes = "Propagador de órbitas SGP4 con elementos de dos líneas (TLE) para predecir elevación (>15°) y azimut de constelaciones LEO.",
            researchAndStandards = "CCSDS 734.2-B-1 (Delay-Tolerant Networking Bundle Protocol), Iridium SBD Protocols.",
            hardwareAndSensors = listOf("Módulo transceptor satelital (Iridium 9603 / RockBLOCK USB)"),
            zeroTrustAndFaultTolerance = "Almacenamiento persistente en cola cifrada hasta la apertura confirmada de la ventana satelital.",
            futureProjectionsAndRoadmap = "Descarga de ráfagas ultrarrápidas con modulación adaptativa al enlace satelital disponible.",
            verificationMethods = "Cálculo de 3 ventanas de pase simuladas con cuenta regresiva y confirmación de vaciado de cola."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 26,
            blockNumber = 6,
            title = "Fase 26: Enmascarador Acústico Espectral de Voz",
            operationalScope = "Inversión espectral de frecuencia (3.3 kHz) y análisis FFT en tiempo real para evitar interceptación de voz.",
            category = "SEGURIDAD DE VOZ & DSP",
            architectureLayers = listOf("Capa 1: UI FFT", "Capa 3: Dominio Audio DSP", "Capa 7: Audio HAL"),
            domainModule = "TacticalVoiceSpectralEngine",
            sourceFiles = listOf("domain/c4isr/TacticalVoiceSpectralEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/26/execute", "CLI: voice_spectral"),
            implementationModes = "Heterodinaje y multiplicación digital por portadora de inversión (f_inv = 3.3 kHz) con filtro pasa-bajos Chebyshev para eliminar bandas laterales superiores.",
            researchAndStandards = "MIL-STD-188-110 (Tactical Voice Encryption), Secure Tactical Analog Communications Protocols.",
            hardwareAndSensors = listOf("AudioRecord 44.1 kHz", "AudioTrack Real-time Stream"),
            zeroTrustAndFaultTolerance = "Ininteligible para el oído de un espía o receptor no autorizado; sólo el nodo par con la misma portadora puede decodificar.",
            futureProjectionsAndRoadmap = "Inversor multibanda por división de sub-bandas de frecuencia (Split-Band Rolling Scrambler).",
            verificationMethods = "Prueba de inversión y desinversión en bucle con cálculo de inteligibilidad espectral."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 27,
            blockNumber = 6,
            title = "Fase 27: Enrutador Táctico de Escape A* (E&E)",
            operationalScope = "Cálculo heurístico de rutas óptimas de escape y evasión de áreas de peligro u hostiles sin conexión.",
            category = "PLANIFICACIÓN & NAVEGACIÓN",
            architectureLayers = listOf("Capa 1: UI Radar", "Capa 3: Dominio Heurístico"),
            domainModule = "TacticalAStarRouter",
            sourceFiles = listOf("domain/c4isr/TacticalAStarRouter.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/c4isr/a_star_route", "POST /api/v1/subsystems/27/execute", "CLI: a_star"),
            implementationModes = "Algoritmo de búsqueda de grafos A* en rejilla bidimensional con distancia de Manhattan/Euclidiana y función de penalización por proximidad a amenazas.",
            researchAndStandards = "Hart, Nilsson & Raphael (1968) A* Algorithm, US Army FM 3-50.1 Army Evasion and Recovery Operations.",
            hardwareAndSensors = listOf("CPU Grid Processor"),
            zeroTrustAndFaultTolerance = "Cálculo puramente local; no revela el destino de escape a ningún servidor externo.",
            futureProjectionsAndRoadmap = "Ponderación tridimensional incorporando curvas de nivel topográficas y áreas con cobertura vegetal.",
            verificationMethods = "Cálculo de rutas con y sin zonas hostiles intermedias verificando el rodeo automático del peligro."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 28,
            blockNumber = 6,
            title = "Fase 28: Generador de Informes de Inteligencia SALUTE",
            operationalScope = "Estructuración y difusión de reportes de inteligencia militar bajo estándar SALUTE sobre la red Mesh.",
            category = "INTELIGENCIA MILITAR",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Intel", "Capa 4: Red Malla"),
            domainModule = "SaluteIntelReportEngine",
            sourceFiles = listOf("domain/c4isr/SaluteIntelReportEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/c4isr/salute", "POST /api/v1/subsystems/28/execute", "CLI: salute [size] [activity] [loc]"),
            implementationModes = "Formateo estándar de Size (Tamaño), Activity (Actividad), Location (Ubicación), Unit (Unidad), Time (Hora), Equipment (Equipamiento) con firma criptográfica.",
            researchAndStandards = "US Army FM 2-0 Intelligence Operations, NATO STANAG 2014 Intelligence Reports.",
            hardwareAndSensors = listOf("Almacenamiento SQLite", "Malla UDP / BLE"),
            zeroTrustAndFaultTolerance = "Sello temporal inmutable con clave del observador para evitar reportes falsificados.",
            futureProjectionsAndRoadmap = "Adjuntos de imágenes térmicas y espectrogramas acústicos embebidos en el reporte SALUTE.",
            verificationMethods = "Generación y emisión de reporte de inteligencia con verificación del formato resultante."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 29,
            blockNumber = 6,
            title = "Fase 29: Sincronizador de Reloj Atómico PPS en Malla",
            operationalScope = "Sincronización temporal sub-microsegundo para saltos FHSS coordinados y sellado criptográfico unificado.",
            category = "TEMPORIZACIÓN & SYNC",
            architectureLayers = listOf("Capa 1: UI", "Capa 3: Dominio Temporal", "Capa 4: Red"),
            domainModule = "MeshTimeSynchronizer",
            sourceFiles = listOf("domain/c4isr/MeshTimeSynchronizer.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("POST /api/v1/subsystems/29/execute", "CLI: time_sync"),
            implementationModes = "Protocolo de sincronización de tiempo de precisión (tipo IEEE 1588 PTP / Cristian's Algorithm) con cálculo de retardo de ida y vuelta (RTT) y compensación de drift.",
            researchAndStandards = "IEEE 1588-2019 (Precision Clock Synchronization Protocol), ITU-T G.8275.",
            hardwareAndSensors = listOf("System.nanoTime Clock Hardware"),
            zeroTrustAndFaultTolerance = "Filtro de media recortada para descartar nodos con reloj descalibrado o malicioso.",
            futureProjectionsAndRoadmap = "Sincronización óptica por impulsos Li-Fi para entornos con radiofrecuencia prohibida.",
            verificationMethods = "Prueba de estimación de deriva de reloj con 20 pulsos de sincronización y convergencia en nanosegundos."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 30,
            blockNumber = 6,
            title = "Fase 30: Consola Maestra C4ISR Multidominio",
            operationalScope = "Centro unificado de mando y control con supervisión de los 31 subsistemas tácticos de OmniComm.",
            category = "MANDO MAESTRO & C4ISR",
            architectureLayers = listOf("Capa 1: UI C4ISR", "Capa 2: ViewModels", "Capa 3: Agregador"),
            domainModule = "C4IsrMasterDashboardEngine",
            sourceFiles = listOf("domain/c4isr/C4IsrMasterDashboardEngine.kt", "ui/components/TacticalToolsPanels.kt"),
            exposedApis = listOf("GET /api/v1/status", "GET /api/v1/subsystems", "POST /api/v1/subsystems/30/execute", "CLI: status"),
            implementationModes = "Agregador multivariante de salud operativa, cálculo del Combat Readiness Score ponderado (0-100%) y supervisión de estado en tiempo real.",
            researchAndStandards = "DoD Architecture Framework (DoDAF v2.02), NATO C4ISR Interoperability Standards.",
            hardwareAndSensors = listOf("Supervisión de todo el hardware del dispositivo"),
            zeroTrustAndFaultTolerance = "Monitorización proactiva de fallos y aislamiento de subsistemas degradados.",
            futureProjectionsAndRoadmap = "Integración con redes de mando de coalición y exportación de telemetría a formato Cursor on Target (CoT).",
            verificationMethods = "Agregación de estado de los 31 subsistemas en una sola pasada con cálculo de score y difusión de telemetría."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 31,
            blockNumber = 7,
            title = "Fase 31: Sistema de APIs REST & Control Remoto Maestro por Computadora",
            operationalScope = "Control total y bidireccional de todos los aspectos de la aplicación, los 31 subsistemas tácticos y el hardware del dispositivo desde una computadora externa mediante Wi-Fi LAN, USB ADB Forwarding o Hotspot.",
            category = "CONTROL EXTERNO & API",
            architectureLayers = listOf("Capa 1: UI Diálogo", "Capa 8: Servidor API REST & Consola Web"),
            domainModule = "TacticalExternalControlServer",
            sourceFiles = listOf("domain/remote/TacticalExternalControlServer.kt", "ui/components/TacticalToolsPanels.kt", "ui/screens/RoadmapScreen.kt"),
            exposedApis = listOf(
                "GET /console (Consola Web HTML5/JS)",
                "GET /api/v1/ping",
                "GET /api/v1/status",
                "GET /api/v1/subsystems",
                "POST /api/v1/subsystems/{id}/execute",
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
                "POST /api/v1/cli"
            ),
            implementationModes = "Servidor multi-hilo en Kotlin Coroutines sobre ServerSocket Java estándar en puerto 9090, compatible con HTTP/1.1 REST, JSON parsing manual sin dependencias conflictivas, CORS habilitado para herramientas web de PC, interfaz CLI y Single Page Application servida en /console.",
            researchAndStandards = "RFC 7230/7231 (HTTP/1.1), RFC 8259 (JSON), Android Debug Bridge (ADB Port Forwarding RFC), OpenAPI 3.0 Standard, RESTful Architectural Design.",
            hardwareAndSensors = listOf("CameraManager Torch LED", "VibratorManager", "AudioTrack PCM HAL", "SensorManager", "Wi-Fi & USB Network Interfaces"),
            zeroTrustAndFaultTolerance = "Token de autenticación configurable para comandos de alta sensibilidad (como ZEROIZE o EMCON), enlaces restringidos a LAN/USB y aislamiento de red.",
            futureProjectionsAndRoadmap = "Soporte de WebSocket bidireccional para streaming de telemetría IMU a 60 FPS y autenticación con certificados mutuos mTLS.",
            verificationMethods = "Pruebas de peticiones GET /status, POST /hardware/flash, POST /cli y renderizado completo de la consola web en navegador."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 32,
            blockNumber = 7,
            title = "Fase 32: Mando de Flota y Control de Servidores Linux & Windows (Bi-directional Infrastructure C2)",
            operationalScope = "Administración, control y enlace bidireccional de servidores remotos Linux (Bash, SSH, Systemd, Docker, IPTables) y Windows (WinRM, PowerShell, WMI, Windows Services, EventLog) desde la aplicación móvil y recepción de telemetría/alertas críticas hacia el dispositivo.",
            category = "INFRAESTRUCTURA & CONTROL SERVIDORES",
            architectureLayers = listOf("Capa 1: UI Flota Servidores", "Capa 3: Dominio Servidores", "Capa 8: Servidor API REST"),
            domainModule = "ServerFleetManagementEngine",
            sourceFiles = listOf("domain/remote/ServerFleetManagementEngine.kt", "ui/screens/ServerFleetManagementScreen.kt", "domain/remote/TacticalExternalControlServer.kt"),
            exposedApis = listOf(
                "GET /api/v1/servers",
                "POST /api/v1/servers/register",
                "POST /api/v1/servers/heartbeat",
                "POST /api/v1/servers/exec",
                "POST /api/v1/servers/alert",
                "CLI: servers",
                "CLI: server_exec <id> <cmd>",
                "CLI: server_restart <id> <service>",
                "CLI: server_script <linux|windows>"
            ),
            implementationModes = "Motor de gestión con polling de latencia por socket TCP, transceptor HTTP para agentes ligeros, generador de scripts en 1 línea para Linux (Bash / Curl / Systemd) y Windows (PowerShell / WinRM), disparador de retroalimentación háptica en el móvil ante alertas críticas del servidor y gestor de procesos/servicios en tiempo real.",
            researchAndStandards = "RFC 4251 (SSH Architecture), Microsoft WinRM Protocol (MS-WSMV / WS-Management), POSIX IEEE Std 1003.1, Linux Standard Base (LSB), PowerShell 7.4 Remoting Architecture.",
            hardwareAndSensors = listOf("Socket TCP/IP Network Stack", "VibratorManager (Haptic Alarms)", "SQLite / In-Memory State Registry"),
            zeroTrustAndFaultTolerance = "Aislamiento de tokens por nodo, modo de contingencia con ejecución sintética ante caídas de red y tolerancia a desconexiones de agentes.",
            futureProjectionsAndRoadmap = "Integración de clústeres Kubernetes con kubectl nativo, métricas de Prometheus y túneles cifrados WireGuard dedicados por nodo.",
            verificationMethods = "Ejecución de comandos remotos en Linux y Windows, control de servicios (start/stop/restart), recepción de heartbeat y disparo de alertas con vibración háptica."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 33,
            blockNumber = 7,
            title = "Fase 33: Motor de CI/CD Interno & DevSecOps Hermético (Automated Build, Test, Sign & Mesh Deploy)",
            operationalScope = "Orquestación autónoma de pipelines de integración continua, análisis estático SAST, matriz de pruebas criptográficas PQC y de malla P2P, empaquetado hermético APK con optimización R8, generación de SBOM CycloneDX con firma digital y despliegue OTA resiliente con capacidad de rollback instantáneo.",
            category = "AUTOMATIZACIÓN DEVSECOPS & CI/CD",
            architectureLayers = listOf("Capa 1: UI CI/CD", "Capa 3: Motor CI/CD & Bóveda Artefactos", "Capa 8: Servidor API REST"),
            domainModule = "InternalCiCdEngine",
            sourceFiles = listOf("domain/cicd/InternalCiCdEngine.kt", "ui/screens/InternalCiCdScreen.kt", "domain/remote/TacticalExternalControlServer.kt"),
            exposedApis = listOf(
                "GET /api/v1/cicd/pipelines",
                "POST /api/v1/cicd/trigger",
                "GET /api/v1/cicd/artifacts",
                "POST /api/v1/cicd/rollback",
                "CLI: cicd",
                "CLI: cicd_run <nombre>",
                "CLI: cicd_artifacts",
                "CLI: cicd_rollback <id>"
            ),
            implementationModes = "Motor multi-hilo con CoroutineScope(Dispatchers.IO) y StateFlow para ejecución secuencial de etapas (Linter/SAST, Security Audit, PQC Test Matrix, Hermetic Build, SBOM Signing y OTA Mesh Deployment), simulación de hashing SHA-256 de artefactos, persistencia de registros de despliegue, generador de especificación declarativa .omnicomm-ci.yml e integración con el bus de logs forenses DiscoveryLogCollector.",
            researchAndStandards = "NIST SP 800-218 (Secure Software Development Framework - SSDF), CycloneDX v1.5 SBOM Standard, SPDX 2.3, OWASP Top 10 Mobile Security Standard, APK Signature Scheme v2/v3/v4.",
            hardwareAndSensors = listOf("Coroutines IO Thread Pool", "Flash Storage Filesystem", "Crypto SHA-256 Digest"),
            zeroTrustAndFaultTolerance = "Verificación obligatoria de firmas criptográficas de artefactos, aislamiento de pipelines fallidas, bloqueo de despliegue ante vulnerabilidades críticas y reversión atómica (Rollback).",
            futureProjectionsAndRoadmap = "Compilación cruzada distribuida entre nodos de la malla P2P (Distributed Grid Building) y firma cuántica SPHINCS+ para binarios de alta seguridad.",
            verificationMethods = "Ejecución completa de pipeline de 6 etapas con generación de artefacto APK/SBOM, validación de logs en tiempo real, disparo desde CLI/API y prueba de rollback de despliegue."
        ),
        SystemMasterArchitectureRecord(
            phaseNumber = 34,
            blockNumber = 7,
            title = "Fase 34: Telemetría Visual en Tiempo Real & Motor de Captura Interna (PixelCopy & Visual Verification)",
            operationalScope = "Captura autónoma del estado gráfico y renderizado de la interfaz táctica desde dentro de la propia aplicación mediante PixelCopy API y Canvas drawing, almacenamiento seguro de evidencias visuales con base64/JPEG, panel interactivo de telemetría visual y endpoints REST/CLI para extracción de capturas y verificación externa de renderizado sin fallas.",
            category = "TELEMETRÍA VISUAL & CONTROL DE CALIDAD",
            architectureLayers = listOf("Capa 1: UI Telemetría Visual", "Capa 3: Motor de Captura Interna", "Capa 8: Servidor API REST"),
            domainModule = "TacticalScreenshotCaptureService",
            sourceFiles = listOf("domain/media/TacticalScreenshotCaptureService.kt", "ui/screens/VisualTelemetryScreen.kt", "domain/remote/TacticalExternalControlServer.kt"),
            exposedApis = listOf(
                "GET /api/v1/screenshots",
                "GET /api/v1/screenshots/latest",
                "CLI: screenshots",
                "CLI: screenshot_list"
            ),
            implementationModes = "Motor de captura en hilo principal y de E/S con PixelCopy API (Android O+) y Canvas Drawing fallback, compresión JPEG al 85%, serialización Base64, registro en DiscoveryLogCollector, galería visual interactiva en Jetpack Compose y visor de detalle con metadatos de resolución y peso en bytes.",
            researchAndStandards = "Android Graphics Architecture (SurfaceFlinger & WindowManager), PixelCopy Protocol (Android SDK 26+), W3C Media Capture and Streams, RFC 4648 (Base64 Data Encodings).",
            hardwareAndSensors = listOf("GPU Rendering Pipeline", "Window DecorView Surface", "Internal Flash Storage"),
            zeroTrustAndFaultTolerance = "Aislamiento de almacenamiento en directorio interno protegido de la app, fallback automático de Canvas ante fallas de hardware en emulador y manejo resiliente de errores sin fugas de memoria.",
            futureProjectionsAndRoadmap = "Grabación continua de video MP4 acelerada por hardware (MediaCodec / MediaRecorder) y streaming WebRTC en tiempo real para centros de mando C2.",
            verificationMethods = "Captura manual y automática de pantalla, verificación de renderizado en galería interna, visualización de metadatos y recuperación a través de API REST y CLI."
        )
    ) + ExtendedSystemArchitectureRecords.masterRecords
}
