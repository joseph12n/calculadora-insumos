package com.bioplast.insumos.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Un número leído de la foto junto con el RECORTE de la letra que lo originó
 * (cuando se pudo obtener). El recorte se muestra en el diálogo de confirmación
 * para que la persona compare su propia letra con el número reconocido.
 */
data class CandidatoFoto(
    val numero: Int,
    val recorte: Bitmap? = null,
)

/** Tope de candidatos de una foto (parser + UI + ViewModel). */
internal const val MAXIMO_CANDIDATOS_FOTO = MAXIMO_CANDIDATOS_IMAGEN

/** Tope de valor aceptado como cantidad (igual que el teclado propio). */
internal const val MAXIMO_VALOR_FOTO = 999_999

/** Lado máximo de trabajo (memoria/velocidad del análisis). */
private const val LADO_MAXIMO = 2400

/** Si la foto es más chica que esto, se amplía (la letra pequeña se lee mejor). */
private const val LADO_MINIMO = 1600

/** Cuántos números (recortes) se re-analizan como máximo. */
private const val MAXIMO_RECORTES = 10

/**
 * Lectura RECURSIVA de los números de una foto de la hoja, pensada para letra
 * manuscrita (donde ML Kit confunde 1/7/4/8 con facilidad). Hace varias pasadas:
 *
 *  1. **Foto original** (ya enderezada por EXIF) → candidatos.
 *  2. **Foto procesada**: escala de grises + estirado de contraste + **umbral
 *     adaptativo** (Bradley) para separar la tinta del papel → candidatos.
 *  3. **Recorte ampliado de cada número detectado** (×2, con margen y el mismo
 *     procesado) → candidatos refinados; el recorte se guarda para mostrarlo.
 *
 * Después **fusiona** las tres listas con prioridad: recortes refinados →
 * procesada → original; deduplica y filtra lo que no cabe como cantidad
 * (0..[MAXIMO_VALOR_FOTO]). NUNCA lanza: un fallo devuelve lista vacía.
 *
 * Todo corre fuera del hilo principal ([Dispatchers.Default]).
 */
suspend fun leerNumerosDeFoto(context: Context, uri: Uri): List<CandidatoFoto> =
    withContext(Dispatchers.Default) {
        try {
            val original = decodificarYEnderezar(context, uri) ?: return@withContext emptyList()
            leerNumerosDeBitmap(original)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

/**
 * Igual que [leerNumerosDeFoto] pero sobre un [Bitmap] ya listo (útil para los
 * tests instrumentados, que dibujan una hoja de números en memoria).
 */
internal suspend fun leerNumerosDeBitmap(original: Bitmap): List<CandidatoFoto> {
    val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    return try {
        // --- Pasada 1: foto original ------------------------------------------
        val textoOriginal = reconocedor.process(InputImage.fromBitmap(original, 0)).esperarTexto()
        val numerosOriginales = extraerNumerosCandidatos(textoOriginal.text)

        // --- Pasada 2: foto procesada (gris + contraste + umbral) -------------
        val procesada = preprocesar(original)
        val textoProcesado = reconocedor.process(InputImage.fromBitmap(procesada, 0)).esperarTexto()
        val numerosProcesados = extraerNumerosCandidatos(textoProcesado.text)

        // --- Pasada 3: recorte ampliado de cada número ------------------------
        val elementos = (elementosConDigitos(textoOriginal) + elementosConDigitos(textoProcesado))
            .distinctBy { caja -> "${caja.left},${caja.top},${caja.width()}" }
            .take(MAXIMO_RECORTES)
        val refinados = mutableListOf<CandidatoFoto>()
        for (caja in elementos) {
            val recorte = recortarYAmpliar(original, caja) ?: continue
            val textoRecorte = reconocedor
                .process(InputImage.fromBitmap(preprocesar(recorte), 0))
                .esperarTexto()
            val numeros = extraerNumerosCandidatos(textoRecorte.text)
            val recorteParaMostrar = paraMostrar(recorte)
            numeros.forEach { numero -> refinados += CandidatoFoto(numero, recorteParaMostrar) }
        }

        val fusion = fusionarNumeros(
            refinados.map { it.numero },
            numerosProcesados,
            numerosOriginales,
        )
        val recortePorNumero = refinados
            .filter { it.recorte != null }
            .associate { it.numero to it.recorte }
        fusion.map { numero -> CandidatoFoto(numero, recortePorNumero[numero]) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    } finally {
        reconocedor.close()
    }
}

/**
 * Fusión pura y testeable: concatena las listas POR PRIORIDAD (la primera manda),
 * descarta lo que no es una cantidad válida (0..[MAXIMO_VALOR_FOTO]) y
 * deduplica conservando el orden; corta en [maximo].
 */
internal fun fusionarNumeros(
    vararg listas: List<Int>,
    maximo: Int = MAXIMO_CANDIDATOS_FOTO,
): List<Int> =
    listas.asSequence()
        .flatten()
        .filter { it in 0..MAXIMO_VALOR_FOTO }
        .distinct()
        .take(maximo)
        .toList()

// ---------------------------------------------------------------------------
// Imagen: decodificación, enderezado y procesado
// ---------------------------------------------------------------------------

/**
 * Decodifica la foto con EXIF aplicado (la cámara del sistema suele guardar la
 * orientación en la cabecera) y la amplía si es pequeña. Devuelve `null` si no
 * se pudo leer.
 */
private fun decodificarYEnderezar(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver

    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

    // Reducción por potencias de 2 para no pasarse de lado.
    var muestra = 1
    while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= LADO_MAXIMO) {
        muestra *= 2
    }
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = muestra
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val bitmap = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opciones)
    } ?: return null

    val grados = runCatching {
        resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    }.getOrDefault(0)

    val enderezada = if (grados != 0) {
        val matriz = Matrix().apply { postRotate(grados.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    } else {
        bitmap
    }

    val lado = maxOf(enderezada.width, enderezada.height)
    return if (lado in 1 until LADO_MINIMO) {
        val factor = LADO_MINIMO.toFloat() / lado
        Bitmap.createScaledBitmap(
            enderezada,
            (enderezada.width * factor).toInt(),
            (enderezada.height * factor).toInt(),
            true,
        )
    } else {
        enderezada
    }
}

/**
 * Procesa la imagen para que ML Kit distinga mejor la tinta del papel:
 * escala de grises → estirado de contraste (rango completo) → **umbral
 * adaptativo** por ventana (Bradley) que decide blanco/negro según el promedio
 * local, robusto a sombras y papel amarillento.
 *
 * `internal` para poder verificarlo en los tests instrumentados.
 */
internal fun preprocesar(origen: Bitmap): Bitmap {
    val ancho = origen.width
    val alto = origen.height
    val pixeles = IntArray(ancho * alto)
    origen.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)

    // 1) Gris (luma aproximada por enteros, rápida).
    val gris = IntArray(pixeles.size)
    var minimo = 255
    var maximo = 0
    for (i in pixeles.indices) {
        val color = pixeles[i]
        val valor = (((color shr 16) and 0xFF) * 77 +
            ((color shr 8) and 0xFF) * 150 +
            (color and 0xFF) * 29) shr 8
        gris[i] = valor
        if (valor < minimo) minimo = valor
        if (valor > maximo) maximo = valor
    }

    // 2) Contraste: estira el rango real de la foto a 0..255.
    val rango = (maximo - minimo).coerceAtLeast(1)
    for (i in gris.indices) {
        gris[i] = ((gris[i] - minimo) * 255 / rango).coerceIn(0, 255)
    }

    // 3) Umbral adaptativo (Bradley) con imagen integral.
    val anchoIntegral = ancho + 1
    val integral = LongArray(anchoIntegral * (alto + 1))
    for (y in 0 until alto) {
        var sumaFila = 0L
        val base = y * ancho
        for (x in 0 until ancho) {
            sumaFila += gris[base + x]
            integral[(y + 1) * anchoIntegral + (x + 1)] =
                integral[y * anchoIntegral + (x + 1)] + sumaFila
        }
    }
    val ventana = (minOf(ancho, alto) / 12).coerceAtLeast(8)
    val factor = 0.85
    val salida = IntArray(pixeles.size)
    for (y in 0 until alto) {
        val y1 = (y - ventana).coerceAtLeast(0)
        val y2 = (y + ventana).coerceAtMost(alto - 1)
        val alto2 = (y2 - y1 + 1).toLong()
        for (x in 0 until ancho) {
            val x1 = (x - ventana).coerceAtLeast(0)
            val x2 = (x + ventana).coerceAtMost(ancho - 1)
            val cuenta = (x2 - x1 + 1).toLong() * alto2
            val suma = integral[(y2 + 1) * anchoIntegral + (x2 + 1)] -
                integral[y1 * anchoIntegral + (x2 + 1)] -
                integral[(y2 + 1) * anchoIntegral + x1] +
                integral[y1 * anchoIntegral + x1]
            val tinta = gris[y * ancho + x] * cuenta < (suma * factor).toLong()
            salida[y * ancho + x] = if (tinta) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }

    return Bitmap.createBitmap(salida, ancho, alto, Bitmap.Config.ARGB_8888)
}

/**
 * Cajas de los elementos de texto que CONTIENEN dígitos (los números de la
 * hoja), de mayor a menor área: los grandes suelen ser los conteos y los
 * primeros en interesar.
 */
private fun elementosConDigitos(resultado: Text): List<Rect> =
    resultado.textBlocks
        .flatMap { it.lines }
        .flatMap { it.elements }
        .mapNotNull { elemento ->
            val caja = elemento.boundingBox
            if (caja != null &&
                caja.width() > 2 &&
                caja.height() > 2 &&
                elemento.text.any { it.isDigit() }
            ) {
                caja
            } else {
                null
            }
        }
        .sortedByDescending { it.width() * it.height() }

/**
 * Recorta la caja con un margen y amplía ×2 (tope 1000px de lado): la letra
 * manuscrita pequeña mejora mucho al re-leerse ampliada.
 */
private fun recortarYAmpliar(origen: Bitmap, caja: Rect): Bitmap? {
    val margenX = (caja.width() * 0.20f).toInt().coerceAtLeast(8)
    val margenY = (caja.height() * 0.30f).toInt().coerceAtLeast(8)
    val recorte = Rect(
        (caja.left - margenX).coerceAtLeast(0),
        (caja.top - margenY).coerceAtLeast(0),
        (caja.right + margenX).coerceAtMost(origen.width),
        (caja.bottom + margenY).coerceAtMost(origen.height),
    )
    if (recorte.width() <= 2 || recorte.height() <= 2) return null

    val trozo = Bitmap.createBitmap(
        origen,
        recorte.left,
        recorte.top,
        recorte.width(),
        recorte.height(),
    )
    val lado = maxOf(trozo.width, trozo.height)
    val factor = (2000f / lado).coerceIn(1f, 3f)
    return Bitmap.createScaledBitmap(
        trozo,
        (trozo.width * factor).toInt().coerceAtLeast(1),
        (trozo.height * factor).toInt().coerceAtLeast(1),
        true,
    )
}

/** Reduce el recorte para mostrarlo en el diálogo sin gastar memoria. */
private fun paraMostrar(recorte: Bitmap): Bitmap =
    if (recorte.width <= 480) {
        recorte
    } else {
        val alto = (recorte.height * (480f / recorte.width)).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(recorte, 480, alto, true)
    }

/** Convierte la `Task` de ML Kit en suspensión sin dependencias extra. */
private suspend fun Task<Text>.esperarTexto(): Text =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { resultado -> continuation.resume(resultado) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
