package com.bioplast.insumos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioplast.insumos.ui.theme.GreyActionContainer
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Cabecera de los pasos del flujo (jerarquía clara estilo SO nativo):
 *  - Botón "← Atrás" grande (mínimo 64dp) siempre visible, arriba a la izquierda.
 *  - Indicador textual "Paso X de 3" dentro de una píldora suave a la derecha
 *    (sin pestañas ni puntos de página).
 *
 * @param step paso actual (1..3).
 * @param onBack acción de volver al paso anterior / al inicio.
 * @param totalSteps total de pasos del flujo (por defecto 3).
 * @param backEnabled si es false el botón "Atrás" queda deshabilitado (opacidad 0.4).
 */
@Composable
fun StepHeader(
    step: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    totalSteps: Int = 3,
    backEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SeniorButton(
            text = "← Atrás",
            onClick = onBack,
            variant = SeniorButtonVariant.GREY,
            enabled = backEnabled,
            contentDescription = "Atrás, volver al paso anterior"
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GreyActionContainer
            ) {
                Text(
                    text = "Paso $step de $totalSteps",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
