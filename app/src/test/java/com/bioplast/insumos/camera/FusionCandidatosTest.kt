package com.bioplast.insumos.camera

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de la FUSIÓN de candidatos de la foto (pura, sin Android).
 *
 * Contrato de [fusionarNumeros]:
 *  - El ORDEN manda: primero los recortes refinados, después la pasada procesada
 *    y al final la original (la primera aparición gana).
 *  - Deduplica conservando el orden.
 *  - Descarta lo que no puede ser cantidad (fuera de 0..999.999).
 *  - Corta en el tope de candidatos.
 */
class FusionCandidatosTest {

    @Test
    fun priorizaElPrimerGrupoYConservaElOrden() {
        val fusion = fusionarNumeros(
            listOf(25, 10), // recortes refinados (máxima prioridad)
            listOf(10, 7),  // pasada procesada
            listOf(3, 25),  // pasada original
        )
        assertEquals(listOf(25, 10, 7, 3), fusion)
    }

    @Test
    fun descartaValoresQueNoSonCantidad() {
        val fusion = fusionarNumeros(
            listOf(-1, 7, 1_000_000, 0, 999_999),
        )
        assertEquals(listOf(7, 0, 999_999), fusion)
    }

    @Test
    fun cortaEnElTopeDeCandidatos() {
        val fusion = fusionarNumeros((1..40).toList())
        assertEquals(MAXIMO_CANDIDATOS_IMAGEN, fusion.size)
        assertEquals((1..MAXIMO_CANDIDATOS_IMAGEN).toList(), fusion)
    }

    @Test
    fun listasVaciasDanListaVacia() {
        assertEquals(emptyList<Int>(), fusionarNumeros(emptyList(), emptyList(), emptyList()))
    }
}
