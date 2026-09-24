package com.bioplast.insumos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.ui.components.MoneyText
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Pantalla 4 — ¡LISTO! (éxito tras guardar el LOTE completo).
 *
 * Palomita verde enorme + "¡Guardado!" + resumen del lote
 * ("N productos · TOTAL" en MONEY sin decimales). **↩ DESHACER** solo se
 * muestra mientras la ventana de ~5 s siga abierta (`canUndo`): borra el lote
 * recién guardado y RESTAURA la lista en la revisión. **LISTO** vuelve al inicio.
 *
 * NOTA: la ViewModel es quien vuelve a START sola al cerrar la ventana de 5 s
 * (`InventoryViewModel.programarCierreDeVentanaDeDeshacer`); esta pantalla no
 * pone temporizadores propios ni decide cuándo cerrar.
 *
 * El resumen se deriva de [lastSavedRecords] (lo que de verdad se insertó);
 * [cantidadLote]/[totalLoteCop] sirven de respaldo defensivo.
 *
 * @param lastSavedRecords lote insertado (con ids reales).
 * @param cantidadLote cuántos productos llevaba el lote.
 * @param totalLoteCop total del lote en COP.
 * @param canUndo `true` dentro de la ventana de deshacer (~5 s).
 * @param onDeshacer toque en **↩ DESHACER** → borra el lote y restaura la lista.
 * @param onListo toque en **LISTO** → vuelve al inicio limpiando la sesión.
 */
@Composable
fun SuccessScreen(
    lastSavedRecords: List<InventoryRecord>,
    cantidadLote: Int,
    totalLoteCop: Double,
    canUndo: Boolean,
    onDeshacer: () -> Unit,
    onListo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resumen del lote: lo insertado manda; si estuviera vacío, el estado del lote.
    // Nota: `cantidadLote` cuenta UNIDADES (suma de cantidades), igual que en el
    // revisión; tras guardar, los pendientes ya están vacíos, así que se recalcula
    // desde lastSavedRecords para mostrar el lote COMPLEto y no solo el item actual.
    val guardados = lastSavedRecords
    val totalProductos = if (guardados.isNotEmpty()) {
        guardados.sumOf { it.quantity }
    } else {
        cantidadLote
    }
    val totalCop = if (guardados.isNotEmpty()) {
        guardados.sumOf { it.totalCop }
    } else {
        totalLoteCop
    }
    val plural = if (totalProductos == 1) "producto" else "productos"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null, // "¡Guardado!" ya lo anuncia TalkBack
            tint = GreenAction,
            modifier = Modifier.size(160.dp),
        )

        Text(
            text = "¡Guardado!",
            fontSize = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            color = GreenAction,
            textAlign = TextAlign.Center,
        )

        // "N productos · TOTAL" con MONEY sin decimales (pesos enteros).
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "$totalProductos $plural · Total",
                style = MaterialTheme.typography.titleLarge,
                color = GreyAction,
                textAlign = TextAlign.Center,
            )
            MoneyText(valor = totalCop, size = 56.sp)
        }

        if (canUndo) {
            SeniorButton(
                text = "↩ DESHACER",
                onClick = onDeshacer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
                variant = SeniorButtonVariant.RED,
                contentDescription =
                    "Deshacer, quitar los $totalProductos $plural que acabo de guardar",
            )
        }

        // Mientras la ventana de 5 s siga abierta, la ViewModel volverá a START
        // sola; LISTO solo acelera esa vuelta y limpia la sesión.
        SeniorButton(
            text = "LISTO",
            onClick = onListo,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            variant = SeniorButtonVariant.GREEN,
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
