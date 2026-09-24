package com.bioplast.insumos.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test INSTRUMENTADO del motor de lectura de la foto (ML Kit real, en el
 * dispositivo). Dibuja una "hoja" con varios números (fuente impresa gruesa,
 * como aproximación controlada) y verifica que la lectura RECURSIVA:
 *
 *  1. encuentra todos los números,
 *  2. no devuelve duplicados ni valores fuera de rango,
 *  3. no inventa números en una foto en blanco,
 *  4. el preprocesado conserva el tamaño y deja papel blanco + tinta negra.
 *
 * Requiere un dispositivo/emulador con ML Kit (el mismo que usa la app).
 */
@RunWith(AndroidJUnit4::class)
class FotoOcrTest {

    /** Dibuja [numeros] separados sobre un fondo blanco. */
    private fun hojaConNumeros(vararg numeros: String): Bitmap {
        val ancho = 1600
        val alto = 500
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val pintura = Paint().apply {
            color = Color.BLACK
            textSize = 150f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var x = 70f
        for (numero in numeros) {
            canvas.drawText(numero, x, 320f, pintura)
            x += pintura.measureText(numero) + 160f
        }
        return bitmap
    }

    @Test
    fun laLecturaRecursivaEncuentraLosNumerosDeLaHoja() = runBlocking {
        val hoja = hojaConNumeros("12", "74", "8", "4")
        val numeros = leerNumerosDeBitmap(hoja).map { it.numero }

        for (esperado in listOf(12, 74, 8, 4)) {
            assertTrue("Debe leer el $esperado (leyó $numeros)", numeros.contains(esperado))
        }
        assertEquals("Sin duplicados", numeros.size, numeros.distinct().size)
        assertTrue(
            "Nunca devuelve algo que no quepa como cantidad",
            numeros.all { it in 0..MAXIMO_VALOR_FOTO },
        )
    }

    @Test
    fun elPreprocesadoConservaElTamañoYDejaPapelBlancoConTintaNegra() {
        val hoja = hojaConNumeros("25")
        val procesada = preprocesar(hoja)

        assertEquals(hoja.width, procesada.width)
        assertEquals(hoja.height, procesada.height)

        val pixeles = IntArray(procesada.width * procesada.height)
        procesada.getPixels(
            pixeles, 0, procesada.width, 0, 0, procesada.width, procesada.height,
        )
        var blancos = 0
        var negros = 0
        for (pixel in pixeles) {
            when (pixel) {
                0xFFFFFFFF.toInt() -> blancos++
                0xFF000000.toInt() -> negros++
            }
        }
        assertTrue("Debe predominar el papel blanco", blancos > pixeles.size / 2)
        assertTrue("Debe quedar tinta negra", negros > 0)
    }

    @Test
    fun unaFotoEnBlancoNoInventaNumeros() = runBlocking {
        val blanca = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        assertEquals(emptyList<Int>(), leerNumerosDeBitmap(blanca).map { it.numero })
    }
}
