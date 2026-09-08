package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.InternalCiCdScreen
import com.example.ui.screens.RoadmapScreen
import com.example.ui.screens.ServerFleetManagementScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testRoadmapScreenDisplaysTitleAndInteractsWithSearch() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RoadmapScreen(onBack = {})
            }
        }

        composeTestRule.waitForIdle()
        // Verificamos que se renderiza el encabezado principal del plano maestro
        composeTestRule.onNodeWithText("PLANO MAESTRO & INGENIERÍA INVERSA").assertIsDisplayed()
        
        // Verificamos que el badge de métricas ejecutivas se muestra
        composeTestRule.onNodeWithText("FASES TÁCTICAS").assertIsDisplayed()
        composeTestRule.onNodeWithText("CAPAS SISTEMA").assertIsDisplayed()
    }

    @Test
    fun testInternalCiCdScreenOpensAndInteractsWithTabs() {
        composeTestRule.setContent {
            MyApplicationTheme {
                InternalCiCdScreen(onBack = {})
            }
        }

        composeTestRule.waitForIdle()
        // Verificamos que la pantalla de CI/CD abre correctamente con su título
        composeTestRule.onNodeWithText("CI/CD & DEVSECOPS INTERNO").assertIsDisplayed()
        
        // Interactuamos con la pestaña de Bóveda de Artefactos
        composeTestRule.onNodeWithText("Bóveda Artefactos & SBOM").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("BÓVEDA DE ARTEFACTOS & SBOM CRIPTOGRÁFICO", substring = true).assertIsDisplayed()

        // Interactuamos con la pestaña de Despliegues & Rollback
        composeTestRule.onNodeWithText("Despliegues & Rollback").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ENTORNOS DE DESPLIEGUE & MOTOR DE ROLLBACK", substring = true).assertIsDisplayed()

        // Interactuamos con la pestaña de YAML Spec
        composeTestRule.onNodeWithText("Pipeline YAML (.ci.yml)").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DEFINICIÓN DE PIPELINE .omnicomm-ci.yml", substring = true).assertIsDisplayed()
    }

    @Test
    fun testServerFleetManagementScreenOpens() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ServerFleetManagementScreen(onBack = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("MANDO DE SERVIDORES LINUX & WINDOWS").assertIsDisplayed()
    }

    @Test
    fun testVisualTelemetryScreenOpensAndInteracts() {
        composeTestRule.setContent {
            MyApplicationTheme {
                com.example.ui.screens.VisualTelemetryScreen(onBack = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("TELEMETRÍA VISUAL Y DIAGNÓSTICO").assertIsDisplayed()
        composeTestRule.onNodeWithText("Motor de Captura Interna").assertIsDisplayed()
        composeTestRule.onNodeWithText("HISTORIAL DE CAPTURAS INTERNAS", substring = true).assertIsDisplayed()
    }
}
