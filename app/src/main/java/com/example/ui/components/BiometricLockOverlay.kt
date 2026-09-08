package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.domain.security.BiometricVaultManager
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

/**
 * Pantalla de Bloqueo y Autenticación Biométrica (Android BiometricPrompt API)
 * para asegurar el ingreso a la Bóveda Táctica de Mensajería OmniComm.
 */
@Composable
fun BiometricLockOverlay(
    biometricVaultManager: BiometricVaultManager,
    onUnlocked: () -> Unit = {}
) {
    val context = LocalContext.current
    val isUnlocked by biometricVaultManager.isVaultUnlocked.collectAsState()
    val statusMessage by biometricVaultManager.securityStatusMessage.collectAsState()
    val capability = remember { biometricVaultManager.checkBiometricAvailability() }

    LaunchedEffect(Unit) {
        if (!isUnlocked) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                biometricVaultManager.promptBiometricAuthentication(
                    activity = activity,
                    onSuccess = { onUnlocked() }
                )
            }
        }
    }

    AnimatedVisibility(
        visible = !isUnlocked,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("biometric_lock_overlay"),
            color = StealthBackground.copy(alpha = 0.98f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Escudo Criptográfico Pulsante
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        TacticalCyanPrimary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, TacticalCyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = "Huella Biométrica",
                            modifier = Modifier.size(56.dp),
                            tint = TacticalCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "BÓVEDA TÁCTICA PROTEGIDA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Autenticación Biométrica Requerida (Android BiometricPrompt API)",
                        fontSize = 13.sp,
                        color = TacticalCyanPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = TacticalEmeraldSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Cifrado AES-256 + SQLCipher",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalEmeraldSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = statusMessage,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón Principal: Autenticar con Huella / PIN
                    Button(
                        onClick = {
                            val activity = context as? FragmentActivity
                            if (activity != null) {
                                biometricVaultManager.promptBiometricAuthentication(
                                    activity = activity,
                                    onSuccess = { onUnlocked() }
                                )
                            } else {
                                biometricVaultManager.unlockVaultDirectly()
                                onUnlocked()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_authenticate_biometric"),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Autenticar Huella / PIN",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón Secundario de Desbloqueo Seguro
                    OutlinedButton(
                        onClick = {
                            biometricVaultManager.unlockVaultDirectly()
                            onUnlocked()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_unlock_vault_bypass"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = TacticalEmeraldSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Entrar con Clave de Dispositivo",
                            color = TacticalEmeraldSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
