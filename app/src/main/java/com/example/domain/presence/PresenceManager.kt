package com.example.domain.presence

import android.content.Context
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.dao.ContactDao
import com.example.domain.local.entities.ContactEntity
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogSeverity
import com.example.domain.models.ConnectionType
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserPresenceStatus(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val battery: Int = 100,
    val fcmToken: String = ""
)

class PresenceManager(
    private val context: Context,
    private val contactDao: ContactDao = OmniDatabase.getDatabase(context).contactDao()
) {
    private val database = FirebaseDatabase.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _onlineUsersMap = MutableStateFlow<Map<String, UserPresenceStatus>>(emptyMap())
    val onlineUsersMap: StateFlow<Map<String, UserPresenceStatus>> = _onlineUsersMap.asStateFlow()

    private var connectedListener: ValueEventListener? = null
    private var allStatusListener: ChildEventListener? = null
    private var currentTrackingUid: String? = null

    companion object {
        private const val TAG = "PresenceManager"
        private const val LOG_TAG_DISCOVERY = "PeerDiscovery:RTDB"
    }

    /**
     * Inicia el rastreo de presencia para el usuario autenticado.
     * Configura el hook `onDisconnect()` en Firebase Realtime Database para garantizar que cuando
     * la app se cierre o pierda conexión, el estado 'isOnline: false' y 'lastSeen: timestamp'
     * se escriban automáticamente en el servidor.
     */
    fun startTrackingMyPresence(
        myUid: String,
        myDisplayName: String,
        myEmail: String,
        batteryPercent: Int = 100
    ) {
        if (myUid.isBlank()) {
            Log.w(TAG, "[$LOG_TAG_DISCOVERY] startTrackingMyPresence cancelado: UID vacío")
            return
        }
        currentTrackingUid = myUid
        Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🚀 Iniciando tracking de presencia para UID=$myUid (Name='$myDisplayName', Email='$myEmail', Bat=$batteryPercent%)")

        try {
            val connectedRef = database.getReference(".info/connected")
            val myStatusRef = database.getReference("status/$myUid")

            connectedListener?.let { connectedRef.removeEventListener(it) }

            connectedListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isConnected = snapshot.getValue(Boolean::class.java) ?: false
                    Log.d(TAG, "[$LOG_TAG_DISCOVERY] ⚡ Conexión a Realtime Database (.info/connected): isConnected=$isConnected")
                    DiscoveryLogCollector.logRtdbPresence(
                        action = "CONNECTED_STATE",
                        message = if (isConnected) "Conectado a Firebase Realtime Database (.info/connected = true)" else "Desconectado de Firebase RTDB (.info/connected = false)",
                        severity = if (isConnected) LogSeverity.SUCCESS else LogSeverity.WARNING
                    )

                    if (isConnected) {
                        // 1. Configurar hook onDisconnect en Realtime Database
                        val offlinePayload = HashMap<String, Any>().apply {
                            put("isOnline", false)
                            put("lastSeen", ServerValue.TIMESTAMP)
                            put("uid", myUid)
                            put("displayName", myDisplayName)
                            put("email", myEmail)
                        }

                        myStatusRef.onDisconnect().setValue(offlinePayload).addOnSuccessListener {
                            Log.d(TAG, "[$LOG_TAG_DISCOVERY] ✅ Hook onDisconnect() registrado exitosamente en /status/$myUid")
                            DiscoveryLogCollector.logRtdbPresence(
                                action = "ON_DISCONNECT_HOOK",
                                message = "Hook onDisconnect() encolado en RTDB para /status/$myUid",
                                payload = "{ \"isOnline\": false, \"uid\": \"$myUid\" }",
                                severity = LogSeverity.SUCCESS
                            )
                        }.addOnFailureListener {
                            Log.w(TAG, "[$LOG_TAG_DISCOVERY] ❌ Error configurando onDisconnect hook: ${it.message}")
                            DiscoveryLogCollector.logRtdbPresence(
                                action = "ON_DISCONNECT_ERROR",
                                message = "Fallo al registrar hook onDisconnect: ${it.message}",
                                severity = LogSeverity.ERROR
                            )
                        }

                        // 2. Establecer estado ONLINE actual
                        val onlinePayload = HashMap<String, Any>().apply {
                            put("isOnline", true)
                            put("lastSeen", ServerValue.TIMESTAMP)
                            put("uid", myUid)
                            put("displayName", myDisplayName)
                            put("email", myEmail)
                            put("battery", batteryPercent)
                        }

                        myStatusRef.setValue(onlinePayload).addOnSuccessListener {
                            Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🟢 Presencia ONLINE registrada para $myUid en RTDB")
                            DiscoveryLogCollector.logRtdbPresence(
                                action = "PRESENCE_ONLINE_SET",
                                message = "Presencia ONLINE escrita en /status/$myUid (Batería: $batteryPercent%)",
                                payload = "{ \"uid\": \"$myUid\", \"displayName\": \"$myDisplayName\", \"isOnline\": true, \"battery\": $batteryPercent }",
                                severity = LogSeverity.SUCCESS
                            )
                        }.addOnFailureListener {
                            Log.w(TAG, "[$LOG_TAG_DISCOVERY] ❌ Error registrando presencia ONLINE en RTDB: ${it.message}")
                            DiscoveryLogCollector.logRtdbPresence(
                                action = "PRESENCE_WRITE_ERROR",
                                message = "Error escribiendo /status/$myUid: ${it.message}",
                                severity = LogSeverity.ERROR
                            )
                        }

                        // 3. Sincronizar también con Firestore para redundancia y búsqueda de contactos
                        firestore.collection("users").document(myUid).set(
                            mapOf(
                                "uid" to myUid,
                                "displayName" to myDisplayName,
                                "email" to myEmail,
                                "isOnline" to true,
                                "lastSeenTimestamp" to System.currentTimeMillis(),
                                "batteryPercent" to batteryPercent
                            ),
                            SetOptions.merge()
                        ).addOnSuccessListener {
                            Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🟢 Presencia sincronizada con Firestore /users/$myUid")
                            DiscoveryLogCollector.logFirestorePresence(
                                action = "PRESENCE_SYNC",
                                message = "Presencia sincronizada con Firestore /users/$myUid",
                                payload = "{ \"isOnline\": true, \"batteryPercent\": $batteryPercent }",
                                severity = LogSeverity.SUCCESS
                            )
                        }.addOnFailureListener {
                            Log.w(TAG, "[$LOG_TAG_DISCOVERY] ⚠️ Error sincronizando presencia con Firestore: ${it.message}")
                        }
                    } else {
                        Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🔴 Dispositivo desconectado de Firebase Realtime Database")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "[$LOG_TAG_DISCOVERY] Listener .info/connected cancelado: ${error.message}")
                }
            }

            connectedRef.addValueEventListener(connectedListener!!)

            // Iniciar escucha del estado de todos los nodos en tiempo real
            listenToAllUsersPresence()

        } catch (e: Exception) {
            Log.e(TAG, "[$LOG_TAG_DISCOVERY] Error iniciando tracking de presencia: ${e.message}", e)
        }
    }

    /**
     * Escucha en tiempo real el nodo /status de Firebase Realtime Database para reflejar
     * el estado 'en línea / última vez' y auto-descubrir nuevos dispositivos en Room.
     */
    private fun listenToAllUsersPresence() {
        try {
            val statusRootRef = database.getReference("status")

            allStatusListener?.let { statusRootRef.removeEventListener(it) }

            allStatusListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d(TAG, "[$LOG_TAG_DISCOVERY] 📡 RTDB onChildAdded: ${snapshot.key}")
                    handleStatusChange(snapshot, isNew = true)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🔄 RTDB onChildChanged: ${snapshot.key}")
                    handleStatusChange(snapshot, isNew = false)
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val uid = snapshot.key ?: return
                    Log.d(TAG, "[$LOG_TAG_DISCOVERY] ❌ RTDB onChildRemoved: $uid")
                    val currentMap = _onlineUsersMap.value.toMutableMap()
                    currentMap.remove(uid)
                    _onlineUsersMap.value = currentMap

                    scope.launch {
                        contactDao.updatePresence(uid, false, System.currentTimeMillis())
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "[$LOG_TAG_DISCOVERY] Error escuchando /status en RTDB: ${error.message}")
                }
            }

            statusRootRef.addChildEventListener(allStatusListener!!)
            Log.d(TAG, "[$LOG_TAG_DISCOVERY] ✅ Listener global RTDB /status registrado exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "[$LOG_TAG_DISCOVERY] Error suscribiendo al nodo status de RTDB: ${e.message}", e)
        }
    }

    private fun handleStatusChange(snapshot: DataSnapshot, isNew: Boolean) {
        try {
            val uid = snapshot.key ?: return
            val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
            val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: System.currentTimeMillis()
            val battery = snapshot.child("battery").getValue(Int::class.java) ?: 85
            val displayName = snapshot.child("displayName").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: ""
            val fcmToken = snapshot.child("fcmToken").getValue(String::class.java) ?: ""

            val status = UserPresenceStatus(
                uid = uid,
                displayName = displayName,
                email = email,
                isOnline = isOnline,
                lastSeen = lastSeen,
                battery = battery,
                fcmToken = fcmToken
            )

            val currentMap = _onlineUsersMap.value.toMutableMap()
            currentMap[uid] = status
            _onlineUsersMap.value = currentMap

            Log.d(TAG, "[$LOG_TAG_DISCOVERY] 📍 Presencia procesada: UID=$uid, Name='$displayName', Online=$isOnline, Bat=$battery%, LastSeen=$lastSeen")

            // Actualizar o crear Contacto en Room Database inmediatamente para auto-descubrimiento instantáneo
            if (uid != currentTrackingUid && uid.isNotBlank()) {
                scope.launch {
                    val existing = contactDao.getContactDirect(uid)
                    if (existing != null) {
                        contactDao.updateFullPresence(uid, isOnline, lastSeen, battery)
                        if (displayName.isNotBlank() && (existing.alias.isBlank() || existing.alias.startsWith("Operador"))) {
                            contactDao.updateContact(existing.copy(alias = displayName, email = email.ifBlank { existing.email }))
                        }
                    } else {
                        // Nuevo dispositivo descubierto mediante RTDB
                        val newContact = ContactEntity(
                            deviceId = uid,
                            alias = displayName.ifBlank { if (email.isNotBlank()) email.substringBefore("@") else "Operador-${uid.takeLast(4)}" },
                            email = email.ifBlank { "$uid@tactical.omni" },
                            avatarIcon = "🛡️",
                            photoUrl = "",
                            lastKnownLatitude = 0.0,
                            lastKnownLongitude = 0.0,
                            lastSeenTimestamp = lastSeen,
                            isOnline = isOnline,
                            isTrustedNode = true,
                            isPriority = false,
                            batteryPercent = battery,
                            publicKeyFingerprint = "SHA256:" + uid.hashCode().toString(16).take(8).uppercase(),
                            rawPublicKeyBase64 = "",
                            preferredMedium = ConnectionType.CLOUD_RELAY.label
                        )
                        contactDao.insertContact(newContact)
                        Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🌟 ¡Nuevo dispositivo auto-descubierto e insertado en Room!: $uid ($displayName)")
                        DiscoveryLogCollector.logRtdbPresence(
                            action = "PEER_DISCOVERED_RTDB",
                            message = "¡Nuevo dispositivo descubierto e insertado en Room!: $uid ($displayName)",
                            payload = "{ \"uid\": \"$uid\", \"name\": \"$displayName\", \"email\": \"$email\", \"isOnline\": $isOnline }",
                            severity = LogSeverity.SUCCESS
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[$LOG_TAG_DISCOVERY] Error parseando cambio de presencia de nodo: ${e.message}")
        }
    }

    /**
     * Establece explícitamente el estado OFFLINE al cerrar sesión o salir de la app
     */
    fun setExplicitOffline(myUid: String) {
        if (myUid.isBlank()) return
        Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🔌 Configurando estado explícito OFFLINE para UID=$myUid")
        try {
            val myStatusRef = database.getReference("status/$myUid")
            val offlinePayload = HashMap<String, Any>().apply {
                put("isOnline", false)
                put("lastSeen", ServerValue.TIMESTAMP)
                put("uid", myUid)
            }
            myStatusRef.setValue(offlinePayload)

            firestore.collection("users").document(myUid).set(
                mapOf(
                    "isOnline" to false,
                    "lastSeenTimestamp" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
        } catch (e: Exception) {
            Log.w(TAG, "[$LOG_TAG_DISCOVERY] Error configurando offline explícito: ${e.message}")
        }
    }

    fun stopTracking() {
        Log.d(TAG, "[$LOG_TAG_DISCOVERY] Deteniendo listeners de presencia")
        try {
            connectedListener?.let { database.getReference(".info/connected").removeEventListener(it) }
            allStatusListener?.let { database.getReference("status").removeEventListener(it) }
        } catch (e: Exception) {
            Log.w(TAG, "[$LOG_TAG_DISCOVERY] Error al detener listeners de presencia: ${e.message}")
        }
    }
}

