package com.bioplast.insumos.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios de [CurrencyFormat]: formato COP con `Locale("es", "CO")`
 * (coma decimal), redondeo HALF_UP a 2 decimales y corrección del residuo binario
 * de los Doubles. Se usan `contains` para no depender de `$ ` vs `$` ni de los
 * espacios no separables que decide cada versión de CLDR/JDK.
 */
class CurrencyFormatTest {

    @Test
    fun formatUsaComaDecimalEsCo() {
        val texto = CurrencyFormat.format(71.8)
        assertTrue("Se esperaba la coma decimal '71,80' en [$texto]", texto.contains("71,80"))
    }

    @Test
    fun formatDeEnterosUsaDosDecimales() {
        val cinco = CurrencyFormat.format(5.0)
        assertTrue("Se esperaba '5,00' en [$cinco]", cinco.contains("5,00"))

        val cero = CurrencyFormat.format(0.0)
        assertTrue("Se esperaba '0,00' en [$cero]", cero.contains("0,00"))
    }

    @Test
    fun formatSinRedondeoDejaPasarElResiduoBinario() {
        // Contraste con formatConDecimalesRedondeados: el format crudo formatea
        // el valor más cercano de 78.97999999999999 → "78,98" de todas formas
        // porque el formato COP solo pide 2 decimales; la prueba clave de redondeo
        // está en el test de abajo (residuo en el 3er decimal).
        val bruto = ProductType.FRASCO_ORINA.totalPara(11) // 78.97999999999999
        assertTrue(CurrencyFormat.format(bruto).contains("78,98"))
    }

    @Test
    fun formatConDecimalesRedondeadosCorrigeElResiduoBinario() {
        // 11 × 7.18 = 78.97999999999999 → debe mostrar "$78,98" con coma decimal.
        val texto = CurrencyFormat.formatConDecimalesRedondeados(
            ProductType.FRASCO_ORINA.totalPara(11),
        )
        assertTrue("Se esperaba '78,98' en [$texto]", texto.contains("78,98"))
    }

    @Test
    fun halfUpRedondeaSetentaYUnoComaOchoCincoCincoHaciaArriba() {
        // Requisito: 71.855 → "71,86" (HALF_UP, no HALF_EVEN que además
        // aquí redondearía igual pero cambia en empates exactos, ver test siguiente).
        val texto = CurrencyFormat.formatConDecimalesRedondeados(71.855)
        assertTrue("Se esperaba '71,86' en [$texto]", texto.contains("71,86"))
    }

    @Test
    fun halfUpSeDiferenciaDeHalfEvenEnEmpatesExactos() {
        // 71.125 es EXACTO en binario y su 3er decimal es 5 con par anterior par:
        //  - HALF_UP   → 71,13
        //  - HALF_EVEN → 71,12  (el bug que este redondeo explícito evita)
        val texto = CurrencyFormat.formatConDecimalesRedondeados(71.125)
        assertTrue(
            "Se esperaba '71,13' (HALF_UP) en [$texto]; si da '71,12' se está usando HALF_EVEN",
            texto.contains("71,13"),
        )
    }

    @Test
    fun negativosConservanElSignoYRedondeanHaciaFuera() {
        val negativo = CurrencyFormat.formatConDecimalesRedondeados(-71.8)
        assertTrue("[$negativo] debe contener el signo menos", negativo.contains("-"))
        assertTrue("Se esperaba '71,80' en [$negativo]", negativo.contains("71,80"))

        // HALF_UP aleja de cero también en negativos: -71.855 → "-71,86".
        val negativoEmpate = CurrencyFormat.formatConDecimalesRedondeados(-71.855)
        assertTrue(
            "Se esperaba '71,86' con signo en [$negativoEmpate]",
            negativoEmpate.contains("71,86") && negativoEmpate.contains("-"),
        )
    }

    @Test
    fun ceroFormateaComoMonedaSinSignoNegativo() {
        val texto = CurrencyFormat.formatConDecimalesRedondeados(0.0)
        assertTrue("Se esperaba '0,00' en [$texto]", texto.contains("0,00"))
        assertFalse("El cero no debe llevar signo menos [$texto]", texto.contains("-"))
    }

    // ---------------------------------------------------------------------
    // formatEntero (FASE 3): los TOTALES en pantalla salen SIN decimales
    // ---------------------------------------------------------------------

    @Test
    fun formatEnteroDeSetentaYUnoPuntoCincoSubeASetentaYDos() {
        // 71.5 → BigDecimal.setScale(0, HALF_UP) → 72 → es-CO "…72".
        val texto = CurrencyFormat.formatEntero(71.5)
        assertTrue("Se esperaba '72' en [$texto]", texto.contains("72"))
        assertFalse("No debe llevar coma decimal [$texto]", texto.contains(","))
        assertFalse("No debe llevar punto decimal [$texto]", texto.contains("."))
        assertTrue(
            "Debe llevar el símbolo/agnóstico de moneda es-CO en [$texto]",
            texto.contains("\$") || texto.contains("COP"),
        )
    }

    @Test
    fun formatEnteroDelResiduoBinarioDaElEnteroCorrecto() {
        // 11 × 7.18 = 78.97999999999999 (residuo binario) → HALF_UP entero → 79.
        val texto = CurrencyFormat.formatEntero(ProductType.FRASCO_ORINA.totalPara(11))
        assertTrue("Se esperaba '79' en [$texto]", texto.contains("79"))
        assertFalse("Sin decimales [$texto]", texto.contains(","))
        assertFalse("Sin decimales [$texto]", texto.contains("."))
    }

    @Test
    fun formatEnteroDeCeroEsCeroSinDecimalesNiSignoMenos() {
        val texto = CurrencyFormat.formatEntero(0.0)
        assertTrue("Se esperaba '0' en [$texto]", texto.contains("0"))
        assertFalse("Sin decimales [$texto]", texto.contains(","))
        assertFalse("El cero no debe llevar signo menos [$texto]", texto.contains("-"))
    }

    @Test
    fun formatEnteroDelTotalDeDiezFrascosEsElQuePintaElPaso3() {
        // Contrato de UI (FlujoBasicoTest): 10 × 7.18 = 71.8 → "…72", y el
        // formato viejo con decimales ("71,80") ya NO debe aparecer.
        val texto = CurrencyFormat.formatEntero(ProductType.FRASCO_ORINA.totalPara(10))
        assertTrue("Se esperaba '72' en [$texto]", texto.contains("72"))
        assertFalse(
            "El total grande ya no muestra decimales [$texto]",
            texto.contains("71,80"),
        )
    }
}
