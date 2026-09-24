package com.bioplast.insumos.ui

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bioplast.insumos.MainActivity
import com.bioplast.insumos.data.AppDatabase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión de la regla sénior "Atrás nunca pierde lo escrito".
 *
 * Escrito 25 en la calculadora → "← Atrás" (vuelve al Inicio) → reentrar con
 * CONTAR INSUMOS: la cantidad sigue siendo 25 y el insumo elegido también.
 *
 * La regla es [MainActivity], que pinta el estado real de la ViewModel
 * (`InventoryViewModel.onAtras` debe conservar `quantityInput` y
 * `selectedProduct`; la pantalla de la calculadora solo navega, no limpia).
 */
@RunWith(AndroidJUnit4::class)
class AtrasConservaCantidadTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    @Before
    fun limpiarLaBase() {
        // Estado inicial vacío (ver detalle del SQL crudo en FlujoBasicoTest).
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppDatabase.getInstance(context)
            .openHelper.writableDatabase
            .execSQL("DELETE FROM inventory_records")
    }

    private fun esperarTexto(texto: String, timeout: Long = 10_000) {
        regla.waitUntil(timeout) {
            regla.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Selector NO ambiguo de la cifra del visor (`CalculatorScreen.kt`).
     *
     * El nodo fusionado de la tecla "7" del teclado propio TAMBIÉN contiene el
     * texto "7" (su `contentDescription` es "Tecla 7"), así que
     * `onNodeWithText("7")` encuentra 2 nodos y lanza "Expected at most 1 node".
     * La cifra del visor es el único "7" SIN esa descripción.
     */
    private fun cifraDelVisor(texto: String): SemanticsMatcher =
        hasText(texto) and !hasContentDescription("Tecla $texto")

    @Test
    fun atrasDesdeLaCalculadoraNoPierdeLaCantidadEscrita() {
        // --- INICIO → CALCULADORA → "Frasco de orina" -------------------------
        esperarTexto("¿Qué hacemos hoy?")
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        regla.onNodeWithText("Frasco de orina").performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()

        // --- Escribe 25 con el teclado propio -----------------------------------
        regla.onNodeWithContentDescription("Tecla 2").performClick()
        regla.onNodeWithContentDescription("Tecla 5").performClick()
        regla.onNodeWithText("25").assertIsDisplayed()

        // --- "← Atrás" de la calculadora → INICIO -------------------------------
        regla.onNodeWithContentDescription("Atrás, volver al paso anterior").performClick()
        esperarTexto("¿Qué hacemos hoy?")

        // --- Reentrar: la cantidad (y el insumo) deben seguir ahí ---------------
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithText("25").assertIsDisplayed() // ← la regla "Atrás conserva"
        regla.onNodeWithText("Frasco de orina").assertIsDisplayed()

        // Sin error residual: "Atrás" apagó quantityError, SEGUIR sigue habilitado.
        regla.onNodeWithText("Primero escribe cuántos").assertDoesNotExist()
        regla.onNodeWithText("SEGUIR →").assertIsEnabled()
    }

    @Test
    fun atrasConservaTambienElInsumoElegido() {
        esperarTexto("¿Qué hacemos hoy?")
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("Frasco de orina").performClick()
        regla.onNodeWithContentDescription("Tecla 7").performClick()
        regla.onNode(cifraDelVisor("7")).assertIsDisplayed()

        // Atrás → INICIO → reentrar: la "7" y el insumo sobrevivieron.
        regla.onNodeWithContentDescription("Atrás, volver al paso anterior").performClick()
        esperarTexto("¿Qué hacemos hoy?")
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNode(cifraDelVisor("7")).assertIsDisplayed()
        regla.onNodeWithText("Frasco de orina").assertIsDisplayed()
    }
}
