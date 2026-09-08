package com.example.domain.security

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogSeverity
import com.example.domain.models.UserProfile
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthManager(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    companion object {
        private const val TAG = "FirebaseAuthManager"
        private const val LOG_TAG_AUTH = "FirebaseAuthHandshake"
        private const val LOG_TAG_DISCOVERY = "PeerDiscovery:Auth"
    }

    init {
        // Observar cambios de autenticación
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.d(TAG, "[$LOG_TAG_AUTH] 🔑 AuthStateListener disparado: user=${user?.uid ?: "NULL (Sin sesión)"}, email=${user?.email ?: "N/A"}")
            DiscoveryLogCollector.logAuthHandshake(
                step = "AUTH_STATE_CHANGE",
                message = if (user != null) "Sesión activa detectada para UID=${user.uid} (${user.email ?: "Sin email"})" else "Sin sesión activa / Usuario desautenticado",
                payload = user?.let { "{ \"uid\": \"${it.uid}\", \"email\": \"${it.email}\", \"displayName\": \"${it.displayName}\", \"isAnonymous\": ${it.isAnonymous} }" },
                severity = if (user != null) LogSeverity.SUCCESS else LogSeverity.INFO
            )
            if (user != null) {
                scope.launch {
                    Log.d(TAG, "[$LOG_TAG_AUTH] Iniciando sincronización de handshake inicial para UID=${user.uid}")
                    syncUserProfileToFirestore(user)
                    listenToUserProfile(user.uid)
                }
            } else {
                Log.d(TAG, "[$LOG_TAG_AUTH] Estado actual: Usuario desautenticado")
                _currentUserProfile.value = null
            }
        }
    }

    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Iniciar sesión con Google a través de Android Jetpack CredentialManager
     */
    suspend fun signInWithGoogle(activityContext: Context, webClientId: String = ""): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "[$LOG_TAG_AUTH] 🛡️ Paso 1: Iniciando flujo de Google Sign-In con CredentialManager...")
            val credentialManager = CredentialManager.create(activityContext)
            
            val clientId = if (webClientId.isNotBlank()) {
                webClientId
            } else {
                // Fallback a client ID de Firebase si existe en el entorno
                "27856452583-omnicomm.apps.googleusercontent.com"
            }

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(clientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            Log.d(TAG, "[$LOG_TAG_AUTH] 🛡️ Paso 2: Solicitando credenciales de Google al sistema...")
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Log.d(TAG, "[$LOG_TAG_AUTH] 🛡️ Paso 3: Credencial ID Token de Google obtenida. Autenticando con Firebase...")
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw Exception("Error al recuperar usuario autenticado")
                
                Log.d(TAG, "[$LOG_TAG_AUTH] 🛡️ Paso 4: Firebase Auth exitoso para UID=${user.uid}. Sincronizando perfil y claves criptográficas...")
                val profile = syncUserProfileToFirestore(user)
                Result.success(profile)
            } else {
                Log.w(TAG, "[$LOG_TAG_AUTH] ❌ Tipo de credencial de Google no soportada: ${credential::class.java.simpleName}")
                Result.failure(Exception("Tipo de credencial de Google no soportada"))
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "[$LOG_TAG_AUTH] Error parseando token de Google ID: ${e.message}", e)
            Result.failure(Exception("Error en credencial de Google: ${e.message}"))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "[$LOG_TAG_AUTH] Error en CredentialManager: ${e.message}", e)
            Result.failure(Exception("Autenticación cancelada o fallo de servicio: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "[$LOG_TAG_AUTH] Excepción durante Google Sign-In: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Iniciar sesión táctica con Correo y Contraseña
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "[$LOG_TAG_AUTH] 📧 Iniciando sesión con Correo: $email")
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Usuario no encontrado")
            Log.d(TAG, "[$LOG_TAG_AUTH] 📧 Autenticación exitosa con correo. UID=${user.uid}")
            val profile = syncUserProfileToFirestore(user)
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "[$LOG_TAG_AUTH] ❌ Error de inicio de sesión con correo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Registrar nuevo nodo operativo con Correo y Contraseña
     */
    suspend fun signUpWithEmail(displayName: String, email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "[$LOG_TAG_AUTH] 📝 Creando nueva cuenta táctica para $email ($displayName)")
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user ?: throw Exception("Error al crear cuenta")
            
            // Actualizar DisplayName en FirebaseAuth
            val updateReq = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName.trim())
                .build()
            user.updateProfile(updateReq).await()
            Log.d(TAG, "[$LOG_TAG_AUTH] 📝 Perfil de usuario creado con éxito. UID=${user.uid}")

            val profile = syncUserProfileToFirestore(user, explicitDisplayName = displayName.trim())
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "[$LOG_TAG_AUTH] ❌ Error al crear cuenta táctica: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza y sincroniza la identidad criptográfica y presencia del usuario en Firestore (/users/{uid})
     */
    suspend fun syncUserProfileToFirestore(
        user: FirebaseUser,
        explicitDisplayName: String? = null
    ): UserProfile = withContext(Dispatchers.IO) {
        Log.d(TAG, "[$LOG_TAG_AUTH] 🔐 [Paso 1/4] Extrayendo huella criptográfica RSA-2048...")
        val publicKeyFingerprint = cryptoManager.getPublicKeyFingerprint()
        val rawPublicKeyBase64 = cryptoManager.getPublicEncryptionKeyBase64()
        Log.d(TAG, "[$LOG_TAG_AUTH] 🔐 Huella digital pública obtenida: $publicKeyFingerprint")
        DiscoveryLogCollector.logAuthHandshake(
            step = "STEP_1_KEY_EXTRACT",
            message = "Clave criptográfica y huella RSA-2048 extraídas exitosamente: $publicKeyFingerprint",
            payload = "{ \"fingerprint\": \"$publicKeyFingerprint\", \"keyLength\": ${rawPublicKeyBase64.length} }",
            severity = LogSeverity.SUCCESS
        )

        val name = explicitDisplayName ?: user.displayName ?: user.email?.substringBefore("@") ?: "Operador Táctico"
        val email = user.email ?: ""
        val photoUrl = user.photoUrl?.toString() ?: ""

        val profile = UserProfile(
            uid = user.uid,
            displayName = name,
            email = email,
            photoUrl = photoUrl,
            publicKeyFingerprint = publicKeyFingerprint,
            rawPublicKeyBase64 = rawPublicKeyBase64,
            isOnline = true,
            lastSeenTimestamp = System.currentTimeMillis(),
            statusMessage = "Enlace E2EE Seguro Activo"
        )

        val startTime = System.currentTimeMillis()
        try {
            Log.d(TAG, "[$LOG_TAG_AUTH] ☁️ [Paso 2/4] Sincronizando documento a Firestore /users/${user.uid}...")
            firestore.collection("users").document(user.uid)
                .set(profile, SetOptions.merge())
                .await()
            _currentUserProfile.value = profile
            val syncDuration = System.currentTimeMillis() - startTime
            Log.d(TAG, "[$LOG_TAG_AUTH] ✅ [Paso 3/4] Documento /users/${user.uid} sincronizado exitosamente en Firestore ($syncDuration ms)")
            DiscoveryLogCollector.logAuthHandshake(
                step = "STEP_2_FIRESTORE_SYNC",
                message = "Perfil de usuario sincronizado en Firestore /users/${user.uid}",
                payload = "{ \"uid\": \"${user.uid}\", \"displayName\": \"$name\", \"email\": \"$email\", \"isOnline\": true }",
                severity = LogSeverity.SUCCESS,
                durationMs = syncDuration
            )

            // Actualizar también en el pool de nodos tácticos para auto-enlace
            Log.d(TAG, "[$LOG_TAG_AUTH] 📡 [Paso 4/4] Publicando en /tactical_nodes para auto-descubrimiento global de pares...")
            val nodePayload = mapOf(
                "nodeId" to user.uid,
                "displayName" to name,
                "avatar" to "🛡️",
                "model" to android.os.Build.MODEL,
                "publicKeyFingerprint" to publicKeyFingerprint,
                "rawPublicKeyBase64" to rawPublicKeyBase64,
                "batteryPercent" to 95,
                "isOnline" to true,
                "lastSeenTimestamp" to System.currentTimeMillis(),
                "preferredMedium" to "CLOUD_RELAY + LAN_P2P"
            )
            firestore.collection("tactical_nodes").document(user.uid).set(
                nodePayload,
                SetOptions.merge()
            ).await()
            Log.d(TAG, "[$LOG_TAG_DISCOVERY] 🌟 Nodo publicado en /tactical_nodes/${user.uid} para auto-descubrimiento inmediato")
            DiscoveryLogCollector.logAuthHandshake(
                step = "STEP_3_TACTICAL_NODE_ANNOUNCE",
                message = "Nodo táctico publicado en /tactical_nodes/${user.uid} para auto-enlace de malla",
                payload = "{ \"nodeId\": \"${user.uid}\", \"model\": \"${android.os.Build.MODEL}\", \"preferredMedium\": \"CLOUD_RELAY + LAN_P2P\" }",
                severity = LogSeverity.SUCCESS
            )
        } catch (e: Exception) {
            Log.w(TAG, "[$LOG_TAG_AUTH] ⚠️ Advertencia durante sincronización a Firestore: ${e.message}")
            DiscoveryLogCollector.logAuthHandshake(
                step = "HANDSHAKE_WARN",
                message = "Aviso durante sincronización de Firestore: ${e.message}",
                severity = LogSeverity.WARNING
            )
            _currentUserProfile.value = profile
        }

        profile
    }

    /**
     * Escucha en tiempo real cambios en el perfil del usuario actual desde Firestore
     */
    private fun listenToUserProfile(uid: String) {
        firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirebaseAuthManager", "Error observando perfil de usuario: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val userProfile = snapshot.toObject(UserProfile::class.java)
                    if (userProfile != null) {
                        _currentUserProfile.value = userProfile
                    }
                }
            }
    }

    /**
     * Actualiza el estado en línea / fuera de línea
     */
    suspend fun updatePresence(isOnline: Boolean) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        try {
            firestore.collection("users").document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeenTimestamp" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "Error actualizando presencia: ${e.message}")
        }
    }

    /**
     * Cierra la sesión de forma segura
     */
    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            updatePresence(false)
            auth.signOut()
            _currentUserProfile.value = null
        } catch (e: Exception) {
            Log.e("FirebaseAuthManager", "Error al cerrar sesión: ${e.message}", e)
            auth.signOut()
            _currentUserProfile.value = null
        }
    }
}
