package com.bioplast.insumos

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bioplast.insumos.camera.MAXIMO_CANDIDATOS_IMAGEN
import com.bioplast.insumos.data.InventoryRepository
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.model.Screen
import com.bioplast.insumos.model.WeeklySummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/** Año con calendario de la semana del locale por defecto ([WeekFields.weekBasedYear]). */
private fun anioIsoDe(fecha: LocalDate): Int =
    fecha.get(WeekFields.of(Locale.getDefault()).weekBasedYear())

/** Número de semana (1..53) según el locale por defecto ([WeekFields.weekOfWeekBasedYear]). */
private fun semanaIsoDe(fecha: LocalDate): Int =
    fecha.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())

/**
 * Estado único (UDF) que consume la UI para dibujar las pantallas.
 *
 * Es un `data class` inmutable: la ViewModel nunca lo muta, solo emite copias vía
 * `MutableStateFlow.update`. Los campos de sesión (`selectedProduct`, `quantityInput`,
 * errores) se limpian al volver a [Screen.START]; los campos reactivos (`weeklySummary`,
 * `weeklySummaries`, `recordsForSelectedWeek`) vienen de Room y nunca se tocan a mano.
 *
 * @property screen Pantalla actual del flujo.
 * @property selectedProduct Insumo elegido en la calculadora; `null` mientras no se elija.
 * @property quantityInput Dígitos escritos en la calculadora; `"0"` por defecto (String para
 *   conservar el tecleo tal cual; leer la cantidad con [cantidad]).
 * @property itemsPendientes Lote en memoria de la sesión: drafts (`id = 0`) con
 *   fecha/semana/año ya calculados, en orden de guardado. Se arma en CONFIRM con
 *   [InventoryViewModel.onAgregarOtro] y solo se persiste en [InventoryViewModel.onConfirmarGuardar].
 * @property lastSavedRecords Registros del lote recién guardado en [Screen.SUCCESS], con
 *   sus ids REALES (los borra [InventoryViewModel.onDeshacer]; ver también el getter de
 *   compatibilidad [lastSavedRecord]).
 * @property canUndo `true` solo dentro de la ventana de ~5 s de [Screen.SUCCESS].
 * @property weeklySummary Acumulado de la SEMANA ACTUAL. `null` = semana sin registros
 *   → la UI debe mostrar `$0` y `0` insumos (el número de semana sale de [semanaActual]).
 * @property weeklySummaries Historial de todas las semanas (de la más reciente a la más antigua).
 * @property recordsForSelectedWeek Desglose de la semana elegida en el historial
 *   (se rellena solo al llamar [InventoryViewModel.onElegirSemana]).
 * @property selectedDate Fecha que llevará el registro; por defecto hoy
 *   (la revisión ofrece el selector de fecha; ver [InventoryViewModel.onFechaElegida]).
 * @property quantityError `true` → la calculadora muestra el aviso rojo "Primero
 *   escribe cuántos". Se pone en [InventoryViewModel.onSiguiente] si cantidad < 1 y
 *   se borra al escribir/borrar dígitos. SEGUIR sigue tocable para volver a validar.
 * @property productError `true` → la calculadora pide elegir el insumo (aviso rojo) y no
 *   avanza. Se pone en [InventoryViewModel.onSiguiente]/[InventoryViewModel.onConfirmarGuardar]
 *   si no hay producto y se borra al elegirlo o al volver.
 * @property savingError Mensaje de error de persistencia en lenguaje cotidiano (`null` = sin error).
 * @property selectedWeek Semana seleccionada en el historial (`null` fuera de [Screen.DAY_DETAIL]).
 * @property ocrMessage Mensaje cuando la lectura de la FOTO falla (`null` = sin fallas).
 *   Nunca borra [quantityInput]: la UI lo muestra junto al visor para que la persona
 *   escriba a mano. Lo pinta `MENSAJE_IMAGEN_FALLIDO`.
 * @property ocrCandidatos Números leídos de una FOTO cuando hubo VARIOS: la UI muestra
 *   un diálogo para que la persona elija ([InventoryViewModel.onCandidatoElegido]); vacío
 *   cuando no hay selección de candidatos en curso ([InventoryViewModel.onCerrarCandidatos]).
 * @property loading `true` mientras no llega la primera emisión de Room (primer frame de carga).
 * @property dataError `true` si la base de datos local no está disponible (no se pudieron
 *   cargar los flujos). Señal para mostrar un estado de error reintentable en la UI.
 * @property isSaving `true` mientras se persiste el registro (revisión): la UI deshabilita
 *   botones para evitar dobles toques.
 * @property anioActual Año (semana-local) de hoy, para la tarjeta de resumen.
 * @property semanaActual Número de semana de hoy, para la tarjeta de resumen.
 */
data class InventoryUiState(
    val screen: Screen = Screen.START,
    val selectedProduct: ProductType? = null,
    val quantityInput: String = "0",
    val canUndo: Boolean = false,
    val weeklySummary: WeeklySummary? = null,
    val weeklySummaries: List<WeeklySummary> = emptyList(),
    val recordsForSelectedWeek: List<InventoryRecord> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val quantityError: Boolean = false,
    val productError: Boolean = false,
    val savingError: String? = null,
    val selectedWeek: WeeklySummary? = null,
    val ocrMessage: String? = null,
    val ocrCandidatos: List<Int> = emptyList(),
    val loading: Boolean = true,
    val dataError: Boolean = false,
    val isSaving: Boolean = false,
    val anioActual: Int = anioIsoDe(LocalDate.now()),
    val semanaActual: Int = semanaIsoDe(LocalDate.now()),
    val itemsPendientes: List<InventoryRecord> = emptyList(),
    val lastSavedRecords: List<InventoryRecord> = emptyList(),
) {
    /** Cantidad actual interpretada de [quantityInput] (`0` si no es un entero válido). */
    val cantidad: Int
        get() = quantityInput.toIntOrNull() ?: 0

    /** Total vivo = cantidad × precio unitario, SIN redondear; para mostrar la UI usa
     * `CurrencyFormat.formatConDecimalesRedondeados` (de `com.bioplast.insumos.model`). */
    val totalActualCop: Double
        get() = selectedProduct?.totalPara(cantidad) ?: 0.0

    /** Total del lote: pendientes + el item que se está capturando ahora (sin redondear). */
    val totalLoteCop: Double
        get() = itemsPendientes.sumOf { it.totalCop } + totalActualCop

    /** Insumos totales del lote: pendientes + el item actual. */
    val cantidadLote: Int
        get() = itemsPendientes.sumOf { it.quantity } + cantidad

    /** COMPAT: no es parámetro del constructor. Último registro del lote guardado (el de
     * siempre cuando el lote fue de uno solo); `null` si no hay lote guardado. */
    val lastSavedRecord: InventoryRecord?
        get() = lastSavedRecords.lastOrNull()
}

/**
 * ViewModel (MVVM + UDF) del flujo lineal:
 * START → CALCULATOR (insumo + cantidad + total en vivo) → CONFIRM (revisar/guardar)
 * → SUCCESS (Deshacer ~5 s) → START, más HISTORY → DAY_DETAIL (borrado por fila).
 *
 * - Estado: un único [MutableStateFlow] con [InventoryUiState]; la UI solo emite eventos
 *   (los métodos `onX`) y dibuja el estado (StateFlow).
 * - Persistencia: [InventoryRepository] (Room) con `Flow` reactivo colectado en [viewModelScope];
 *   TODOS los `suspend` van envueltos en `try/catch` para no crashear nunca.
 * - Semana: [WeekFields] del locale por defecto; el año/semana del registro se calculan
 *   con la fecha DEL REGISTRO en [onConfirmarGuardar].
 * - Señales visuales únicas (sin sonido ni voz): `quantityError`, `ocrMessage`,
 *   `savingError`, `dataError`.
 * - Lote: [InventoryUiState.itemsPendientes] se arma en CONFIRM con [onAgregarOtro] y se
 *   persiste TODO junto en [onConfirmarGuardar] (varios productos en una sesión).
 *
 * Creada con [factory]. Sin estado global: cada sesión empieza limpio en `START`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(
    private val repository: InventoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    /** Estado observable para la UI: `viewModel.uiState.collectAsStateWithLifecycle()`. */
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    /** Semana elegida en el historial; alimenta el Flow de [InventoryUiState.recordsForSelectedWeek]. */
    private val semanaSeleccionada = MutableStateFlow<WeeklySummary?>(null)

    /** Temporizador de la ventana de "Deshacer" (~5 s). */
    private var ventanaDeshacerJob: Job? = null

    /** Jobs de los colectores de Room; se cancelan y recrean en [suscribirDatos]. */
    private var jobsColecta: List<Job> = emptyList()

    init {
        suscribirDatos()
    }

    /**
     * (Re)suscribe los Flujos de Room en [viewModelScope] guardando sus [Job] en
     * [jobsColecta]. SIEMPRE cancela la suscripción anterior antes de lanzar las
     * nuevas: así [onReintentar] puede llamarse las veces que haga falta sin duplicar
     * colectores. (La VM puede vivir fuera de un ViewModelStore — `onCleared()` no
     * llegaría —, por lo que esta cancelación explícita es la garantía de que solo
     * quedan 3 jobs vivos por instancia.)
     * Se lanza desde [init] y desde [onReintentar].
     */
    private fun suscribirDatos() {
        jobsColecta.forEach { it.cancel() }

        // Semana local de HOY (año + número de semana) para la tarjeta de resumen.
        val hoy = LocalDate.now()
        val anioHoy = anioIsoDe(hoy)
        val semanaHoy = semanaIsoDe(hoy)

        // 1) Acumulado de la semana actual (emite null si la semana está vacía).
        val jobResumenSemanal = viewModelScope.launch {
            repository.getWeeklyTotal(anioHoy, semanaHoy)
                .catch {
                    marcarDatosNoDisponibles()
                    emit(null)
                }
                .collect { resumen ->
                    _uiState.update { it.copy(loading = false, weeklySummary = resumen) }
                }
        }

        // 2) Historial completo de semanas.
        val jobHistorial = viewModelScope.launch {
            repository.getAllWeeklySummaries()
                .catch {
                    marcarDatosNoDisponibles()
                    emit(emptyList())
                }
                .collect { resumenes ->
                    _uiState.update { it.copy(loading = false, weeklySummaries = resumenes) }
                }
        }

        // 3) Desglose de la semana seleccionada en el historial (vacío si no hay selección).
        val jobDesglose = viewModelScope.launch {
            semanaSeleccionada
                .flatMapLatest { semana ->
                    if (semana == null) {
                        flowOf(emptyList<InventoryRecord>())
                    } else {
                        repository.getRecordsByWeek(semana.year, semana.weekOfYear)
                    }
                }
                .catch {
                    marcarDatosNoDisponibles()
                    emit(emptyList())
                }
                .collect { registros ->
                    _uiState.update { it.copy(recordsForSelectedWeek = registros) }
                }
        }

        jobsColecta = listOf(jobResumenSemanal, jobHistorial, jobDesglose)
    }

    // ---------------------------------------------------------------------
    // Calculadora: producto + cantidad (teclado propio)
    // ---------------------------------------------------------------------

    /**
     * Elige el insumo y lo refleja en el visor de la calculadora (sin cambiar de
     * pantalla). Conserva lo ya tecleado y limpia errores/mensajes.
     */
    fun onProductoElegido(producto: ProductType) {
        _uiState.update {
            it.copy(
                selectedProduct = producto,
                productError = false,
                quantityError = false,
                ocrMessage = null,
                savingError = null,
            )
        }
    }

    // ---------------------------------------------------------------------
    // Cantidad: teclado propio + lectura de foto (OCR)
    // ---------------------------------------------------------------------

    /**
     * Agrega los dígitos tocados (acepta "7" y también "00") al campo de cantidad.
     * Evita ceros a la izquierda, limita a [MAXIMO_DIGITOS_CANTIDAD] dígitos y
     * limpia `quantityError`/`ocrMessage`. Ignora lo que no sean dígitos.
     */
    fun onDigito(digito: String) {
        val digitos = digito.filter { it.isDigit() }
        if (digitos.isEmpty()) return
        _uiState.update { estado ->
            val actual = estado.quantityInput
            val agregado = when {
                actual == "0" -> digitos.trimStart('0').ifEmpty { "0" }
                else -> actual + digitos
            }.take(MAXIMO_DIGITOS_CANTIDAD)
            estado.copy(quantityInput = agregado, quantityError = false, ocrMessage = null)
        }
    }

    /** Borra el último dígito (queda `"0"` si el campo queda vacío). Limpia errores. */
    fun onBorrarDigito() {
        _uiState.update { estado ->
            val recortado = estado.quantityInput.dropLast(1)
            estado.copy(
                quantityInput = recortado.ifEmpty { "0" },
                quantityError = false,
                ocrMessage = null,
            )
        }
    }

    // ---------------------------------------------------------------------
    // OCR de FOTO: la persona fotografía la hoja y elige el número leído
    // ---------------------------------------------------------------------

    /**
     * Resultado de leer una FOTO (tomada con la cámara del sistema; la UI llama a
     * `camera.leerNumerosDeFoto` y entrega aquí los números):
     * - **Vacía** (o todos los leídos fuera de `0..MAXIMO_VALOR_CANTIDAD`) →
     *   [MENSAJE_IMAGEN_FALLIDO] en `ocrMessage`; **conserva** `quantityInput`.
     * - **Con números** → `ocrCandidatos` = la lista (hasta [MAXIMO_CANDIDATOS_IMAGEN])
     *   y `ocrMessage = null`; **`quantityInput` queda INTACTO** hasta que la persona
     *   confirme con [onCandidatoElegido]. SIEMPRE se confirma (aunque la foto haya
     *   dado un solo número): la letra manuscrita se confunde con facilidad (1/7/4/8)
     *   y el diálogo muestra el recorte de la letra para comparar.
     */
    fun onImagenLeida(numeros: List<Int>) {
        val candidatos = numeros
            .filter { it in 0..MAXIMO_VALOR_CANTIDAD }
            .take(MAXIMO_CANDIDATOS_IMAGEN)
        if (candidatos.isEmpty()) {
            _uiState.update { it.copy(ocrMessage = MENSAJE_IMAGEN_FALLIDO) }
        } else {
            _uiState.update { it.copy(ocrCandidatos = candidatos, ocrMessage = null) }
        }
    }

    /**
     * La persona toca un chip de [InventoryUiState.ocrCandidatos]: carga ese número en
     * `quantityInput` con los mismos topes que el teclado/OCR de cámara
     * (`0..MAXIMO_VALOR_CANTIDAD`) y cierra la selección
     * (`ocrCandidatos = []`, `ocrMessage = null`, `quantityError = false`).
     */
    fun onCandidatoElegido(numero: Int) {
        val limitado = numero.coerceIn(0, MAXIMO_VALOR_CANTIDAD)
        _uiState.update {
            it.copy(
                quantityInput = limitado.toString(),
                ocrCandidatos = emptyList(),
                ocrMessage = null,
                quantityError = false,
            )
        }
    }

    /** Botón "✍️ Escribir a mano": cierra los candidatos sin tocar `quantityInput`. */
    fun onCerrarCandidatos() {
        _uiState.update { it.copy(ocrCandidatos = emptyList()) }
    }

    /**
     * Valida la calculadora y avanza al paso de revisión:
     * - Sin producto → `productError = true` (aviso "Primero elige el insumo") y NO avanza.
     * - Cantidad < 1 → `quantityError = true` (aviso "Primero escribe cuántos") y NO avanza.
     * - Correcto → [Screen.CONFIRM].
     */
    fun onSiguiente() {
        _uiState.update { estado ->
            val cantidad = estado.quantityInput.toIntOrNull() ?: 0
            when {
                estado.selectedProduct == null ->
                    estado.copy(productError = true, quantityError = false)

                cantidad < 1 ->
                    estado.copy(quantityError = true, productError = false)

                else ->
                    estado.copy(
                        screen = Screen.CONFIRM,
                        quantityError = false,
                        productError = false,
                        ocrMessage = null,
                        savingError = null,
                    )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Lote: varios productos en una sesión (feedback del usuario)
    // ---------------------------------------------------------------------

    /**
     * Agrega el item actual al lote ([InventoryUiState.itemsPendientes]) como draft
     * (`id = 0`, con fecha/semana/año de [InventoryUiState.selectedDate]) y vuelve a la
     * calculadora para elegir el siguiente insumo; limpia la selección actual (producto,
     * cantidad → `"0"` y fecha → hoy). Mismas validaciones que guardar: sin producto →
     * `productError`; cantidad < 1 → `quantityError` y NO agrega.
     * Solo actúa desde CONFIRM. **NO persiste nada**: el lote se guarda con
     * [onConfirmarGuardar].
     */
    fun onAgregarOtro() {
        val estado = _uiState.value
        val producto = estado.selectedProduct
        if (producto == null) {
            _uiState.update { it.copy(screen = Screen.CALCULATOR, productError = true) }
            return
        }
        val cantidad = estado.quantityInput.toIntOrNull()
        if (cantidad == null || cantidad < 1) {
            _uiState.update { it.copy(screen = Screen.CALCULATOR, quantityError = true) }
            return
        }

        // Mismo guard que guardar: el lote SOLO se edita desde el paso de revisión.
        if (estado.screen != Screen.CONFIRM) return

        val draft = crearRegistro(producto, cantidad, estado.selectedDate)
        _uiState.update {
            it.copy(
                itemsPendientes = it.itemsPendientes + draft,
                screen = Screen.CALCULATOR, // listo para el siguiente insumo
                selectedProduct = null,
                quantityInput = "0",
                quantityError = false,
                productError = false,
                ocrMessage = null,
                selectedDate = LocalDate.now(),
            )
        }
    }

    /**
     * Quita el item en la posición [pos] del lote. Permanece en [Screen.CONFIRM]
     * (lista vacía + sin producto actual → el comportamiento de siempre).
     * Posición fuera de rango → no-op.
     */
    fun onQuitarPendiente(pos: Int) {
        _uiState.update { estado ->
            if (pos !in estado.itemsPendientes.indices) {
                estado
            } else {
                estado.copy(
                    itemsPendientes = estado.itemsPendientes.filterIndexed { i, _ -> i != pos },
                )
            }
        }
    }

    /**
     * Cambia SOLO la fecha (y por tanto semana/año) del item [pos] del lote;
     * `totalCop` y `unitPriceCop` no cambian. Posición fuera de rango → no-op.
     */
    fun onFechaDePendiente(pos: Int, fecha: LocalDate) {
        _uiState.update { estado ->
            if (pos !in estado.itemsPendientes.indices) {
                estado
            } else {
                val lista = estado.itemsPendientes.toMutableList()
                val item = lista[pos]
                lista[pos] = item.copy(
                    date = fecha,
                    weekOfYear = semanaIsoDe(fecha),
                    year = anioIsoDe(fecha),
                )
                estado.copy(itemsPendientes = lista)
            }
        }
    }

    /**
     * Tarjeta "continuar lista" del Inicio: START → CONFIRM para revisar/guardar el lote
     * sin capturar otro insumo. No-op si el lote está vacío o no estás en START.
     */
    fun onContinuarLote() {
        _uiState.update { estado ->
            when {
                estado.itemsPendientes.isEmpty() -> estado
                estado.screen != Screen.START -> estado
                else -> estado.copy(screen = Screen.CONFIRM)
            }
        }
    }

    /**
     * Botón "➕ CONTAR INSUMOS" del Inicio: START → CALCULATOR siempre,
     * independientemente de que haya producto/cantidad preservados de una cadena de
     * "Atrás". La selección (producto/cantidad) se conserva a propósito: si la persona
     * reingresa, su número escrito sigue ahí. Solo limpia flags de error/OCR.
     * No-op si no estás en START o hay un guardado en curso.
     */
    fun onEmpezarConteo() {
        _uiState.update { estado ->
            when {
                estado.screen != Screen.START -> estado
                estado.isSaving -> estado
                else -> estado.copy(
                    screen = Screen.CALCULATOR,
                    quantityError = false,
                    productError = false,
                    savingError = null,
                    ocrMessage = null,
                    ocrCandidatos = emptyList(),
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Navegación general
    // ---------------------------------------------------------------------

    /**
     * Recorrido inverso: CONFIRM → CALCULATOR → START; DAY_DETAIL → HISTORY → START.
     * **Preserva `quantityInput`** (y el producto) en toda la cadena; solo se apagan
     * los flags de error.
     * Desde SUCCESS vuelve a START limpiando (equivale a [onListo]).
     */
    fun onAtras() {
        val estadoPrevia = _uiState.value
        if (estadoPrevia.isSaving) return // no interrumpir un guardado en curso

        _uiState.update { estado ->
            when (estado.screen) {
                Screen.CONFIRM ->
                    estado.copy(screen = Screen.CALCULATOR, savingError = null)

                // quantityInput y producto PRESERVADOS (se conservan al volver).
                Screen.CALCULATOR ->
                    estado.copy(
                        screen = Screen.START,
                        quantityError = false,
                        productError = false,
                        ocrMessage = null,
                    )

                Screen.DAY_DETAIL ->
                    estado.copy(
                        screen = Screen.HISTORY,
                        selectedWeek = null,
                        recordsForSelectedWeek = emptyList(),
                    )

                Screen.HISTORY ->
                    estado.copy(screen = Screen.START)

                Screen.SUCCESS -> {
                    estadoEnInicio(estado) // la fila ya quedó guardada
                }

                Screen.START -> estado
            }
        }

        // Efectos secundarios FUERA del update (que puede re-ejecutar el lambda).
        when (estadoPrevia.screen) {
            Screen.DAY_DETAIL -> semanaSeleccionada.value = null
            Screen.SUCCESS -> {
                ventanaDeshacerJob?.cancel()
                ventanaDeshacerJob = null
            }
            else -> Unit
        }
    }

    /** Botón "Listo" de SUCCESS: vuelve a START limpiando la sesión (incluida la lista de
     *  pendientes, que ya quedó persistida). */
    fun onListo() {
        if (_uiState.value.isSaving) return
        ventanaDeshacerJob?.cancel()
        ventanaDeshacerJob = null
        _uiState.update { estadoEnInicio(it) }
    }

    /** Abre el historial de semanas. */
    fun onAbrirHistorial() {
        _uiState.update { it.copy(screen = Screen.HISTORY) }
    }

    /** Entra al detalle de la semana elegida y carga su desglose ([InventoryUiState.recordsForSelectedWeek]). */
    fun onElegirSemana(resumen: WeeklySummary) {
        val semanaPrevia = _uiState.value.selectedWeek
        semanaSeleccionada.value = resumen
        _uiState.update {
            it.copy(
                screen = Screen.DAY_DETAIL,
                selectedWeek = resumen,
                // Si es la misma semana el Flow no vuelve a emitir: conservar el desglose.
                recordsForSelectedWeek = if (semanaPrevia == resumen) it.recordsForSelectedWeek else emptyList(),
            )
        }
    }

    /** Sale del historial y vuelve al inicio. (Desde DAY_DETAIL, para volver al
     * historial usa [onAtras]; este método es el "Volver al inicio" de HISTORY.) */
    fun onVolverDeHistorial() {
        semanaSeleccionada.value = null
        _uiState.update {
            it.copy(
                screen = Screen.START,
                selectedWeek = null,
                recordsForSelectedWeek = emptyList(),
            )
        }
    }

    /** Selector de fecha del registro (por defecto hoy; ver [InventoryUiState.selectedDate]). */
    fun onFechaElegida(fecha: LocalDate) {
        _uiState.update { it.copy(selectedDate = fecha) }
    }

    /** Borra una fila del desglose en DAY_DETAIL (borrado por fila, con `runCatching`). */
    fun onEliminarRegistro(record: InventoryRecord) {
        viewModelScope.launch {
            try {
                repository.deleteRecord(record)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(savingError = MENSAJE_BORRAR_FALLIDO) }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Revisión: guardado + ventana de deshacer
    // ---------------------------------------------------------------------

    /**
     * Única vía de persistencia (la UI solo llega aquí desde CONFIRM).
     *
     * Guarda el LOTE completo: [InventoryUiState.itemsPendientes] en orden y, si el item
     * actual es válido (producto + cantidad ≥ 1), este al final. Si NO hay lote, mandan
     * las validaciones de siempre (sin producto → `productError`; cantidad < 1 →
     * `quantityError`, sin salir de la pantalla); si el lote NO está vacío, un item
     * actual inválido se omite sin más (así se guarda solo la lista).
     *
     * Cada [InventoryRecord] usa la fecha elegida (hoy por defecto) con año/semana de ESA
     * fecha vía [WeekFields] del locale por defecto y
     * `totalCop = BigDecimal(cantidad × precio).setScale(2, HALF_UP)`.
     *
     * Éxito → [Screen.SUCCESS] con `lastSavedRecords` (ids REALES), lote y selección
     * limpios y `canUndo` (ventana de ~5 s). Fallo → `savingError` con mensaje cotidiano
     * y SE MANTIENE en CONFIRM (reintentable); la persistencia es ATÓMICA
     * (`insertBatch` transaccional de Room: o entran todos los ítems o ninguno, sin
     * duplicados al reintentar). Nunca lanza ni crashea (re-lanza [CancellationException]).
     */
    fun onConfirmarGuardar() {
        val estado = _uiState.value
        if (estado.isSaving) return // evita doble toque = doble registro

        val producto = estado.selectedProduct
        val cantidad = estado.quantityInput.toIntOrNull() ?: 0
        val hayLote = estado.itemsPendientes.isNotEmpty()

        if (!hayLote) {
            // Sin lote mandan las validaciones CLÁSICAS (contrato del flujo).
            if (producto == null) {
                _uiState.update { it.copy(screen = Screen.CALCULATOR, productError = true) }
                return
            }
            if (cantidad < 1) {
                _uiState.update { it.copy(screen = Screen.CALCULATOR, quantityError = true) }
                return
            }
        }

        // Guard: solo se PERSISTE desde la revisión (CONFIRM). Fuera de ahí (con datos ya
        // válidos) se ignora en silencio: misma pantalla y SIN insertar.
        if (estado.screen != Screen.CONFIRM) return

        // Ítems a persistir EN ORDEN: lote pendiente + item actual (si es válido).
        val actual =
            if (producto != null && cantidad >= 1) crearRegistro(producto, cantidad, estado.selectedDate)
            else null
        val aInsertar = if (actual != null) estado.itemsPendientes + actual else estado.itemsPendientes

        _uiState.update { it.copy(isSaving = true, savingError = null) }

        viewModelScope.launch {
            try {
                // Transacción ATÓMICA de Room: o se insertan todos los ítems o ninguno.
                val ids = repository.insertBatch(aInsertar)
                val guardados = aInsertar.zip(ids).map { (registro, id) -> registro.copy(id = id) }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        screen = Screen.SUCCESS,
                        lastSavedRecords = guardados,
                        itemsPendientes = emptyList(),
                        selectedProduct = null,
                        quantityInput = "0",
                        quantityError = false,
                        ocrMessage = null,
                        ocrCandidatos = emptyList(),
                        selectedDate = LocalDate.now(),
                        canUndo = true,
                        savingError = null,
                    )
                }
                programarCierreDeVentanaDeDeshacer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // insertBatch es transaccional: no hay inserts parciales que revertir.
                _uiState.update { it.copy(isSaving = false, savingError = MENSAJE_GUARDAR_FALLIDO) }
            }
        }
    }

    /**
     * "Deshacer" de [Screen.SUCCESS]: solo actúa dentro de la ventana (`canUndo`).
     * Borra POR PK TODOS los registros de [InventoryUiState.lastSavedRecords] y
     * restaura [InventoryUiState.itemsPendientes] = esos registros con `id = 0`
     * (drafts), volviendo a [Screen.CONFIRM] para que la persona corrija la lista.
     * Si el borrado falla, igual sale de SUCCESS (para no quedar trabado), NO restaura
     * (los registros siguen guardados) y lo avisa con `savingError`. Mismos mensajes y
     * catches que siempre (re-lanza [CancellationException]).
     */
    fun onDeshacer() {
        val estado = _uiState.value
        val guardados = estado.lastSavedRecords
        if (!estado.canUndo || guardados.isEmpty()) return

        ventanaDeshacerJob?.cancel()
        ventanaDeshacerJob = null
        _uiState.update { it.copy(canUndo = false) } // cierra la ventana ya (evita doble toque)

        viewModelScope.launch {
            val error: String? = try {
                // Borrado ATÓMICO del lote (transacción Room): o se borran todos o ninguno.
                repository.deleteBatch(guardados)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MENSAJE_DESHACER_FALLIDO
            }
            _uiState.update {
                it.copy(
                    screen = Screen.CONFIRM,
                    itemsPendientes =
                        if (error == null) guardados.map { r -> r.copy(id = 0L) }
                        else it.itemsPendientes,
                    lastSavedRecords = emptyList(),
                    canUndo = false,
                    savingError = error,
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Error de datos: botón "Intentar de nuevo"
    // ---------------------------------------------------------------------

    /**
     * Botón "Intentar de nuevo" de la pantalla de error (`dataError` / base de datos
     * no disponible): (a) limpia los flags de error —`dataError = false`,
     * `loading = true`, `savingError = null`, `ocrMessage = null`—, (b) cancela los
     * jobs de colecta actuales y (c) re-suscribe los Flujos de Room vía
     * [suscribirDatos]. NO toca la sesión de registro (producto/cantidad/lote/fecha),
     * la navegación, la lista de candidatos de OCR ni la ventana de deshacer.
     */
    fun onReintentar() {
        _uiState.update {
            it.copy(dataError = false, loading = true, savingError = null, ocrMessage = null)
        }
        suscribirDatos()
    }

    // ---------------------------------------------------------------------
    // Internos
    // ---------------------------------------------------------------------

    /**
     * Crea un registro (draft si `id = 0`) con año/semana de [fecha] (WeekFields del
     * locale por defecto), `productName` = `displayName`, `unitPriceCop` del catálogo y
     * `totalCop = BigDecimal(cantidad × precio).setScale(2, HALF_UP)`.
     */
    private fun crearRegistro(producto: ProductType, cantidad: Int, fecha: LocalDate): InventoryRecord =
        InventoryRecord(
            id = 0L,
            date = fecha,
            weekOfYear = semanaIsoDe(fecha),
            year = anioIsoDe(fecha),
            productName = producto.displayName,
            unitPriceCop = producto.unitPriceCop,
            quantity = cantidad,
            totalCop = BigDecimal(producto.totalPara(cantidad))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble(),
        )

    /** Cierra la ventana de deshacer a los ~5 s; si sigues en SUCCESS, vuelve a START limpiando. */
    private fun programarCierreDeVentanaDeDeshacer() {
        ventanaDeshacerJob?.cancel()
        ventanaDeshacerJob = viewModelScope.launch {
            delay(VENTANA_DESHACER_MS)
            _uiState.update { estado ->
                if (estado.screen == Screen.SUCCESS) {
                    estadoEnInicio(estado) // "vuelve a Inicio" tras la ventana
                } else {
                    estado.copy(canUndo = false, lastSavedRecords = emptyList())
                }
            }
        }
    }

    /** Copia idéntica pero en START con la sesión de registro limpia (fecha vuelve a hoy,
     *  la lista de pendientes se vacía y el lote guardado se olvida). */
    private fun estadoEnInicio(estado: InventoryUiState): InventoryUiState = estado.copy(
        screen = Screen.START,
        selectedProduct = null,
        quantityInput = "0",
        itemsPendientes = emptyList(),
        lastSavedRecords = emptyList(),
        canUndo = false,
        quantityError = false,
        productError = false,
        savingError = null,
        ocrMessage = null,
        isSaving = false,
        selectedDate = LocalDate.now(),
    )

    /** Señal de "base de datos no disponible" para la UI (sin crashear). */
    private fun marcarDatosNoDisponibles() {
        _uiState.update { it.copy(loading = false, dataError = true) }
    }

    companion object {

        /** Duración de la ventana de "Deshacer" en SUCCESS (~5 s). */
        const val VENTANA_DESHACER_MS = 5_000L

        /** Tope de dígitos tecleables en la cantidad (evita desbordes de Int). */
        const val MAXIMO_DIGITOS_CANTIDAD = 6

        /** Tope de valor de cantidad (999.999). */
        const val MAXIMO_VALOR_CANTIDAD = 999_999

        /** `InventoryUiState.ocrMessage` cuando la FOTO no dio números legibles (o el
         *  único leído superaba los 999.999 y se omitió). */
        const val MENSAJE_IMAGEN_FALLIDO =
            "No pude leer números en la foto. Escríbelo tú mismo."

        /** `InventoryUiState.savingError` cuando `insertRecord` lanza. */
        const val MENSAJE_GUARDAR_FALLIDO = "No pude guardar el registro. Intenta una vez más."

        /** `InventoryUiState.savingError` cuando `deleteRecord` (deshacer) lanza. */
        const val MENSAJE_DESHACER_FALLIDO =
            "No pude deshacer el guardado. El registro sigue en tu historial."

        /** `InventoryUiState.savingError` cuando `deleteRecord` (borrado por fila) lanza. */
        const val MENSAJE_BORRAR_FALLIDO = "No pude borrar el registro. Intenta una vez más."

        /** `InventoryUiState.dataError` cuando los Flujos de Room fallan. */
        const val MENSAJE_DATOS_NO_DISPONIBLES =
            "No pude cargar tus datos locales. Cierra y vuelve a abrir la app."

        /**
         * Factory para la UI: `viewModel(factory = InventoryViewModel.factory(context))`.
         * Usa el singleton de [InventoryRepository] ligado al Contexto de la aplicación
         * (no se filtra la Activity).
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return InventoryViewModel(InventoryRepository.getInstance(appContext)) as T
                }
            }
        }
    }
}
