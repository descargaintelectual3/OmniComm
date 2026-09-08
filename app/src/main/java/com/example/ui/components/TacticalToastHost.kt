package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.TacticalToastNotification
import com.example.domain.models.ToastType
import kotlinx.coroutines.delay

/**
 * Toast / Banner no intrusivo flotante con diseño militar táctico y animaciones fluidas.
 */
@Composable
fun TacticalToastHost(
    currentToast: TacticalToastNotification?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(currentToast) {
        if (currentToast != null) {
            delay(4200)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = currentToast != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        currentToast?.let { toast ->
            val (bgColor, accentColor, icon) = when (toast.type) {
                ToastType.WELCOME_HANDSHAKE -> Triple(
                    Color(0xFF064E3B),
                    Color(0xFF34D399),
                    Icons.Default.Security
                )
                ToastType.PRIORITY_DEVICE_LINKED -> Triple(
                    Color(0xFF451A03),
                    Color(0xFFFBBF24),
                    Icons.Default.Star
                )
                ToastType.SELF_HEALING_ACTIVATED -> Triple(
                    Color(0xFF4C1D95),
                    Color(0xFFA78BFA),
                    Icons.Default.Sync
                )
                ToastType.SYNC_ALL_COMPLETE -> Triple(
                    Color(0xFF1E3A8A),
                    Color(0xFF60A5FA),
                    Icons.Default.Security
                )
                ToastType.REMOTE_CONTROL_EXECUTED -> Triple(
                    Color(0xFF831843),
                    Color(0xFFF472B6),
                    Icons.Default.Notifications
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor.copy(alpha = 0.96f),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .testTag("tactical_toast_banner"),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(toast.avatarIcon, fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = toast.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = toast.message,
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 15.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
