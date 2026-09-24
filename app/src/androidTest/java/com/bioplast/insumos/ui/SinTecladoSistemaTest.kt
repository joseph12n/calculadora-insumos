package com.bioplast.insumos.ui

import android.widget.EditText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bioplast.insumos.MainActivity
import com.bioplast.insumos.data.AppDatabase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión de la regla sénior: "el teclado del sistema NUNCA aparece".
 *
 * La cantidad se escribe SOLO con el [com.bioplast.insumos.ui.components.Keypad]
 * propio (Keypad.kt documenta "Nunca usa BasicTextField ni el teclado del
 * sistema"). Este test toca la zona del número del visor y las teclas, y luego
 * verifica desde DOS ángulos que no existe ningún campo editable:
 *
 *  1. Espresso: ningún `EditText` en la jerarquía de vistas de la activity
 *     (si el IME del sistema se abriera, habría un EditText con foco).
 *  2. Compose: ningún nodo con `SetText` (los `BasicTextField`/`TextField` lo
 *     exponen en su configuración de semántica).
 *
 * Si alguien sustituyera el Keypad por un `BasicTextField`, uno de los dos
 * asserts fallaría.
 */
@RunWith(AndroidJUnit4::class)
class SinTecladoSistemaTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    @Before
    fun limpiarLaBase() {
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

    @Test
    fun tocarLaCifraYElTecladoNoInvocaElTecladoDelSistema() {
        // --- Llega a la calculadora ---------------------------------------------
        esperarTexto("¿Qué hacemos hoy?")
        regla.onNodeWithText("➕ CONTAR INSUMOS").performScrollTo().performClick()
        regla.onNodeWithText("Frasco de orina").performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()

        // --- Toca la ZONA de la cifra del visor (el "0" sin click action: un    //
        //     toque normal ahí no debe abrir ningún IME).                         //
        regla.onNode(hasText("0") and !hasClickAction())
            .performTouchInput { click() }

        // --- Toca el teclado PROPIO (contentDescription por tecla) ----------------
        regla.onNodeWithContentDescription("Tecla 1").performClick()
        regla.onNodeWithContentDescription("Tecla 7").performClick()
        regla.onNodeWithText("17").assertIsDisplayed() // el teclado propio funciona

        // --- 1) Espresso: NO existe ningún EditText en pantalla ------------------
        onView(isAssignableFrom(EditText::class.java)).check(doesNotExist())

        // --- 2) Compose: NINGÚN nodo es editable de texto (sin BasicTextField) ---
        regla.onNode(hasSetTextAction()).assertDoesNotExist()
    }
}
