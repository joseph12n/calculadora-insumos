package com.bioplast.insumos.ui.texto

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.math.roundToInt

/**
 * Preferencias del tamaño de letra ajustable por la persona usuaria.
 *
 * Archivo `SharedPreferences` "ajustes_texto", clave "escala_fuente":
 * multiplica el `fontScale` del sistema (1.0 = normal). Rango 0.85..1.6 con
 * paso 0.15; el valor se persiste al instante y sobrevive a cerrar la app.
 *
 * La aplicación real de la escala la hace [ConEscalaDeTexto] (LocalDensity);
 * aquí SOLO se guarda y se lee.
 */
object AjustesTexto {

    /** Archivo de preferencias. */
    private const val ARCHIVO = "ajustes_texto"

    /** Clave del valor de escala. */
    private const val CLAVE = "escala_fuente"

    /** Paso de cada toque en A− / A+. */
    const val PASO = 0.15f

    /** Tamaño mínimo (letra un poco más chica que la normal). */
    const val MINIMA = 0.85f

    /** Tamaño máximo (letra claramente grande para personas mayores). */
    const val MAXIMA = 1.6f

    /** Escala normal (100 %). */
    const val NORMAL = 1.0f

    /** Escala guardada, acotada a [MINIMA]..[MAXIMA] (por defecto [NORMAL]). */
    fun escala(contexto: Context): Float =
        limitar(
            contexto
                .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
                .getFloat(CLAVE, NORMAL)
        )

    /** Persiste la escala (siempre acotada a [MINIMA]..[MAXIMA]). */
    fun guardar(contexto: Context, escala: Float) {
        contexto
            .getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
            .edit()
            .putFloat(CLAVE, limitar(escala))
            .apply()
    }

    /** Acota al rango y redondea a 2 decimales (evita arrastre de flotantes). */
    private fun limitar(valor: Float): Float =
        ((valor * 100f).roundToInt() / 100f).coerceIn(MINIMA, MAXIMA)
}

/**
 * Control del tamaño de letra que recibe toda la app vía [LocalControlDeTexto].
 *
 * @property escala escala activa (0.85..1.6).
 * @property puedeAumentar `false` al llegar a [AjustesTexto.MAXIMA] → A+ deshabilitado.
 * @property puedeDisminuir `false` al llegar a [AjustesTexto.MINIMA] → A− deshabilitado.
 * @property aumentar un toque en **A+**: +[AjustesTexto.PASO], persistido.
 * @property disminuir un toque en **A−**: −[AjustesTexto.PASO], persistido.
 */
data class ControlDeTexto(
    val escala: Float,
    val puedeAumentar: Boolean,
    val puedeDisminuir: Boolean,
    val aumentar: () -> Unit,
    val disminuir: () -> Unit,
)

/** Control accesible desde cualquier composable dentro de [ConEscalaDeTexto]. */
val LocalControlDeTexto = androidx.compose.runtime.compositionLocalOf {
    ControlDeTexto(
        escala = AjustesTexto.NORMAL,
        puedeAumentar = AjustesTexto.NORMAL < AjustesTexto.MAXIMA,
        puedeDisminuir = AjustesTexto.NORMAL > AjustesTexto.MINIMA,
        aumentar = {},
        disminuir = {},
    )
}

/**
 * Envuelve TODO el contenido de la app y aplica el tamaño de letra elegido:
 * lee la escala persistida y provee
 * `LocalDensity provides Density(density.density, density.fontScale * escala)`.
 *
 * Solo cambia el `fontScale` (los textos crecen); la densidad dp queda intacta,
 * así los botones conservan sus ≥64dp y el diseño no se rompe.
 * Además expone [LocalControlDeTexto] para los botones A− / A+ (la escala se
 * aplica en vivo y se guarda en [AjustesTexto] en el mismo toque).
 */
@Composable
fun ConEscalaDeTexto(content: @Composable () -> Unit) {
    val contexto = LocalContext.current.applicationContext
    var escala by rememberSaveable { mutableStateOf(AjustesTexto.escala(contexto)) }

    val density = LocalDensity.current

    val control = remember(escala) {
        ControlDeTexto(
            escala = escala,
            puedeAumentar = escala < AjustesTexto.MAXIMA,
            puedeDisminuir = escala > AjustesTexto.MINIMA,
            aumentar = {
                val siguiente = (escala + AjustesTexto.PASO).coerceAtMost(AjustesTexto.MAXIMA)
                escala = siguiente
                AjustesTexto.guardar(contexto, siguiente)
            },
            disminuir = {
                val siguiente = (escala - AjustesTexto.PASO).coerceAtLeast(AjustesTexto.MINIMA)
                escala = siguiente
                AjustesTexto.guardar(contexto, siguiente)
            },
        )
    }

    CompositionLocalProvider(
        LocalControlDeTexto provides control,
        LocalDensity provides Density(density.density, density.fontScale * escala),
        content = content,
    )
}
