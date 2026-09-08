package com.example

import com.example.domain.models.*
import com.example.domain.presence.UserPresenceStatus
import org.junit.Assert.*
import org.junit.Test

class DomainModelsAndSyncTest {

    @Test
    fun testUserProfileDefaults() {
        val profile = UserProfile(
            uid = "test_uid_99",
            displayName = "Comandante Recon",
            email = "recon@mesh.tactical",
            batteryPercent = 95,
            isOnline = true
        )

        assertEquals("test_uid_99", profile.uid)
        assertEquals("Comandante Recon", profile.displayName)
        assertEquals("recon@mesh.tactical", profile.email)
        assertEquals(95, profile.batteryPercent)
        assertTrue(profile.isOnline)
        assertTrue(profile.roles.contains("OPERATOR"))
    }

    @Test
    fun testEncryptedChatSessionCreation() {
        val session = EncryptedChatSession(
            sessionId = "sess_tactical_01",
            participantUids = listOf("uid_a", "uid_b"),
            participantNames = mapOf("uid_a" to "Alpha", "uid_b" to "Bravo"),
            title = "Enlace Táctico Directo",
            isGroup = false,
            sessionKeyFingerprint = "7B:9A:C3:F1"
        )

        assertEquals("sess_tactical_01", session.sessionId)
        assertEquals(2, session.participantUids.size)
        assertEquals("Bravo", session.participantNames["uid_b"])
        assertFalse(session.isGroup)
        assertEquals("7B:9A:C3:F1", session.sessionKeyFingerprint)
    }

    @Test
    fun testActiveSyncTaskProperties() {
        val syncTask = ActiveSyncTask(
            taskName = "Sincronización Bóveda E2EE",
            targetNodeId = "node_02",
            targetNodeName = "Nodo Secundario",
            progressPercent = 75,
            bytesTransferred = 75000L,
            totalBytes = 100000L,
            syncType = SyncTaskType.SQLCIPHER_VAULT,
            isEncrypted = true
        )

        assertEquals(75, syncTask.progressPercent)
        assertTrue(syncTask.isEncrypted)
        assertEquals(SyncTaskType.SQLCIPHER_VAULT, syncTask.syncType)
        assertEquals("🔐", syncTask.syncType.iconEmoji)
    }

    @Test
    fun testRemoteControlCommand() {
        val command = RemoteControlCommand(
            targetNodeId = "node_damaged_screen",
            actionType = RemoteActionType.DUMP_VAULT_BACKUP,
            payload = "BACKUP_ALL"
        )

        assertEquals("node_damaged_screen", command.targetNodeId)
        assertEquals(RemoteActionType.DUMP_VAULT_BACKUP, command.actionType)
        assertEquals("Rescatar Bóveda de Datos", command.actionType.label)
    }

    @Test
    fun testNetworkNodeHealthAndHistory() {
        val history = listOf(
            ConnectionHistoryItem(eventTitle = "Handshake Inicial", latencyMs = 15, throughputMbps = 50f)
        )
        val node = NetworkNode(
            id = "node_test_1",
            name = "Nodo Bravo",
            status = NodeStatus.AUTHENTICATED_AND_TRUSTED,
            rssi = -60,
            batteryLevel = 90,
            connectionHistory = history
        )

        assertEquals("Nodo Bravo", node.name)
        assertEquals(NodeStatus.AUTHENTICATED_AND_TRUSTED, node.status)
        assertEquals(1, node.connectionHistory.size)
        assertEquals(15L, node.connectionHistory[0].latencyMs)
    }

    @Test
    fun testTacticalNotificationPayloadSerialization() {
        val notif = TacticalNotificationPayload(
            type = "CHAT_MESSAGE",
            title = "Alpha 1",
            body = "📷 Fotografía Táctica Recibida",
            sessionId = "session_xyz",
            senderUid = "user_alpha_1",
            senderName = "Alpha 1",
            timestamp = 1700000000000L,
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/app/test.jpg",
            isRead = false
        )

        assertEquals("CHAT_MESSAGE", notif.type)
        assertEquals("Alpha 1", notif.senderName)
        assertFalse(notif.isRead)
        assertEquals("https://firebasestorage.googleapis.com/v0/b/app/test.jpg", notif.mediaUrl)
    }

    @Test
    fun testUserPresenceStatusDefaults() {
        val presence = UserPresenceStatus(
            isOnline = true,
            lastSeen = 1700000000000L,
            battery = 88,
            displayName = "Delta Operator",
            email = "delta@recon.ops"
        )

        assertTrue(presence.isOnline)
        assertEquals(88, presence.battery)
        assertEquals("Delta Operator", presence.displayName)
        assertEquals("delta@recon.ops", presence.email)
    }
}
