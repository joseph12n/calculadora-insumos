package com.bioplast.insumos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bioplast.insumos.InventoryViewModel
import com.bioplast.insumos.data.AppDatabase
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.model.Screen
import com.bioplast.insumos.ui.theme.CalculadoraInsumosTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fallback del OCR: "No pude leer el número. Escríbelo tú mismo." sin borrar
 * la cantidad que la persona ya había escrito.
 *
 * DECISIÓN DE IMPLEMENTACIÓN DEL TEST (documentada como pide la consigna):
 * el camino 100 % por UI exige abrir [CameraScanScreen] (permiso de cámara +
 * hardware real) y esperar su cierre con `null` — no automatizable de forma
 * estable sin un dispositivo con cámara/permisos. MainActivity.kt:141-144 hace,
 * al cerrar la cámara: `viewModel.onEscanearResultado(numero)` con `numero == null`.
 * Por eso este suite prueba las DOS mitades de ese contrato exacto:
 *
 *  1. [laUiDelPaso2MuestraElMensajeSinBorrarLoEscrito]: nivel de COMPOSICIÓN
 *     (autorizado por la consigna): compone [EnterQuantityScreen] con un estado
 *     ya resultante (`ocrMessage = MENSAJE_OCR_FALLIDO`, `quantityInput = "25"`)
 *     y verifica que el mensaje aparece y la cifra NO desaparece.
 *  2. [laViewModelRealConservaLaCantidadCuandoElOcrFalla]: invoca la ViewModel
 *     REAL —misma llamada que haría MainActivity tras el fallo de cámara—
 *     `onEscanearResultado(null)` y verifica `ocrMessage` + `quantityInput`.
 *
 * Para probar también el camino de cámara completo, conectar un celular y
 * ampliar este suite con un test de UI sobre CameraScanScreen (corrida posterior).
 */
@RunWith(AndroidJUnit4::class)
class MensajeOcrFallbackTest {

    /** Content rule: no lanza MainActivity; compone solo lo que se le pida. */
    @get:Rule
    val regla = createComposeRule()

    @Before
    fun limpiarLaBase() {
        // La ViewModel real del test 2 abre la DB singleton de la app.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        AppDatabase.getInstance(context)
            .openHelper.writableDatabase
            .execSQL("DELETE FROM inventory_records")
    }

    // ---------------------------------------------------------------------
    // 1) UI (nivel composición): mensaje visible + cantidad intacta
    // ---------------------------------------------------------------------

    @Test
    fun laUiDelPaso2MuestraElMensajeDelOcrSinBorrarLoEscrito() {
        regla.setContent {
            CalculadoraInsumosTheme {
                EnterQuantityScreen(
                    quantityInput = "25",
                    producto = ProductType.FRASCO_ORINA,
                    quantityError = false,
                    isSaving = false,
                    ocrMessage = InventoryViewModel.MENSAJE_OCR_FALLIDO,
                    savingError = null,
                    onDigito = {},
                    onBorrarDigito = {},
                    onEscanear = {},
                    onSiguiente = {},
                    onAtras = {},
                )
            }
        }

        // El mensaje literal de la consigna está en pantalla…
        regla.onNodeWithText("No pude leer el número. Escríbelo tú mismo.")
            .assertIsDisplayed()
        // …y la cantidad escrita ANTES del fallo NO se borró.
        regla.onNodeWithText("25").assertIsDisplayed()
        // El fallo de OCR no es un error de validación: no muestra el aviso rojo.
        regla.onNodeWithText("Primero escribe cuántos").assertDoesNotExist()
    }

    // ---------------------------------------------------------------------
    // 2) ViewModel real: la MISMA llamada que hace MainActivity con el null
    // ---------------------------------------------------------------------

    @Test
    fun laViewModelRealConservaLaCantidadCuandoElOcrFalla() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel =
            InventoryViewModel.factory(context).create(InventoryViewModel::class.java)

        // Persona en el PASO 2 que ya escribió 25 a mano…
        viewModel.onProductoElegido(ProductType.FRASCO_ORINA)
        viewModel.onDigito("2")
        viewModel.onDigito("5")
        assertEquals("25", viewModel.uiState.value.quantityInput)

        // …y la cámara falla: exactamente la llamada de MainActivity.kt:142
        // (CameraScanScreen entrega null → onResultado(null)).
        viewModel.onEscanearResultado(null)

        val estado = viewModel.uiState.value
        assertEquals(
            "Debe mostrarse el mensaje de fallback literal",
            "No pude leer el número. Escríbelo tú mismo.",
            estado.ocrMessage,
        )
        assertEquals(
            InventoryViewModel.MENSAJE_OCR_FALLIDO,
            estado.ocrMessage,
        )
        assertEquals(
            "El OCR fallido NUNCA borra lo que la persona escribió",
            "25",
            estado.quantityInput,
        )
        assertEquals(
            "El fallo de OCR no saca del PASO 2",
            Screen.ENTER_QUANTITY,
            estado.screen,
        )
    }
}
