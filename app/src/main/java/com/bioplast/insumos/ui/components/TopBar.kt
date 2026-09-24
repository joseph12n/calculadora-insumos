package com.bioplast.insumos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Cabecera de pantalla simple y alineada: **← Atrás** a la izquierda.
 *
 * Sustituye a la antigua cabecera con la píldora "Paso X de 3": con la
 * calculadora en UNA sola pantalla ya no hay pasos que numerar y la cabecera
 * queda sin elementos de distinta altura que se vieran "desencajados".
 *
 * El botón hereda [SeniorButton] (≥64dp, gris = volver, un solo toque).
 *
 * @param onBack acción de volver (a la pantalla anterior o al inicio).
 * @param backEnabled `false` = deshabilitado (p. ej. mientras se guarda).
 * @param contentDescription etiqueta de TalkBack del botón.
 */
@Composable
fun TopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    contentDescription: String = "Atrás, volver al paso anterior",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        SeniorButton(
            text = "← Atrás",
            onClick = onBack,
            variant = SeniorButtonVariant.GREY,
            enabled = backEnabled,
            contentDescription = contentDescription,
        )
    }
}
