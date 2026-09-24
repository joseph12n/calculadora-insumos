package com.bioplast.insumos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.CurrencyFormat
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.ProductAccent
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Tarjeta-botón apilada para elegir un insumo (diálogo de la calculadora).
 * Emoji dentro de un círculo blanco que respeta H-1: mínimo 64dp que crece con
 * el glifo a cualquier escala de fuente y se mantiene circular y centrado
 * (helper [Modifier.circuloMinimo] — nunca un tamaño fijo) + nombre
 * (26sp bold) + precio unitario en gris (22sp CON decimales: es texto pequeño
 * y la precisión ahí sí importa). Toda la tarjeta es clicable (un solo toque,
 * sin gestos), mínimo 80dp de alto, esquinas 20dp, elevación suave y
 * feedback táctil (scale 0.98). Si [selected] es `true` lleva borde verde y
 * una palomita al final, para que se vea cuál está elegido.
 *
 * @param product insumo a mostrar (nombre, precio y emoji vienen del modelo).
 * @param onClick selección del producto.
 * @param selected `true` = es el insumo elegido en este momento.
 */
@Composable
fun ProductCard(
    product: ProductType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .escalaAlTocar(interactionSource),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ProductAccent),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = if (selected) BorderStroke(width = 3.dp, color = GreenAction) else null,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono en círculo de color (estética de SO nativos): sin tamaño
            // fijo — mínimo 64dp que crece con el emoji (regla sénior H-1).
            Surface(
                shape = CircleShape,
                color = Color.White,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.circuloMinimo()
                ) {
                    Text(
                        text = product.emoji,
                        fontSize = 34.sp
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.displayName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    // Precio unitario: texto pequeño CON decimales (excepción FASE 3).
                    text = CurrencyFormat.format(product.unitPriceCop),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = GreyAction
                )
            }
            if (selected) {
                Text(
                    text = "✓",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenAction,
                )
            }
        }
    }
}
