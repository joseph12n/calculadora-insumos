package com.bioplast.insumos.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.RedActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/** Separación entre teclas del teclado numérico. */
private val ESPACIO_ENTRE_TECLAS = 12.dp

/**
 * TECLADO NUMÉRICO PROPIO con aspecto de calculadora. Nunca usa
 * BasicTextField ni el teclado del sistema: la cantidad se escribe únicamente
 * tocando estas teclas (cero gestos, cero teclado oculto).
 *
 * Distribución de 3 columnas, **todas las teclas del mismo alto**:
 *   1  2  3
 *   4  5  6
 *   7  8  9
 *   ⌫  0  00
 *
 * Ya no hay huecos ni teclas de dos líneas que quedaran más altas que sus
 * vecinas: `⌫` es un solo glifo rojo suave, `0` y `00` son dígitos.
 *
 * @param onDigit recibe el texto de la tecla tocada ("0".."9" u "00").
 * @param onDelete borra el último dígito escrito.
 * @param keyHeight alto EXACTO de cada tecla (lo calcula la pantalla para
 *   llenar el espacio disponible sin scroll; mínimo 64dp — regla sénior).
 */
@Composable
fun Keypad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 64.dp,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ESPACIO_ENTRE_TECLAS),
    ) {
        DigitRow(digits = listOf('1', '2', '3'), keyHeight = keyHeight, onDigit = onDigit)
        DigitRow(digits = listOf('4', '5', '6'), keyHeight = keyHeight, onDigit = onDigit)
        DigitRow(digits = listOf('7', '8', '9'), keyHeight = keyHeight, onDigit = onDigit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ESPACIO_ENTRE_TECLAS),
        ) {
            DeleteKey(
                onDelete = onDelete,
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
            )
            DigitKey(
                digit = "0",
                onDigit = onDigit,
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
            )
            DigitKey(
                digit = "00",
                onDigit = onDigit,
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
            )
        }
    }
}

@Composable
private fun DigitRow(
    digits: List<Char>,
    keyHeight: Dp,
    onDigit: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ESPACIO_ENTRE_TECLAS),
    ) {
        digits.forEach { digit ->
            DigitKey(
                digit = digit.toString(),
                onDigit = onDigit,
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight),
            )
        }
    }
}

@Composable
private fun DigitKey(
    digit: String,
    onDigit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyCap(
        label = digit,
        contentDescription = "Tecla $digit",
        onClick = { onDigit(digit) },
        modifier = modifier,
        background = SurfaceSoft,
        contentColor = TextPrimary,
        fontSize = 32.sp,
    )
}

@Composable
private fun DeleteKey(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyCap(
        label = "⌫",
        contentDescription = "Borrar el último número",
        onClick = onDelete,
        modifier = modifier,
        background = RedActionContainer,
        contentColor = RedAction,
        fontSize = 32.sp,
    )
}

/**
 * Tecla base: llena el alto [keyHeight] que le pasa el teclado (mínimo 64dp
 * garantizado por quien lo usa), esquinas 20dp, texto bold y feedback táctil
 * (scale 0.98). El contenido se centra en la tecla completa.
 */
@Composable
private fun KeyCap(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = SurfaceSoft,
    contentColor: Color = TextPrimary,
    fontSize: TextUnit = 32.sp,
) {
    val description = contentDescription
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .escalaAlTocar(interactionSource)
            .semantics { this.contentDescription = description },
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                color = contentColor,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
