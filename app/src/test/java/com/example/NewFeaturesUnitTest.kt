package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.local.entities.ContactEntity
import com.example.domain.local.entities.ThreadCustomizationEntity
import com.example.domain.models.EncryptedChatSession
import com.example.domain.security.BiometricVaultManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NewFeaturesUnitTest {

    private lateinit var db: OmniDatabase
    private lateinit var biometricVaultManager: BiometricVaultManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OmniDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        biometricVaultManager = BiometricVaultManager(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testThreadCustomizationPersistenceInRoom() = runBlocking {
        val customizationDao = db.threadCustomizationDao()
        val customEntity = ThreadCustomizationEntity(
            sessionId = "group_tactical_alpha",
            wallpaperPreset = "CYBER_EMERALD",
            customWallpaperColorHex = "#0D2818",
            outgoingBubbleColorHex = "#00E676",
            incomingBubbleColorHex = "#1B4332",
            updatedAtTimestamp = System.currentTimeMillis()
        )

        customizationDao.saveCustomization(customEntity)

        val retrieved = customizationDao.getCustomizationFlow("group_tactical_alpha").first()
        assertNotNull("Customization should be stored in Room", retrieved)
        assertEquals("CYBER_EMERALD", retrieved?.wallpaperPreset)
        assertEquals("#00E676", retrieved?.outgoingBubbleColorHex)
        assertEquals("#1B4332", retrieved?.incomingBubbleColorHex)

        // Direct query
        val directRetrieved = customizationDao.getCustomizationDirect("group_tactical_alpha")
        assertNotNull(directRetrieved)
        assertEquals("CYBER_EMERALD", directRetrieved?.wallpaperPreset)

        // Update customization to Amber Radar
        val updatedEntity = customEntity.copy(
            wallpaperPreset = "AMBER_RADAR",
            outgoingBubbleColorHex = "#FFB300"
        )
        customizationDao.saveCustomization(updatedEntity)

        val updatedRetrieved = customizationDao.getCustomizationFlow("group_tactical_alpha").first()
        assertEquals("AMBER_RADAR", updatedRetrieved?.wallpaperPreset)
        assertEquals("#FFB300", updatedRetrieved?.outgoingBubbleColorHex)
    }

    @Test
    fun testBiometricVaultLockAndUnlock() {
        // Unlock vault directly
        biometricVaultManager.unlockVaultDirectly()
        assertTrue("Vault should be unlocked after direct unlock", biometricVaultManager.isVaultUnlocked.value)

        // Lock vault
        biometricVaultManager.lockVault()
        assertFalse("Vault should be locked after manual lock", biometricVaultManager.isVaultUnlocked.value)

        // Unlock vault again
        biometricVaultManager.unlockVaultDirectly()
        assertTrue("Vault should be unlocked again", biometricVaultManager.isVaultUnlocked.value)
    }

    @Test
    fun testGroupChatSessionModel() {
        val groupSession = EncryptedChatSession(
            sessionId = "group_ops_77",
            title = "Escuadrón Reconocimiento",
            groupDescription = "Operación de vigilancia táctica",
            groupIcon = "🛡️",
            isGroup = true,
            createdByUid = "operator_alpha",
            adminUids = listOf("operator_alpha"),
            participantUids = listOf("operator_alpha", "operator_bravo", "operator_charlie"),
            participantNames = mapOf(
                "operator_alpha" to "Líder Alpha",
                "operator_bravo" to "Especialista Bravo",
                "operator_charlie" to "Comms Charlie"
            ),
            sessionKeyFingerprint = "9A:8B:7C:6D"
        )

        assertTrue(groupSession.isGroup)
        assertEquals("Escuadrón Reconocimiento", groupSession.title)
        assertEquals("🛡️", groupSession.groupIcon)
        assertEquals(3, groupSession.participantUids.size)
        assertTrue(groupSession.adminUids.contains("operator_alpha"))
        assertFalse(groupSession.adminUids.contains("operator_bravo"))
    }

    @Test
    fun testVoiceNoteMessagePersistenceAndDeliveryReceipts() = runBlocking {
        val chatDao = db.chatDao()

        // 1. Insert voice note message
        val voiceMsg = ChatMessageEntity(
            id = "audio_msg_101",
            sessionId = "session_direct_ops",
            senderId = "operator_bravo",
            senderName = "Bravo",
            text = "🎙️ Nota de voz táctica",
            mediaType = "AUDIO",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/app/voice_note_101.m4a",
            localFilePath = "/storage/emulated/0/tactical_audio/voice_101.m4a",
            mediaDurationMs = 12500L,
            status = "SENT",
            timestamp = System.currentTimeMillis()
        )

        chatDao.insertMessage(voiceMsg)

        val retrievedList = chatDao.getMessagesForSession("session_direct_ops").first()
        assertEquals(1, retrievedList.size)
        val retrieved = retrievedList[0]
        assertEquals("AUDIO", retrieved.mediaType)
        assertEquals(12500L, retrieved.mediaDurationMs)
        assertEquals("SENT", retrieved.status)

        // 2. Transition delivery status to DELIVERED and READ
        val deliveredMsg = retrieved.copy(status = "DELIVERED")
        chatDao.insertMessage(deliveredMsg)
        val afterDelivered = chatDao.getMessagesForSession("session_direct_ops").first()[0]
        assertEquals("DELIVERED", afterDelivered.status)

        val readMsg = retrieved.copy(status = "READ")
        chatDao.insertMessage(readMsg)
        val afterRead = chatDao.getMessagesForSession("session_direct_ops").first()[0]
        assertEquals("READ", afterRead.status)
    }

    @Test
    fun testPhotoAttachmentPersistence() = runBlocking {
        val chatDao = db.chatDao()

        val photoMsg = ChatMessageEntity(
            id = "photo_msg_202",
            sessionId = "session_direct_ops",
            senderId = "operator_alpha",
            senderName = "Alpha",
            text = "📷 Fotografía Táctica",
            mediaType = "IMAGE",
            mediaUrl = "https://firebasestorage.googleapis.com/v0/b/app/photo_202.jpg",
            status = "DELIVERED",
            timestamp = System.currentTimeMillis()
        )

        chatDao.insertMessage(photoMsg)

        val messages = chatDao.getMessagesForSession("session_direct_ops").first()
        assertTrue(messages.any { it.id == "photo_msg_202" && it.mediaType == "IMAGE" })
    }
}
