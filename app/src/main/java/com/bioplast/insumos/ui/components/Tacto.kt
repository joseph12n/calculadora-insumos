package com.bioplast.insumos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Feedback táctil estilo SO nativo (Material You / One UI / iOS):
 * al mantener presionado, el elemento se encoge un 2 % (scale 0.98) y al
 * soltar vuelve a su tamaño con una animación corta (110 ms).
 *
 * NO añade gestos nuevos ni dobles toques: solo pinta la respuesta del toque
 * simple que el botón ya maneja. Aplicar DESPUÉS de `heightIn`/`fillMaxWidth`
 * para que la escala afecte a todo el botón.
 *
 * @param interactionSource la fuente de interacciones que ya usa el botón;
 *   `null` → se devuelve [this] sin cambios.
 */
@Composable
fun Modifier.escalaAlTocar(interactionSource: InteractionSource?): Modifier {
    if (interactionSource == null) return this

    val presionado by interactionSource.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.98f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "tacto",
    )

    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}
