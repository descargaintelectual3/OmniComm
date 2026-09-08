package com.example.domain.contacts

import android.content.Context
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.dao.ContactDao
import com.example.domain.local.entities.ContactEntity
import com.example.domain.models.TacticalNotificationPayload
import com.example.domain.models.UserProfile
import com.example.domain.presence.PresenceManager
import com.example.services.OmniPushService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ContactRepository(
    private val context: Context,
    private val contactDao: ContactDao = OmniDatabase.getDatabase(context).contactDao(),
    val presenceManager: PresenceManager = PresenceManager(context, contactDao)
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "ContactRepository"
        private const val LOG_TAG_SYNC = "ContactRepo:Sync"
        private const val LOG_TAG_DISCOVERY = "PeerDiscovery:Firestore"
    }

    private var allUsersListener: ListenerRegistration? = null
    private var tacticalNodesListener: ListenerRegistration? = null
    private var myContactsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null

    /**
     * Flujo reactivo de contactos locales cacheados en Room (SQLCipher)
     */
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val onlineContacts: Flow<List<ContactEntity>> = contactDao.getOnlineContacts()
    val priorityContacts: Flow<List<ContactEntity>> = contactDao.getPriorityContacts()

    fun searchContacts(query: String): Flow<List<ContactEntity>> {
        return if (query.isBlank()) {
            allContacts
        } else {
            contactDao.searchContacts(query.trim())
        }
    }

    /**
     * Inicia la sincronización en tiempo real con Firestore y Firebase Realtime Database.
     * Escucha la presencia y estado de todos los nodos y contactos, actualizando la base de datos Room.
     */
    fun startRealtimeSynchronization(
        myUid: String,
        myDisplayName: String = "",
        myEmail: String = "",
        batteryPercent: Int = 100
    ) {
        stopRealtimeSynchronization()
        Log.d(TAG, "[$LOG_TAG_SYNC] 🚀 Iniciando sincronización en tiempo real para UID=$myUid ($myDisplayName)")

        // 1. Iniciar el tracking de presencia con Realtime Database y onDisconnect() hooks
        if (myUid.isNotBlank()) {
            Log.d(TAG, "[$LOG_TAG_SYNC] Conectando PresenceManager con Realtime Database...")
            presenceManager.startTrackingMyPresence(
                myUid = myUid,
                myDisplayName = myDisplayName,
                myEmail = myEmail,
                batteryPercent = batteryPercent
            )
            // Sincronizar token FCM
            OmniPushService.registerCurrentToken(myUid)
        }

        // 2. Escuchar la red de usuarios registrados en Firestore (/users)
        Log.d(TAG, "[$LOG_TAG_DISCOVERY] 📡 Suscribiendo listener en tiempo real a colección Firestore /users...")
        allUsersListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "[$LOG_TAG_DISCOVERY] ❌ Error escuchando usuarios en Firestore: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d(TAG, "[$LOG_TAG_DISCOVERY] 📥 Snapshot de /users recibido: ${snapshot.size()} documentos.")
                    scope.launch(Dispatchers.IO) {
                        val contactEntities = snapshot.documents.mapNotNull { doc ->
                            val user = doc.toObject(UserProfile::class.java)
                            if (user != null && user.uid != myUid && user.uid.isNotBlank()) {
                                // Consultar si el Realtime Database tiene un estado más fresco
                                val rtdbStatus = presenceManager.onlineUsersMap.value[user.uid]
                                val liveIsOnline = rtdbStatus?.isOnline ?: user.isOnline
                                val liveLastSeen = rtdbStatus?.lastSeen ?: user.lastSeenTimestamp
                                val liveBattery = rtdbStatus?.battery ?: user.batteryPercent

                                Log.d(TAG, "[$LOG_TAG_DISCOVERY] 👤 Par detectado en /users: UID=${user.uid}, Name='${user.displayName}', Online=$liveIsOnline, Bat=$liveBattery%, Fingerprint=${user.publicKeyFingerprint.take(12)}...")

                                ContactEntity(
                                    deviceId = user.uid,
                                    alias = user.displayName.ifBlank { user.email.substringBefore("@") },
                                    email = user.email,
                                    avatarIcon = "🛡️",
                                    photoUrl = user.photoUrl,
                                    lastKnownLatitude = 0.0,
                                    lastKnownLongitude = 0.0,
                                    lastSeenTimestamp = liveLastSeen,
                                    isOnline = liveIsOnline,
                                    isTrustedNode = true,
                                    isPriority = false,
                                    batteryPercent = liveBattery,
                                    publicKeyFingerprint = user.publicKeyFingerprint,
                                    rawPublicKeyBase64 = user.rawPublicKeyBase64,
                                    preferredMedium = "CLOUD_RELAY + RFCOMM"
                                )
                            } else null
                        }

                        if (contactEntities.isNotEmpty()) {
                            contactDao.insertContacts(contactEntities)
                            Log.d(TAG, "[$LOG_TAG_DISCOVERY] ✅ ${contactEntities.size} contactos sincronizados e insertados en Room")
                        }
                    }
                }
            }

        // 2b. Escuchar el pool de nodos tácticos (/tactical_nodes) para descubrir dispositivos anónimos/locales
        Log.d(TAG, "[$LOG_TAG_DISCOVERY] 📡 Suscribiendo listener a colección Firestore /tactical_nodes...")
        tacticalNodesListener = firestore.collection("tactical_nodes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "[$LOG_TAG_DISCOVERY] ⚠️ Error en /tactical_nodes: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val nodeId = doc.getString("nodeId") ?: doc.id
                            if (nodeId.isNotBlank() && nodeId != myUid) {
                                val displayName = doc.getString("displayName") ?: "Operador ${doc.getString("model") ?: "Táctico"}"
                                val isOnline = doc.getBoolean("isOnline") ?: true
                                val battery = (doc.getLong("batteryPercent") ?: 90L).toInt()
                                val lastSeen = doc.getLong("lastSeenTimestamp") ?: System.currentTimeMillis()
                                val fingerprint = doc.getString("publicKeyFingerprint") ?: ""
                                val publicKeyBase64 = doc.getString("rawPublicKeyBase64") ?: ""

                                val existing = contactDao.getContactDirect(nodeId)
                                if (existing == null) {
                                    val newContact = ContactEntity(
                                        deviceId = nodeId,
                                        alias = displayName,
                                        email = "$nodeId@tactical.omni",
                                        avatarIcon = "🛡️",
                                        photoUrl = "",
                                        lastKnownLatitude = 0.0,
                                        lastKnownLongitude = 0.0,
                                        lastSeenTimestamp = lastSeen,
                                        isOnline = isOnline,
                                        isTrustedNode = true,
                                        isPriority = false,
                                        batteryPercent = battery,
                                        publicKeyFingerprint = fingerprint.ifBlank { "SHA256:" + nodeId.hashCode().toString(16).take(8).uppercase() },
                                        rawPublicKeyBase64 = publicKeyBase64,
                                        preferredMedium = "CLOUD_RELAY + LAN_P2P"
                                    )
                                    contactDao.insertContact(newContact)
                                    Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🌟 Nuevo nodo táctico descubierto desde /tactical_nodes: $nodeId ($displayName)")
                                } else {
                                    contactDao.updateFullPresence(nodeId, isOnline, lastSeen, battery)
                                }
                            }
                        }
                    }
                }
            }

        // 3. Escuchar la lista de contactos específicos agregados por el usuario
        if (myUid.isNotBlank()) {
            myContactsListener = firestore.collection("users")
                .document(myUid)
                .collection("contacts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[$LOG_TAG_SYNC] Error escuchando contactos específicos: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                val isPriority = doc.getBoolean("isPriority") ?: false
                                val aliasOverride = doc.getString("alias")
                                val contactId = doc.id

                                val existing = contactDao.getContactDirect(contactId)
                                if (existing != null) {
                                    contactDao.updateContact(
                                        existing.copy(
                                            isPriority = isPriority,
                                            alias = aliasOverride ?: existing.alias
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

            // 4. Escuchar notificaciones entrantes de solicitudes de contacto en tiempo real
            notificationsListener = firestore.collection("users")
                .document(myUid)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "[$LOG_TAG_SYNC] Error escuchando notificaciones: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        for (doc in snapshot.documents) {
                            val notif = doc.toObject(TacticalNotificationPayload::class.java)
                            if (notif != null && notif.type == "CONTACT_REQUEST") {
                                Log.d(TAG, "[$LOG_TAG_SYNC] 🔔 Notificación de solicitud de contacto recibida de: ${notif.senderName}")
                                OmniPushService.showLocalNotification(
                                    context = context,
                                    title = notif.title.ifBlank { "Nueva Solicitud de Contacto" },
                                    body = notif.body.ifBlank { "${notif.senderName} te ha agregado a su red de contactos tácticos." },
                                    channelId = OmniPushService.CHANNEL_CONTACTS_ID,
                                    targetScreen = "contacts"
                                )
                                // Marcar como leída
                                doc.reference.update("isRead", true)
                            }
                        }
                    }
                }
        }
    }

    /**
     * Agrega un nuevo contacto buscando en Firestore por correo electrónico o identificador táctico
     * y envía una notificación push al usuario destinatario.
     */
    suspend fun addContact(
        myUid: String,
        myDisplayName: String = "",
        query: String,
        customAlias: String? = null
    ): Result<ContactEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim()
            if (cleanQuery.isBlank()) {
                return@withContext Result.failure(Exception("Debe ingresar un correo o ID válido"))
            }

            // Buscar por email o por uid en Firestore
            val emailQuerySnapshot = firestore.collection("users")
                .whereEqualTo("email", cleanQuery)
                .limit(1)
                .get()
                .await()

            val userDoc = if (!emailQuerySnapshot.isEmpty) {
                emailQuerySnapshot.documents.first()
            } else {
                val uidDoc = firestore.collection("users").document(cleanQuery).get().await()
                if (uidDoc.exists()) uidDoc else null
            }

            if (userDoc == null) {
                return@withContext Result.failure(Exception("No se encontró ningún nodo o usuario registrado con '$query'"))
            }

            val user = userDoc.toObject(UserProfile::class.java) 
                ?: return@withContext Result.failure(Exception("Error al parsear el nodo de usuario"))

            if (user.uid == myUid) {
                return@withContext Result.failure(Exception("No puedes agregarte a ti mismo como contacto"))
            }

            val finalAlias = customAlias?.ifBlank { null } 
                ?: user.displayName.ifBlank { user.email.substringBefore("@") }

            val contactEntity = ContactEntity(
                deviceId = user.uid,
                alias = finalAlias,
                email = user.email,
                avatarIcon = "🛡️",
                photoUrl = user.photoUrl,
                lastSeenTimestamp = user.lastSeenTimestamp,
                isOnline = user.isOnline,
                isTrustedNode = true,
                isPriority = false,
                batteryPercent = user.batteryPercent,
                publicKeyFingerprint = user.publicKeyFingerprint,
                rawPublicKeyBase64 = user.rawPublicKeyBase64,
                preferredMedium = "DUAL_RADIO"
            )

            // 1. Guardar en Room local
            contactDao.insertContact(contactEntity)

            // 2. Guardar en Firestore subcolección del usuario
            if (myUid.isNotBlank()) {
                firestore.collection("users")
                    .document(myUid)
                    .collection("contacts")
                    .document(user.uid)
                    .set(
                        mapOf(
                            "contactUid" to user.uid,
                            "alias" to finalAlias,
                            "email" to user.email,
                            "isPriority" to false,
                            "addedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                    .await()

                // 3. Enviar notificación push al destinatario
                val notifId = UUID.randomUUID().toString()
                val notification = TacticalNotificationPayload(
                    id = notifId,
                    targetUid = user.uid,
                    senderUid = myUid,
                    senderName = myDisplayName.ifBlank { "Operador Táctico" },
                    title = "📡 Nueva Vinculación de Nodo",
                    body = "${myDisplayName.ifBlank { "Un operador" }} ha añadido tu nodo a sus enlaces seguros.",
                    type = "CONTACT_REQUEST",
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )

                firestore.collection("users")
                    .document(user.uid)
                    .collection("notifications")
                    .document(notifId)
                    .set(notification)
            }

            Result.success(contactEntity)
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error agregando contacto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Alterna la prioridad de un contacto (Pin / Favorito)
     */
    suspend fun togglePriority(myUid: String, contactId: String, currentPriority: Boolean) = withContext(Dispatchers.IO) {
        val newPriority = !currentPriority
        contactDao.setNodePriority(contactId, newPriority)

        if (myUid.isNotBlank()) {
            try {
                firestore.collection("users")
                    .document(myUid)
                    .collection("contacts")
                    .document(contactId)
                    .set(mapOf("isPriority" to newPriority), SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w("ContactRepository", "No se pudo actualizar prioridad en Firestore: ${e.message}")
            }
        }
    }

    /**
     * Elimina un contacto de la lista local y remota
     */
    suspend fun deleteContact(myUid: String, contactId: String) = withContext(Dispatchers.IO) {
        contactDao.deleteContactById(contactId)

        if (myUid.isNotBlank()) {
            try {
                firestore.collection("users")
                    .document(myUid)
                    .collection("contacts")
                    .document(contactId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w("ContactRepository", "Error eliminando contacto de Firestore: ${e.message}")
            }
        }
    }

    fun stopRealtimeSynchronization() {
        Log.d(TAG, "[$LOG_TAG_SYNC] Deteniendo sincronización en tiempo real de contactos y presencia")
        allUsersListener?.remove()
        allUsersListener = null
        tacticalNodesListener?.remove()
        tacticalNodesListener = null
        myContactsListener?.remove()
        myContactsListener = null
        notificationsListener?.remove()
        notificationsListener = null
        presenceManager.stopTracking()
    }

    /**
     * Flujo de contactos con llaves públicas y bundles cifrados para E2EE
     */
    val contactsWithPublicKeys: Flow<List<ContactEntity>> = contactDao.getContactsWithPublicKeys()

    /**
     * Guarda o actualiza un contacto con su clave pública y paquete cifrado E2EE
     */
    suspend fun saveContactWithEncryptedPublicKey(
        deviceId: String,
        alias: String,
        email: String = "",
        rawPublicKey: String,
        encryptedKeyBundle: String = "",
        fingerprint: String,
        algorithm: String = "RSA-2048",
        avatarIcon: String = "🛡️",
        isTrustedNode: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val existing = contactDao.getContactDirect(deviceId)
        val entity = ContactEntity(
            deviceId = deviceId,
            alias = alias,
            email = email,
            avatarIcon = avatarIcon,
            photoUrl = existing?.photoUrl ?: "",
            lastKnownLatitude = existing?.lastKnownLatitude ?: 0.0,
            lastKnownLongitude = existing?.lastKnownLongitude ?: 0.0,
            lastSeenTimestamp = System.currentTimeMillis(),
            isOnline = existing?.isOnline ?: true,
            isTrustedNode = isTrustedNode,
            isPriority = existing?.isPriority ?: false,
            accessCount = (existing?.accessCount ?: 0) + 1,
            batteryPercent = existing?.batteryPercent ?: 95,
            publicKeyFingerprint = fingerprint,
            rawPublicKeyBase64 = rawPublicKey,
            preferredMedium = "DUAL_RADIO",
            encryptedPublicKey = encryptedKeyBundle.ifBlank { rawPublicKey },
            keyAlgorithm = algorithm,
            isKeyVerified = true,
            keyExchangeTimestamp = System.currentTimeMillis(),
            e2eeProtocolVersion = "v2.4"
        )
        contactDao.insertContact(entity)
        Log.d(TAG, "Contacto E2EE registrado con éxito: $deviceId ($alias) [Algo: $algorithm]")
    }

    /**
     * Obtiene la clave pública para un contacto determinado
     */
    suspend fun getContactPublicKey(deviceId: String): String? = withContext(Dispatchers.IO) {
        contactDao.getPublicKeyForContact(deviceId)
    }

    /**
     * Obtiene el paquete de clave pública cifrada de un contacto
     */
    suspend fun getContactEncryptedKey(deviceId: String): String? = withContext(Dispatchers.IO) {
        contactDao.getEncryptedPublicKeyForContact(deviceId)
    }

    /**
     * Actualiza la clave pública y el fingerprint de un contacto tras una rotación de llaves
     */
    suspend fun updateContactPublicKey(
        deviceId: String,
        rawPublicKey: String,
        encryptedKey: String,
        fingerprint: String,
        algorithm: String = "RSA-2048"
    ) = withContext(Dispatchers.IO) {
        contactDao.updateContactPublicKey(
            deviceId = deviceId,
            rawKey = rawPublicKey,
            encryptedKey = encryptedKey,
            fingerprint = fingerprint,
            algorithm = algorithm,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Verifica la huella digital criptográfica out-of-band de un contacto
     */
    suspend fun verifyContactFingerprint(deviceId: String, expectedFingerprint: String): Boolean = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactDirect(deviceId) ?: return@withContext false
        val matches = contact.publicKeyFingerprint.equals(expectedFingerprint.trim(), ignoreCase = true)
        if (matches) {
            contactDao.insertContact(contact.copy(isKeyVerified = true))
        }
        matches
    }

    /**
     * Inicializa contactos tácticos con pares de claves E2EE si la base de datos está vacía
     */
    suspend fun seedInitialTacticalE2eeContacts() = withContext(Dispatchers.IO) {
        val count = contactDao.getPriorityScanNodes().size
        if (count == 0) {
            val defaultNodes = listOf(
                ContactEntity(
                    deviceId = "NODE_HQ_TACTICAL",
                    alias = "Comando Central HQ",
                    email = "hq@omnicomm.mesh",
                    avatarIcon = "🏢",
                    isOnline = true,
                    isTrustedNode = true,
                    isPriority = true,
                    batteryPercent = 100,
                    publicKeyFingerprint = "SHA256:4A:78:E2:B1:09:9C:34:5F:62:00:81:AA:5D:F3:11:02",
                    rawPublicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxK01H9Q/hq_tactical_key_sample_alpha_root==",
                    encryptedPublicKey = "ENC_PK_BUNDLE_HQ_TACTICAL_AES_GCM_PAYLOAD_001",
                    keyAlgorithm = "RSA-2048",
                    isKeyVerified = true,
                    keyExchangeTimestamp = System.currentTimeMillis()
                ),
                ContactEntity(
                    deviceId = "NODE_ALPHA_01",
                    alias = "Unidad Alfa 01",
                    email = "alfa1@omnicomm.mesh",
                    avatarIcon = "⚡",
                    isOnline = true,
                    isTrustedNode = true,
                    isPriority = true,
                    batteryPercent = 88,
                    publicKeyFingerprint = "SHA256:BC:55:12:F0:99:A1:74:3C:80:DF:11:43:2A:7B:66:81",
                    rawPublicKeyBase64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzV90P4k/alfa_unit_primary_key_bundle_4096==",
                    encryptedPublicKey = "ENC_PK_BUNDLE_ALPHA_01_AES_GCM_PAYLOAD_002",
                    keyAlgorithm = "RSA-2048",
                    isKeyVerified = true,
                    keyExchangeTimestamp = System.currentTimeMillis()
                ),
                ContactEntity(
                    deviceId = "NODE_BRAVO_02",
                    alias = "Explorador Bravo 02",
                    email = "bravo2@omnicomm.mesh",
                    avatarIcon = "🎯",
                    isOnline = true,
                    isTrustedNode = true,
                    isPriority = false,
                    batteryPercent = 74,
                    publicKeyFingerprint = "SHA256:71:0D:33:A8:E5:1B:CC:90:42:05:89:1E:44:AC:EE:19",
                    rawPublicKeyBase64 = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9f/bravo_ecc_p256_public_key_stream_bundle==",
                    encryptedPublicKey = "ENC_PK_BUNDLE_BRAVO_02_AES_GCM_PAYLOAD_003",
                    keyAlgorithm = "ECDH-P256",
                    isKeyVerified = true,
                    keyExchangeTimestamp = System.currentTimeMillis()
                )
            )
            contactDao.insertContacts(defaultNodes)
            Log.d(TAG, "Contactos iniciales E2EE precargados en Room SQLCipher con éxito.")
        }
    }
}
