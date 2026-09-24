package com.bioplast.insumos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.ui.components.ProductCard
import com.bioplast.insumos.ui.components.StepHeader
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Pantalla 2 — PASO 1: "¿Qué vas a contar?".
 *
 * Cabecera con **← Atrás** siempre visible + indicador "Paso 1 de 3" (componente
 * [StepHeader]) y, debajo, las 5 tarjetas-botón del catálogo apiladas una sobre
 * otra (emoji + nombre + precio). Un solo toque por tarjeta: cero gestos.
 *
 * La lista sale del propio enum [ProductType.entries]; esta pantalla no calcula
 * ni decide precios, solo notifica la elección a la ViewModel.
 *
 * @param onProductoElegido toque sobre una tarjeta → la ViewModel abre el Paso 2.
 * @param onAtras toque en ← Atrás → vuelve al inicio.
 */
@Composable
fun PickProductScreen(
    onProductoElegido: (ProductType) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(
            step = 1,
            onBack = onAtras,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "¿Qué vas a contar?",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )

        ProductType.entries.forEach { producto ->
            ProductCard(
                product = producto,
                onClick = { onProductoElegido(producto) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
