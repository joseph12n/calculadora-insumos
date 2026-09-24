package com.bioplast.insumos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.CurrencyFormat
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.model.WeeklySummary
import com.bioplast.insumos.ui.components.MoneyText
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.components.TopBar
import com.bioplast.insumos.ui.components.escalaAlTocar
import com.bioplast.insumos.ui.components.fechaCorta
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.OnAction
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.RedActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Pantalla 6 — DETALLE de la semana elegida en el historial.
 *
 * Encabezado con **← Atrás**, "Semana N" y el total; debajo, filas enormes con
 * fecha corta, "10 Frascos de orina", el total en cifra grande y el botón rojo
 * **🗑 Borrar** por fila. Borrar SIEMPRE pasa antes por un diálogo de
 * confirmación (AlertDialog de Material 3): "¿Borrar este registro?" con
 * "Sí, borrar" (rojo) y "Cancelar" (gris). El borrado real lo hace la
 * ViewModel ([onEliminarRegistro]); esta pantalla solo lo confirma.
 *
 * Si el borrado FALLA, la ViewModel lo comunica en `savingError` y aquí se
 * pinta en un recuadro rojo 22sp (nunca un error en silencio).
 *
 * Estado vacío: "Esta semana no tiene registros."
 *
 * @param semana semana abierta (`null` defensivo → encabezado mínimo).
 * @param registros filas de esa semana (orden que entrega la ViewModel).
 * @param onEliminarRegistro confirmación del diálogo → borra la fila.
 * @param onAtras toque en ← Atrás → vuelve al historial.
 * @param savingError mensaje de error de la ViewModel (`null` = sin error);
 *   p. ej. "No pude borrar el registro. Intenta una vez más."
 */
@Composable
fun DayDetailScreen(
    semana: WeeklySummary?,
    registros: List<InventoryRecord>,
    onEliminarRegistro: (InventoryRecord) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    savingError: String? = null,
) {
    /** Fila que espera confirmación; `null` = diálogo cerrado. */
    var registroAEliminar by remember { mutableStateOf<InventoryRecord?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TopBar(
            onBack = onAtras,
            contentDescription = "Atrás, volver al historial",
        )

        if (semana != null) {
            Text(
                text = "Semana ${semana.weekOfYear}",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
            )
            MoneyText(valor = semana.totalCop, size = 52.sp)
            Text(
                text = "${semana.totalQuantity} insumos",
                style = MaterialTheme.typography.bodyLarge,
                color = GreyAction,
                fontWeight = FontWeight.Bold,
            )
        }

        // Error de la ViewModel (p. ej. "No pude borrar el registro…"):
        // recuadro rojo 22sp, mismo patrón que ConfirmScreen.
        if (savingError != null) {
            RecuadroError(texto = savingError)
        }

        if (registros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "Esta semana no tiene registros.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(items = registros, key = { it.id }) { registro ->
                    FilaRegistro(
                        registro = registro,
                        onBorrar = { registroAEliminar = registro },
                    )
                }
            }
        }
    }

    registroAEliminar?.let { registro ->
        AlertDialog(
            onDismissRequest = { registroAEliminar = null },
            title = {
                Text(
                    text = "¿Borrar este registro?",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            },
            text = {
                Text(
                    text = "${registro.quantity} ${registro.productName} · " +
                        CurrencyFormat.format(registro.totalCop),
                    fontSize = 22.sp,
                    color = TextPrimary,
                )
            },
            confirmButton = {
                BotonDeDialogo(
                    texto = "Sí, borrar",
                    color = RedAction,
                    onClick = {
                        registroAEliminar = null
                        onEliminarRegistro(registro)
                    },
                )
            },
            dismissButton = {
                BotonDeDialogo(
                    texto = "Cancelar",
                    color = GreyAction,
                    onClick = { registroAEliminar = null },
                )
            },
        )
    }
}

/**
 * Fila de un registro: fecha corta + "10 Frascos de orina" + total en texto
 * pequeño CON decimales (22sp; detalle del historial es la excepción que sí
 * muestra coma decimal) + botón rojo **🗑 Borrar** (≥64dp) a la derecha.
 * Esquinas 20dp, elevación suave.
 */
@Composable
private fun FilaRegistro(
    registro: InventoryRecord,
    onBorrar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = fechaCorta(registro.date),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreyAction,
            )
            Text(
                text = "${registro.quantity} ${registro.productName}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Detalle por fila: texto pequeño CON decimales (excepción FASE 3).
                MoneyText(
                    valor = registro.totalCop,
                    size = 22.sp,
                    conDecimales = true,
                    modifier = Modifier.weight(1f),
                )
                SeniorButton(
                    text = "🗑 Borrar",
                    onClick = onBorrar,
                    variant = SeniorButtonVariant.RED,
                    contentDescription =
                        "Borrar el registro de ${registro.quantity} ${registro.productName}",
                )
            }
        }
    }
}

/**
 * Botón del diálogo de confirmación (Material 3 [AlertDialog]).
 *
 * NOTA de diseño: aquí NO se reutiliza [SeniorButton] porque DOS de sus botones
 * (24sp + relleno por defecto) no caben en el ancho del diálogo y se saldrían
 * del recuadro. Este botón replica las reglas sénior: ≥64dp de alto, esquinas
 * 16dp, texto bold ≥20sp y el mismo código de color (rojo = borrar,
 * gris = cancelar).
 */
@Composable
private fun BotonDeDialogo(
    texto: String,
    color: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Button(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 64.dp)
            .escalaAlTocar(interactionSource),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = OnAction,
        ),
        interactionSource = interactionSource,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 12.dp,
        ),
    ) {
        Text(
            text = texto,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Recuadro rojo para [savingError]: fondo rojo suave + texto 22sp. Se pinta
 * debajo del encabezado para que un borrado fallido NUNCA pase en silencio.
 */
@Composable
private fun RecuadroError(texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = RedActionContainer,
    ) {
        Text(
            text = texto,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = RedAction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}
