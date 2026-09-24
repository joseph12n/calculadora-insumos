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
 * Escrito en el PASO 2: 25 → "← Atrás" (vuelve al PASO 1) → reentrar al PASO 2
 * eligiendo el producto otra vez → la cantidad sigue siendo 25.
 *
 * La regla es [MainActivity], que pinta el estado real de la ViewModel
 * (`InventoryViewModel.onAtras` debe conservar `quantityInput`; la copia del
 * estado en EnterQuantityScreen.kt solo navega, no limpia el campo).
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
     * Selector NO ambiguo de la cifra gigante del Paso 2 (`EnterQuantityScreen.kt:116-123`).
     *
     * El nodo fusionado de la tecla "7" del teclado propio TAMBIÉN contiene el
     * texto "7" (su `contentDescription` es "Tecla 7", `Keypad.kt:119`), así que
     * `onNodeWithText("7")` encuentra 2 nodos y lanza "Expected at most 1 node".
     * La cifra de 72 sp es el único "7" SIN esa descripción.
     */
    private fun cifraGigante(texto: String): SemanticsMatcher =
        hasText(texto) and !hasContentDescription("Tecla $texto")

    @Test
    fun atrasDesdeElPaso2NoPierdeLaCantidadEscrita() {
        // --- INICIO → PASO 1 → PASO 2 -----------------------------------------
        esperarTexto("¿Qué hacemos hoy?")
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()

        // --- Escribe 25 con el teclado propio -----------------------------------
        regla.onNodeWithContentDescription("Tecla 2").performScrollTo().performClick()
        regla.onNodeWithContentDescription("Tecla 5").performScrollTo().performClick()
        regla.onNodeWithText("25").assertIsDisplayed()

        // --- "← Atrás" del PASO 2 → PASO 1 --------------------------------------
        regla.onNodeWithContentDescription("Atrás, volver al paso anterior").performClick()
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()

        // --- Reentrar al PASO 2: la cantidad debe seguir siendo 25 ---------------
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithText("25").assertIsDisplayed() // ← la regla "Atrás conserva"

        // Sin error residual: "Atrás" apagó quantityError, SEGUIR sigue habilitado.
        regla.onNodeWithText("Primero escribe cuántos").assertDoesNotExist()
        regla.onNodeWithText("SEGUIR →").performScrollTo().assertIsEnabled()
    }

    @Test
    fun atrasDelPaso1AlInicioTambienConservaLaCantidad() {
        // Variante larga: PASO 2 → PASO 1 → INICIO → reentrar → cantidad intacta.
        esperarTexto("¿Qué hacemos hoy?")
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithContentDescription("Tecla 7").performScrollTo().performClick()
        regla.onNode(cifraGigante("7")).assertIsDisplayed()

        // PASO 2 → PASO 1 → INICIO (dos veces Atrás).
        regla.onNodeWithContentDescription("Atrás, volver al paso anterior").performClick()
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        regla.onNodeWithContentDescription("Atrás, volver al paso anterior").performClick()
        esperarTexto("¿Qué hacemos hoy?")

        // Reentrar: la "7" sobrevivió a toda la cadena de Atrás.
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNode(cifraGigante("7")).assertIsDisplayed()
    }
}
