package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.models.AuthUiState
import com.example.domain.models.UserProfile
import com.example.domain.security.FirebaseAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = FirebaseAuthManager(application.applicationContext)

    val currentUserProfile: StateFlow<UserProfile?> = authManager.currentUserProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        checkInitialAuthState()
    }

    private fun checkInitialAuthState() {
        val user = authManager.currentFirebaseUser
        if (user != null) {
            _uiState.value = AuthUiState.Authenticated(
                UserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: "Operador Táctico",
                    email = user.email ?: "",
                    photoUrl = user.photoUrl?.toString() ?: "",
                    isOnline = true
                )
            )
        } else {
            _uiState.value = AuthUiState.Unauthenticated()
        }
    }

    /**
     * Inicia el flujo de autenticación de Google con Android Jetpack CredentialManager
     */
    fun signInWithGoogle(activityContext: Context, webClientId: String = "") {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _errorMessage.value = null
            
            val result = authManager.signInWithGoogle(activityContext, webClientId)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = AuthUiState.Authenticated(profile)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Unauthenticated(error.message)
                    _errorMessage.value = error.localizedMessage ?: "Fallo en Google Sign-In"
                }
            )
        }
    }

    /**
     * Inicia sesión con Correo y Contraseña
     */
    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Por favor ingresa correo y contraseña"
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _errorMessage.value = null
            
            val result = authManager.signInWithEmail(email, pass)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = AuthUiState.Authenticated(profile)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Unauthenticated(error.message)
                    _errorMessage.value = error.localizedMessage ?: "Credenciales inválidas"
                }
            )
        }
    }

    /**
     * Registra un nuevo operador táctico con Correo y Contraseña
     */
    fun signUpWithEmail(displayName: String, email: String, pass: String) {
        if (displayName.isBlank() || email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Por favor completa todos los campos requeridos"
            return
        }
        if (pass.length < 6) {
            _errorMessage.value = "La clave debe contener al menos 6 caracteres"
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            _errorMessage.value = null
            
            val result = authManager.signUpWithEmail(displayName, email, pass)
            result.fold(
                onSuccess = { profile ->
                    _uiState.value = AuthUiState.Authenticated(profile)
                },
                onFailure = { error ->
                    _uiState.value = AuthUiState.Unauthenticated(error.message)
                    _errorMessage.value = error.localizedMessage ?: "Error al registrar cuenta"
                }
            )
        }
    }

    /**
     * Cierra la sesión de usuario
     */
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _uiState.value = AuthUiState.Unauthenticated()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
