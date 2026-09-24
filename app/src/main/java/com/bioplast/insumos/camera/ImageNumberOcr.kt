package com.bioplast.insumos.camera

/**
 * Tope de candidatos que se devuelven/muestran para elegir (parser + UI + ViewModel).
 * 12 números alcanzan para una nota corta sin abrumar a la persona en la pantalla
 * de selección de candidatos.
 */
internal const val MAXIMO_CANDIDATOS_IMAGEN = 12

/** Mínimo de dígitos por candidato (1 cifra es válida: ej. "3"). */
private const val MIN_DIGITOS = 1

/** Máximo razonable de dígitos: 7 (9.999.999; descarta seriales largos). */
private const val MAX_DIGITOS = 7

private val SEPARACION = Regex("""\s+""")
private val CORRIDA_DE_DIGITOS = Regex("""\d+""")

/**
 * Fecha (contiene '-' o '/' con patrón de fecha): año-mes `2026-09`, fecha completa
 * `2026-09-23` o `23/09/2026` — nunca es una cantidad de insumos.
 */
private val PATRON_FECHA =
    Regex("""\d{4}[-/]\d{1,2}([-/]\d{1,2})?|\d{1,2}[-/]\d{1,2}[-/]\d{2,4}""")

/**
 * Precio: inmediatamente DESPUÉS de la corrida vienen `[,.]` + exactamente 2 dígitos
 * (no seguidos de otro dígito): `7,18` o `7.18` son PRECIO, no cantidad.
 */
private val PATRON_PRECIO = Regex("""^[.,]\d{2}(?!\d)""")

/**
 * Extrae los números-candidato del texto reconocido por ML Kit (OCR de la FOTO).
 *
 * Heurística en dos pasadas sobre los tokens (separados por espacios/saltos de línea),
 * MAYOR CONFIANZA PRIMERO:
 *  1. **Pasada 1:** tokens 100 % dígitos de 1..7 cifras, en orden de lectura
 *     (`"10"` → `[10]`; `"10 20 30"` → `[10, 20, 30]`).
 *  2. **Pasada 2:** por cada token MIXTO, su primera corrida contigua de 1..7 dígitos
 *     (`"x:12"` → `[12]`), siempre que no sea fecha ni precio (ver abajo).
 *     Los mixtos van DESPUÉS de los puros (`"x:5 42"` → `[42, 5]`).
 *
 * Anti-error (por qué NO rellena solo un número equivocado):
 *  - **Fechas excluidas**: token que contiene patrón de fecha (`23/09/2026`,
 *    `2026-09-23`, `2026-09`) → se salta el token entero.
 *  - **Precios excluidos**: dígitos seguidos de `,`/`.` + exactamente 2 dígitos
 *    (`7,18`, `7.18`) NO son cantidad; si la primera corrida del token es un precio,
 *    el token no aporta candidato (la regla es "primera corrida por token").
 *    Como la persona ELEGIRÁ entre candidatos (y verá el recorte de su letra),
 *    preferimos menos opciones antes que una mala.
 *  - **Corridas > 7 cifras descartadas** completas (seriales), nunca truncadas.
 *  - **Deduplica preservando orden** (primer ganador) y **limita a
 *    [MAXIMO_CANDIDATOS_IMAGEN]** candidatos.
 *
 * `internal` para que los tests unitarios la ejerciten (mismo paquete). El flujo
 * completo (varias pasadas + fusión) vive en [leerNumerosDeFoto].
 */
internal fun extraerNumerosCandidatos(texto: String): List<Int> {
    val puros = mutableListOf<Int>()
    val mixtos = mutableListOf<Int>()

    for (token in texto.split(SEPARACION)) {
        if (token.isEmpty()) continue

        if (token.all { it.isDigit() }) {
            // Pasada 1: token 100 % dígitos. Los de ≥ 8 cifras se descartan completos.
            if (token.length in MIN_DIGITOS..MAX_DIGITOS) {
                token.toIntOrNull()?.let { puros += it }
            }
            continue
        }

        // Pasada 2: tokens mixtos → primero descartar fechas y precios.
        if (PATRON_FECHA.containsMatchIn(token)) continue
        val coincidencia = CORRIDA_DE_DIGITOS.find(token) ?: continue
        val corrida = coincidencia.value
        if (corrida.length !in MIN_DIGITOS..MAX_DIGITOS) continue
        val despuesDeLaCorrida = token.substring(coincidencia.range.last + 1)
        if (PATRON_PRECIO.containsMatchIn(despuesDeLaCorrida)) continue // ej. "7,18"
        corrida.toIntOrNull()?.let { mixtos += it }
    }

    return (puros + mixtos).distinct().take(MAXIMO_CANDIDATOS_IMAGEN)
}
