package com.bioplast.insumos.ui

import androidx.compose.ui.test.assertIsDisplayed
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
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test de UI del LOTE (FASE 3) sobre [MainActivity]: dos productos en una sesión.
 *
 * Recorrido (un toque por acción, teclado PROPIO, sin gestos):
 *  1. INICIO → PASO 1 → "Frasco de orina" × 1 → PASO 3 (sin lista todavía:
 *     NO aparece "En tu lista").
 *  2. **➕ Agregar otro** → vuelve al PASO 1 con la lista empezada.
 *  3. "Cryotubo" × 1 → PASO 3 con **2 filas visibles**: el item ACTUAL
 *     ("1 Cryotubo") y el pendiente de la lista ("1 Frasco de orina") dentro de
 *     la sección **"En tu lista"**, con sus botones circulares 📅 / ✕.
 *  4. Totales del lote: "2 productos" (1 + 1) y la cifra SIN decimales
 *     (7.18 + 7.90 = 15.08 → `CurrencyFormat.formatEntero` → "…15"; el formato
 *     viejo "15,08" no debe existir).
 *  5. **✔ GUARDAR TODO** → insertBatch → ÉXITO con "2 productos · Total".
 *
 * Base de datos: tabla vacía antes/después de cada test (SQL crudo, mismo
 * criterio documentado en [FlujoBasicoTest.vaciarLaBase]).
 */
@RunWith(AndroidJUnit4::class)
class LoteBasicoTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    /** Vacía la tabla real (ver el detalle en FlujoBasicoTest.vaciarLaBase). */
    private fun vaciarLaBase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppDatabase.getInstance(context)
            .openHelper.writableDatabase
            .execSQL("DELETE FROM inventory_records")
    }

    @Before
    fun limpiarAntesDelTest() = vaciarLaBase()

    @After
    fun limpiarDespuesDelTest() = vaciarLaBase()

    /** Espera (hasta 10 s) a que un nodo con [texto] exista (Room/insert son async). */
    private fun esperarTexto(texto: String, substring: Boolean = false, timeout: Long = 10_000) {
        regla.waitUntil(timeout) {
            regla.onAllNodesWithText(texto, substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Toca un botón que puede estar fuera del viewport (Column + verticalScroll). */
    private fun tocar(texto: String) {
        regla.onNodeWithText(texto).performScrollTo().performClick()
    }

    @Test
    fun loteDeDosProductosSeGuardaTodoYTerminaEnExitoConDosProductos() {
        // --- INICIO → PASO 1 → PASO 2 (Frasco de orina × 1) --------------------
        esperarTexto("0 insumos")
        tocar("➕ CONTAR INSUMOS")
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithContentDescription("Tecla 1").performScrollTo().performClick()

        // --- PASO 3 del primer item: aún NO hay lista ---------------------------
        tocar("SEGUIR →")
        regla.onNodeWithText("¿Guardamos esto?").assertIsDisplayed()
        // El Paso 3 entero vive en una Column con verticalScroll (ConfirmScreen.kt:120-124):
        // con la tarjeta del item + "Agregar otro" + totales, GUARDAR TODO queda
        // bajo el pliegue del viewport → hay que hacer scroll antes del assert.
        regla.onNodeWithText("✔ GUARDAR TODO").performScrollTo().assertIsDisplayed()
        regla.onNodeWithText("En tu lista").assertDoesNotExist() // lista vacía

        // --- ➕ Agregar otro → vuelve al PASO 1 con la lista empezada ------------
        tocar("➕ Agregar otro")
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()

        // --- Segundo item: Cryotubo × 1 → PASO 3 con 1 pendiente + actual -------
        regla.onNodeWithText("Cryotubo").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithContentDescription("Tecla 1").performScrollTo().performClick()
        tocar("SEGUIR →")
        regla.onNodeWithText("¿Guardamos esto?").assertIsDisplayed()

        // --- 2 filas en pantalla: el item ACTUAL y el pendiente "En tu lista" ---
        regla.onNodeWithText("En tu lista").assertIsDisplayed()
        regla.onNodeWithText("1 Cryotubo").assertIsDisplayed() // fila del actual
        regla.onNodeWithText("1 Frasco de orina").assertIsDisplayed() // fila del lote
        // Botones circulares de la fila del lote (📅 cambiar fecha / ✕ quitar).
        regla.onNodeWithContentDescription("Cambiar la fecha de 1 Frasco de orina")
            .assertIsDisplayed()
        regla.onNodeWithContentDescription("Quitar de tu lista 1 Frasco de orina")
            .assertIsDisplayed()

        // --- Total del lote: 1 + 1 = 2 unidades; 15.08 SIN decimales ------------
        // Igual que GUARDAR TODO: la tarjeta de totales queda bajo el pliegue del
        // viewport en el Paso 3 (Column con verticalScroll, ConfirmScreen.kt:127-131).
        regla.onNodeWithText("2 productos").performScrollTo().assertIsDisplayed()
        esperarTexto("15", substring = true) // 7.18 + 7.90 = 15.08 → "…15"
        assertTrue(
            "El total del lote se pinta SIN decimales (formatEntero)",
            regla.onAllNodesWithText("15", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
        regla.onNodeWithText("15,08", substring = true).assertDoesNotExist()

        // --- ✔ GUARDAR TODO → inserta los 2 → ÉXITO con "2 productos" ----------
        tocar("✔ GUARDAR TODO")
        esperarTexto("¡Guardado!") // el insertBatch corre en IO
        regla.onNodeWithText("¡Guardado!").assertIsDisplayed()
        regla.onNodeWithText("2 productos", substring = true).assertIsDisplayed()
        regla.onNodeWithText("↩ DESHACER").assertIsDisplayed()
    }
}
