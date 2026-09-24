package com.bioplast.insumos.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitarios de [extraerMejorNumero] (extracción de número del texto OCR).
 * Debe vivir en el MISMO paquete que la fuente porque la función es `internal`.
 */
class ExtraerNumeroTest {

    @Test
    fun enteroPuroLoDevuelveTalCual() {
        assertEquals(12, extraerMejorNumero("12"))
        assertEquals(3, extraerMejorNumero("3"))
        assertEquals(9_999_999, extraerMejorNumero("9999999")) // 7 dígitos: máximo permitido
    }

    @Test
    fun tokenMixtoConDosPuntosExtraeElNumero() {
        // Pasada 2: primera corrida de dígitos dentro de un token mixto.
        assertEquals(12, extraerMejorNumero("x:12"))
        assertEquals(45, extraerMejorNumero("CANT=45"))
    }

    @Test
    fun textoSinDigitosDevuelveNull() {
        assertNull(extraerMejorNumero("abc"))
        assertNull(extraerMejorNumero(""))
        assertNull(extraerMejorNumero("   \n  "))
        assertNull(extraerMejorNumero("sin números aquí"))
    }

    @Test
    fun fechasSeDescartan() {
        // "2026-09-23" y "23/09/2026" empiezan como fecha → no son cantidades.
        assertNull(extraerMejorNumero("2026-09-23"))
        assertNull(extraerMejorNumero("23/09/2026"))
        assertNull(extraerMejorNumero("Fecha: 2026-09-23 lote x"))
    }

    @Test
    fun decimalConPuntoTomaElEnteroIzquierdo() {
        assertEquals(7, extraerMejorNumero("7.18"))
    }

    @Test
    fun tokensDeMasDeSieteDigitosSeIgnoran() {
        // Pasada 1: token puro demasiado largo → descartado.
        assertNull(extraerMejorNumero("12345678"))
        // Pasada 2: corrida de más de 7 dígitos dentro de token mixto → descartado.
        assertNull(extraerMejorNumero("Lote 12345678"))
        assertNull(extraerMejorNumero("SERIE 1234567890123"))
        assertNull(extraerMejorNumero("123456789012345"))
    }

    @Test
    fun tokenPuroTienePrioridadSobreElMixto() {
        // "x:5" aparece primero, pero el primer token 100% dígitos es "42"
        // → debe ganar la pasada 1 (no "5" ni el posterior "17").
        assertEquals(42, extraerMejorNumero("x:5 42 17"))
    }

    @Test
    fun variosTokensMixtosTomaElPrimeroEnOrdenDeLectura() {
        assertEquals(123, extraerMejorNumero("abc123 def456"))
        assertEquals(8, extraerMejorNumero("lote:8 fecha:2026-09-23"))
    }

    @Test
    fun respetaSaltosDeLineaComoSeparadores() {
        assertEquals(12, extraerMejorNumero("CANT\n12"))
        assertEquals(9, extraerMejorNumero("  9  "))
    }
}
