package com.bioplast.insumos.camera

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Analizador de CameraX que extrae un **número entero** del frame (OCR 100 % on-device).
 *
 * Rol en el flujo: la pantalla del Paso 2 (`ENTER_QUANTITY`) crea una instancia, la
 * registra en `ImageAnalysis` (con `STRATEGY_KEEP_ONLY_LATEST`) y recibe cada lectura
 * en [onNumberDetected]:
 *  - `Int` → número detectado; la UI lo entrega a `InventoryViewModel.onEscanearResultado(n)`.
 *  - `null` → en este frame no se leyó nada utilizable; la UI muestra
 *    *"No pude leer el número. Escríbelo tú mismo."* (el mensaje debe mostrarse al
 *    cerrar/abortar el escaneo o por timeout, no en cada frame nulo: el preview
 *    emite `null` mientras no haya número a la vista).
 *
 * El OCR es **opcional y nunca destructivo**: si ML Kit falla o no encuentra dígitos
 * solo se emite `null`; `quantityInput` vive en la ViewModel y no se toca hasta que
 * llegue un `Int`.
 *
 * Anti-spam: no se emite un resultado igual al anterior consecutivo, ni si faltan
 * menos de [cooldownMs] (~2 s) desde la última emisión, para no saturar la UI con
 * frames idénticos (~30 fps). Si el mismo número se sigue viendo después del
 * cooldown tampoco se re-emite (ya fue entregado).
 *
 * Backpressure: si llega un frame mientras otro está en vuelo, este se cierra
 * directamente (además de `STRATEGY_KEEP_ONLY_LATEST` en la UI).
 *
 * Hilos: [onNumberDetected] se invoca **siempre en el hilo principal** (listeners de
 * ML Kit en main + reenvío por `Handler(main)`), seguro para tocar `StateFlow`/Compose.
 *
 * Ciclo de vida: una instancia por sesión de cámara; llamar a [close] al destruir la
 * pantalla para liberar el [TextRecognizer].
 */
class NumberRecognitionAnalyzer(
    private val onNumberDetected: (Int?) -> Unit,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
) : ImageAnalysis.Analyzer, Closeable {

    /** Creado en init; se libera en [close]. */
    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** true mientras ML Kit procesa un frame: el siguiente se descarta (backpressure). */
    private val procesando = AtomicBoolean(false)

    /** Serializa todas las emisiones en main → el anti-spam no necesita locks. */
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var cerrado: Boolean = false

    /** Último resultado entregado; solo se toca en el hilo principal. */
    private var ultimoEmitido: Int? = null

    /** `elapsedRealtime` de la última emisión; solo se toca en el hilo principal. */
    private var ultimaEmisionMs: Long = 0L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (cerrado || !procesando.compareAndSet(false, true)) {
            // Analizador cerrado, o ya hay un frame en vuelo: libera el frame nuevo ya.
            imageProxy.close()
            return
        }
        var enVuelo = false
        try {
            val mediaImage = imageProxy.image
                ?: error("ImageProxy sin imagen adjunta")
            val rotacion = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotacion)
            val tarea = textRecognizer.process(inputImage)
            tarea.addOnSuccessListener { resultado ->
                emitir(extraerMejorNumero(resultado.text))
            }
            tarea.addOnFailureListener { e ->
                Log.w(TAG, "ML Kit no pudo procesar el frame", e)
                emitir(null)
            }
            tarea.addOnCompleteListener {
                // Camino asíncrono: el ImageProxy se cierra recién aquí, porque
                // MediaImage debe seguir vivo hasta que la tarea termina.
                procesando.set(false)
                imageProxy.close()
            }
            enVuelo = true
        } catch (e: Exception) {
            Log.w(TAG, "Frame OCR descartado", e)
            emitir(null)
        } finally {
            if (!enVuelo) {
                // La tarea nunca arrancó (o falló al crearla): cierra aquí para que
                // ningún camino filtre el ImageProxy. Los dos caminos se excluyen
                // mutuamente vía `enVuelo`, así que nunca se cierra dos veces.
                procesando.set(false)
                imageProxy.close()
            }
        }
    }

    /** Libera el [TextRecognizer]; idempotente. Los frames posteriores se descartan. */
    override fun close() {
        cerrado = true
        textRecognizer.close()
    }

    /**
     * Anti-spam + serialización en main: descarta resultados repetidos consecutivos y
     * aplica el cooldown de [cooldownMs] entre emisiones cualesquiera.
     */
    private fun emitir(numero: Int?) {
        if (cerrado) return
        mainHandler.post {
            if (cerrado) return@post
            val ahora = SystemClock.elapsedRealtime()
            val repetidoConsecutivo = numero == ultimoEmitido
            val enCooldown = ahora - ultimaEmisionMs < cooldownMs
            if (repetidoConsecutivo || enCooldown) return@post
            ultimoEmitido = numero
            ultimaEmisionMs = ahora
            onNumberDetected(numero)
        }
    }

    companion object {
        private const val TAG = "NumberOcrAnalyzer"

        /** Ventana anti-spam: ~2 s entre emisiones a la UI. */
        const val DEFAULT_COOLDOWN_MS: Long = 2_000L
    }
}

/** Mínimo de dígitos aceptados en un candidato (1 dígito es válido: ej. "3"). */
private const val MIN_DIGITOS = 1

/** Máximo razonable de dígitos: 7 (9.999.999 entra en `Int` y descarta seriales largos). */
private const val MAX_DIGITOS = 7

private val SEPARADORES = Regex("""\s+""")
private val PRIMERA_CORRIDA_DE_DIGITOS = Regex("""\d+""")

/** Fechas tipo `2026-09-23` o `23/09/2026` vistas desde el inicio del token. */
private val FECHA_EN_EL_INICIO = Regex("""\d{4}[-/]\d{1,2}|\d{1,2}/\d{1,2}/\d{2,4}""")

/**
 * Extrae el mejor número entero del texto reconocido por ML Kit. `null` = nada válido.
 *
 * Estrategia en dos pasadas sobre los tokens (separados por espacios/saltos de línea):
 *  1. **Pasada principal:** primer token 100 % dígitos con longitud razonable (1..7),
 *     en orden de lectura (p. ej. `"12"` en `"CANT: 12"`).
 *  2. **Pasada de reserva:** si no hay token puro, la primera secuencia contigua de
 *     dígitos (1..7) dentro de tokens mixtos (p. ej. `"x:12"` → `12`).
 *
 * Descarta basura: tokens con más de 7 dígitos (seriales), desbordes de `Int`,
 * y tokens que empiezan como fecha (`2026-09-…`, `23/09/2026`) en la pasada de
 * reserva. `internal` para que los tests unitarios puedan ejercitarla.
 */
internal fun extraerMejorNumero(textoReconocido: String): Int? {
    if (textoReconocido.isBlank()) return null
    val tokens = textoReconocido.split(SEPARADORES).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null

    // Pasada 1: token 100 % dígitos, en orden de lectura.
    val tokenPuro = tokens.firstOrNull { token ->
        token.length in MIN_DIGITOS..MAX_DIGITOS && token.all { it.isDigit() }
    }
    if (tokenPuro != null) return tokenPuro.toIntOrNull()

    // Pasada 2: primera corrida de dígitos dentro de tokens mixtos.
    for (token in tokens) {
        if (!token.any { it.isDigit() }) continue
        val empiezaEnElIndiceCero = FECHA_EN_EL_INICIO.find(token)?.range?.first == 0
        if (empiezaEnElIndiceCero) continue
        val corrida = PRIMERA_CORRIDA_DE_DIGITOS.find(token)?.value ?: continue
        if (corrida.length !in MIN_DIGITOS..MAX_DIGITOS) continue
        return corrida.toIntOrNull()
    }
    return null
}
