# 🛠️ Catálogo de Skills y Sistemas de Agentes (SKILLS.md)

## OmniComm — Ecosistema de Habilidades Especializadas para Agentes Tácticos e IA

**Versión del Catálogo**: 2.4.0  
**Clasificación**: Especificación de Capacidades y Herramientas Autónomas  

---

## 📑 1. Introducción al Sistema de Skills

El ecosistema de **Skills** de OmniComm define conjuntos de capacidades técnicas modulares y protocolos estandarizados diseñados para que agentes de IA autónomos (Gemini, Antigravity) e ingenieros de software operen, amplíen y mantengan el repositorio sin violar los principios fundamentales de **Offline-First**, **Zero-Trust** y **Resiliencia Operativa**.

Cada *Skill* describe:
- **Disparador Operativo**: Cuándo debe activarse o consultarse la habilidad.
- **Invariantes Técnicos**: Restricciones inmutables que no pueden ser alteradas.
- **Herramientas y Componentes Asignados**: Clases Kotlin y servicios de Android involucrados.
- **Protocolo de Verificación**: Cómo verificar que la implementación cumple los requisitos de grado militar.

---

## 🧰 2. Catálogo Maestro de Habilidades Tácticas

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CATÁLOGO DE SKILLS DE OMNICOMM                        │
├──────────────────────────┬──────────────────────────┬───────────────────────┤
│  SKILL-01: C4ISR & APP-6 │  SKILL-02: P2P MESH DTN  │ SKILL-03: AFSK MODEM  │
│  MGRS, OTAN, SALUTE      │  UDP Sockets, CBOR, TTL  │ Bell 202, AudioTrack  │
├──────────────────────────┼──────────────────────────┼───────────────────────┤
│  SKILL-04: OMNI-DEX HDMI │  SKILL-05: TV REMOTE IR  │ SKILL-06: ROOM E2EE   │
│  DisplayManager, Trackpad│  ConsumerIrManager, LAN  │ AES-256-GCM, Fallback │
├──────────────────────────┼──────────────────────────┼───────────────────────┤
│  SKILL-07: EMCON & ZERO  │  SKILL-08: USB SERIAL    │ SKILL-09: GEMINI C2   │
│  Niveles 0-4, DoD 5220   │  UART OTG, 115200 baud   │ SALUTE Report, Speech │
└──────────────────────────┴──────────────────────────┴───────────────────────┘
```

---

### 📡 SKILL-01: Protocolo Táctico C4ISR y Simbología OTAN APP-6
- **Identificador**: `tactical-c4isr-protocol`
- **Ámbito**: Coordenadas militares (MGRS/UTM), clasificación de unidades amigas/hostiles y reportes operacionales.
- **Reglas e Invariantes**:
  1. Las coordenadas terrestres deben siempre convertirse bidireccionalmente entre Lat/Long (WGS84) y MGRS (Military Grid Reference System).
  2. Los objetivos visuales en el radar deben asociarse a códigos de afinidad estándar: `AMIGO (Azul)`, `HOSTIL (Rojo)`, `NEUTRAL (Verde)` o `DESCONOCIDO (Amarillo)`.
  3. Los reportes de avistamiento deben estructurarse conforme al estándar militar **SALUTE** (*Size, Activity, Location, Unit, Time, Equipment*).

---

### 🕸️ SKILL-02: Red de Malla P2P Autónoma y Tolerancia a Demoras (DTN)
- **Identificador**: `mesh-p2p-dtn-engine`
- **Ámbito**: Descubrimiento entre pares, enrutamiento multi-salto y almacenamiento oportunista (*Store-and-Forward*).
- **Reglas e Invariantes**:
  1. Todo paquete de baliza (*beacon*) debe emitirse en broadcast o multicast (`224.0.0.1`) sin requerir conexión a un enrutador Wi-Fi o servidor DHCP.
  2. La carga útil debe empaquetarse en formato binario compacto (CBOR o compresión Huffman) con verificación CRC-16.
  3. Cada paquete debe incluir un contador de saltos (*hop count*) decremental y un identificador único (UUID) para descarte inmediato de duplicados y prevención de tormentas de difusión (*broadcast storms*).

---

### 📻 SKILL-03: Módem Acústico AFSK Bell 202 (Transmisión por Audio)
- **Identificador**: `afsk-acoustic-modem`
- **Ámbito**: Modulación y demodulación de datos mediante tonos audibles/inaudibles por micrófono y altavoz o cable de audio.
- **Reglas e Invariantes**:
  1. Modulación estricta a 1200 baudios: 1200 Hz para bit '1' (marca), 2200 Hz para bit '0' (espacio).
  2. La captura de audio debe ejecutarse en un hilo secundario continuo (`Dispatchers.Default`) con un búfer de tamaño óptimo calculado mediante `AudioRecord.getMinBufferSize()`.
  3. Prohibido el bloqueo del hilo de UI durante la síntesis PCM o el filtrado FFT de la señal de audio.

---

### 🖥️ SKILL-04: Estación de Mando HDMI Omni-DeX y Pantallas Grandes
- **Identificador**: `omnidex-c2-display`
- **Ámbito**: Proyección en monitores externos vía HDMI OTG, DisplayPort USB-C o Cast, y emulación de trackpad.
- **Reglas e Invariantes**:
  1. El sistema debe responder reactivamente a los eventos de `DisplayListener` (`onDisplayAdded`, `onDisplayRemoved`).
  2. En modo Omni-DeX, la pantalla externa muestra la consola de situación a pantalla completa mientras la pantalla del teléfono sirve de superficie táctil para el cursor.
  3. Las coordenadas del trackpad deben mapearse proporcionalmente $[0.0, 1.0]$ para soportar cualquier relación de aspecto (16:9, 16:10, 21:9).

---

### 📺 SKILL-05: Control Remoto Universal de Televisores (IR & LAN)
- **Identificador**: `universal-tv-remote`
- **Ámbito**: Emisión de ráfagas infrarrojas de hardware (`ConsumerIrManager`) y emulación por red local para Smart TVs.
- **Reglas e Invariantes**:
  1. Verificar siempre la presencia de emisor físico con `ConsumerIrManager.hasIrEmitter()`. Si no existe, conmutar transparentemente al modo de emulación Smart TV por red local.
  2. Las frecuencias portadoras deben respetar las especificaciones del fabricante (Samsung/LG a 38 kHz, Sony a 40 kHz).
  3. Prohibido ejecutar llamadas de transmisión infrarroja en el hilo principal de la UI.

---

### 🔐 SKILL-06: Persistencia Segura y Fallback Resiliente en Room (E2EE)
- **Identificador**: `room-e2ee-resilience`
- **Ámbito**: Base de datos local Room, cifrado AES-256-GCM de cargas útiles y mitigación de fallas nativas.
- **Reglas e Invariantes**:
  1. Todo payload almacenado debe procesarse a través de `E2EERoomPayloadSecurityUtility` antes de insertarse en Room.
  2. En caso de fallas de librerías nativas (`UnsatisfiedLinkError` en SQLCipher), el agente debe mantener activo el fallback automático a Room SQLite estándar con cifrado a nivel de aplicación.
  3. Toda transacción o consulta a la base de datos debe ejecutarse en `Dispatchers.IO`.

---

### 🛑 SKILL-07: Control de Emisiones (EMCON) y Ceroización DoD 5220.22-M
- **Identificador**: `zero-trust-emcon`
- **Ámbito**: Silencio electromagnético y borrado seguro de emergencia (*Panic Button*).
- **Reglas e Invariantes**:
  1. El comando de activación de `EMCON 4` tiene precedencia absoluta sobre cualquier otra tarea o retransmisión de datos.
  2. La ceroización de memoria debe realizarse sobreescribiendo los arreglos de bytes con `0x00`, `0xFF` y bytes pseudoaleatorios antes de solicitar la recolección de basura.
  3. Una vez ejecutada la ceroización, debe revocarse cualquier token de sesión y cerrarse todas las conexiones de base de datos activas.

---

### 🌐 SKILL-08: Pasarela Web Multi-Dispositivo (Servidor HTTP 8888)
- **Identificador**: `multi-device-web-gateway`
- **Ámbito**: Servidor micro-HTTP local para interoperabilidad con computadoras portátiles, iPhones y tabletas.
- **Reglas e Invariantes**:
  1. El servidor debe escuchar en todas las interfaces de red locales excepto loopback (`0.0.0.0:8888`).
  2. El servidor debe servir interfaces SPA estáticas y responder a llamadas REST con cabeceras `Content-Type: application/json` y `Access-Control-Allow-Origin: *`.
  3. Los comandos recibidos desde clientes web deben ser validados contra la lista blanca de órdenes permitidas antes de su ejecución.

---

### 🧠 SKILL-09: Inteligencia Táctica Asistida por Servidor (Gemini C2)
- **Identificador**: `server-side-gemini-c4isr`
- **Ámbito**: Procesamiento de inteligencia, análisis de telemetría y formateo de reportes con la API de Gemini del lado del servidor (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`).
- **Reglas e Invariantes**:
  1. Las llamadas a la API de Gemini deben realizarse siempre con manejo de fallos y fallback a procesamiento heurístico local cuando no haya conexión a Internet.
  2. Las credenciales deben consumirse a través de `BuildConfig` y el panel de secretos de AI Studio, sin hardcodear claves en el código fuente.
  3. Los resultados generados por el modelo deben someterse a validación de formato antes de presentarse en las consolas tácticas.

---

<div align="center">
<b>Fin del Catálogo de Skills — OmniComm Architecture System</b>
</div>
