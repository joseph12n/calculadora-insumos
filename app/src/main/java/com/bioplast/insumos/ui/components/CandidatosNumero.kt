package com.bioplast.insumos.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bioplast.insumos.ui.theme.AppBackground
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Diálogo de CONFIRMACIÓN de los números leídos de una FOTO de la hoja.
 *
 * La letra manuscrita se confunde con facilidad (1/7/4/8), así que SIEMPRE se
 * confirma, incluso cuando la foto dio un solo número:
 *
 *  - **Un número** → "¿Es este el número de tu hoja?" con el **recorte de la
 *    letra** bien grande, botón verde **✔ Usar N** y gris **✍️ Escribir a mano**.
 *  - **Varios** → "¿Cuál número ves en tu hoja?" con un botón por número que
 *    muestra, cuando se pudo, el recorte de la letra al lado de la cifra.
 *
 * Ver el recorte permite comparar la propia letra con lo que entendió la app:
 * si no coincide, se escribe a mano. Es un DIÁLOGO (nunca queda debajo del
 * pliegue) y todo su contenido se desplaza si la letra es gigante.
 *
 * Lista vacía → no se pinta nada (defensivo; el llamador también revisa).
 *
 * @param ocrCandidatos números detectados en la foto (hasta 12).
 * @param recortes recorte de la letra de cada número (puede faltar alguno).
 * @param onElegido toque sobre un número → ese pasa a ser la cantidad.
 * @param onCerrar toque en **✍️ Escribir a mano** (o fuera del diálogo) →
 *   se cierran los candidatos (la persona vuelve a escribir con el teclado).
 */
@Composable
fun CandidatosNumeros(
    ocrCandidatos: List<Int>,
    onElegido: (Int) -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier,
    recortes: Map<Int, Bitmap> = emptyMap(),
) {
    if (ocrCandidatos.isEmpty()) return

    val unico = ocrCandidatos.singleOrNull()

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            // El diálogo COMPLETO se desplaza si la letra es gigante; "Escribir a
            // mano" queda siempre alcanzable.
            val altoMaximo = maxHeight * 0.92f
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = altoMaximo)
                    .semantics { contentDescription = "Números leídos en tu foto" },
                shape = RoundedCornerShape(28.dp),
                color = AppBackground,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = if (unico != null) {
                            "¿Es este el número de tu hoja?"
                        } else {
                            "¿Cuál número ves en tu hoja?"
                        },
                        style = MaterialTheme.typography.titleLarge, // 26sp bold
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (unico != null) {
                        // Confirmación de UN número: recorte grande + cifra.
                        RecorteDeLetra(
                            recorte = recortes[unico],
                            anchoMaximo = 260.dp,
                            altoMaximo = 140.dp,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$unico",
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        SeniorButton(
                            text = "✔ Usar $unico",
                            onClick = { onElegido(unico) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp),
                            variant = SeniorButtonVariant.GREEN,
                            contentDescription = "Usar el número $unico como cantidad",
                        )
                    } else {
                        Text(
                            text = "El recorte te muestra tu letra: toca el número correcto.",
                            fontSize = 20.sp,
                            color = GreyAction,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ocrCandidatos.forEach { numero ->
                            BotonCandidato(
                                numero = numero,
                                recorte = recortes[numero],
                                onClick = { onElegido(numero) },
                            )
                        }
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
    }
}

/**
 * Recorte de la letra: fondo claro con borde suave, escalado para que quepa sin
 * deformarse (se muestra el ancho/alto máximos indicados).
 */
@Composable
private fun RecorteDeLetra(
    recorte: Bitmap?,
    anchoMaximo: androidx.compose.ui.unit.Dp,
    altoMaximo: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    if (recorte == null) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceSoft,
        border = BorderStroke(width = 2.dp, color = GreenAction.copy(alpha = 0.4f)),
    ) {
        Image(
            bitmap = recorte.asImageBitmap(),
            contentDescription = null, // el número ya se anuncia aparte
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = anchoMaximo)
                .heightIn(max = altoMaximo)
                .padding(6.dp),
        )
    }
}

/**
 * Botón GIGANTE de candidato: fondo blanco, borde verde suave 2dp, el recorte de
 * la letra a la izquierda (si existe) y el número en 36sp bold, ancho completo,
 * alto mínimo 72dp y escala 0.98 al tocar.
 * TalkBack lee "Elegir el número N".
 */
@Composable
private fun BotonCandidato(
    numero: Int,
    recorte: Bitmap?,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RecorteDeLetra(
                recorte = recorte,
                anchoMaximo = 150.dp,
                altoMaximo = 64.dp,
            )
            Text(
                text = "$numero",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
