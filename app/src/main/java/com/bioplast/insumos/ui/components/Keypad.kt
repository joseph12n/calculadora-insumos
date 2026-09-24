package com.bioplast.insumos.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.RedActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * TECLADO NUMÉRICO PROPIO. Nunca usa BasicTextField ni el teclado del sistema:
 * la cantidad se escribe únicamente tocando estas teclas (cero gestos, cero teclado oculto).
 *
 * Distribución de 3 columnas:
 *   1 2 3
 *   4 5 6
 *   7 8 9
 *   Borrar 0 00
 *
 * Teclas de ≥64dp, esquinas 20dp, gris claro con texto negro y feedback
 * táctil (scale 0.98 al presionar); Borrar en rojo suave.
 *
 * @param onDigit recibe el texto de la tecla tocada ("0".."9" u "00").
 * @param onDelete borra el último dígito escrito.
 * @param showDoubleZero muestra u oculta la tecla "00".
 */
@Composable
fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDoubleZero: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DigitRow(digits = listOf("1", "2", "3"), onDigit = onDigit)
        DigitRow(digits = listOf("4", "5", "6"), onDigit = onDigit)
        DigitRow(digits = listOf("7", "8", "9"), onDigit = onDigit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeleteKey(
                onDelete = onDelete,
                modifier = Modifier.weight(1f)
            )
            DigitKey(
                digit = "0",
                onDigit = onDigit,
                modifier = Modifier.weight(1f)
            )
            if (showDoubleZero) {
                DigitKey(
                    digit = "00",
                    onDigit = onDigit,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DigitRow(
    digits: List<String>,
    onDigit: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        digits.forEach { digit ->
            DigitKey(
                digit = digit,
                onDigit = onDigit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: String,
    onDigit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    KeyCap(
        label = digit,
        contentDescription = "Tecla $digit",
        onClick = { onDigit(digit) },
        modifier = modifier,
        background = SurfaceSoft,
        contentColor = TextPrimary,
        fontSize = 32.sp
    )
}

@Composable
private fun DeleteKey(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    KeyCap(
        label = "⌫\nBorrar",
        contentDescription = "Borrar el último número",
        onClick = onDelete,
        modifier = modifier,
        background = RedActionContainer,
        contentColor = RedAction,
        fontSize = 22.sp
    )
}

/**
 * Tecla base: mínimo 64dp de alto (crece con el escalado de fuente del sistema),
 * esquinas 20dp, texto bold y feedback táctil (scale 0.98).
 */
@Composable
private fun KeyCap(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = SurfaceSoft,
    contentColor: Color = TextPrimary,
    fontSize: TextUnit = 32.sp
) {
    val description = contentDescription
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 64.dp)
            .escalaAlTocar(interactionSource)
            .semantics { this.contentDescription = description },
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                color = contentColor,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }
    }
}
