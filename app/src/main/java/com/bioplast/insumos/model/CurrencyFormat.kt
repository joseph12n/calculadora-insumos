package com.bioplast.insumos.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Formateo de montos en Pesos colombianos.
 *
 * Fuente única de verdad: `NumberFormat.getCurrencyInstance(Locale("es", "CO"))`.
 * No se altera la configuración por defecto de ese formato (símbolo, separadores y
 * decimales son los que es-CO decide; p. ej. `71.8` → `"$71,80"` con coma decimal).
 *
 * `NumberFormat` **no es thread-safe**: este objeto nunca comparte una instancia
 * viva entre hilos — cada hilo usa su propia copia (ThreadLocal) y [COP_PATTERN]
 * devuelve una copia por llamada.
 *
 * Entrada esperada: `monto` finito (la ViewModel garantiza `0.0` como mínimo);
 * `NaN`/`Infinity` lanzarían `NumberFormatException` desde `BigDecimal`.
 */
object CurrencyFormat {

    /** Locale monetario obligatorio: español de Colombia. */
    private val COP_LOCALE = Locale("es", "CO")

    /**
     * Instancia por hilo (usa `initialValue()`, disponible desde API 1, porque
     * `minSdk = 26` y `ThreadLocal.withInitial` requeriría API 26).
     */
    private val patternPorHilo = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat = NumberFormat.getCurrencyInstance(COP_LOCALE)
    }

    /**
     * Formateador COP **copia por llamada**: cada acceso devuelve una instancia
     * nueva e independiente, por lo que mutarla (p. ej. `maximumFractionDigits`)
     * no afecta a [format] ni a otros hilos. Úsala para formateos puntuales;
     * para el caso normal basta con [format].
     */
    val COP_PATTERN: NumberFormat
        get() = NumberFormat.getCurrencyInstance(COP_LOCALE)

    /**
     * Formatea [monto] tal cual, con los decimales por defecto de es-CO
     * (sin forzar `minimumFractionDigits` ni nada por el estilo).
     *
     * Ej: `format(71.8)` → `"$71,80"`; un entero se muestra como lo decida el
     * formato por defecto (p. ej. `format(5.0)` → `"$5,00"`), no se inventa otro.
     */
    fun format(monto: Double): String = COP_PATTERN.format(monto)

    /**
     * Redondea [monto] a 2 decimales con HALF_UP **antes** de formatear y luego
     * delega en [format]. Úsala para totales (`cantidad × precioUnitario`), donde
     * el Double deja residuos binarios: `11 × 7.18 = 78.97999999999999` → `"$78,98"`.
     * Además fija HALF_UP explícito (DecimalFormat redondearía HALF_EVEN).
     */
    fun formatConDecimalesRedondeados(monto: Double): String = format(redondear(monto))

    /**
     * Formatea [monto] como pesos ENTEROS, **sin decimales visibles**: redondea a entero
     * con HALF_UP (`BigDecimal.setScale(0)`) y luego formatea con es-CO usando una copia
     * de [COP_PATTERN] fijada a 0 dígitos fraccionarios → `71.5` → `"$72"`, `72.0` → `"$72"`.
     *
     * Uso: totales grandes del dashboard donde los decimales ensucian y agrandan el
     * número. La PRECISIÓN interna no cambia —el cálculo y la persistencia siguen con
     * 2 decimales—, solo lo que se MUESTRA. [format] y [formatConDecimalesRedondeados]
     * no se tocan. Entrada esperada: `monto` finito (misma restricción que [format]).
     */
    fun formatEntero(monto: Double): String {
        val entero = BigDecimal(monto).setScale(0, RoundingMode.HALF_UP).toDouble()
        val formato = COP_PATTERN // copia por llamada: mutarla no afecta a otros hilos
        formato.minimumFractionDigits = 0
        formato.maximumFractionDigits = 0
        return formato.format(entero)
    }

    /** Redondeo decimal (no binario) a 2 cifras: `$21.54` exacto para el mismo Double. */
    private fun redondear(monto: Double): Double =
        BigDecimal(monto).setScale(2, RoundingMode.HALF_UP).toDouble()
}
