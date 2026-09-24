package com.bioplast.insumos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Sección de selección de números leídos de una FOTO de la cuenta (FASE 4).
 *
 * Cuando la imagen contiene VARIOS números, la ViewModel los deja en
 * `ocrCandidatos` y esta sección pregunta **"¿Cuál número ves en tu cuenta?"**
 * con botones GIANTES por candidato (texto 36sp bold, ancho completo, alto
 * mínimo 72dp, fondo blanco con borde verde suave y feedback táctil 0.98) y el
 * botón gris **✍️ Escribir a mano** para descartar la lectura y volver al
 * teclado. Un solo toque, sin gestos; esquinas 20dp y panel suave (estética
 * FASE 3/4).
 *
 * Lista vacía → no se pinta nada (defensivo; el llamador también revisa).
 *
 * @param ocrCandidatos números detectados en la imagen (hasta 12).
 * @param onElegido toque sobre un candidato → ese número pasa a ser la cantidad.
 * @param onCerrar toque en **✍️ Escribir a mano** → se cierran los candidatos
 *   (la persona vuelve a escribir con el teclado propio).
 * @param modifier modifier del panel.
 */
@Composable
fun CandidatosNumeros(
    ocrCandidatos: List<Int>,
    onElegido: (Int) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (ocrCandidatos.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceSoft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "¿Cuál número ves en tu cuenta?",
                style = MaterialTheme.typography.titleLarge, // 26sp bold
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )

            ocrCandidatos.forEach { numero ->
                BotonCandidato(
                    numero = numero,
                    onClick = { onElegido(numero) },
                )
            }

            SeniorButton(
                text = "✍️ Escribir a mano",
                onClick = onCerrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                variant = SeniorButtonVariant.GREY,
                contentDescription = "Escribir el número a mano con el teclado",
            )
        }
    }
}

/**
 * Botón GIGANTE de candidato: fondo blanco, borde verde suave 2dp, número en
 * 36sp bold, ancho completo, alto mínimo 72dp y escala 0.98 al tocar.
 * TalkBack lee "Elegir el número N" (contiene el texto visible "N").
 */
@Composable
private fun BotonCandidato(
    numero: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .escalaAlTocar(interactionSource)
            .semantics { this.contentDescription = "Elegir el número $numero" },
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        contentColor = TextPrimary,
        border = BorderStroke(width = 2.dp, color = GreenAction.copy(alpha = 0.5f)),
        interactionSource = interactionSource,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
        ) {
            Text(
                text = "$numero",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
