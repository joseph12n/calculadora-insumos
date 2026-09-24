package com.bioplast.insumos.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bioplast.insumos.model.InventoryRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests del [InventoryDao] con Room IN-MEMORY sobre el SQLite REAL de Android.
 * Son la copia EJECUTABLE de `app/src/test/.../data/InventoryDaoTest.kt` (que está
 * @Ignore'ado en la JVM porque Room necesita el runtime de Android; sin Robolectric
 * no puede correr ahí).
 *
 * Ejecución: `./gradlew :app:connectedAndroidTest` con un emulador o dispositivo.
 */
@RunWith(AndroidJUnit4::class)
class InventoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: InventoryDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.inventoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Fechas fijas: las consultas filtran por los campos GUARDADOS (year/weekOfYear),
    // así que los tests son deterministas sin depender del locale del dispositivo.
    private fun registro(
        fecha: LocalDate = LocalDate.of(2026, 3, 2),
        anio: Int = 2026,
        semana: Int = 10,
        producto: String = "Frasco de orina",
        precio: Double = 7.18,
        cantidad: Int = 1,
        total: Double = 7.18,
    ) = InventoryRecord(
        date = fecha,
        weekOfYear = semana,
        year = anio,
        productName = producto,
        unitPriceCop = precio,
        quantity = cantidad,
        totalCop = total,
    )

    @Test
    fun insertDevuelveIdPositivoYElRegistroEsRecuperablePorId() = runBlocking {
        val id = dao.insertRecord(registro())
        assertTrue("insertRecord debe devolver el rowId autogenerado (> 0)", id > 0)

        val recuperado = dao.getRecordsByDate(LocalDate.of(2026, 3, 2)).first()
        assertEquals(1, recuperado.size)
        assertEquals(id, recuperado.first().id) // id real → el Deshacer puede borrar por PK
    }

    @Test
    fun getRecordsByDateFiltraPorFechaYOrdenaDesc() = runBlocking {
        val fecha = LocalDate.of(2026, 3, 2)
        val id1 = dao.insertRecord(registro(fecha = fecha, producto = "Primero"))
        val id2 = dao.insertRecord(registro(fecha = fecha, producto = "Segundo"))
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 3), producto = "Otro día"))

        val delDia = dao.getRecordsByDate(fecha).first()
        assertEquals(2, delDia.size)
        assertEquals(id2, delDia[0].id) // ORDER BY id DESC: el más reciente primero
        assertEquals(id1, delDia[1].id)
        assertEquals("Segundo", delDia[0].productName)
    }

    @Test
    fun getWeeklyTotalEsNuloParaSemanaVacia() = runBlocking {
        assertNull(dao.getWeeklyTotal(2026, 99).first())
    }

    @Test
    fun getWeeklyTotalSumaTotalesYCantidades() = runBlocking {
        dao.insertRecord(registro(cantidad = 2, total = 10.0))
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 3), cantidad = 3, total = 20.5))
        // Otra semana NO debe mezclarse:
        dao.insertRecord(registro(semana = 11, cantidad = 9, total = 99.0))

        val resumen = dao.getWeeklyTotal(2026, 10).first()
        assertNotNull("La semana 10 tiene registros: no debe ser null", resumen)
        resumen!!
        assertEquals(2026, resumen.year)
        assertEquals(10, resumen.weekOfYear)
        assertEquals(30.5, resumen.totalCop, 0.0)
        assertEquals(5L, resumen.totalQuantity)
    }

    @Test
    fun getAllWeeklySummariesOrdenaDeLaSemanaMasRecienteALaMasAntigua() = runBlocking {
        dao.insertRecord(registro(semana = 1, total = 1.0))
        dao.insertRecord(registro(semana = 10, total = 2.0))
        dao.insertRecord(registro(anio = 2025, semana = 52, total = 3.0))

        val resumenes = dao.getAllWeeklySummaries().first()
        assertEquals(3, resumenes.size)
        // ORDER BY year DESC, weekOfYear DESC:
        assertEquals(2026 to 10, resumenes[0].year to resumenes[0].weekOfYear)
        assertEquals(2026 to 1, resumenes[1].year to resumenes[1].weekOfYear)
        assertEquals(2025 to 52, resumenes[2].year to resumenes[2].weekOfYear)
    }

    @Test
    fun getRecordsByWeekFiltraAnioYSemana() = runBlocking {
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 5), producto = "De la semana"))
        dao.insertRecord(registro(fecha = LocalDate.of(2026, 3, 2), producto = "También"))
        dao.insertRecord(registro(semana = 11, producto = "Otra semana"))
        dao.insertRecord(registro(anio = 2025, producto = "Otro año"))

        val deLaSemana = dao.getRecordsByWeek(2026, 10).first()
        assertEquals(2, deLaSemana.size)
        // ORDER BY date ASC, id ASC:
        assertEquals(LocalDate.of(2026, 3, 2), deLaSemana[0].date)
        assertEquals("También", deLaSemana[0].productName)
        assertEquals(LocalDate.of(2026, 3, 5), deLaSemana[1].date)
    }

    @Test
    fun deleteRecordEliminaSoloElRegistroIndicado() = runBlocking {
        val fecha = LocalDate.of(2026, 3, 2)
        val idBorrable = dao.insertRecord(registro(fecha = fecha, producto = "Bórrame"))
        dao.insertRecord(registro(fecha = fecha, producto = "Déjame"))

        // Regresión del bug corregido: borrar debe usar el id REAL devuelto por insert.
        val filas = dao.getRecordsByDate(fecha).first()
        val aBorrable = filas.first { it.id == idBorrable }
        dao.deleteRecord(aBorrable)

        val restantes = dao.getRecordsByDate(fecha).first()
        assertEquals(1, restantes.size)
        assertEquals("Déjame", restantes.single().productName)
    }
}
