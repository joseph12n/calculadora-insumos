package com.bioplast.insumos.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.ui.components.CandidatosNumeros
import com.bioplast.insumos.ui.components.Keypad
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.components.StepHeader
import com.bioplast.insumos.ui.theme.RedAction
import com.bioplast.insumos.ui.theme.RedActionContainer
import com.bioplast.insumos.ui.theme.SurfaceSoft
import com.bioplast.insumos.ui.theme.TextPrimary

/**
 * Pantalla 3 — PASO 2: "¿Cuántos?".
 *
 * Arriba la cabecera (**← Atrás** + "Paso 2 de 3"), luego el número escrito en
 * 72sp, el producto elegido, los avisos, el **teclado numérico propio**
 * ([Keypad] — nunca el del sistema), el botón opcional de cámara y por último
 * el botón verde **SEGUIR →**.
 *
 * Reglas sénior de esta pantalla:
 *  - Todo el contenido cabe con scroll vertical únicamente cuando la pantalla
 *    es pequeña o el texto del sistema está al 200 %; el scroll jamás sustituye
 *    a un botón (las acciones siguen siendo toques simples).
 *  - `quantityError` muestra el aviso rojo "Primero escribe cuántos" y mantiene
 *    **SEGUIR →** deshabilitado.
 *  - `ocrMessage` se muestra en un recuadro suave **sin borrar el número ya
 *    escrito** (la ViewModel ya garantiza eso). Aquí llegan tanto
 *    `MENSAJE_OCR_FALLIDO` (cámara en vivo) como `MENSAJE_IMAGEN_FALLIDO`
 *    (foto de la cuenta que no se pudo leer): se pintan IGUAL, sin cambios.
 *  - `savingError` (p. ej. "no pude deshacer") aparece en un recuadro rojo
 *    discreto; los botones siguen disponibles.
 *
 * FASE 4 — imagen de la cuenta: el botón **🖼 Cargar imagen de la cuenta**
 * (OUTLINE) abre el Photo Picker del sistema (MainActivity, sin permisos);
 * mientras se lee la foto aparece un overlay "Leyendo tu imagen..." y luego:
 * 1 número → se carga directo en la cantidad; varios → la sección
 * [CandidatosNumeros] sobre el teclado; ninguno → `ocrMessage`.
 *
 * @param quantityInput dígitos escritos por el usuario ("0" si aún no escribe).
 * @param producto insumo elegido en el Paso 1 (`null` → sin fila de producto).
 * @param quantityError `true` → mostrar aviso y deshabilitar SEGUIR.
 * @param isSaving `true` mientras la ViewModel persiste (bloquea dobles toques).
 * @param ocrMessage mensaje del OCR fallido (`null` = sin mensaje).
 * @param savingError mensaje de error de guardado/deshacer (`null` = sin error).
 * @param onDigito toque sobre una tecla numérica ("0".."9").
 * @param onBorrarDigito toque sobre la tecla ⌫ Borrar.
 * @param onEscanear toque en 📋 Escanear con la cámara (opcional).
 * @param onSiguiente toque en **SEGUIR →** → Paso 3 (la ViewModel valida).
 * @param onAtras toque en ← Atrás → vuelve al Paso 1.
 * @param onCargarImagen toque en **🖼 Cargar imagen de la cuenta** (FASE 4):
 *   abre el selector de imágenes del sistema.
 * @param ocrCandidatos números leídos de la foto cuando hubo varios (`vacío`
 *   = sin selección en curso → la sección no se pinta).
 * @param onCandidatoElegido toque sobre un candidato → pasa a ser la cantidad.
 * @param onCerrarCandidatos toque en **✍️ Escribir a mano** → cierra los
 *   candidatos y la persona escribe con el teclado propio.
 */
@Composable
fun EnterQuantityScreen(
    quantityInput: String,
    producto: ProductType?,
    quantityError: Boolean,
    isSaving: Boolean,
    ocrMessage: String?,
    savingError: String?,
    onDigito: (String) -> Unit,
    onBorrarDigito: () -> Unit,
    onEscanear: () -> Unit,
    onSiguiente: () -> Unit,
    onAtras: () -> Unit,
    onCargarImagen: () -> Unit = {},
    ocrCandidatos: List<Int> = emptyList(),
    onCandidatoElegido: (Int) -> Unit = {},
    onCerrarCandidatos: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(step = 2, onBack = onAtras, backEnabled = !isSaving)

        Text(
            text = "¿Cuántos?",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
        )

        // Número gigante: 72sp (dentro del rango sénior de cifras 40–80sp).
        Text(
            text = quantityInput,
            fontSize = 72.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        if (producto != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = producto.emoji, fontSize = 40.sp)
                Text(
                    text = producto.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            }
        }

        // Mensaje del OCR: recuadro suave que NO sustituye el número escrito.
        if (ocrMessage != null) {
            RecuadroAviso(texto = ocrMessage)
        }

        // Error proveniente de la ViewModel (p. ej. "no pude deshacer").
        if (savingError != null) {
            RecuadroAviso(texto = savingError, enError = true)
        }

        // FASE 4: candidatos leídos de la FOTO — panel sobre el teclado. Al
        // elegir, el número grande de arriba cambia a la vista de la persona;
        // SEGUIR y el teclado siguen abajo accesibles con scroll.
        if (ocrCandidatos.isNotEmpty()) {
            CandidatosNumeros(
                ocrCandidatos = ocrCandidatos,
                onElegido = onCandidatoElegido,
                onCerrar = onCerrarCandidatos,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Teclado numérico PROPIO (9 dígitos + Borrar + 0): jamás el del sistema.
        Keypad(
            onDigit = onDigito,
            onDelete = onBorrarDigito,
            showDoubleZero = false,
            modifier = Modifier.fillMaxWidth(),
        )

        // Botón OCR OPCIONAL: MainActivity abre CameraScanScreen (preview +
        // ImageAnalysis + NumberRecognitionAnalyzer) y devuelve el resultado a
        // InventoryViewModel.onEscanearResultado(numero). Si la cámara falla o no
        // lee nada, la VM muestra MENSAJE_OCR_FALLIDO sin tocar lo ya escrito.
        SeniorButton(
            text = "📷 Escanear con la cámara",
            onClick = onEscanear,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            variant = SeniorButtonVariant.OUTLINE,
        )

        // FASE 4: segunda venta opcional — cargar una FOTO de la cuenta.
        // MainActivity abre el Photo Picker (PickVisualMedia, SIN permisos),
        // lee la imagen con camera/ImageNumberOcr y entrega los números a
        // InventoryViewModel.onImagenLeida (1 → cantidad; varios → candidatos;
        // vacío → MENSAJE_IMAGEN_FALLIDO vía ocrMessage, pintado igual que arriba).
        SeniorButton(
            text = "🖼 Cargar imagen de la cuenta",
            onClick = onCargarImagen,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            variant = SeniorButtonVariant.OUTLINE,
            contentDescription = "Cargar una imagen de tu cuenta para leer los números",
        )

        if (quantityError) {
            Text(
                text = "Primero escribe cuántos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = RedAction,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SeniorButton(
            text = "SEGUIR →",
            onClick = onSiguiente,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            variant = SeniorButtonVariant.GREEN,
            enabled = !quantityError && !isSaving,
        )
    }
}

/**
 * Recuadro de aviso del Paso 2: fondo suave (o rojo suave si [enError]) con
 * texto de 22sp. Se usa para el mensaje del OCR y para los errores de la
 * ViewModel, sin tocar nunca el número que escribió el usuario.
 */
@Composable
private fun RecuadroAviso(
    texto: String,
    enError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fondo: Color = if (enError) RedActionContainer else SurfaceSoft
    val colorTexto: Color = if (enError) RedAction else TextPrimary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = fondo,
    ) {
        Text(
            text = texto,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorTexto,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}
