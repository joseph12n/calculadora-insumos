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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

/**
 * Tests de UI del flujo COMPLETO con Compose sobre [MainActivity].
 *
 * Cobertura (fase 2, ACTUALIZADA en fase 3 al contrato nuevo):
 *  1. INICIO muestra "¿Qué hacemos hoy?" + CONTAR; CONTAR abre el PASO 1 con los
 *     5 productos; elegir "Frasco de orina" abre el PASO 2; el teclado PROPIO
 *     escribe "10" (el número gigante se pinta y el error "Primero escribe cuántos"
 *     NO aparece); SEGUIR abre el PASO 3 con "¿Guardamos esto?" y el total
 *     SIN decimales (10 × 7.18 = 71.8 → `CurrencyFormat.formatEntero` → "…72",
 *     FASE 3: los totales grandes ya no pintan coma decimal); "✔ GUARDAR TODO"
 *     lleva a ¡GUARDADO! con "10 productos · Total" y "Deshacer" disponible.
 *  2. Los pasos 6 y 7 (HISTORIAL y DETALLE DE LA SEMANA) son alcanzables en orden
 *     tras guardar, y "Atrás" regresa por el mismo camino hasta INICIO.
 *
 * Base de datos: cada test empieza con la tabla `inventory_records` vacía
 * (véase [vaciarLaBase]); el registro que crea GUARDAR se limpia en @After para
 * no ensuciar los tests siguientes.
 *
 * NOTA: usa `performScrollTo()` antes de tocar botones que viven dentro de las
 * Column con `verticalScroll` (StartScreen/PickProduct/EnterQuantity/Confirm/
 * Success) para que el test también pase en pantallas bajas o con fuente al 200 %.
 */
@RunWith(AndroidJUnit4::class)
class FlujoBasicoTest {

    @get:Rule
    val regla = createAndroidComposeRule<MainActivity>()

    /**
     * Vacía la tabla real de la app antes de cada test (estado inicial vacío).
     *
     * Se usa SQL crudo contra la instancia singleton de [AppDatabase] en vez de
     * `Context.deleteDatabase(...)`: borrar el archivo con la conexión abierta deja
     * handles huérfanos, y el DAO no expone un `deleteAll` (no se puede tocar el
     * código fuente). El InvalidationTracker de Room dispara sus triggers también
     * con `execSQL`, así que los Flujos de la ViewModel se re-emiten vacíos.
     */
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

    // ---------------------------------------------------------------------
    // 1) Flujo de registro: INICIO → PASO 1 → PASO 2 → PASO 3 → ¡GUARDADO!
    // ---------------------------------------------------------------------

    @Test
    fun flujoDeRegistroPasoAPasoTerminaEnExitoConDeshacerDisponible() {
        // --- INICIO (estado inicial vacío: la tabla se vació en @Before) -------
        esperarTexto("0 insumos") // espera a que Room emita el resumen vacío
        regla.onNodeWithText("¿Qué hacemos hoy?").assertIsDisplayed()
        regla.onNodeWithText("0 insumos").assertIsDisplayed()

        // --- INICIO → PASO 1 --------------------------------------------------
        tocar("➕ CONTAR INSUMOS")
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        regla.onNodeWithText("Paso 1 de 3").assertIsDisplayed()
        for (producto in listOf(
            "Frasco de orina",
            "Cryotubo",
            "Caja coprológica",
            "Baciloscopia",
            "Falcon",
        )) {
            regla.onNodeWithText(producto).assertExists()
        }

        // --- PASO 1 → PASO 2 --------------------------------------------------
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithText("Paso 2 de 3").assertIsDisplayed()

        // --- Teclado PROPIO escribe "10" --------------------------------------
        regla.onNodeWithContentDescription("Tecla 1").performScrollTo().performClick()
        regla.onNodeWithContentDescription("Tecla 0").performScrollTo().performClick()
        regla.onNodeWithText("10").assertIsDisplayed() // el "10" gigante se pinta
        regla.onNodeWithText("Primero escribe cuántos").assertDoesNotExist()

        // --- PASO 2 → PASO 3 (muestra la pregunta y el total) ------------------
        tocar("SEGUIR →")
        regla.onNodeWithText("¿Guardamos esto?").assertIsDisplayed()
        regla.onNodeWithText("Paso 3 de 3").assertIsDisplayed()
        // FASE 3: los totales grandes se pintan SIN decimales (MoneyText por
        // defecto usa CurrencyFormat.formatEntero, es-CO con HALF_UP): el total
        // 10 × 7.18 = 71.8 sube a "…72". Se busca "72" como subcadena para no
        // atarnos a "$72" vs "$ 72" de cada CLDR/ICU.
        esperarTexto("72", substring = true)
        assertTrue(
            "El total grande del Paso 3 debe pintarse SIN decimales",
            regla.onAllNodesWithText("72", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )
        // Regresión FASE 3: el formato viejo con decimales ya NO debe existir.
        regla.onNodeWithText("71,80", substring = true).assertDoesNotExist()

        // --- PASO 3 → GUARDAR TODO → ¡GUARDADO! -------------------------------
        tocar("✔ GUARDAR TODO")
        esperarTexto("¡Guardado!") // el insert corre en IO: esperar la transición
        regla.onNodeWithText("¡Guardado!").assertIsDisplayed()
        // Resumen del lote: "N unidades · Total" (conteo de unidades guardadas).
        regla.onNodeWithText("10 productos", substring = true).assertIsDisplayed()
        regla.onNodeWithText("↩ DESHACER").assertIsDisplayed() // ventana de ~5 s abierta
    }

    // ---------------------------------------------------------------------
    // 2) Pasos 6 y 7: HISTORIAL y DETALLE DE LA SEMANA (los 7 pasos en orden)
    // ---------------------------------------------------------------------

    @Test
    fun historialYDetalleDeLaSemanaSonAlcanzablesTrasGuardar() {
        esperarTexto("0 insumos")

        // --- PASO 6: HISTORIAL vacío -------------------------------------------
        tocar("📋 VER REGISTROS")
        regla.onNodeWithText("Registro semanal").assertIsDisplayed()
        regla.onNodeWithText("Aún no has registrado nada.").assertIsDisplayed()
        regla.onNodeWithContentDescription("Atrás, volver al inicio").performClick()
        esperarTexto("¿Qué hacemos hoy?")

        // --- Guarda un registro (mismo camino que FlujoBasicoTest.test1) --------
        tocar("➕ CONTAR INSUMOS")
        regla.onNodeWithText("Frasco de orina").performScrollTo().performClick()
        regla.onNodeWithContentDescription("Tecla 1").performScrollTo().performClick()
        regla.onNodeWithContentDescription("Tecla 0").performScrollTo().performClick()
        tocar("SEGUIR →")
        regla.onNodeWithText("¿Guardamos esto?").assertIsDisplayed()
        tocar("✔ GUARDAR TODO")
        esperarTexto("¡Guardado!")

        // "LISTO" vuelve al inicio; si la ventana de 5 s se cerrara sola, la app
        // ya estaría en INICIO — el test acepta ambos caminos para no ser frágil.
        regla.waitUntil(10_000) {
            (regla.onAllNodesWithText("LISTO").fetchSemanticsNodes() +
                regla.onAllNodesWithText("¿Qué hacemos hoy?").fetchSemanticsNodes()).isNotEmpty()
        }
        if (regla.onAllNodesWithText("LISTO").fetchSemanticsNodes().isNotEmpty()) {
            regla.onNodeWithText("LISTO").performScrollTo().performClick()
        }
        esperarTexto("¿Qué hacemos hoy?")

        // --- PASO 6 de nuevo: ahora la semana tiene el registro -----------------
        tocar("📋 VER REGISTROS")
        regla.onNodeWithText("Registro semanal").assertIsDisplayed()
        esperarTexto("Semana ", substring = true) // la tarjeta de la semana con datos

        // --- PASO 7: DETALLE DE LA SEMANA ---------------------------------------
        regla.onNodeWithText("Semana ", substring = true).performClick()
        esperarTexto("🗑 Borrar")
        regla.onNodeWithText("🗑 Borrar").assertIsDisplayed()

        // --- Regreso en orden: DETALLE → HISTORIAL → INICIO ----------------------
        regla.onNodeWithContentDescription("Atrás, volver al historial").performClick()
        regla.onNodeWithText("Registro semanal").assertIsDisplayed()
        regla.onNodeWithContentDescription("Atrás, volver al inicio").performClick()
        esperarTexto("¿Qué hacemos hoy?")
    }
}
