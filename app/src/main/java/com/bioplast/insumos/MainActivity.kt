package com.bioplast.insumos

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
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
import com.bioplast.insumos.camera.extraerNumerosDeImagen
import com.bioplast.insumos.model.Screen
import com.bioplast.insumos.ui.CameraScanScreen
import com.bioplast.insumos.ui.ConfirmScreen
import com.bioplast.insumos.ui.DayDetailScreen
import com.bioplast.insumos.ui.EnterQuantityScreen
import com.bioplast.insumos.ui.HistoryScreen
import com.bioplast.insumos.ui.PickProductScreen
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
 * Raíz del flujo de 7 pantallas.
 *
 * - [InventoryViewModel] única para toda la sesión (`uiState` colectado con
 *   ciclo de vida vía `collectAsStateWithLifecycle`); NUNCA se crean instancias
 *   a mano — el botón "Intentar de nuevo" llama a
 *   [InventoryViewModel.onReintentar], que limpia `dataError`/`loading` y
 *   re-suscribe los Flujos de Room dentro de esta misma instancia.
 * - `loading` → indicador grande mientras Room no emite su primer resumen.
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
            // Mapea el back a los MISMOS caminos que los botones "← Atrás" de
            // pantalla: nunca sale de la app ni pierde el lote sin confirmar.
            BackHandler(enabled = estado.screen != Screen.START) {
                when (estado.screen) {
                    Screen.HISTORY -> viewModel.onVolverDeHistorial()
                    Screen.SUCCESS -> viewModel.onListo()
                    // PICK / ENTER_QUANTITY / CONFIRM / DAY_DETAIL
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

            // Surface del tema ya envuelve el contenido; safeDrawingPadding evita
            // que las barras del sistema tapen botones con edge-to-edge activado.
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
                        // onContar: "empezar un registro nuevo" → SIEMPRE el
                        // Paso 1 (PICK_PRODUCT), aunque haya producto/cantidad
                        // preservados de una cadena de "Atrás" (bug: onSiguiente
                        // con datos válidos saltaba directo al Paso 3).
                        onContar = { viewModel.onEmpezarConteo() },
                        onContinuarLista = { viewModel.onContinuarLote() },
                        onVerRegistros = { viewModel.onAbrirHistorial() },
                        // Reintento DENTRO de la misma ViewModel (sin crear
                        // instancias zombi fuera del ViewModelStore).
                        onReintentar = { viewModel.onReintentar() },
                    )

                    // ---------------------------------------------------------
                    // 2) PASO 1 — ¿Qué vas a contar?
                    // ---------------------------------------------------------
                    Screen.PICK_PRODUCT -> PickProductScreen(
                        onProductoElegido = { viewModel.onProductoElegido(it) },
                        onAtras = { viewModel.onAtras() },
                    )

                    // ---------------------------------------------------------
                    // 3) PASO 2 — ¿Cuántos?
                    // ---------------------------------------------------------
                    Screen.ENTER_QUANTITY -> {
                        // Estado LOCAL de UI (no forma parte de InventoryUiState):
                        // la navegación sigue siendo estado.screen; la cámara y la
                        // imagen se superponen al Paso 2 y al cerrar vuelve a
                        // pintarse EnterQuantityScreen con el resultado ya aplicado.
                        var mostrandoCamara by rememberSaveable { mutableStateOf(false) }

                        // --- FASE 4: cargar imagen de la cuenta ------------------
                        // Photo Picker (PickVisualMedia): NO requiere permisos.
                        var procesandoImagen by rememberSaveable { mutableStateOf(false) }
                        var uriPendiente by rememberSaveable { mutableStateOf<Uri?>(null) }

                        val selectorDeImagen = rememberLauncherForActivityResult(
                            ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) {
                                procesandoImagen = true
                                uriPendiente = uri
                            }
                        }

                        // Lectura de la foto en UI (LaunchedEffect suspend directo,
                        // NO en viewModelScope) y entrega a la ViewModel:
                        //   1 número → llena quantityInput;
                        //   varios   → ocrCandidatos (selección en la pantalla);
                        //   vacío   → MENSAJE_IMAGEN_FALLIDO en ocrMessage.
                        val uriEnProceso = uriPendiente
                        LaunchedEffect(procesandoImagen, uriEnProceso) {
                            if (procesandoImagen && uriEnProceso != null) {
                                val numeros = extraerNumerosDeImagen(context, uriEnProceso)
                                viewModel.onImagenLeida(numeros)
                                uriPendiente = null
                                procesandoImagen = false
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (mostrandoCamara) {
                                CameraScanScreen(
                                    // numero != null → sobreescribe la cantidad;
                                    // numero == null → "No pude leer el número…"
                                    // conservando intacto lo ya escrito (contrato de
                                    // InventoryViewModel.onEscanearResultado).
                                    onResultado = { numero ->
                                        viewModel.onEscanearResultado(numero)
                                        mostrandoCamara = false
                                    },
                                )
                            } else {
                                EnterQuantityScreen(
                                    quantityInput = estado.quantityInput,
                                    producto = estado.selectedProduct,
                                    quantityError = estado.quantityError,
                                    isSaving = estado.isSaving,
                                    ocrMessage = estado.ocrMessage,
                                    savingError = estado.savingError,
                                    onDigito = { viewModel.onDigito(it) },
                                    onBorrarDigito = { viewModel.onBorrarDigito() },
                                    onEscanear = { mostrandoCamara = true },
                                    onSiguiente = { viewModel.onSiguiente() },
                                    onAtras = { viewModel.onAtras() },
                                    // FASE 4: imagen de la cuenta → Photo Picker.
                                    onCargarImagen = {
                                        selectorDeImagen.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    ocrCandidatos = estado.ocrCandidatos,
                                    onCandidatoElegido = { viewModel.onCandidatoElegido(it) },
                                    onCerrarCandidatos = { viewModel.onCerrarCandidatos() },
                                )
                            }

                            // Overlay de carga mientras se lee la foto:
                            // fondo claro semitransparente, sin toques ni gestos.
                            if (procesandoImagen) {
                                OverlayLeyendoImagen(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // 4) PASO 3 — Revisa que esté bien
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
                    // 5) ¡LISTO! (ventana de deshacer ~5 s)
                    // ---------------------------------------------------------
                    Screen.SUCCESS -> SuccessScreen(
                        lastSavedRecords = estado.lastSavedRecords,
                        cantidadLote = estado.cantidadLote,
                        totalLoteCop = estado.totalLoteCop,
                        canUndo = estado.canUndo,
                        // DESHACER borra el lote y RESTAURA la lista en el Paso 3.
                        onDeshacer = { viewModel.onDeshacer() },
                        onListo = { viewModel.onListo() },
                    )

                    // ---------------------------------------------------------
                    // 6) HISTORIAL
                    // ---------------------------------------------------------
                    Screen.HISTORY -> HistoryScreen(
                        resumenes = estado.weeklySummaries,
                        onElegirSemana = { viewModel.onElegirSemana(it) },
                        // onVolverDeHistorial además limpia la semana elegida
                        // y su desglose al salir del historial.
                        onAtras = { viewModel.onVolverDeHistorial() },
                    )

                    // ---------------------------------------------------------
                    // 7) DETALLE DE LA SEMANA (borrado por fila)
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
 * Overlay de carga FASE 4: fondo claro semitransparente con texto grande
 * "Leyendo tu imagen..." + indicador verde.
 *
 * NO define toques ni gestos para la persona: consume los punteres en el pass
 * inicial para que nadie toque la pantalla mientras se lee la foto (sin dobles
 * toques ni acciones accidentales). TalkBack anuncia el texto por su liveRegion.
 */
@Composable
private fun OverlayLeyendoImagen(modifier: Modifier = Modifier) {
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
                text = "Leyendo tu imagen...",
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
