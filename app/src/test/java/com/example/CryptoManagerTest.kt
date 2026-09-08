package com.example

import com.example.domain.security.CryptoManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CryptoManagerTest {

    @Test
    fun testCryptoManagerInitialization() {
        val cryptoManager = CryptoManager()
        assertNotNull(cryptoManager)
    }

    @Test
    fun testPublicKeyFingerprintFormat() {
        val cryptoManager = CryptoManager()
        val fingerprint = cryptoManager.getPublicKeyFingerprint()
        assertNotNull(fingerprint)
        assertTrue("Fingerprint should not be empty", fingerprint.isNotEmpty())
    }

    @Test
    fun testPublicEncryptionKeyBase64() {
        val cryptoManager = CryptoManager()
        val pubKeyBase64 = cryptoManager.getPublicEncryptionKeyBase64()
        assertNotNull(pubKeyBase64)
        assertTrue("Public key base64 should not be empty", pubKeyBase64.isNotEmpty())
    }

    @Test
    fun testEncryptAndDecryptFallbackOrRoundtrip() {
        val cryptoManager = CryptoManager()
        val sampleText = "TACTICAL_E2EE_MESSAGE_ALPHA_77"
        val encrypted = cryptoManager.encrypt(sampleText)
        assertNotNull(encrypted)
        assertTrue(encrypted.isNotEmpty())

        val decrypted = cryptoManager.decrypt(encrypted)
        assertNotNull(decrypted)
        assertEquals(sampleText, decrypted)
    }
}
