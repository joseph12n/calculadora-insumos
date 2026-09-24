package com.bioplast.insumos.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bioplast.insumos.model.CurrencyFormat
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.ui.components.CandidatosNumeros
import com.bioplast.insumos.ui.components.Keypad
import com.bioplast.insumos.ui.components.MoneyText
import com.bioplast.insumos.ui.components.ProductCard
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.components.TopBar
import com.bioplast.insumos.ui.components.escalaAlTocar
import com.bioplast.insumos.ui.theme.AppBackground
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.GreenActionContainer
import com.bioplast.insumos.ui.theme.GreyAction
import com.bioplast.insumos.ui.theme.OutlineSoft
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.RedActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/** Altura mínima de una tecla del teclado (regla sénior). */
private val ALTO_MINIMO_TECLA = 64.dp

/**
 * Pantalla principal de captura: **una calculadora**, no un formulario.
 *
 * De arriba abajo, todo con toques simples y sin gestos:
 *  1. **← Atrás** (gris, ≥64dp).
 *  2. Fila del insumo: tarjeta del producto elegido (**CAMBIAR**) + botón
 *     cuadrado **📷 FOTO** (cámara del sistema, ≥64dp).
 *  3. **Visor**: "¿Cuántos?" con el número en 56sp y el **total en vivo**
 *     ("= $72") en verde; ahí mismo salen los avisos ("Primero escribe
 *     cuántos" / "Primero elige el insumo").
 *  4. **Teclado propio** que llena el espacio disponible con teclas TODAS del
 *     mismo alto (1 2 3 / 4 5 6 / 7 8 9 / ⌫ 0 00).
 *  5. **SEGUIR →** verde fijo abajo, siempre visible sin desplazar.
 *
 * El diálogo del insumo se abre solo al entrar sin producto y con **CAMBIAR**;
 * los números leídos de una foto aparecen en su propio diálogo ([CandidatosNumeros])
 * para que nunca queden debajo del pliegue.
 *
 * @param quantityInput dígitos escritos por la persona ("0" si aún no escribe).
 * @param producto insumo elegido (`null` → la fila invita a elegirlo).
 * @param quantityError `true` → aviso "Primero escribe cuántos".
 * @param productError `true` → aviso "Primero elige el insumo".
 * @param totalCop total vivo (cantidad × precio) que se pinta en el visor.
 * @param isSaving `true` mientras la ViewModel persiste (bloquea dobles toques).
 * @param ocrMessage mensaje cuando la foto no se pudo leer (`null` = sin mensaje).
 * @param savingError mensaje de error de guardado/deshacer (`null` = sin error).
 * @param onDigito toque sobre una tecla numérica ("0".."9", "00").
 * @param onBorrarDigito toque sobre la tecla ⌫.
 * @param onProductoElegido insumo elegido en el diálogo.
 * @param onHacerFoto toque en **📷 FOTO** → cámara del sistema.
 * @param onSiguiente toque en **SEGUIR →** → revisar (la ViewModel valida).
 * @param onAtras toque en ← Atrás → vuelve al inicio.
 * @param ocrCandidatos números leídos de la foto cuando hubo varios.
 * @param onCandidatoElegido toque sobre un número del diálogo → es la cantidad.
 * @param onCerrarCandidatos toque en **✍️ Escribir a mano** → cierra el diálogo.
 * @param recortesCandidatos recorte de la letra de cada número leído (para que la
 *   persona compare su propia letra antes de confirmar; puede venir vacío).
 */
@Composable
fun CalculatorScreen(
    quantityInput: String,
    producto: ProductType?,
    quantityError: Boolean,
    productError: Boolean,
    totalCop: Double,
    isSaving: Boolean,
    ocrMessage: String?,
    savingError: String?,
    onDigito: (String) -> Unit,
    onBorrarDigito: () -> Unit,
    onProductoElegido: (ProductType) -> Unit,
    onHacerFoto: () -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    ocrCandidatos: List<Int> = emptyList(),
    onCandidatoElegido: (Int) -> Unit = {},
    onCerrarCandidatos: () -> Unit = {},
    recortesCandidatos: Map<Int, Bitmap> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    // El selector se abre solo cuando todavía no hay insumo elegido; también se
    // reabre al volver de "Agregar otro" (la ViewModel deja producto = null).
    var selectorAbierto by rememberSaveable { mutableStateOf(producto == null) }
    LaunchedEffect(producto) {
        if (producto == null) selectorAbierto = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Zona flexible: TODO el contenido (cabecera, insumo, visor, teclado).
        // Con letra normal no hace falta desplazar nada; con letra gigante (A+
        // al máximo × escala del sistema) se puede desplazar y **SEGUIR →**
        // sigue fijo abajo, siempre visible.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // Estimación del bloque de arriba (cabecera + insumo + visor) MÁS los
            // 12dp de separación y los 36dp de espacios entre las 4 filas: con
            // eso las teclas llenan el espacio disponible sin dejar la última
            // fila a medias. Crece con la letra elegida (A−/A+ × sistema).
            val factorLetra = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val altoFijoEstimado = 390.dp * factorLetra
            val altoTecla = ((maxHeight - altoFijoEstimado) / 4)
                .coerceAtLeast(ALTO_MINIMO_TECLA)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TopBar(onBack = onAtras, backEnabled = !isSaving)

                    // ---- Fila del insumo + acceso a la cámara --------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SelectorDeProducto(
                            producto = producto,
                            onClick = { selectorAbierto = true },
                            modifier = Modifier.weight(1f),
                        )
                        BotonFoto(
                            onClick = onHacerFoto,
                            enabled = !isSaving,
                        )
                    }

                    // ---- Visor: cantidad + total en vivo + avisos ----------
                    VisorDeCantidad(
                        quantityInput = quantityInput,
                        producto = producto,
                        totalCop = totalCop,
                        quantityError = quantityError,
                        productError = productError,
                    )

                    // ---- Avisos OCR / guardado (nunca tapan el número) ------
                    if (ocrMessage != null) {
                        MensajeDeLaFoto(texto = ocrMessage)
                    }
                    if (savingError != null) {
                        MensajeDeLaFoto(texto = savingError, enError = true)
                    }
                }

                // ---- Teclado: teclas del MISMO alto (se ajustan al espacio) --
                Keypad(
                    onDigit = onDigito,
                    onDelete = onBorrarDigito,
                    keyHeight = altoTecla,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        // ---- Acción principal SIEMPRE visible -------------------------------
        SeniorButton(
            text = "SEGUIR →",
            onClick = onSiguiente,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            variant = SeniorButtonVariant.GREEN,
            enabled = !isSaving,
        )
    }

    if (selectorAbierto) {
        DialogoElegirProducto(
            productoActual = producto,
            onElegir = {
                onProductoElegido(it)
                selectorAbierto = false
            },
            onCancelar = { selectorAbierto = false },
        )
    }

    // Varios números en la foto → diálogo ENCIMA de todo para elegir.
    CandidatosNumeros(
        ocrCandidatos = ocrCandidatos,
        onElegido = onCandidatoElegido,
        onCerrar = onCerrarCandidatos,
        recortes = recortesCandidatos,
    )
}

/**
 * Fila del insumo elegido (o invitación a elegirlo). Toda la fila es un botón
 * de ≥72dp con borde: dice **CAMBIAR ▾** cuando ya hay producto y
 * **ELEGIR ▾** cuando no. Un solo toque abre el diálogo.
 */
@Composable
private fun SelectorDeProducto(
    producto: ProductType?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val sinProducto = producto == null
    val description = if (sinProducto) {
        "Elegir el insumo que vas a contar"
    } else {
        "Cambiar el insumo, ahora ${producto.displayName}"
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 72.dp)
            .escalaAlTocar(interactionSource)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(20.dp),
        color = if (sinProducto) GreenActionContainer else SurfaceSoft,
        border = BorderStroke(
            width = 2.dp,
            color = if (sinProducto) GreenAction else OutlineSoft,
        ),
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (producto != null) {
                    Text(text = producto.emoji, fontSize = 30.sp)
                }
                Text(
                    text = if (sinProducto) "Elegir insumo" else producto.displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sinProducto) GreenAction else TextPrimary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = if (sinProducto) "ELEGIR ▾" else "CAMBIAR ▾",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GreenAction,
            )
        }
    }
}

/**
 * Botón cuadrado **📷 FOTO** (≥72dp de alto, estilo OUTLINE): abre la cámara
 * del sistema para fotografiar la hoja. Es la única entrada a la lectura de
 * números; ya no hay dos botones distintos de cámara/galería.
 */
@Composable
private fun BotonFoto(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 72.dp)
            .escalaAlTocar(interactionSource)
            .semantics { contentDescription = "Hacer una foto a la hoja para leer el número" },
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(width = 2.dp, color = GreyAction),
        interactionSource = interactionSource,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "📷", fontSize = 26.sp)
            Text(
                text = "FOTO",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
    }
}

/**
 * Visor tipo calculadora: "¿Cuántos?" + cifra grande + precio unitario y
 * **total en vivo** ("= $72" en verde). Los avisos de validación viven aquí,
 * junto al número, para que la persona sepa qué corregir sin buscar.
 */
@Composable
private fun VisorDeCantidad(
    quantityInput: String,
    producto: ProductType?,
    totalCop: Double,
    quantityError: Boolean,
    productError: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GreenActionContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "¿Cuántos?",
                style = MaterialTheme.typography.titleMedium,
                color = GreyAction,
            )
            Text(
                text = quantityInput,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (producto != null) {
                        "${CurrencyFormat.format(producto.unitPriceCop)} cada uno"
                    } else {
                        "Elige el insumo"
                    },
                    fontSize = 20.sp,
                    color = GreyAction,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "=",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenAction,
                )
                MoneyText(valor = totalCop, size = 30.sp, color = GreenAction)
            }

            if (productError) {
                Text(
                    text = "Primero elige el insumo",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedAction,
                )
            }
            if (quantityError) {
                Text(
                    text = "Primero escribe cuántos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedAction,
                )
            }
        }
    }
}

/**
 * Diálogo para elegir el insumo: título **"¿Qué vas a contar?"** y las 5
 * tarjetas de [ProductCard] (la elegida con borde verde y ✓). Un solo toque
 * elige y cierra; **Cancelar** (gris) cierra sin cambiar nada.
 */
@Composable
private fun DialogoElegirProducto(
    productoActual: ProductType?,
    onElegir: (ProductType) -> Unit,
    onCancelar: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints {
            // El diálogo COMPLETO (título + tarjetas + Cancelar) se desplaza si
            // la letra es gigante: nunca se recorta el botón de salida.
            val altoMaximo = maxHeight * 0.92f
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .heightIn(max = altoMaximo),
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
                        text = "¿Qué vas a contar?",
                        style = MaterialTheme.typography.headlineMedium, // 32sp bold
                        color = TextPrimary,
                    )

                    ProductType.entries.forEach { insumo ->
                        ProductCard(
                            product = insumo,
                            onClick = { onElegir(insumo) },
                            selected = insumo == productoActual,
                        )
                    }

                    SeniorButton(
                        text = "Cancelar",
                        onClick = onCancelar,
                        modifier = Modifier.fillMaxWidth(),
                        variant = SeniorButtonVariant.GREY,
                        contentDescription = "Cancelar y volver a la calculadora",
                    )
                }
            }
        }
    }
}

/**
 * Recuadro de aviso del visor (mensaje de la foto o error de guardado):
 * fondo suave —o rojo suave si [enError]— con texto 22sp. Nunca toca el
 * número escrito.
 */
@Composable
private fun MensajeDeLaFoto(
    texto: String,
    modifier: Modifier = Modifier,
    enError: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (enError) RedActionContainer else SurfaceSoft,
    ) {
        Text(
            text = texto,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enError) RedAction else TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
