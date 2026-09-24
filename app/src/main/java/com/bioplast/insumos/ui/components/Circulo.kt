package com.bioplast.insumos.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ÁREA CIRCULAR de icono que respeta las reglas sénior H-1: un CUADRADO de
 * mínimo [minimo] (64dp por defecto) que CRECE con el contenido y se queda
 * centrado. Aplicarlo sobre el CONTENIDO de una `Surface(shape = CircleShape)`
 * (no sobre la Surface) para que el fondo de círculo cubra todo el área.
 *
 * Mecanismo: [defaultMinSize] pone el mínimo y el `layout` posterior mide el
 * contenido en su tamaño natural (sin mínimos) y toma el lado MAYOR como lado
 * del cuadrado, centrado y nunca mayor que el espacio disponible. Así el glifo
 * a escala de fuente máxima (1.6 app × 200 % sistema ≈ 256 sp) no desborda
 * nunca el círculo, que además sigue siendo cuadrado/circular.
 *
 * NOTA: aquí NO se usa `aspectRatio(1f)` (la alternativa obvia) porque dentro
 * de una `Row` el hijo sin peso recibe `maxWidth` FINITO (resto de la fila) y
 * `AspectRatioNode.findSize` (foundation-layout 1.7.6) intenta `tryMaxWidth`
 * ANTES que el contenido → el elemento se convertiría en un cuadrado del ancho
 * completo de la fila.
 *
 * @param minimo lado mínimo del cuadrado (64dp = regla sénior de área táctil).
 */
fun Modifier.circuloMinimo(minimo: Dp = 64.dp): Modifier =
    this
        .defaultMinSize(minWidth = minimo, minHeight = minimo)
        .layout { measurable, constraints ->
            val contenido = measurable.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                )
            )
            val minimoPx = minimo.roundToPx()
            var lado = maxOf(
                contenido.width,
                contenido.height,
                minimoPx,
            )
            if (constraints.hasBoundedWidth) lado = lado.coerceAtMost(constraints.maxWidth)
            if (constraints.hasBoundedHeight) lado = lado.coerceAtMost(constraints.maxHeight)
            layout(lado, lado) {
                contenido.place(
                    x = (lado - contenido.width) / 2,
                    y = (lado - contenido.height) / 2,
                )
            }
        }
