package com.bioplast.insumos.ui

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bioplast.insumos.camera.CameraPermissionGate
import com.bioplast.insumos.camera.NumberRecognitionAnalyzer
import com.bioplast.insumos.ui.components.SeniorButton
import com.bioplast.insumos.ui.components.SeniorButtonVariant
import com.bioplast.insumos.ui.theme.AppBackground
import com.bioplast.insumos.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Tag de log de esta pantalla (fallos de apertura de cámara). */
private const val TAG = "CameraScanScreen"

/** TIMEOUT ~8 s: sin número detectado en ese tiempo se cierra con `null`. */
private const val TIMEOUT_MILLIS = 8_000L

/** Alpha del overlay oscuro semi-transparente sobre el preview. */
private const val OVERLAY_ALPHA = 0.5f

/** Marco guía grande: alto y margen lateral (área donde apuntar al número). */
private val MARCO_ALTO = 240.dp
private val MARCO_PADDING_HORIZONTAL = 32.dp

/** Mensaje cuando la persona deniega el permiso de cámara. */
private const val MENSAJE_SIN_PERMISO =
    "Necesito la cámara para escanear. Puedes escribir el número a mano."

/** Mensaje cuando el dispositivo no pudo abrir la cámara (fallo técnico). */
private const val MENSAJE_SIN_CAMARA =
    "No pude abrir la cámara. Puedes escribir el número a mano."

/**
 * Pantalla a pantalla completa de escaneo OCR (botón 📷 del Paso 2).
 *
 * No participa en la navegación del flujo de 7 pasos: la decide
 * `InventoryUiState.screen`; esta pantalla se superpone desde MainActivity con
 * estado local y al cerrarse vuelve a pintarse `EnterQuantityScreen`.
 *
 * - [CameraPermissionGate] → sin permiso: pantalla amable con "Volver"
 *   → [onResultado](null).
 * - Con permiso: `Preview` (CameraX + `AndroidView(PreviewView)`) e
 *   `ImageAnalysis` con `STRATEGY_KEEP_ONLY_LATEST` alimentando a
 *   [NumberRecognitionAnalyzer]; `close()` + `unbindAll()` son **obligatorios** y
 *   ocurren en `DisposableEffect.onDispose`.
 * - Overlay oscuro semi-transparente, marco guía grande y texto 22sp
 *   "Apunta al número"; botón **Volver** (GREY, 64dp) arriba.
 * - Cierre: detección de un número (`Int != null`) → [onResultado](numero) en el
 *   acto; o timeout de ~8 s, o Volver, o permiso denegado/fallo de cámara →
 *   [onResultado](null). El `null` inicial del preview **no** cierra nada.
 * - [onResultado] se invoca **exactamente una vez** por sesión de escaneo.
 *
 * El mensaje de fallo ("No pude leer el número. Escríbelo tú mismo.") lo pinta la
 * ViewModel en `ocrMessage` vía `InventoryViewModel.onEscanearResultado(null)`.
 */
@Composable
fun CameraScanScreen(
    onResultado: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Callback siempre vigente + garantía de UNA sola entrega por sesión.
    val callbackActual = rememberUpdatedState(onResultado)
    val yaEntrego = remember { mutableStateOf(false) }
    val entregar: (Int?) -> Unit = { numero ->
        if (!yaEntrego.value) {
            yaEntrego.value = true
            callbackActual.value(numero)
        }
    }

    var permisoDenegado by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CameraPermissionGate(
            onGranted = {
                CamaraEnVivo(
                    entregar = entregar,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            onDenied = { permisoDenegado = true },
        )

        // Denegado: estado amable; el gate no compone nada en este caso.
        if (permisoDenegado) {
            PantallaSinCamara(
                mensaje = MENSAJE_SIN_PERMISO,
                onVolver = { entregar(null) },
            )
        }
    }
}

/**
 * Rama "con permiso": preview en vivo + analyzer + overlay + timeout de 8 s.
 */
@Composable
private fun CamaraEnVivo(
    entregar: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    // Executor dedicado: el análisis corre legejos del hilo principal.
    val executor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        // Nota: el lambda va con nombre porque cooldownMs es el último parámetro
        // del constructor (la sintaxis de trailing lambda no aplica aquí).
        NumberRecognitionAnalyzer(onNumberDetected = { numero ->
            // Solo un número REAL cierra la pantalla: los null iniciales del
            // preview se ignoran aquí (el null de cierre lo decide el timeout
            // o el botón Volver).
            if (numero != null) entregar(numero)
        })
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }
    }
    val preview = remember { Preview.Builder().build() }

    // Evita bindear tras salir (la suspensión del proveedor puede resumirse tarde).
    val sesionActiva = remember { mutableStateOf(true) }
    var proveedor by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var falloCamara by remember { mutableStateOf(false) }

    // TIMEOUT ~8 s sin detección → cerrar con null.
    LaunchedEffect(Unit) {
        delay(TIMEOUT_MILLIS)
        entregar(null)
    }

    // Une Preview + ImageAnalysis a la cámara trasera del ciclo de vida.
    LaunchedEffect(Unit) {
        try {
            val cameraProvider = context.cameraProvider()
            if (!sesionActiva.value) return@LaunchedEffect
            proveedor = cameraProvider
            cameraProvider.unbindAll()
            preview.setSurfaceProvider(previewView.surfaceProvider)
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis,
            )
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo abrir la cámara", e)
            if (sesionActiva.value) falloCamara = true
        }
    }

    // OBLIGATORIO: al salir de la pantalla se libera todo (cámara, recognizer,
    // executor). Sin esto la luz/cámara quedaría encendida y el recognizer filtrado.
    DisposableEffect(Unit) {
        onDispose {
            sesionActiva.value = false
            runCatching { proveedor?.unbindAll() }
            runCatching { preview.setSurfaceProvider(null) }
            analyzer.close()
            executor.shutdown()
        }
    }

    if (falloCamara) {
        PantallaSinCamara(
            mensaje = MENSAJE_SIN_CAMARA,
            onVolver = { entregar(null) },
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Vista previa en vivo.
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )

            // Overlay oscuro semi-transparente + marco guía + instrucción.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = OVERLAY_ALPHA))
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MARCO_PADDING_HORIZONTAL)
                        .height(MARCO_ALTO)
                        .border(
                            width = 4.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(24.dp),
                        )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Apunta al número",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    // Sombra: legibilidad sobre fondos de preview muy claros.
                    style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 10f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                )
            }

            // Volver (GREY, 64dp) arriba, siempre visible.
            SeniorButton(
                text = "Volver",
                onClick = { entregar(null) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .heightIn(min = 64.dp),
                variant = SeniorButtonVariant.GREY,
            )
        }
    }
}

/**
 * Estado amable a pantalla completa: sin permiso o sin cámara. Fondo claro del
 * tema + mensaje grande + botón Volver (GREY, ≥64dp) que cierra con `null`.
 */
@Composable
private fun PantallaSinCamara(
    mensaje: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = mensaje,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        SeniorButton(
            text = "Volver",
            onClick = onVolver,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            variant = SeniorButtonVariant.GREY,
        )
    }
}

/** Espera la instancia de [ProcessCameraProvider] sin bloquear el hilo. */
private suspend fun Context.cameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }
