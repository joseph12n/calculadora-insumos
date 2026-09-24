package com.bioplast.insumos.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests unitarios de [extraerNumerosCandidatos] (candidatos del OCR de FOTO).
 * Debe vivir en el MISMO paquete que la fuente porque la función es `internal`.
 */
class ExtraerNumerosCandidatosTest {

    @Test
    fun tokenPuroLoDevuelveTalCual() {
        assertEquals(listOf(10), extraerNumerosCandidatos("10"))
        assertEquals(listOf(3), extraerNumerosCandidatos("3"))
        assertEquals(listOf(9_999_999), extraerNumerosCandidatos("9999999")) // 7 cifras: máximo
    }

    @Test
    fun excluyePreciosConComaYPunto() {
        // "7,18" y "7.18" son PRECIO (dígitos + separador + exactamente 2 cifras).
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("7,18"))
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("7.18"))
        // En frase: el precio se salta y la cantidad igual se recoge.
        assertEquals(listOf(12), extraerNumerosCandidatos("cantidad 12 precio 7,18"))
        // La primera corrida del token ES el precio → el token no aporta candidato.
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("x:7,18"))
    }

    @Test
    fun excluyeFechas() {
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("23/09/2026"))
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("2026-09-23"))
        // Fecha embebida en un token mixto → se salta el token entero.
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("lote:23/09/2026"))
        // Fecha en frase: solo queda la cantidad.
        assertEquals(listOf(8), extraerNumerosCandidatos("fecha 2026-09-23 cant8"))
    }

    @Test
    fun tokenMixtoExtraeSuPrimeraCorrida() {
        assertEquals(listOf(12), extraerNumerosCandidatos("x:12"))
        assertEquals(listOf(45), extraerNumerosCandidatos("CANT=45"))
        assertEquals(listOf(12), extraerNumerosCandidatos("CANT\n12")) // saltos de línea separan
    }

    @Test
    fun deduplicaPreservandoOrden() {
        assertEquals(listOf(10), extraerNumerosCandidatos("10 10"))
        // También entre pasadas: el puro "10" gana y el mixto "x:10" se descarta.
        assertEquals(listOf(10), extraerNumerosCandidatos("10 x:10"))
        assertEquals(listOf(10, 20), extraerNumerosCandidatos("10 20 10 20"))
    }

    @Test
    fun descartaCorridasDeMasDeSieteCifras() {
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("12345678")) // puro, 8 cifras
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("SERIE 12345678")) // mixto, 8 cifras
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("x:1234567890123")) // corrida enorme
    }

    @Test
    fun variosEnOrdenDeLectura() {
        assertEquals(listOf(10, 20, 30), extraerNumerosCandidatos("10 20 30"))
    }

    @Test
    fun losPurosVanAnteLosMixtosPorConfianza() {
        // Misma prioridad que extraerMejorNumero (puro gana), pero plural: los tokens
        // 100 % dígitos se listan primero, luego los mixtos en orden de lectura.
        assertEquals(listOf(42, 5), extraerNumerosCandidatos("x:5 42"))
    }

    @Test
    fun limitaElNumeroDeCandidatos() {
        val texto = (1..15).joinToString(" ") // 15 tokens puros
        val candidatos = extraerNumerosCandidatos(texto)
        assertEquals(MAXIMO_CANDIDATOS_IMAGEN, candidatos.size) // tope = 12
        assertEquals(listOf(1, 2, 3), candidatos.take(3)) // conserva el orden inicial
    }

    @Test
    fun textoVacioOSinDigitosDevuelveListaVacia() {
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos(""))
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("   \n  "))
        assertEquals(emptyList<Int>(), extraerNumerosCandidatos("sin números aquí"))
    }
}
