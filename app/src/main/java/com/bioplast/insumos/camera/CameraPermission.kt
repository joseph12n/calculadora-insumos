package com.bioplast.insumos.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permiso que la app solo necesita para el OCR opcional ("📷 Escanear con la cámara").
 * Ya está declarado en `AndroidManifest.xml`.
 */
const val CAMERA_PERMISSION: String = Manifest.permission.CAMERA

/** true si [CAMERA_PERMISSION] ya fue concedida (chequeo síncrono, sin lanzar diálogo). */
fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Compuerta de permiso de cámara para la pantalla de escaneo, sin ViewModel.
 *
 * - Si el permiso ya está concedido, compone [onGranted] de inmediato (cámara lista).
 * - Si no, lanza el diálogo del sistema una sola vez; al conceder compone [onGranted].
 * - Si se deniega, llama a [onDenied] **una sola vez** y no compone nada: la pantalla
 *   madre muestra el estado de error (ej. "La cámara no está disponible. Puedes
 *   escribir la cantidad tú mismo.") y puede seguir usando el teclado manual.
 *
 * Reintentar: basta con que la pantalla vuelva a componer la compuerta (p. ej. al
 * reentrar en el Paso 2); si el permiso fue denegado "permanentemente", el sistema
 * responde denegado al instante y vuelve a llamar a [onDenied].
 */
@Composable
fun CameraPermissionGate(
    onGranted: @Composable () -> Unit,
    onDenied: () -> Unit,
) {
    val context = LocalContext.current
    var concedido by remember { mutableStateOf(hasCameraPermission(context)) }
    // Si ya estaba concedido, no hace falta lanzar la solicitud.
    var solicitudLanzada by remember { mutableStateOf(concedido) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        concedido = otorgado
        if (!otorgado) onDenied()
    }

    LaunchedEffect(Unit) {
        if (!concedido && !solicitudLanzada) {
            solicitudLanzada = true
            launcher.launch(CAMERA_PERMISSION)
        }
    }

    if (concedido) {
        onGranted()
    }
}
