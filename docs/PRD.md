# 📄 Documento de Requisitos del Producto (PRD)

## OmniComm — Plataforma Táctica de Comunicaciones Resilientes y Comando C4ISR

**Versión del Documento**: 2.4.0  
**Fecha de Publicación**: Septiembre 2026  
**Estado**: Producción / Aprobado  
**Clasificación**: Documento Oficial de Ingeniería Táctica  

---

## 1. Resumen Ejecutivo y Visión

### 1.1 Visión del Producto
**OmniComm** es una plataforma móvil unificada de mando, control, comunicaciones, computación, inteligencia, vigilancia y reconocimiento (**C4ISR**) diseñada para garantizar la continuidad operativa ininterrumpida de equipos tácticos, rescatistas de primera respuesta y operadores de emergencias en teatros donde la infraestructura convencional de telecomunicaciones (torres celulares, satélites comerciales e Internet) ha colapsado, está bajo denegación electromagnética (interferencias / guerra electrónica) o es inexistente.

### 1.2 Declaración del Problema
Los sistemas de mensajería convencionales y las herramientas de coordinación actuales dependen al 100% de servidores centralizados y acceso continuo a Internet. En situaciones de catástrofe natural, operaciones en zonas agrestes o ambientes con interferencia de radio activa (guerra electrónica), las aplicaciones tradicionales quedan inutilizadas de inmediato, incomunicando a los operadores y arriesgando vidas o misiones críticas.

### 1.3 Propuesta de Valor Única
- **Operación 100% Autónoma (Air-Gap)**: El sistema no requiere ningún servidor externo para su operación completa en campo.
- **Transmisión de Datos Multi-Portadora**: Conecta nodos por Wi-Fi Direct, sockets UDP multicast, módem acústico de audio (AFSK Bell 202 por jack de 3.5 mm o micrófono) y radio serial USB OTG.
- **Estación de Mando HDMI Omni-DeX**: Convierte cualquier monitor o televisor conectado por HDMI en una sala de situación C4ISR militar con trackpad táctil en el teléfono.
- **Control Remoto de Pantallas (IR & LAN)**: Permite gobernar pantallas de campo mediante ráfagas infrarrojas de hardware o paquetes de red local sin mandos a distancia externos.
- **Pasarela Web Multi-Dispositivo**: Acceso inmediato a la red táctica para cualquier laptop o iPhone mediante un servidor web local (puerto 8888).
- **Criptografía Post-Cuántica y Enclave**: Protección de datos locales con Room SQLite + cifrado **AES-256-GCM** y transporte con **ML-KEM Kyber-768**.

---

## 2. Personas Objetivo y Casos de Uso

### 2.1 Personas Objetivo
1. **Operador Táctico / Comandante de Escuadra**:
   - Necesita coordinar a su equipo en mallas locales sin emitir firmas de radio identificables (control estricto de emisiones EMCON).
   - Requiere visualizar posiciones relativas, reportes de avistamiento SALUTE y telemetría de drones en mapas tácticos con simbología OTAN APP-6.
2. **Rescatista de Primera Respuesta (Búsqueda y Rescate - SAR)**:
   - Trabaja en zonas de terremoto o huracán sin señal celular.
   - Utiliza la red tolerante a demoras (DTN) para transmitir listas de supervivientes y necesidades médicas entre grupos distantes mediante nodos móviles (*data mules*).
3. **Oficial de Comunicaciones y Ciberseguridad**:
   - Responsable de la integridad criptográfica, distribución de claves Shamir $(k, n)$ y ejecución de protocolos de ceroización segura (DoD 5220.22-M) en caso de riesgo de captura.

---

## 3. Requisitos Funcionales (FR)

### FR-01: Red de Malla P2P Autónoma
- **FR-01.1**: El sistema debe descubrir automáticamente nodos pares en la red local mediante paquetes de baliza UDP Multicast periódicos (`224.0.0.1`, puerto `8888/9090`).
- **FR-01.2**: Debe establecer túneles de comunicación Wi-Fi Direct P2P entre dispositivos sin requerir puntos de acceso inalámbrico externos.
- **FR-01.3**: Debe implementar enrutamiento reactivo multi-salto con contabilización de saltos (*hop count*) y supresión de bucles para extender la cobertura de la malla.

### FR-02: Red Tolerante a Demoras (DTN Store-and-Forward)
- **FR-02.1**: Los mensajes dirigidos a nodos fuera del alcance directo deben almacenarse localmente en la base de datos Room con un tiempo de expiración (TTL) configurable.
- **FR-02.2**: Cuando un nodo intermedio se aproxime físicamente al nodo destino o a otro repetidor, los paquetes acumulados deben transmitirse de forma oportunista.
- **FR-02.3**: Las cargas útiles deben comprimirse mediante algoritmos adaptativos Huffman/CBOR para minimizar los tiempos de transmisión en el aire.

### FR-03: Módem Acústico AFSK Bell 202
- **FR-03.1**: Debe modular datos digitales en señales de audio analógicas utilizando modulación por desplazamiento de frecuencia de audio (AFSK) estándar Bell 202 (frecuencias de marca: 1200 Hz, espacio: 2200 Hz) a una tasa de 1200 baudios.
- **FR-03.2**: La señal modulada debe poder reproducirse por el altavoz integrado del dispositivo o por el conector de audio de 3.5 mm conectado a la entrada de micrófono de transceptores de radio HF/VHF/UHF.
- **FR-03.3**: El motor de demodulación debe capturar audio continuo a través de `AudioRecord` (16-bit PCM, 44.1 kHz), filtrar la señal mediante DSP y reconstruir los bytes transmitidos con verificación CRC-16.

### FR-04: Control Remoto Universal de Pantallas (IR Blaster & Smart TV LAN)
- **FR-04.1**: El sistema debe consultar el servicio del sistema `ConsumerIrManager` de Android para verificar la disponibilidad de emisores físicos infrarrojos.
- **FR-04.2**: Para pantallas convencionales no conectadas a la red, debe emitir patrones de pulsos IR a frecuencias portadoras estándar (36 kHz a 56 kHz) para marcas globales (Samsung, LG, Sony, TCL, Philips, Panasonic, Hisense).
- **FR-04.3**: Para pantallas inteligentes (Smart TVs) en la misma red Wi-Fi o punto de acceso, debe enviar comandos de control a través de sockets de red (REST / UDP).
- **FR-04.4**: Debe proveer una interfaz de botonera táctica completa: encendido, silencio, selector de fuentes HDMI, D-Pad de navegación, volumen y canales.

### FR-05: Estación de Escritorio Táctico Omni-DeX (HDMI C4ISR)
- **FR-05.1**: El sistema debe detectar pantallas externas secundarias conectadas mediante HDMI OTG, adaptadores DisplayPort USB-C o protocolos inalámbricos (Miracast/Google Cast) utilizando `DisplayManager`.
- **FR-05.2**: Debe proyectar una vista secundaria independiente (`Presentation` o Canvas C2) que despliegue el reloj militar ZULU, nivel DEFCON, ventanas de telemetría de drones, espectrograma de radio y mapa táctico.
- **FR-05.3**: Debe habilitar en la pantalla del dispositivo móvil un trackpad virtual sensible al tacto para gobernar el cursor y disparar clics en la pantalla secundaria externa.

### FR-06: Pasarela Web Multi-Dispositivo (Servidor HTTP Local)
- **FR-06.1**: Debe incorporar un servidor HTTP embebido autónomo escuchando en el puerto local `8888`.
- **FR-06.2**: Debe servir una interfaz web ligera SPA (Single Page Application) accesible desde navegadores web modernos (Chrome, Safari, Firefox, Edge) sin requerir instalación de software en los equipos cliente (Windows, macOS, Linux, iOS).
- **FR-06.3**: La pasarela debe permitir consultar el estado de la malla, transmitir órdenes C2 a la escuadra y operar el mando de televisión desde la laptop o tableta conectada.

### FR-07: Chat Táctico y Mensajería E2EE
- **FR-07.1**: Debe soportar mensajes de texto enriquecidos, notas de voz comprimidas, reportes militares estructurados (SALUTE) y ubicaciones geográficas en formato MGRS y lat/long.
- **FR-07.2**: Todo mensaje debe ser firmado digitalmente y cifrado de extremo a extremo mediante **AES-256-GCM** y encapsulación post-cuántica **ML-KEM Kyber-768**.
- **FR-07.3**: Los mensajes pendientes por falta de enlace deben encolarse automáticamente en Room y reintentarse con retroceso exponencial (*exponential backoff*).

### FR-08: Navegación Inercial sin GPS (PDR) y Radar Táctico
- **FR-08.1**: El sistema debe calcular la trayectoria del operador a partir de los datos inerciales del acelerómetro, giroscopio y magnetómetro mediante el algoritmo de paso Weinberg.
- **FR-08.2**: Debe proveer una brújula solar que estime el azimut verdadero basándose en la posición astronómica del sol y la hora UTC del sistema.
- **FR-08.3**: Debe calcular rutas de escape seguras mediante el algoritmo A* evitando zonas hostiles o de radiación electromagnética reportadas.

### FR-09: Persistencia Segura y Fallback Resiliente en Room
- **FR-09.1**: La información persistente (mensajes, sesiones, bitácoras C2, entidades detectadas) debe almacenarse en una base de datos relacional **AndroidX Room**.
- **FR-09.2**: Las cargas útiles sensibles deben cifrarse antes de su inserción con **AES-256-GCM** mediante `E2EERoomPayloadSecurityUtility`.
- **FR-09.3**: Si la biblioteca nativa SQLCipher experimenta fallos de enlace en ciertas arquitecturas de hardware, el motor debe transicionar de forma transparente y sin pérdidas a Room SQLite estándar con cifrado en capa de aplicación.

### FR-10: Control de Emisiones (EMCON) y Ceroización Segura (Zeroize)
- **FR-10.1**: Debe permitir alternar instantáneamente entre los niveles de emisión EMCON 0, 1, 2, 3 y 4.
- **FR-10.2**: En nivel EMCON 4, todos los transmisores de radio, balizas y periféricos deben silenciarse de inmediato.
- **FR-10.3**: El comando de Ceroización (Panic Zeroize) debe sobreescribir las claves de memoria en tres fases conforme a la directiva DoD 5220.22-M y eliminar la base de datos local.

---

## 4. Requisitos No Funcionales (NFR)

- **NFR-01 (Disponibilidad y Tolerancia a Fallos)**: La aplicación debe poseer un interceptor de excepciones global (*Crash Guardian*) que prevenga caídas involuntarias del proceso ante excepciones no críticas.
- **NFR-02 (Rendimiento y Latencia)**: El despacho de mensajes en la malla local directa debe completarse en menos de **150 ms** a través de sockets UDP.
- **NFR-03 (Eficiencia Energética)**: En modo de vigilancia pasiva (escucha de malla), el consumo de batería no debe superar el **3.5% por hora** en dispositivos con batería estándar de 4000 mAh.
- **NFR-04 (Compatibilidad de Plataforma)**: Soporte completo desde **Android 8.0 (API 26)** hasta **Android 15 (API 35)**.
- **NFR-05 (Seguridad Criptográfica)**: Claves simétricas de 256 bits (AES-GCM), vectores de inicialización aleatorios de 96 bits por cada operación y algoritmos post-cuánticos evaluados por el NIST (Kyber-768).
- **NFR-06 (Accesibilidad)**: Objetivos táctiles mínimos de 48dp x 48dp, contraste visual superior a 4.5:1 en modo táctico de alto contraste y compatibilidad con lectores de pantalla TalkBack.

---

## 5. Matriz de Trazabilidad y Validación

| ID Requisito | Caso de Prueba | Criterio de Aceptación |
|---|---|---|
| `FR-01` | Descubrimiento UDP en red ad-hoc | Los nodos pares aparecen en el directorio de contactos en < 2 segundos tras conectarse a la misma red. |
| `FR-03` | Transmisión AFSK por altavoz | El audio modulado a 1200 baudios es recibido y decodificado íntegramente por un segundo dispositivo sin pérdida de bytes. |
| `FR-04` | Emisión IR para TV Samsung/LG | El televisor responde al comando de encendido y selector de fuentes con latencia inferior a 250 ms. |
| `FR-05` | Conexión HDMI de monitor externo | La pantalla externa despliega la interfaz Omni-DeX C4ISR sin congelar la interfaz táctil del teléfono. |
| `FR-06` | Conexión web desde laptop (Safari/Chrome) | El navegador carga el panel C2 en `http://<ip>:8888` y permite disparar comandos de control. |
| `FR-09` | Fallback de Room ante fallo nativo | La aplicación arranca sin `UnsatisfiedLinkError` y almacena mensajes cifrados con AES-256-GCM. |
| `FR-10` | Activación de EMCON 4 | Se suspenden de inmediato todos los hilos de emisión UDP, Bluetooth y escaneos periódicos. |

---

<div align="center">
<b>Fin del Documento de Requisitos del Producto (PRD) — OmniComm Core</b>
</div>
