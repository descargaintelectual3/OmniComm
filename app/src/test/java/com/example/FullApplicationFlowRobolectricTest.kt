package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.domain.media.TacticalScreenshotCaptureService
import com.example.domain.security.BiometricVaultManager
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = TestOmniApplication::class)
class FullApplicationFlowRobolectricTest : BaseRobolectricTestWithFirebase() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testMainActivityLaunchesAndHubIsFullyInteractive() {
        composeTestRule.waitForIdle()

        // 1. Verificar que la actividad MainActivity carga y muestra el encabezado del Hub
        composeTestRule.onNodeWithText("OmniComm Tactical Hub").assertIsDisplayed()

        // 2. Verificar que los módulos clave se renderizan en el LazyVerticalGrid
        composeTestRule.onNodeWithTag("feature_contacts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feature_team_chat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feature_cicd").assertIsDisplayed()
        composeTestRule.onNodeWithTag("feature_visual_telemetry").assertIsDisplayed()

        // 3. Interactuar con el switch de Nodo Servidor Mesh
        val serverSwitch = composeTestRule.onNodeWithTag("mesh_server_switch")
        serverSwitch.assertIsDisplayed()
        serverSwitch.performClick()
        composeTestRule.waitForIdle()

        // 4. Navegar a Telemetría Visual y verificar captura
        composeTestRule.onNodeWithTag("feature_visual_telemetry").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("TELEMETRÍA VISUAL Y DIAGNÓSTICO").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_trigger_screenshot").assertIsDisplayed()

        // Ejecutar botón de captura
        composeTestRule.onNodeWithTag("btn_trigger_screenshot").performClick()
        composeTestRule.waitForIdle()

        // 5. Regresar al Hub
        composeTestRule.onNodeWithTag("btn_back_visual_telemetry").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("OmniComm Tactical Hub").assertIsDisplayed()
    }

    @Test
    fun testBiometricVaultDefaultSafeState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vaultManager = BiometricVaultManager(context)
        
        // En entorno de preview / emulador, el requerimiento por defecto debe ser false para permitir arranque fluido
        assertFalse(vaultManager.isBiometricSecurityRequired())
        assertTrue(vaultManager.isVaultUnlocked.value)
    }

    @Test
    fun testTacticalScreenshotCaptureServiceCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = TacticalScreenshotCaptureService.getInstance(context)
        assertNotNull(service)
        assertNotNull(service.screenshotsList.value)
    }
}
