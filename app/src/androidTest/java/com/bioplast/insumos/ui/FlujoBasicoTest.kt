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
 * Cobertura (flujo de la calculadora, ACTUALIZADO al rediseño):
 *  1. INICIO muestra "¿Qué hacemos hoy?" + CONTAR; CONTAR abre la calculadora
 *     con el diálogo "¿Qué vas a contar?" (los 5 insumos); elegir "Frasco de
 *     orina" lo cierra y deja el visor "¿Cuántos?"; el teclado PROPIO escribe
 *     "10" (el número gigante se pinta y el error "Primero escribe cuántos" NO
 *     aparece); el visor muestra el total EN VIVO (10 × 7.18 = 71.8 → "$72");
 *     SEGUIR abre la revisión con "¿Guardamos esto?" y "✔ GUARDAR TODO" lleva a
 *     ¡GUARDADO! con "10 productos · Total" y "Deshacer" disponible.
 *  2. El HISTORIAL y el DETALLE DE LA SEMANA son alcanzables en orden tras
 *     guardar, y "Atrás" regresa por el mismo camino hasta INICIO.
 *
 * Base de datos: cada test empieza con la tabla `inventory_records` vacía
 * (véase [vaciarLaBase]); el registro que crea GUARDAR se limpia en @After.
 *
 * NOTA: usa `performScrollTo()` antes de tocar nodos que viven dentro de
 * Column con `verticalScroll` (Inicio/Revisión/Éxito) para que el test también
 * pase en pantallas bajas o con fuente al 200 %.
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

    /** Toca un nodo que puede estar fuera del viewport (Column + verticalScroll). */
    private fun tocar(texto: String) {
        regla.onNodeWithText(texto).performScrollTo().performClick()
    }

    /**
     * Recorrido corto hasta la revisión: CONTAR → elegir "Frasco de orina" →
     * teclear 10. Deja la app en la calculadora con el visor listo.
     */
    private fun escribirFrascoDeOrinaDiez() {
        tocar("➕ CONTAR INSUMOS")
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        regla.onNodeWithText("Frasco de orina").performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithContentDescription("Tecla 1").performClick()
        regla.onNodeWithContentDescription("Tecla 0").performClick()
    }

    // ---------------------------------------------------------------------
    // 1) Flujo de registro: INICIO → CALCULADORA → REVISIÓN → ¡GUARDADO!
    // ---------------------------------------------------------------------

    @Test
    fun flujoDeRegistroTerminaEnExitoConDeshacerDisponible() {
        // --- INICIO (estado inicial vacío: la tabla se vació en @Before) -------
        esperarTexto("0 insumos") // espera a que Room emita el resumen vacío
        regla.onNodeWithText("¿Qué hacemos hoy?").assertIsDisplayed()
        regla.onNodeWithText("0 insumos").assertIsDisplayed()

        // --- INICIO → CALCULADORA (diálogo del insumo abierto solo) -----------
        tocar("➕ CONTAR INSUMOS")
        regla.onNodeWithText("¿Qué vas a contar?").assertIsDisplayed()
        for (producto in listOf(
            "Frasco de orina",
            "Cryotubo",
            "Caja coprológica",
            "Baciloscopia",
            "Falcon",
        )) {
            regla.onNodeWithText(producto).assertExists()
        }

        // --- Elegir el insumo → visor "¿Cuántos?" -----------------------------
        regla.onNodeWithText("Frasco de orina").performClick()
        regla.onNodeWithText("¿Cuántos?").assertIsDisplayed()
        regla.onNodeWithText("Frasco de orina").assertIsDisplayed() // fila del insumo

        // --- Teclado PROPIO escribe "10" --------------------------------------
        regla.onNodeWithContentDescription("Tecla 1").performClick()
        regla.onNodeWithContentDescription("Tecla 0").performClick()
        regla.onNodeWithText("10").assertIsDisplayed() // la cifra del visor se pinta
        regla.onNodeWithText("Primero escribe cuántos").assertDoesNotExist()

        // --- Total EN VIVO en el visor: 10 × 7.18 = 71.8 → "$72" --------------
        esperarTexto("72", substring = true)
        assertTrue(
            "El visor debe mostrar el total en vivo sin decimales",
            regla.onAllNodesWithText("72", substring = true).fetchSemanticsNodes().isNotEmpty(),
        )

        // --- CALCULADORA → REVISIÓN -------------------------------------------
        regla.onNodeWithText("SEGUIR →").performClick()
        regla.onNodeWithText("¿Guardamos esto?").assertIsDisplayed()
        regla.onNodeWithText("10 Frasco de orina").assertIsDisplayed()

        // --- REVISIÓN → GUARDAR TODO → ¡GUARDADO! ------------------------------
        tocar("✔ GUARDAR TODO")
        esperarTexto("¡Guardado!") // el insert corre en IO: esperar la transición
        regla.onNodeWithText("¡Guardado!").assertIsDisplayed()
        // Resumen del lote: "N unidades · Total" (conteo de unidades guardadas).
        regla.onNodeWithText("10 productos", substring = true).assertIsDisplayed()
        regla.onNodeWithText("↩ DESHACER").assertIsDisplayed() // ventana de ~5 s abierta
    }

    // ---------------------------------------------------------------------
    // 2) HISTORIAL y DETALLE DE LA SEMANA (alcanzables tras guardar)
    // ---------------------------------------------------------------------

    @Test
    fun historialYDetalleDeLaSemanaSonAlcanzablesTrasGuardar() {
        esperarTexto("0 insumos")

        // --- HISTORIAL vacío ----------------------------------------------------
        tocar("📋 VER REGISTROS")
        regla.onNodeWithText("Registro semanal").assertIsDisplayed()
        regla.onNodeWithText("Aún no has registrado nada.").assertIsDisplayed()
        regla.onNodeWithContentDescription("Atrás, volver al inicio").performClick()
        esperarTexto("¿Qué hacemos hoy?")

        // --- Guarda un registro (mismo camino que el test 1) --------------------
        escribirFrascoDeOrinaDiez()
        regla.onNodeWithText("SEGUIR →").performClick()
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

        // --- HISTORIAL de nuevo: ahora la semana tiene el registro --------------
        tocar("📋 VER REGISTROS")
        regla.onNodeWithText("Registro semanal").assertIsDisplayed()
        esperarTexto("Semana ", substring = true) // la tarjeta de la semana con datos

        // --- DETALLE DE LA SEMANA ------------------------------------------------
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
