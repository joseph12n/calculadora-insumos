package com.bioplast.insumos.camera

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
 * Extrae los números-candidato del texto reconocido por ML Kit (OCR de FOTO de galería).
 *
 * Heurística en dos pasadas sobre los tokens (separados por espacios/saltos de línea),
 * MAYOR CONFIANZA PRIMERO (misma prioridad que [extraerMejorNumero], pero plural):
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
 *    Nota: el OCR en vivo ([extraerMejorNumero]) sí devuelve el entero izquierdo de
 *    `7.18` porque es un único intento de última hora; aquí el usuario ELEGIRÁ entre
 *    candidatos, así que preferimos menos opciones antes que una mala.
 *  - **Corridas > 7 cifras descartadas** completas (seriales), nunca truncadas.
 *  - **Deduplica preservando orden** (primer ganador) y **limita a
 *    [MAXIMO_CANDIDATOS_IMAGEN]** candidatos.
 *
 * `internal` para que los tests unitarios la ejerciten (mismo paquete).
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

/**
 * Lee los números de una FOTO elegida de la galería con ML Kit **on-device** y devuelve
 * los candidatos de [extraerNumerosCandidatos] (lista vacía = nada legible).
 *
 * - **Hilos:** corre en [Dispatchers.IO] (leer la URI y decodificar la imagen es I/O;
 *   `InputImage.fromFilePath` también aplica la rotación EXIF de la foto).
 * - **NUNCA lanza**: cualquier error (URI inválida, permiso denegado, fallo de ML Kit)
 *   devuelve `emptyList()`; la UI lo traduce a `MENSAJE_IMAGEN_FALLIDO` vía
 *   `InventoryViewModel.onImagenLeida`. Solo la cancelación del scope se re-lanza.
 * - **Recursos:** se crea UN TextRecognizer NUEVO por llamada y se cierra en el
 *   `finally`. Justificación: la lectura de galería es *one-shot* (una foto por uso);
 *   crear/cerrar libera el modelo nativo de memoria al terminar, evita mantener un
 *   modelo cargado mientras nadie lee imágenes y hace la función segura ante llamadas
 *   concurrentes (cada una con su recognizer, sin estado compartido). El caso opuesto —
 *   recognizer por sesión — es el del video en vivo ([NumberRecognitionAnalyzer]),
 *   donde `close()` se llama al destruir la pantalla. `InputImage` no es `Closeable`
 *   (usa bitmap/buffers propios), así que no requiere cierre.
 */
suspend fun extraerNumerosDeImagen(context: Context, uri: Uri): List<Int> =
    withContext(Dispatchers.IO) {
        try {
            val imagen = InputImage.fromFilePath(context, uri) // lanza IOException si la URI falla
            val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                val resultado = reconocedor.process(imagen).esperarResultado()
                extraerNumerosCandidatos(resultado.text)
            } finally {
                reconocedor.close()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

/** Convierte la `Task` de ML Kit en suspensión (sin dependencias extra de play-services). */
private suspend fun <T> Task<T>.esperarResultado(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { resultado -> continuation.resume(resultado) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
