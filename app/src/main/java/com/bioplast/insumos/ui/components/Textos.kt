package com.bioplast.insumos.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Textos compartidos de las pantallas (extraídos para no duplicar código en
 * ConfirmScreen / DayDetailScreen / StartScreen).
 */

/**
 * Fecha corta para filas, p. ej. "Lun 23" (es-CO, con inicial mayúscula).
 * Se usa en el detalle del historial y en la lista de pendientes de la revisión.
 */
fun fechaCorta(fecha: LocalDate): String {
    val texto = DateTimeFormatter.ofPattern("EEE d", Locale("es", "CO")).format(fecha)
    return texto.replaceFirstChar { inicial -> inicial.titlecase(Locale("es", "CO")) }
}

/**
 * Plural sencillo para no escribir "1 productos", p. ej. `pluralDe(1)` →
 * "producto" y `pluralDe(27)` → "productos". El plural agrega "s" a [singular].
 */
fun pluralDe(cantidad: Int, singular: String = "producto"): String =
    if (cantidad == 1) singular else "${singular}s"
