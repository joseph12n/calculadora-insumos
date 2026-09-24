package com.bioplast.insumos.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.ui.components.MoneyText
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.components.TopBar
import com.bioplast.insumos.ui.components.circuloMinimo
import com.bioplast.insumos.ui.components.escalaAlTocar
import com.bioplast.insumos.ui.components.fechaCorta
import com.bioplast.insumos.ui.components.pluralDe
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreenActionContainer
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.GreyActionContainer
import com.bioplast.insumos.ui.theme.OnAction
import com.bioplast.insumos.ui.theme.ProductAccent
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.RedActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Pantalla 3 — REVISAR "Tu lista": revisión del item actual y del LOTE.
 *
 * Arriba: cabecera (**← Atrás**) y "¿Guardamos esto?".
 * Después la fila del item actual (cantidad × producto en texto enorme, MONEY
 * sin decimales de 48–60sp, fecha + botón **📅 Cambiar fecha**).
 *
 * Luego **➕ Agregar otro** (OUTLINE gris, ≥64dp) que vuelve a la calculadora dejando
 * la lista intacta, y la SECCIÓN "En tu lista" con los pendientes: cada fila
 * trae producto, fecha y total (entero), el botón circular **📅** para cambiar
 * la fecha de ese item (mismo DatePicker) y el botón circular **✕** ROJO para
 * quitarlo (≥48dp, rojo SOLO = borrar). Si no hay pendientes, la sección no
 * aparece: queda solo el item actual (sin texto raro).
 *
 * Abajo: totales del lote ("N productos" + MONEY entero), **✔ GUARDAR TODO**
 * (verde, guarda el lote completo) y **← Corregir** (gris). Si la ViewModel
 * reporta `savingError`, se muestra en un recuadro rojo y GUARDAR TODO sigue
 * visible para reintentar con un toque.
 *
 * @param cantidad número del item actual.
 * @param producto insumo del item actual (`null` defensivo → solo la cifra).
 * @param totalCop total vivo del item actual (cantidad × precio).
 * @param selectedDate fecha del item actual (hoy por defecto).
 * @param itemsPendientes productos ya agregados al lote (drafts), en orden.
 * @param cantidadLote cuántos productos llevará el lote al guardar.
 * @param totalLoteCop suma del lote: pendientes + item actual (MONEY entero).
 * @param isSaving `true` mientras persiste (bloquea GUARDAR / Atrás).
 * @param savingError mensaje de error de persistencia (`null` = sin error).
 * @param onGuardar toque en **✔ GUARDAR TODO** → guarda el lote completo.
 * @param onAgregarOtro toque en **➕ Agregar otro** → calculadora dejando la lista.
 * @param onQuitarPendiente toque en ✕ de la fila `pos` → la quita de la lista.
 * @param onFechaDePendiente fecha elegida para el pendiente `pos`.
 * @param onAtras toque en **← Corregir** → vuelve a la calculadora.
 * @param onFecha nueva fecha elegida para el item actual.
 */
@Composable
fun ConfirmScreen(
    cantidad: Int,
    producto: ProductType?,
    totalCop: Double,
    selectedDate: LocalDate,
    itemsPendientes: List<InventoryRecord>,
    cantidadLote: Int,
    totalLoteCop: Double,
    isSaving: Boolean,
    savingError: String?,
    onGuardar: () -> Unit,
    onAgregarOtro: () -> Unit,
    onQuitarPendiente: (Int) -> Unit,
    onFechaDePendiente: (Int, LocalDate) -> Unit,
    onAtras: () -> Unit,
    onFecha: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarSelectorDeFecha by remember { mutableStateOf(false) }

    /** Índice del pendiente cuyo calendario está abierto; `null` = cerrado. */
    var indiceFechaPendiente by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TopBar(onBack = onAtras, backEnabled = !isSaving)

        Text(
            text = "¿Guardamos esto?",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // -------------------------------------------------------------
        // Item ACTUAL: "10 Frascos de orina" + MONEY sin decimales + fecha
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ProductAccent),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val nombre = producto?.displayName
                Text(
                    text = if (nombre != null) "$cantidad $nombre" else "$cantidad",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Cifra gigante 56sp (rango sénior 40–80sp), SIN decimales.
                MoneyText(valor = totalCop, size = 56.sp)

                Text(
                    text = "Fecha: ${fechaLegible(selectedDate)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                )

                SeniorButton(
                    text = "📅 Cambiar fecha",
                    onClick = { mostrarSelectorDeFecha = true },
                    modifier = Modifier.fillMaxWidth(),
                    variant = SeniorButtonVariant.OUTLINE,
                    contentDescription = "Cambiar la fecha del registro actual",
                )
            }
        }

        // -------------------------------------------------------------
        // ➕ Agregar otro → calculadora dejando la lista intacta
        // -------------------------------------------------------------
        SeniorButton(
            text = "➕ Agregar otro",
            onClick = onAgregarOtro,
            modifier = Modifier.fillMaxWidth(),
            variant = SeniorButtonVariant.OUTLINE,
            enabled = !isSaving,
            contentDescription = "Agregar otro producto a tu lista",
        )

        // -------------------------------------------------------------
        // SECCIÓN LISTA (solo si hay pendientes; si no, solo el item actual)
        // -------------------------------------------------------------
        if (itemsPendientes.isNotEmpty()) {
            Text(
                text = "En tu lista",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
            )

            itemsPendientes.forEachIndexed { posicion, pendiente ->
                FilaPendiente(
                    pendiente = pendiente,
                    onQuitar = { onQuitarPendiente(posicion) },
                    onCambiarFecha = { indiceFechaPendiente = posicion },
                )
            }
        }

        // -------------------------------------------------------------
        // Totales del lote (MONEY entero)
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = GreenActionContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "$cantidadLote ${pluralDe(cantidadLote)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                MoneyText(valor = totalLoteCop, size = 56.sp)
            }
        }

        if (savingError != null) {
            RecuadroErrorGuardado(texto = savingError)
        }

        SeniorButton(
            text = "✔ GUARDAR TODO",
            onClick = onGuardar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            variant = SeniorButtonVariant.GREEN,
            enabled = !isSaving,
            contentDescription = "Guardar los $cantidadLote productos de tu lista",
        )

        SeniorButton(
            text = "← Corregir",
            onClick = onAtras,
            modifier = Modifier.fillMaxWidth(),
            variant = SeniorButtonVariant.GREY,
            enabled = !isSaving,
        )
    }

    // Selector del item ACTUAL.
    if (mostrarSelectorDeFecha) {
        DatePickerModal(
            fechaActual = selectedDate,
            onConfirmar = { fecha ->
                mostrarSelectorDeFecha = false
                onFecha(fecha)
            },
            onCancelar = { mostrarSelectorDeFecha = false },
        )
    }

    // Selector de UN pendiente (mismo DatePicker).
    val posicionEnFecha = indiceFechaPendiente
    val pendienteEnFecha = posicionEnFecha?.let { itemsPendientes.getOrNull(it) }
    if (posicionEnFecha != null && pendienteEnFecha != null) {
        DatePickerModal(
            fechaActual = pendienteEnFecha.date,
            onConfirmar = { fecha ->
                indiceFechaPendiente = null
                onFechaDePendiente(posicionEnFecha, fecha)
            },
            onCancelar = { indiceFechaPendiente = null },
        )
    }
}

/**
 * Fila de un producto pendiente del lote: "10 Frascos de orina / lun 23 / $71",
 * con botón circular **📅** (cambiar fecha de ESTE item) y botón circular
 * **✕** ROJO (quitar de la lista, ≥48dp; rojo SOLO = borrar).
 */
@Composable
private fun FilaPendiente(
    pendiente: InventoryRecord,
    onQuitar: () -> Unit,
    onCambiarFecha: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "${pendiente.quantity} ${pendiente.productName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = fechaCorta(pendiente.date),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreyAction,
                )
                // Total del pendiente SIN decimales (pesos enteros).
                MoneyText(valor = pendiente.totalCop, size = 26.sp)
            }

            // 📅 circular (gris): cambiar la fecha de este item.
            BotonCircularDeFila(
                glypho = "📅",
                contentDescription = "Cambiar la fecha de ${pendiente.quantity} ${pendiente.productName}",
                containerColor = GreyActionContainer,
                contentColor = GreyAction,
                onClick = onCambiarFecha,
            )

            // ✕ circular (ROJO): quitar de la lista.
            BotonCircularDeFila(
                glypho = "✕",
                contentDescription = "Quitar de tu lista ${pendiente.quantity} ${pendiente.productName}",
                containerColor = RedAction,
                contentColor = Color.White,
                onClick = onQuitar,
            )
        }
    }
}

/**
 * Botón circular de fila (icono en círculo de color): mínimo 64dp que CRECE
 * con el glifo (regla sénior H-1), feedback táctil scale 0.98 y un solo toque.
 * El rojo se usa SOLO para quitar.
 *
 * El cuadrado lo da [Modifier.circuloMinimo] (helpers en
 * `ui/components/Circulo.kt`) sobre el CONTENIDO (no sobre la
 * Surface) para que el círculo cubra todo el área. NOTA (hallazgo verificado en
 * el bytecode de foundation-layout 1.7.6): aquí NO se usa `aspectRatio(1f)`
 * porque dentro de una `Row` el hijo sin peso recibe `maxWidth` FINITO (resto de
 * la fila) y `AspectRatioNode.findSize` intenta `tryMaxWidth` ANTES que el
 * contenido → el botón se convertiría en un cuadrado del ancho completo de la
 * fila. Esta variante mide primero el contenido y toma el lado mayor.
 */
@Composable
private fun BotonCircularDeFila(
    glypho: String,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = contentDescription
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        modifier = modifier
            .escalaAlTocar(interactionSource)
            .semantics { this.contentDescription = description },
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.circuloMinimo(),
        ) {
            Text(
                text = glypho,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

/**
 * Recuadro rojo para [savingError]: fondo rojo suave + texto 22sp. El botón
 * **✔ GUARDAR TODO** sigue visible debajo, así que reintentar es un solo toque.
 */
@Composable
private fun RecuadroErrorGuardado(texto: String, modifier: Modifier = Modifier) {
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

/**
 * Selector de fecha de Material 3 ([DatePickerDialog] + [DatePicker]) para la
 * fecha del registro. La conversión LocalDate ⇄ millis usa UTC (mismo criterio
 * interno del DatePicker) para no correr de día al elegir cerca de medianoche.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    fechaActual: LocalDate,
    onConfirmar: (LocalDate) -> Unit,
    onCancelar: () -> Unit,
) {
    val inicialMillis = fechaActual.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = inicialMillis)

    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            // ≥64dp (hallazgo 4): los TextButton de M3 quedaban en ~48dp.
            // Verde = avanzar/confirmar (regla sénior).
            BotonDeFecha(
                texto = "Listo",
                color = GreenAction,
                onClick = {
                    val elegido = state.selectedDateMillis
                    if (elegido != null) {
                        onConfirmar(
                            Instant_a_LocalDate(elegido)
                        )
                    } else {
                        onCancelar()
                    }
                }
            )
        },
        dismissButton = {
            // Gris = volver/cancelar (regla sénior).
            BotonDeFecha(
                texto = "Cancelar",
                color = GreyAction,
                onClick = onCancelar,
            )
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * Botón del diálogo de fecha ≥64dp con texto 22sp bold, esquinas 20dp y
 * feedback táctil. Verde para confirmar, gris para cancelar (código de color
 * sénior); el rojo queda SOLO para borrar.
 */
@Composable
private fun BotonDeFecha(
    texto: String,
    color: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

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
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        interactionSource = interactionSource,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 20.dp,
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

/** millis (época, UTC) → [LocalDate] sin corrimiento de huso horario. */
private fun Instant_a_LocalDate(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** Fecha en lenguaje cotidiano, p. ej. "23 sept 2026" (es-CO, solo fecha). */
private fun fechaLegible(fecha: LocalDate): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale("es", "CO"))
        .format(fecha)
