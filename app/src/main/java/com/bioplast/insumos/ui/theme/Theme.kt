package com.bioplast.insumos.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Esquema SIEMPRE claro (sin dark theme y sin dynamicColor)
 * para mantener contraste estable y predecible.
 */
private val AppColorScheme = lightColorScheme(
    primary = GreenAction,
    onPrimary = OnAction,
    primaryContainer = GreenActionContainer,
    onPrimaryContainer = TextPrimary,

    secondary = GreyAction,
    onSecondary = OnAction,
    secondaryContainer = GreyActionContainer,
    onSecondaryContainer = TextPrimary,

    tertiary = GreenAction,
    onTertiary = OnAction,
    tertiaryContainer = GreenActionContainer,
    onTertiaryContainer = TextPrimary,

    error = RedAction,
    onError = OnAction,
    errorContainer = RedActionContainer,
    onErrorContainer = TextPrimary,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = AppBackground,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextPrimary,

    surfaceContainerLow = AppBackground,
    surfaceContainer = AppBackground,
    surfaceContainerHigh = SurfaceSoft,
    surfaceContainerHighest = SurfaceSoft,

    outline = GreyAction,
    outlineVariant = OutlineSoft,
    scrim = Color.Black
)

/**
 * Esquinas generosas al estilo de los SO nativos (Material You / One UI / iOS):
 * contenedores 24dp, superficies 20dp y elementos chicos 16dp.
 */
private val AppShapes = Shapes(
    extraLarge = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(24.dp),
    medium = RoundedCornerShape(20.dp),
    small = RoundedCornerShape(16.dp)
)

/**
 * Tema de la app. Siempre fondo claro, alto contraste.
 * MainActivity ya llama a este composable.
 */
@Composable
fun CalculadoraInsumosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            content()
        }
    }
}
