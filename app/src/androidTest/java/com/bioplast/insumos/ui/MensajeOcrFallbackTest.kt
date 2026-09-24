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
 * Fallback de la lectura de la FOTO: "No pude leer números en la foto.
 * Escríbelo tú mismo." sin borrar la cantidad que la persona ya había escrito.
 *
 * DECISIÓN DE IMPLEMENTACIÓN DEL TEST: el camino 100 % por UI exige abrir la
 * cámara del sistema con `ActivityResultContracts.TakePicture` (necesita app de
 * cámara real) y elegir una foto — no automatizable de forma estable sin un
 * dispositivo con cámara. MainActivity.kt hace, tras leer la foto:
 * `viewModel.onImagenLeida(numeros)` (lista vacía cuando no se leyó nada).
 * Por eso este suite prueba las DOS mitades de ese contrato exacto:
 *
 *  1. [laUiDeLaCalculadoraMuestraElMensajeSinBorrarLoEscrito]: nivel de
 *     COMPOSICIÓN: compone [CalculatorScreen] con un estado ya resultante
 *     (`ocrMessage = MENSAJE_IMAGEN_FALLIDO`, `quantityInput = "25"`) y verifica
 *     que el mensaje aparece y la cifra NO desaparece.
 *  2. [laViewModelRealConservaLaCantidadCuandoLaFotoFalla]: invoca la ViewModel
 *     REAL —misma llamada que haría MainActivity con una foto ilegible—
 *     `onImagenLeida(emptyList())` y verifica `ocrMessage` + `quantityInput`.
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
    fun laUiDeLaCalculadoraMuestraElMensajeDeLaFotoSinBorrarLoEscrito() {
        regla.setContent {
            CalculadoraInsumosTheme {
                CalculatorScreen(
                    quantityInput = "25",
                    producto = ProductType.FRASCO_ORINA,
                    quantityError = false,
                    productError = false,
                    totalCop = ProductType.FRASCO_ORINA.totalPara(25),
                    isSaving = false,
                    ocrMessage = InventoryViewModel.MENSAJE_IMAGEN_FALLIDO,
                    savingError = null,
                    onDigito = {},
                    onBorrarDigito = {},
                    onProductoElegido = {},
                    onHacerFoto = {},
                    onSiguiente = {},
                    onAtras = {},
                )
            }
        }

        // El mensaje literal está en pantalla…
        regla.onNodeWithText("No pude leer números en la foto. Escríbelo tú mismo.")
            .assertIsDisplayed()
        // …y la cantidad escrita ANTES del fallo NO se borró.
        regla.onNodeWithText("25").assertIsDisplayed()
        // El fallo de lectura no es un error de validación: no muestra el aviso rojo.
        regla.onNodeWithText("Primero escribe cuántos").assertDoesNotExist()
    }

    // ---------------------------------------------------------------------
    // 2) ViewModel real: la MISMA llamada que hace MainActivity con la foto vacía
    // ---------------------------------------------------------------------

    @Test
    fun laViewModelRealConservaLaCantidadCuandoLaFotoFalla() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel =
            InventoryViewModel.factory(context).create(InventoryViewModel::class.java)

        // Persona en la calculadora que ya escribió 25 a mano…
        viewModel.onEmpezarConteo()
        viewModel.onProductoElegido(ProductType.FRASCO_ORINA)
        viewModel.onDigito("2")
        viewModel.onDigito("5")
        assertEquals("25", viewModel.uiState.value.quantityInput)

        // …y la foto sale ilegible: exactamente la llamada de MainActivity
        // (extraerNumerosDeImagen devuelve lista vacía → onImagenLeida(emptyList())).
        viewModel.onImagenLeida(emptyList())

        val estado = viewModel.uiState.value
        assertEquals(
            "Debe mostrarse el mensaje de fallback literal",
            "No pude leer números en la foto. Escríbelo tú mismo.",
            estado.ocrMessage,
        )
        assertEquals(
            InventoryViewModel.MENSAJE_IMAGEN_FALLIDO,
            estado.ocrMessage,
        )
        assertEquals(
            "La foto ilegible NUNCA borra lo que la persona escribió",
            "25",
            estado.quantityInput,
        )
        assertEquals(
            "El fallo de la foto no saca de la calculadora",
            Screen.CALCULATOR,
            estado.screen,
        )
    }
}
