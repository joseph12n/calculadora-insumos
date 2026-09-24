package com.bioplast.insumos

import com.bioplast.insumos.camera.MAXIMO_CANDIDATOS_IMAGEN
import com.bioplast.insumos.data.InventoryDao
import com.bioplast.insumos.data.InventoryRepository
import com.bioplast.insumos.model.InventoryRecord
import com.bioplast.insumos.model.ProductType
import com.bioplast.insumos.model.Screen
import com.bioplast.insumos.model.WeeklySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Doble falso del DAO: implementa [InventoryDao] con Flujos en memoria (puro JVM,
 * sin Robolectric). Permite construir el [InventoryRepository] REAL (su constructor
 * exige esta interfaz) y así probar [InventoryViewModel] de punta a punta.
 */
private class FakeInventoryDao : InventoryDao {

    private val registros = MutableStateFlow<List<InventoryRecord>>(emptyList())

    /** Registros que el DAO aceptó (para comprobar que guardar NO ocurre fuera del Paso 3). */
    val insertados = mutableListOf<InventoryRecord>()

    /** Registros borrados (deshacer / borrar por fila). */
    val borrados = mutableListOf<InventoryRecord>()

    var fallarInsert = false

    private var siguienteId = 1L

    fun registrosActuales(): List<InventoryRecord> = registros.value

    override suspend fun insertRecord(record: InventoryRecord): Long {
        if (fallarInsert) throw RuntimeException("Fallo simulado de insert (fake)")
        val id = siguienteId++
        val guardado = record.copy(id = id)
        registros.value = registros.value + guardado
        insertados += guardado
        return id
    }

    /**
     * Lote para `insertBatch` (método por defecto de la interfaz que delega aquí):
     * ids secuenciales en orden, como Room. `fallarInsert` lanza ANTES de insertar
     * cualquiera, emulando la transacción atómica real (o todos o ninguno).
     */
    override suspend fun insertAll(records: List<InventoryRecord>): List<Long> {
        if (fallarInsert) throw RuntimeException("Fallo simulado de insert (fake)")
        return records.map { record ->
            val id = siguienteId++
            val guardado = record.copy(id = id)
            registros.value = registros.value + guardado
            insertados += guardado
            id
        }
    }

    override suspend fun deleteRecord(record: InventoryRecord) {
        borrados += record
        registros.value = registros.value.filterNot { it.id == record.id }
    }

    /**
     * `deleteBatch` (método por defecto de la interfaz, @Transaction en Room) delega
     * aquí: borra todos los ids indicados y los registra en `borrados`, como el real.
     */
    override suspend fun deleteAll(records: List<InventoryRecord>) {
        val ids = records.map { it.id }.toSet()
        borrados += records
        registros.value = registros.value.filterNot { it.id in ids }
    }

    override fun getRecordsByDate(date: LocalDate): Flow<List<InventoryRecord>> =
        registros.map { lista -> lista.filter { it.date == date }.sortedByDescending { it.id } }

    override fun getRecordsByWeek(year: Int, week: Int): Flow<List<InventoryRecord>> =
        registros.map { lista ->
            lista.filter { it.year == year && it.weekOfYear == week }
                .sortedWith(compareBy({ it.date }, { it.id }))
        }

    override fun getWeeklyTotal(year: Int, week: Int): Flow<WeeklySummary?> =
        registros.map { lista ->
            resumenDe(
                lista.filter { it.year == year && it.weekOfYear == week },
                year,
                week,
            )
        }

    override fun getAllWeeklySummaries(): Flow<List<WeeklySummary>> =
        registros.map { lista ->
            lista.groupBy { it.year to it.weekOfYear }
                .map { (clave, filas) -> resumenDe(filas, clave.first, clave.second) }
                .filterNotNull()
                .sortedWith(
                    compareByDescending<WeeklySummary> { it.year }
                        .thenByDescending { it.weekOfYear },
                )
        }

    private fun resumenDe(
        filas: List<InventoryRecord>,
        year: Int,
        week: Int,
    ): WeeklySummary? =
        if (filas.isEmpty()) {
            null
        } else {
            WeeklySummary(
                year = year,
                weekOfYear = week,
                totalCop = filas.sumOf { it.totalCop },
                totalQuantity = filas.sumOf { it.quantity }.toLong(),
            )
        }
}

/**
 * Tests de estados de [InventoryViewModel] (UDF) sobre un repositorio real
 * construido con un DAO falso en memoria. Tiempo virtual con
 * `kotlinx-coroutines-test` (ver [TestCoroutineScheduler]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var dao: FakeInventoryDao
    private lateinit var repository: InventoryRepository

    @Before
    fun setUp() {
        scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        dao = FakeInventoryDao()
        repository = InventoryRepository(dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = InventoryViewModel(repository)

    /** Teclea la cantidad dígito a dígito con el teclado propio de la app. */
    private fun InventoryViewModel.escribirCantidad(texto: String) {
        texto.forEach { onDigito(it.toString()) }
    }

    /** Recorrido completo: Paso 1 → Paso 2 → Paso 3 → guardar. */
    private fun guardarRapido(
        vm: InventoryViewModel,
        producto: ProductType,
        cantidad: String,
        fecha: LocalDate? = null,
    ) {
        vm.onProductoElegido(producto)
        vm.escribirCantidad(cantidad)
        vm.onSiguiente()
        if (fecha != null) vm.onFechaElegida(fecha)
        vm.onConfirmarGuardar()
        scheduler.runCurrent()
    }

    // ---------------------------------------------------------------------
    // Paso 2: validación de cantidad
    // ---------------------------------------------------------------------

    @Test
    fun cantidadCeroActivaElErrorYNoAvanzaAlPaso3() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FRASCO_ORINA) // Paso 1 → Paso 2 (cantidad "0")
        assertEquals(Screen.ENTER_QUANTITY, vm.uiState.value.screen)

        vm.onSiguiente() // cantidad 0 → debe quedarse en el Paso 2 con error

        val estado = vm.uiState.value
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
        assertTrue("quantityError debe estar activo con cantidad 0", estado.quantityError)
        assertEquals(0, estado.cantidad)
        assertEquals("Nunca persiste con cantidad 0", 0, dao.insertados.size)

        // Al escribir un dígito el error se limpia (la UI habilita "Siguiente").
        vm.onDigito("5")
        assertFalse(vm.uiState.value.quantityError)
        assertEquals("5", vm.uiState.value.quantityInput)
    }

    @Test
    fun productoElegidoLlevaAlPasoDeCantidad() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)

        val estado = vm.uiState.value
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
        assertEquals(2, estado.screen.paso)
        assertEquals(ProductType.CRYOTUBO, estado.selectedProduct)
        assertFalse(estado.quantityError)
    }

    @Test
    fun siguienteSinProductoRegresaAlPaso1() = runTest {
        val vm = crearViewModel()
        vm.escribirCantidad("4")
        vm.onSiguiente() // sin producto seleccionado
        assertEquals(Screen.PICK_PRODUCT, vm.uiState.value.screen)
    }

    // ---------------------------------------------------------------------
    // Navegación: "Atrás" nunca pierde la cantidad
    // ---------------------------------------------------------------------

    @Test
    fun atrasNuncaPierdeLaCantidadEscrita() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CAJA_COPROLOGICA)
        vm.escribirCantidad("718")
        assertEquals("718", vm.uiState.value.quantityInput)

        vm.onAtras() // Paso 2 → Paso 1
        assertEquals(Screen.PICK_PRODUCT, vm.uiState.value.screen)
        assertEquals("718", vm.uiState.value.quantityInput)

        vm.onAtras() // Paso 1 → Inicio
        assertEquals(Screen.START, vm.uiState.value.screen)
        assertEquals("718", vm.uiState.value.quantityInput)

        // Reentrar al registro: la cantidad sigue intacta.
        vm.onProductoElegido(ProductType.CAJA_COPROLOGICA)
        assertEquals(Screen.ENTER_QUANTITY, vm.uiState.value.screen)
        assertEquals("718", vm.uiState.value.quantityInput)
    }

    // ---------------------------------------------------------------------
    // Fórmula en vivo: totalActualCop = cantidad × precioUnitario
    // ---------------------------------------------------------------------

    @Test
    fun totalActualCopEsCantidadPorPrecioUnitario() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.BACILOSCOPIA) // 9.88
        vm.escribirCantidad("2")
        assertEquals(19.76, vm.uiState.value.totalActualCop, 1e-9)

        val vm2 = crearViewModel()
        vm2.onProductoElegido(ProductType.FRASCO_ORINA) // 7.18
        vm2.escribirCantidad("11")
        // 11 × 7.18 = 78.97999999999999 (residuo binario tolerable en el estado vivo;
        // el redondeo a 2 decimales se aplica al guardar y al mostrar).
        assertEquals(78.98, vm2.uiState.value.totalActualCop, 1e-9)
    }

    // ---------------------------------------------------------------------
    // Guardar (Paso 3): registro con semana/año y total redondeado
    // ---------------------------------------------------------------------

    @Test
    fun guardarCreaElRegistroConSemanaAnioYTotalRedondeado() = runTest {
        val vm = crearViewModel()
        val fecha = LocalDate.of(2026, 6, 15) // lunes dentro de la semana 25 (es_CO)
        guardarRapido(vm, ProductType.FRASCO_ORINA, "11", fecha)

        val estado = vm.uiState.value
        assertEquals(Screen.SUCCESS, estado.screen)
        assertFalse(estado.isSaving)
        assertTrue("Deshacer habilitado dentro de la ventana", estado.canUndo)
        assertEquals(1, dao.insertados.size)
        assertEquals(1, dao.registrosActuales().size)

        val record = estado.lastSavedRecord
        assertNotNull("lastSavedRecord debe traer el id real", record)
        record!!
        assertTrue(
            "El id real debe ser > 0 para poder deshacer (regresión del bug corregido)",
            record.id > 0,
        )
        assertEquals(fecha, record.date)
        assertEquals("Frasco de orina", record.productName)
        assertEquals(7.18, record.unitPriceCop, 0.0)
        assertEquals(11, record.quantity)

        // Semana/año calculados con el MISMO WeekFields del locale por defecto
        // que usa la app (InventoryViewModel.anioIsoDe/semanaIsoDe).
        val campos = WeekFields.of(Locale.getDefault())
        assertEquals(fecha.get(campos.weekOfWeekBasedYear()), record.weekOfYear)
        assertEquals(fecha.get(campos.weekBasedYear()), record.year)

        // totalCop = 11 × 7.18 → BigDecimal HALF_UP a 2 decimales = 78.98 exacto.
        assertEquals(78.98, record.totalCop, 1e-9)
    }

    @Test
    fun primerDiaDelAnioEsSemanaUnoDelWeekBasedYear() = runTest {
        val vm = crearViewModel()
        val fecha = LocalDate.of(2026, 1, 1)
        guardarRapido(vm, ProductType.CAJA_COPROLOGICA, "2", fecha)

        val record = vm.uiState.value.lastSavedRecord
        assertNotNull(record)
        record!!

        val campos = WeekFields.of(Locale.getDefault())
        assertEquals(fecha.get(campos.weekOfWeekBasedYear()), record.weekOfYear)
        assertEquals(fecha.get(campos.weekBasedYear()), record.year)
        // 1 de enero cae en la semana 1 del año-de-semana (locales es/en: siempre 1).
        assertEquals(1, record.weekOfYear)
        assertEquals(2026, record.year)
        // 2 × 5.00 = 10.00 exacto.
        assertEquals(10.0, record.totalCop, 0.0)
    }

    @Test
    fun treintaYUnDeDiciembreUsaWeekBasedYearNoElAnioCalendario() = runTest {
        val vm = crearViewModel()
        val fecha = LocalDate.of(2025, 12, 31) // miércoles de cierre de año
        guardarRapido(vm, ProductType.FALCON, "3", fecha)

        val record = vm.uiState.value.lastSavedRecord
        assertNotNull(record)
        record!!

        val campos = WeekFields.of(Locale.getDefault())
        assertEquals(fecha.get(campos.weekOfWeekBasedYear()), record.weekOfYear)
        assertEquals(fecha.get(campos.weekBasedYear()), record.year)
        assertTrue("weekOfYear debe estar en 1..53 (fue ${record.weekOfYear})", record.weekOfYear in 1..53)
        // En es_CO el 31/12/2025 pertenece a la semana 1 del año-de-semana 2026:
        // guardar el año calendario (2025) sería el bug clásico que este test caza.
        assertEquals(fecha.get(campos.weekBasedYear()), record.year)
        assertEquals(23.70, record.totalCop, 1e-9) // 3 × 7.90 = 23,70 redondeado
    }

    @Test
    fun semana52DeDiciembreSeGuardaEnSuSemana() = runTest {
        val vm = crearViewModel()
        val fecha = LocalDate.of(2025, 12, 22) // lunes de la última semana de diciembre
        guardarRapido(vm, ProductType.BACILOSCOPIA, "2", fecha)

        val record = vm.uiState.value.lastSavedRecord
        assertNotNull(record)
        record!!

        val campos = WeekFields.of(Locale.getDefault())
        assertEquals(fecha.get(campos.weekOfWeekBasedYear()), record.weekOfYear)
        assertEquals(fecha.get(campos.weekBasedYear()), record.year)
        assertTrue(
            "Diciembre tardío debe caer en semana 50..53 (fue ${record.weekOfYear})",
            record.weekOfYear in 50..53,
        )
        assertEquals(2025, record.year) // 22/12/2025 sigue siendo año-de-semana 2025
    }

    @Test
    fun fallaAlGuardarMantieneElPaso3YElErrorReintentable() = runTest {
        val vm = crearViewModel()
        dao.fallarInsert = true
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("4")
        vm.onSiguiente()
        assertEquals(Screen.CONFIRM, vm.uiState.value.screen)

        vm.onConfirmarGuardar()
        scheduler.runCurrent()

        val estado = vm.uiState.value
        assertEquals(Screen.CONFIRM, estado.screen) // se mantiene para reintentar
        assertFalse(estado.isSaving)
        assertEquals(InventoryViewModel.MENSAJE_GUARDAR_FALLIDO, estado.savingError)
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun guardarSinProductoNoPersisteYVuelveAlPaso1() = runTest {
        val vm = crearViewModel()
        vm.onConfirmarGuardar() // sin producto elegido
        scheduler.runCurrent()
        assertEquals(Screen.PICK_PRODUCT, vm.uiState.value.screen)
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun guardarConCantidadCeroNoPersiste() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FRASCO_ORINA) // cantidad sigue "0"
        vm.onConfirmarGuardar()
        scheduler.runCurrent()
        assertEquals(Screen.ENTER_QUANTITY, vm.uiState.value.screen)
        assertTrue(vm.uiState.value.quantityError)
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun guardarSoloDebeOcurrirDesdeElPaso3() = runTest {
        // REQUISITO: Screen.CONFIRM (Paso 3) es la "única vía de guardar"
        // (Screen.kt) / "guardar solo desde el Paso 3" (qa.md).
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FRASCO_ORINA) // estamos en el Paso 2
        vm.escribirCantidad("5")
        assertEquals(Screen.ENTER_QUANTITY, vm.uiState.value.screen)

        vm.onConfirmarGuardar() // nadie debería poder persistir sin pasar por CONFIRM
        scheduler.runCurrent()

        assertEquals(
            "onConfirmarGuardar debe ignorarse fuera de Screen.CONFIRM " +
                "(InventoryViewModel.onConfirmarGuardar no valida la pantalla actual)",
            0,
            dao.insertados.size,
        )
        assertEquals(Screen.ENTER_QUANTITY, vm.uiState.value.screen)
    }

    // ---------------------------------------------------------------------
    // Ventana de "Deshacer" (~5 s)
    // ---------------------------------------------------------------------

    @Test
    fun deshacerDentroDeLaVentanaBorraElRegistroYVuelveAlPaso2() = runTest {
        val vm = crearViewModel()
        guardarRapido(vm, ProductType.CRYOTUBO, "3")
        assertEquals(Screen.SUCCESS, vm.uiState.value.screen)
        assertTrue(vm.uiState.value.canUndo)
        assertEquals(1, dao.registrosActuales().size)

        vm.onDeshacer() // dentro de la ventana
        scheduler.runCurrent()

        // RE-BASELINE FASE 3: el contrato de Deshacer CAMBIÓ y la verificación de
        // comportamiento nueva es CORRECTA (InventoryViewModel.onDeshacer):
        // ya NO vuelve al Paso 2 conservando lo escrito; BORRA por PK, RESTAURA
        // itemsPendientes y regresa al PASO 3 (CONFIRM) para corregir la lista.
        // (Se conserva el nombre del test como referencia de la línea base.)
        val estado = vm.uiState.value
        assertEquals(Screen.CONFIRM, estado.screen)
        assertFalse(estado.canUndo)
        assertNull(estado.lastSavedRecord)
        // quantityInput es "0" porque la sesión se limpió AL GUARDAR (el éxito
        // pone quantityInput = "0" y selectedProduct = null); Deshacer no lo toca.
        assertEquals("0", estado.quantityInput)
        assertNull(estado.selectedProduct)
        // El registro borrado se RESTAURA como draft (id = 0) en itemsPendientes.
        assertEquals(1, estado.itemsPendientes.size)
        val restaurado = estado.itemsPendientes.single()
        assertEquals("El draft restaurado vuelve con id = 0", 0L, restaurado.id)
        assertEquals("Cryotubo", restaurado.productName)
        assertEquals(3, restaurado.quantity)
        assertEquals(23.70, restaurado.totalCop, 1e-9) // 3 × 7.90 redondeado a 2 decimales
        assertNull(estado.savingError)
        assertEquals(1, dao.borrados.size)
        assertTrue("El registro debe haberse borrado", dao.registrosActuales().isEmpty())
    }

    @Test
    fun deshacerDespuesDeLaVentanaNoBorra() = runTest {
        val vm = crearViewModel()
        guardarRapido(vm, ProductType.CRYOTUBO, "3")
        assertEquals(1, dao.registrosActuales().size)

        // Se cumple la ventana de ~5 s: la UI vuelve a Inicio con la ventana cerrada.
        scheduler.advanceTimeBy(InventoryViewModel.VENTANA_DESHACER_MS + 100)
        scheduler.runCurrent()

        val estadoCerrado = vm.uiState.value
        assertEquals(Screen.START, estadoCerrado.screen)
        assertFalse(estadoCerrado.canUndo)
        assertNull(estadoCerrado.lastSavedRecord)

        vm.onDeshacer() // fuera de la ventana: debe ignorarse por completo
        scheduler.runCurrent()

        assertEquals("Fuera de la ventana Deshacer no debe borrar nada", 0, dao.borrados.size)
        assertEquals(1, dao.registrosActuales().size)
        assertEquals(Screen.START, vm.uiState.value.screen)
    }

    // ---------------------------------------------------------------------
    // OCR: nunca destrutivo
    // ---------------------------------------------------------------------

    @Test
    fun ocrFallidoMuestraElMensajeSinBorrarLaCantidad() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("42")

        vm.onEscanearResultado(null)

        val estado = vm.uiState.value
        assertEquals(InventoryViewModel.MENSAJE_OCR_FALLIDO, estado.ocrMessage)
        assertEquals(
            "No pude leer el número. Escríbelo tú mismo.",
            estado.ocrMessage,
        )
        assertEquals(
            "El OCR fallido NUNCA borra lo que el usuario escribió",
            "42",
            estado.quantityInput,
        )
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
    }

    @Test
    fun ocrExitosoSobreescribeLaCantidadYLimpiaElMensaje() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("9")
        vm.onEscanearResultado(7)

        assertEquals("7", vm.uiState.value.quantityInput)
        assertNull(vm.uiState.value.ocrMessage)

        // Un número enorme se limita al tope (999.999).
        vm.onEscanearResultado(1_500_000)
        assertEquals("999999", vm.uiState.value.quantityInput)
    }

    // ---------------------------------------------------------------------
    // Lote: varios productos en una sesión (feedback del usuario)
    // ---------------------------------------------------------------------

    @Test
    fun agregarOtroNoPersisteHastaGuardar() = runTest {
        val hoy = LocalDate.now()
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        assertEquals(Screen.CONFIRM, vm.uiState.value.screen)

        vm.onAgregarOtro()
        scheduler.runCurrent()

        val estado = vm.uiState.value
        assertEquals("Agregar al lote NO persiste hasta GUARDAR", 0, dao.insertados.size)
        assertEquals(1, estado.itemsPendientes.size)

        val draft = estado.itemsPendientes.first()
        assertEquals("El draft vive en memoria con id=0", 0L, draft.id)
        assertEquals("Cryotubo", draft.productName)
        assertEquals(7.90, draft.unitPriceCop, 0.0)
        assertEquals(3, draft.quantity)
        assertEquals(23.70, draft.totalCop, 1e-9) // 3 × 7.90 redondeado a 2 decimales
        assertEquals(hoy, draft.date)
        val campos = WeekFields.of(Locale.getDefault())
        assertEquals(hoy.get(campos.weekOfWeekBasedYear()), draft.weekOfYear)
        assertEquals(hoy.get(campos.weekBasedYear()), draft.year)

        // Vuelve al Paso 1 con la selección limpia; el lote ya suma en los derivados.
        assertEquals(Screen.PICK_PRODUCT, estado.screen)
        assertNull(estado.selectedProduct)
        assertEquals("0", estado.quantityInput)
        assertEquals(3, estado.cantidadLote) // pendientes 3 + actual 0
        assertEquals(23.70, estado.totalLoteCop, 1e-9)
    }

    @Test
    fun guardarLoteInsertaPendientesYActualConIdsReales() = runTest {
        val vm = crearViewModel()
        // Dos productos se agregan al lote...
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        vm.onAgregarOtro()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("2")
        vm.onSiguiente()
        vm.onAgregarOtro()
        // ...y el tercero queda como item ACTUAL en el Paso 3.
        vm.onProductoElegido(ProductType.FRASCO_ORINA)
        vm.escribirCantidad("5")
        vm.onSiguiente()
        assertEquals(Screen.CONFIRM, vm.uiState.value.screen)
        assertEquals(2, vm.uiState.value.itemsPendientes.size)
        assertEquals(10, vm.uiState.value.cantidadLote) // 3 + 2 + 5
        assertEquals(75.40, vm.uiState.value.totalLoteCop, 1e-9) // 23.70 + 15.80 + 35.90
        assertEquals(0, dao.insertados.size) // nada persistido aún

        vm.onConfirmarGuardar()
        scheduler.runCurrent()

        val estado = vm.uiState.value
        assertEquals(Screen.SUCCESS, estado.screen)
        assertEquals("El lote se vacía al guardarse", 0, estado.itemsPendientes.size)
        assertEquals("Se insertan pendientes + actual, en orden", 3, dao.insertados.size)
        assertEquals(3, estado.lastSavedRecords.size)
        assertTrue("ids REALES de Room", estado.lastSavedRecords.all { it.id > 0 })
        assertEquals(
            listOf("Cryotubo", "Falcon", "Frasco de orina"),
            estado.lastSavedRecords.map { it.productName },
        )
        assertEquals(
            "lastSavedRecord COMPAT = último del lote",
            estado.lastSavedRecords.last(),
            estado.lastSavedRecord,
        )
        // La selección queda limpia tras el éxito.
        assertNull(estado.selectedProduct)
        assertEquals("0", estado.quantityInput)
        assertEquals("El resumen semanal reacciona a los 3 inserts", 3, dao.registrosActuales().size)
    }

    @Test
    fun quitarPendienteEliminaSoloEseItemYPermaneceEnConfirm() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        vm.onAgregarOtro()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("2")
        vm.onSiguiente()
        vm.onAgregarOtro()
        // Item actual para volver al Paso 3 con el lote de 2.
        vm.onProductoElegido(ProductType.BACILOSCOPIA)
        vm.escribirCantidad("1")
        vm.onSiguiente()
        assertEquals(Screen.CONFIRM, vm.uiState.value.screen)
        assertEquals(2, vm.uiState.value.itemsPendientes.size)

        vm.onQuitarPendiente(0)

        val estado = vm.uiState.value
        assertEquals("Permanece en el Paso 3", Screen.CONFIRM, estado.screen)
        assertEquals(1, estado.itemsPendientes.size)
        assertEquals("Quitó el primero (Cryotubo) y conserva Falcon", "Falcon", estado.itemsPendientes[0].productName)
        assertEquals("Quitar del lote NO persiste nada", 0, dao.insertados.size)

        vm.onQuitarPendiente(99) // posición fuera de rango: no-op, no crashea
        assertEquals(1, vm.uiState.value.itemsPendientes.size)
    }

    @Test
    fun deshacerLoteBorraPorPkYRestauraLaListaEnElPaso3() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        vm.onAgregarOtro()
        vm.onProductoElegido(ProductType.CAJA_COPROLOGICA)
        vm.escribirCantidad("4")
        vm.onSiguiente()
        assertEquals(Screen.CONFIRM, vm.uiState.value.screen)

        vm.onConfirmarGuardar()
        scheduler.runCurrent()
        assertEquals(2, dao.insertados.size)
        assertEquals(2, dao.registrosActuales().size)

        vm.onDeshacer() // dentro de la ventana
        scheduler.runCurrent()

        val estado = vm.uiState.value
        assertEquals("Vuelve al Paso 3 para corregir la lista", Screen.CONFIRM, estado.screen)
        assertFalse(estado.canUndo)
        assertNull(estado.lastSavedRecord)
        assertNull(estado.savingError)
        assertEquals("Borra TODOS por PK", 2, dao.borrados.size)
        assertTrue(dao.registrosActuales().isEmpty())
        // La lista se restaura como drafts (id=0) para corregir el lote.
        assertEquals(2, estado.itemsPendientes.size)
        assertTrue(estado.itemsPendientes.all { it.id == 0L })
        assertEquals(
            listOf("Cryotubo", "Caja coprológica"),
            estado.itemsPendientes.map { it.productName },
        )
    }

    // ---------------------------------------------------------------------
    // OCR de IMAGEN (FASE 4): 1 número / varios / ninguno — nunca destructivo
    // ---------------------------------------------------------------------

    @Test
    fun imagenConUnNumeroLlenaLaCantidadYLimpiaElMensaje() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("7")

        // Un fallo previo de la foto queda en ocrMessage…
        vm.onImagenLeida(emptyList())
        assertEquals(InventoryViewModel.MENSAJE_IMAGEN_FALLIDO, vm.uiState.value.ocrMessage)

        // …y una lectura con EXACTAMENTE 1 número lo carga y limpia el mensaje.
        vm.onImagenLeida(listOf(42))

        val estado = vm.uiState.value
        assertEquals("42", estado.quantityInput)
        assertNull("La lectura limpia el mensaje de fallo previo", estado.ocrMessage)
        assertTrue("Sin candidatos cuando hubo un solo número", estado.ocrCandidatos.isEmpty())
        assertFalse(estado.quantityError)
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
        assertEquals("La foto jamás persiste", 0, dao.insertados.size)
    }

    @Test
    fun imagenConVariosNumerosMuestraCandidatosSinTocarLaCantidad() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("7") // lo que la persona venía escribiendo

        vm.onImagenLeida(listOf(10, 25))

        val estado = vm.uiState.value
        assertEquals(listOf(10, 25), estado.ocrCandidatos)
        assertEquals(
            "Varios números NO tocan quantityInput hasta que se elija uno",
            "7",
            estado.quantityInput,
        )
        assertNull(estado.ocrMessage)
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun imagenSinNumerosMuestraElMensajeFallidoConservandoLaCantidad() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("25")

        vm.onImagenLeida(emptyList())

        val estado = vm.uiState.value
        assertEquals(InventoryViewModel.MENSAJE_IMAGEN_FALLIDO, estado.ocrMessage)
        assertEquals(
            "No pude leer números en la imagen. Escríbelo tú mismo.",
            estado.ocrMessage,
        )
        assertEquals(
            "La foto fallida NUNCA borra lo que la persona escribió",
            "25",
            estado.quantityInput,
        )
        assertTrue(estado.ocrCandidatos.isEmpty())
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
    }

    @Test
    fun candidatoElegidoCargaLaCantidadYCierraLosCandidatos() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO) // la foto solo se ofrece en el Paso 2
        vm.onImagenLeida(listOf(10, 25))

        vm.onCandidatoElegido(25)

        val estado = vm.uiState.value
        assertEquals("25", estado.quantityInput)
        assertTrue("Al elegir se cierra la selección", estado.ocrCandidatos.isEmpty())
        assertNull(estado.ocrMessage)
        assertFalse(estado.quantityError)
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)

        // Mismos topes que el teclado: > 999.999 se limita al tope.
        vm.onImagenLeida(listOf(10, 25))
        vm.onCandidatoElegido(1_500_000)
        assertEquals("999999", vm.uiState.value.quantityInput)
        assertTrue(vm.uiState.value.ocrCandidatos.isEmpty())
    }

    @Test
    fun cerrarCandidatosConservaLaCantidadEscrita() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("7")
        vm.onImagenLeida(listOf(10, 25))

        vm.onCerrarCandidatos() // botón "✍️ Escribir a mano"

        val estado = vm.uiState.value
        assertTrue("Los chips desaparecen", estado.ocrCandidatos.isEmpty())
        assertEquals(
            "Cerrar candidatos NO cambia la cantidad: la persona escribe a mano",
            "7",
            estado.quantityInput,
        )
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun imagenConUnNumeroSobreElTopeSeOmiteYConservaLaCantidad() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("3")

        // 1 número pero > 999.999: no es escribible → se OMITE con el mensaje.
        vm.onImagenLeida(listOf(1_500_000))

        val estado = vm.uiState.value
        assertEquals(InventoryViewModel.MENSAJE_IMAGEN_FALLIDO, estado.ocrMessage)
        assertEquals("3", estado.quantityInput)
        assertTrue(estado.ocrCandidatos.isEmpty())
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
    }

    @Test
    fun losCandidatosSeLimitanAlTopeDeLaApp() = runTest {
        val vm = crearViewModel()
        vm.onImagenLeida((1..20).toList())

        val estado = vm.uiState.value
        assertEquals(
            "La lista de chips se trunca en el tope MAXIMO_CANDIDATOS_IMAGEN",
            MAXIMO_CANDIDATOS_IMAGEN,
            estado.ocrCandidatos.size,
        )
        assertEquals(12, estado.ocrCandidatos.size) // tope documentado (parser + UI + VM)
        assertTrue("quantityInput intacto hasta elegir", estado.quantityInput == "0")
    }

    // ---------------------------------------------------------------------
    // Lote: guardas de onAgregarOtro + onContinuarLote + quitar el último
    // ---------------------------------------------------------------------

    @Test
    fun agregarOtroSoloAgregaDesdeElPaso3() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        // Estamos en el PASO 2 (ENTER_QUANTITY): el guard "solo desde CONFIRM"
        // debe hacer no-op — ni agrega, ni cambia de pantalla, ni limpia nada.
        vm.onAgregarOtro()

        val estado = vm.uiState.value
        assertEquals(Screen.ENTER_QUANTITY, estado.screen)
        assertTrue("No debe agregar nada fuera del Paso 3", estado.itemsPendientes.isEmpty())
        assertEquals("3", estado.quantityInput)
        assertEquals(ProductType.CRYOTUBO, estado.selectedProduct)
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun agregarOtroSinProductoVuelveAlPaso1SinAgregar() = runTest {
        val vm = crearViewModel() // INICIO, sin producto elegido
        vm.onAgregarOtro()

        val estado = vm.uiState.value
        // Mismo guard que guardar: sin producto → Paso 1, sin agregar ni persistir.
        assertEquals(Screen.PICK_PRODUCT, estado.screen)
        assertTrue(estado.itemsPendientes.isEmpty())
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun continuarLoteDesdeElInicioAbreElPaso3ConLaLista() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        vm.onAgregarOtro() // lista = [Cryotubo × 3], pantalla = Paso 1
        vm.onAtras() // Paso 1 → INICIO (el lote NO debe limpiarse en el camino)
        assertEquals(Screen.START, vm.uiState.value.screen)
        assertEquals(1, vm.uiState.value.itemsPendientes.size)

        vm.onContinuarLote()

        val estado = vm.uiState.value
        assertEquals(Screen.CONFIRM, estado.screen)
        assertEquals(1, estado.itemsPendientes.size)
        assertEquals("Cryotubo", estado.itemsPendientes.single().productName)
        assertEquals(0L, estado.itemsPendientes.single().id) // sigue siendo draft
        assertEquals(3, estado.cantidadLote) // pendientes 3 + actual 0
        assertEquals(23.70, estado.totalLoteCop, 1e-9)
        assertEquals("Seguimos sin persistir", 0, dao.insertados.size)
    }

    @Test
    fun continuarLoteSinListaOEnOtraPantallaEsNoOp() = runTest {
        // (a) Sin lista empezada en INICIO → no-op (sigue en INICIO).
        val vm = crearViewModel()
        vm.onContinuarLote()
        assertEquals(Screen.START, vm.uiState.value.screen)

        // (b) Con lista pero FUERA de INICIO (Paso 1 tras Agregar otro) → no-op.
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        vm.onAgregarOtro()
        assertEquals(Screen.PICK_PRODUCT, vm.uiState.value.screen)

        vm.onContinuarLote()

        assertEquals(
            "Fuera de START onContinuarLote no debe navegar",
            Screen.PICK_PRODUCT,
            vm.uiState.value.screen,
        )
        assertEquals(1, vm.uiState.value.itemsPendientes.size) // la lista no se toca
        assertEquals(0, dao.insertados.size)
    }

    @Test
    fun quitarElUltimoPendienteDejaLaListaVaciaSinSalirDelPaso3() = runTest {
        val vm = crearViewModel()
        vm.onProductoElegido(ProductType.CRYOTUBO)
        vm.escribirCantidad("3")
        vm.onSiguiente()
        vm.onAgregarOtro()
        vm.onProductoElegido(ProductType.FALCON)
        vm.escribirCantidad("2")
        vm.onSiguiente()
        assertEquals(Screen.CONFIRM, vm.uiState.value.screen)
        assertEquals(1, vm.uiState.value.itemsPendientes.size)

        vm.onQuitarPendiente(0) // quita el ÚNICO pendiente

        val estado = vm.uiState.value
        assertEquals("Permanece en el Paso 3", Screen.CONFIRM, estado.screen)
        assertTrue("La lista queda vacía (solo resta el item actual)", estado.itemsPendientes.isEmpty())
        assertEquals(2, estado.cantidadLote) // solo el item actual: 2 Falcon
        assertEquals(15.80, estado.totalLoteCop, 1e-9) // 2 × 7.90
        assertEquals("Quitar del lote NO persiste nada", 0, dao.insertados.size)
    }
}
