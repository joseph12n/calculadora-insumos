package com.bioplast.insumos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios del catálogo [ProductType]:
 * precios literales de los 5 insumos, fórmula `totalCop = cantidad × precioUnitario`
 * y round-trip de persistencia (`fromNombre` / `byId`).
 */
class ProductTypeTest {

    // ---------------------------------------------------------------------
    // Precios literales (prompt_de_construcci_n.md §2)
    // ---------------------------------------------------------------------

    @Test
    fun losCincoInsumosTienenElPrecioLiteral() {
        assertEquals(5, ProductType.entries.size)
        assertEquals(7.18, ProductType.FRASCO_ORINA.unitPriceCop, 0.0)
        assertEquals(7.90, ProductType.CRYOTUBO.unitPriceCop, 0.0)
        assertEquals(5.00, ProductType.CAJA_COPROLOGICA.unitPriceCop, 0.0)
        assertEquals(9.88, ProductType.BACILOSCOPIA.unitPriceCop, 0.0)
        assertEquals(7.90, ProductType.FALCON.unitPriceCop, 0.0)
    }

    // ---------------------------------------------------------------------
    // Fórmula: totalCop = cantidad × precioUnitario
    // ---------------------------------------------------------------------

    @Test
    fun totalParaCeroEsCero() {
        for (producto in ProductType.entries) {
            assertEquals(0.0, producto.totalPara(0), 0.0)
        }
    }

    @Test
    fun totalParaUnoEsElPrecioUnitario() {
        for (producto in ProductType.entries) {
            assertEquals(producto.unitPriceCop, producto.totalPara(1), 0.0)
        }
        assertEquals(7.18, ProductType.FRASCO_ORINA.totalPara(1), 0.0)
    }

    @Test
    fun totalParaDiezEsElPrecioPorDiez() {
        assertEquals(71.8, ProductType.FRASCO_ORINA.totalPara(10), 1e-9)
        assertEquals(79.0, ProductType.CRYOTUBO.totalPara(10), 1e-9)
        assertEquals(50.0, ProductType.CAJA_COPROLOGICA.totalPara(10), 1e-9)
        assertEquals(98.8, ProductType.BACILOSCOPIA.totalPara(10), 1e-9)
        assertEquals(79.0, ProductType.FALCON.totalPara(10), 1e-9)
    }

    @Test
    fun totalParaOnceTieneResiduoBinarioDocumentado() {
        // 11 × 7.18 = 78.97999999999999 en Double: por eso el redondeo a 2 decimales
        // se aplica al mostrar (CurrencyFormat.formatConDecimalesRedondeados) y al persistir.
        val total = ProductType.FRASCO_ORINA.totalPara(11)
        assertEquals(78.98, total, 1e-9)
        assertTrue(
            "El Double crudo debe conservar el residuo binario (demuestra la necesidad del redondeo)",
            total != 78.98,
        )
    }

    // ---------------------------------------------------------------------
    // fromNombre: round-trip con productName persistido (displayName)
    // ---------------------------------------------------------------------

    @Test
    fun fromNombreRoundTripPorDisplayName() {
        for (producto in ProductType.entries) {
            assertEquals(producto, ProductType.fromNombre(producto.displayName))
        }
    }

    @Test
    fun fromNombreAceptaLaConstanteCaseInsensitiveYEspacios() {
        assertEquals(ProductType.FRASCO_ORINA, ProductType.fromNombre("  frasco_orina  "))
        assertEquals(ProductType.CAJA_COPROLOGICA, ProductType.fromNombre("CAJA_COPROLOGICA"))
        assertEquals(ProductType.FALCON, ProductType.fromNombre("falcon"))
    }

    @Test
    fun fromNombreNuloVacioODesconocidoDevuelveNull() {
        assertNull(ProductType.fromNombre(null))
        assertNull(ProductType.fromNombre(""))
        assertNull(ProductType.fromNombre("    "))
        assertNull(ProductType.fromNombre("Insumo fantasma"))
    }

    // ---------------------------------------------------------------------
    // byId: round-trip con el nombre de la constante del enum
    // ---------------------------------------------------------------------

    @Test
    fun byIdRoundTripPorConstante() {
        for (producto in ProductType.entries) {
            assertEquals(producto, ProductType.byId(producto.name))
            assertEquals(producto, ProductType.byId(producto.name.lowercase()))
        }
    }

    @Test
    fun byIdInvalidosDevuelvenNull() {
        assertNull(ProductType.byId(null))
        assertNull(ProductType.byId(""))
        assertNull(ProductType.byId("   "))
        assertNull(ProductType.byId("CRYO_TUBO"))
        // byId SOLO acepta la constante del enum, no el displayName:
        assertNull(ProductType.byId("Frasco de orina"))
    }
}
