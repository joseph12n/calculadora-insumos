package com.bioplast.insumos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.OnAction
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Variantes de botón sénior. Significado fijo en toda la app:
 *  - GREEN  → avanzar / guardar
 *  - GREY   → volver / corregir
 *  - RED    → borrar (nunca para otra acción)
 *  - OUTLINE → acción opcional neutra (ej. ver registros, cambiar fecha)
 */
enum class SeniorButtonVariant { GREEN, GREY, RED, OUTLINE }

/**
 * Botón reutilizable: mínimo 64dp de alto, esquinas 20dp, texto 24sp bold,
 * elevación suave y feedback táctil (scale 0.98 al presionar).
 * Estado [enabled] = false se muestra con opacidad 0.4.
 *
 * @param contentDescription etiqueta para TalkBack; si es null se lee [text].
 */
@Composable
fun SeniorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: SeniorButtonVariant = SeniorButtonVariant.GREEN,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val (containerColor: Color, contentColor: Color) = when (variant) {
        SeniorButtonVariant.GREEN -> GreenAction to OnAction
        SeniorButtonVariant.GREY -> GreyAction to OnAction
        SeniorButtonVariant.RED -> RedAction to OnAction
        SeniorButtonVariant.OUTLINE -> Color.Transparent to TextPrimary
    }

    val description = contentDescription
    val semanticsModifier = if (description != null) {
        Modifier.semantics { this.contentDescription = description }
    } else {
        Modifier
    }

    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 64.dp)
            .escalaAlTocar(interactionSource)
            .then(semanticsModifier),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        border = if (variant == SeniorButtonVariant.OUTLINE) {
            BorderStroke(width = 2.dp, color = GreyAction)
        } else {
            null
        },
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
