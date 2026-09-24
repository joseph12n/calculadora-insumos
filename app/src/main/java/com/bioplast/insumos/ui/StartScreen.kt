package com.bioplast.insumos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.ui.components.MoneyText
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.components.pluralDe
import com.bioplast.insumos.ui.texto.LocalControlDeTexto
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreenActionContainer
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.GreyActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Pantalla 1 — INICIO.
 *
 * Jerarquía clara en tres bloques:
 *  1. **Resumen de la semana** (tarjeta "visor": Semana N, total grande,
 *     insumos contados) — la memoria semanal, que es la función distintiva.
 *  2. **Acciones**: ➕ CONTAR INSUMOS (verde, la principal) y, si hay una lista
 *     empezada, el aviso + CONTINUAR LISTA. 📋 VER REGISTROS queda como
 *     secundaria en estilo OUTLINE para que no compita con la principal.
 *  3. **Tamaño de letra** (A− / A+), en una sola fila compacta.
 *
 * Cero gestos y cero menús ocultos: todo se alcanza con un solo toque.
 *
 * Estados propios de esta pantalla:
 *  - `loading` → círculo de carga grande mientras Room no ha emitido.
 *  - `dataError` → "No pude cargar tus datos. Intenta de nuevo." + botón reintentar.
 *
 * La pantalla no calcula nada: los números llegan ya resueltos desde la ViewModel
 * (`null` de `weeklySummary` se traduce a `$0` / `0 insumos` antes de llegar aquí).
 *
 * @param semanaActual número de semana de hoy (sale de la ViewModel).
 * @param totalCop acumulado en COP de la semana actual.
 * @param totalInsumos insumos contados esta semana.
 * @param productosEnLista cuántos productos lleva la lista empezada (0 = ninguna).
 * @param loading `true` mientras no llega el primer resumen de Room.
 * @param dataError `true` si la base local no respondió (estado reintentable).
 * @param onContar toque en ➕ CONTAR INSUMOS → abre la calculadora.
 * @param onContinuarLista toque en **CONTINUAR LISTA** → vuelve a revisar la lista.
 * @param onVerRegistros toque en 📋 VER REGISTROS → abre el historial.
 * @param onReintentar toque en "Intentar de nuevo" del estado de error.
 */
@Composable
fun StartScreen(
    semanaActual: Int,
    totalCop: Double,
    totalInsumos: Long,
    productosEnLista: Int,
    loading: Boolean,
    dataError: Boolean,
    onContar: () -> Unit,
    onContinuarLista: () -> Unit,
    onVerRegistros: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            loading -> EstadoCargando()

            dataError -> EstadoSinDatos(onReintentar = onReintentar)

            else -> {
                Text(
                    text = "¿Qué hacemos hoy?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                TarjetaResumenSemanal(
                    semanaActual = semanaActual,
                    totalCop = totalCop,
                    totalInsumos = totalInsumos,
                )

                // Lista empezada: aviso + CONTINUAR LISTA.
                if (productosEnLista > 0) {
                    AvisoListaEnCurso(productosEnLista = productosEnLista)

                    SeniorButton(
                        text = "CONTINUAR LISTA",
                        onClick = onContinuarLista,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp),
                        variant = SeniorButtonVariant.GREEN,
                        contentDescription =
                            "Continuar tu lista, $productosEnLista ${pluralDe(productosEnLista)} pendientes",
                    )
                }

                SeniorButton(
                    text = "➕ CONTAR INSUMOS",
                    onClick = onContar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 88.dp),
                    variant = SeniorButtonVariant.GREEN,
                    contentDescription = "Contar insumos, empezar un registro nuevo",
                )

                SeniorButton(
                    text = "📋 VER REGISTROS",
                    onClick = onVerRegistros,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    variant = SeniorButtonVariant.OUTLINE,
                    contentDescription = "Ver los registros de semanas anteriores",
                )

                ControlTamanoLetra()
            }
        }
    }
}

/**
 * Resumen de la semana (visor): etiqueta "ESTA SEMANA · Semana N", cifra
 * grande SIN decimales y "N insumos". Es la función de memoria semanal
 * destacada como protagonista del Inicio.
 */
@Composable
private fun TarjetaResumenSemanal(
    semanaActual: Int,
    totalCop: Double,
    totalInsumos: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = GreenActionContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "ESTA SEMANA",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GreyAction,
            )
            Text(
                text = "Semana $semanaActual",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            MoneyText(valor = totalCop, size = 56.sp, modifier = Modifier.padding(top = 6.dp))
            Text(
                text = "$totalInsumos insumos",
                style = MaterialTheme.typography.bodyLarge,
                color = GreyAction,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Aviso de lista empezada: "📋 Tienes N productos en tu lista". */
@Composable
private fun AvisoListaEnCurso(
    productosEnLista: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GreyActionContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "📋", fontSize = 40.sp)
            Text(
                text = "Tienes $productosEnLista ${pluralDe(productosEnLista)} en tu lista",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Control de tamaño de letra (persistente): en UNA fila, la etiqueta
 * "Tamaño de letra" + **A−** (OUTLINE) / **A+** (GREY) de 64dp. Se deshabilitan
 * al llegar a los límites (0.85..1.6, paso 0.15) y el cambio se aplica al
 * instante en toda la app (ver [com.bioplast.insumos.ui.texto.ConEscalaDeTexto]).
 */
@Composable
private fun ControlTamanoLetra(modifier: Modifier = Modifier) {
    val control = LocalControlDeTexto.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceSoft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tamaño de letra",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = GreyAction,
                modifier = Modifier.weight(1f),
            )
            SeniorButton(
                text = "A−",
                onClick = control.disminuir,
                modifier = Modifier.heightIn(min = 64.dp),
                variant = SeniorButtonVariant.OUTLINE,
                enabled = control.puedeDisminuir,
                contentDescription = "Letra más pequeña",
            )
            SeniorButton(
                text = "A+",
                onClick = control.aumentar,
                modifier = Modifier.heightIn(min = 64.dp),
                variant = SeniorButtonVariant.GREY,
                enabled = control.puedeAumentar,
                contentDescription = "Letra más grande",
            )
        }
    }
}

/** Estado "cargando": círculo verde grande + palabra "Cargando…". */
@Composable
private fun EstadoCargando(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(72.dp),
                color = GreenAction,
                strokeWidth = 8.dp,
            )
            Text(
                text = "Cargando…",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
        }
    }
}

/** Estado "sin datos": mensaje en lenguaje cotidiano + botón de reintento. */
@Composable
private fun EstadoSinDatos(
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "No pude cargar tus datos. Intenta de nuevo.",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        SeniorButton(
            text = "Intentar de nuevo",
            onClick = onReintentar,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            variant = SeniorButtonVariant.GREEN,
        )
    }
}
