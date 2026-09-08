package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.local.entities.ContactEntity
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
class RoomDatabaseTest {

    private lateinit var db: OmniDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, OmniDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveContact() = runBlocking {
        val contactDao = db.contactDao()
        val contact = ContactEntity(
            deviceId = "node_alpha_01",
            alias = "Alpha Recon",
            email = "alpha@recon.tactical",
            isPriority = true,
            isOnline = true,
            publicKeyFingerprint = "7B:9A:C3:F1"
        )

        contactDao.insertContact(contact)

        val retrieved = contactDao.getContactDirect("node_alpha_01")
        assertNotNull(retrieved)
        assertEquals("Alpha Recon", retrieved?.alias)
        assertEquals("alpha@recon.tactical", retrieved?.email)
        assertTrue(retrieved?.isPriority == true)
        assertTrue(retrieved?.isOnline == true)

        // Toggle priority
        contactDao.setNodePriority("node_alpha_01", false)
        val updated = contactDao.getContactDirect("node_alpha_01")
        assertFalse(updated?.isPriority == true)
    }

    @Test
    fun testInsertAndRetrieveChatMessage() = runBlocking {
        val chatDao = db.chatDao()
        val message = ChatMessageEntity(
            id = "msg_001",
            sessionId = "session_bravo",
            senderId = "user_123",
            senderName = "Operator 1",
            text = "Coordenadas transmitidas vía E2EE",
            status = "DELIVERED",
            timestamp = System.currentTimeMillis()
        )

        chatDao.insertMessage(message)

        val sessionMessages = chatDao.getMessagesForSession("session_bravo").first()
        assertEquals(1, sessionMessages.size)
        assertEquals("Coordenadas transmitidas vía E2EE", sessionMessages[0].text)
        assertEquals("session_bravo", sessionMessages[0].sessionId)
    }

    @Test
    fun testPendingMessagesQueue() = runBlocking {
        val chatDao = db.chatDao()
        val pendingMsg = ChatMessageEntity(
            id = "msg_pending_1",
            sessionId = "global",
            senderId = "me",
            senderName = "My Node",
            text = "Mensaje offline en cola",
            status = "PENDING",
            timestamp = System.currentTimeMillis()
        )

        chatDao.insertMessage(pendingMsg)

        val pendingList = chatDao.getPendingMessages()
        assertEquals(1, pendingList.size)
        assertEquals("msg_pending_1", pendingList[0].id)
    }

    @Test
    fun testFullTextSearchInRoom() = runBlocking {
        val chatDao = db.chatDao()
        val msg1 = ChatMessageEntity(
            id = "msg_sec_1",
            sessionId = "session_alpha",
            senderId = "operator_a",
            senderName = "Alpha 1",
            text = "Reporte de reconocimiento en cuadrante norte",
            status = "DELIVERED",
            timestamp = 1000L
        )
        val msg2 = ChatMessageEntity(
            id = "msg_sec_2",
            sessionId = "session_alpha",
            senderId = "operator_b",
            senderName = "Bravo 2",
            text = "Fotografía táctica del puesto de mando enviada",
            mediaType = "IMAGE",
            mediaUrl = "https://firebasestorage.googleapis.com/test_photo.jpg",
            status = "DELIVERED",
            timestamp = 2000L
        )
        val msg3 = ChatMessageEntity(
            id = "msg_sec_3",
            sessionId = "session_beta",
            senderId = "operator_c",
            senderName = "Charlie 3",
            text = "Estado del suministro de combustible",
            status = "DELIVERED",
            timestamp = 3000L
        )

        chatDao.insertMessage(msg1)
        chatDao.insertMessage(msg2)
        chatDao.insertMessage(msg3)

        // 1. Búsqueda global por texto parcial
        val searchRecon = chatDao.searchAllMessages("reconocimiento").first()
        assertEquals(1, searchRecon.size)
        assertEquals("msg_sec_1", searchRecon[0].id)

        // 2. Búsqueda por sesión y término
        val searchPhotoInAlpha = chatDao.searchSessionMessages("session_alpha", "fotografía").first()
        assertEquals(1, searchPhotoInAlpha.size)
        assertEquals("msg_sec_2", searchPhotoInAlpha[0].id)
        assertEquals("IMAGE", searchPhotoInAlpha[0].mediaType)
        assertNotNull(searchPhotoInAlpha[0].mediaUrl)

        // 3. Búsqueda en sesión que no coincide
        val searchSuministroInAlpha = chatDao.searchSessionMessages("session_alpha", "suministro").first()
        assertTrue(searchSuministroInAlpha.isEmpty())
    }

    @Test
    fun testContactPresenceAndBattery() = runBlocking {
        val contactDao = db.contactDao()
        val contact = ContactEntity(
            deviceId = "node_delta_04",
            alias = "Delta Spec",
            email = "delta@spec.ops",
            isOnline = false,
            batteryPercent = 50
        )
        contactDao.insertContact(contact)

        // Actualizar presencia y batería en tiempo real desde RTDB
        contactDao.updateFullPresence("node_delta_04", isOnline = true, lastSeen = 5000L, battery = 92)
        val updated = contactDao.getContactDirect("node_delta_04")

        assertNotNull(updated)
        assertTrue(updated?.isOnline == true)
        assertEquals(92, updated?.batteryPercent)
        assertEquals(5000L, updated?.lastSeenTimestamp)
    }

    @Test
    fun testCryptographicKeyGenerationAndRoomStorage() = runBlocking {
        val keyDao = db.cryptographicKeyDao()

        // 1. Generar par de claves RSA 2048 usando el generador táctico
        val generatedRsa = com.example.domain.security.CryptographicKeyGenerator.generateRsaKeyPair(
            alias = "Clave Primaria Alfa",
            keySizeBits = 2048,
            useAndroidKeyStore = false,
            purpose = "E2EE_MESSAGING"
        )
        assertNotNull(generatedRsa.publicKeyBase64)
        assertTrue(generatedRsa.publicKeyBase64.isNotBlank())
        assertTrue(generatedRsa.fingerprint.startsWith("SHA") || generatedRsa.fingerprint.contains(":"))

        val entityRsa = com.example.domain.local.entities.CryptographicKeyEntity(
            keyId = generatedRsa.keyId,
            alias = generatedRsa.alias,
            algorithm = generatedRsa.algorithm,
            purpose = generatedRsa.purpose,
            publicKeyBase64 = generatedRsa.publicKeyBase64,
            fingerprint = generatedRsa.fingerprint,
            keySizeBits = generatedRsa.keySizeBits,
            isHardwareBacked = generatedRsa.isHardwareBacked,
            isPrimary = true,
            ownerNodeId = "LOCAL_NODE",
            ownerDisplayName = "Terminal Alfa"
        )
        keyDao.insertKey(entityRsa)

        // 2. Comprobar que es la clave primaria activa
        val primaryKey = keyDao.getPrimaryActiveKey().first()
        assertNotNull(primaryKey)
        assertEquals(generatedRsa.keyId, primaryKey?.keyId)
        assertTrue(primaryKey?.isPrimary == true)

        // 3. Generar e insertar clave de un nodo par
        val generatedPeer = com.example.domain.security.CryptographicKeyGenerator.generateEcKeyPair(
            alias = "Clave Pública Nodo Bravo",
            useAndroidKeyStore = false,
            purpose = "E2EE_MESSAGING"
        )
        val peerEntity = com.example.domain.local.entities.CryptographicKeyEntity(
            keyId = generatedPeer.keyId,
            alias = generatedPeer.alias,
            algorithm = generatedPeer.algorithm,
            purpose = generatedPeer.purpose,
            publicKeyBase64 = generatedPeer.publicKeyBase64,
            fingerprint = generatedPeer.fingerprint,
            keySizeBits = generatedPeer.keySizeBits,
            isHardwareBacked = false,
            isPrimary = false,
            ownerNodeId = "NODE_BRAVO_02",
            ownerDisplayName = "Operador Bravo"
        )
        keyDao.insertKey(peerEntity)

        // 4. Comprobar listado y filtros de búsqueda
        val allKeys = keyDao.getAllKeys().first()
        assertEquals(2, allKeys.size)

        val searchBravo = keyDao.searchKeys("Bravo").first()
        assertEquals(1, searchBravo.size)
        assertEquals("NODE_BRAVO_02", searchBravo[0].ownerNodeId)

        // 5. Revocar clave par
        keyDao.revokeKey(generatedPeer.keyId)
        val revokedKey = keyDao.getKeyById(generatedPeer.keyId)
        assertNotNull(revokedKey)
        assertTrue(revokedKey?.isRevoked == true)

        // 6. Eliminar clave
        keyDao.deleteKeyById(generatedPeer.keyId)
        assertNull(keyDao.getKeyById(generatedPeer.keyId))
    }
}
