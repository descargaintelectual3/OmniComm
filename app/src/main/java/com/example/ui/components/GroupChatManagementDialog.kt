package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.local.entities.ContactEntity
import com.example.domain.models.EncryptedChatSession
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

private val GROUP_ICONS = listOf("🛡️", "⚔️", "📡", "⚡", "🛸", "🔒", "🌐", "🎯")

/**
 * Diálogo para crear un nuevo Grupo de Chat Táctico Multi-Usuario
 */
@Composable
fun CreateGroupChatDialog(
    availableContacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onCreateGroup: (title: String, description: String, icon: String, selectedMembers: List<Pair<String, String>>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🛡️") }
    val selectedMembers = remember { mutableStateMapOf<String, String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("create_group_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = TacticalCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Crear Grupo Táctico Multi-Usuario",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selector de Icono Táctico
                Text("Icono de Escuadrón", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(GROUP_ICONS) { icon ->
                        val isSelected = selectedIcon == icon
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { selectedIcon = icon }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TacticalCyanPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(icon, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del Grupo / Escuadrón") },
                    placeholder = { Text("Ej: Operaciones Alfa 1") },
                    modifier = Modifier.fillMaxWidth().testTag("group_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción o Misión") },
                    placeholder = { Text("Ej: Coordinación táctica encriptada") },
                    modifier = Modifier.fillMaxWidth().testTag("group_desc_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "Seleccionar Operadores (${selectedMembers.size} seleccionados)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (availableContacts.isEmpty()) {
                    Text(
                        "No tienes contactos en tu agenda aún. Puedes crear el grupo e invitar después.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        items(availableContacts) { contact ->
                            val contactId = contact.deviceId
                            val contactName = contact.alias.ifBlank { contact.email.ifBlank { contact.deviceId } }
                            val isChecked = selectedMembers.containsKey(contactId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) {
                                            selectedMembers.remove(contactId)
                                        } else {
                                            selectedMembers[contactId] = contactName
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedMembers[contactId] = contactName
                                        } else {
                                            selectedMembers.remove(contactId)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        contactName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        contact.email.ifBlank { contact.deviceId },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onCreateGroup(
                                    title.trim(),
                                    description.trim(),
                                    selectedIcon,
                                    selectedMembers.map { (uid, name) -> Pair(uid, name) }
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.testTag("btn_confirm_create_group")
                    ) {
                        Text("Crear Grupo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo de Administración de Grupo (Permisos de Admin, Miembros, Promociones y Expulsión)
 */
@Composable
fun GroupInfoAndAdminDialog(
    groupSession: EncryptedChatSession,
    currentUserId: String,
    availableContacts: List<ContactEntity>,
    onDismiss: () -> Unit,
    onAddMember: (uid: String, name: String) -> Unit,
    onRemoveMember: (uid: String) -> Unit,
    onPromoteAdmin: (uid: String) -> Unit,
    onDemoteAdmin: (uid: String) -> Unit,
    onUpdateInfo: (title: String, desc: String, icon: String) -> Unit
) {
    val isAdmin = groupSession.adminUids.contains(currentUserId)
    var showAddMemberSection by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(groupSession.title) }
    var editDesc by remember { mutableStateOf(groupSession.groupDescription) }
    var isEditing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("group_admin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(groupSession.groupIcon.ifBlank { "🛡️" }, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                groupSession.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                if (isAdmin) "🛡️ Eres Administrador de este Grupo" else "👤 Miembro del Grupo Táctico",
                                fontSize = 11.sp,
                                color = if (isAdmin) TacticalEmeraldSecondary else TacticalCyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isEditing && isAdmin) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Nombre del Grupo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { isEditing = false }) { Text("Cancelar") }
                        Button(
                            onClick = {
                                onUpdateInfo(editTitle, editDesc, groupSession.groupIcon)
                                isEditing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
                        ) {
                            Text("Guardar Cambios", color = Color.Black)
                        }
                    }
                } else {
                    if (groupSession.groupDescription.isNotBlank()) {
                        Text(
                            text = groupSession.groupDescription,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isAdmin) {
                        TextButton(
                            onClick = { isEditing = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar información del grupo", fontSize = 11.sp)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Miembros (${groupSession.participantUids.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isAdmin) {
                        TextButton(
                            onClick = { showAddMemberSection = !showAddMemberSection },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Añadir", fontSize = 11.sp)
                        }
                    }
                }

                // Sección para añadir miembros disponibles
                if (showAddMemberSection && isAdmin) {
                    val availableToAdd = availableContacts.filter { !groupSession.participantUids.contains(it.deviceId) }
                    if (availableToAdd.isEmpty()) {
                        Text("Todos tus contactos ya están en este grupo.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                            items(availableToAdd) { contact ->
                                val contactName = contact.alias.ifBlank { contact.email.ifBlank { contact.deviceId } }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(contactName, fontSize = 12.sp)
                                    FilledTonalButton(
                                        onClick = {
                                            onAddMember(contact.deviceId, contactName)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Agregar", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                }

                // Lista de Miembros Actuales con Insignias de Admin
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(groupSession.participantUids) { uid ->
                        val memberName = groupSession.participantNames[uid] ?: if (uid == currentUserId) "Yo (Tú)" else "Operador"
                        val isMemberAdmin = groupSession.adminUids.contains(uid)
                        val isCreator = groupSession.createdByUid == uid

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isMemberAdmin) TacticalEmeraldSecondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        memberName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isMemberAdmin) TacticalEmeraldSecondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(memberName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (uid == currentUserId) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(Tú)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (isCreator) {
                                        Text("👑 Creador del Grupo", fontSize = 10.sp, color = TacticalAmberTertiary, fontWeight = FontWeight.Bold)
                                    } else if (isMemberAdmin) {
                                        Text("🛡️ Administrador", fontSize = 10.sp, color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Acciones de Administración si el usuario actual es Admin y no es sobre el creador principal
                            if (isAdmin && uid != currentUserId && !isCreator) {
                                Row {
                                    if (isMemberAdmin) {
                                        IconButton(onClick = { onDemoteAdmin(uid) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.RemoveModerator, contentDescription = "Degradar", tint = TacticalAmberTertiary, modifier = Modifier.size(16.dp))
                                        }
                                    } else {
                                        IconButton(onClick = { onPromoteAdmin(uid) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Default.AddModerator, contentDescription = "Hacer Admin", tint = TacticalEmeraldSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = { onRemoveMember(uid) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.PersonRemove, contentDescription = "Expulsar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
