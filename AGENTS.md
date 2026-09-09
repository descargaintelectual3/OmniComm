# 🤖 AGENTS.md — Protocolos Operativos y Directivas de Ingeniería para Agentes AI

> **IMPORTANTE**: Este archivo contiene las normas maestras y directivas inmutables de desarrollo para cualquier Agente de IA que opere, modifique o mantenga el repositorio de **OmniComm**. Estas directivas tienen prioridad operativa absoluta.

---

## 🧭 1. Misión Primaria y Principios Fundacionales

1. **Doctrina Offline-First Absoluta**:
   - Todo componente o funcionalidad añadida **debe** operar de forma 100% autónoma en ausencia de red celular o acceso a Internet.
   - Jamás introduzcas dependencias que bloqueen la UI o detengan la ejecución por falta de conectividad remota o servicios de Google Play.

2. **Cero Datos Simulados / Integraciones Reales**:
   - Está terminantemente prohibido utilizar generadores de datos aleatorios ("Math.random() mocks") para simular sensores, enlaces de radio, telemetría o paquetes de red cuando existan APIs reales en el framework de Android.
   - Implementa llamadas concretas al Hardware Abstraction Layer (HAL): `SensorManager`, `ConsumerIrManager`, `DisplayManager`, `AudioRecord`, `AudioTrack`, `UsbManager`, etc.

3. **Arquitectura Zero-Trust y Criptografía Auténtica**:
   - Nunca transmitas ni almacenes información sensible en texto plano.
   - Todo dato almacenado en la base de datos local Room debe procesarse con cifrado autenticado de cargas útiles (**AES-256-GCM** vía `E2EERoomPayloadSecurityUtility`).

---

## 🏛️ 2. Reglas de Arquitectura y Código en Android

### A. Persistencia y Room Database
- **Patrón de Resiliencia Multicapa**: Toda interacción con la base de datos debe contemplar la inicialización segura de `OmniDatabase`.
- En caso de que bibliotecas nativas de cifrado (ej. SQLCipher) experimenten fallas de enlace (`UnsatisfiedLinkError`) en ciertas arquitecturas de CPU o emuladores, el sistema debe efectuar un fallback inmediato y transparente a Room estándar con cifrado de nivel de aplicación (AES-256-GCM), preservando la integridad de los datos sin provocar caídas (*crashes*).
- Los accesos a Room deben realizarse siempre en despachadores de E/S (`Dispatchers.IO`) mediante corrutinas de Kotlin o flujos reactivos (`Flow`).

### B. ViewModels y Gestión de Estado (MVI / MVVM)
- Utiliza **Kotlin Coroutines** y **StateFlow** / **SharedFlow**.
- Toda pantalla o componente principal debe exponer un estado inmutable `StateFlow<UiState>` y recibir eventos o intenciones a través de métodos explícitos en el ViewModel.
- Prohibida la mutación directa de estados dentro de funciones Composable.

### C. Jetpack Compose y Material Design 3 (M3)
- Todas las interfaces deben desarrollarse exclusivamente en **Jetpack Compose**.
- Mantén consistencia estricta con la paleta de colores táctica definida en `com.example.ui.theme.Theme.kt`.
- Cumple con los estándares de accesibilidad de Android:
  - Objetivos táctiles mínimos de **48dp x 48dp**.
  - `contentDescription` significativo para todo elemento visual o interactivo.
  - Soporte de `Modifier.testTag("nombre_del_tag")` en todos los componentes interactivos principales.

### D. Control de Emisiones (EMCON)
- Respeta estrictamente los niveles EMCON (0 a 4):
  - Al recibir una orden de `EMCON 4`, detén de inmediato cualquier transmisión por radio, balizas UDP multicast, escaneos Bluetooth LE y emisiones IR/acústicas.

---

## 🛠️ 3. Protocolo de Pruebas y Validación

1. **Compilación Limpia (`compile_applet`)**:
   - Cada cambio de código debe verificarse con `compile_applet` antes de dar por completada la tarea.
   - Si la compilación falla, analiza el registro del compilador, aplica una corrección quirúrgica y reintenta (máximo 3 intentos iterativos con enfoque diferenciado).

2. **Pruebas JVM Locales con Robolectric**:
   - No intentes ejecutar pruebas instrumentadas dependientes de emuladores (`connectedAndroidTest` o `adb`).
   - Utiliza pruebas unitarias JVM locales con **Robolectric** (`gradle :app:testDebugUnitTest`) para validar flujos críticos.

3. **Prohibición de Limpieza Innecesaria**:
   - No ejecutes `gradle clean` a menos que sea un recurso de última instancia, ya que ralentiza significativamente el contenedor de construcción.

---

## 📋 4. Convenciones de Git y Control de Versiones

- Commits atómicos siguiendo la convención **Conventional Commits**:
  - `feat(modulo): descripción breve`
  - `fix(modulo): corrección del error identificado`
  - `docs(sección): actualización o creación de documentación`
  - `refactor(modulo): mejora estructural sin cambio funcional`
- Mantener sincronizados siempre ambos remotos del proyecto (`origin` y `descargaintelectual`) cuando se realicen operaciones de push.

---

## ⚙️ 5. Catálogo de Subsistemas Tácticos en OmniComm

| Identificador | Módulo / Clase Principal | Función Crítica |
|---|---|---|
| `SUB-01` | `AfskBell202ModemEngine` | Modulación y demodulación acústica AFSK de 1200 baudios |
| `SUB-02` | `TacticalDeXDisplayEngine` | Salida de video HDMI OTG / DisplayPort y trackpad virtual |
| `SUB-03` | `TacticalUniversalTvRemoteEngine` | Control de pantallas por IR Blaster y red LAN Smart TV |
| `SUB-04` | `TacticalUniversalDeviceGateway` | Servidor micro-HTTP local (puerto 8888) para clientes web |
| `SUB-05` | `E2EERoomPayloadSecurityUtility` | Cifrado y descifrado AES-256-GCM para almacenamiento Room |
| `SUB-06` | `PdrNavigationEngine` | Navegación a estima por peatón (PDR) sin GPS |
| `SUB-07` | `UdpMeshManager` | Enrutamiento de malla P2P mediante sockets UDP multicast |
| `SUB-08` | `TacticalUsbRadioSerialEngine` | Comunicación serial con transceptores de radio USB OTG |

*Adhiérete fielmente a estos lineamientos para mantener la robustez, resiliencia y estabilidad militar de OmniComm.*
