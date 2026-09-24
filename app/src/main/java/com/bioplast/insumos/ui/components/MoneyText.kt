package com.bioplast.insumos.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.CurrencyFormat
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Texto de dinero gigante y legible (cifras 40–80sp).
 *
 * Por defecto SIN decimales visibles (`CurrencyFormat.formatEntero` → "$71"):
 * los totales, dashboards y resúmenes se muestran en pesos enteros; la precisión
 * vive en los datos, no en la pantalla grande.
 *
 * @param valor monto en pesos colombianos.
 * @param size tamaño del texto (por defecto 48sp; usar 40–80sp en cifras principales).
 * @param color color del texto (por defecto casi negro sobre fondo claro).
 * @param conDecimales `true` → con coma decimal ("$71,80"). SOLO para texto
 *   pequeño (20–22sp): precio unitario en [ProductCard] y detalle por fila del
 *   historial. En cifras grandes debe quedar en `false` (regla sénior FASE 3).
 */
@Composable
fun MoneyText(
    valor: Double,
    modifier: Modifier = Modifier,
    size: TextUnit = 48.sp,
    color: Color = TextPrimary,
    conDecimales: Boolean = false
) {
    val texto = if (conDecimales) {
        CurrencyFormat.format(valor)
    } else {
        CurrencyFormat.formatEntero(valor)
    }

    Text(
        text = texto,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.ExtraBold
    )
}
