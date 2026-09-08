package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.config.NavigationLayoutStyle
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import com.example.ui.theme.TacticalAmberTertiary

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

val PRIMARY_NAV_ITEMS = listOf(
    NavItem("contacts", "Contactos", Icons.Default.Contacts, "nav_item_contacts"),
    NavItem("chat", "Chat Activo", Icons.Default.Forum, "nav_item_chat"),
    NavItem("cloud_drive", "Cloud Vault", Icons.Default.Cloud, "nav_item_cloud_vault"),
    NavItem("radar", "Radar GIS", Icons.Default.Radar, "nav_item_radar"),
    NavItem("hub", "Mando", Icons.Default.Dashboard, "nav_item_hub")
)

@Composable
fun TacticalBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0D1117),
        contentColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().testTag("tactical_bottom_bar")
    ) {
        PRIMARY_NAV_ITEMS.forEach { item ->
            val selected = (currentRoute == item.route) || (item.route == "chat" && currentRoute == "team_chat")
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        item.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = TacticalCyanPrimary,
                    indicatorColor = TacticalCyanPrimary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag(item.testTag)
            )
        }
    }
}

@Composable
fun TacticalFloatingDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xDD0D1117),
            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.5f)),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PRIMARY_NAV_ITEMS.forEach { item ->
                    val isSelected = (currentRoute == item.route) || (item.route == "chat" && currentRoute == "team_chat")
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) TacticalCyanPrimary else Color.Transparent)
                            .clickable { onNavigate(item.route) }
                            .padding(10.dp)
                            .testTag(item.testTag)
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) Color.Black else Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalSideNavigationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF0D1117),
        contentColor = Color.White,
        modifier = Modifier.fillMaxHeight().testTag("tactical_nav_rail")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        PRIMARY_NAV_ITEMS.forEach { item ->
            val selected = (currentRoute == item.route) || (item.route == "chat" && currentRoute == "team_chat")
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(22.dp))
                },
                label = { Text(item.title, fontSize = 9.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = TacticalCyanPrimary,
                    indicatorColor = TacticalCyanPrimary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                modifier = Modifier.testTag(item.testTag)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
