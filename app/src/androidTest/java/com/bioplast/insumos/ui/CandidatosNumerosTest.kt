package com.bioplast.insumos.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bioplast.insumos.ui.components.CandidatosNumeros
import com.bioplast.insumos.ui.theme.CalculadoraInsumosTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Contrato del diálogo de confirmación de la foto:
 *  - UN número → pregunta "¿Es este el número de tu hoja?" y el botón verde lo usa.
 *  - VARIOS → pregunta "¿Cuál número ves en tu hoja?" y cada botón elige el suyo.
 *  - "✍️ Escribir a mano" cierra sin tocar la cantidad.
 *
 * (Los recortes de la letra son Bitmap; aquí se prueba el contrato de la UI sin
 * ellos, que ya está cubierto por [com.bioplast.insumos.camera.FotoOcrTest].)
 */
@RunWith(AndroidJUnit4::class)
class CandidatosNumerosTest {

    @get:Rule
    val regla = createComposeRule()

    @Test
    fun unNumeroPideConfirmacionYElBotonVerdeLoUsa() {
        var elegido: Int? = null
        regla.setContent {
            CalculadoraInsumosTheme {
                CandidatosNumeros(
                    ocrCandidatos = listOf(25),
                    onElegido = { elegido = it },
                    onCerrar = {},
                )
            }
        }

        regla.onNodeWithText("¿Es este el número de tu hoja?").assertIsDisplayed()
        regla.onNodeWithText("25").assertIsDisplayed()
        regla.onNodeWithText("✔ Usar 25").assertIsDisplayed().performClick()
        assertEquals(25, elegido)
    }

    @Test
    fun variosNumerosSeEligenPorBoton() {
        var elegido: Int? = null
        regla.setContent {
            CalculadoraInsumosTheme {
                CandidatosNumeros(
                    ocrCandidatos = listOf(10, 25),
                    onElegido = { elegido = it },
                    onCerrar = {},
                )
            }
        }

        regla.onNodeWithText("¿Cuál número ves en tu hoja?").assertIsDisplayed()
        regla.onNodeWithContentDescription("Elegir el número 25").performClick()
        assertEquals(25, elegido)
    }

    @Test
    fun escribirAManoCierraSinElegir() {
        var cerrado = false
        var elegido: Int? = null
        regla.setContent {
            CalculadoraInsumosTheme {
                CandidatosNumeros(
                    ocrCandidatos = listOf(7),
                    onElegido = { elegido = it },
                    onCerrar = { cerrado = true },
                )
            }
        }

        regla.onNodeWithText("✍️ Escribir a mano").performClick()
        assertEquals(true, cerrado)
        assertEquals(null, elegido)
    }
}
