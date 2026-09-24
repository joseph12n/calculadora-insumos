package com.bioplast.insumos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.WeeklySummary
import com.bioplast.insumos.ui.components.MoneyText
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.components.escalaAlTocar
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Pantalla 6 — HISTORIAL de semanas ("Registro semanal").
 *
 * Header simple (en vez de [com.bioplast.insumos.ui.components.StepHeader],
 * que está pensado para "Paso X de 3"): botón **← Atrás** + título grande.
 * Debajo, tarjetas grandes en orden descendente —la lista ya viene ordenada
 * desde la ViewModel— con "Semana N", el total en cifra grande y "N insumos".
 * Un toque sobre la tarjeta abre el detalle de esa semana.
 *
 * Estado vacío: "Aún no has registrado nada." en texto grande.
 *
 * @param resumenes semanas registradas, de la más reciente a la más antigua.
 * @param onElegirSemana toque sobre una tarjeta → abre el detalle de la semana.
 * @param onAtras toque en ← Atrás → vuelve al inicio.
 */
@Composable
fun HistoryScreen(
    resumenes: List<WeeklySummary>,
    onElegirSemana: (WeeklySummary) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SeniorButton(
            text = "← Atrás",
            onClick = onAtras,
            modifier = Modifier.heightIn(min = 64.dp),
            variant = SeniorButtonVariant.GREY,
            contentDescription = "Atrás, volver al inicio",
        )

        Text(
            text = "Registro semanal",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )

        if (resumenes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "Aún no has registrado nada.",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = resumenes,
                    key = { "${it.year}-${it.weekOfYear}" },
                ) { resumen ->
                    TarjetaSemana(resumen = resumen, onClick = { onElegirSemana(resumen) })
                }
            }
        }
    }
}

/** Tarjeta grande de una semana: "Semana N" + total SIN decimales + "N insumos". */
@Composable
private fun TarjetaSemana(
    resumen: WeeklySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .escalaAlTocar(interactionSource),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Semana ${resumen.weekOfYear}",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            MoneyText(valor = resumen.totalCop, size = 48.sp)
            Text(
                text = "${resumen.totalQuantity} insumos",
                style = MaterialTheme.typography.bodyLarge,
                color = GreyAction,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
