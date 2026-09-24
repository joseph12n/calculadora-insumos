package com.bioplast.insumos

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bioplast.insumos.camera.borrarFotoTemporal
import com.bioplast.insumos.camera.crearUriParaFoto
import com.bioplast.insumos.camera.leerNumerosDeFoto
import com.bioplast.insumos.model.Screen
import com.bioplast.insumos.ui.CalculatorScreen
import com.bioplast.insumos.ui.ConfirmScreen
import com.bioplast.insumos.ui.DayDetailScreen
import com.bioplast.insumos.ui.HistoryScreen
import com.bioplast.insumos.ui.StartScreen
import com.bioplast.insumos.ui.SuccessScreen
import com.bioplast.insumos.ui.theme.CalculadoraInsumosTheme
import com.bioplast.insumos.ui.theme.GreenAction
import com.bioplast.insumos.ui.theme.TextPrimary
import com.bioplast.insumos.ui.texto.ConEscalaDeTexto

/**
 * Single-activity. Toda la navegación ocurre dentro de Compose.
 *
 * NO usa Navigation Compose: el destino lo decide [InventoryUiState.screen]
 * (single source of truth). La UI solo emite eventos a la [InventoryViewModel]
 * y pinta el estado: cada pantalla vive en su propio archivo bajo
 * `com.bioplast.insumos.ui`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalculadoraDeInsumosApp()
        }
    }
}

/**
 * Raíz del flujo: Inicio → Calculadora → Revisar → ¡Guardado!, más Historial y
 * Detalle de la semana.
 *
 * - [InventoryViewModel] única para toda la sesión (`uiState` colectado con
 *   ciclo de vida vía `collectAsStateWithLifecycle`); NUNCA se crean instancias
 *   a mano — el botón "Intentar de nuevo" llama a
 *   [InventoryViewModel.onReintentar], que limpia `dataError`/`loading` y
 *   re-suscribe los Flujos de Room dentro de esta misma instancia.
 * - `loading` → indicador grande mientras Room no emite su primer resumen.
 * - **Foto de la hoja**: un solo botón en la calculadora abre la cámara del
 *   sistema (`ActivityResultContracts.TakePicture`, SIN permiso de cámara).
 *   Al volver, la foto se lee con ML Kit y sus números se ofrecen para elegir.
 * - **Back del sistema**: `BackHandler` (dentro de [ConEscalaDeTexto]) mapeja
 *   el botón físico/a-gestos a los métodos de la ViewModel para no perder el
 *   lote; en START con productos en la lista el back queda absorbido a propósito.
 */
@Composable
private fun CalculadoraDeInsumosApp() {
    val context = LocalContext.current
    val factory = remember { InventoryViewModel.factory(context.applicationContext) }

    val viewModel: InventoryViewModel = viewModel(factory = factory)
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    CalculadoraInsumosTheme {
        // Tamaño de letra ajustable (A−/A+ del Inicio): lee la escala persistida
        // y la provee vía LocalDensity para TODO el árbol, dentro del tema.
        ConEscalaDeTexto {
            // --- Back del sistema (raíz de TODO el flujo) -------------------
            BackHandler(enabled = estado.screen != Screen.START) {
                when (estado.screen) {
                    Screen.HISTORY -> viewModel.onVolverDeHistorial()
                    Screen.SUCCESS -> viewModel.onListo()
                    // CALCULATOR / CONFIRM / DAY_DETAIL
                    else -> viewModel.onAtras()
                }
            }
            // En INICIO con la lista empezada el back NO cierra la app (se
            // perdería el lote); la tarjeta "Tienes N productos en tu lista"
            // con CONTINUAR LISTA es el recuerdo visible de esa decisión.
            BackHandler(
                enabled = estado.screen == Screen.START &&
                    estado.itemsPendientes.isNotEmpty(),
            ) { /* no-op deliberado: protege el lote sin guardar */ }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
            when {
                estado.loading -> LoadingIndicator()

                else -> when (estado.screen) {
                    // ---------------------------------------------------------
                    // 1) INICIO
                    // ---------------------------------------------------------
                    Screen.START -> StartScreen(
                        semanaActual = estado.semanaActual,
                        totalCop = estado.weeklySummary?.totalCop ?: 0.0,
                        totalInsumos = estado.weeklySummary?.totalQuantity ?: 0L,
                        productosEnLista = estado.itemsPendientes.size,
                        loading = estado.loading,
                        dataError = estado.dataError,
                        // onContar: empezar un registro nuevo → SIEMPRE la
                        // calculadora (aunque haya producto/cantidad
                        // preservados de una cadena de "Atrás").
                        onContar = { viewModel.onEmpezarConteo() },
                        onContinuarLista = { viewModel.onContinuarLote() },
                        onVerRegistros = { viewModel.onAbrirHistorial() },
                        // Reintento DENTRO de la misma ViewModel (sin crear
                        // instancias zombi fuera del ViewModelStore).
                        onReintentar = { viewModel.onReintentar() },
                    )

                    // ---------------------------------------------------------
                    // 2) CALCULADORA — insumo + cantidad + total en vivo
                    // ---------------------------------------------------------
                    Screen.CALCULATOR -> {
                        // FASE FOTO: estado LOCAL de UI. Un solo botón abre la
                        // cámara del sistema; la URI la crea el FileProvider de
                        // la app (sin permiso de cámara) y al volver se lee.
                        var procesandoFoto by rememberSaveable { mutableStateOf(false) }
                        var uriFoto by rememberSaveable { mutableStateOf<Uri?>(null) }

                        // Recorte de la letra de cada número leído (para que la
                        // persona compare antes de confirmar). Vive solo en la UI:
                        // los Bitmap no van al estado de la ViewModel.
                        var recortesFoto by remember {
                            mutableStateOf<Map<Int, android.graphics.Bitmap>>(emptyMap())
                        }

                        val camara = rememberLauncherForActivityResult(
                            ActivityResultContracts.TakePicture()
                        ) { exito ->
                            if (exito) {
                                procesandoFoto = true
                            } else {
                                // La persona canceló: se borra la foto temporal vacía.
                                uriFoto?.let { borrarFotoTemporal(context, it) }
                                uriFoto = null
                                procesandoFoto = false
                            }
                        }

                        // Lectura RECURSIVA de la foto (varias pasadas + recortes)
                        // en UI y entrega a la ViewModel: con números → diálogo de
                        // confirmación mostrando el recorte de la letra; vacío →
                        // MENSAJE_IMAGEN_FALLIDO.
                        val uriEnProceso = uriFoto
                        LaunchedEffect(procesandoFoto, uriEnProceso) {
                            if (procesandoFoto && uriEnProceso != null) {
                                val candidatos = leerNumerosDeFoto(context, uriEnProceso)
                                recortesFoto = candidatos
                                    .mapNotNull { c -> c.recorte?.let { c.numero to it } }
                                    .toMap()
                                viewModel.onImagenLeida(candidatos.map { it.numero })
                                borrarFotoTemporal(context, uriEnProceso)
                                uriFoto = null
                                procesandoFoto = false
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            CalculatorScreen(
                                quantityInput = estado.quantityInput,
                                producto = estado.selectedProduct,
                                quantityError = estado.quantityError,
                                productError = estado.productError,
                                totalCop = estado.totalActualCop,
                                isSaving = estado.isSaving,
                                ocrMessage = estado.ocrMessage,
                                savingError = estado.savingError,
                                onDigito = { viewModel.onDigito(it) },
                                onBorrarDigito = { viewModel.onBorrarDigito() },
                                onProductoElegido = { viewModel.onProductoElegido(it) },
                                onHacerFoto = {
                                    // Sin cámara disponible: se avisa con el
                                    // mensaje de lectura fallida (nunca crashea).
                                    recortesFoto = emptyMap()
                                    runCatching {
                                        val uri = crearUriParaFoto(context)
                                        uriFoto = uri
                                        camara.launch(uri)
                                    }.onFailure {
                                        viewModel.onImagenLeida(emptyList())
                                        uriFoto?.let { borrarFotoTemporal(context, it) }
                                        uriFoto = null
                                        procesandoFoto = false
                                    }
                                },
                                onSiguiente = { viewModel.onSiguiente() },
                                onAtras = { viewModel.onAtras() },
                                ocrCandidatos = estado.ocrCandidatos,
                                onCandidatoElegido = { viewModel.onCandidatoElegido(it) },
                                onCerrarCandidatos = {
                                    viewModel.onCerrarCandidatos()
                                    recortesFoto = emptyMap()
                                },
                                recortesCandidatos = recortesFoto,
                            )

                            // Overlay de carga mientras se lee la foto:
                            // fondo claro semitransparente, sin toques ni gestos.
                            if (procesandoFoto) {
                                OverlayLeyendoFoto(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // 3) REVISAR — item actual + lote + fecha + guardar
                    // ---------------------------------------------------------
                    Screen.CONFIRM -> ConfirmScreen(
                        cantidad = estado.cantidad,
                        producto = estado.selectedProduct,
                        totalCop = estado.totalActualCop,
                        selectedDate = estado.selectedDate,
                        itemsPendientes = estado.itemsPendientes,
                        cantidadLote = estado.cantidadLote,
                        totalLoteCop = estado.totalLoteCop,
                        isSaving = estado.isSaving,
                        savingError = estado.savingError,
                        // GUARDAR TODO: guarda pendientes + item actual (lote).
                        onGuardar = { viewModel.onConfirmarGuardar() },
                        onAgregarOtro = { viewModel.onAgregarOtro() },
                        onQuitarPendiente = { posicion -> viewModel.onQuitarPendiente(posicion) },
                        onFechaDePendiente = { posicion, fecha ->
                            viewModel.onFechaDePendiente(posicion, fecha)
                        },
                        onAtras = { viewModel.onAtras() },
                        onFecha = { viewModel.onFechaElegida(it) },
                    )

                    // ---------------------------------------------------------
                    // 4) ¡LISTO! (ventana de deshacer ~5 s)
                    // ---------------------------------------------------------
                    Screen.SUCCESS -> SuccessScreen(
                        lastSavedRecords = estado.lastSavedRecords,
                        cantidadLote = estado.cantidadLote,
                        totalLoteCop = estado.totalLoteCop,
                        canUndo = estado.canUndo,
                        // DESHACER borra el lote y RESTAURA la lista en el paso de revisión.
                        onDeshacer = { viewModel.onDeshacer() },
                        onListo = { viewModel.onListo() },
                    )

                    // ---------------------------------------------------------
                    // 5) HISTORIAL
                    // ---------------------------------------------------------
                    Screen.HISTORY -> HistoryScreen(
                        resumenes = estado.weeklySummaries,
                        onElegirSemana = { viewModel.onElegirSemana(it) },
                        // onVolverDeHistorial además limpia la semana elegida
                        // y su desglose al salir del historial.
                        onAtras = { viewModel.onVolverDeHistorial() },
                    )

                    // ---------------------------------------------------------
                    // 6) DETALLE DE LA SEMANA (borrado por fila)
                    // ---------------------------------------------------------
                    Screen.DAY_DETAIL -> DayDetailScreen(
                        semana = estado.selectedWeek,
                        registros = estado.recordsForSelectedWeek,
                        onEliminarRegistro = { viewModel.onEliminarRegistro(it) },
                        onAtras = { viewModel.onAtras() },
                        // Un borrado fallido (MENSAJE_BORRAR_FALLIDO) se PINTA:
                        // nunca se pierde un error en silencio.
                        savingError = estado.savingError,
                    )
                }
            }
                }
            }
        }
}

/** Indicador de carga grande y legible (primer frame mientras Room emite). */
@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
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

/**
 * Overlay de carga mientras se lee la foto: fondo claro semitransparente con
 * texto grande "Leyendo tu foto..." + indicador verde.
 *
 * NO define toques ni gestos para la persona: consume los punteros en el pass
 * inicial para que nadie toque la pantalla mientras se lee la foto (sin dobles
 * toques ni acciones accidentales). TalkBack anuncia el texto por su liveRegion.
 */
@Composable
private fun OverlayLeyendoFoto(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = Color.White.copy(alpha = 0.88f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val eventos = awaitPointerEvent(PointerEventPass.Initial)
                        eventos.changes.forEach { cambio -> cambio.consume() }
                    }
                }
            },
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
                text = "Leyendo tu foto...",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
