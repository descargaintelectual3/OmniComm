# 🛰️ OmniComm

<div align="center">

![OmniComm Banner](assets/icons/banner.png)

**Resilient Tactical Communications, Zero-Trust C4ISR Hub & Autonomous Mesh Operations Platform**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-blue.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Room-SQLite%20%2B%20AES--256--GCM-orange.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Zero Trust](https://img.shields.io/badge/Security-Zero--Trust%20EMCON%200--4-red.svg?style=flat)](docs/SDD.md)
[![License](https://img.shields.io/badge/License-Proprietary%20%2F%20Tactical-black.svg)](LICENSE)

*Operación 100% autónoma en entornos electromagnéticamente hostiles, zonas de desastre y teatros de operaciones sin infraestructura.*

[Arquitectura](README_ARCHITECTURE.md) • [PRD](docs/PRD.md) • [SDD](docs/SDD.md) • [Instrucciones de Agentes](AGENTS.md) • [Protocolo Gemini](GEMINI.md) • [Catálogo de Skills](docs/SKILLS.md)

</div>

---

## 📑 Tabla de Contenidos

- [Visión General](#-visión-general)
- [Capacidades Clave](#-capacidades-clave)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Ecosistema C4ISR & Control Remoto](#-ecosistema-c4isr--control-remoto)
- [Seguridad & Zero-Trust](#-seguridad--zero-trust)
- [Estructura del Repositorio](#-estructura-del-repositorio)
- [Guía de Inicio Rápido](#-guía-de-inicio-rápido)
- [Documentación del Sistema](#-documentación-del-sistema)
- [Gobernanza de Agentes & Automatización](#-gobernanza-de-agentes--automatización)

---

## 🌐 Visión General

**OmniComm** es un centro integral de comunicaciones tácticas unificadas de grado militar y supervivencia civil. Ha sido concebido y construido bajo la doctrina de **desconexión total garantizada (Air-Gapped / Offline-First)**: el 100% de sus funcionalidades críticas —incluyendo mensajería entre pares, posicionamiento inercial sin satélites (PDR), módem acústico, comando y control (C2) y cifrado post-cuántico— operan sin depender de servidores centrales, conexión celular o acceso a Internet.

Cuando se detecta conectividad externa, el sistema se sincroniza de forma híbrida mediante una bóveda en la nube cifrada en el cliente con **AES-256-GCM** y verificación de integridad criptográfica.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         OMNICOMM TACTICAL PLATFORM                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  HUD Táctico M3  │ Radar GIS / APP-6 │ Team Chat E2EE │ Salas WebRTC P2P   │
├──────────────────┴───────────────────┴────────────────┴─────────────────────┤
│                    ESTACIÓN OMNI-DEX & PASARELA MULTI-OS                    │
│   Escritorio HDMI OTG C4ISR • Servidor HTTP Local 8888 • Remoto IR / LAN TV │
├─────────────────────────────────────────────────────────────────────────────┤
│                  ENLACES TÁCTICOS FÍSICOS & RADIOFRECUENCIA                 │
│  Módem Acústico AFSK Bell 202 (Audio Jack) • Radio Serial USB OTG (115200b)│
├─────────────────────────────────────────────────────────────────────────────┤
│                      MOTOR DE MALLA P2P RESILIENTE                          │
│   UDP Multicast Mesh • Wi-Fi Direct • DTN Store & Forward • Huffman Coder  │
├─────────────────────────────────────────────────────────────────────────────┤
│                      NÚCLEO CRIPTOGRÁFICO ZERO-TRUST                        │
│   AES-256-GCM • ML-KEM Kyber-768 • Shamir GF(256) • DoD 5220.22-M Zeroize  │
├─────────────────────────────────────────────────────────────────────────────┤
│                   PERSISTENCIA LOCAL & FALLBACK RESILIENTE                  │
│       Room Database (AndroidX) + Cifrado Autenticado de Cargas Útiles       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ⚡ Capacidades Clave

### 1. Malla P2P Autónoma & Red DTN
- **Descubrimiento Descentralizado**: Transmisión de balizas por sockets UDP Multicast (`224.0.0.1`) y canales Wi-Fi Direct P2P sin infraestructura previa.
- **Red Tolerante a Demoras (DTN)**: Almacenamiento local de paquetes con tiempo de vida (TTL), enrutamiento oportunista por contacto físico (*Store-and-Forward*) y compresión adaptativa Huffman/CBOR.
- **Salto de Frecuencias Lógico (FHSS)**: Rotación pseudoaleatoria sincronizada de puertos y canales de escucha para eludir interferencias y detección de espectro.

### 2. Canales Alternativos de Transmisión (Cero Radiación RF)
- **Módem Acústico AFSK Bell 202**: Transmisión y recepción de datos digitales modulados por tonos de audio audible o ultrasónico (1200/2200 Hz) a través del altavoz y micrófono del dispositivo, o interconectado por cable de audio de 3.5 mm a transceptores de radio HF/VHF/UHF (Baofeng, Yaesu, Motorola).
- **Enlace Físico Radio Serial USB OTG**: Conexión plug-and-play directa por puerto USB-C / microUSB con adaptadores UART FTDI, CP2102 y CH340 a 115200 baudios para radios de paquetes y módems LoRa.

### 3. Centro de Comando C4ISR, Omni-DeX & Pantalla Grande
- **Escritorio Táctico Omni-DeX**: Detección plug-and-play de pantallas externas y monitores de sala de situación mediante **HDMI OTG**, DisplayPort alternativo o proyección inalámbrica (Miracast). Proyecta un entorno de escritorio táctico con telemetría en tiempo real, mapas y ventanas C2.
- **Trackpad Táctico de Pantalla**: La pantalla táctil del móvil se transforma automáticamente en un trackpad virtual para controlar el puntero y acciones en el televisor o monitor externo.
- **Control Remoto Universal de Pantallas (IR Blaster & LAN)**: Emisión física de ráfagas infrarrojas (`ConsumerIrManager`) para televisores y pantallas convencionales, junto con comandos de red para Smart TVs (Samsung, LG, Sony, TCL, Philips, Panasonic, Hisense).

### 4. Pasarela Web Multi-Dispositivo (PC, Mac, Linux, iPhone)
- **Servidor Micro-HTTP Local Integrado**: Despliega un portal web táctico local en el puerto `8888`.
- **Interoperabilidad Universal**: Permite que cualquier operador con laptop (Windows, macOS, Linux) o dispositivo móvil secundario (iPad, iPhone, Android) acceda a las funciones C2, envíe órdenes y monitoree la malla simplemente abriendo el navegador en la red local o punto de acceso WiFi táctico.

### 5. Navegación Inercial & Conocimiento Situacional Sin GPS
- **PDR (Pedestrian Dead Reckoning)**: Cálculo de trayectoria y pasos mediante fusión de acelerómetro, giroscopio y magnetómetro con filtrado adaptativo de paso Weinberg.
- **Brújula Solar y Azimut Real**: Estimación astronómica de orientación norte a partir del ángulo del sol y la hora UTC/ZULU.
- **Cartografía Táctica & Rutas de Escape**: Simbología militar estándar OTAN APP-6 y motor de enrutamiento A* (*A-Star*) para evasión de zonas de peligro.

---

## 🏛️ Arquitectura del Sistema

OmniComm implementa una arquitectura desacoplada de 8 capas inspirada en Clean Architecture y principios de alta disponibilidad:

| Capa | Denominación | Paquete Principal | Responsabilidad |
|---|---|---|---|
| **L1** | Presentación & UI Táctica | `com.example.ui.*` | Jetpack Compose M3, Canvas, HUD militar, dialogs y paneles tácticos. |
| **L2** | Orquestación & Estado | `com.example.ui.viewmodel.*` | ViewModels, StateFlow, SharedFlow, arquitectura MVI unidireccional. |
| **L3** | Dominio C4ISR, Cripto & DSP | `com.example.domain.*` | Criptografía cuántica, enrutamiento, algoritmos de detección y telemetría. |
| **L4** | Malla P2P & DTN | `com.example.domain.mesh.*` | Capa de transporte UDP, sockets multicast, paquetes binarios CBOR. |
| **L5** | Zero-Trust & Enclave | `com.example.domain.security.*` | Android KeyStore HW, niveles EMCON 0-4, detector de intrusiones, Zeroize. |
| **L6** | Persistencia Local & Nube | `com.example.domain.local.*` | Room Database SQLite, DAOs de alta velocidad, migración destructiva controlada. |
| **L7** | Hardware HAL & Sensores | `com.example.domain.hardware.*` | IR Blaster, DisplayManager, USB Serial, Audio HAL, sensores IMU (50-100Hz). |
| **L8** | Pasarelas & APIs Externas | `com.example.domain.c2.*` | Servidor HTTP embebido (puerto 8888), API REST y túnel de control remoto. |

---

## 🔒 Seguridad & Zero-Trust

OmniComm aplica las normas más estrictas de ciberdefensa táctica:

- **Cifrado de Cargas Útiles Room (E2EE)**: Todo mensaje, archivo o telemetría almacenado localmente es cifrado mediante **AES-256-GCM** antes de ingresar a la base de datos Room, garantizando confidencialidad absoluta incluso si el archivo físico de la base de datos fuese extraído.
- **Criptografía Post-Cuántica (PQC)**: Integración de **ML-KEM Kyber-768** (FIPS 203) para intercambio de claves resistente a ataques por computación cuántica.
- **División de Secretos Shamir**: Esquema $(k, n)$ en campo de Galois $GF(256)$ para fragmentar claves críticas entre múltiples nodos de la escuadra.
- **Gestión EMCON (Control de Emisiones)**:
  - `EMCON 0`: Operación normal completa (RF + Sensores).
  - `EMCON 1`: Restricción de balizas periódicas activas.
  - `EMCON 2`: Silencio de radio Wi-Fi/Bluetooth; solo enlaces acústicos/cable.
  - `EMCON 3`: Recepción pasiva exclusiva (*Passive Listening*).
  - `EMCON 4`: Silencio electromagnético absoluto (Cero transmisiones).
- **Procedimiento de Ceroización (Zeroize DoD 5220.22-M)**: Sobreescritura en memoria de tres fases con ceros, unos y patrones aleatorios para claves y datos sensibles ante captura inminente.

---

## 📂 Estructura del Repositorio

```
OmniComm/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── domain/
│   │   │   │   ├── c2/               # Pasarelas de comando, gateway web HTTP y túneles
│   │   │   │   ├── hardware/         # HAL: Control TV IR, Omni-DeX Display, USB Serial
│   │   │   │   ├── local/            # Room Database, entidades y DAOs
│   │   │   │   ├── media/            # Módem acústico AFSK Bell 202, códecs de audio
│   │   │   │   ├── mesh/             # Red P2P UDP, DTN store & forward, ruteo
│   │   │   │   ├── security/         # AES-256-GCM, Kyber-768, Shamir, Zeroize
│   │   │   │   └── sync/             # Sincronización offline en segundo plano (WorkManager)
│   │   │   ├── ui/
│   │   │   │   ├── components/       # Componentes M3, HUD y suite C2
│   │   │   │   ├── screens/          # 14 pantallas tácticas (Hub, Radar, Chat, etc.)
│   │   │   │   ├── theme/            # Paleta táctica, tipografías y formas M3
│   │   │   │   └── viewmodel/        # ViewModels reactivos con StateFlow
│   │   │   ├── MainActivity.kt       # Punto de entrada de UI y orquestación NavHost
│   │   │   └── OmniApplication.kt    # Inicialización de servicios y Crash Guardian
│   │   ├── res/                      # Recursos Android (XML, valores, drawables)
│   │   └── AndroidManifest.xml       # Permisos HAL y servicios del sistema
│   └── build.gradle.kts              # Configuración del módulo de la aplicación
├── docs/
│   ├── PRD.md                        # Documento de Requisitos del Producto
│   ├── SDD.md                        # Documento de Diseño de Software / Arquitectura
│   └── SKILLS.md                     # Catálogo de Herramientas y Habilidades para Agentes
├── AGENTS.md                         # Normas operativas y directivas de desarrollo para Agentes
├── GEMINI.md                         # Protocolo de operaciones y directrices para Gemini LLM
├── README_ARCHITECTURE.md            # Mapa arquitectónico exhaustivo preexistente
├── metadata.json                     # Metadatos oficiales de Google AI Studio
├── build.gradle.kts                  # Configuración raíz de Gradle
└── settings.gradle.kts               # Módulos y catálogos de dependencias
```

---

## 🚀 Guía de Inicio Rápido

### Requisitos Previos
- **Android Studio**: Ladybug / Meerkat o superior.
- **JDK**: Java 17 o Java 21.
- **Android SDK**: MinSdk 26 (Android 8.0 Oreo), TargetSdk 35 (Android 15).
- **Dispositivo Físico**: Recomendado para pruebas de sensores (IMU, IR Blaster, USB OTG, Salida HDMI).

### Compilación y Ejecución
```bash
# Clonar el repositorio
git clone https://github.com/descargaintelectual3/OmniComm.git
cd OmniComm

# Compilar la aplicación en modo Debug
./gradlew assembleDebug

# Ejecutar las pruebas unitarias y de arquitectura
./gradlew testDebugUnitTest
```

---

## 📚 Documentación del Sistema

Para consultar las especificaciones técnicas completas, visite:
- **[Documento de Requisitos del Producto (PRD)](docs/PRD.md)**: Alcance funcional, historias de usuario y métricas de éxito.
- **[Documento de Diseño de Software (SDD)](docs/SDD.md)**: Diagramas de secuencia, especificación de protocolos, esquemas de base de datos y diseño del HAL.
- **[Mapa Maestro de Arquitectura](README_ARCHITECTURE.md)**: Detalle exhaustivo de las 8 capas y los 31 subsistemas tácticos.
- **[Catálogo de Skills para Agentes](docs/SKILLS.md)**: Especificación de habilidades especializadas para asistentes de inteligencia artificial.

---

## 🤖 Gobernanza de Agentes & Automatización

Este repositorio implementa protocolos estrictos para el trabajo asistido por IA:
- **[AGENTS.md](AGENTS.md)**: Directivas inmutables de código, arquitectura de fallbacks, directrices de prueba y cero simulación.
- **[GEMINI.md](GEMINI.md)**: Integración con la API de Gemini del lado del servidor, análisis de situaciones C4ISR y generación de reportes estructurados.

---

<div align="center">
<b>OmniComm Tactical Core</b> • Diseñado para la resiliencia operativa y la comunicación ininterrumpida.
</div>
